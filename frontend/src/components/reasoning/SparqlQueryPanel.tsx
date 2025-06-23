// src/components/reasoning/SparqlQueryPanel.tsx
import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { Spinner } from '@/components/ui/spinner';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import reasoningService from '@/services/reasoningService';
import { SparqlQueryResult, PredefinedQuery } from '@/types/tiptop';
import { Check, AlertTriangle, Play, FileQuestion } from 'lucide-react';

interface SparqlQueryPanelProps {
  masterItemCode: string;
}

const SparqlQueryPanel: React.FC<SparqlQueryPanelProps> = ({ masterItemCode }) => {
  const [query, setQuery] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [queryResult, setQueryResult] = useState<SparqlQueryResult | null>(null);
  const [predefinedQueries, setPredefinedQueries] = useState<PredefinedQuery[]>([]);
  const [selectedPredefined, setSelectedPredefined] = useState<string>('');

  useEffect(() => {
    // Fetch predefined queries when component mounts
    const fetchPredefinedQueries = async () => {
      try {
        const queries = await reasoningService.getPredefinedQueries();
        setPredefinedQueries(queries);
      } catch (err) {
        console.error('Error fetching predefined queries:', err);
        setError('Failed to load predefined queries');
      }
    };

    fetchPredefinedQueries();
  }, []);

  const handleQueryChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setQuery(e.target.value);
  };

  const handlePredefinedChange = (value: string) => {
    setSelectedPredefined(value);
    const selected = predefinedQueries.find(q => q.name === value);
    if (selected) {
      // Replace any placeholders in the query as needed
      let processedQuery = selected.query;
      
      // Replace COMPONENT_CODE placeholder with masterItemCode if present
      if (processedQuery.includes('COMPONENT_CODE')) {
        processedQuery = processedQuery.replace(/COMPONENT_CODE/g, masterItemCode);
      }
      
      // Replace search_term with a sample if present
      if (processedQuery.includes('search_term')) {
        processedQuery = processedQuery.replace(/search_term/g, 'oil');
      }
      
      setQuery(processedQuery);
    }
  };

  const executeQuery = async () => {
    if (!query.trim()) {
      setError('Query cannot be empty');
      return;
    }

    setLoading(true);
    setError(null);
    setQueryResult(null);

    try {
      const result = await reasoningService.executeSparqlQuery(masterItemCode, query);
      setQueryResult(result);
    } catch (err: any) {
      setError(err.message || 'Error executing SPARQL query');
      console.error('SPARQL query error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex justify-between items-center">
            <span>SPARQL Query Editor</span>
            <Button 
              onClick={executeQuery} 
              disabled={loading || !query.trim()} 
              size="sm" 
              className="flex items-center gap-1"
              variant={'outline'}
            >
              {loading ? <Spinner className="h-4 w-4" /> : <Play className="h-4 w-4" />}
              {loading ? 'Running...' : 'Execute Query'}
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">
                Predefined Queries
              </label>
              <Select 
                value={selectedPredefined} 
                onValueChange={handlePredefinedChange}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a predefined query" />
                </SelectTrigger>
                <SelectContent>
                  {predefinedQueries.map(query => (
                    <SelectItem key={query.name} value={query.name}>
                      {query.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {selectedPredefined && (
                <p className="text-sm text-gray-500 mt-1">
                  {predefinedQueries.find(q => q.name === selectedPredefined)?.description}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Query
              </label>
              <Textarea 
                value={query} 
                onChange={handleQueryChange} 
                placeholder="Enter SPARQL query..."
                className="font-mono h-80"
              />
            </div>

            {error && (
              <Alert variant="destructive">
                <AlertTitle>Error</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Query Results</CardTitle>
        </CardHeader>
        <CardContent>
          {queryResult ? (
            <div>
              <div className="flex items-center mb-4">
                <Badge variant="outline" className="text-base mr-2">
                  {queryResult.type}
                </Badge>
                {queryResult.type === 'SELECT' && queryResult.results && (
                  <span className="text-sm text-gray-500">
                    {queryResult.results.length} results
                  </span>
                )}
                {queryResult.type === 'ASK' && (
                  <span className={`flex items-center ${queryResult.result ? 'text-green-500' : 'text-red-500'}`}>
                    {queryResult.result ? (
                      <>
                        <Check className="h-4 w-4 mr-1" />
                        <span>True</span>
                      </>
                    ) : (
                      <>
                        <AlertTriangle className="h-4 w-4 mr-1" />
                        <span>False</span>
                      </>
                    )}
                  </span>
                )}
              </div>

              {queryResult.type === 'SELECT' && queryResult.results && (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        {queryResult.variables?.map((variable, index) => (
                          <TableHead key={index}>{variable}</TableHead>
                        ))}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {queryResult.results.map((row, rowIndex) => (
                        <TableRow key={rowIndex}>
                          {queryResult.variables?.map((variable, colIndex) => (
                            <TableCell key={colIndex} className="font-mono text-xs">
                              {formatValue(row[variable])}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}

              {(queryResult.type === 'CONSTRUCT' || queryResult.type === 'DESCRIBE') && queryResult.model && (
                <div>
                  <h3 className="text-sm font-medium mb-2">Model Output:</h3>
                  <pre className="bg-gray-50 p-4 rounded-md overflow-x-auto text-xs">
                    {queryResult.model}
                  </pre>
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-80 text-center">
              <FileQuestion className="h-16 w-16 text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-700 mb-2">No Query Results</h3>
              <p className="text-gray-500 max-w-md">
                Execute a SPARQL query to view its results here. You can select a predefined query
                or write your own to explore the BOM ontology.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

// Helper function to format SPARQL query result values
const formatValue = (value: string | null): string => {
  if (!value) return 'N/A';

  // Format URIs to show only the local name
  if (value.startsWith('http')) {
    const hashIndex = value.lastIndexOf('#');
    if (hashIndex !== -1) {
      return value.substring(hashIndex + 1);
    }
    
    const slashIndex = value.lastIndexOf('/');
    if (slashIndex !== -1) {
      return value.substring(slashIndex + 1);
    }
  }
  
  return value;
};


export default SparqlQueryPanel;