// src/components/layout/Navbar.tsx
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ModeToggle } from '@/components/ui/mode-toggle';
import { Database, Menu } from 'lucide-react';

interface NavbarProps {
  toggleSidebar?: () => void;
}

const Navbar = ({ toggleSidebar }: NavbarProps) => {
  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background">
      <div className="container flex h-16 items-center px-4 sm:px-6">
        <Button 
          variant="ghost" 
          className="mr-2 md:hidden" 
          onClick={toggleSidebar}
          size="icon"
        >
          <Menu className="h-5 w-5" />
          <span className="sr-only">Toggle menu</span>
        </Button>

        <div className="flex items-center">
          <Link to="/" className="flex items-center space-x-2">
            <Database className="h-6 w-6" />
            <span className="inline-block font-bold">TiptopERP to OWL</span>
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-end space-x-4">
          <nav className="flex items-center space-x-2">
            <Link to="/items">
              <Button variant="ghost" size="sm">
                Items
              </Button>
            </Link>
            <Link to="/export">
              <Button variant="ghost" size="sm">
                Export
              </Button>
            </Link>
          </nav>
          <ModeToggle />
        </div>
      </div>
    </header>
  );
};

export default Navbar;