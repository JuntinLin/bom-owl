// src/components/reasoning/CustomRulesPanel.tsx
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
import { ExampleRule, CustomRuleResult } from '@/types/tiptop';
import { AlertTriangle, BookOpen, Zap } from 'lucide-react';

interface CustomRulesPanelProps {
  masterItemCode: string;
}

const CustomRulesPanel: React.FC<CustomRulesPanelProps> = ({ masterItemCode }) => {
  const [rules, setRules] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ruleResult, setRuleResult] = useState<CustomRuleResult | null>(null);
  const [exampleRules, setExampleRules] = useState<ExampleRule[]>([]);
  const [selectedExample, setSelectedExample] = useState<string>('');

  useEffect(() => {
    // Fetch example rules when component mounts
    const fetchExampleRules = async () => {
      try {
        const examples = await reasoningService.getExampleRules();
        setExampleRules(examples);
      } catch (err) {
        console.error('Error fetching example rules:', err);
        setError('Failed to load example rules');
      }
    };

    fetchExampleRules();
  }, []);

  const handleRulesChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setRules(e.target.value);
  };

  const handleExampleChange = (value: string) => {
    setSelectedExample(value);
    const selected = exampleRules.find(r => r.name === value);
    if (selected) {
      // Replace any placeholders in the rule
      let processedRule = selected.rule;
      setRules(processedRule);
    }
  };

  const applyRules = async () => {
    if (!rules.trim()) {
      setError('Rules cannot be empty');
      return;
    }

    setLoading(true);
    setError(null);
    setRuleResult(null);

    try {
      const result = await reasoningService.applyCustomRules(masterItemCode, rules);
      setRuleResult(result);
    } catch (err: any) {
      setError(err.message || 'Error applying custom rules');
      console.error('Rules application error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex justify-between items-center">
            <span>Custom Rules</span>
            <Button 
              onClick={applyRules} 
              disabled={loading || !rules.trim()} 
              size="sm" 
              className="flex items-center gap-1"
              variant="outline"
            >
              {loading ? <Spinner className="h-4 w-4" /> : <Zap className="h-4 w-4" />}
              {loading ? 'Applying...' : 'Apply Rules'}
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">
                Example Rules
              </label>
              <Select 
                value={selectedExample} 
                onValueChange={handleExampleChange}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select an example rule" />
                </SelectTrigger>
                <SelectContent>
                  {exampleRules.map(rule => (
                    <SelectItem key={rule.name} value={rule.name}>
                      {rule.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {selectedExample && (
                <p className="text-sm text-gray-500 mt-1">
                  {exampleRules.find(r => r.name === selectedExample)?.description}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Rules (Jena Rule Syntax)
              </label>
              <Textarea 
                value={rules} 
                onChange={handleRulesChange} 
                placeholder="Enter custom rules in Jena rule syntax..."
                className="font-mono h-80"
              />
              <p className="text-xs text-gray-500 mt-1">
                Format: [ruleName: (?subject ?predicate ?object) -&gt; (?subject rdf:type ?newType)]
              </p>
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
          <CardTitle>Rule Application Results</CardTitle>
        </CardHeader>
        <CardContent>
          {ruleResult ? (
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center">
                  <Badge variant="outline" className="text-base mr-2">
                    {ruleResult.appliedRules} rules applied
                  </Badge>
                  <span className="text-sm text-gray-500">
                    {ruleResult.inferredStatements.length} new statements inferred
                  </span>
                </div>
              </div>

              {ruleResult.inferredStatements.length > 0 ? (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-1/3">Subject</TableHead>
                        <TableHead className="w-1/3">Predicate</TableHead>
                        <TableHead className="w-1/3">Object</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {ruleResult.inferredStatements.map((statement, index) => (
                        <TableRow key={index}>
                          <TableCell className="font-mono text-xs truncate max-w-xs" title={statement.subject}>
                            {formatResourceName(statement.subject)}
                          </TableCell>
                          <TableCell className="font-mono text-xs truncate max-w-xs" title={statement.predicate}>
                            {formatResourceName(statement.predicate)}
                          </TableCell>
                          <TableCell className="font-mono text-xs truncate max-w-xs" title={statement.object}>
                            {formatResourceName(statement.object)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <div className="text-center p-6 bg-gray-50 rounded-md">
                  <AlertTriangle className="h-10 w-10 text-yellow-500 mx-auto mb-2" />
                  <p className="text-gray-700">No new statements were inferred.</p>
                  <p className="text-sm text-gray-500 mt-1">Try different rules or check your rule syntax.</p>
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-80 text-center">
              <BookOpen className="h-16 w-16 text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-700 mb-2">No Rule Results</h3>
              <p className="text-gray-500 max-w-md">
                Apply custom rules to the ontology to see the inferred statements here.
                You can select an example rule or write your own in Jena rule format.
              </p>
              <div className="mt-4 p-4 bg-gray-50 rounded-md w-full max-w-md text-left">
                <h4 className="font-medium text-sm mb-2">Rule Format Example:</h4>
                <pre className="text-xs font-mono text-gray-700 overflow-x-auto">
                  [CriticalComponent: <br />
                  (?component rdf:type &lt;http://www.jfc.com/tiptop/ontology#ComponentItem&gt;)<br />
                  (?bom &lt;http://www.jfc.com/tiptop/ontology#hasComponentItem&gt; ?component)<br />
                  -&gt;<br />
                  (?component rdf:type &lt;http://www.jfc.com/tiptop/ontology#CriticalComponent&gt;)]
                </pre>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

// Helper function to format resource URIs for better readability
const formatResourceName = (uri: string): string => {
  // If it's a full URI, extract the local name (after # or last /)
  if (uri.startsWith('http')) {
    const hashIndex = uri.lastIndexOf('#');
    if (hashIndex !== -1) {
      return uri.substring(hashIndex + 1);
    }
    
    const slashIndex = uri.lastIndexOf('/');
    if (slashIndex !== -1) {
      return uri.substring(slashIndex + 1);
    }
  }
  
  return uri;
};

export default CustomRulesPanel;