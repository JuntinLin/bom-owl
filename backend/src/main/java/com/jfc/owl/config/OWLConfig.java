package com.jfc.owl.config;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLStorerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class OWLConfig {

    @Value("${owl.ontology.base-path}")
    private String ontologyBasePath;
    
    @Value("${owl.reasoner.type:hermit}")
    private String reasonerType;
    
    @Bean
    OWLOntologyManager owlOntologyManager() {
        return OWLManager.createOWLOntologyManager();
    }
    
    @Bean
    OWLReasonerFactory owlReasonerFactory() {
        if ("pellet".equalsIgnoreCase(reasonerType)) {
            // For Pellet, you would need to add the appropriate dependency
            // return new PelletReasonerFactory();
            throw new IllegalArgumentException("Pellet reasoner not yet configured");
        } else {
            return new ReasonerFactory(); // HermiT
        }
    }

    @Bean
    File ontologyBaseDirectory() {
        File directory = new File(ontologyBasePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    @Bean
    Set<OWLStorerFactory> owlStorerFactories() {
        // Get the storer factories from the default OWL Manager
        OWLOntologyManager tempManager = OWLManager.createOWLOntologyManager();
        PriorityCollection<OWLStorerFactory> storers = tempManager.getOntologyStorers();
        
        // Convert to a Set to satisfy Spring's dependency requirements
        Set<OWLStorerFactory> storerFactories = new HashSet<>();
        storers.forEach(storerFactories::add);
        
        return storerFactories;
    }

    @Bean
    Set<OWLOntologyFactory> owlOntologyFactories() {
        // Get the ontology factories from the default OWL Manager
        OWLOntologyManager tempManager = OWLManager.createOWLOntologyManager();
        PriorityCollection<OWLOntologyFactory> factories = tempManager.getOntologyFactories();
        
        // Convert to a Set to satisfy Spring's dependency requirements
        Set<OWLOntologyFactory> ontologyFactories = new HashSet<>();
        factories.forEach(ontologyFactories::add);
        
        return ontologyFactories;
    }

    @Bean
    Set<OWLOntologyIRIMapper> owlOntologyIRIMappers() {
        // Get the IRI mappers from the default OWL Manager or create an empty set
        OWLOntologyManager tempManager = OWLManager.createOWLOntologyManager();
        PriorityCollection<OWLOntologyIRIMapper> mappers = tempManager.getIRIMappers();
        
        // Convert to a Set to satisfy Spring's dependency requirements
        Set<OWLOntologyIRIMapper> iriMappers = new HashSet<>();
        mappers.forEach(iriMappers::add);
        
        return iriMappers;
    }

    @Bean
    Set<OWLParserFactory> owlParserFactories() {
        // Get the parser factories from the default OWL Manager
        OWLOntologyManager tempManager = OWLManager.createOWLOntologyManager();
        PriorityCollection<OWLParserFactory> parsers = tempManager.getOntologyParsers();
        
        // Convert to a Set to satisfy Spring's dependency requirements
        Set<OWLParserFactory> parserFactories = new HashSet<>();
        parsers.forEach(parserFactories::add);
        
        return parserFactories;
    }
}