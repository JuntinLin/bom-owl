// src/pages/ItemSearch.tsx
import { useState, FormEvent, ChangeEvent } from 'react';
import { Link } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService'; // Use the service instead of axios directly
import {
  Card,
  CardContent,
  CardHeader
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
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ImaFile } from '@/types/tiptop'; // Import types

interface SearchParams {
  code: string;
  name: string;
  spec: string;
}

const ItemSearch = () => {
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
  };

  const handleSearch = async (e: FormEvent) => {
    e.preventDefault();

    // Validate that at least one search parameter is provided
    if (!searchParams.code && !searchParams.name && !searchParams.spec) {
      setError('Please enter at least one search parameter');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSearched(true);

      if (searchParams.code) {
        // If code is provided, perform direct lookup
        const material = await tiptopService.getMaterialByCode(searchParams.code);
        setItems(material ? [material] : []);
      } else {
        // Otherwise, use the complex search endpoint
        const results = await tiptopService.searchItemsComplex(searchParams);
        setItems(results);
      }
    } catch (err) {
      console.error('Search error:', err);
      setError(err instanceof Error ? err.message : 'Failed to search items. Please try again.');
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Item Search</h1>

      <Card className="mb-8">
        <CardContent className="p-6">
          <form onSubmit={handleSearch}>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
                  Item Code
                </label>
                <Input
                  id="code"
                  name="code"
                  value={searchParams.code}
                  onChange={handleChange}
                  placeholder="Enter item code"
                />
              </div>
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                  Item Name
                </label>
                <Input
                  id="name"
                  name="name"
                  value={searchParams.name}
                  onChange={handleChange}
                  placeholder="Enter item name"
                />
              </div>
              <div>
                <label htmlFor="spec" className="block text-sm font-medium text-gray-700 mb-1">
                  Item Specification
                </label>
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
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            

            <div className="flex justify-end">
              <Button
                type="submit"
                disabled={loading}
                variant="outline"
                size="default"
                className="min-w-24"
              >
                {loading ? (
                  <>
                    <svg
                      className="mr-2 h-4 w-4 animate-spin"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                    Searching...
                  </>
                ) : (
                  'Search'
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {searched && (
        <Card>
          <CardHeader className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold">Search Results</h2>
            <p className="text-sm text-gray-500">
              {items.length === 0
                ? 'No items found matching your criteria.'
                : `Found ${items.length} item(s).`}
            </p>
          </CardHeader>

          {items.length > 0 && (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Item Code</TableHead>
                    <TableHead>Item Name</TableHead>
                    <TableHead>Specification</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map(item => (
                    <TableRow key={item.ima01}>
                      <TableCell className="font-medium">{item.ima01}</TableCell>
                      <TableCell>{item.ima02 || '-'}</TableCell>
                      <TableCell>{item.ima021 || '-'}</TableCell>
                      <TableCell>
                        <div className="flex space-x-2">
                          <Link
                            to={`/bom/${item.ima01}`}
                            className="text-indigo-600 hover:text-indigo-900"
                          >
                            View BOM
                          </Link>
                          <Link
                            to={`/bom-tree/${item.ima01}`}
                            className="text-green-600 hover:text-green-900"
                          >
                            BOM Tree
                          </Link>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};

export default ItemSearch;
