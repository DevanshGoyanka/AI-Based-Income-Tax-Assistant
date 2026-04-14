import React from 'react';

type StatColor = 'gold' | 'teal' | 'blue' | 'rose' | 'success' | 'warning' | 'danger';

interface StatCardProps {
  label: string;
  value: string | number;
  accent: StatColor;
  icon?: React.ReactNode;
  meta?: React.ReactNode;
}

const colorStyles: Record<StatColor, string> = {
  gold: 'before:bg-gold',
  teal: 'before:bg-accent-teal',
  blue: 'before:bg-accent-blue',
  rose: 'before:bg-accent-rose',
  success: 'before:bg-success',
  warning: 'before:bg-warning',
  danger: 'before:bg-danger',
};

export default function StatCard({ label, value, accent, icon, meta }: StatCardProps) {
  return (
    <div
      className={`
        bg-bg-card border border-border rounded-[10px] p-[18px] px-5 relative overflow-hidden
        before:absolute before:top-0 before:left-0 before:w-full before:h-[3px]
        ${colorStyles[accent]}
      `}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="text-[11.5px] font-medium text-text-muted uppercase tracking-[0.5px]">{label}</div>
          <div className="font-serif text-[32px] font-semibold text-text-primary leading-[1.1] my-1.5">{value}</div>
          {meta && <div className="text-[12px] text-text-muted">{meta}</div>}
        </div>
        {icon && <div className="text-text-muted opacity-40">{icon}</div>}
      </div>
    </div>
  );
}
