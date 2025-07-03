// src/pages/BomGeneratorPage.tsx
import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  bomGeneratorService,
  NewItemInfo,
  GeneratedBom,
  ComponentCategory,
  SimilarCylinder,
  CodeValidationResult
} from '@/services/bomGeneratorService';
import knowledgeBaseService, {
  ExportRequest
} from '@/services/knowledgeBaseService';
import knowledgeBaseSearchService, {
  SearchType,
  SortOrder,
  SimilarBOMDTO,
  SearchResultDTO,
  SearchStatus,
  SearchProgressDTO,
  ProcessingPhase
} from '@/services/knowledgeBaseSearchService';

import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Spinner } from '@/components/ui/spinner';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Progress } from '@/components/ui/progress';
import { toast } from 'sonner';
import {
  Download,
  Info,
  CheckCircle2,
  XCircle,
  Settings,
  List,
  BarChart3,
  Share2,
  Layers,
  Database,
  Save,
  Brain,
  Search,
  AlertCircle,
  Clock,
  Zap,
} from 'lucide-react';

// Utility function for formatting file size
const formatFileSize = (bytes: number): string => {
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  if (bytes === 0) return '0 Bytes';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
};

interface ComponentSelectorProps {
  category: ComponentCategory;
  selectedOption: string;
  onSelectOption: (code: string) => void;
  onChangeQuantity: (quantity: number) => void;
  quantity: number;
}

