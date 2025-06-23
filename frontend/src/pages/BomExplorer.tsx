// src/pages/BomExplorer.tsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService';
import { BomTreeNode } from '@/types/tiptop';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { Badge } from '@/components/ui/badge';
import { ChevronRight, ChevronDown, FileText, Brain } from 'lucide-react';
import { toast } from 'sonner';

const BomExplorer = () => {
  const { masterItemCode } = useParams<{ masterItemCode: string }>();
  const navigate = useNavigate();

  const [bomTree, setBomTree] = useState<BomTreeNode | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());

  useEffect(() => {
    const fetchBomTree = async () => {
      if (!masterItemCode) return;

      setLoading(true);
      setError(null);

      try {
        const tree = await tiptopService.getBomTree(masterItemCode);
        setBomTree(tree);
      } catch (err: any) {
        console.error('Error fetching BOM tree:', err);
        setError(err.message || 'Failed to load BOM structure');
      } finally {
        setLoading(false);
      }
    };

    fetchBomTree();
  }, [masterItemCode]);

  const toggleNode = (nodeId: string) => {
    setExpandedNodes(prev => {
      const newSet = new Set(prev);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  };

  const exportBom = () => {
    if (!masterItemCode) return;
    
    try {
      tiptopService.exportBom(masterItemCode);
      toast.success('BOM export initiated');
    } catch (err) {
      console.error('Error exporting BOM:', err);
      toast.error('Failed to export BOM');
    }
  };

  const navigateToReasoning = () => {
    if (masterItemCode) {
      navigate(`/reasoning/${masterItemCode}`);
    }
  };

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

  if (!bomTree) {
    return (
      <Alert className="my-4">
        <AlertTitle>No BOM Found</AlertTitle>
        <AlertDescription>No Bill of Materials found for this item code.</AlertDescription>
      </Alert>
    );
  }

  // Recursive component to render tree nodes
  const renderTreeNode = (node: BomTreeNode, depth = 0, path = '') => {
    const nodeId = `${path}-${node.itemCode}`;
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedNodes.has(nodeId);
    
    return (
      <div key={nodeId} className="mb-2">
        <div 
          className={`flex items-start rounded-md border ${depth === 0 ? 'bg-blue-50 border-blue-200' : 'bg-white border-gray-200'} p-3`}
        >
          <div className="flex-1">
            <div className="flex items-center">
              {hasChildren && (
                <button 
                  onClick={() => toggleNode(nodeId)} 
                  className="mr-1 p-1 hover:bg-gray-100 rounded"
                >
                  {isExpanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>
              )}
              <span className="font-medium mr-2">{node.itemName || node.itemCode}</span>
              <Badge variant={depth === 0 ? "default" : "outline"}>
                {depth === 0 ? 'Master Item' : 'Component'}
              </Badge>
              {node.quantity && (
                <Badge variant="outline" className="ml-2">
                  Qty: {node.quantity}
                </Badge>
              )}
            </div>
            <div className="text-sm text-gray-500 mt-1 pl-6">
              <span className="font-mono">{node.itemCode}</span>
              {node.itemSpec && <span className="ml-2">• {node.itemSpec}</span>}
              {node.effectiveDate && (
                <span className="ml-2">• Effective: {formatDate(node.effectiveDate)}</span>
              )}
              {node.expiryDate && (
                <span className="ml-2">• Expires: {formatDate(node.expiryDate)}</span>
              )}
            </div>
          </div>
        </div>
        
        {hasChildren && isExpanded && (
          <div className="pl-8 mt-2 border-l-2 border-gray-200">
            {node.children?.map((child, index) => 
              renderTreeNode(child, depth + 1, `${nodeId}-${index}`)
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">BOM Explorer</h1>
        <div className="space-x-2">
          <Button 
            variant="outline" 
            onClick={() => navigate(`/items/view/${masterItemCode}`)}
          >
            <FileText className="h-4 w-4 mr-2" />
            View Item Details
          </Button>
          
          <Button 
            variant="secondary"
            onClick={exportBom}>
            Export as OWL
          </Button>
          
          <Button 
            variant="secondary"
            onClick={navigateToReasoning}
          >
            <Brain className="h-4 w-4 mr-2" />
            Reasoning
          </Button>
        </div>
      </div>
      
      <Card>
        <CardHeader>
          <CardTitle>Bill of Materials Structure</CardTitle>
        </CardHeader>
        <CardContent>
          {renderTreeNode(bomTree)}
        </CardContent>
      </Card>
    </div>
  );
};

// Helper function to format dates
const formatDate = (date: Date): string => {
  return new Date(date).toLocaleDateString();
};

export default BomExplorer;