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

interface HydraulicCylinderProps {
  bore?: string;
  stroke?: string;
  rodEndType?: string;
  series?: string;
  type?: string;
  installation?: string;
  shaftEndJoin?: string;
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

  // Check if this is a hydraulic cylinder (code starts with 3 or 4)
  const isHydraulicCylinder = bomHierarchy.code.startsWith('3') || bomHierarchy.code.startsWith('4');

  // Extract hydraulic cylinder specific properties
  const cylinderProps = isHydraulicCylinder ? extractHydraulicCylinderProps(bomHierarchy.inferredProperties) : null;


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
                {isHydraulicCylinder && (
                  <Badge className="bg-amber-100 text-amber-800 hover:bg-amber-200">Hydraulic Cylinder</Badge>
                )}
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
              {/* Display hydraulic cylinder specific properties if available */}
              {isHydraulicCylinder && cylinderProps && (
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm">
                  {cylinderProps.bore && (
                    <span className="text-gray-700">
                      <span className="font-medium">Bore:</span> {cylinderProps.bore}mm
                    </span>
                  )}
                  {cylinderProps.stroke && (
                    <span className="text-gray-700">
                      <span className="font-medium">Stroke:</span> {cylinderProps.stroke}mm
                    </span>
                  )}
                  {cylinderProps.series && (
                    <span className="text-gray-700">
                      <span className="font-medium">Series:</span> {cylinderProps.series}
                    </span>
                  )}
                  {cylinderProps.type && (
                    <span className="text-gray-700">
                      <span className="font-medium">Type:</span> {cylinderProps.type}
                    </span>
                  )}
                  {cylinderProps.rodEndType && (
                    <span className="text-gray-700">
                      <span className="font-medium">Rod End:</span> {cylinderProps.rodEndType}
                    </span>
                  )}
                  {cylinderProps.installation && (
                    <span className="text-gray-700">
                      <span className="font-medium">Installation:</span> {cylinderProps.installation}
                    </span>
                  )}
                  {cylinderProps.shaftEndJoin && (
                    <span className="text-gray-700">
                      <span className="font-medium">Shaft Join:</span> {cylinderProps.shaftEndJoin}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>

          {showDetails && masterInferredCount > 0 && (
            <div className="mt-3 pl-6 border-l-2 border-gray-200">
              <p className="text-sm font-medium text-gray-700 mb-1">Inferred Properties:</p>
              <ul className="space-y-1">
              {Object.entries(bomHierarchy.inferredProperties).map(([key, values]) => {
                  // Skip hydraulic-specific properties if already shown
                  if (isHydraulicCylinder && isHydraulicProperty(key)) {
                    return null;
                  }
                  return (
                    <li key={key} className="text-xs">
                      <span className="font-medium">{formatResourceName(key)}:</span>{' '}
                      {values.map(formatResourceName).join(', ')}
                    </li>
                  );
                })}
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

  // Check for installation or shaft end join properties in component code
  const hasSpecialInstallation = component.code && component.code.length >= 5 && 
    ["201", "202", "203", "206", "207", "208"].includes(component.code.substring(2, 5));
  
  const hasSpecialShaftEndJoin = component.code && component.code.length >= 5 && 
    ["209", "210", "211"].includes(component.code.substring(2, 5));
  
  // Determine installation type
  let installationType = "";
  if (hasSpecialInstallation && component.code) {
    const code = component.code.substring(2, 5);
    switch(code) {
      case "201": installationType = "CA"; break;
      case "202": installationType = "CB"; break;
      case "203": installationType = "FA"; break;
      case "206": installationType = "TC"; break;
      case "207": installationType = "LA"; break;
      case "208": installationType = "LB"; break;
    }
  }
  
  // Determine shaft end join type
  let shaftEndJoinType = "";
  if (hasSpecialShaftEndJoin && component.code) {
    const code = component.code.substring(2, 5);
    switch(code) {
      case "209": shaftEndJoinType = "Y"; break;
      case "210": shaftEndJoinType = "I"; break;
      case "211": shaftEndJoinType = "Pin"; break;
    }
  }

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
              {(hasSpecialInstallation || hasSpecialShaftEndJoin) && (
                <Badge className="bg-purple-100 text-purple-800 ml-2">
                  {hasSpecialInstallation ? `Installation: ${installationType}` : `Shaft Join: ${shaftEndJoinType}`}
                </Badge>
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

// Helper function to extract hydraulic cylinder properties from inferredProperties
const extractHydraulicCylinderProps = (properties: Record<string, string[]>): HydraulicCylinderProps => {
  const result: HydraulicCylinderProps = {};
  
  if (!properties) return result;
  
  // Map property URIs to property names
  const propertyMap: Record<string, keyof HydraulicCylinderProps> = {
    'http://www.jfc.com/tiptop/ontology#bore': 'bore',
    'http://www.jfc.com/tiptop/ontology#stroke': 'stroke',
    'http://www.jfc.com/tiptop/ontology#rodEndType': 'rodEndType',
    'http://www.jfc.com/tiptop/ontology#series': 'series',
    'http://www.jfc.com/tiptop/ontology#type': 'type',
    'http://www.jfc.com/tiptop/ontology#installation': 'installation',
    'http://www.jfc.com/tiptop/ontology#shaftEndJoin': 'shaftEndJoin'
  };
  
  // Check each property
  Object.entries(properties).forEach(([propUri, values]) => {
    const propName = propertyMap[propUri];
    if (propName && values && values.length > 0) {
      result[propName] = values[0];
    }
  });
  
  return result;
};

// Helper function to check if a property is a hydraulic-specific property
const isHydraulicProperty = (propUri: string): boolean => {
  const hydraulicProps = [
    'http://www.jfc.com/tiptop/ontology#bore',
    'http://www.jfc.com/tiptop/ontology#stroke',
    'http://www.jfc.com/tiptop/ontology#rodEndType',
    'http://www.jfc.com/tiptop/ontology#series',
    'http://www.jfc.com/tiptop/ontology#type',
    'http://www.jfc.com/tiptop/ontology#installation',
    'http://www.jfc.com/tiptop/ontology#shaftEndJoin'
  ];
  
  return hydraulicProps.includes(propUri);
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