// src/pages/ReasoningDashboard.tsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { useKnowledgeBaseStats } from '@/hooks/useKnowledgeBase';

// Services
import reasoningService from '@/services/reasoningService';
import { tiptopService } from '@/services/tiptopService';
import {
  bomGeneratorService,
  NewItemInfo,
  GeneratedBom,
  ComponentCategory,
  SimilarCylinder,
  CodeValidationResult
} from '@/services/bomGeneratorService';

// Types
import { ReasoningResult, ReasonerInfo } from '@/types/tiptop';

// Import the reasoning components we've created
import ReasonerResults from '@/components/reasoning/ReasonerResults';
import SparqlQueryPanel from '@/components/reasoning/SparqlQueryPanel';
import CustomRulesPanel from '@/components/reasoning/CustomRulesPanel';
import BomHierarchyViz from '@/components/reasoning/BomHierarchyViz';

// Icons
import {
  Brain,
  Code,
  Settings,
  Layers,
  Share2,
  BarChart3,
  CheckCircle2,
  XCircle,
  Download,
  Info,
  List
} from 'lucide-react';

// Component interfaces for BOM Generator
interface ComponentSelectorProps {
  category: ComponentCategory;
  selectedOption: string;
  onSelectOption: (code: string) => void;
  onChangeQuantity: (quantity: number) => void;
  quantity: number;
}

interface SimilarCylinderCardProps {
  cylinder: SimilarCylinder;
  onUseAsReference: () => void;
}

