// src/types/knowledgeBase.ts (create if doesn't exist)

export interface SimilarBOMDTO {
  masterItemCode: string;
  fileName: string;
  description: string;
  similarityScore: number;
  createdAt: string;
  tripleCount: number;
  fileSize: number;
  format: string;
  qualityScore?: number;
  validationStatus?: string;
  sourceSystem?: string;
  isHydraulicCylinder?: boolean;
  hydraulicCylinderSpecs?: string;
  parsedSpecs?: {
    series: string;
    type: string;
    bore: string;
    stroke: string;
    rodEndType: string;
  };
}