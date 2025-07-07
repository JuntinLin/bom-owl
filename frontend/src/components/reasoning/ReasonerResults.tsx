// src/components/reasoning/ReasonerResults.tsx
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Clock, 
  Info,
  Database,
  GitBranch,
  Layers,
  BarChart3,
  Zap
} from 'lucide-react';
import { ReasoningResult } from '@/types/tiptop';
import ReasoningMetrics from './ReasoningMetrics';

interface ReasonerResultsProps {
  result: ReasoningResult;
}

const ReasonerResults: React.FC<ReasonerResultsProps> = ({ result }) => {
  const [activeTab, setActiveTab] = useState('summary');

  // Handle the new isValid field structure
  const isValid = result.isValid === true || result.isValid === 'skipped';
  const validationSkipped = result.isValid === 'skipped';

  const renderStatus = () => {
    if (result.error || result.reasoningError) {
      return (
        <div className="flex items-center text-red-500">
          <XCircle className="h-5 w-5 mr-2" />
          <span>Failed</span>
        </div>
      );
    }

    if (result.reasoningTimeout) {
      return (
        <div className="flex items-center text-orange-500">
          <Clock className="h-5 w-5 mr-2" />
          <span>Timeout (Fallback Used)</span>
        </div>
      );
    }

    if (!isValid && !validationSkipped) {
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

  const getProcessingTime = () => {
    // No processingTimeMs in new structure, but we can indicate if it was slow
    if (result.reasoningTimeout) {
      return "Timed out";
    }
    return "Completed";
  };

  return (
    <div className="space-y-4">
      {/* Status Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            Reasoning Results
            {result.reasonerUsed && (
              <Badge variant={result.reasonerUsed.includes('fallback') ? 'secondary' : 'default'}>
                {result.reasonerUsed}
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Status</p>
              {renderStatus()}
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Model Size</p>
              <div className="flex items-center">
                <Database className="h-4 w-4 mr-1 text-gray-500" />
                <span>{result.modelSize?.toLocaleString() || 'N/A'} statements</span>
              </div>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Validation</p>
              <div className="flex items-center">
                {validationSkipped ? (
                  <>
                    <Info className="h-4 w-4 mr-1 text-blue-500" />
                    <span>Skipped</span>
                  </>
                ) : isValid ? (
                  <>
                    <CheckCircle className="h-4 w-4 mr-1 text-green-500" />
                    <span>Valid</span>
                  </>
                ) : (
                  <>
                    <XCircle className="h-4 w-4 mr-1 text-red-500" />
                    <span>Invalid</span>
                  </>
                )}
              </div>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Inferences</p>
              <div className="flex items-center">
                <Zap className="h-4 w-4 mr-1 text-yellow-500" />
                <span>{result.inferredCount || result.inferredStatements?.length || 0}</span>
              </div>
            </div>
          </div>

          {/* Warnings */}
          {result.warnings && result.warnings.length > 0 && (
            <Alert className="mb-4">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>Warnings</AlertTitle>
              <AlertDescription>
                <ul className="list-disc list-inside mt-2">
                  {result.warnings.map((warning, index) => (
                    <li key={index} className="text-sm">{warning}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}

          {/* Error Messages */}
          {(result.error || result.reasoningError) && (
            <Alert variant="destructive">
              <XCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>
                {result.error || result.reasoningError}
                {result.errorType && (
                  <div className="text-xs mt-1">Type: {result.errorType}</div>
                )}
              </AlertDescription>
            </Alert>
          )}

          {/* Validation Note */}
          {validationSkipped && result.validationNote && (
            <Alert>
              <Info className="h-4 w-4" />
              <AlertDescription>{result.validationNote}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* Reasoning Metrics */}
      {result.reasoningMetrics && (
        <ReasoningMetrics 
          metrics={result.reasoningMetrics}
          modelSize={result.modelSize}
          reasonerUsed={result.reasonerUsed}
        />
      )}

      {/* Results Tabs */}
      <Card>
        <CardContent className="pt-6">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="grid grid-cols-4 mb-4">
              <TabsTrigger value="summary">Summary</TabsTrigger>
              <TabsTrigger value="validation">
                Validation
                {result.validationIssues && result.validationIssues.length > 0 && (
                  <Badge variant="destructive" className="ml-2">
                    {result.validationIssues.length}
                  </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="inferred">
                Inferred
                <Badge variant="outline" className="ml-2">
                  {result.inferredCount || result.inferredStatements?.length || 0}
                </Badge>
              </TabsTrigger>
              <TabsTrigger value="subclasses">
                Subclasses
                <Badge variant="outline" className="ml-2">
                  {result.inferredSubclasses?.length || 0}
                </Badge>
              </TabsTrigger>
            </TabsList>

            <TabsContent value="summary">
              <div className="space-y-4">
                <div>
                  <h3 className="text-lg font-medium mb-2">Reasoning Overview</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-gray-50 p-3 rounded-md">
                      <p className="text-sm font-medium text-gray-500">Requested Reasoner</p>
                      <p className="font-medium">{result.reasonerUsed?.includes('fallback') ? 
                        result.reasonerUsed.split(' ')[0] : 
                        result.reasonerUsed || 'N/A'}</p>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-md">
                      <p className="text-sm font-medium text-gray-500">Actual Reasoner Used</p>
                      <p className="font-medium">{result.reasonerUsed || 'N/A'}</p>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-md">
                      <p className="text-sm font-medium text-gray-500">Ontology Validity</p>
                      <p className="font-medium">
                        {validationSkipped ? 'Skipped' : isValid ? 'Valid' : 'Invalid'}
                      </p>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-md">
                      <p className="text-sm font-medium text-gray-500">Total Inferences</p>
                      <p className="font-medium">{result.inferredCount || result.inferredStatements?.length || 0}</p>
                    </div>
                  </div>
                </div>

                {/* BOM Hierarchy Summary */}
                {result.bomHierarchy && (
                  <div className="bg-blue-50 p-4 rounded-md">
                    <h4 className="font-medium mb-2">BOM Hierarchy</h4>
                    <div className="text-sm space-y-1">
                      <p>Master Item: <span className="font-mono">{result.bomHierarchy.code}</span></p>
                      {result.bomHierarchy.isHydraulicCylinder && (
                        <Badge>Hydraulic Cylinder</Badge>
                      )}
                      {result.bomHierarchy.components && (
                        <p>Components: {result.bomHierarchy.components.length}</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </TabsContent>

            <TabsContent value="validation">
              {result.validationIssues && result.validationIssues.length > 0 ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-1/3">Type</TableHead>
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
                  {validationSkipped ? (
                    <>
                      <Info className="h-12 w-12 mx-auto text-blue-500 mb-3" />
                      <p className="text-gray-600">Validation was skipped for performance optimization.</p>
                      <p className="text-gray-500 text-sm mt-2">{result.validationNote}</p>
                    </>
                  ) : (
                    <>
                      <CheckCircle className="h-12 w-12 mx-auto text-green-500 mb-3" />
                      <p className="text-gray-600">No validation issues found. The ontology is valid.</p>
                    </>
                  )}
                </div>
              )}
            </TabsContent>

            <TabsContent value="inferred">
              {result.inferredStatements && result.inferredStatements.length > 0 ? (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-1/4">Subject</TableHead>
                        <TableHead className="w-1/4">Predicate</TableHead>
                        <TableHead className="w-1/4">Object</TableHead>
                        <TableHead className="w-1/4">Category</TableHead>
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
                          <TableCell>
                            {statement.category && (
                              <Badge variant="outline" className="text-xs">
                                {statement.category}
                              </Badge>
                            )}
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
              {result.inferredSubclasses && result.inferredSubclasses.length > 0 ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-1/3">Subclass</TableHead>
                      <TableHead className="w-1/3">Superclass</TableHead>
                      <TableHead className="w-1/3">Confidence</TableHead>
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
                        <TableCell>
                          {relation.confidence && (
                            <Badge variant="outline" className="text-xs">
                              {relation.confidence}
                            </Badge>
                          )}
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

      {/* Hydraulic Cylinder Inferences */}
      {result.hydraulicCylinderInferences && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              Hydraulic Cylinder Analysis
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="specs" className="w-full">
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="specs">Specifications</TabsTrigger>
                <TabsTrigger value="suggestions">Component Suggestions</TabsTrigger>
                <TabsTrigger value="similar">Similar Cylinders</TabsTrigger>
              </TabsList>

              <TabsContent value="specs" className="mt-4">
                {result.hydraulicCylinderInferences.specifications ? (
                  <div className="grid grid-cols-2 gap-3">
                    {Object.entries(result.hydraulicCylinderInferences.specifications).map(([key, value]) => (
                      <div key={key} className="bg-gray-50 p-3 rounded-md">
                        <p className="text-sm font-medium text-gray-500 capitalize">
                          {key.replace(/([A-Z])/g, ' $1').trim()}
                        </p>
                        <p className="font-medium">{value}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-4">No specifications extracted</p>
                )}
              </TabsContent>

              <TabsContent value="suggestions" className="mt-4">
                {result.hydraulicCylinderInferences.componentSuggestions && 
                 Object.keys(result.hydraulicCylinderInferences.componentSuggestions).length > 0 ? (
                  <div className="space-y-4">
                    {Object.entries(result.hydraulicCylinderInferences.componentSuggestions).map(([category, suggestions]) => (
                      <div key={category}>
                        <h5 className="font-medium mb-2">{category}</h5>
                        <div className="space-y-2">
                          {(suggestions as any[]).slice(0, 3).map((sugg, idx) => (
                            <div key={idx} className="p-2 bg-gray-50 rounded text-sm">
                              <div className="flex justify-between items-center">
                                <span className="font-medium">{sugg.name}</span>
                                <Badge variant="outline" className="text-xs">
                                  {(sugg.confidenceScore * 100).toFixed(0)}%
                                </Badge>
                              </div>
                              <div className="text-xs text-gray-600 mt-1">
                                {sugg.code} - {sugg.compatibilityReason}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-4">No component suggestions available</p>
                )}
              </TabsContent>

              <TabsContent value="similar" className="mt-4">
                {result.hydraulicCylinderInferences.similarCylinders && 
                 result.hydraulicCylinderInferences.similarCylinders.length > 0 ? (
                  <div className="space-y-2">
                    {result.hydraulicCylinderInferences.similarCylinders.slice(0, 5).map((cyl, idx) => (
                      <div key={idx} className="p-3 bg-gray-50 rounded-md">
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-medium">{cyl.name || cyl.code}</span>
                          <Badge>{cyl.similarityScore}% match</Badge>
                        </div>
                        {cyl.similarityReasons && cyl.similarityReasons.length > 0 && (
                          <div className="text-xs text-gray-600">
                            {cyl.similarityReasons.join(' • ')}
                          </div>
                        )}
                        {cyl.componentCount !== undefined && (
                          <div className="text-xs text-gray-500 mt-1">
                            {cyl.componentCount} components
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-4">No similar cylinders found</p>
                )}
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      )}
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

export default ReasonerResults;