import { useState, ReactNode } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";

interface SectionCardProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  badge?: string;
  badgeColor?: string;
  totalLabel?: string;
  totalValue?: string;
  children: ReactNode;
  defaultOpen?: boolean;
}

const SectionCard = ({
  title,
  subtitle,
  icon,
  badge,
  badgeColor = "bg-gold-pale text-primary",
  totalLabel,
  totalValue,
  children,
  defaultOpen = false,
}: SectionCardProps) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="bg-card border border-border rounded-lg overflow-hidden">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center gap-3 px-5 py-3.5 hover:bg-muted/50 transition-colors text-left"
      >
        {isOpen ? (
          <ChevronDown className="w-4 h-4 text-muted-foreground flex-shrink-0" />
        ) : (
          <ChevronRight className="w-4 h-4 text-muted-foreground flex-shrink-0" />
        )}
        {icon && <span className="flex-shrink-0">{icon}</span>}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-semibold text-foreground">{title}</h3>
            {badge && (
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${badgeColor}`}>
                {badge}
              </span>
            )}
          </div>
          {subtitle && <p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>}
        </div>
        {totalLabel && totalValue && (
          <div className="text-right flex-shrink-0">
            <p className="text-[10px] uppercase tracking-wider text-muted-foreground">{totalLabel}</p>
            <p className="text-base font-display font-semibold text-foreground">{totalValue}</p>
          </div>
        )}
      </button>
      {isOpen && (
        <div className="border-t border-border px-5 py-4">
          {children}
        </div>
      )}
    </div>
  );
};

export default SectionCard;
