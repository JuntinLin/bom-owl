// src/components/Navbar.tsx
import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <nav className="bg-indigo-600 text-white shadow-lg">
      <div className="container mx-auto px-4">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="flex-shrink-0 flex items-center">
              <svg 
                className="h-8 w-8 mr-2" 
                viewBox="0 0 24 24" 
                fill="none" 
                xmlns="http://www.w3.org/2000/svg"
              >
                <path 
                  d="M21 12L13 4V8C13 8 4 8 4 16C4 16 8 12 13 12V16L21 12Z" 
                  fill="currentColor"
                />
              </svg>
              <span className="font-bold text-xl">TiptopERP to OWL</span>
            </Link>
          </div>
          <div className="flex items-center space-x-4">
            <Link to="/" className="px-3 py-2 rounded-md hover:bg-indigo-700">
              Dashboard
            </Link>
            <Link to="/items" className="px-3 py-2 rounded-md hover:bg-indigo-700">
              Item Search
            </Link>
            <Link to="/export" className="px-3 py-2 rounded-md hover:bg-indigo-700">
              Export Ontology
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;