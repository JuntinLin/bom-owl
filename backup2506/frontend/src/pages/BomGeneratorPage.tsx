// src/pages/BomGeneratorPage.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  bomGeneratorService,
  NewItemInfo,
  GeneratedBom,
  ComponentCategory,
  SimilarCylinder,
  CodeValidationResult
} from '@/services/bomGeneratorService';

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
  Layers
} from 'lucide-react';

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
  return (
    <Card className="mb-4">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg">{category.category}</CardTitle>
        <CardDescription>
          Select a component for this category
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

const BomGeneratorPage = () => {
  const [activeTab, setActiveTab] = useState('input');
  const [itemCode, setItemCode] = useState('');
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
  const [error, setError] = useState<string | null>(null);
  
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
      setActiveTab('components');
      
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

  return (
    <div className="container mx-auto py-6">
      <h1 className="text-2xl font-bold mb-6">Hydraulic Cylinder BOM Generator</h1>
      
      {error && (
        <Alert variant="destructive" className="mb-6">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      
      <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-6">
        <TabsList className="grid grid-cols-3 w-full">
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
                      
                      <div className="text-xs text-gray-500">
                        <Info className="h-3 w-3 inline mr-1" />
                        This will export the BOM with your selected components and quantities.
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
      </Tabs>
    </div>
  );
};

export default BomGeneratorPage;