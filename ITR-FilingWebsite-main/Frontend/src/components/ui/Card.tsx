import React from 'react';

interface CardProps {
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

const paddingStyles = {
  none: '',
  sm: 'p-4',
  md: 'p-5 px-[22px]',
  lg: 'p-6 px-7',
};

export default function Card({ title, subtitle, action, children, className = '', padding = 'md' }: CardProps) {
  return (
    <div className={`bg-bg-card border border-border rounded-[10px] ${className}`}>
      {(title || action) && (
        <div className={`flex items-center justify-between ${padding === 'none' ? 'p-5 px-[22px]' : paddingStyles[padding]} ${children ? 'border-b border-border' : ''}`}>
          <div>
            {title && <div className="text-[14px] font-semibold text-text-primary">{title}</div>}
            {subtitle && <div className="text-[12px] text-text-muted mt-0.5">{subtitle}</div>}
          </div>
          {action}
        </div>
      )}
      <div className={paddingStyles[padding]}>
        {children}
      </div>
    </div>
  );
}
