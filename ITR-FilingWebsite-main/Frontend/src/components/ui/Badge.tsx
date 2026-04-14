type BadgeVariant = 'gold' | 'blue' | 'teal' | 'rose' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

interface BadgeProps {
  label: string;
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
}

const variantStyles: Record<BadgeVariant, string> = {
  gold: 'bg-gold-pale text-gold border-gold/20',
  blue: 'bg-info-bg text-info border-info/20',
  teal: 'bg-[#CCFBF1] text-accent-teal border-accent-teal/20',
  rose: 'bg-[#FCE7F3] text-accent-rose border-accent-rose/20',
  success: 'bg-success-bg text-success border-success/20',
  warning: 'bg-warning-bg text-warning border-warning/20',
  danger: 'bg-danger-bg text-danger border-danger/20',
  info: 'bg-info-bg text-info border-info/20',
  neutral: 'bg-bg border-border-strong text-text-secondary',
};

export default function Badge({ label, variant = 'neutral', size = 'sm' }: BadgeProps) {
  const sizeClass = size === 'sm' ? 'text-[10.5px] px-2 py-0.5' : 'text-[11.5px] px-2.5 py-1';
  return (
    <span className={`inline-flex items-center font-medium rounded-[6px] border ${variantStyles[variant]} ${sizeClass}`}>
      {label}
    </span>
  );
}
