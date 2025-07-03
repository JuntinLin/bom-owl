// src/pages/KnowledgeBaseSearch.tsx
import { useState, useEffect } from 'react';
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
  Info
} from 'lucide-react';

// Import search service
import knowledgeBaseSearchService, {
  SearchType,
  SortOrder,
  SimilarBOMDTO,
  SearchProgressDTO,
  ProcessingPhase,
  CacheStatsDTO
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
            {(result.similarityScore * 100).toFixed(1)}% Match
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
  
  // Load cache stats on mount
  useEffect(() => {
    loadCacheStats();
  }, []);
  
  const loadCacheStats = async () => {
    try {
      const stats = await knowledgeBaseSearchService.getCacheStats();
      setCacheStats(stats);
    } catch (err) {
      console.error('Failed to load cache stats:', err);
    }
  };
  
  // Handle synchronous search
  const handleSearch = async () => {
    if (Object.values(specifications).every(v => !v.trim())) {
      toast.error('Please enter at least one search criterion');
      return;
    }
    
    setLoading(true);
    setError(null);
    setSearchResults(null);
    
    try {
      const request = knowledgeBaseSearchService.buildSearchRequest(
        specifications,
        searchOptions,
        searchType
      );
      
      const results = await knowledgeBaseSearchService.searchSimilar(request);
      setSearchResults(results);
      
      if (results.totalResults === 0) {
        toast.info('No matching items found');
      } else {
        toast.success(`Found ${results.totalResults} matching items`);
      }
      
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Search failed';
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };
  
  // Handle asynchronous search
  const handleAsyncSearch = async () => {
    if (Object.values(specifications).every(v => !v.trim())) {
      toast.error('Please enter at least one search criterion');
      return;
    }
    
    setLoading(true);
    setError(null);
    setSearchResults(null);
    setSearchProgress(null);
    setIsPolling(true);
    
    try {
      const request = knowledgeBaseSearchService.buildSearchRequest(
        specifications,
        searchOptions,
        searchType
      );
      
      const initialResult = await knowledgeBaseSearchService.searchSimilarAsync(request);
      
      if (initialResult.searchId && initialResult.status === 'PROCESSING') {
        toast.info('Search started, tracking progress...');
        
        // Poll for results
        const finalResults = await knowledgeBaseSearchService.pollSearchResults(
          initialResult.searchId,
          (progress) => setSearchProgress(progress),
          1000,
          60
        );
        
        setSearchResults(finalResults);
        toast.success('Search completed successfully');
      } else {
        // If search completed immediately
        setSearchResults(initialResult);
      }
      
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Async search failed';
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
      setIsPolling(false);
      setSearchProgress(null);
    }
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
      
      toast.success(`Batch search completed: ${results.summary.successfulSearches}/${results.summary.totalSearches} successful`);
      
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Batch search failed';
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };
  
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
        <div className="flex gap-2">
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
                  <div>
                    <Label htmlFor="series">Series</Label>
                    <Input
                      id="series"
                      value={specifications.series}
                      onChange={(e) => setSpecifications(prev => ({
                        ...prev,
                        series: e.target.value
                      }))}
                      placeholder="e.g., 10, 11, 12"
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
                      placeholder="e.g., F, D, S"
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
                  
                  <Button 
                    onClick={handleSearch} 
                    disabled={loading}
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
                    {searchResults.durationMs > 0 && (
                      <Badge variant="outline">
                        <Clock className="w-3 h-3 mr-1" />
                        {formatDuration(searchResults.durationMs)}
                      </Badge>
                    )}
                  </div>
                  
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
                    <p className="text-gray-500">
                      Try adjusting your search criteria or lowering the similarity threshold
                    </p>
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
                  
                  <div>
                    <Label htmlFor="timeout">Timeout (seconds)</Label>
                    <Input
                      id="timeout"
                      type="number"
                      value={searchOptions.timeoutSeconds}
                      onChange={(e) => setSearchOptions(prev => ({
                        ...prev,
                        timeoutSeconds: parseInt(e.target.value) || 30
                      }))}
                      min={10}
                      max={300}
                    />
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
                  
                  <Button
                    onClick={handleAsyncSearch}
                    disabled={loading}
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
                  disabled={loading}
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