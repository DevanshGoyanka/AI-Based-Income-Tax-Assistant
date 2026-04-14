import { Info } from "lucide-react";

interface FieldRowProps {
  label: string;
  name: string;
  value: string;
  onChange: (name: string, value: string) => void;
  type?: "text" | "number" | "select" | "date";
  options?: { label: string; value: string }[];
  hint?: string;
  readOnly?: boolean;
  highlighted?: boolean;
  prefix?: string;
  section?: string;
  bold?: boolean;
  indented?: boolean;
}

const FieldRow = ({
  label,
  name,
  value,
  onChange,
  type = "number",
  options,
  hint,
  readOnly = false,
  highlighted = false,
  prefix = "₹",
  bold = false,
  indented = false,
}: FieldRowProps) => {
  return (
    <div
      className={`flex items-center gap-4 py-2 px-3 rounded-md group transition-colors ${
        highlighted ? "bg-gold-pale/50" : "hover:bg-muted/40"
      } ${bold ? "border-t border-border pt-3" : ""}`}
    >
      <div className={`flex-1 min-w-0 ${indented ? "pl-5" : ""}`}>
        <label className={`text-xs ${bold ? "font-semibold text-foreground" : "text-muted-foreground"}`}>
          {label}
        </label>
        {hint && (
          <div className="flex items-center gap-1 mt-0.5">
            <Info className="w-3 h-3 text-muted-foreground/60" />
            <span className="text-[10px] text-muted-foreground/60">{hint}</span>
          </div>
        )}
      </div>
      <div className="w-52 flex-shrink-0">
        {type === "select" && options ? (
          <select
            value={value}
            onChange={(e) => onChange(name, e.target.value)}
            disabled={readOnly}
            className="w-full px-3 py-1.5 bg-card border border-border-strong rounded-md text-xs text-foreground focus:border-primary focus:outline-none disabled:bg-muted disabled:text-muted-foreground"
          >
            {options.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        ) : (
          <div className="relative">
            {type === "number" && prefix && (
              <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">{prefix}</span>
            )}
            <input
              type={type === "number" ? "text" : type}
              value={value}
              onChange={(e) => onChange(name, e.target.value)}
              readOnly={readOnly}
              className={`w-full py-1.5 bg-card border border-border-strong rounded-md text-xs text-foreground text-right focus:border-primary focus:outline-none disabled:bg-muted font-mono ${
                type === "number" && prefix ? "pl-6 pr-3" : "px-3"
              } ${readOnly ? "bg-muted text-muted-foreground" : ""} ${bold ? "font-semibold" : ""}`}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default FieldRow;
