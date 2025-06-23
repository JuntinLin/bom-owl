// src/pages/Dashboard.tsx
import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService'; // Use the service instead of axios directly
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { BomStats } from '@/types/tiptop'; // Import types 
import {
  Brain,
  Code,
  Search,
  Download,
  Layers,
  Plus,
  Zap,
  Database,
  FileText,
  HardDrive,
  Settings
} from 'lucide-react';
// Add this to Dashboard.tsx after the existing action cards
import { useKnowledgeBaseStats } from '@/hooks/useKnowledgeBase';
import { formatFileSize } from '@/utils/formatters';

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
}

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<BomStats>({
    totalItems: 0,
    masterItemsCount: 0,
    componentItemsCount: 0,
    bomRelationshipsCount: 0
  });
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [quickSearchQuery, setQuickSearchQuery] = useState<string>('');
  // Inside Dashboard component, add:
  const { stats: kbStats, loading: kbLoading } = useKnowledgeBaseStats();

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const statsData = await tiptopService.getBomStats();
        setStats(statsData);
        setError(null);
      } catch (err) {
        console.error('Error fetching stats:', err);
        setError(err instanceof Error ? err.message : 'Failed to load statistics. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const handleQuickSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (quickSearchQuery.trim()) {
      navigate(`/items?search=${encodeURIComponent(quickSearchQuery.trim())}`);
    }
  };

  const handleGenerateNewBom = () => {
    // Navigate to reasoning dashboard with generator tab active
    navigate('/reasoning/new?tab=generator');
  };

  const StatCard = ({ title, value, icon, color }: StatCardProps) => (
    <Card className={`border-l-4 ${color}`}>
      <CardContent className="p-6">
        <div className="flex items-center">
          <div className="p-3 rounded-full bg-opacity-10 mr-4">{icon}</div>
          <div>
            <p className="text-gray-500 text-sm font-medium">{title}</p>
            <p className="text-2xl font-bold">
              {loading ? <span className="animate-pulse">Loading...</span> : value.toLocaleString()}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold">TiptopERP to OWL System Dashboard</h1>
          <p className="text-gray-600 mt-1">Advanced semantic reasoning and BOM generation platform</p>
        </div>

        {/* Quick Search */}
        <form onSubmit={handleQuickSearch} className="flex gap-2 w-full sm:w-auto">
          <Input
            placeholder="Quick search items..."
            value={quickSearchQuery}
            onChange={(e) => setQuickSearchQuery(e.target.value)}
            className="w-full sm:w-64"
          />
          <Button type="submit" size="icon">
            <Search className="h-4 w-4" />
          </Button>
        </form>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Total Items"
          value={stats.totalItems}
          icon={<svg className="w-6 h-6 text-blue-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M2 5a2 2 0 012-2h12a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V5zm3.293 1.293a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 01-1.414-1.414L7.586 10 5.293 7.707a1 1 0 010-1.414z" clipRule="evenodd" /></svg>}
          color="border-blue-500"
        />
        <StatCard
          title="Master Items"
          value={stats.masterItemsCount}
          icon={<svg className="w-6 h-6 text-green-500" fill="currentColor" viewBox="0 0 20 20"><path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" /></svg>}
          color="border-green-500"
        />
        <StatCard
          title="Component Items"
          value={stats.componentItemsCount}
          icon={<svg className="w-6 h-6 text-purple-500" fill="currentColor" viewBox="0 0 20 20"><path d="M7 9a2 2 0 012-2h6a2 2 0 012 2v6a2 2 0 01-2 2H9a2 2 0 01-2-2V9z" /><path d="M5 3a2 2 0 00-2 2v6a2 2 0 002 2V5h8a2 2 0 00-2-2H5z" /></svg>}
          color="border-purple-500"
        />
        <StatCard
          title="BOM Relationships"
          value={stats.bomRelationshipsCount}
          icon={<svg className="w-6 h-6 text-yellow-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 002 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clipRule="evenodd" /></svg>}
          color="border-yellow-500"
        />
      </div>

      {/* Main Action Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {/* BOM Generator Card */}
        <Card className="hover:shadow-lg transition-shadow cursor-pointer border-2 border-dashed border-blue-200 hover:border-blue-400">
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-100 rounded-full">
                <Plus className="w-6 h-6 text-blue-600" />
              </div>
              <Button
                onClick={handleGenerateNewBom}
                variant="outline"
              >
                Generate BOM
              </Button>
            </div>
            <h3 className="text-lg font-semibold mb-2">Create New BOM</h3>
            <p className="text-gray-600 text-sm mb-4">
              Generate intelligent BOMs for new hydraulic cylinders using AI-powered semantic reasoning and domain knowledge.
            </p>
            <div className="flex items-center text-sm text-blue-600">
              <Zap className="w-4 h-4 mr-1" />
              <span>AI-Powered Generation</span>
            </div>
          </CardContent>
        </Card>

        {/* Search & Explore Card */}
        <Card className="hover:shadow-lg transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-100 rounded-full">
                <Search className="w-6 h-6 text-green-600" />
              </div>
              <Link to="/items">
                <Button variant="outline">
                  Search Items
                </Button>
              </Link>
            </div>
            <h3 className="text-lg font-semibold mb-2">Search & Explore</h3>
            <p className="text-gray-600 text-sm mb-4">
              Find materials, explore BOM structures, and analyze component relationships with advanced search capabilities.
            </p>
            <div className="flex items-center text-sm text-green-600">
              <Database className="w-4 h-4 mr-1" />
              <span>{stats.totalItems.toLocaleString()} Items Available</span>
            </div>
          </CardContent>
        </Card>

        {/* Export & Analysis Card */}
        <Card className="hover:shadow-lg transition-shadow">
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-purple-100 rounded-full">
                <Download className="w-6 h-6 text-purple-600" />
              </div>
              <Link to="/export">
                <Button variant="outline">
                  Export Data
                </Button>
              </Link>
            </div>
            <h3 className="text-lg font-semibold mb-2">Export & Analysis</h3>
            <p className="text-gray-600 text-sm mb-4">
              Export BOMs as ontologies in various formats (OWL, JSON-LD, Turtle) for external analysis and integration.
            </p>
            <div className="flex items-center text-sm text-purple-600">
              <FileText className="w-4 h-4 mr-1" />
              <span>Multiple Export Formats</span>
            </div>
          </CardContent>
        </Card>

        {/* Knowledge Base Management Card */}
        <Card className="hover:shadow-lg transition-shadow cursor-pointer border-2 border-dashed border-green-200 hover:border-green-400">
          <CardContent className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-100 rounded-full">
                <HardDrive className="w-6 h-6 text-green-600" />
              </div>
              <Link to="/knowledge-base">
                <Button variant="outline">
                  Manage KB
                </Button>
              </Link>
            </div>
            <h3 className="text-lg font-semibold mb-2">Knowledge Base Management</h3>
            <p className="text-gray-600 text-sm mb-4">
              Manage OWL knowledge base, export BOMs from ERP, search similar products, and maintain the knowledge repository.
            </p>
            <div className="flex items-center justify-between text-sm">
              <div className="flex items-center text-green-600">
                <Brain className="w-4 h-4 mr-1" />
                <span>Intelligent Knowledge Storage</span>
              </div>
              {kbStats && (
                <Badge variant="outline">
                  {kbStats.totalEntries} entries
                </Badge>
              )}
            </div>
          </CardContent>
        </Card>

      </div>

      {/* Semantic Reasoning Capabilities */}
      <Card className="mb-8">
        <CardHeader>
          <CardTitle className="flex items-center">
            <Brain className="w-6 h-6 mr-2 text-blue-500" />
            Semantic Reasoning & AI Capabilities
          </CardTitle>
        </CardHeader>
        <CardContent className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="border rounded-lg p-4 bg-blue-50 hover:bg-blue-100 transition-colors">
              <div className="flex items-center mb-3">
                <Brain className="w-6 h-6 text-blue-500 mr-2" />
                <h3 className="text-lg font-medium">OWL Reasoning</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Apply advanced OWL reasoners to infer new knowledge from your BOM data. Discover implicit relationships and validate ontology consistency.
              </p>
              <div className="flex flex-wrap gap-2 mb-4">
                <span className="px-2 py-1 bg-blue-200 text-blue-800 text-xs rounded">HermiT</span>
                <span className="px-2 py-1 bg-blue-200 text-blue-800 text-xs rounded">Pellet</span>
                <span className="px-2 py-1 bg-blue-200 text-blue-800 text-xs rounded">RDFS</span>
              </div>
              <Link to="/items">
                <Button variant="outline" className="w-full">
                  Find Items to Analyze
                </Button>
              </Link>
            </div>

            <div className="border rounded-lg p-4 bg-purple-50 hover:bg-purple-100 transition-colors">
              <div className="flex items-center mb-3">
                <Code className="w-6 h-6 text-purple-500 mr-2" />
                <h3 className="text-lg font-medium">SPARQL Queries</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Execute powerful SPARQL queries to find complex patterns in your BOM data. Use predefined templates or create custom queries.
              </p>
              <div className="flex flex-wrap gap-2 mb-4">
                <span className="px-2 py-1 bg-purple-200 text-purple-800 text-xs rounded">SELECT</span>
                <span className="px-2 py-1 bg-purple-200 text-purple-800 text-xs rounded">CONSTRUCT</span>
                <span className="px-2 py-1 bg-purple-200 text-purple-800 text-xs rounded">ASK</span>
              </div>
              <Link to="/items">
                <Button variant="outline" className="w-full">
                  Find Items to Query
                </Button>
              </Link>
            </div>

            <div className="border rounded-lg p-4 bg-amber-50 hover:bg-amber-100 transition-colors">
              <div className="flex items-center mb-3">
                <Layers className="w-6 h-6 text-amber-500 mr-2" />
                <h3 className="text-lg font-medium">BOM Generation</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Generate intelligent BOMs for new products using domain-specific rules and AI-powered component selection algorithms.
              </p>
              <div className="flex flex-wrap gap-2 mb-4">
                <span className="px-2 py-1 bg-amber-200 text-amber-800 text-xs rounded">Logic Rules</span>
                <span className="px-2 py-1 bg-amber-200 text-amber-800 text-xs rounded">AI Selection</span>
                <span className="px-2 py-1 bg-amber-200 text-amber-800 text-xs rounded">Validation</span>
              </div>
              <Button
                onClick={handleGenerateNewBom}
                variant="outline"
                className="w-full"
              >
                Generate New BOM
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Feature Highlights Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Getting Started</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <ol className="list-decimal list-inside space-y-3 text-gray-600">
              <li>
                <strong>Search for Materials:</strong> Use the search function to find master items or components in your inventory.
              </li>
              <li>
                <strong>Explore BOM Structure:</strong> View detailed hierarchical BOM structures with interactive tree visualization.
              </li>
              <li>
                <strong>Generate New BOMs:</strong> Create intelligent BOMs for new hydraulic cylinders using AI-powered reasoning.
              </li>
              <li>
                <strong>Apply Semantic Reasoning:</strong> Use OWL reasoning to infer new knowledge and validate data consistency.
              </li>
              <li>
                <strong>Query with SPARQL:</strong> Find specific information using powerful semantic queries with predefined templates.
              </li>
              <li>
                <strong>Export as Ontologies:</strong> Save BOMs in various semantic formats for integration with other tools.
              </li>
            </ol>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Advanced Features</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <div className="space-y-4">
              <div className="flex items-start">
                <div className="flex-shrink-0 w-5 h-5 bg-green-100 rounded-full flex items-center justify-center mr-3 mt-0.5">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">Dual-Paradigm Reasoning</h4>
                  <p className="text-sm text-gray-600">Combines logic-based OWL reasoning with AI-powered component selection</p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-5 h-5 bg-blue-100 rounded-full flex items-center justify-center mr-3 mt-0.5">
                  <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">Hydraulic Cylinder Expertise</h4>
                  <p className="text-sm text-gray-600">Specialized domain knowledge for hydraulic cylinder BOM generation</p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-5 h-5 bg-purple-100 rounded-full flex items-center justify-center mr-3 mt-0.5">
                  <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">Multiple Export Formats</h4>
                  <p className="text-sm text-gray-600">OWL/RDF, JSON-LD, Turtle, N-Triples for maximum compatibility</p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-5 h-5 bg-yellow-100 rounded-full flex items-center justify-center mr-3 mt-0.5">
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">Interactive Visualizations</h4>
                  <p className="text-sm text-gray-600">Rich tree views and hierarchical displays of BOM structures</p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-5 h-5 bg-red-100 rounded-full flex items-center justify-center mr-3 mt-0.5">
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">Validation & Consistency</h4>
                  <p className="text-sm text-gray-600">Automatic validation and consistency checking for generated BOMs</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity / Quick Stats */}
      <Card className="mt-6">
        <CardHeader>
          <CardTitle>System Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-600">{((stats.componentItemsCount / stats.totalItems) * 100).toFixed(1)}%</div>
              <div className="text-sm text-gray-600">Component Ratio</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-green-600">{(stats.bomRelationshipsCount / stats.masterItemsCount).toFixed(1)}</div>
              <div className="text-sm text-gray-600">Avg Components/BOM</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-purple-600">100%</div>
              <div className="text-sm text-gray-600">Data Coverage</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-yellow-600">24/7</div>
              <div className="text-sm text-gray-600">System Availability</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Knowledge Base Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-green-600">
                {kbLoading ? <span className="animate-pulse">...</span> : (kbStats?.totalEntries || 0)}
              </div>
              <div className="text-sm text-gray-600">KB Entries</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-600">
                {kbLoading ? <span className="animate-pulse">...</span> : formatFileSize(kbStats?.totalFileSize || 0)}
              </div>
              <div className="text-sm text-gray-600">Storage Used</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl font-bold text-purple-600">
                {kbLoading ? <span className="animate-pulse">...</span> : (kbStats?.hydraulicCylinderCount || 0)}
              </div>
              <div className="text-sm text-gray-600">Hydraulic Cylinders</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <Link to="/knowledge-base" className="text-blue-600 hover:text-blue-800">
                <Settings className="w-6 h-6 mx-auto mb-1" />
                <div className="text-sm">Manage</div>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Dashboard;