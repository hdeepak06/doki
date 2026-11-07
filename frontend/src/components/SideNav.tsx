import React from 'react';
import {
  Home,
  MessageSquare,
  Mic,
  Laptop,
  Smartphone,
  Globe,
  Database,
  History,
  Settings,
} from 'lucide-react';

export type NavTab =
  | 'home'
  | 'chat'
  | 'voice'
  | 'laptop'
  | 'mobile'
  | 'research'
  | 'memory'
  | 'history'
  | 'settings';

interface SideNavProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

const navItems: Array<{ id: NavTab; label: string; icon: React.FC<{ className?: string }> }> = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'chat', label: 'AI Chat', icon: MessageSquare },
  { id: 'voice', label: 'Voice Assistant', icon: Mic },
  { id: 'laptop', label: 'Laptop Control', icon: Laptop },
  { id: 'mobile', label: 'Mobile Control', icon: Smartphone },
  { id: 'research', label: 'Internet Research', icon: Globe },
  { id: 'memory', label: 'Memory', icon: Database },
  { id: 'history', label: 'Task History', icon: History },
  { id: 'settings', label: 'Settings', icon: Settings },
];

export const SideNav: React.FC<SideNavProps> = ({ activeTab, onTabChange }) => {
  return (
    <aside className="w-64 border-r border-cyan-500/20 bg-slate-950/60 backdrop-blur-md flex flex-col justify-between py-6 px-3 select-none">
      <div className="space-y-1">
        <p className="px-3 text-[10px] uppercase font-mono tracking-widest text-cyan-500/60 font-bold mb-3">
          SYSTEM MODULES
        </p>

        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;

          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-xs font-mono transition-all ${
                isActive
                  ? 'jarvis-glass-active text-cyan-300 font-bold'
                  : 'text-slate-400 hover:text-cyan-200 hover:bg-slate-900/60'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-cyan-400' : 'text-slate-500'}`} />
              <span className="truncate">{item.label}</span>
              {isActive && (
                <span className="ml-auto w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-[0_0_8px_#06B6D4]" />
              )}
            </button>
          );
        })}
      </div>

      {/* Protocol Banner */}
      <div className="px-3 py-3 rounded-lg border border-cyan-500/10 bg-cyan-950/10 font-mono text-[10px] text-slate-500">
        <div className="flex items-center justify-between text-cyan-400/80 mb-1">
          <span>SECURITY</span>
          <span>ACTIVE</span>
        </div>
        <p className="text-slate-400">Zero-cost free model router with authenticated device mesh.</p>
      </div>
    </aside>
  );
};
