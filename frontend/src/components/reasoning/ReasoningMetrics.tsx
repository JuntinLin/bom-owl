// src/components/reasoning/ReasoningMetrics.tsx

import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { BarChart3, TrendingUp, Database, Zap } from 'lucide-react';
import { ReasoningMetrics as MetricsType } from '@/types/tiptop';

interface ReasoningMetricsProps {
  metrics: MetricsType;
  modelSize?: number;
  reasonerUsed?: string;
}

const ReasoningMetrics: React.FC<ReasoningMetricsProps> = ({ 
  metrics, 
  modelSize,
  reasonerUsed 
}) => {
  const inferencePercentage = metrics.originalStatements > 0 
    ? (metrics.inferredStatements / metrics.originalStatements) * 100 
    : 0;

  const getReasonerBadgeVariant = (reasoner: string): "default" | "secondary" | "outline" => {
    if (reasoner?.includes('fallback')) return "secondary";
    if (reasoner === 'OWL') return "outline";
    return "default";
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5" />
            Reasoning Metrics
          </span>
          {reasonerUsed && (
            <Badge variant={getReasonerBadgeVariant(reasonerUsed)} className="text-xs">
              {reasonerUsed}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Model Size */}
          {modelSize !== undefined && (
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="h-4 w-4 text-gray-500" />
                <span className="text-sm font-medium">Model Size</span>
              </div>
              <span className="text-sm text-gray-700">
                {modelSize.toLocaleString()} statements
              </span>
            </div>
          )}

          {/* Original Statements */}
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium">Original Statements</span>
            <span className="text-sm text-gray-700">
              {metrics.originalStatements.toLocaleString()}
            </span>
          </div>

          {/* Inferred Statements */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-green-500" />
              <span className="text-sm font-medium">Inferred Statements</span>
            </div>
            <span className="text-sm text-gray-700">
              {metrics.inferredStatements.toLocaleString()}
            </span>
          </div>

          {/* Total Statements */}
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium">Total Statements</span>
            <span className="text-sm font-semibold">
              {metrics.totalStatements.toLocaleString()}
            </span>
          </div>

          {/* Inference Ratio */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Zap className="h-4 w-4 text-blue-500" />
                <span className="text-sm font-medium">Inference Ratio</span>
              </div>
              <span className="text-sm text-gray-700">
                {metrics.inferenceRatio.toFixed(2)} ({inferencePercentage.toFixed(0)}%)
              </span>
            </div>
            <Progress value={inferencePercentage} className="h-2" />
          </div>

          {/* Reasoning Completeness */}
          {metrics.reasoningCompleteness !== undefined && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Reasoning Completeness</span>
                <span className="text-sm text-gray-700">
                  {(metrics.reasoningCompleteness * 100).toFixed(0)}%
                </span>
              </div>
              <Progress value={metrics.reasoningCompleteness * 100} className="h-2" />
            </div>
          )}

          {/* Performance Indicator */}
          <div className="pt-2 border-t">
            <div className="text-xs text-gray-500">
              {metrics.inferenceRatio > 0.5 ? (
                <span className="text-green-600">✓ High inference rate indicates good reasoning coverage</span>
              ) : metrics.inferenceRatio > 0.2 ? (
                <span className="text-blue-600">→ Moderate inference rate</span>
              ) : (
                <span className="text-orange-600">↓ Low inference rate - consider using a more powerful reasoner</span>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default ReasoningMetrics;