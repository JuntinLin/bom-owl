// src/pages/BomViewer.tsx
import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService'; // Use the service instead of axios directly
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2 } from "lucide-react";
import { ImaFile, BomComponent, OntologyExportFormat } from '@/types/tiptop'; // Import types

const BomViewer = () => {
  const { itemCode } = useParams<{ itemCode: string }>();
  const [masterItem, setMasterItem] = useState<ImaFile | null>(null);
  const [components, setComponents] = useState<BomComponent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        if (!itemCode) {
          setError('No item code provided');
          return;
        }
        
        // Fetch master item details
        const masterItemData = await tiptopService.getMaterialByCode(itemCode);
        setMasterItem(masterItemData);        
        
        // Fetch components
        const componentsData = await tiptopService.getBomComponents(itemCode);
        setComponents(componentsData);
        
        setError(null);
      } catch (err) {
        console.error('Error fetching BOM data:', err);
        setError(err instanceof Error ? err.message : 'Failed to load BOM data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    if (itemCode) {
      fetchData();
    }
  }, [itemCode]);

  const formatDate = (dateString: string | null | undefined) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  };

  const handleExportOntology = (format: OntologyExportFormat) => {
    if (!itemCode) return;
    tiptopService.exportBom(itemCode, format);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="h-12 w-12 animate-spin text-indigo-600" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!masterItem) {
    return (
      <Alert>
        <AlertDescription>No item found with code: {itemCode}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Bill of Materials</h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => handleExportOntology(OntologyExportFormat.RDF_XML)}>
              Export as OWL
          </Button>
          <Button variant="outline" onClick={() => handleExportOntology(OntologyExportFormat.JSON_LD)}>
              Export as JSON-LD
          </Button>
          <Button variant="outline" onClick={() => handleExportOntology(OntologyExportFormat.TURTLE)}>
              Export as Turtle
          </Button>
        </div>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Master Item Details</CardTitle>
          <CardDescription>Information about the selected master item</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid md:grid-cols-3 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Item Code</p>
              <p className="text-lg font-semibold">{masterItem.ima01}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Item Name</p>
              <p className="text-lg">{masterItem.ima02 || '-'}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Specification</p>
              <p className="text-lg">{masterItem.ima021 || '-'}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Components</CardTitle>
          <CardDescription>
            {components.length === 0 
              ? 'No components found for this item.' 
              : `This item has ${components.length} component(s).`}
          </CardDescription>
        </CardHeader>
        
        {components.length > 0 && (
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Seq</TableHead>
                  <TableHead>Item Code</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Quantity</TableHead>
                  <TableHead>Effective Date</TableHead>
                  <TableHead>Expiry Date</TableHead>
                  <TableHead>Char. Code</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {components.map((component) => (
                  <TableRow key={`${component.componentItemCode}-${component.sequence}`}>
                    <TableCell>{component.sequence}</TableCell>
                    <TableCell className="font-medium">{component.componentItemCode}</TableCell>
                    <TableCell>
                      <div>
                        <p>{component.componentItemName || '-'}</p>
                        <p className="text-xs text-gray-500">{component.componentItemSpec || '-'}</p>
                      </div>
                    </TableCell>
                    <TableCell>{component.quantity?.toLocaleString() || '-'}</TableCell>
                    <TableCell>{formatDate(component.effectiveDate)}</TableCell>
                    <TableCell>{formatDate(component.expiryDate)}</TableCell>
                    <TableCell>
                      {component.characteristicCode && (
                        <Badge variant="outline">{component.characteristicCode}</Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <Link 
                        to={`/bom/${component.componentItemCode}`}
                        className="text-indigo-600 hover:text-indigo-900"
                      >
                        View BOM
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        )}
      </Card>
    </div>
  );
};

export default BomViewer;