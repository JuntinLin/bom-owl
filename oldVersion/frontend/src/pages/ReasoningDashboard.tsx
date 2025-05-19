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
import reasoningService from '@/services/reasoningService';
import { tiptopService } from '@/services/tiptopService';
import { ReasoningResult, ReasonerInfo } from '@/types/tiptop';

// Import the components we've created
import ReasonerResults from '@/components/reasoning/ReasonerResults';
import SparqlQueryPanel from '@/components/reasoning/SparqlQueryPanel';
import CustomRulesPanel from '@/components/reasoning/CustomRulesPanel';
import BomHierarchyViz from '@/components/reasoning/BomHierarchyViz';

const ReasoningDashboard = () => {
  const { masterItemCode } = useParams<{ masterItemCode: string }>();
  const navigate = useNavigate();
  
  const [activeTab, setActiveTab] = useState('reasoner');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reasonerType, setReasonerType] = useState('OWL');
  const [reasoningResult, setReasoningResult] = useState<ReasoningResult | null>(null);
  const [availableReasoners, setAvailableReasoners] = useState<ReasonerInfo[]>([]);
  const [materialName, setMaterialName] = useState<string>('');
  
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
        } catch (err) {
          console.error('Error fetching material details:', err);
          // Not setting error here as it's not critical
        }
      }
    };
    
    fetchReasoners();
    fetchMaterialDetails();
  }, [masterItemCode]);
  
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
        <TabsList className="grid grid-cols-3 w-full max-w-md">
          <TabsTrigger value="reasoner">Reasoner</TabsTrigger>
          <TabsTrigger value="sparql">SPARQL</TabsTrigger>
          <TabsTrigger value="rules">Custom Rules</TabsTrigger>
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
                    variant="default"
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