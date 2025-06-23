// src/pages/ExportPage.tsx
import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService';
import { ImaFile, OntologyExportFormat } from '@/types/tiptop';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Spinner } from '@/components/ui/spinner';
import { Switch } from '@/components/ui/switch';
import { toast } from 'sonner';
import { ArrowDownToLine, Search, FileDown, Database, FolderTree, Layers } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";

const ExportPage = () => {
  const [searchParams] = useSearchParams();
  const initialMasterItem = searchParams.get('masterItem') || '';

  const [masterItem, setMasterItem] = useState(initialMasterItem);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState<ImaFile[]>([]);
  const [selectedItem, setSelectedItem] = useState<string>(initialMasterItem);
  const [format, setFormat] = useState<string>(OntologyExportFormat.RDF_XML);
  const [exportType, setExportType] = useState<'single' | 'all'>('single');
  const [searching, setSearching] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [useSimpleExport, setUseSimpleExport] = useState(false);

  useEffect(() => {
    // If a master item was provided in the URL, set it as selected
    if (initialMasterItem) {
      setSelectedItem(initialMasterItem);
    }
  }, [initialMasterItem]);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  const handleItemInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setMasterItem(e.target.value);
    setSelectedItem(e.target.value);
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) return;

    setSearching(true);
    try {
      const results = await tiptopService.searchMaterials(searchTerm);
      setSearchResults(results);
    } catch (err) {
      console.error('Error searching items:', err);
      toast.error('Failed to search for items');
    } finally {
      setSearching(false);
    }
  };

  const handleSelectSearchResult = (item: ImaFile) => {
    setMasterItem(item.ima01);
    setSelectedItem(item.ima01);
    setSearchResults([]);
    setSearchTerm('');
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      if (exportType === 'single') {
        if (!selectedItem) {
          toast.error('Please select an item to export');
          return;
        }
        if (useSimpleExport) {
          // Export only direct components
          await tiptopService.exportSimpleBom(selectedItem, format as OntologyExportFormat);
          toast.success(`Simple export started for item ${selectedItem} (direct components only)`);
        } else {
          // Export complete hierarchy
          await tiptopService.exportBom(selectedItem, format as OntologyExportFormat);
          toast.success(`Complete hierarchy export started for item ${selectedItem}`);
        }
      } else {
        if (useSimpleExport) {
          // Export all BOMs with only direct components
          await tiptopService.exportAllSimpleBoms(format as OntologyExportFormat);
          toast.success('Simple export of all BOMs started (direct components only)');
        } else {
          // Export all BOMs with complete hierarchy
          await tiptopService.exportAllBoms(format as OntologyExportFormat);
          toast.success('Complete hierarchy export of all BOMs started');
        }
      }
    } catch (err) {
      console.error('Error exporting:', err);
      toast.error('Export failed');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="container mx-auto py-6">
      <h1 className="text-2xl font-bold mb-6">OWL Ontology Export</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Search className="h-5 w-5 mr-2" />
              Find Items to Export
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <Label htmlFor="search">Search for items</Label>
                <div className="flex mt-1">
                  <Input
                    id="search"
                    value={searchTerm}
                    onChange={handleSearchChange}
                    placeholder="Enter item name or spec"
                    className="rounded-r-none"
                  />
                  <Button
                    onClick={handleSearch}
                    disabled={!searchTerm.trim() || searching}
                    className="rounded-l-none"
                    variant={'outline'}
                    
                  >
                    {searching ? <Spinner className="h-4 w-4 mr-2" /> : null}
                    Search
                  </Button>
                </div>
              </div>

              {searchResults.length > 0 && (
                <div className="border rounded-md divide-y">
                  {searchResults.map((item) => (
                    <div
                      key={item.ima01}
                      className="p-3 hover:bg-gray-50 cursor-pointer"
                      onClick={() => handleSelectSearchResult(item)}
                    >
                      <div className="font-medium">{item.ima02}</div>
                      <div className="text-sm text-gray-500 flex justify-between">
                        <span className="font-mono">{item.ima01}</span>
                        <span>{item.ima021}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div>
                <Label htmlFor="itemCode">Item code</Label>
                <Input
                  id="itemCode"
                  value={masterItem}
                  onChange={handleItemInputChange}
                  placeholder="Enter item code directly"
                  className="mt-1"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <FileDown className="h-5 w-5 mr-2" />
              Export Options
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <Label>Export Type</Label>
                <RadioGroup
                  value={exportType}
                  onValueChange={(value) => setExportType(value as 'single' | 'all')}
                  className="mt-2"
                >
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="single" id="single" />
                    <Label htmlFor="single" className="cursor-pointer">
                      Single Item BOM
                    </Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="all" id="all" />
                    <Label htmlFor="all" className="cursor-pointer">
                      All BOMs
                    </Label>
                  </div>
                </RadioGroup>
              </div>

              <div>
                <Label htmlFor="format">Export Format</Label>
                <Select value={format} onValueChange={setFormat}>
                  <SelectTrigger id="format" className="mt-1">
                    <SelectValue placeholder="Select format" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={OntologyExportFormat.RDF_XML}>RDF/XML</SelectItem>
                    <SelectItem value={OntologyExportFormat.TURTLE}>Turtle</SelectItem>
                    <SelectItem value={OntologyExportFormat.JSON_LD}>JSON-LD</SelectItem>
                    <SelectItem value={OntologyExportFormat.N_TRIPLES}>N-Triples</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="pt-2">
                <Label htmlFor="hierarchyOption" className="mb-2 block">Hierarchy Option</Label>
                <div className="flex items-center justify-between bg-gray-50 p-3 rounded-md">
                  <div className="flex items-center">
                    {useSimpleExport ? (
                      <Layers className="h-5 w-5 mr-2 text-amber-600" />
                    ) : (
                      <FolderTree className="h-5 w-5 mr-2 text-green-600" />
                    )}
                    <span className="text-sm font-medium">
                      {useSimpleExport ? 'Direct Components Only' : 'Complete Hierarchy'}
                    </span>
                  </div>
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div>
                          <Switch
                            id="hierarchyOption"
                            checked={useSimpleExport}
                            onCheckedChange={setUseSimpleExport}
                          />
                        </div>
                      </TooltipTrigger>
                      <TooltipContent>
                        <p className="max-w-xs text-sm">
                          {useSimpleExport 
                            ? 'Only includes direct components (faster, smaller files)' 
                            : 'Includes all sub-components at all levels (more complete)'}
                        </p>
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                </div>
              </div>

              <div className="flex items-center pt-4">
                <Button
                  onClick={handleExport}
                  disabled={exporting || (exportType === 'single' && !selectedItem)}
                  className="w-full"
                  variant={'outline'}
                >
                  {exporting ? (
                    <Spinner className="h-4 w-4 mr-2" />
                  ) : (
                    <ArrowDownToLine className="h-4 w-4 mr-2" />
                  )}
                  {exportType === 'single'
                    ? 'Export Selected Item'
                    : 'Export All BOMs'}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Database className="h-5 w-5 mr-2" />
            About OWL Exports
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="prose max-w-none">
            <p>
              The OWL export feature converts Bill of Materials (BOM) data from TiptopERP into Web Ontology Language (OWL) 
              format. This conversion enables semantic representation of your product structures and supports advanced reasoning capabilities.
            </p>
            
            <h3 className="text-lg font-medium mt-4">Available Export Formats</h3>
            <ul className="list-disc pl-6 mt-2 space-y-1">
              <li><strong>RDF/XML</strong> - The standard OWL serialization format with good tool support</li>
              <li><strong>Turtle</strong> - A human-friendly, compact syntax that's easier to read</li>
              <li><strong>JSON-LD</strong> - JSON-based format optimal for web applications and API integrations</li>
              <li><strong>N-Triples</strong> - A line-based, plain text format that's simple to parse</li>
            </ul>

            <h3 className="text-lg font-medium mt-4">Hierarchy Options</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-2">
              <div className="border rounded-md p-3">
                <div className="flex items-center mb-1">
                  <FolderTree className="h-4 w-4 mr-2 text-green-600" />
                  <strong>Complete Hierarchy</strong>
                </div>
                <p className="text-sm">
                  Exports the entire BOM tree structure, including all sub-components at all levels recursively.
                  Recommended for semantic reasoning and complete product analysis.
                </p>
              </div>
              
              <div className="border rounded-md p-3">
                <div className="flex items-center mb-1">
                  <Layers className="h-4 w-4 mr-2 text-amber-600" />
                  <strong>Direct Components Only</strong>
                </div>
                <p className="text-sm">
                  Exports only direct components without their sub-components. Faster processing
                  and smaller file size, suitable for simpler visualization needs.
                </p>
              </div>
            </div>
            
            <h3 className="text-lg font-medium mt-4">What's Included in the Export</h3>
            <ul className="list-disc pl-6 mt-2 space-y-1">
              <li>Master items and component items as OWL classes and individuals</li>
              <li>BOM relationships with quantities and effective dates</li>
              <li>Item properties (code, name, specification)</li>
              <li>Semantic relationships between components</li>
            </ul>
            
            <h3 className="text-lg font-medium mt-4">Using the Exported Ontology</h3>
            <p className="mt-2">
              The exported OWL files can be used with ontology editors like Protégé, reasoners like HermiT or Pellet, or integrated 
              into semantic web applications. You can also use the reasoning features built into this application to perform analysis 
              directly on your exported ontologies.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ExportPage;