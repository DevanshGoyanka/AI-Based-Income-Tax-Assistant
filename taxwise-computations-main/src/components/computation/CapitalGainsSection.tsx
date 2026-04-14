import { useState } from "react";
import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { TrendingUp, Plus, Trash2 } from "lucide-react";

interface CapitalGainEntry {
  id: string;
  description: string;
  saleConsideration: string;
  costOfAcquisition: string;
  costOfImprovement: string;
  transferExpenses: string;
  exemption: string;
  netGain: string;
}

interface CapitalGainsSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const CapitalGainsSection = ({ data, onChange }: CapitalGainsSectionProps) => {
  const [stcgEntries, setStcgEntries] = useState<CapitalGainEntry[]>([
    { id: "1", description: "", saleConsideration: "", costOfAcquisition: "", costOfImprovement: "", transferExpenses: "", exemption: "", netGain: "" },
  ]);
  const [ltcgEntries, setLtcgEntries] = useState<CapitalGainEntry[]>([
    { id: "1", description: "", saleConsideration: "", costOfAcquisition: "", costOfImprovement: "", transferExpenses: "", exemption: "", netGain: "" },
  ]);

  const addEntry = (type: "stcg" | "ltcg") => {
    const newEntry: CapitalGainEntry = {
      id: Date.now().toString(),
      description: "", saleConsideration: "", costOfAcquisition: "", costOfImprovement: "", transferExpenses: "", exemption: "", netGain: "",
    };
    if (type === "stcg") setStcgEntries([...stcgEntries, newEntry]);
    else setLtcgEntries([...ltcgEntries, newEntry]);
  };

  const removeEntry = (type: "stcg" | "ltcg", id: string) => {
    if (type === "stcg") setStcgEntries(stcgEntries.filter(e => e.id !== id));
    else setLtcgEntries(ltcgEntries.filter(e => e.id !== id));
  };

  return (
    <SectionCard
      title="Capital Gains"
      subtitle="Sections 45–55A — Short-Term & Long-Term Capital Gains"
      icon={<TrendingUp className="w-4 h-4 text-accent-rose" />}
      badge="Schedule CG"
      totalLabel="Net Capital Gains"
      totalValue={`₹${data.netCapitalGains || "0"}`}
    >
      <div className="space-y-4">
        {/* STCG */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Short-Term Capital Gains</p>
            <button onClick={() => addEntry("stcg")} className="inline-flex items-center gap-1 text-[10px] font-medium text-accent-blue hover:underline">
              <Plus className="w-3 h-3" /> Add Asset
            </button>
          </div>

          <FieldRow label="STCG on Equity Shares / Units u/s 111A (15%)" name="stcg111a" value={data.stcg111a || ""} onChange={onChange} />
          <FieldRow label="STCG on Other Assets (Slab Rate)" name="stcgOther" value={data.stcgOther || ""} onChange={onChange} />

          {stcgEntries.map((entry) => (
            <div key={entry.id} className="ml-4 mt-2 p-3 border border-border rounded-md bg-muted/20 space-y-1">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[10px] font-medium text-muted-foreground">Asset Detail</span>
                {stcgEntries.length > 1 && (
                  <button onClick={() => removeEntry("stcg", entry.id)} className="text-danger hover:text-danger/70">
                    <Trash2 className="w-3 h-3" />
                  </button>
                )}
              </div>
              <FieldRow label="Description of Asset" name={`stcg_desc_${entry.id}`} value={entry.description} onChange={() => {}} type="text" prefix="" />
              <FieldRow label="Full Value of Consideration" name={`stcg_sale_${entry.id}`} value={entry.saleConsideration} onChange={() => {}} />
              <FieldRow label="Cost of Acquisition" name={`stcg_cost_${entry.id}`} value={entry.costOfAcquisition} onChange={() => {}} />
              <FieldRow label="Expenditure on Transfer" name={`stcg_exp_${entry.id}`} value={entry.transferExpenses} onChange={() => {}} />
            </div>
          ))}

          <FieldRow label="Total Short-Term Capital Gains" name="totalSTCG" value={data.totalSTCG || ""} onChange={onChange} readOnly bold />
        </div>

        {/* LTCG */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Long-Term Capital Gains</p>
            <button onClick={() => addEntry("ltcg")} className="inline-flex items-center gap-1 text-[10px] font-medium text-accent-blue hover:underline">
              <Plus className="w-3 h-3" /> Add Asset
            </button>
          </div>

          <FieldRow label="LTCG on Equity Shares / Units u/s 112A (12.5%)" name="ltcg112a" value={data.ltcg112a || ""} onChange={onChange} hint="Exempt up to ₹1.25 lakh" />
          <FieldRow label="LTCG on Other Assets u/s 112 (12.5%)" name="ltcg112" value={data.ltcg112 || ""} onChange={onChange} />

          {ltcgEntries.map((entry) => (
            <div key={entry.id} className="ml-4 mt-2 p-3 border border-border rounded-md bg-muted/20 space-y-1">
              <div className="flex items-center justify-between mb-1">
                <span className="text-[10px] font-medium text-muted-foreground">Asset Detail</span>
                {ltcgEntries.length > 1 && (
                  <button onClick={() => removeEntry("ltcg", entry.id)} className="text-danger hover:text-danger/70">
                    <Trash2 className="w-3 h-3" />
                  </button>
                )}
              </div>
              <FieldRow label="Description of Asset" name={`ltcg_desc_${entry.id}`} value={entry.description} onChange={() => {}} type="text" prefix="" />
              <FieldRow label="Full Value of Consideration" name={`ltcg_sale_${entry.id}`} value={entry.saleConsideration} onChange={() => {}} />
              <FieldRow label="Cost of Acquisition (Indexed)" name={`ltcg_cost_${entry.id}`} value={entry.costOfAcquisition} onChange={() => {}} />
              <FieldRow label="Exemption u/s 54/54EC/54F" name={`ltcg_exempt_${entry.id}`} value={entry.exemption} onChange={() => {}} />
            </div>
          ))}

          <FieldRow label="Total Long-Term Capital Gains" name="totalLTCG" value={data.totalLTCG || ""} onChange={onChange} readOnly bold />
        </div>

        <FieldRow label="Net Capital Gains (STCG + LTCG)" name="netCapitalGains" value={data.netCapitalGains || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default CapitalGainsSection;
