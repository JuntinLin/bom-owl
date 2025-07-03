// src/pages/ItemSearch.tsx
import { useState, FormEvent, ChangeEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { ImaFile } from '@/types/tiptop';
import { toast } from 'sonner';

// Icons
import {
  Search,
  Eye,
  GitBranch,
  Package,
  AlertCircle,
  Brain,
  Database
} from 'lucide-react';

interface SearchParams {
  code: string;
  name: string;
  spec: string;
}

const ItemSearch = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useState<SearchParams>({
    code: '',
    name: '',
    spec: ''
  });
  const [items, setItems] = useState<ImaFile[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState<boolean>(false);

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setSearchParams(prev => ({ ...prev, [name]: value }));
    // Clear error when user starts typing
    if (error) setError(null);
  };

  const handleSearch = async (e: FormEvent) => {
    e.preventDefault();

    // Validate that at least one search parameter is provided
    if (!searchParams.code && !searchParams.name && !searchParams.spec) {
      setError('Please enter at least one search parameter');
      toast.error('Please enter at least one search parameter');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSearched(true);

      let results: ImaFile[] = [];

      if (searchParams.code) {
        // If code is provided, perform direct lookup
        try {
          const material = await tiptopService.getMaterialByCode(searchParams.code);
          results = material ? [material] : [];
        } catch (err) {
          // If direct lookup fails, fall back to complex search
          console.warn('Direct lookup failed, trying complex search', err);
          results = await tiptopService.searchItemsComplex(searchParams);
        }
      } else {
        // Otherwise, use the complex search endpoint
        results = await tiptopService.searchItemsComplex(searchParams);
      }

      setItems(results);
      
      if (results.length === 0) {
        toast.info('No items found matching your criteria');
      } else {
        toast.success(`Found ${results.length} item${results.length > 1 ? 's' : ''}`);
      }
    } catch (err) {
      console.error('Search error:', err);
      const errorMessage = err instanceof Error ? err.message : 'Failed to search items. Please try again.';
      setError(errorMessage);
      toast.error(errorMessage);
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSearchParams({
      code: '',
      name: '',
      spec: ''
    });
    setItems([]);
    setError(null);
    setSearched(false);
  };

  const isHydraulicCylinder = (itemCode: string): boolean => {
    return itemCode && itemCode.length >= 2 && (itemCode.startsWith('3') || itemCode.startsWith('4'));
  };

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Item Search</h1>
        <p className="text-gray-600 mt-1">Search for materials in the ERP system</p>
      </div>

      <Card className="mb-8">
        <CardHeader>
          <CardTitle>Search Criteria</CardTitle>
          <CardDescription>
            Enter at least one search parameter to find items
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch}>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div className="space-y-2">
                <Label htmlFor="code">Item Code</Label>
                <Input
                  id="code"
                  name="code"
                  value={searchParams.code}
                  onChange={handleChange}
                  placeholder="e.g., 312F050-0146Y"
                  className="font-mono"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name">Item Name</Label>
                <Input
                  id="name"
                  name="name"
                  value={searchParams.name}
                  onChange={handleChange}
                  placeholder="Enter item name"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="spec">Item Specification</Label>
                <Input
                  id="spec"
                  name="spec"
                  value={searchParams.spec}
                  onChange={handleChange}
                  placeholder="Enter specification"
                />
              </div>
            </div>

            {error && (
              <Alert variant="destructive" className="mb-4">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Error</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={handleReset}
                disabled={loading}
              >
                Reset
              </Button>
              <Button
                type="submit"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <Spinner className="mr-2 h-4 w-4" />
                    Searching...
                  </>
                ) : (
                  <>
                    <Search className="mr-2 h-4 w-4" />
                    Search
                  </>
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {searched && (
        <Card>
          <CardHeader>
            <CardTitle>Search Results</CardTitle>
            <CardDescription>
              {items.length === 0
                ? 'No items found matching your criteria.'
                : `Found ${items.length} item${items.length > 1 ? 's' : ''}.`}
            </CardDescription>
          </CardHeader>

          {items.length > 0 && (
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[200px]">Item Code</TableHead>
                      <TableHead>Item Name</TableHead>
                      <TableHead>Specification</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {items.map(item => (
                      <TableRow key={item.ima01}>
                        <TableCell className="font-mono">
                          <div className="flex items-center gap-2">
                            {item.ima01}
                            {isHydraulicCylinder(item.ima01) && (
                              <Badge variant="secondary" className="text-xs">
                                <Package className="h-3 w-3 mr-1" />
                                Hydraulic
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>{item.ima02 || '-'}</TableCell>
                        <TableCell className="max-w-[300px] truncate" title={item.ima021 || '-'}>
                          {item.ima021 || '-'}
                        </TableCell>
                        <TableCell>
                          <div className="flex justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/items/view/${item.ima01}`)}
                              title="View Details"
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/bom/${item.ima01}`)}
                              title="View BOM"
                            >
                              <Package className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/bom-tree/${item.ima01}`)}
                              title="BOM Tree"
                            >
                              <GitBranch className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/reasoning/${item.ima01}`)}
                              title="Reasoning"
                            >
                              <Brain className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => navigate(`/knowledge-base`)}
                              title="Knowledge Base"
                            >
                              <Database className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          )}
        </Card>
      )}
    </div>
  );
};

export default ItemSearch;