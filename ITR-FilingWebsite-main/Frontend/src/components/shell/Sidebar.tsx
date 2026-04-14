import React from 'react';
import { useAppStore } from '../../store/useAppStore';

type NavItem = {
  id: string;
  label: string;
  badge?: string | number;
  badgeColor?: 'gold' | 'blue' | 'red';
  icon: React.ReactNode;
};

const SECTIONS: { label: string; items: NavItem[] }[] = [
  {
    label: 'Overview',
    items: [
      {
        id: 'dashboard', label: 'Dashboard',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="2" y="2" width="7" height="7" rx="1.5"/><rect x="11" y="2" width="7" height="7" rx="1.5"/><rect x="2" y="11" width="7" height="7" rx="1.5"/><rect x="11" y="11" width="7" height="7" rx="1.5"/></svg>,
      },
    ],
  },
  {
    label: 'Clients & Filing',
    items: [
      {
        id: 'clients', label: 'Client Master', badge: '5,000',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><circle cx="8" cy="6" r="3"/><path d="M2 17c0-3.3 2.7-6 6-6"/><circle cx="15" cy="12" r="3"/><path d="M11 17.9c.3-3.1 2.5-5.5 5.4-5.9"/></svg>,
      },
      {
        id: 'filing', label: 'ITR Filing', badge: 342, badgeColor: 'blue',
        icon: <svg className="w-[18px] h-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M14 2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z"/><path d="M8 10h4M8 13h4M8 7h2"/></svg>,
      },
    ],
  },
];

function BadgePill({ value, color }: { value: string | number; color?: 'gold' | 'blue' | 'red' }) {
  const colorMap = {
    gold: 'bg-gold text-navy',
    blue: 'bg-[#3B82F6] text-white',
    red: 'bg-[#EF4444] text-white',
  };
  return (
    <span className={`ml-auto text-[10px] font-bold px-1.5 py-px rounded-[10px] min-w-[18px] text-center ${colorMap[color ?? 'gold']}`}>
      {value}
    </span>
  );
}

export default function Sidebar() {
  const { activeView, setActiveView, globalSearch, setGlobalSearch } = useAppStore();

  return (
    <aside className="fixed top-0 left-0 w-[258px] h-screen bg-navy flex flex-col z-[100] overflow-hidden">
      <div className="px-[22px] py-5 pb-4 border-b border-white/[0.08] flex-shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 bg-gold rounded-lg flex items-center justify-center font-serif text-[18px] font-semibold text-navy flex-shrink-0">
            IT
          </div>
          <div className="flex flex-col">
            <span className="font-serif text-base font-semibold text-white tracking-[0.2px] leading-[1.2]">
              IncomeTax ERP
            </span>
            <span className="text-[10.5px] text-gold-light uppercase tracking-[0.4px] font-medium">
              Advocate Practice
            </span>
          </div>
        </div>
      </div>

      <div className="px-4 pt-3.5 pb-2.5 flex-shrink-0">
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          <input
            value={globalSearch}
            onChange={(e) => setGlobalSearch(e.target.value)}
            placeholder="Search clients, PAN…"
            className="w-full bg-white/[0.07] border border-white/10 rounded-[7px] py-2 pl-8 pr-3 text-white text-[13px] placeholder:text-text-muted outline-none focus:border-gold transition-colors font-sans"
          />
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-2.5 pb-2.5 sidebar-nav">
        {SECTIONS.map((section) => (
          <React.Fragment key={section.label}>
            <div className="text-[10px] font-semibold tracking-[1px] uppercase text-white/30 px-3 pt-3.5 pb-1.5">
              {section.label}
            </div>
            {section.items.map((item) => {
              const isActive = activeView === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveView(item.id as any)}
                  className={`
                    relative w-full flex items-center gap-2.5 px-3 py-[9px] rounded-lg cursor-pointer
                    text-[13.5px] font-sans transition-all duration-150 mb-px text-left
                    ${isActive
                      ? 'bg-gold/[0.18] text-gold-light font-medium nav-active'
                      : 'text-white/65 hover:bg-white/[0.07] hover:text-white/90 font-normal'}
                  `}
                >
                  <span className={`flex-shrink-0 ${isActive ? 'opacity-100' : 'opacity-80'}`}>
                    {item.icon}
                  </span>
                  {item.label}
                  {item.badge !== undefined && (
                    <BadgePill value={item.badge} color={item.badgeColor} />
                  )}
                </button>
              );
            })}
          </React.Fragment>
        ))}
      </nav>

      <div className="px-4 py-3.5 border-t border-white/[0.08] flex-shrink-0">
        <div className="flex items-center gap-2.5 mb-3">
          <div className="w-[34px] h-[34px] rounded-full bg-gold flex items-center justify-center text-[13px] font-bold text-navy flex-shrink-0">
            {(() => {
              const user = JSON.parse(localStorage.getItem('itr_erp_user') || '{}');
              const email = user.username || '';
              return email.substring(0, 2).toUpperCase() || 'AP';
            })()}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[13px] font-medium text-white truncate">
              {JSON.parse(localStorage.getItem('itr_erp_user') || '{}').username || 'User'}
            </div>
            <div className="text-[11px] text-white/45">
              {JSON.parse(localStorage.getItem('itr_erp_user') || '{}').role || 'Admin'}
            </div>
          </div>
        </div>
        <button
          onClick={() => {
            localStorage.removeItem('itr_erp_token');
            localStorage.removeItem('itr_erp_user');
            window.location.reload();
          }}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-white/[0.07] hover:bg-white/[0.12] text-white/80 hover:text-white transition-all text-[13px] font-medium"
        >
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M9 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5M15 13l4-4-4-4M19 9H9"/>
          </svg>
          Logout
        </button>
      </div>
    </aside>
  );
}
