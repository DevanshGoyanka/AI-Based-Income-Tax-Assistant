import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { ArrowDownUp } from "lucide-react";

interface LossSetOffSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const LossSetOffSection = ({ data, onChange }: LossSetOffSectionProps) => {
  return (
    <SectionCard
      title="Set-Off & Carry Forward of Losses"
      subtitle="Sections 70–80 — Loss Adjustment, B/F Losses"
      icon={<ArrowDownUp className="w-4 h-4 text-danger" />}
      badge="Schedule CYLA/BFLA"
      badgeColor="bg-danger-bg text-danger"
      totalLabel="Net Loss C/F"
      totalValue={`₹${data.netLossCarryForward || "0"}`}
    >
      <div className="space-y-0.5">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">Current Year Loss Adjustment (CYLA)</p>
        <FieldRow label="HP Loss set off against Salary" name="hpLossVsSalary" value={data.hpLossVsSalary || ""} onChange={onChange} hint="Max ₹2,00,000" />
        <FieldRow label="HP Loss set off against Other Sources" name="hpLossVsOther" value={data.hpLossVsOther || ""} onChange={onChange} />
        <FieldRow label="Business Loss set off against Other Heads" name="businessLossVsOther" value={data.businessLossVsOther || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Brought Forward Loss Adjustment (BFLA)</p>
        <FieldRow label="B/F HP Loss (AY)" name="bfHPLoss" value={data.bfHPLoss || ""} onChange={onChange} />
        <FieldRow label="B/F Business Loss (AY)" name="bfBusinessLoss" value={data.bfBusinessLoss || ""} onChange={onChange} />
        <FieldRow label="B/F Speculation Loss (AY)" name="bfSpeculationLoss" value={data.bfSpeculationLoss || ""} onChange={onChange} />
        <FieldRow label="B/F STCG Loss (AY)" name="bfSTCGLoss" value={data.bfSTCGLoss || ""} onChange={onChange} />
        <FieldRow label="B/F LTCG Loss (AY)" name="bfLTCGLoss" value={data.bfLTCGLoss || ""} onChange={onChange} />
        <FieldRow label="B/F Loss from Owning Racehorses" name="bfRacehorseLoss" value={data.bfRacehorseLoss || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Losses to be Carried Forward</p>
        <FieldRow label="HP Loss to be Carried Forward" name="cfHPLoss" value={data.cfHPLoss || ""} onChange={onChange} readOnly />
        <FieldRow label="Business Loss to be Carried Forward" name="cfBusinessLoss" value={data.cfBusinessLoss || ""} onChange={onChange} readOnly />
        <FieldRow label="Capital Loss to be Carried Forward" name="cfCapitalLoss" value={data.cfCapitalLoss || ""} onChange={onChange} readOnly />
        <FieldRow label="Total Loss Carried Forward" name="netLossCarryForward" value={data.netLossCarryForward || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default LossSetOffSection;
