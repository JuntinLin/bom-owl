// src/components/reasoning/ReasonerResults.tsx
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { CheckCircle, XCircle, AlertTriangle, Clock } from 'lucide-react';
import { ReasoningResult } from '@/types/tiptop';

interface ReasonerResultsProps {
  result: ReasoningResult;
}

const ReasonerResults: React.FC<ReasonerResultsProps> = ({ result }) => {
  const [activeTab, setActiveTab] = useState('summary');

  const renderStatus = () => {
    if (result.errorMessage) {
      return (
        <div className="flex items-center text-red-500">
          <XCircle className="h-5 w-5 mr-2" />
          <span>Failed</span>
        </div>
      );
    }

    if (!result.valid) {
      return (
        <div className="flex items-center text-yellow-500">
          <AlertTriangle className="h-5 w-5 mr-2" />
          <span>Invalid ontology</span>
        </div>
      );
    }

    return (
      <div className="flex items-center text-green-500">
        <CheckCircle className="h-5 w-5 mr-2" />
        <span>Success</span>
      </div>
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reasoning Results</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <p className="text-sm font-medium text-gray-500">Status</p>
            {renderStatus()}
          </div>
          <div>
            <p className="text-sm font-medium text-gray-500">Processing Time</p>
            <div className="flex items-center">
              <Clock className="h-4 w-4 mr-1 text-gray-500" />
              <span>{(result.processingTimeMs / 1000).toFixed(2)} seconds</span>
            </div>
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid grid-cols-4 mb-4">
            <TabsTrigger value="summary">Summary</TabsTrigger>
            <TabsTrigger value="validation">
              Validation
              {result.validationIssues.length > 0 && (
                <Badge variant="destructive" className="ml-2">
                  {result.validationIssues.length}
                </Badge>
              )}
            </TabsTrigger>
            <TabsTrigger value="inferred">
              Inferred Statements
              <Badge variant="outline" className="ml-2">
                {result.inferredStatements.length}
              </Badge>
            </TabsTrigger>
            <TabsTrigger value="subclasses">
              Subclasses
              <Badge variant="outline" className="ml-2">
                {result.inferredSubclasses.length}
              </Badge>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="summary">
            <div className="space-y-4">
              <div>
                <h3 className="text-lg font-medium mb-2">Reasoning Overview</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-gray-50 p-3 rounded-md">
                    <p className="text-sm font-medium text-gray-500">Reasoner Type</p>
                    <p className="font-medium">{result.reasonerType}</p>
                  </div>
                  <div className="bg-gray-50 p-3 rounded-md">
                    <p className="text-sm font-medium text-gray-500">Ontology Validity</p>
                    <p className="font-medium">{result.valid ? 'Valid' : 'Invalid'}</p>
                  </div>
                  <div className="bg-gray-50 p-3 rounded-md">
                    <p className="text-sm font-medium text-gray-500">Inferred Statements</p>
                    <p className="font-medium">{result.inferredStatements.length}</p>
                  </div>
                  <div className="bg-gray-50 p-3 rounded-md">
                    <p className="text-sm font-medium text-gray-500">Inferred Subclasses</p>
                    <p className="font-medium">{result.inferredSubclasses.length}</p>
                  </div>
                </div>
              </div>

              {result.errorMessage && (
                <div className="bg-red-50 border border-red-300 p-4 rounded-md">
                  <h3 className="text-red-700 font-medium mb-1">Error</h3>
                  <p className="text-red-600">{result.errorMessage}</p>
                </div>
              )}
            </div>
          </TabsContent>

          <TabsContent value="validation">
            {result.validationIssues.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-1/4">Type</TableHead>
                    <TableHead>Description</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.validationIssues.map((issue, index) => (
                    <TableRow key={index}>
                      <TableCell className="font-mono">{issue.type}</TableCell>
                      <TableCell>{issue.description}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <div className="text-center py-8">
                <CheckCircle className="h-12 w-12 mx-auto text-green-500 mb-3" />
                <p className="text-gray-600">No validation issues found. The ontology is valid.</p>
              </div>
            )}
          </TabsContent>

          <TabsContent value="inferred">
            {result.inferredStatements.length > 0 ? (
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
                    {result.inferredStatements.slice(0, 100).map((statement, index) => (
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
                {result.inferredStatements.length > 100 && (
                  <div className="text-center p-3 text-gray-500">
                    Showing first 100 statements of {result.inferredStatements.length}
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-8">
                <AlertTriangle className="h-12 w-12 mx-auto text-yellow-500 mb-3" />
                <p className="text-gray-600">No inferred statements generated.</p>
                <p className="text-gray-500 text-sm mt-2">
                  This could be because the ontology is already complete or the reasoner didn't find additional facts.
                </p>
              </div>
            )}
          </TabsContent>

          <TabsContent value="subclasses">
            {result.inferredSubclasses.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-1/2">Subclass</TableHead>
                    <TableHead className="w-1/2">Superclass</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.inferredSubclasses.map((relation, index) => (
                    <TableRow key={index}>
                      <TableCell className="font-mono text-xs truncate" title={relation.subclass}>
                        {formatResourceName(relation.subclass)}
                      </TableCell>
                      <TableCell className="font-mono text-xs truncate" title={relation.superclass}>
                        {formatResourceName(relation.superclass)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <div className="text-center py-8">
                <AlertTriangle className="h-12 w-12 mx-auto text-yellow-500 mb-3" />
                <p className="text-gray-600">No inferred subclass relationships discovered.</p>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
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

export default ReasonerResults;