import React from 'react';

interface ActivityItemProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  time: string;
  color?: 'gold' | 'blue' | 'teal' | 'rose';
}

const colorStyles = {
  gold: 'bg-gold-pale text-gold',
  blue: 'bg-info-bg text-info',
  teal: 'bg-[#CCFBF1] text-accent-teal',
  rose: 'bg-[#FCE7F3] text-accent-rose',
};

export default function ActivityItem({ icon, title, description, time, color = 'gold' }: ActivityItemProps) {
  return (
    <div className="flex gap-3 py-3 border-b border-border last:border-0">
      <div className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 ${colorStyles[color]}`}>
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[13px] font-medium text-text-primary">{title}</div>
        <div className="text-[12px] text-text-muted mt-0.5">{description}</div>
      </div>
      <div className="text-[11px] text-text-muted flex-shrink-0">{time}</div>
    </div>
  );
}
