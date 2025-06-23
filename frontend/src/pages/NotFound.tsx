// src/pages/NotFound.tsx
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Home, ArrowLeft, Search } from 'lucide-react';

const NotFound = () => {
  const navigate = useNavigate();
  
  // Log the 404 error
  useEffect(() => {
    console.error('404 Page Not Found');
  }, []);

  return (
    <div className="container mx-auto py-12 px-4 flex flex-col items-center justify-center min-h-[70vh]">
      <Card className="max-w-md w-full">
        <CardContent className="pt-6 px-8 pb-8 flex flex-col items-center text-center">
          <div className="w-24 h-24 flex items-center justify-center rounded-full bg-gray-100 mb-4">
            <span className="text-4xl font-bold text-gray-400">404</span>
          </div>
          
          <h1 className="text-2xl font-bold mb-2">Page Not Found</h1>
          
          <p className="text-gray-600 mb-6">
            The page you are looking for doesn't exist or has been moved.
          </p>
          
          <div className="flex flex-wrap gap-3 justify-center">
            <Button 
              variant="outline" 
              onClick={() => navigate('/')}
              className="flex items-center"
            >
              <Home className="mr-2 h-4 w-4" />
              Go to Dashboard
            </Button>
            
            <Button 
              variant="outline" 
              onClick={() => navigate(-1)}
              className="flex items-center"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Go Back
            </Button>
            
            <Button 
              variant="outline" 
              onClick={() => navigate('/items')}
              className="flex items-center"
            >
              <Search className="mr-2 h-4 w-4" />
              Search Items
            </Button>
          </div>
        </CardContent>
      </Card>
      
      <div className="mt-8 text-center text-gray-500 text-sm">
        <p>Having trouble finding what you need?</p>
        <p>Try searching for items or check the navigation menu for available options.</p>
      </div>
    </div>
  );
};

export default NotFound;