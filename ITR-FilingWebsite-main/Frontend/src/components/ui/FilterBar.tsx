interface FilterOption {
  label: string;
  value: string;
}

interface FilterBarProps {
  filters: {
    label: string;
    key: string;
    options: FilterOption[];
  }[];
  activeFilters: Record<string, string>;
  onChange: (key: string, value: string) => void;
  onClear?: () => void;
}

export default function FilterBar({ filters, activeFilters, onChange, onClear }: FilterBarProps) {
  const hasActiveFilters = Object.values(activeFilters).some((v) => v !== '');

  return (
    <div className="flex items-center gap-3 flex-wrap">
      {filters.map((filter) => (
        <select
          key={filter.key}
          value={activeFilters[filter.key] || ''}
          onChange={(e) => onChange(filter.key, e.target.value)}
          className="bg-bg-card border border-border rounded-lg px-3 py-2 text-[13px] text-text-primary outline-none focus:border-gold transition-colors"
        >
          <option value="">{filter.label}: All</option>
          {filter.options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ))}
      {hasActiveFilters && onClear && (
        <button
          onClick={onClear}
          className="text-[12px] text-text-muted hover:text-text-primary transition-colors"
        >
          Clear filters
        </button>
      )}
    </div>
  );
}
