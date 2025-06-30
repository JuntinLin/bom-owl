// src/pages/KnowledgeBaseManagement.tsx
import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Switch } from '@/components/ui/switch';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { toast } from 'sonner';

// Icons
import {
  Database,
  Download,
  Upload,
  Search,
  RefreshCw,
  Trash2,
  FileText,
  BarChart3,
  Settings,
  CheckCircle,
  Clock,
  HardDrive,
  Layers,
  Brain,
  Zap, Loader2,Pause,
  Play,
  X,
} from 'lucide-react';

// Import service and types
import knowledgeBaseService, {
  type KnowledgeBaseEntry,
  type KnowledgeBaseStats,
  type BatchExportResult,
  type CleanupResult,
  type BatchProgress,
  type ProcessingLog
} from '@/services/knowledgeBaseService';

// Utility functions
const formatFileSize = (bytes: number): string => {
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  if (bytes === 0) return '0 Bytes';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
};

const formatNumber = (num: number): string => {
  return new Intl.NumberFormat().format(num);
};

const formatDuration = (seconds?: number): string => {
  if (!seconds) return 'N/A';
  
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${secs}s`;
  } else {
    return `${secs}s`;
  }
};

const getStatusVariant = (status: string): "default" | "secondary" | "destructive" | "outline" => {
  switch (status) {
    case 'COMPLETED':
      return 'default';
    case 'PROCESSING':
      return 'secondary';
    case 'FAILED':
      return 'destructive';
    case 'PAUSED':
    case 'CANCELLED':
      return 'outline';
    default:
      return 'outline';
  }
};

// Main Component
const KnowledgeBaseManagement = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState<KnowledgeBaseStats | null>(null);
  const [searchResults, setSearchResults] = useState<KnowledgeBaseEntry[]>([]);
  
  // Export states
  const [exportLoading, setExportLoading] = useState(false);
  const [exportProgress, setExportProgress] = useState(0);
  const [batchResult, setBatchResult] = useState<BatchExportResult | null>(null);
  
  // Batch processing states
  const [batchId, setBatchId] = useState<string | null>(null);
  const [batchStatus, setBatchStatus] = useState<'idle' | 'processing' | 'paused' | 'completed' | 'failed'>('idle');
  const [batchProgress, setBatchProgress] = useState<BatchProgress | null>(null);
  const [batchHistory, setBatchHistory] = useState<ProcessingLog[]>([]);
  const [isPolling, setIsPolling] = useState(false);
  
  // Search states
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchLoading, setSearchLoading] = useState(false);
  
  // Single export form states
  const [singleExportForm, setSingleExportForm] = useState({
    masterItemCode: '',
    format: 'RDF/XML',
    includeHierarchy: true,
    description: ''
  });

  // Load initial data
  useEffect(() => {
    loadStatistics();
    loadBatchHistory();
  }, []);

  // Polling for batch progress
  useEffect(() => {
    let intervalId: NodeJS.Timeout;
    
    if (isPolling && batchId) {
      intervalId = setInterval(async () => {
        try {
          const progress = await knowledgeBaseService.getBatchProgress(batchId);
          setBatchProgress(progress);
          
          // Update progress
          setExportProgress(progress.progressPercentage || 0);
          
          // Check if completed
          if (progress.status === 'COMPLETED' || progress.status === 'FAILED' || progress.status === 'CANCELLED') {
            setIsPolling(false);
            setBatchStatus(progress.status.toLowerCase() as any);
            
            // Reload statistics after completion
            if (progress.status === 'COMPLETED') {
              await loadStatistics();
              toast.success(`Batch export completed: ${progress.successCount} successful, ${progress.failureCount} failed`);
            }
          }
        } catch (error) {
          console.error('Error polling batch progress:', error);
        }
      }, 2000); // Poll every 2 seconds
    }
    
    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [isPolling, batchId]);

  const loadStatistics = async () => {
    try {
      setLoading(true);
      const statsData = await knowledgeBaseService.getStatistics();
      setStats(statsData);
      setError(null);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load knowledge base statistics';
      setError(errorMessage);
      console.error('Error loading statistics:', err);

      // Set default stats to prevent UI breaks
      setStats({
        totalEntries: 0,
        totalFileSize: 0,
        totalTriples: 0,
        hydraulicCylinderCount: 0,
        formatDistribution: {},
        cacheSize: 0,
        lastMasterUpdate: new Date().toISOString()
      });
    } finally {
      setLoading(false);
    }
  };

  const loadBatchHistory = async () => {
    try {
      const history = await knowledgeBaseService.getBatchHistory();
      setBatchHistory(history);
    } catch (err) {
      console.error('Failed to load batch history:', err);
    }
  };

  const handleInitializeKnowledgeBase = async () => {
    try {
      setLoading(true);
      const result = await knowledgeBaseService.initializeKnowledgeBase();
      toast.success(result || 'Knowledge base initialized successfully');
      await loadStatistics();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to initialize knowledge base';
      toast.error(errorMessage);
      console.error('Error initializing knowledge base:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSingleExport = async () => {
    if (!singleExportForm.masterItemCode.trim()) {
      toast.error('Please enter a master item code');
      return;
    }

    try {
      setExportLoading(true);
      const result = await knowledgeBaseService.exportSingleToKnowledgeBase(singleExportForm);
      toast.success(`Successfully exported ${result.masterItemCode} to knowledge base`);
      
      // Reset form
      setSingleExportForm({
        masterItemCode: '',
        format: 'RDF/XML',
        includeHierarchy: true,
        description: ''
      });
      
      await loadStatistics();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to export to knowledge base';
      toast.error(errorMessage);
      console.error('Error exporting single item:', err);
    } finally {
      setExportLoading(false);
    }
  };

  const handleBatchExport = async () => {
    try {
      setExportLoading(true);
      setExportProgress(0);
      setBatchResult(null);
      setBatchStatus('processing');
      
      const result = await knowledgeBaseService.exportAllToKnowledgeBase({
        format: 'RDF/XML',
        includeHierarchy: true
      });
      
      if (result.batchId) {
        setBatchId(result.batchId);
        setIsPolling(true);
        toast.success(`Batch export started with ID: ${result.batchId}`);
      } else {
        // Legacy response without batch ID
        setBatchResult(result);
        setExportProgress(100);
        setBatchStatus('completed');
        toast.success(`Batch export completed: ${result.successCount} successful, ${result.failureCount} failed`);
        await loadStatistics();
      }
      
    } catch (err) {
      setBatchStatus('failed');
      const errorMessage = err instanceof Error ? err.message : 'Failed to perform batch export';
      toast.error(errorMessage);
      console.error('Error batch exporting:', err);
    } finally {
      setExportLoading(false);
    }
  };

  const handlePauseBatch = async () => {
    if (!batchId) return;
    
    try {
      await knowledgeBaseService.pauseBatchExport(batchId);
      setBatchStatus('paused');
      setIsPolling(false);
      toast.info('Batch export paused');
    } catch (err) {
      toast.error('Failed to pause batch export');
    }
  };

  const handleResumeBatch = async () => {
    if (!batchId) return;
    
    try {
      await knowledgeBaseService.resumeBatchExport(batchId);
      setBatchStatus('processing');
      setIsPolling(true);
      toast.success('Batch export resumed');
    } catch (err) {
      toast.error('Failed to resume batch export');
    }
  };

  const handleCancelBatch = async () => {
    if (!batchId) return;
    
    try {
      await knowledgeBaseService.cancelBatchExport(batchId);
      setBatchStatus('completed');
      setIsPolling(false);
      toast.warning('Batch export cancelled');
    } catch (err) {
      toast.error('Failed to cancel batch export');
    }
  };
  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      toast.error('Please enter a search keyword');
      return;
    }

    try {
      setSearchLoading(true);
      const results = await knowledgeBaseService.searchKnowledgeBase(searchKeyword);
      setSearchResults(results);

      if (results.length === 0) {
        toast.info('No results found');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to search knowledge base';
      toast.error(errorMessage);
      console.error('Error searching:', err);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleCleanup = async () => {
    try {
      setLoading(true);
      const result = await knowledgeBaseService.cleanupKnowledgeBase();
      toast.success(`Cleanup completed: ${result.deletedCount} entries deleted`);
      await loadStatistics();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to cleanup knowledge base';
      toast.error(errorMessage);
      console.error('Error cleaning up:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (masterItemCode: string, format: string) => {
    try {
      await knowledgeBaseService.downloadOWLFile(masterItemCode, format);
      toast.success('Download started');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to download file';
      toast.error(errorMessage);
      console.error('Error downloading:', err);
    }
  };

  const handleResumePreviousBatch = async (previousBatchId: string) => {
    setBatchId(previousBatchId);
    setBatchStatus('processing');
    setIsPolling(true);
    
    try {
      await knowledgeBaseService.resumeBatchExport(previousBatchId);
      toast.success('Previous batch export resumed');
    } catch (err) {
      setBatchStatus('failed');
      setIsPolling(false);
      toast.error('Failed to resume previous batch');
    }
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">OWL Knowledge Base Management</h1>
          <p className="text-gray-600">Manage OWL files for intelligent BOM generation</p>
        </div>
        <div className="flex gap-2">
          <Button onClick={loadStatistics} variant="outline" disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button onClick={handleInitializeKnowledgeBase} variant="outline" disabled={loading}>
            <Settings className="h-4 w-4 mr-2" />
            Initialize
          </Button>
        </div>
      </div>

      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid grid-cols-4 w-full max-w-2xl">
          <TabsTrigger value="overview">
            <BarChart3 className="h-4 w-4 mr-2" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="export">
            <Upload className="h-4 w-4 mr-2" />
            Export
          </TabsTrigger>
          <TabsTrigger value="search">
            <Search className="h-4 w-4 mr-2" />
            Search
          </TabsTrigger>
          <TabsTrigger value="maintenance">
            <Settings className="h-4 w-4 mr-2" />
            Maintenance
          </TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview" className="mt-6">
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-3 bg-blue-100 rounded-full mr-4">
                      <Database className="w-6 h-6 text-blue-600" />
                    </div>
                    <div>
                      <p className="text-gray-500 text-sm font-medium">Total Entries</p>
                      <p className="text-2xl font-bold">{formatNumber(stats.totalEntries)}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-3 bg-green-100 rounded-full mr-4">
                      <HardDrive className="w-6 h-6 text-green-600" />
                    </div>
                    <div>
                      <p className="text-gray-500 text-sm font-medium">Total Size</p>
                      <p className="text-2xl font-bold">{formatFileSize(stats.totalFileSize)}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-3 bg-purple-100 rounded-full mr-4">
                      <Layers className="w-6 h-6 text-purple-600" />
                    </div>
                    <div>
                      <p className="text-gray-500 text-sm font-medium">Total Triples</p>
                      <p className="text-2xl font-bold">{formatNumber(stats.totalTriples)}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="p-6">
                  <div className="flex items-center">
                    <div className="p-3 bg-yellow-100 rounded-full mr-4">
                      <Brain className="w-6 h-6 text-yellow-600" />
                    </div>
                    <div>
                      <p className="text-gray-500 text-sm font-medium">Cache Size</p>
                      <p className="text-2xl font-bold">{stats.cacheSize}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Format Distribution */}
          {stats && stats.formatDistribution && Object.keys(stats.formatDistribution).length > 0 && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Format Distribution</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {Object.entries(stats.formatDistribution).map(([format, count]) => (
                    <div key={format} className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-blue-600">{count}</div>
                      <div className="text-sm text-gray-600">{format}</div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* System Status */}
          <Card>
            <CardHeader>
              <CardTitle>System Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">Knowledge Base Status</span>
                  <Badge variant="default" className="bg-green-100 text-green-800">
                    <CheckCircle className="w-3 h-3 mr-1" />
                    Active
                  </Badge>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">Last Master Update</span>
                  <span className="text-sm text-gray-600">
                    {stats?.lastMasterUpdate ? new Date(stats.lastMasterUpdate).toLocaleString() : 'Never'}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium">Cache Performance</span>
                  <Badge variant="outline" className="text-blue-600">
                    <Zap className="w-3 h-3 mr-1" />
                    Optimized
                  </Badge>
                </div>
                {stats?.hydraulicCylinderCount !== undefined && (
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Hydraulic Cylinders</span>
                    <span className="text-sm text-gray-600">
                      {stats.hydraulicCylinderCount} ({stats.hydraulicCylinderPercentage?.toFixed(1)}%)
                    </span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Export Tab */}
        <TabsContent value="export" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Single Export */}
            <Card>
              <CardHeader>
                <CardTitle>Export Single Item</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="masterItemCode">Master Item Code</Label>
                    <Input
                      id="masterItemCode"
                      value={singleExportForm.masterItemCode}
                      onChange={(e) => setSingleExportForm(prev => ({
                        ...prev,
                        masterItemCode: e.target.value
                      }))}
                      placeholder="Enter item code"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="format">Export Format</Label>
                    <Select
                      value={singleExportForm.format}
                      onValueChange={(value) => setSingleExportForm(prev => ({
                        ...prev,
                        format: value
                      }))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="RDF/XML">OWL/RDF (XML)</SelectItem>
                        <SelectItem value="JSON-LD">JSON-LD</SelectItem>
                        <SelectItem value="TURTLE">Turtle</SelectItem>
                        <SelectItem value="N-TRIPLES">N-Triples</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <Switch
                      id="includeHierarchy"
                      checked={singleExportForm.includeHierarchy}
                      onCheckedChange={(checked) => setSingleExportForm(prev => ({
                        ...prev,
                        includeHierarchy: checked
                      }))} 
                    />
                    <Label htmlFor="includeHierarchy">Include Complete Hierarchy</Label>
                  </div>
                  
                  <div>
                    <Label htmlFor="description">Description</Label>
                    <Input
                      id="description"
                      value={singleExportForm.description}
                      onChange={(e) => setSingleExportForm(prev => ({
                        ...prev,
                        description: e.target.value
                      }))}
                      placeholder="Optional description"
                    />
                  </div>
                  
                  <Button 
                    onClick={handleSingleExport} 
                    disabled={exportLoading}
                    className="w-full"
                    variant={'outline'}
                  >
                    {exportLoading ? (
                      <>
                        <Loader2  className="mr-2 h-4 w-4 animate-spin" />
                        Exporting...
                      </>
                    ) : (
                      <>
                        <Upload className="mr-2 h-4 w-4" />
                        Export to Knowledge Base
                      </>
                    )}
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Enhanced Batch Export */}
            <Card>
              <CardHeader>
                <CardTitle>Batch Export All BOMs</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <Alert>
                    <AlertTitle>Batch Export</AlertTitle>
                    <AlertDescription>
                      Export all BOMs from the ERP system to the knowledge base.
                      This process supports pause/resume functionality.
                    </AlertDescription>
                  </Alert>
                  
                  {/* Batch Progress */}
                  {(exportLoading || batchStatus === 'processing' || batchStatus === 'paused') && batchProgress && (
                    <div className="space-y-4">
                      <div className="flex justify-between text-sm">
                        <span>Export Progress</span>
                        <span>{batchProgress.progressPercentage?.toFixed(1)}%</span>
                      </div>
                      <Progress value={exportProgress} className="w-full" />
                      
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <span className="text-gray-500">Processed:</span>
                          <span className="ml-2 font-medium">
                            {batchProgress.processedItems} / {batchProgress.totalItems}
                          </span>
                        </div>
                        <div>
                          <span className="text-gray-500">Success Rate:</span>
                          <span className="ml-2 font-medium text-green-600">
                            {batchProgress.processedItems > 0 
                              ? ((batchProgress.successCount / batchProgress.processedItems) * 100).toFixed(1)
                              : 0}%
                          </span>
                        </div>
                        <div>
                          <span className="text-gray-500">Failed:</span>
                          <span className="ml-2 font-medium text-red-600">
                            {batchProgress.failureCount}
                          </span>
                        </div>
                        <div>
                          <span className="text-gray-500">Est. Time:</span>
                          <span className="ml-2 font-medium">
                            {formatDuration(batchProgress.estimatedRemainingSeconds)}
                          </span>
                        </div>
                      </div>
                      
                      {/* Batch Control Buttons */}
                      <div className="flex gap-2">
                        {batchStatus === 'processing' && (
                          <Button onClick={handlePauseBatch} variant="outline" size="sm">
                            <Pause className="h-4 w-4 mr-2" />
                            Pause
                          </Button>
                        )}
                        {batchStatus === 'paused' && (
                          <Button onClick={handleResumeBatch} variant="outline" size="sm">
                            <Play className="h-4 w-4 mr-2" />
                            Resume
                          </Button>
                        )}
                        <Button onClick={handleCancelBatch} variant="destructive" size="sm">
                          <X className="h-4 w-4 mr-2" />
                          Cancel
                        </Button>
                      </div>
                    </div>
                  )}
                  
                  {/* Legacy batch result display */}
                  {batchResult && !batchId && (
                    <div className="space-y-2">
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div className="flex justify-between">
                          <span>Total Items:</span>
                          <span className="font-medium">{batchResult.totalItems}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Successful:</span>
                          <span className="font-medium text-green-600">{batchResult.successCount}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Failed:</span>
                          <span className="font-medium text-red-600">{batchResult.failureCount}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Master File:</span>
                          <span className="font-medium text-blue-600">{batchResult.masterKnowledgeBaseFile}</span>
                        </div>
                      </div>
                    </div>
                  )}
                  
                  {/* Start Button */}
                  {batchStatus === 'idle' && (
                    <Button 
                      onClick={handleBatchExport} 
                      disabled={exportLoading}
                      className="w-full"
                      variant="secondary"
                    >
                      {exportLoading ? (
                        <>
                          <Loader2  className="mr-2 h-4 w-4 animate-spin" />
                          Starting Batch Export...
                        </>
                      ) : (
                        <>
                          <Database className="mr-2 h-4 w-4" />
                          Start Batch Export
                        </>
                      )}
                    </Button>
                  )}
                  
                  {/* Batch History */}
                  <div className="border-t pt-4">
                    <div className="flex justify-between items-center mb-2">
                      <h4 className="text-sm font-medium">Recent Batch Operations</h4>
                      <Button 
                        size="sm" 
                        variant="ghost" 
                        onClick={loadBatchHistory}
                      >
                        <RefreshCw className="h-3 w-3" />
                      </Button>
                    </div>
                    
                    {batchHistory.length > 0 && (
                      <div className="space-y-2">
                        {batchHistory.slice(0, 3).map(log => (
                          <div key={log.batchId} className="text-xs p-2 bg-gray-50 rounded">
                            <div className="flex justify-between">
                              <span className="font-medium">{log.batchId.substring(0, 8)}...</span>
                              <Badge variant={getStatusVariant(log.status)} className="text-xs">
                                {log.status}
                              </Badge>
                            </div>
                            <div className="flex justify-between mt-1 text-gray-500">
                              <span>{new Date(log.startTime).toLocaleString()}</span>
                              <span>{log.successCount}/{log.totalItems} succeeded</span>
                            </div>
                            {log.status === 'PAUSED' && (
                              <Button 
                                size="sm" 
                                variant="link" 
                                className="mt-1 p-0 h-auto text-xs"
                                onClick={() => handleResumePreviousBatch(log.batchId)}
                              >
                                Resume this batch
                              </Button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Search Tab */}
        <TabsContent value="search" className="mt-6">
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Search Knowledge Base</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex gap-2">
                <Input
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  placeholder="Search by item code, description, or tags..."
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                />
                <Button onClick={handleSearch} disabled={searchLoading}>
                  {searchLoading ? (
                    <Loader2  className="h-4 w-4 animate-spin" />
                  ) : (
                    <Search className="h-4 w-4" />
                  )}
                </Button>
              </div>
              
              {searchResults.length > 0 && (
                <div className="mt-4">
                  <p className="text-sm text-gray-600 mb-4">
                    Found {searchResults.length} result(s)
                  </p>
                  
                  <div className="space-y-4">
                    {searchResults.map((entry) => (
                      <Card key={entry.id} className="border-l-4 border-l-blue-500">
                        <CardContent className="p-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <h4 className="font-medium">{entry.masterItemCode}</h4>
                              <p className="text-sm text-gray-600">{entry.description}</p>
                            </div>
                            <div className="flex gap-2">
                              <Badge variant={entry.active ? "default" : "secondary"}>
                                {entry.active ? "Active" : "Inactive"}
                              </Badge>
                              <Badge variant="outline">{entry.format}</Badge>
                            </div>
                          </div>
                          
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                            <div>
                              <span className="text-gray-500">File Size:</span>
                              <div className="font-medium">{formatFileSize(entry.fileSize)}</div>
                            </div>
                            <div>
                              <span className="text-gray-500">Triples:</span>
                              <div className="font-medium">{formatNumber(entry.tripleCount)}</div>
                            </div>
                            <div>
                              <span className="text-gray-500">Usage:</span>
                              <div className="font-medium">{entry.usageCount} times</div>
                            </div>
                            <div>
                              <span className="text-gray-500">Created:</span>
                              <div className="font-medium">
                                {new Date(entry.createdAt).toLocaleDateString()}
                              </div>
                            </div>
                          </div>
                          
                          <div className="flex justify-between items-center mt-3">
                            <div className="flex items-center text-sm text-gray-500">
                              <Clock className="w-4 h-4 mr-1" />
                              {entry.includeHierarchy ? "Complete Hierarchy" : "Direct Components Only"}
                            </div>
                            <div className="flex gap-2">
                              <Button 
                                size="sm" 
                                variant="outline"
                                onClick={() => handleDownload(entry.masterItemCode, entry.format)}
                              >
                                <Download className="w-4 h-4 mr-1" />
                                Download
                              </Button>
                              <Button size="sm" variant="outline">
                                <FileText className="w-4 h-4 mr-1" />
                                View Details
                              </Button>
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Maintenance Tab */}
        <TabsContent value="maintenance" className="mt-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Cleanup Operations */}
            <Card>
              <CardHeader>
                <CardTitle>Cleanup Operations</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <Alert>
                    <AlertTitle>Cleanup Knowledge Base</AlertTitle>
                    <AlertDescription>
                      Remove invalid entries, corrupted files, and optimize the knowledge base.
                    </AlertDescription>
                  </Alert>
                  
                  <Button 
                    onClick={handleCleanup} 
                    disabled={loading}
                    variant="destructive"
                    className="w-full"
                  >
                    {loading ? (
                      <>
                        <Loader2  className="mr-2 h-4 w-4 animate-spin" />
                        Cleaning up...
                      </>
                    ) : (
                      <>
                        <Trash2 className="mr-2 h-4 w-4" />
                        Start Cleanup
                      </>
                    )}
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* System Health */}
            <Card>
              <CardHeader>
                <CardTitle>System Health</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Storage Usage</span>
                    <span className="text-sm text-gray-600">
                      {stats ? formatFileSize(stats.totalFileSize) : 'Loading...'}
                    </span>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Cache Hit Rate</span>
                    <Badge variant="outline" className="text-green-600">
                      <CheckCircle className="w-3 h-3 mr-1" />
                      95%
                    </Badge>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Data Integrity</span>
                    <Badge variant="outline" className="text-green-600">
                      <CheckCircle className="w-3 h-3 mr-1" />
                      Verified
                    </Badge>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Background Tasks</span>
                    <Badge variant="outline" className="text-blue-600">
                      <Clock className="w-3 h-3 mr-1" />
                      Active
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Advanced Settings */}
          <Card className="mt-6">
            <CardHeader>
              <CardTitle>Advanced Settings</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <h4 className="font-medium">Cache Management</h4>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Auto-refresh cache</span>
                    <Switch defaultChecked />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Preload frequently used models</span>
                    <Switch defaultChecked />
                  </div>
                </div>
                
                <div className="space-y-4">
                  <h4 className="font-medium">Storage Management</h4>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Auto-cleanup inactive entries</span>
                    <Switch />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Compress older files</span>
                    <Switch />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default KnowledgeBaseManagement;