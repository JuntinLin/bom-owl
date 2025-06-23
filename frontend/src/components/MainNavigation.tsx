// src/components/MainNavigation.tsx
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  Home,
  Database,
  Brain,
  Search,
  Download
} from 'lucide-react';

interface NavItem {
  title: string;
  href: string;
  icon: React.ElementType;
  badge?: string;
}

const MainNavigation = () => {
  const location = useLocation();
  
  const navItems: NavItem[] = [
    {
      title: 'Dashboard',
      href: '/',
      icon: Home
    },
    {
      title: 'Search Items',
      href: '/items',
      icon: Search
    },
    {
      title: 'Knowledge Base',
      href: '/knowledge-base',
      icon: Database,
      badge: 'New'
    },
    {
      title: 'Reasoning',
      href: '/reasoning',
      icon: Brain
    },
    {
      title: 'Export',
      href: '/export',
      icon: Download
    }
  ];
  
  return (
    <nav className="flex items-center space-x-6 lg:space-x-8">
      {navItems.map((item) => {
        const Icon = item.icon;
        const isActive = location.pathname === item.href || 
                        location.pathname.startsWith(item.href + '/');
        
        return (
          <Link
            key={item.href}
            to={item.href}
            className={cn(
              "flex items-center text-sm font-medium transition-colors hover:text-primary",
              isActive ? "text-primary" : "text-muted-foreground"
            )}
          >
            <Icon className="w-4 h-4 mr-2" />
            {item.title}
            {item.badge && (
              <span className="ml-2 px-2 py-0.5 text-xs bg-blue-100 text-blue-800 rounded-full">
                {item.badge}
              </span>
            )}
          </Link>
        );
      })}
    </nav>
  );
};

export default MainNavigation;