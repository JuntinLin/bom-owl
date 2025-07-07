package com.jfc.owl.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ValidityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Manager class to handle reasoning performance optimizations
 * Provides timeout mechanisms and performance monitoring for reasoning operations
 */
@Component
public class ReasoningPerformanceManager {
    private static final Logger logger = LoggerFactory.getLogger(ReasoningPerformanceManager.class);
    
    private final ExecutorService executorService;
    private final Map<String, PerformanceMetrics> metricsCache;
    
    public ReasoningPerformanceManager() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Reasoning-Worker-" + t.getId());
            return t;
        });
        this.metricsCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Perform reasoning with timeout protection
     */
    public InfModel performReasoningWithTimeout(Reasoner reasoner, OntModel ontModel, 
                                               String reasonerType, int timeoutSeconds) 
                                               throws TimeoutException, ExecutionException {
        
        long startTime = System.currentTimeMillis();
        String metricsKey = reasonerType + "_" + ontModel.size();
        
        Future<InfModel> future = executorService.submit(() -> {
            logger.debug("Starting reasoning with {} on model with {} statements", 
                        reasonerType, ontModel.size());
            
            InfModel infModel = org.apache.jena.rdf.model.ModelFactory.createInfModel(reasoner, ontModel);
            
            // Force initial reasoning by accessing the model
            infModel.size(); // This triggers reasoning
            
            return infModel;
        });
        
        try {
            InfModel result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            // Record success metrics
            long duration = System.currentTimeMillis() - startTime;
            recordMetrics(metricsKey, true, duration, ontModel.size(), result.size());
            
            return result;
            
        } catch (TimeoutException e) {
            future.cancel(true);
            
            // Record timeout metrics
            long duration = System.currentTimeMillis() - startTime;
            recordMetrics(metricsKey, false, duration, ontModel.size(), -1);
            
            logger.error("Reasoning timeout after {} seconds for {} with {} statements", 
                        timeoutSeconds, reasonerType, ontModel.size());
            throw e;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Reasoning interrupted", e);
        }
    }
    
    /**
     * Validate model with timeout protection
     */
    public ValidityReport validateWithTimeout(InfModel infModel, int timeoutSeconds) 
                                            throws TimeoutException, ExecutionException {
        
        Future<ValidityReport> future = executorService.submit(() -> {
            logger.debug("Starting model validation");
            return infModel.validate();
        });
        
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.error("Validation timeout after {} seconds", timeoutSeconds);
            throw e;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Validation interrupted", e);
        }
    }
    
    /**
     * Get recommended reasoner based on model characteristics
     */
    public String recommendReasoner(OntModel ontModel, boolean needsHydraulicReasoning) {
        long modelSize = ontModel.size();
        
        // Check past performance metrics
        String owlKey = "OWL_" + modelSize;
        PerformanceMetrics owlMetrics = metricsCache.get(owlKey);
        
        if (owlMetrics != null && !owlMetrics.successful) {
            logger.info("Previous OWL reasoning failed for similar model size, recommending alternative");
            return needsHydraulicReasoning ? "ENHANCED_HYDRAULIC" : "OWL_MINI";
        }
        
        // Size-based recommendations
        if (modelSize > 50000) {
            logger.info("Very large model ({} statements), recommending RDFS", modelSize);
            return "RDFS";
        } else if (modelSize > 10000) {
            logger.info("Large model ({} statements), recommending OWL_MICRO", modelSize);
            return "OWL_MICRO";
        } else if (modelSize > 5000) {
            logger.info("Medium model ({} statements), recommending OWL_MINI", modelSize);
            return "OWL_MINI";
        } else if (needsHydraulicReasoning) {
            logger.info("Small model ({} statements) with hydraulic components, recommending ENHANCED_HYDRAULIC", modelSize);
            return "ENHANCED_HYDRAULIC";
        } else {
            logger.info("Small model ({} statements), OWL reasoning feasible", modelSize);
            return "OWL";
        }
    }
    
    /**
     * Get timeout recommendation based on model size and reasoner type
     */
    public int getRecommendedTimeout(long modelSize, String reasonerType) {
        // Base timeout
        int timeout = 30;
        
        // Adjust for model size
        if (modelSize > 10000) {
            timeout += 30;
        } else if (modelSize > 5000) {
            timeout += 15;
        }
        
        // Adjust for reasoner type
        switch (reasonerType) {
            case "OWL":
                timeout *= 2; // OWL needs more time
                break;
            case "ENHANCED_HYDRAULIC":
                timeout = (int) (timeout * 1.5); // Custom rules need some extra time
                break;
            case "OWL_MINI":
                timeout = (int) (timeout * 1.2);
                break;
            case "RDFS":
                timeout = Math.max(10, timeout / 2); // RDFS is fast
                break;
        }
        
        return Math.min(timeout, 300); // Cap at 5 minutes
    }
    
    /**
     * Record performance metrics for analysis
     */
    private void recordMetrics(String key, boolean successful, long duration, 
                              long inputSize, long outputSize) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.successful = successful;
        metrics.duration = duration;
        metrics.inputSize = inputSize;
        metrics.outputSize = outputSize;
        metrics.timestamp = System.currentTimeMillis();
        
        metricsCache.put(key, metrics);
        
        // Clean old metrics (older than 1 hour)
        long cutoff = System.currentTimeMillis() - 3600000;
        metricsCache.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoff);
    }
    
    /**
     * Get performance report
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Summary statistics
        int totalOperations = metricsCache.size();
        long successCount = metricsCache.values().stream().filter(m -> m.successful).count();
        double successRate = totalOperations > 0 ? (double) successCount / totalOperations : 0.0;
        
        report.put("totalOperations", totalOperations);
        report.put("successCount", successCount);
        report.put("successRate", Math.round(successRate * 100));
        
        // Average durations by reasoner type
        Map<String, Double> avgDurations = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        
        for (Map.Entry<String, PerformanceMetrics> entry : metricsCache.entrySet()) {
            String reasonerType = entry.getKey().split("_")[0];
            PerformanceMetrics metrics = entry.getValue();
            
            if (metrics.successful) {
                avgDurations.merge(reasonerType, (double) metrics.duration, Double::sum);
                counts.merge(reasonerType, 1, Integer::sum);
            }
        }
        
        // Calculate averages
        Map<String, Long> averages = new HashMap<>();
        avgDurations.forEach((type, total) -> {
            int count = counts.get(type);
            averages.put(type, Math.round(total / count));
        });
        
        report.put("averageDurationsByType", averages);
        
        return report;
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Inner class to hold performance metrics
     */
    private static class PerformanceMetrics {
        boolean successful;
        long duration;
        long inputSize;
        long outputSize;
        long timestamp;
    }
}