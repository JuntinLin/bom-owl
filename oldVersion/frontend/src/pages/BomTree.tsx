// src/pages/BomTree.tsx
import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { BomTreeNode } from '@/types/tiptop';
import { tiptopService } from '@/services/tiptopService';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Loader2, FileDown, ChevronRight, ChevronDown, ZoomIn, ZoomOut } from "lucide-react";

interface TreeNodeProps {
  node: BomTreeNode;
  level: number;
  expanded: Record<string, boolean>;
  toggleExpand: (id: string) => void;
}

const TreeNode = ({ node, level, expanded, toggleExpand }: TreeNodeProps) => {
  const hasChildren = node.children && node.children.length > 0;
  const nodeId = `${node.itemCode}-${level}`;
  const isExpanded = expanded[nodeId];
  
  const formatDate = (date?: Date) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString();
  };

  return (
    <div className="mt-1">
      <div 
        className={`flex items-start p-2 hover:bg-gray-50 rounded-md ${level === 0 ? 'bg-indigo-50' : ''}`}
        style={{ marginLeft: `${level * 20}px` }}
      >
        <div className="mr-2 mt-1">
          {hasChildren ? (
            <button 
              onClick={() => toggleExpand(nodeId)}
              className="text-gray-500 hover:text-indigo-500"
            >
              {isExpanded ? 
                <ChevronDown className="h-4 w-4" /> : 
                <ChevronRight className="h-4 w-4" />
              }
            </button>
          ) : (
            <div className="w-4"></div>
          )}
        </div>
        
        <div className="flex-1">
          <div className="flex items-center justify-between">
            <div>
              <span className="font-medium">{node.itemCode}</span>
              <span className="ml-2 text-gray-500">{node.itemName || ''}</span>
              {node.characteristicCode && (
                <Badge variant="outline" className="ml-2">{node.characteristicCode}</Badge>
              )}
            </div>
            
            {node.quantity && (
              <div className="text-sm text-gray-600">
                Qty: {node.quantity.toString()}
              </div>
            )}
          </div>
          
          {node.itemSpec && (
            <div className="text-xs text-gray-500 mt-1">{node.itemSpec}</div>
          )}
          
          {(node.effectiveDate || node.expiryDate) && (
            <div className="text-xs text-gray-500 mt-1">
              {node.effectiveDate && (
                <span>From: {formatDate(node.effectiveDate)}</span>
              )}
              {node.expiryDate && (
                <span className="ml-2">To: {formatDate(node.expiryDate)}</span>
              )}
            </div>
          )}
        </div>
        
        <div className="ml-2">
          <Link to={`/bom/${node.itemCode}`}>
            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
              <ZoomIn className="h-4 w-4" />
            </Button>
          </Link>
        </div>
      </div>
      
      {isExpanded && hasChildren && (
        <div>
          {node.children?.map((child, index) => (
            <TreeNode
              key={`${child.itemCode}-${index}`}
              node={child}
              level={level + 1}
              expanded={expanded}
              toggleExpand={toggleExpand}
            />
          ))}
        </div>
      )}
    </div>
  );
};

const BomTree = () => {
  const { itemCode } = useParams<{ itemCode: string }>();
  const [bomTree, setBomTree] = useState<BomTreeNode | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [zoomLevel, setZoomLevel] = useState<number>(100);

  useEffect(() => {
    const fetchBomTree = async () => {
      if (!itemCode) return;
      
      try {
        setLoading(true);
        setError(null);
        
        const tree = await tiptopService.getBomTree(itemCode);
        setBomTree(tree);
        
        // Initialize expansion state for root node
        setExpanded({
          [`${tree.itemCode}-0`]: true
        });
      } catch (err) {
        console.error('Error fetching BOM tree:', err);
        if (err instanceof Error) {
          setError(err.message);
        } else {
          setError('An unexpected error occurred');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchBomTree();
  }, [itemCode]);

  const toggleExpand = (nodeId: string) => {
    setExpanded(prev => ({
      ...prev,
      [nodeId]: !prev[nodeId]
    }));
  };

  const expandAll = () => {
    // Recursively create expansion state for all nodes
    const expandAllNodes = (node: BomTreeNode, level: number, state: Record<string, boolean>) => {
      const nodeId = `${node.itemCode}-${level}`;
      state[nodeId] = true;
      
      if (node.children && node.children.length > 0) {
        node.children.forEach(child => {
          expandAllNodes(child, level + 1, state);
        });
      }
      
      return state;
    };
    
    if (bomTree) {
      const newExpandedState = expandAllNodes(bomTree, 0, {});
      setExpanded(newExpandedState);
    }
  };

  const collapseAll = () => {
    // Only keep root node expanded
    if (bomTree) {
      setExpanded({
        [`${bomTree.itemCode}-0`]: true
      });
    }
  };

  const increaseZoom = () => {
    setZoomLevel(prev => Math.min(prev + 10, 150));
  };

  const decreaseZoom = () => {
    setZoomLevel(prev => Math.max(prev - 10, 70));
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

  if (!bomTree) {
    return (
      <Alert>
        <AlertDescription>No BOM tree found for item code: {itemCode}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">BOM Tree</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={expandAll}>
            Expand All
          </Button>
          <Button variant="outline" size="sm" onClick={collapseAll}>
            Collapse All
          </Button>
          <Button variant="outline" size="sm" onClick={decreaseZoom}>
            <ZoomOut className="h-4 w-4" />
          </Button>
          <Button variant="outline" size="sm" onClick={increaseZoom}>
            <ZoomIn className="h-4 w-4" />
          </Button>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={() => tiptopService.exportBom(itemCode || '')}
          >
            <FileDown className="h-4 w-4 mr-1" />
            Export
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Bill of Materials Tree</CardTitle>
          <CardDescription>
            Hierarchical view of components for {bomTree.itemCode} - {bomTree.itemName}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div style={{ zoom: `${zoomLevel}%` }}>
            <TreeNode
              node={bomTree}
              level={0}
              expanded={expanded}
              toggleExpand={toggleExpand}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default BomTree;