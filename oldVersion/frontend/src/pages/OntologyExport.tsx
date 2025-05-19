// src/pages/OntologyExport.tsx
import { useState } from 'react';
import { tiptopService } from '@/services/tiptopService';
import { OntologyExportFormat } from '@/types/tiptop';
import { 
  Card, 
  CardContent, 
  CardHeader, 
  CardTitle, 
  CardDescription, 
  CardFooter 
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, Download, Code, Database, FileText } from "lucide-react";

const OntologyExport = () => {
  const [itemCode, setItemCode] = useState<string>('');
  const [format, setFormat] = useState<OntologyExportFormat>(OntologyExportFormat.RDF_XML);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string>('single');

  const handleSingleExport = async () => {
    if (!itemCode.trim()) {
      setError('Please enter an item code.');
      return;
    }

    try {
      setIsExporting(true);
      setError(null);
      setSuccess(null);

      // Export BOM ontology for a single item
      await tiptopService.exportBom(itemCode, format);
      
      setSuccess(`Export started for item ${itemCode}`);
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An unexpected error occurred during export.');
      }
    } finally {
      setIsExporting(false);
    }
  };

  const handleCompleteExport = async () => {
    try {
      setIsExporting(true);
      setError(null);
      setSuccess(null);

      // Export all BOMs
      await tiptopService.exportAllBoms(format);
      
      setSuccess('Complete BOM export started');
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An unexpected error occurred during export.');
      }
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Export Ontology</h1>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="mb-8">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="single">Single Item Export</TabsTrigger>
          <TabsTrigger value="complete">Complete Database Export</TabsTrigger>
        </TabsList>

        <TabsContent value="single">
          <Card>
            <CardHeader>
              <CardTitle>Export Bill of Materials as Ontology</CardTitle>
              <CardDescription>
                Export the BOM structure for a specific item to an ontology file.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {error && (
                <Alert variant="destructive" className="mb-4">
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {success && (
                <Alert className="mb-4">
                  <AlertTitle>Success</AlertTitle>
                  <AlertDescription>{success}</AlertDescription>
                </Alert>
              )}

              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="itemCode">Item Code</Label>
                  <Input
                    id="itemCode"
                    value={itemCode}
                    onChange={(e) => setItemCode(e.target.value)}
                    placeholder="Enter the master item code"
                  />
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="format">Export Format</Label>
                  <Select
                    value={format}
                    onValueChange={(value) => setFormat(value as OntologyExportFormat)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select format" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={OntologyExportFormat.RDF_XML}>OWL/RDF (XML)</SelectItem>
                      <SelectItem value={OntologyExportFormat.JSON_LD}>JSON-LD</SelectItem>
                      <SelectItem value={OntologyExportFormat.TURTLE}>Turtle</SelectItem>
                      <SelectItem value={OntologyExportFormat.N_TRIPLES}>N-Triples</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardContent>
            <CardFooter className="flex justify-end">
              <Button onClick={handleSingleExport} disabled={isExporting}>
                {isExporting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Exporting...
                  </>
                ) : (
                  <>
                    <Download className="mr-2 h-4 w-4" />
                    Export
                  </>
                )}
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

        <TabsContent value="complete">
          <Card>
            <CardHeader>
              <CardTitle>Export Complete BOM Database</CardTitle>
              <CardDescription>
                Export the entire BOM database as an ontology file. This might take some time for large databases.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {error && (
                <Alert variant="destructive" className="mb-4">
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {success && (
                <Alert className="mb-4">
                  <AlertTitle>Success</AlertTitle>
                  <AlertDescription>{success}</AlertDescription>
                </Alert>
              )}

              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="completeFormat">Export Format</Label>
                  <Select
                    value={format}
                    onValueChange={(value) => setFormat(value as OntologyExportFormat)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select format" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={OntologyExportFormat.RDF_XML}>OWL/RDF (XML)</SelectItem>
                      <SelectItem value={OntologyExportFormat.JSON_LD}>JSON-LD</SelectItem>
                      <SelectItem value={OntologyExportFormat.TURTLE}>Turtle</SelectItem>
                      <SelectItem value={OntologyExportFormat.N_TRIPLES}>N-Triples</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardContent>
            <CardFooter className="flex justify-end">
              <Button onClick={handleCompleteExport} disabled={isExporting}>
                {isExporting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Exporting...
                  </>
                ) : (
                  <>
                    <Database className="mr-2 h-4 w-4" />
                    Export All
                  </>
                )}
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>
      </Tabs>

      <Card>
        <CardHeader>
          <CardTitle>About Ontology Formats</CardTitle>
          <CardDescription>
            Learn about the different ontology export formats available
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-medium flex items-center">
                <FileText className="h-5 w-5 mr-2 text-indigo-500" />
                OWL/RDF (XML)
              </h3>
              <p className="text-sm text-gray-500">
                The standard Web Ontology Language format. Best for compatibility with ontology editors
                like Protégé and most semantic web tools.
              </p>
            </div>

            <div>
              <h3 className="text-lg font-medium flex items-center">
                <Code className="h-5 w-5 mr-2 text-green-500" />
                JSON-LD
              </h3>
              <p className="text-sm text-gray-500">
                JavaScript Object Notation for Linked Data. Best for web applications and
                integration with JavaScript-based systems.
              </p>
            </div>

            <div>
              <h3 className="text-lg font-medium flex items-center">
                <FileText className="h-5 w-5 mr-2 text-blue-500" />
                Turtle
              </h3>
              <p className="text-sm text-gray-500">
                Terse RDF Triple Language. A compact and readable syntax that is widely
                used in the semantic web community.
              </p>
            </div>

            <div>
              <h3 className="text-lg font-medium flex items-center">
                <FileText className="h-5 w-5 mr-2 text-purple-500" />
                N-Triples
              </h3>
              <p className="text-sm text-gray-500">
                A simple, line-based plain text format for RDF. Best for very large datasets
                and streaming processing.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default OntologyExport;