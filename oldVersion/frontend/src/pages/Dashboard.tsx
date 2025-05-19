// src/pages/Dashboard.tsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { tiptopService } from '@/services/tiptopService'; // Use the service instead of axios directly
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BomStats } from '@/types/tiptop'; // Import types 
import {  
  Share2, 
  Brain,
  Code
} from 'lucide-react';

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
}

const Dashboard = () => {
  const [stats, setStats] = useState<BomStats>({
    totalItems: 0,
    masterItemsCount: 0,
    componentItemsCount: 0,
    bomRelationshipsCount: 0
  });
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

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
      <h1 className="text-2xl font-bold mb-6">TiptopERP to OWL System Dashboard</h1>
      
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardContent className="p-6">
            <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
            <div className="space-y-4">
              <Link 
                to="/items" 
                className="flex items-center p-3 bg-indigo-50 hover:bg-indigo-100 rounded-md transition duration-200"
              >
                <svg className="w-6 h-6 text-indigo-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd" />
                </svg>
                <span>Search Items</span>
              </Link>
              <Link 
                to="/export" 
                className="flex items-center p-3 bg-green-50 hover:bg-green-100 rounded-md transition duration-200"
              >
                <svg className="w-6 h-6 text-green-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
                <span>Export Ontology</span>
              </Link>
            </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardContent className="p-6">
            <h2 className="text-xl font-semibold mb-4">About This System</h2>
            <p className="text-gray-600 mb-4">
              The TiptopERP to OWL System converts your ERP data into Web Ontology Language (OWL) format, enabling semantic 
              representation of your product structures.
            </p>
            <p className="text-gray-600">
              View Bill of Materials (BOM) structures, export ontologies in various formats (OWL/RDF, JSON-LD, Turtle), 
              and explore BOM hierarchies with interactive visualizations.
            </p>
          </CardContent>
        </Card>
      </div>

      <Card className="mb-8">
        <CardHeader>
          <CardTitle>Semantic Reasoning Capabilities</CardTitle>
        </CardHeader>
        <CardContent className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="border rounded-lg p-4 bg-blue-50">
              <div className="flex items-center mb-2">
                <Brain className="w-6 h-6 text-blue-500 mr-2" />
                <h3 className="text-lg font-medium">OWL Reasoning</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Apply OWL reasoners to infer new knowledge from your BOM data. Discover implicit relationships and check ontology consistency.
              </p>
              <Link to="/items">
                <Button variant="outline" className="w-full">
                  Find Items to Analyze
                </Button>
              </Link>
            </div>
            
            <div className="border rounded-lg p-4 bg-purple-50">
              <div className="flex items-center mb-2">
                <Code className="w-6 h-6 text-purple-500 mr-2" />
                <h3 className="text-lg font-medium">SPARQL Queries</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Execute SPARQL queries to find complex patterns in your BOM data. Use predefined queries or create your own.
              </p>
              <Link to="/items">
                <Button variant="outline" className="w-full">
                  Find Items to Query
                </Button>
              </Link>
            </div>
            
            <div className="border rounded-lg p-4 bg-amber-50">
              <div className="flex items-center mb-2">
                <Share2 className="w-6 h-6 text-amber-500 mr-2" />
                <h3 className="text-lg font-medium">Custom Rules</h3>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Define custom rules to derive new information from your BOM data. Identify critical components and detect patterns.
              </p>
              <Link to="/items">
                <Button variant="outline" className="w-full">
                  Find Items to Apply Rules
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Getting Started</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <ol className="list-decimal list-inside space-y-2 text-gray-600">
              <li><strong>Search for Materials:</strong> Find master items or components.</li>
              <li><strong>View Material Details:</strong> Explore item information and BOM structure.</li>
              <li><strong>Apply Reasoning:</strong> Use OWL reasoning to infer new knowledge.</li>
              <li><strong>Query with SPARQL:</strong> Find specific information using semantic queries.</li>
              <li><strong>Export as OWL:</strong> Save ontologies for use in other tools.</li>
            </ol>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader>
            <CardTitle>Reasoning Features</CardTitle>
          </CardHeader>
          <CardContent className="p-6">
            <ul className="space-y-2">
              <li className="flex items-start">
                <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span className="text-gray-600">Multiple reasoners: OWL, RDFS, rule-based</span>
              </li>
              <li className="flex items-start">
                <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span className="text-gray-600">SPARQL query support with predefined templates</span>
              </li>
              <li className="flex items-start">
                <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span className="text-gray-600">Custom rule definition in Jena rule syntax</span>
              </li>
              <li className="flex items-start">
                <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span className="text-gray-600">Ontology validation and consistency checking</span>
              </li>
              <li className="flex items-start">
                <svg className="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span className="text-gray-600">Interactive BOM hierarchy visualization with inferred information</span>
              </li>
            </ul>
          </CardContent>
        </Card>
      </div>

    </div>
  );
};

export default Dashboard;