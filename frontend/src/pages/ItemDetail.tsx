// src/pages/ItemDetail.tsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService';
import { ImaFile, BomComponent } from '@/types/tiptop';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { BookOpen, FileText, Share2, Code, Brain, Database, Upload } from 'lucide-react';
import { useQuickExport } from '@/hooks/useKnowledgeBase';

// Knowledge Base Actions Component
const KnowledgeBaseActions = ({ masterItemCode, itemName }: { masterItemCode: string; itemName?: string }) => {
  const { quickExport, exporting } = useQuickExport();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center text-lg">
          <Database className="w-5 h-5 mr-2" />
          Knowledge Base Actions
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <Button
            onClick={() => quickExport(masterItemCode)}
            disabled={exporting}
            variant="outline"
            className="w-full"
          >
            {exporting ? (
              <>
                <Spinner className="mr-2 h-4 w-4 animate-spin" />
                Exporting...
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" />
                Export to Knowledge Base
              </>
            )}
          </Button>

          <Link to="/knowledge-base" className="block">
            <Button variant="ghost" className="w-full">
              <Database className="mr-2 h-4 w-4" />
              Manage Knowledge Base
            </Button>
          </Link>
        </div>

        <div className="mt-4 p-3 bg-muted rounded-md">
          <p className="text-xs text-muted-foreground">
            Export this BOM to the knowledge base for:
          </p>
          <ul className="text-xs text-muted-foreground mt-1 ml-4 list-disc">
            <li>AI-powered BOM generation</li>
            <li>Similarity searches</li>
            <li>Semantic reasoning</li>
          </ul>
        </div>
      </CardContent>
    </Card>
  );
};

const ItemDetail = () => {
  const { itemCode } = useParams<{ itemCode: string }>();
  const navigate = useNavigate();

  const [material, setMaterial] = useState<ImaFile | null>(null);
  const [components, setComponents] = useState<BomComponent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (!itemCode) return;

      setLoading(true);
      setError(null);

      try {
        // Fetch material details
        const materialData = await tiptopService.getMaterialByCode(itemCode);
        setMaterial(materialData);

        // Fetch BOM components if this is a master item
        try {
          const componentsData = await tiptopService.getBomComponents(itemCode);
          setComponents(componentsData);
        } catch (err) {
          // If it fails to get components, it's probably not a master item
          // This is not a critical error, so we just log it
          console.log('Not a master item or no components:', err);
          setComponents([]);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to fetch item details');
        console.error('Error fetching item details:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [itemCode]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive" className="my-4">
        <AlertTitle>Error</AlertTitle>
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!material) {
    return (
      <Alert className="my-4">
        <AlertTitle>Item Not Found</AlertTitle>
        <AlertDescription>The requested item could not be found.</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Item Details</h1>
        <div className="space-x-2">
          <Button variant="outline" onClick={() => navigate('/items')}>
            Back to Search
          </Button>

          {components.length > 0 && (
            <>
              <Button
                variant="outline"
                onClick={() => navigate(`/bom/${itemCode}`)}
                className="flex items-center gap-1"
              >
                <BookOpen className="h-4 w-4" />
                <span>View BOM</span>
              </Button>

              <Button
                variant="default"
                onClick={() => navigate(`/reasoning/${itemCode}`)}
                className="flex items-center gap-1"
              >
                <Brain className="h-4 w-4" />
                <span>Reasoning</span>
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center">
              <FileText className="h-5 w-5 mr-2" />
              Item Information
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Item Code</p>
                <p className="text-lg font-medium">{material.ima01}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Item Name</p>
                <p className="text-lg">{material.ima02}</p>
              </div>
              {material.ima021 && (
                <div className="md:col-span-2">
                  <p className="text-sm font-medium text-gray-500">Specification</p>
                  <p>{material.ima021}</p>
                </div>
              )}
              <div>
                <p className="text-sm font-medium text-gray-500">Type</p>
                <p>{material.ima09 || 'N/A'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Product Line</p>
                <p>{material.ima10 || 'N/A'}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Share2 className="h-5 w-5 mr-2" />
              BOM Status
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <p className="text-sm font-medium text-gray-500">BOM Type</p>
                <p>
                  {components.length > 0 ? (
                    <Badge className="bg-green-100 text-green-800 hover:bg-green-200">
                      Master Item
                    </Badge>
                  ) : (
                    <Badge className="bg-blue-100 text-blue-800 hover:bg-blue-200">
                      Component
                    </Badge>
                  )}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Components</p>
                <p className="text-2xl font-bold">{components.length}</p>
              </div>
              {components.length > 0 && (
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => navigate(`/export?masterItem=${itemCode}`)}
                >
                  <Code className="h-4 w-4 mr-2" />
                  Export as OWL
                </Button>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Knowledge Base Actions - Only show for master items */}
        {components.length > 0 && itemCode && (
          <KnowledgeBaseActions masterItemCode={itemCode} itemName={material.ima02} />
        )}
      </div>

      {components.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Bill of Materials Components</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Component Code</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Specification</TableHead>
                    <TableHead className="text-right">Quantity</TableHead>
                    <TableHead>Effective Date</TableHead>
                    <TableHead>Expiry Date</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {components.map((component, index) => (
                    <TableRow key={index}>
                      <TableCell className="font-medium">{component.componentItemCode}</TableCell>
                      <TableCell>{component.componentItemName}</TableCell>
                      <TableCell>{component.componentItemSpec}</TableCell>
                      <TableCell className="text-right">{component.quantity}</TableCell>
                      <TableCell>{formatDate(component.effectiveDate)}</TableCell>
                      <TableCell>{formatDate(component.expiryDate)}</TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => navigate(`/items/view/${component.componentItemCode}`)}
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

// Helper function to format dates
const formatDate = (dateString?: string): string => {
  if (!dateString) return 'N/A';

  try {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  } catch (e) {
    return dateString;
  }
};

export default ItemDetail;