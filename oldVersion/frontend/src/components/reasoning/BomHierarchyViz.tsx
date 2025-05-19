// src/components/reasoning/BomHierarchyViz.tsx
import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { ChevronRight, ChevronDown, Info } from 'lucide-react';
import { 
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { InferredBomHierarchy, InferredComponentItem } from '@/types/tiptop';

interface BomHierarchyVizProps {
  bomHierarchy: InferredBomHierarchy;
}

const BomHierarchyViz: React.FC<BomHierarchyVizProps> = ({ bomHierarchy }) => {
  const [showDetails, setShowDetails] = useState<boolean>(false);
  
  if (!bomHierarchy) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">No BOM hierarchy information available.</p>
      </div>
    );
  }

  // Count inferred properties for the master item
  const masterInferredCount = Object.keys(bomHierarchy.inferredProperties || {}).length;

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-medium">BOM Structure with Inferences</h3>
        <Badge 
          variant="outline" 
          className="cursor-pointer" 
          onClick={() => setShowDetails(!showDetails)}
        >
          {showDetails ? 'Hide Details' : 'Show Details'}
        </Badge>
      </div>

      <Card className="border-2 border-blue-200">
        <CardContent className="p-4">
          <div className="flex items-start">
            <div className="flex-1">
              <div className="flex items-center">
                <span className="font-medium mr-2">{formatResourceName(bomHierarchy.uri)}</span>
                <Badge className="bg-blue-100 text-blue-800 hover:bg-blue-200">Master Item</Badge>
                {masterInferredCount > 0 && (
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <span className="ml-2 cursor-help">
                          <Info className="h-4 w-4 text-blue-500" />
                        </span>
                      </TooltipTrigger>
                      <TooltipContent>
                        <p>{masterInferredCount} inferred properties</p>
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                )}
              </div>
              <p className="text-sm text-gray-500 mt-1">Code: {bomHierarchy.code}</p>
            </div>
          </div>

          {showDetails && masterInferredCount > 0 && (
            <div className="mt-3 pl-6 border-l-2 border-gray-200">
              <p className="text-sm font-medium text-gray-700 mb-1">Inferred Properties:</p>
              <ul className="space-y-1">
                {Object.entries(bomHierarchy.inferredProperties).map(([key, values]) => (
                  <li key={key} className="text-xs">
                    <span className="font-medium">{formatResourceName(key)}:</span>{' '}
                    {values.map(formatResourceName).join(', ')}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Components list */}
          <div className="mt-4 pl-6 space-y-2">
            {bomHierarchy.components && bomHierarchy.components.length > 0 ? (
              bomHierarchy.components.map((component, index) => (
                <ComponentItem 
                  key={index} 
                  component={component} 
                  showDetails={showDetails}
                />
              ))
            ) : (
              <p className="text-gray-500 italic">No components found for this item.</p>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

interface ComponentItemProps {
  component: InferredComponentItem;
  showDetails: boolean;
}

const ComponentItem: React.FC<ComponentItemProps> = ({ component, showDetails }) => {
  const [isOpen, setIsOpen] = useState(false);
  
  // Count inferred properties for this component
  const hasInferredProps = component.inferredProperties && 
    Object.keys(component.inferredProperties).length > 0;
  
  return (
    <Card className="border border-gray-200">
      <CardContent className="p-3">
        <div className="flex items-start">
          <div className="flex-1">
            <div className="flex items-center">
              <span className="font-medium mr-2">{component.name || formatResourceName(component.uri)}</span>
              <Badge className="bg-green-100 text-green-800 hover:bg-green-200">Component</Badge>
              {component.quantity && (
                <Badge variant="outline" className="ml-2">
                  Qty: {component.quantity}
                </Badge>
              )}
              {hasInferredProps && (
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <span className="ml-2 cursor-help">
                        <Info className="h-4 w-4 text-blue-500" />
                      </span>
                    </TooltipTrigger>
                    <TooltipContent>
                      <p>{Object.keys(component.inferredProperties || {}).length} inferred properties</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              )}
            </div>
            <div className="text-sm text-gray-500 mt-1 space-x-2">
              <span>Code: {component.code}</span>
              {component.spec && <span>• Spec: {component.spec}</span>}
              {component.effectiveDate && <span>• Effective: {formatDate(component.effectiveDate)}</span>}
              {component.expiryDate && <span>• Expires: {formatDate(component.expiryDate)}</span>}
            </div>
          </div>
          
          {hasInferredProps && showDetails && (
            <Collapsible open={isOpen} onOpenChange={setIsOpen} className="w-full">
              <CollapsibleTrigger asChild>
                <button className="ml-2 p-1 hover:bg-gray-100 rounded-md">
                  {isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>
              </CollapsibleTrigger>
              <CollapsibleContent className="mt-2">
                <div className="pl-4 border-l-2 border-gray-200">
                  <p className="text-sm font-medium text-gray-700 mb-1">Inferred Properties:</p>
                  <ul className="space-y-1">
                    {Object.entries(component.inferredProperties || {}).map(([key, values]) => (
                      <li key={key} className="text-xs">
                        <span className="font-medium">{formatResourceName(key)}:</span>{' '}
                        {values.map(formatResourceName).join(', ')}
                      </li>
                    ))}
                  </ul>
                </div>
              </CollapsibleContent>
            </Collapsible>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Helper function to format resource URIs for better readability
const formatResourceName = (uri: string): string => {
  if (!uri) return 'Unknown';
  
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

// Helper function to format dates
const formatDate = (dateString: string | Date): string => {
  try {
    const date = typeof dateString === 'string' ? new Date(dateString) : dateString;
    return date.toLocaleDateString();
  } catch (e) {
    return String(dateString);
  }
};

export default BomHierarchyViz;