const ComponentSelector = ({
  category,
  selectedOption,
  onSelectOption,
  onChangeQuantity,
  quantity
}: ComponentSelectorProps) => {
  const selectedComponent = category.options.find(opt => opt.code === selectedOption);
  return (
    <Card className="mb-4">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center justify-between">
          {category.categoryDisplayName || category.category}
          {category.isRequired && (
            <Badge variant="default" className="text-xs">Required</Badge>
          )}
        </CardTitle>
        <CardDescription>
          {category.categoryDescription || `Select a component for this category`}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid gap-4">
          <div>
            <Label htmlFor={`${category.category}-component`}>Component</Label>
            <Select
              value={selectedOption || ''}
              onValueChange={onSelectOption}
            >
              <SelectTrigger id={`${category.category}-component`}>
                <SelectValue placeholder="Select component" />
              </SelectTrigger>
              <SelectContent>
                {category.options.map(option => (
                  <SelectItem key={option.code} value={option.code}>
                    <div className="flex flex-col">
                      <div className="flex items-center justify-between w-full">
                        <span>{option.name}</span>
                        {option.compatibilityScore && (
                          <Badge variant="outline" className="ml-2 text-xs">
                            {(option.compatibilityScore * 100).toFixed(0)}% match
                          </Badge>
                        )}
                      </div>
                      <span className="text-xs text-gray-500">{option.code}</span>
                      {option.spec && (
                        <span className="text-xs text-gray-400">{option.spec}</span>
                      )}
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {selectedComponent && selectedComponent.recommendationLevel && (
            <div className="p-2 bg-blue-50 rounded text-sm">
              <span className="font-medium">Recommendation: </span>
              <span className={`
                ${selectedComponent.recommendationLevel === 'Highly Recommended' ? 'text-green-700' : ''}
                ${selectedComponent.recommendationLevel === 'Recommended' ? 'text-blue-700' : ''}
                ${selectedComponent.recommendationLevel === 'Consider' ? 'text-yellow-700' : ''}
                ${selectedComponent.recommendationLevel === 'Alternative' ? 'text-gray-700' : ''}
              `}>
                {selectedComponent.recommendationLevel}
              </span>
            </div>
          )}

          <div>
            <Label htmlFor={`${category.category}-quantity`}>Quantity</Label>
            <Input
              id={`${category.category}-quantity`}
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => onChangeQuantity(parseInt(e.target.value) || 1)}
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

interface SimilarCylinderCardProps {
  cylinder: SimilarCylinder;
  onUseAsReference: () => void;
}

const SimilarCylinderCard = ({ cylinder, onUseAsReference }: SimilarCylinderCardProps) => {
  return (
    <Card className="mb-4">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-center">
          <CardTitle className="text-lg">{cylinder.name || cylinder.code}</CardTitle>
          <Badge>{cylinder.similarityScore}% Match</Badge>
        </div>
        <CardDescription>
          {cylinder.spec || 'No specification available'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-2 text-sm mb-4">
          <div>
            <span className="font-medium">Series:</span> {cylinder.specifications.series}
          </div>
          <div>
            <span className="font-medium">Type:</span> {cylinder.specifications.type}
          </div>
          <div>
            <span className="font-medium">Bore:</span> {cylinder.specifications.bore}
          </div>
          <div>
            <span className="font-medium">Stroke:</span> {cylinder.specifications.stroke}
          </div>
          <div>
            <span className="font-medium">Rod End:</span> {cylinder.specifications.rodEndType}
          </div>
        </div>
      </CardContent>
      <CardFooter>
        <Button variant="outline" className="w-full" onClick={onUseAsReference}>
          <Share2 className="h-4 w-4 mr-2" />
          Use as Reference
        </Button>
      </CardFooter>
    </Card>
  );
};

interface KnowledgeBaseSuggestionCardProps {
  suggestion: SimilarBOMDTO;
  onViewDetails: () => void;
}

const KnowledgeBaseSuggestionCard = ({ suggestion, onViewDetails }: KnowledgeBaseSuggestionCardProps) => {
  return (
    <Card className="border-l-4 border-l-green-500">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-center">
          <CardTitle className="text-lg">{suggestion.masterItemCode}</CardTitle>
          <Badge variant="outline" className="bg-green-50">
            <Brain className="h-3 w-3 mr-1" />
            {(suggestion.similarityScore * 100).toFixed(1)}% Match
          </Badge>
        </div>
        <CardDescription>
          {suggestion.description || 'From Knowledge Base'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="text-sm space-y-1">
          <div className="flex justify-between">
            <span className="text-gray-500">Format:</span>
            <span>{suggestion.format}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Size:</span>
            <span>{formatFileSize(suggestion.fileSize)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Triples:</span>
            <span>{suggestion.tripleCount.toLocaleString()}</span>
          </div>
          {suggestion.qualityScore && (
            <div className="flex justify-between">
              <span className="text-gray-500">Quality:</span>
              <Badge variant="outline" className="text-xs">
                {(suggestion.qualityScore * 100).toFixed(0)}%
              </Badge>
            </div>
          )}
          {suggestion.validationStatus && (
            <div className="flex justify-between">
              <span className="text-gray-500">Status:</span>
              <Badge
                variant={suggestion.validationStatus === 'VALIDATED' ? 'default' : 'secondary'}
                className="text-xs"
              >
                {suggestion.validationStatus}
              </Badge>
            </div>
          )}
          <div className="flex justify-between">
            <span className="text-gray-500">Created:</span>
            <span>{new Date(suggestion.createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      </CardContent>
      <CardFooter>
        <Button
          variant="outline"
          size="sm"
          className="w-full"
          onClick={onViewDetails}
        >
          View Details
        </Button>
      </CardFooter>
    </Card>
  );
};

// New component for async search status
interface AsyncSearchStatusCardProps {
  searchResult: SearchResultDTO;
  progress?: SearchProgressDTO;
  onRefresh: () => void;
}

const AsyncSearchStatusCard = ({ searchResult, progress, onRefresh }: AsyncSearchStatusCardProps) => {
  const getStatusIcon = () => {
    switch (searchResult.status) {
      case SearchStatus.PENDING:
        return <Clock className="h-5 w-5 text-yellow-500" />;
      case SearchStatus.PROCESSING:
        return <Spinner className="h-5 w-5 text-blue-500" />;
      case SearchStatus.COMPLETED:
        return <CheckCircle2 className="h-5 w-5 text-green-500" />;
      case SearchStatus.FAILED:
        return <XCircle className="h-5 w-5 text-red-500" />;
      case SearchStatus.CANCELLED:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
      case SearchStatus.PARTIAL:
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
      default:
        return null;
    }
  };

  const getStatusColor = () => {
    switch (searchResult.status) {
      case SearchStatus.PENDING:
      case SearchStatus.PARTIAL:
        return 'border-yellow-500 bg-yellow-50';
      case SearchStatus.PROCESSING:
        return 'border-blue-500 bg-blue-50';
      case SearchStatus.COMPLETED:
        return 'border-green-500 bg-green-50';
      case SearchStatus.FAILED:
        return 'border-red-500 bg-red-50';
      case SearchStatus.CANCELLED:
        return 'border-gray-500 bg-gray-50';
      default:
        return '';
    }
  };

  return (
    <Card className={`border-2 ${getStatusColor()}`}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {getStatusIcon()}
            <CardTitle className="text-lg">Knowledge Base Search</CardTitle>
          </div>
          <Button
            size="sm"
            variant="ghost"
            onClick={onRefresh}
            disabled={searchResult.status === SearchStatus.PROCESSING}
          >
            <Search className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600">Status:</span>
            <Badge variant={searchResult.status === SearchStatus.COMPLETED ? 'default' : 'secondary'}>
              {searchResult.status}
            </Badge>
          </div>

          {progress && (
            <>
              <div className="space-y-1">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-600">Progress:</span>
                  <span className="font-medium">{Math.round(progress.percentComplete)}%</span>
                </div>
                <Progress value={progress.percentComplete} className="h-2" />
              </div>

              {progress.currentPhase && (
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-600">Phase:</span>
                  <span className="font-medium">{progress.currentPhase}</span>
                </div>
              )}

              {progress.foundMatches > 0 && (
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-600">Matches Found:</span>
                  <span className="font-medium">{progress.foundMatches}</span>
                </div>
              )}
            </>
          )}

          {searchResult.error && (
            <Alert variant="destructive" className="mt-2">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{searchResult.error}</AlertDescription>
            </Alert>
          )}

          {searchResult.durationMs > 0 && (
            <div className="flex items-center justify-between text-sm text-gray-500">
              <span>Duration:</span>
              <span>{Math.round(searchResult.durationMs / 1000)}s</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

const BomGeneratorPage = () => {
  const [activeTab, setActiveTab] = useState('input');
  const [itemCode, setItemCode] = useState('');
  const [itemName, setItemName] = useState('');
  const [itemSpec, setItemSpec] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [codeValidation, setCodeValidation] = useState<CodeValidationResult | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isSavingToKB, setIsSavingToKB] = useState(false);
  const [generatedBom, setGeneratedBom] = useState<GeneratedBom | null>(null);
  const [selectedComponents, setSelectedComponents] = useState<Record<string, string>>({});
  const [componentQuantities, setComponentQuantities] = useState<Record<string, number>>({});
  const [exportFormat, setExportFormat] = useState('JSONLD');
  const [error, setError] = useState<string | null>(null);
  const [knowledgeBaseSuggestions, setKnowledgeBaseSuggestions] = useState<SimilarBOMDTO[]>([]);

  // New states for async search
  const [searchMode, setSearchMode] = useState<'sync' | 'async'>('sync');
  const [asyncSearchId, setAsyncSearchId] = useState<string | null>(null);
  const [searchResult, setSearchResult] = useState<SearchResultDTO | null>(null);
  const [isPollingStatus, setIsPollingStatus] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    // Initialize default selected components and quantities
    if (generatedBom) {
      const initialComponents: Record<string, string> = {};
      const initialQuantities: Record<string, number> = {};

      generatedBom.componentCategories.forEach(category => {
        if (category.options.length > 0) {
          initialComponents[category.category] = category.options[0].code;
        }
        initialQuantities[category.category] = category.defaultQuantity;
      });

      setSelectedComponents(initialComponents);
      setComponentQuantities(initialQuantities);
    }
  }, [generatedBom]);

  // Polling for async search results
  useEffect(() => {
    let intervalId: NodeJS.Timeout;

    if (asyncSearchId && isPollingStatus) {
      intervalId = setInterval(async () => {
        try {
          const progress = await knowledgeBaseSearchService.getSearchProgress(asyncSearchId);
          
          // Update progress in search result
          if (searchResult) {
            setSearchResult({ ...searchResult, progress });
          }

          // Check if search is complete
          if (progress.percentComplete >= 100 || progress.currentPhase === ProcessingPhase.FINALIZING) {
            setIsPollingStatus(false);

            // Poll for final results
            try {
              const results = await knowledgeBaseSearchService.pollSearchResults(
                asyncSearchId,
                undefined,
                1000,
                5
              );
              
              setSearchResult(results);
              
              if (results.results) {
                setKnowledgeBaseSuggestions(results.results);
                toast.success(`Found ${results.results.length} similar BOMs in knowledge base`);
              }
            } catch (err) {
              console.error('Error getting final results:', err);
            }
          }
        } catch (err) {
          console.error('Error polling search progress:', err);
          setIsPollingStatus(false);
        }
      }, 2000); // Poll every 2 seconds
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [asyncSearchId, isPollingStatus, searchResult]);

  const validateCylinderCode = async () => {
    if (!itemCode) {
      setError('Please enter an item code');
      return;
    }

    setIsValidating(true);
    setError(null);

    try {
      const result = await bomGeneratorService.validateCylinderCode(itemCode);
      setCodeValidation(result);

      if (!result.isValid) {
        setError(result.message);
      }

    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An error occurred during validation');
      }
      setCodeValidation(null);
    } finally {
      setIsValidating(false);
    }
  };

  const performKnowledgeBaseSearch = async (specifications: any) => {
    const searchSpecs: Record<string, string> = {
      series: specifications.series,
      type: specifications.type,
      bore: specifications.bore,
      stroke: specifications.stroke,
      rodEndType: specifications.rodEndType,
      ...(specifications.installationType && { installationType: specifications.installationType }),
      ...(specifications.shaftEndJoin && { shaftEndJoin: specifications.shaftEndJoin })
    };

    const searchRequest = knowledgeBaseSearchService.buildSearchRequest(
      searchSpecs,
      {
        maxResults: 10,
        minSimilarityScore: 0.7,
        onlyHydraulicCylinders: true,
        useCache: true,
        sortOrder: SortOrder.SIMILARITY_DESC
      },
      SearchType.SIMILARITY
    );

    if (searchMode === 'async') {
      // Start async search
      try {
        const asyncResult = await knowledgeBaseSearchService.searchSimilarAsync(searchRequest);
        setAsyncSearchId(asyncResult.searchId);
        setSearchResult(asyncResult);
        setIsPollingStatus(true);
        toast.info('Knowledge base search started in background');
      } catch (err) {
        console.error('Failed to start async search:', err);
        toast.error('Failed to start knowledge base search');
      }
    } else {
      // Perform synchronous search
      try {
        const result = await knowledgeBaseSearchService.searchSimilar(searchRequest);
        setSearchResult(result);

        if (result.status === SearchStatus.FAILED) {
          console.error('Knowledge base search failed:', result.error);
          toast.error(result.error || 'Knowledge base search failed');
          setKnowledgeBaseSuggestions([]);
        } else if (result.status === SearchStatus.PARTIAL) {
          toast.warning('Knowledge base search completed with some errors');
          if (result.results) {
            setKnowledgeBaseSuggestions(result.results);
            toast.info(`Found ${result.results.length} similar BOMs (partial results)`);
          }
        } else if (result.status === SearchStatus.COMPLETED && result.results) {
          setKnowledgeBaseSuggestions(result.results);
          if (result.results.length > 0) {
            toast.info(`Found ${result.results.length} similar BOMs in knowledge base`);
          } else {
            toast.info('No similar BOMs found in knowledge base');
          }
        }
      } catch (searchError) {
        console.error('Knowledge base search error:', searchError);
        toast.warning('Could not search knowledge base');
        setKnowledgeBaseSuggestions([]);
      }
    }
  };

  const generateBom = async () => {
    if (!codeValidation?.isValid) {
      setError('Please enter a valid hydraulic cylinder code');
      return;
    }

    setIsGenerating(true);
    setError(null);

    try {
      // First, check if similar BOMs exist in knowledge base
      await performKnowledgeBaseSearch(codeValidation.specifications);

      // Generate new BOM
      const newItemInfo: NewItemInfo = {
        itemCode,
        itemName,
        itemSpec
      };

      const result = await bomGeneratorService.generateNewBom(newItemInfo);
      setGeneratedBom(result);
      setActiveTab('components');

    } catch (err) {
      console.error('BOM generation error:', err);
      if (err instanceof Error) {
        setError(err.message);
        toast.error(`Failed to generate BOM: ${err.message}`);
      } else {
        setError('An error occurred during BOM generation');
        toast.error('Failed to generate BOM');
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const refreshSearchStatus = useCallback(async () => {
    if (asyncSearchId) {
      try {
        const progress = await knowledgeBaseSearchService.getSearchProgress(asyncSearchId);
        
        if (searchResult) {
          setSearchResult({ ...searchResult, progress });
        }
        
        if (progress.percentComplete >= 100) {
          const results = await knowledgeBaseSearchService.pollSearchResults(asyncSearchId, undefined, 1000, 1);
          setSearchResult(results);
          if (results.results) {
            setKnowledgeBaseSuggestions(results.results);
          }
        }
      } catch (err) {
        console.error('Failed to refresh search status:', err);
      }
    }
  }, [asyncSearchId, searchResult]);

  const exportBom = async () => {
    if (!generatedBom) {
      setError('No BOM has been generated yet');
      return;
    }

    setIsExporting(true);
    setError(null);

    try {
      // Create a modified BOM structure with the selected components
      const exportBom = { ...generatedBom };

      // Export the BOM
      const ontology = await bomGeneratorService.exportGeneratedBom(exportBom, exportFormat);

      // Create a downloadable file
      const blob = new Blob([ontology], { type: getContentType(exportFormat) });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${itemCode}_bom.${getFileExtension(exportFormat)}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      toast.success('BOM exported successfully');

    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An error occurred during export');
      }
    } finally {
      setIsExporting(false);
    }
  };

  const saveToKnowledgeBase = async () => {
    if (!generatedBom) return;

    setIsSavingToKB(true);

    try {
      const exportRequest: ExportRequest = {
        masterItemCode: generatedBom.masterItemCode,
        format: 'RDF/XML',
        includeHierarchy: true,
        description: `Generated BOM for ${generatedBom.itemName || generatedBom.masterItemCode} - ${new Date().toISOString()}`
      };

      await knowledgeBaseService.exportSingleToKnowledgeBase(exportRequest);
      toast.success('BOM saved to knowledge base successfully');

    } catch (err) {
      toast.error('Failed to save BOM to knowledge base');
    } finally {
      setIsSavingToKB(false);
    }
  };

  const handleSelectComponent = (category: string, code: string) => {
    setSelectedComponents(prev => ({
      ...prev,
      [category]: code
    }));
  };

  const handleChangeQuantity = (category: string, quantity: number) => {
    setComponentQuantities(prev => ({
      ...prev,
      [category]: quantity
    }));
  };

  const handleUseAsReference = (cylinder: SimilarCylinder) => {
    navigate(`/items/view/${cylinder.code}`);
  };

  const handleViewKBDetails = (suggestion: SimilarBOMDTO) => {
    navigate(`/items/view/${suggestion.masterItemCode}`);
  };

  const getContentType = (format: string): string => {
    switch (format.toUpperCase()) {
      case 'TURTLE':
      case 'TTL':
        return 'text/turtle';
      case 'N-TRIPLES':
      case 'NT':
        return 'application/n-triples';
      case 'RDF/XML':
      case 'XML':
        return 'application/rdf+xml';
      case 'JSONLD':
      default:
        return 'application/ld+json';
    }
  };

  const getFileExtension = (format: string): string => {
    switch (format.toUpperCase()) {
      case 'TURTLE':
      case 'TTL':
        return 'ttl';
      case 'N-TRIPLES':
      case 'NT':
        return 'nt';
      case 'RDF/XML':
      case 'XML':
        return 'owl';
      case 'JSONLD':
      default:
        return 'jsonld';
    }
  };

  return (
    <div className="container mx-auto py-6">
      <h1 className="text-2xl font-bold mb-6">Hydraulic Cylinder BOM Generator</h1>

      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Search Mode Toggle */}
      <Card className="mb-6">
        <CardHeader className="py-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Knowledge Base Search Mode</CardTitle>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant={searchMode === 'sync' ? 'default' : 'outline'}
                onClick={() => setSearchMode('sync')}
              >
                <Zap className="h-4 w-4 mr-1" />
                Fast Search
              </Button>
              <Button
                size="sm"
                variant={searchMode === 'async' ? 'default' : 'outline'}
                onClick={() => setSearchMode('async')}
              >
                <Database className="h-4 w-4 mr-1" />
                Deep Search
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          <p className="text-sm text-gray-600">
            {searchMode === 'sync' 
              ? 'Fast search provides quick results from cached data'
              : 'Deep search thoroughly analyzes all knowledge base entries for best matches'}
          </p>
        </CardContent>
      </Card>

      {/* Async Search Status */}
      {searchResult && searchMode === 'async' && (
        <div className="mb-6">
          <AsyncSearchStatusCard
            searchResult={searchResult}
            progress={searchResult.progress}
            onRefresh={refreshSearchStatus}
          />
        </div>
      )}

      <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-6">
        <TabsList className="grid grid-cols-4 w-full">
          <TabsTrigger value="input">
            <Settings className="h-4 w-4 mr-2" />
            Input Specifications
          </TabsTrigger>
          <TabsTrigger value="components" disabled={!generatedBom}>
            <List className="h-4 w-4 mr-2" />
            Components
          </TabsTrigger>
          <TabsTrigger value="similar" disabled={!generatedBom}>
            <BarChart3 className="h-4 w-4 mr-2" />
            Similar Cylinders
          </TabsTrigger>
          <TabsTrigger value="knowledge" disabled={!knowledgeBaseSuggestions.length}>
            <Database className="h-4 w-4 mr-2" />
            Knowledge Base
          </TabsTrigger>
        </TabsList>

        <TabsContent value="input" className="mt-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>New Hydraulic Cylinder Information</CardTitle>
                <CardDescription>
                  Enter the details for the new hydraulic cylinder
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="grid gap-2">
                    <Label htmlFor="itemCode">Cylinder Code *</Label>
                    <div className="flex">
                      <Input
                        id="itemCode"
                        value={itemCode}
                        onChange={(e) => setItemCode(e.target.value)}
                        placeholder="Enter cylinder code"
                        className="rounded-r-none"
                      />
                      <Button
                        onClick={validateCylinderCode}
                        disabled={isValidating || !itemCode}
                        className="rounded-l-none"
                        variant="outline"
                      >
                        {isValidating ? <Spinner className="h-4 w-4 mr-2" /> : null}
                        Validate
                      </Button>
                    </div>
                    <p className="text-xs text-gray-500">
                      Hydraulic cylinder code format: Starts with 3 or 4, followed by series, type, bore, stroke, and rod end type.
                    </p>
                  </div>

                  {codeValidation && (
                    <div className={`p-3 rounded-md ${codeValidation.isValid ? 'bg-green-50' : 'bg-red-50'}`}>
                      <div className="flex items-center mb-2">
                        {codeValidation.isValid ? (
                          <CheckCircle2 className="h-5 w-5 text-green-500 mr-2" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-500 mr-2" />
                        )}
                        <span className={codeValidation.isValid ? 'text-green-700' : 'text-red-700'}>
                          {codeValidation.message}
                        </span>
                      </div>

                      {codeValidation.isValid && (
                        <div className="grid grid-cols-2 gap-2 text-sm">
                          <div>
                            <span className="font-medium">Series:</span> {codeValidation.specifications.series}
                          </div>
                          <div>
                            <span className="font-medium">Type:</span> {codeValidation.specifications.type}
                          </div>
                          <div>
                            <span className="font-medium">Bore:</span> {codeValidation.specifications.bore}
                          </div>
                          <div>
                            <span className="font-medium">Stroke:</span> {codeValidation.specifications.stroke}
                          </div>
                          <div>
                            <span className="font-medium">Rod End:</span> {codeValidation.specifications.rodEndType}
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  <div className="grid gap-2">
                    <Label htmlFor="itemName">Cylinder Name</Label>
                    <Input
                      id="itemName"
                      value={itemName}
                      onChange={(e) => setItemName(e.target.value)}
                      placeholder="Enter cylinder name"
                    />
                  </div>

                  <div className="grid gap-2">
                    <Label htmlFor="itemSpec">Cylinder Specification</Label>
                    <Input
                      id="itemSpec"
                      value={itemSpec}
                      onChange={(e) => setItemSpec(e.target.value)}
                      placeholder="Enter cylinder specification"
                    />
                  </div>
                </div>
              </CardContent>
              <CardFooter className="flex justify-end">
                <Button
                  onClick={generateBom}
                  disabled={isGenerating || !codeValidation?.isValid}
                  variant="outline"
                >
                  {isGenerating ? (
                    <>
                      <Spinner className="mr-2 h-4 w-4 animate-spin" />
                      Generating...
                    </>
                  ) : (
                    <>
                      <Layers className="mr-2 h-4 w-4" />
                      Generate BOM
                    </>
                  )}
                </Button>
              </CardFooter>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Hydraulic Cylinder BOM Structure</CardTitle>
                <CardDescription>
                  Learn about the structure of hydraulic cylinder codes
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-6">
                  <div>
                    <h3 className="text-lg font-medium mb-2">Code Format</h3>
                    <div className="bg-gray-100 p-3 rounded-md font-mono text-sm mb-2">
                      <span className="text-blue-600">4</span>
                      <span className="text-green-600">12</span>
                      <span className="text-yellow-600">F</span>
                      <span className="text-red-600">050</span>
                      <span className="text-gray-400">-</span>
                      <span className="text-purple-600">0146</span>
                      <span className="text-pink-600">Y</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="inline-block w-3 h-3 bg-blue-600 mr-1"></span>
                        <span>Product Type (3 or 4)</span>
                      </div>
                      <div>
                        <span className="inline-block w-3 h-3 bg-green-600 mr-1"></span>
                        <span>Series (e.g. 12)</span>
                      </div>
                      <div>
                        <span className="inline-block w-3 h-3 bg-yellow-600 mr-1"></span>
                        <span>Type (e.g. F)</span>
                      </div>
                      <div>
                        <span className="inline-block w-3 h-3 bg-red-600 mr-1"></span>
                        <span>Bore Size (e.g. 050mm)</span>
                      </div>
                      <div>
                        <span className="inline-block w-3 h-3 bg-purple-600 mr-1"></span>
                        <span>Stroke Length (e.g. 0146mm)</span>
                      </div>
                      <div>
                        <span className="inline-block w-3 h-3 bg-pink-600 mr-1"></span>
                        <span>Rod End Type (e.g. Y)</span>
                      </div>
                    </div>
                  </div>

                  <Separator />

                  <div>
                    <h3 className="text-lg font-medium mb-2">Common Series</h3>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="font-medium">10:</span> Standard Series
                      </div>
                      <div>
                        <span className="font-medium">11:</span> Heavy Duty
                      </div>
                      <div>
                        <span className="font-medium">12:</span> Compact
                      </div>
                      <div>
                        <span className="font-medium">13:</span> Light Duty
                      </div>
                    </div>
                  </div>

                  <Separator />

                  <div>
                    <h3 className="text-lg font-medium mb-2">Rod End Types</h3>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="font-medium">Y:</span> Yoke
                      </div>
                      <div>
                        <span className="font-medium">I:</span> Internal Thread
                      </div>
                      <div>
                        <span className="font-medium">E:</span> External Thread
                      </div>
                      <div>
                        <span className="font-medium">P:</span> Pin
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="components" className="mt-6">
          {generatedBom && (
            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
              <div className="lg:col-span-3 space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Generated BOM Structure</CardTitle>
                    <CardDescription>
                      Select components for the new hydraulic cylinder
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {generatedBom.componentStatistics && (
                      <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                        <h4 className="font-medium mb-2">Generation Statistics</h4>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                          <div>
                            <span className="text-gray-500">Total Components:</span>
                            <div className="font-medium">{generatedBom.componentStatistics.totalComponents}</div>
                          </div>
                          <div>
                            <span className="text-gray-500">Avg. Compatibility:</span>
                            <div className="font-medium">{generatedBom.componentStatistics.averageCompatibilityScore}%</div>
                          </div>
                          <div>
                            <span className="text-gray-500">High Confidence:</span>
                            <div className="font-medium">{generatedBom.componentStatistics.highConfidenceComponents}</div>
                          </div>
                          <div>
                            <span className="text-gray-500">Overall Score:</span>
                            <div className="font-medium">{generatedBom.overallRecommendationScore ? (generatedBom.overallRecommendationScore * 100).toFixed(1) : 'N/A'}%</div>
                          </div>
                        </div>
                      </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {generatedBom.componentCategories.map(category => (
                        <ComponentSelector
                          key={category.category}
                          category={category}
                          selectedOption={selectedComponents[category.category] || ''}
                          onSelectOption={(code) => handleSelectComponent(category.category, code)}
                          quantity={componentQuantities[category.category] || category.defaultQuantity}
                          onChangeQuantity={(qty) => handleChangeQuantity(category.category, qty)}
                        />
                      ))}
                    </div>
                  </CardContent>
                </Card>
              </div>

              <div>
                <Card className="sticky top-6">
                  <CardHeader>
                    <CardTitle>Export Options</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="exportFormat">Export Format</Label>
                        <Select
                          value={exportFormat}
                          onValueChange={setExportFormat}
                        >
                          <SelectTrigger id="exportFormat">
                            <SelectValue placeholder="Select format" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="RDF/XML">OWL/RDF (XML)</SelectItem>
                            <SelectItem value="JSONLD">JSON-LD</SelectItem>
                            <SelectItem value="TURTLE">Turtle</SelectItem>
                            <SelectItem value="N-TRIPLES">N-Triples</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>

                      <Button
                        className="w-full"
                        onClick={exportBom}
                        disabled={isExporting}
                      >
                        {isExporting ? (
                          <>
                            <Spinner className="mr-2 h-4 w-4 animate-spin" />
                            Exporting...
                          </>
                        ) : (
                          <>
                            <Download className="mr-2 h-4 w-4" />
                            Export BOM
                          </>
                        )}
                      </Button>

                      <Button
                        className="w-full"
                        variant="outline"
                        onClick={saveToKnowledgeBase}
                        disabled={isSavingToKB}
                      >
                        {isSavingToKB ? (
                          <>
                            <Spinner className="mr-2 h-4 w-4 animate-spin" />
                            Saving...
                          </>
                        ) : (
                          <>
                            <Save className="mr-2 h-4 w-4" />
                            Save to Knowledge Base
                          </>
                        )}
                      </Button>

                      <div className="text-xs text-gray-500">
                        <Info className="h-3 w-3 inline mr-1" />
                        Save this BOM to the knowledge base to improve future generations.
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          )}
        </TabsContent>

        <TabsContent value="similar" className="mt-6">
          {generatedBom && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {generatedBom.similarCylinders.map((cylinder, index) => (
                <SimilarCylinderCard
                  key={index}
                  cylinder={cylinder}
                  onUseAsReference={() => handleUseAsReference(cylinder)}
                />
              ))}

              {generatedBom.similarCylinders.length === 0 && (
                <div className="col-span-full">
                  <Alert>
                    <AlertTitle>No similar cylinders found</AlertTitle>
                    <AlertDescription>
                      No existing hydraulic cylinders were found with similar specifications.
                      The generated BOM is based on general rules for hydraulic cylinders.
                    </AlertDescription>
                  </Alert>
                </div>
              )}
            </div>
          )}
        </TabsContent>

        <TabsContent value="knowledge" className="mt-6">
          <div className="space-y-4">
            <div className="mb-4">
              <h3 className="text-lg font-semibold mb-2">Knowledge Base Suggestions</h3>
              <p className="text-sm text-gray-600">
                These BOMs from the knowledge base have similar specifications and can be used as reference.
              </p>
            </div>

            {knowledgeBaseSuggestions.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {knowledgeBaseSuggestions.map((suggestion, index) => (
                  <KnowledgeBaseSuggestionCard
                    key={index}
                    suggestion={suggestion}
                    onViewDetails={() => handleViewKBDetails(suggestion)}
                  />
                ))}
              </div>
            ) : (
              <Alert>
                <AlertTitle>No knowledge base suggestions</AlertTitle>
                <AlertDescription>
                  No similar BOMs were found in the knowledge base. Build your knowledge base by exporting existing BOMs.
                </AlertDescription>
              </Alert>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default BomGeneratorPage;