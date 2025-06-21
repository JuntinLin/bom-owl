// src/App.tsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import ItemSearch from './pages/ItemSearch';
import BomViewer from './pages/BomViewer';
import OntologyExport from './pages/OntologyExport';
import BomTree from './pages/BomTree';

const App = () => {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <Navbar />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/items" element={<ItemSearch />} />
            <Route path="/bom/:itemCode" element={<BomViewer />} />
            <Route path="/bom-tree/:itemCode" element={<BomTree />} />
            <Route path="/export" element={<OntologyExport />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;