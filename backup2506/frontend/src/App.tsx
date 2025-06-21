// src/App.tsx
import { BrowserRouter as Router } from 'react-router-dom';
import { Toaster as SonnerToaster } from 'sonner';
import AppRoutes from '@/routes/AppRoutes';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { ThemeProvider } from '@/components/ThemeProvider';

function App() {
  return (
    <ThemeProvider defaultTheme="light" storageKey="tiptop-owl-theme">
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Navbar />
          <div className="flex">
            <Sidebar />
            <main className="flex-1 p-6">
              <AppRoutes />
            </main>
          </div>
          <SonnerToaster position="bottom-right" />
        </div>
      </Router>
    </ThemeProvider>
  );
}

export default App;