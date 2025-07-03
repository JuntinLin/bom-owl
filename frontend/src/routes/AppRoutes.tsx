// src/routes/AppRoutes.tsx
import { Route, Routes } from 'react-router-dom';
import Dashboard from '@/pages/Dashboard';
import ItemSearch from '@/pages/ItemSearch';
import ItemDetail from '@/pages/ItemDetail';
import BomExplorer from '@/pages/BomExplorer';
import ExportPage from '@/pages/ExportPage';
import NotFound from '@/pages/NotFound';
import ReasoningDashboard from '@/pages/ReasoningDashboard';
import KnowledgeBaseManagement from '@/pages/KnowledgeBaseManagement';
import KnowledgeBaseSearch from '@/pages/KnowledgeBaseSearch';
import BomGeneratorPage from '@/pages/BomGeneratorPage';

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/items" element={<ItemSearch />} />
      <Route path="/items/view/:itemCode" element={<ItemDetail />} />
      <Route path="/bom/:masterItemCode" element={<BomExplorer />} />
      <Route path="/export" element={<ExportPage />} />
      <Route path="/reasoning/:masterItemCode" element={<ReasoningDashboard />} />
      <Route path="/knowledge-base" element={<KnowledgeBaseManagement />} />
      <Route path="/knowledge-base/search" element={<KnowledgeBaseSearch />} />      
      <Route path="/bom-generator" element={<BomGeneratorPage />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default AppRoutes;