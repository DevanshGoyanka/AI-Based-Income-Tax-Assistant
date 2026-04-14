interface ProgressBarProps {
  progress: number;
  label?: string;
  color?: 'gold' | 'blue' | 'teal' | 'success';
  showPercentage?: boolean;
}

const colorStyles = {
  gold: 'bg-gold',
  blue: 'bg-accent-blue',
  teal: 'bg-accent-teal',
  success: 'bg-success',
};

export default function ProgressBar({ progress, label, color = 'gold', showPercentage = true }: ProgressBarProps) {
  const pct = Math.min(100, Math.max(0, progress));
  return (
    <div>
      {(label || showPercentage) && (
        <div className="flex items-center justify-between mb-1.5">
          {label && <span className="text-[12px] font-medium text-text-secondary">{label}</span>}
          {showPercentage && <span className="text-[11px] font-mono text-text-muted">{pct.toFixed(0)}%</span>}
        </div>
      )}
      <div className="h-2 bg-bg rounded-full overflow-hidden">
        <div
          className={`h-full transition-all duration-300 ${colorStyles[color]}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
