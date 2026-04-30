import { Menu, Search } from 'lucide-react';

interface AppTopBarProps {
  onOpenSidebar: () => void;
  onOpenSearch: () => void;
}

export function AppTopBar({ onOpenSidebar, onOpenSearch }: AppTopBarProps) {
  return (
    <div className="flex items-center justify-between px-4 py-3 sticky top-0 z-30 bg-white shadow-sm border-b border-gray-100">
      <div className="flex items-center gap-3">
        <button
          onClick={onOpenSidebar}
          className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-xl transition-colors"
        >
          <Menu size={24} />
        </button>
        <h1 className="text-xl font-bold tracking-tight text-gray-900">
          AgentTravel
        </h1>
      </div>

      <div className="flex items-center gap-1.5 relative z-50">
        <button
          onClick={onOpenSearch}
          className="p-2 text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
        >
          <Search size={22} className="text-gray-700" />
        </button>
      </div>
    </div>
  );
}