// Component Selector Component
const ComponentSelector = ({ 
  category, 
  selectedOption, 
  onSelectOption, 
  onChangeQuantity,
  quantity 
}: ComponentSelectorProps) => {
  return (
    <Card className="mb-4">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg">{category.category}</CardTitle>
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
                      <span>{option.name}</span>
                      <span className="text-xs text-gray-500">{option.code}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
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

// Similar Cylinder Card Component
const SimilarCylinderCard = ({ cylinder, onUseAsReference }: SimilarCylinderCardProps) => {
  return (
    <Card className="mb-4">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-center">
          <CardTitle className="text-lg">{cylinder.name || cylinder.code}</CardTitle>
          <Badge>{cylinder.similarityScore}% Match</Badge>
        </div>
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
        </div>
        <Button variant="outline" className="w-full" onClick={onUseAsReference}>
          <Share2 className="h-4 w-4 mr-2" />
          Use as Reference
        </Button>
      </CardContent>
    </Card>
  );
};
const ReasoningDashboard = () => {
  const { masterItemCode } = useParams<{ masterItemCode: string }>();
  const navigate = useNavigate();
  
  // Reasoning states
  const [activeTab, setActiveTab] = useState('reasoner');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reasonerType, setReasonerType] = useState('OWL');
  const [reasoningResult, setReasoningResult] = useState<ReasoningResult | null>(null);
  const [availableReasoners, setAvailableReasoners] = useState<ReasonerInfo[]>([]);
  const [materialName, setMaterialName] = useState<string>('');

  // BOM Generator states
  const [bomActiveTab, setBomActiveTab] = useState('input');
  const [itemCode, setItemCode] = useState(masterItemCode || '');
  const [itemName, setItemName] = useState('');
  const [itemSpec, setItemSpec] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [codeValidation, setCodeValidation] = useState<CodeValidationResult | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [generatedBom, setGeneratedBom] = useState<GeneratedBom | null>(null);
  const [selectedComponents, setSelectedComponents] = useState<Record<string, string>>({});
  const [componentQuantities, setComponentQuantities] = useState<Record<string, number>>({});
  const [exportFormat, setExportFormat] = useState('JSONLD');

  const { stats: kbStats } = useKnowledgeBaseStats();
  
  useEffect(() => {
    // Fetch available reasoners when component mounts
    const fetchReasoners = async () => {
      try {
        const reasoners = await reasoningService.getAvailableReasoners();
        setAvailableReasoners(reasoners);
      } catch (err) {
        setError('Failed to load available reasoners');
        console.error('Error fetching reasoners:', err);
      }
    };
    
    // Fetch material details to show the name
    const fetchMaterialDetails = async () => {
      if (masterItemCode) {
        try {
          const material = await tiptopService.getMaterialByCode(masterItemCode);
          setMaterialName(material.ima02 || '');
          setItemName(material.ima02 || '');
          setItemSpec(material.ima021 || '');
        } catch (err) {
          console.error('Error fetching material details:', err);
          // Not setting error here as it's not critical
        }
      }
    };
    
    fetchReasoners();
    fetchMaterialDetails();
  }, [masterItemCode]);

  // Initialize component selections when BOM is generated
  useEffect(() => {
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
  
  // Reasoning functions
  const performReasoning = async () => {
    if (!masterItemCode) {
      setError('Master item code is required');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const result = await reasoningService.performReasoning(masterItemCode, reasonerType);
      setReasoningResult(result);
    } catch (err: any) {
      setError(err.message || 'Error performing reasoning. Please try again.');
      console.error('Reasoning error:', err);
    } finally {
      setLoading(false);
    }
  };

  // BOM Generator functions
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

  const generateBom = async () => {
    if (!codeValidation?.isValid) {
      setError('Please enter a valid hydraulic cylinder code');
      return;
    }
    
    setIsGenerating(true);
    setError(null);
    
    try {
      const newItemInfo: NewItemInfo = {
        itemCode,
        itemName,
        itemSpec
      };
      
      const result = await bomGeneratorService.generateNewBom(newItemInfo);
      setGeneratedBom(result);
      setBomActiveTab('components');
      
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An error occurred during BOM generation');
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const exportBom = async () => {
    if (!generatedBom) {
      setError('No BOM has been generated yet');
      return;
    }
    
    setIsExporting(true);
    setError(null);
    
    try {
      const exportBom = { ...generatedBom };
      const ontology = await bomGeneratorService.exportGeneratedBom(exportBom, exportFormat);
      
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
  
  // If no master item code is provided, redirect to search page
  useEffect(() => {
    if (!masterItemCode) {
      navigate('/items');
    }
  }, [masterItemCode, navigate]);
  
  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold">Ontology Reasoning</h1>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-gray-500">Material:</span>
            <Badge variant="outline" className="text-base font-mono">
              {masterItemCode}
            </Badge>
            {materialName && (
              <span className="text-gray-700">{materialName}</span>
            )}
          </div>
        </div>
        <Button variant="outline" onClick={() => navigate(`/items/view/${masterItemCode}`)}>
          Back to Item
        </Button>
      </div>
      
      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      
      <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-6">
        <TabsList className="grid grid-cols-4 w-full max-w-2xl">
          <TabsTrigger value="reasoner">
            <Brain className="h-4 w-4 mr-2" />
            OWL Reasoning
          </TabsTrigger>
          <TabsTrigger value="sparql">
            <Code className="h-4 w-4 mr-2" />
            SPARQL
          </TabsTrigger>
          <TabsTrigger value="rules">
            <Settings className="h-4 w-4 mr-2" />
            Custom Rules
          </TabsTrigger>
          <TabsTrigger value="generator">
            <Layers className="h-4 w-4 mr-2" />
            BOM Generator
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="reasoner" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Reasoning Options</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      Reasoner Type
                    </label>
                    <Select 
                      value={reasonerType} 
                      onValueChange={setReasonerType}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select a reasoner" />
                      </SelectTrigger>
                      <SelectContent>
                        {availableReasoners.map(reasoner => (
                          <SelectItem key={reasoner.id} value={reasoner.id}>
                            {reasoner.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <p className="text-sm text-gray-500 mt-1">
                      {availableReasoners.find(r => r.id === reasonerType)?.description || 
                      'Select a reasoner to see its description'}
                    </p>
                  </div>
                  
                  <Button 
                    onClick={performReasoning} 
                    disabled={loading} 
                    variant="outline"
                    className="w-full"
                  >
                    {loading ? <Spinner className="mr-2 h-4 w-4 animate-spin" /> : null}
                    {loading ? 'Processing...' : 'Perform Reasoning'}
                  </Button>
                </div>
              </CardContent>
            </Card>
            
            <div className="lg:col-span-2">
              {reasoningResult ? (
                <ReasonerResults result={reasoningResult} />
              ) : (
                <Card>
                  <CardContent className="flex flex-col items-center justify-center p-12 text-center">
                    <div className="rounded-full bg-gray-100 p-4 mb-4">
                      <svg 
                        className="h-8 w-8 text-gray-500" 
                        xmlns="http://www.w3.org/2000/svg" 
                        fill="none" 
                        viewBox="0 0 24 24" 
                        stroke="currentColor"
                      >
                        <path 
                          strokeLinecap="round" 
                          strokeLinejoin="round" 
                          strokeWidth={2} 
                          d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" 
                        />
                      </svg>
                    </div>
                    <h3 className="text-lg font-medium mb-2">No Reasoning Results</h3>
                    <p className="text-gray-500 mb-4">
                      Select a reasoner type and click "Perform Reasoning" to analyze the ontology 
                      and generate inference results.
                    </p>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>
        </TabsContent>
        
        <TabsContent value="sparql" className="mt-6">
          <SparqlQueryPanel masterItemCode={masterItemCode || ''} />
        </TabsContent>
        
        <TabsContent value="rules" className="mt-6">
          <CustomRulesPanel masterItemCode={masterItemCode || ''} />
        </TabsContent>

        <TabsContent value="generator" className="mt-6">
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Hydraulic Cylinder BOM Generator</CardTitle>
              <CardContent className="text-sm text-gray-600">
                Generate intelligent BOMs for new hydraulic cylinders using semantic reasoning and domain knowledge.
              </CardContent>
            </CardHeader>
          </Card>

          <Tabs value={bomActiveTab} onValueChange={setBomActiveTab} className="mb-6">
            <TabsList className="grid grid-cols-3 w-full">
              <TabsTrigger value="input">
                <Settings className="h-4 w-4 mr-2" />
                Specifications
              </TabsTrigger>
              <TabsTrigger value="components" disabled={!generatedBom}>
                <List className="h-4 w-4 mr-2" />
                Components
              </TabsTrigger>
              <TabsTrigger value="similar" disabled={!generatedBom}>
                <BarChart3 className="h-4 w-4 mr-2" />
                Similar Items
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="input" className="mt-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Cylinder Specifications</CardTitle>
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

                      <div className="flex justify-end pt-4">
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
                      </div>
                    </div>
                  </CardContent>
                </Card>
                
                <Card>
                  <CardHeader>
                    <CardTitle>Code Format Guide</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="bg-gray-100 p-3 rounded-md font-mono text-sm mb-2">
                        <span className="text-blue-600">3</span>
                        <span className="text-green-600">S</span>
                        <span className="text-yellow-600">A</span>
                        <span className="text-red-600">050</span>
                        <span className="text-gray-400">-</span>
                        <span className="text-purple-600">0150</span>
                        <span className="text-pink-600">Y</span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-sm">
                        <div>
                          <span className="inline-block w-3 h-3 bg-blue-600 mr-1"></span>
                          <span>Product Type</span>
                        </div>
                        <div>
                          <span className="inline-block w-3 h-3 bg-green-600 mr-1"></span>
                          <span>Series</span>
                        </div>
                        <div>
                          <span className="inline-block w-3 h-3 bg-yellow-600 mr-1"></span>
                          <span>Type</span>
                        </div>
                        <div>
                          <span className="inline-block w-3 h-3 bg-red-600 mr-1"></span>
                          <span>Bore Size</span>
                        </div>
                        <div>
                          <span className="inline-block w-3 h-3 bg-purple-600 mr-1"></span>
                          <span>Stroke Length</span>
                        </div>
                        <div>
                          <span className="inline-block w-3 h-3 bg-pink-600 mr-1"></span>
                          <span>Rod End Type</span>
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
                  <div className="lg:col-span-3 space-y-4">
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
                            variant={"outline"}
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
                          
                          <div className="text-xs text-gray-500">
                            <Info className="h-3 w-3 inline mr-1" />
                            Export includes selected components and quantities.
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
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
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
                        </AlertDescription>
                      </Alert>
                    </div>
                  )}
                </div>
              )}
            </TabsContent>
          </Tabs>
        </TabsContent>
      </Tabs>
      
      {reasoningResult && reasoningResult.bomHierarchy && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>BOM Hierarchy with Inferred Information</CardTitle>
          </CardHeader>
          <CardContent>
            <BomHierarchyViz bomHierarchy={reasoningResult.bomHierarchy} />
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default ReasoningDashboard;