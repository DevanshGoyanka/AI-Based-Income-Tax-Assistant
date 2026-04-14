import React from 'react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export default function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      {icon && <div className="text-text-muted opacity-40 mb-4">{icon}</div>}
      <h3 className="text-[15px] font-semibold text-text-primary mb-1">{title}</h3>
      {description && <p className="text-[13px] text-text-muted mb-4 max-w-md">{description}</p>}
      {action}
    </div>
  );
}
