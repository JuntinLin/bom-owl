// src/components/layout/Sidebar.tsx
import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  HomeIcon,
  SearchIcon,
  DatabaseIcon,
  ArrowDownIcon,
  Share2Icon,
  BrainIcon,
  InfoIcon,
  HardDrive,
  Layers,
  Search} from 'lucide-react';

interface SidebarLinkProps {
  to: string;
  icon: React.ReactNode;
  label: string;
  end?: boolean;
}

const SidebarLink = ({ to, icon, label, end = false }: SidebarLinkProps) => {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors",
          isActive
            ? "bg-accent text-accent-foreground font-medium"
            : "hover:bg-accent hover:text-accent-foreground"
        )
      }
    >
      {icon}
      <span>{label}</span>
    </NavLink>
  );
};

const Sidebar = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <div
      className={cn(
        "h-screen sticky top-0 border-r bg-background hidden md:block transition-all duration-300",
        isCollapsed ? "w-[70px]" : "w-[200px]"
      )}
    >
      <div className="flex h-16 items-center border-b px-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="ml-auto"
        >
          <ArrowDownIcon
            className={cn(
              "h-4 w-4 transition-transform",
              isCollapsed ? "rotate-90" : "-rotate-90"
            )}
          />
          <span className="sr-only">Toggle sidebar</span>
        </Button>
      </div>

      <div className="py-4">
        <nav className="grid gap-2 px-2">
          <SidebarLink to="/" icon={<HomeIcon className="h-4 w-4" />} label="Dashboard" end />
          <SidebarLink to="/items" icon={<SearchIcon className="h-4 w-4" />} label="Search Items" />
          <SidebarLink to="/export" icon={<ArrowDownIcon className="h-4 w-4" />} label="Export OWL" />

          <div className="my-2 px-3">
            <div className={cn("h-[1px] bg-border", isCollapsed ? "w-4 mx-auto" : "w-full")} />
          </div>

          {!isCollapsed && (
            <p className="text-xs font-medium text-muted-foreground px-3 my-1">
              Knowledge Management
            </p>
          )}

          <SidebarLink to="/knowledge-base" icon={<HardDrive className="h-4 w-4" />} label="KB Management" />
          <SidebarLink to="/knowledge-base/search" icon={<Search className="h-4 w-4" />} label="KB Search" />
          <SidebarLink to="/bom-generator" icon={<Layers className="h-4 w-4" />} label="BOM Generator" />

          <div className="my-2 px-3">
            <div className={cn("h-[1px] bg-border", isCollapsed ? "w-4 mx-auto" : "w-full")} />
          </div>

          {!isCollapsed && (
            <p className="text-xs font-medium text-muted-foreground px-3 my-1">
              Reasoning Tools
            </p>
          )}

          <SidebarLink to="/items" icon={<BrainIcon className="h-4 w-4" />} label="Reasoning" />
          <SidebarLink to="/items" icon={<DatabaseIcon className="h-4 w-4" />} label="BOM Explorer" />
          <SidebarLink to="/items" icon={<Share2Icon className="h-4 w-4" />} label="Component Analysis" />

          <div className="my-2 px-3">
            <div className={cn("h-[1px] bg-border", isCollapsed ? "w-4 mx-auto" : "w-full")} />
          </div>

          <SidebarLink to="/about" icon={<InfoIcon className="h-4 w-4" />} label="About" />
        </nav>
      </div>

      {!isCollapsed && (
        <div className="absolute bottom-4 px-4 w-full">
          <div className="bg-muted p-2 rounded-md text-xs text-muted-foreground">
            <p className="font-medium">TiptopERP to OWL</p>
            <p className="mt-1">Convert BOM data to OWL ontologies</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default Sidebar;