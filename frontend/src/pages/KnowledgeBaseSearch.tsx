// src/pages/KnowledgeBaseSearch.tsx
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { toast } from 'sonner';

// Icons
import {
  Search,
  Filter,
  Clock,
  Zap,
  Database,
  Brain,
  ChevronRight,
  Loader2,
  CheckCircle,
  XCircle,
  AlertCircle,
  BarChart3,
  Trash2,
  Layers,
  Info,
  TrendingUp,
  ShieldCheck,
  RefreshCw,
  X
} from 'lucide-react';

// Import search service
import knowledgeBaseSearchService, {
  SearchType,
  SortOrder,
  SimilarBOMDTO,
  SearchProgressDTO,
  ProcessingPhase,
  CacheStatsDTO,
  SearchRetryOptions
} from '@/services/knowledgeBaseSearchService';

// Utility functions
const formatFileSize = (bytes: number): string => {
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  if (bytes === 0) return '0 Bytes';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
};

const formatDuration = (ms: number): string => {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`;
};

// Component for displaying search complexity
const SearchComplexityIndicator = ({ 
  specifications 
}: { 
  specifications: Record<string, string> 
}) => {
  const complexity = knowledgeBaseSearchService.estimateSearchComplexity(specifications);
  
  const getComplexityColor = () => {
    switch (complexity.complexity) {
      case 'low': return 'text-green-600';
      case 'medium': return 'text-yellow-600';
      case 'high': return 'text-orange-600';
      case 'very-high': return 'text-red-600';
    }
  };
  
  const getComplexityIcon = () => {
    switch (complexity.complexity) {
      case 'low': return <CheckCircle className="w-4 h-4" />;
      case 'medium': return <AlertCircle className="w-4 h-4" />;
      case 'high': return <TrendingUp className="w-4 h-4" />;
      case 'very-high': return <ShieldCheck className="w-4 h-4" />;
    }
  };
  
  return (
    <Alert className="mb-4">
      {/* 使用 AlertTitle 來包裹標題，確保與 AlertDescription 有適當間距 */}
      <AlertTitle>
        <div className={`flex items-center gap-2 ${getComplexityColor()}`}>
          {getComplexityIcon()}
          <span className="font-medium">
            Search Complexity: {complexity.complexity.toUpperCase()}
          </span>
        </div>
      </AlertTitle>
      
      <AlertDescription className="mt-3 space-y-2">
        {/* 使用 flex 或 grid 來組織估計時間和建議方式 */}
        <div className="space-y-1">
          <p className="text-sm text-gray-700">
            <span className="font-medium">Estimated time:</span> {complexity.estimatedTime}s
          </p>
          <p className="text-sm text-gray-700">
            <span className="font-medium">Recommended:</span> {complexity.recommendedApproach}
          </p>
        </div>
        
        {/* 建議列表 */}
        {complexity.recommendations.length > 0 && (
          <div className="mt-3 pt-2 border-t border-gray-200">
            <p className="text-sm font-medium text-gray-700 mb-1">Optimization Tips:</p>
            <ul className="space-y-1">
              {complexity.recommendations.map((rec, idx) => (
                <li key={idx} className="text-sm text-gray-600 flex items-start">
                  <span className="mr-2">•</span>
                  <span>{rec}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </AlertDescription>
    </Alert>
  );
};

// Component for displaying search results
const SearchResultCard = ({ 
  result, 
  onViewDetails 
}: { 
  result: SimilarBOMDTO; 
  onViewDetails: () => void;
}) => {
  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-start">
          <div>
            <CardTitle className="text-lg">{result.masterItemCode}</CardTitle>
            <CardDescription className="mt-1">
              {result.description || 'No description available'}
            </CardDescription>
          </div>
          <Badge 
            variant={result.similarityScore >= 0.9 ? "default" : 
                    result.similarityScore >= 0.7 ? "secondary" : "outline"}
          >
            {(result.similarityScore ).toFixed(1)}% Match
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm mb-4">
          <div>
            <span className="text-gray-500">Format:</span>
            <div className="font-medium">{result.format}</div>
          </div>
          <div>
            <span className="text-gray-500">Size:</span>
            <div className="font-medium">{formatFileSize(result.fileSize)}</div>
          </div>
          <div>
            <span className="text-gray-500">Triples:</span>
            <div className="font-medium">{result.tripleCount.toLocaleString()}</div>
          </div>
          <div>
            <span className="text-gray-500">Created:</span>
            <div className="font-medium">
              {new Date(result.createdAt).toLocaleDateString()}
            </div>
          </div>
        </div>
        
        {/* Hydraulic cylinder specs if available */}
        {result.isHydraulicCylinder && result.parsedSpecs && (
          <div className="mb-3 p-2 bg-blue-50 rounded text-sm">
            <span className="font-medium text-blue-700">Hydraulic Specs: </span>
            <span className="text-blue-600">
              Series {result.parsedSpecs.series}, 
              Bore {result.parsedSpecs.bore}, 
              Stroke {result.parsedSpecs.stroke}
            </span>
          </div>
        )}
        
        {/* Additional info for new fields */}
        {(result.qualityScore !== undefined || result.validationStatus || result.sourceSystem) && (
          <div className="flex gap-2 mb-3">
            {result.qualityScore !== undefined && (
              <Badge variant="outline" className="text-xs">
                Quality: {(result.qualityScore * 100).toFixed(0)}%
              </Badge>
            )}
            {result.validationStatus && (
              <Badge 
                variant={result.validationStatus === 'VALIDATED' ? 'default' : 'secondary'}
                className="text-xs"
              >
                {result.validationStatus}
              </Badge>
            )}
            {result.sourceSystem && (
              <Badge variant="outline" className="text-xs">
                {result.sourceSystem}
              </Badge>
            )}
          </div>
        )}
        
        <div className="flex justify-end">
          <Button size="sm" variant="outline" onClick={onViewDetails}>
            View Details
            <ChevronRight className="w-4 h-4 ml-1" />
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

// Main search page component
const KnowledgeBaseSearch = () => {
  const navigate = useNavigate();
  
  // Search states
  const [activeTab, setActiveTab] = useState('simple');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchAttempt, setSearchAttempt] = useState(0);
  const [useAsyncSearch, setUseAsyncSearch] = useState(false);
  const [serviceHealthy, setServiceHealthy] = useState(true);
  
  // Search form states
  const [specifications, setSpecifications] = useState<Record<string, string>>({
    series: '',
    type: '',
    bore: '',
    stroke: '',
    rodEndType: ''
  });
  
  // Search options
  const [searchOptions, setSearchOptions] = useState({
    maxResults: 10,
    minSimilarityScore: 0.7,
    timeoutSeconds: 30,
    useCache: true,
    onlyHydraulicCylinders: false,
    includeGeneratedBOMs: true,
    onlyValidated: false,
    sortOrder: SortOrder.SIMILARITY_DESC
  });
  
  const [searchType, setSearchType] = useState<SearchType>(SearchType.SIMILARITY);
  
  // Search results
  const [searchResults, setSearchResults] = useState<any>(null);
  const [searchProgress, setSearchProgress] = useState<SearchProgressDTO | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  
  // Batch search states
  const [batchSearchItems, setBatchSearchItems] = useState<string>('');
  const [batchResults, setBatchResults] = useState<any>(null);
  
  // Cache stats
  const [cacheStats, setCacheStats] = useState<CacheStatsDTO | null>(null);
  
  // Retry options
  const [retryOptions, setRetryOptions] = useState<SearchRetryOptions>({
    maxRetries: 2,
    progressiveTimeout: true,
    fallbackToAsync: true,
    reduceScope: true
  });
  
  // Check service health on mount
  useEffect(() => {
    checkServiceHealth();
    loadCacheStats();
  }, []);
  
  const checkServiceHealth = async () => {
    try {
      const healthy = await knowledgeBaseSearchService.healthCheck();
      setServiceHealthy(healthy);
      if (!healthy) {
        toast.error('Search service is currently unavailable');
      }
    } catch (err) {
      console.error('Health check failed:', err);
      setServiceHealthy(false);
    }
  };
  
  const loadCacheStats = async () => {
    try {
      const stats = await knowledgeBaseSearchService.getCacheStats();
      setCacheStats(stats);
    } catch (err) {
      console.error('Failed to load cache stats:', err);
    }
  };
  
  // Cancel active searches on unmount
  useEffect(() => {
    return () => {
      knowledgeBaseSearchService.cancelAllSearches();
    };
  }, []);
  
  // Handle search with enhanced error handling and retry
  const handleSearch = async (forceAsync = false) => {
    if (Object.values(specifications).every(v => !v.trim())) {
      toast.error('Please enter at least one search criterion');
      return;
    }
    
    setLoading(true);
    setError(null);
    setSearchResults(null);
    setSearchAttempt(prev => prev + 1);
    
    try {
      const request = knowledgeBaseSearchService.buildSearchRequest(
        specifications,
        searchOptions,
        searchType
      );
      
      // Check complexity and warn user
      const complexity = knowledgeBaseSearchService.estimateSearchComplexity(specifications);
      if (complexity.complexity === 'very-high' && !forceAsync) {
        toast.warning('This search may take longer. Consider using async search.');
      }
      
      let results;
      
      if (forceAsync || useAsyncSearch) {
        results = await knowledgeBaseSearchService.searchSimilarAsync(request);
        
        if (results.searchId && results.status === 'PROCESSING') {
          toast.info('Async search started, tracking progress...');
          setIsPolling(true);
          
          results = await knowledgeBaseSearchService.pollSearchResults(
            results.searchId,
            (progress) => setSearchProgress(progress),
            1000,
            60
          );
          
          setIsPolling(false);
        }
      } else {
        // Try with retry strategy
        results = await knowledgeBaseSearchService.searchSimilarWithRetry(
          request,
          retryOptions
        );
      }
      
      setSearchResults(results);
      setSearchProgress(null);
      
      if (results.totalResults === 0) {
        toast.info('No matching items found. Try adjusting your criteria.');
      } else {
        toast.success(`Found ${results.totalResults} matching items in ${formatDuration(results.durationMs)}`);
      }
      
      // Log search statistics
      if (results.results) {
        const stats = knowledgeBaseSearchService.calculateSearchStats(results.results);
        console.log('Search statistics:', stats);
      }
      
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Search failed';
      setError(message);
      
      if (message.includes('timeout')) {
        toast.error(
          <div className="space-y-2">
            <p>{message}</p>
            <div className="flex gap-2 mt-2">
              <Button 
                size="sm" 
                variant="secondary"
                onClick={() => {
                  setUseAsyncSearch(true);
                  handleSearch(true);
                }}
              >
                Try Async Search
              </Button>
              <Button 
                size="sm" 
                variant="outline"
                onClick={() => {
                  setSearchOptions(prev => ({
                    ...prev,
                    maxResults: Math.max(5, Math.floor(prev.maxResults / 2)),
                    minSimilarityScore: Math.min(0.9, prev.minSimilarityScore + 0.1)
                  }));
                  toast.info('Search scope reduced. Try again.');
                }}
              >
                Reduce Scope
              </Button>
            </div>
          </div>,
          { duration: 8000 }
        );
      } else {
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  };
  
  // Handle asynchronous search
  const handleAsyncSearch = async () => {
    await handleSearch(true);
  };
  
  // Handle batch search
  const handleBatchSearch = async () => {
    const items = batchSearchItems.split('\n').filter(item => item.trim());
    
    if (items.length === 0) {
      toast.error('Please enter at least one item code');
      return;
    }
    
    if (items.length > 50) {
      toast.error('Batch size cannot exceed 50 items');
      return;
    }
    
    setLoading(true);
    setError(null);
    setBatchResults(null);
    
    try {
      const searchItems = items.map(itemCode => ({
        itemId: itemCode.trim(),
        specifications: { masterItemCode: itemCode.trim() }
      }));
      
      const request = {
        searchItems,
        commonOptions: searchOptions,
        parallel: true,
        continueOnError: true
      };
      
      const results = await knowledgeBaseSearchService.searchBatch(request);
      setBatchResults(results);
      
      const successRate = (results.summary.successfulSearches / results.summary.totalSearches * 100).toFixed(1);
      
      toast.success(
        `Batch search completed: ${results.summary.successfulSearches}/${results.summary.totalSearches} successful (${successRate}%)`
      );
      
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Batch search failed';
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };
  
  const cancelSearch = useCallback(() => {
    knowledgeBaseSearchService.cancelSearch('search-similar');
    setLoading(false);
    setIsPolling(false);
    toast.info('Search cancelled');
  }, []);
  
  const clearCache = async () => {
    try {
      await knowledgeBaseSearchService.clearCache();
      toast.success('Cache cleared successfully');
      await loadCacheStats();
    } catch (err) {
      toast.error('Failed to clear cache');
    }
  };
  
  const handleViewDetails = (masterItemCode: string) => {
    navigate(`/items/view/${masterItemCode}`);
  };
  
  const getPhaseIcon = (phase: ProcessingPhase) => {
    switch (phase) {
      case ProcessingPhase.INITIALIZING:
        return <Clock className="w-4 h-4" />;
      case ProcessingPhase.FILTERING:
        return <Filter className="w-4 h-4" />;
      case ProcessingPhase.CALCULATING:
        return <Brain className="w-4 h-4" />;
      case ProcessingPhase.SORTING:
        return <BarChart3 className="w-4 h-4" />;
      case ProcessingPhase.FINALIZING:
        return <CheckCircle className="w-4 h-4" />;
      default:
        return <Info className="w-4 h-4" />;
    }
  };
  
  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">Knowledge Base Search</h1>
          <p className="text-gray-600">Advanced search with caching and async processing</p>
        </div>
        <div className="flex gap-2 items-center">
          {!serviceHealthy && (
            <Badge variant="destructive">
              <XCircle className="w-3 h-3 mr-1" />
              Service Unavailable
            </Badge>
          )}
          <Button 
            variant="outline" 
            size="sm"
            onClick={checkServiceHealth}
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
          <Button variant="outline" onClick={() => navigate('/knowledge-base')}>
            <Database className="w-4 h-4 mr-2" />
            Manage KB
          </Button>
        </div>
      </div>
      
      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid grid-cols-4 w-full max-w-2xl">
          <TabsTrigger value="simple">
            <Search className="h-4 w-4 mr-2" />
            Simple Search
          </TabsTrigger>
          <TabsTrigger value="advanced">
            <Brain className="h-4 w-4 mr-2" />
            Advanced
          </TabsTrigger>
          <TabsTrigger value="batch">
            <Layers className="h-4 w-4 mr-2" />
            Batch Search
          </TabsTrigger>
          <TabsTrigger value="cache">
            <Zap className="h-4 w-4 mr-2" />
            Cache Stats
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="simple" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-1">
              <CardHeader>
                <CardTitle>Search Criteria</CardTitle>
                <CardDescription>
                  Enter hydraulic cylinder specifications
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {/* Show complexity indicator */}
                  {Object.values(specifications).some(v => v.trim()) && (
                    <SearchComplexityIndicator specifications={specifications} />
                  )}
                  
                  <div>
                    <Label htmlFor="series">Series</Label>
                    <Input
                      id="series"
                      value={specifications.series}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        series: e.target.value
                      }))}
                      placeholder="e.g., 21, 22, 23"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="type">Type</Label>
                    <Input
                      id="type"
                      value={specifications.type}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        type: e.target.value
                      }))}
                      placeholder="e.g., A, C, D"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="bore">Bore Size</Label>
                    <Input
                      id="bore"
                      value={specifications.bore}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        bore: e.target.value
                      }))}
                      placeholder="e.g., 050, 063, 080"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="stroke">Stroke Length</Label>
                    <Input
                      id="stroke"
                      value={specifications.stroke}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        stroke: e.target.value
                      }))}
                      placeholder="e.g., 0100, 0150, 0200"
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="rodEndType">Rod End Type</Label>
                    <Input
                      id="rodEndType"
                      value={specifications.rodEndType}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        rodEndType: e.target.value
                      }))}
                      placeholder="e.g., Y, I, E"
                    />
                  </div>
                  
                  <Separator />
                  
                  <div className="space-y-2">
                    <Label>Max Results</Label>
                    <div className="flex items-center gap-4">
                      <Slider
                        value={[searchOptions.maxResults]}
                        onValueChange={([value]) => setSearchOptions(prev => ({
                          ...prev,
                          maxResults: value
                        }))}
                        min={5}
                        max={50}
                        step={5}
                        className="flex-1"
                      />
                      <span className="w-12 text-right font-medium">
                        {searchOptions.maxResults}
                      </span>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label>Min Similarity Score</Label>
                    <div className="flex items-center gap-4">
                      <Slider
                        value={[searchOptions.minSimilarityScore * 100]}
                        onValueChange={([value]) => setSearchOptions(prev => ({
                          ...prev,
                          minSimilarityScore: value / 100
                        }))}
                        min={50}
                        max={100}
                        step={5}
                        className="flex-1"
                      />
                      <span className="w-12 text-right font-medium">
                        {(searchOptions.minSimilarityScore * 100).toFixed(0)}%
                      </span>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label>Timeout (seconds)</Label>
                    <Select 
                      value={searchOptions.timeoutSeconds.toString()} 
                      onValueChange={(v) => setSearchOptions(prev => ({
                        ...prev,
                        timeoutSeconds: parseInt(v)
                      }))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="15">15 seconds (Fast)</SelectItem>
                        <SelectItem value="30">30 seconds (Normal)</SelectItem>
                        <SelectItem value="60">60 seconds (Thorough)</SelectItem>
                        <SelectItem value="120">120 seconds (Complete)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <Label htmlFor="useCache">Use Cache</Label>
                    <Switch
                      id="useCache"
                      checked={searchOptions.useCache}
                      onCheckedChange={(checked) => setSearchOptions(prev => ({
                        ...prev,
                        useCache: checked
                      }))}
                    />
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <Label htmlFor="onlyHydraulic">Only Hydraulic Cylinders</Label>
                    <Switch
                      id="onlyHydraulic"
                      checked={searchOptions.onlyHydraulicCylinders}
                      onCheckedChange={(checked) => setSearchOptions(prev => ({
                        ...prev,
                        onlyHydraulicCylinders: checked
                      }))}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Button 
                      onClick={() => handleSearch(false)} 
                      disabled={loading || !serviceHealthy}
                      className="w-full"
                    >
                      {loading ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Searching...
                        </>
                      ) : (
                        <>
                          <Search className="mr-2 h-4 w-4" />
                          Search
                        </>
                      )}
                    </Button>
                    
                    {loading && (
                      <Button 
                        onClick={cancelSearch}
                        variant="outline"
                        className="w-full"
                      >
                        <X className="mr-2 h-4 w-4" />
                        Cancel Search
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
            
            <div className="lg:col-span-2">
              {searchProgress && isPolling && (
                <Card className="mb-4">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      {getPhaseIcon(searchProgress.currentPhase)}
                      Search Progress
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div>
                        <div className="flex justify-between text-sm mb-2">
                          <span>Phase: {searchProgress.currentPhase}</span>
                          <span>{searchProgress.percentComplete.toFixed(1)}%</span>
                        </div>
                        <Progress value={searchProgress.percentComplete} />
                      </div>
                      
                      <div className="grid grid-cols-3 gap-4 text-sm">
                        <div>
                          <span className="text-gray-500">Processed:</span>
                          <div className="font-medium">
                            {searchProgress.processedItems} / {searchProgress.totalItems}
                          </div>
                        </div>
                        <div>
                          <span className="text-gray-500">Matches:</span>
                          <div className="font-medium">{searchProgress.foundMatches}</div>
                        </div>
                        <div>
                          <span className="text-gray-500">Elapsed:</span>
                          <div className="font-medium">
                            {formatDuration(searchProgress.elapsedTimeMs)}
                          </div>
                        </div>
                      </div>
                      
                      {searchProgress.estimatedRemainingMs && (
                        <div className="text-sm text-gray-600">
                          Estimated remaining: {formatDuration(searchProgress.estimatedRemainingMs)}
                        </div>
                      )}
                      
                      {searchProgress.warningMessage && (
                        <Alert>
                          <AlertCircle className="h-4 w-4" />
                          <AlertDescription>{searchProgress.warningMessage}</AlertDescription>
                        </Alert>
                      )}
                    </div>
                  </CardContent>
                </Card>
              )}
              
              {searchResults && searchResults.results && searchResults.results.length > 0 && (
                <>
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-semibold">
                      Search Results ({searchResults.totalResults})
                    </h3>
                    <div className="flex items-center gap-2">
                      {searchResults.durationMs > 0 && (
                        <Badge variant="outline">
                          <Clock className="w-3 h-3 mr-1" />
                          {formatDuration(searchResults.durationMs)}
                        </Badge>
                      )}
                      {searchResults.timeoutCount > 0 && (
                        <Badge variant="secondary">
                          {searchResults.timeoutCount} timeouts
                        </Badge>
                      )}
                      {searchAttempt > 1 && (
                        <Badge variant="outline">
                          Attempt {searchAttempt}
                        </Badge>
                      )}
                    </div>
                  </div>
                  
                  {/* Search statistics */}
                  {searchResults.results.length > 0 && (
                    <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                      <div className="grid grid-cols-2 md:grid-cols-5 gap-3 text-sm">
                        <div>
                          <span className="text-gray-500">Avg Similarity:</span>
                          <div className="font-medium">
                            {(knowledgeBaseSearchService.calculateSearchStats(searchResults.results).avgSimilarity ).toFixed(1)}%
                          </div>
                        </div>
                        <div>
                          <span className="text-gray-500">Hydraulic:</span>
                          <div className="font-medium">
                            {knowledgeBaseSearchService.calculateSearchStats(searchResults.results).hydraulicCylinderCount}
                          </div>
                        </div>
                        <div>
                          <span className="text-gray-500">Validated:</span>
                          <div className="font-medium">
                            {knowledgeBaseSearchService.calculateSearchStats(searchResults.results).validatedCount}
                          </div>
                        </div>
                        <div>
                          <span className="text-gray-500">AI Generated:</span>
                          <div className="font-medium">
                            {knowledgeBaseSearchService.calculateSearchStats(searchResults.results).aiGeneratedCount}
                          </div>
                        </div>
                        <div>
                          <span className="text-gray-500">Avg Quality:</span>
                          <div className="font-medium">
                            {(knowledgeBaseSearchService.calculateSearchStats(searchResults.results).avgQualityScore * 100).toFixed(0)}%
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                  
                  <div className="grid gap-4">
                    {searchResults.results.map((result: SimilarBOMDTO, index: number) => (
                      <SearchResultCard
                        key={index}
                        result={result}
                        onViewDetails={() => handleViewDetails(result.masterItemCode)}
                      />
                    ))}
                  </div>
                </>
              )}
              
              {searchResults && searchResults.totalResults === 0 && (
                <Card>
                  <CardContent className="text-center py-12">
                    <Info className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <h3 className="text-lg font-medium mb-2">No Results Found</h3>
                    <p className="text-gray-500 mb-4">
                      Try adjusting your search criteria or lowering the similarity threshold
                    </p>
                    <div className="flex gap-2 justify-center">
                      <Button 
                        variant="outline"
                        onClick={() => setSearchOptions(prev => ({
                          ...prev,
                          minSimilarityScore: Math.max(0.3, prev.minSimilarityScore - 0.1)
                        }))}
                      >
                        Lower Threshold
                      </Button>
                      <Button 
                        variant="outline"
                        onClick={() => setSpecifications({
                          series: '',
                          type: '',
                          bore: '',
                          stroke: '',
                          rodEndType: ''
                        })}
                      >
                        Clear Criteria
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </TabsContent>
        
        <TabsContent value="advanced" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Advanced Search Options</CardTitle>
              <CardDescription>
                Configure advanced search parameters for better results
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="searchType">Search Type</Label>
                    <Select
                      value={searchType}
                      onValueChange={(value) => setSearchType(value as SearchType)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={SearchType.SIMILARITY}>Similarity</SelectItem>
                        <SelectItem value={SearchType.EXACT}>Exact</SelectItem>
                        <SelectItem value={SearchType.FUZZY}>Fuzzy</SelectItem>
                        <SelectItem value={SearchType.SEMANTIC}>Semantic</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div>
                    <Label htmlFor="sortOrder">Sort Order</Label>
                    <Select
                      value={searchOptions.sortOrder}
                      onValueChange={(value) => setSearchOptions(prev => ({
                        ...prev,
                        sortOrder: value as SortOrder
                      }))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={SortOrder.SIMILARITY_DESC}>Similarity (High to Low)</SelectItem>
                        <SelectItem value={SortOrder.SIMILARITY_ASC}>Similarity (Low to High)</SelectItem>
                        <SelectItem value={SortOrder.CREATED_DESC}>Newest First</SelectItem>
                        <SelectItem value={SortOrder.CREATED_ASC}>Oldest First</SelectItem>
                        <SelectItem value={SortOrder.QUALITY_DESC}>Quality (High to Low)</SelectItem>
                        <SelectItem value={SortOrder.USAGE_DESC}>Most Used</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="space-y-2">
                    <Label>Additional Filters</Label>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="onlyValidated" className="text-sm font-normal">Only Validated BOMs</Label>
                        <Switch
                          id="onlyValidated"
                          checked={searchOptions.onlyValidated}
                          onCheckedChange={(checked) => setSearchOptions(prev => ({
                            ...prev,
                            onlyValidated: checked
                          }))}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <Label htmlFor="includeGenerated" className="text-sm font-normal">Include AI-Generated BOMs</Label>
                        <Switch
                          id="includeGenerated"
                          checked={searchOptions.includeGeneratedBOMs}
                          onCheckedChange={(checked) => setSearchOptions(prev => ({
                            ...prev,
                            includeGeneratedBOMs: checked
                          }))}
                        />
                      </div>
                    </div>
                  </div>
                  
                  <Separator />
                  
                  <div className="space-y-2">
                    <Label>Retry Options</Label>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="progressiveTimeout" className="text-sm font-normal">Progressive Timeout</Label>
                        <Switch
                          id="progressiveTimeout"
                          checked={retryOptions.progressiveTimeout}
                          onCheckedChange={(checked) => setRetryOptions(prev => ({
                            ...prev,
                            progressiveTimeout: checked
                          }))}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <Label htmlFor="fallbackToAsync" className="text-sm font-normal">Fallback to Async</Label>
                        <Switch
                          id="fallbackToAsync"
                          checked={retryOptions.fallbackToAsync}
                          onCheckedChange={(checked) => setRetryOptions(prev => ({
                            ...prev,
                            fallbackToAsync: checked
                          }))}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <Label htmlFor="reduceScope" className="text-sm font-normal">Auto-reduce Scope</Label>
                        <Switch
                          id="reduceScope"
                          checked={retryOptions.reduceScope}
                          onCheckedChange={(checked) => setRetryOptions(prev => ({
                            ...prev,
                            reduceScope: checked
                          }))}
                        />
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="space-y-4">
                  <Alert>
                    <Brain className="h-4 w-4" />
                    <AlertTitle>Async Search</AlertTitle>
                    <AlertDescription>
                      Use asynchronous search for large knowledge bases. This allows
                      tracking progress and handling timeouts gracefully.
                    </AlertDescription>
                  </Alert>
                  
                  <div className="flex items-center justify-between mb-4">
                    <Label htmlFor="asyncDefault">Use Async by Default</Label>
                    <Switch
                      id="asyncDefault"
                      checked={useAsyncSearch}
                      onCheckedChange={setUseAsyncSearch}
                    />
                  </div>
                  
                  <Button
                    onClick={handleAsyncSearch}
                    disabled={loading || !serviceHealthy}
                    variant="secondary"
                    className="w-full"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>
                        <Zap className="mr-2 h-4 w-4" />
                        Start Async Search
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="batch" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Batch Search</CardTitle>
              <CardDescription>
                Search for multiple items simultaneously (max 50 items)
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="batchItems">Item Codes (one per line)</Label>
                  <textarea
                    id="batchItems"
                    className="w-full min-h-[200px] p-3 border rounded-md font-mono text-sm"
                    value={batchSearchItems}
                    onChange={(e) => setBatchSearchItems(e.target.value)}
                    placeholder="Enter item codes, one per line..."
                  />
                  <p className="text-sm text-gray-500 mt-1">
                    {batchSearchItems.split('\n').filter(item => item.trim()).length} items entered
                  </p>
                </div>
                
                <Button
                  onClick={handleBatchSearch}
                  disabled={loading || !serviceHealthy}
                  className="w-full"
                >
                  {loading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Processing Batch...
                    </>
                  ) : (
                    <>
                      <Search className="mr-2 h-4 w-4" />
                      Start Batch Search
                    </>
                  )}
                </Button>
                
                {batchResults && (
                  <div className="mt-6">
                    <h3 className="text-lg font-semibold mb-4">Batch Results</h3>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                      <Card>
                        <CardContent className="p-4">
                          <div className="text-2xl font-bold text-blue-600">
                            {batchResults.summary.totalSearches}
                          </div>
                          <div className="text-sm text-gray-500">Total Searches</div>
                        </CardContent>
                      </Card>
                      
                      <Card>
                        <CardContent className="p-4">
                          <div className="text-2xl font-bold text-green-600">
                            {batchResults.summary.successfulSearches}
                          </div>
                          <div className="text-sm text-gray-500">Successful</div>
                        </CardContent>
                      </Card>
                      
                      <Card>
                        <CardContent className="p-4">
                          <div className="text-2xl font-bold text-red-600">
                            {batchResults.summary.failedSearches}
                          </div>
                          <div className="text-sm text-gray-500">Failed</div>
                        </CardContent>
                      </Card>
                      
                      <Card>
                        <CardContent className="p-4">
                          <div className="text-2xl font-bold text-purple-600">
                            {batchResults.summary.totalResultsFound}
                          </div>
                          <div className="text-sm text-gray-500">Results Found</div>
                        </CardContent>
                      </Card>
                    </div>
                    
                    <div className="space-y-4">
                      {batchResults.results.map((result: any, index: number) => (
                        <Card key={index} className={result.error ? 'border-red-200' : ''}>
                          <CardContent className="p-4">
                            <div className="flex justify-between items-center">
                              <div className="flex items-center gap-2">
                                {result.error ? (
                                  <XCircle className="w-5 h-5 text-red-500" />
                                ) : (
                                  <CheckCircle className="w-5 h-5 text-green-500" />
                                )}
                                <span className="font-medium">{result.itemId}</span>
                              </div>
                              <Badge variant="outline">
                                {formatDuration(result.processingTimeMs)}
                              </Badge>
                            </div>
                            
                            {result.error ? (
                              <p className="text-sm text-red-600 mt-2">{result.error}</p>
                            ) : result.searchResult && (
                              <p className="text-sm text-gray-600 mt-2">
                                Found {result.searchResult.totalResults} matches
                                {result.searchResult.results && result.searchResult.results[0] && (
                                  <span className="ml-2 text-gray-500">
                                    (Top match: {(result.searchResult.results[0].similarityScore).toFixed(1)}%)
                                  </span>
                                )}
                              </p>
                            )}
                          </CardContent>
                        </Card>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        
        <TabsContent value="cache" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Cache Statistics</CardTitle>
              <CardDescription>
                Monitor and manage search result caching
              </CardDescription>
            </CardHeader>
            <CardContent>
              {cacheStats ? (
                <div className="space-y-6">
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-green-600">
                        {cacheStats.statistics.hitRate.toFixed(1)}%
                      </div>
                      <div className="text-sm text-gray-600">Hit Rate</div>
                    </div>
                    
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-blue-600">
                        {cacheStats.statistics.hitCount.toLocaleString()}
                      </div>
                      <div className="text-sm text-gray-600">Cache Hits</div>
                    </div>
                    
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-yellow-600">
                        {cacheStats.statistics.missCount.toLocaleString()}
                      </div>
                      <div className="text-sm text-gray-600">Cache Misses</div>
                    </div>
                    
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-purple-600">
                        {formatDuration(cacheStats.statistics.averageLoadTime)}
                      </div>
                      <div className="text-sm text-gray-600">Avg Load Time</div>
                    </div>
                  </div>
                  
                  <div>
                    <h4 className="font-medium mb-2">Cache Sizes</h4>
                    <div className="space-y-2">
                      {Object.entries(cacheStats.sizes).map(([key, size]) => (
                        <div key={key} className="flex justify-between items-center p-2 bg-gray-50 rounded">
                          <span className="text-sm font-medium">{key}</span>
                          <Badge variant="outline">{size} entries</Badge>
                        </div>
                      ))}
                    </div>
                  </div>
                  
                  {cacheStats.statistics.totalCacheSize > 0 && (
                    <div className="text-sm text-gray-600">
                      Total cache size: {cacheStats.statistics.totalCacheSize.toLocaleString()} entries
                      {cacheStats.statistics.evictionCount > 0 && (
                        <span className="ml-2">
                          ({cacheStats.statistics.evictionCount} evictions)
                        </span>
                      )}
                    </div>
                  )}
                  
                  <div className="flex justify-end">
                    <Button onClick={clearCache} variant="destructive">
                      <Trash2 className="w-4 h-4 mr-2" />
                      Clear All Caches
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="text-center py-8">
                  <Loader2 className="w-8 h-8 animate-spin mx-auto text-gray-400" />
                  <p className="text-gray-500 mt-2">Loading cache statistics...</p>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default KnowledgeBaseSearch;