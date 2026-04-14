interface Tab {
  id: string;
  label: string;
  badge?: number;
}

interface TabsProps {
  tabs: Tab[];
  activeTab: string;
  onChange: (id: string) => void;
}

export default function Tabs({ tabs, activeTab, onChange }: TabsProps) {
  return (
    <div className="border-b border-border">
      <div className="flex gap-1">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onChange(tab.id)}
              className={`
                px-4 py-2.5 text-[13px] font-medium border-b-2 transition-colors
                ${isActive
                  ? 'border-gold text-gold'
                  : 'border-transparent text-text-secondary hover:text-text-primary hover:border-border-strong'}
              `}
            >
              {tab.label}
              {tab.badge !== undefined && (
                <span className="ml-2 text-[10px] font-bold px-1.5 py-px rounded-[10px] bg-gold text-navy">
                  {tab.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
