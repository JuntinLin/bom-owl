// src/hooks/useKnowledgeBase.ts
import { useState, useEffect } from 'react';
import { knowledgeBaseService, KnowledgeBaseStats } from '@/services/knowledgeBaseService';
import { toast } from 'sonner';

export const useKnowledgeBaseStats = () => {
  const [stats, setStats] = useState<KnowledgeBaseStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const data = await knowledgeBaseService.getStatistics();
        setStats(data);
        setError(null);
      } catch (err) {
        setError('Failed to load knowledge base statistics');
        console.error('Error fetching KB stats:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const refresh = async () => {
    try {
      setLoading(true);
      const data = await knowledgeBaseService.getStatistics();
      setStats(data);
      setError(null);
    } catch (err) {
      setError('Failed to refresh statistics');
    } finally {
      setLoading(false);
    }
  };

  return { stats, loading, error, refresh };
};

export const useQuickExport = () => {
  const [exporting, setExporting] = useState(false);

  const quickExport = async (masterItemCode: string) => {
    try {
      setExporting(true);
      await knowledgeBaseService.exportSingleToKnowledgeBase({
        masterItemCode,
        format: 'RDF/XML',
        includeHierarchy: true,
        description: `Quick export from ${new Date().toLocaleDateString()}`
      });
      toast.success(`Successfully exported ${masterItemCode} to knowledge base`);
    } catch (err) {
      toast.error('Failed to export to knowledge base');
      console.error('Export error:', err);
    } finally {
      setExporting(false);
    }
  };

  return { quickExport, exporting };
};