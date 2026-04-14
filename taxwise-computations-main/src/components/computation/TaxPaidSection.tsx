import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Receipt, Plus, Trash2 } from "lucide-react";
import { useState } from "react";

interface TDSEntry {
  id: string;
  tanOfDeductor: string;
  nameOfDeductor: string;
  section: string;
  totalAmountCredited: string;
  taxDeducted: string;
  taxClaimed: string;
}

interface TaxPaidSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const TaxPaidSection = ({ data, onChange }: TaxPaidSectionProps) => {
  const [tdsEntries, setTdsEntries] = useState<TDSEntry[]>([
    { id: "1", tanOfDeductor: "", nameOfDeductor: "", section: "192", totalAmountCredited: "", taxDeducted: "", taxClaimed: "" },
  ]);

  const addTDS = () => {
    setTdsEntries([...tdsEntries, {
      id: Date.now().toString(), tanOfDeductor: "", nameOfDeductor: "", section: "", totalAmountCredited: "", taxDeducted: "", taxClaimed: "",
    }]);
  };

  return (
    <SectionCard
      title="Taxes Paid & Verification"
      subtitle="TDS, TCS, Advance Tax, Self-Assessment Tax, Refund/Balance"
      icon={<Receipt className="w-4 h-4 text-info" />}
      badge="Schedule TDS/IT"
      badgeColor="bg-info-bg text-info"
      totalLabel="Balance Tax / Refund"
      totalValue={`₹${data.balanceTaxRefund || "0"}`}
    >
      <div className="space-y-0.5">
        {/* TDS on Salary */}
        <div className="flex items-center justify-between mb-2">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">TDS on Salary — Schedule TDS1</p>
          <button onClick={addTDS} className="inline-flex items-center gap-1 text-[10px] font-medium text-accent-blue hover:underline">
            <Plus className="w-3 h-3" /> Add Deductor
          </button>
        </div>

        {tdsEntries.map((entry, idx) => (
          <div key={entry.id} className="p-3 border border-border rounded-md bg-muted/20 mb-2 space-y-1">
            <div className="flex items-center justify-between mb-1">
              <span className="text-[10px] font-medium text-muted-foreground">Deductor {idx + 1}</span>
              {tdsEntries.length > 1 && (
                <button onClick={() => setTdsEntries(tdsEntries.filter(e => e.id !== entry.id))} className="text-danger hover:text-danger/70">
                  <Trash2 className="w-3 h-3" />
                </button>
              )}
            </div>
            <FieldRow label="TAN of Deductor" name={`tds_tan_${entry.id}`} value={entry.tanOfDeductor} onChange={() => {}} type="text" prefix="" />
            <FieldRow label="Name of Deductor" name={`tds_name_${entry.id}`} value={entry.nameOfDeductor} onChange={() => {}} type="text" prefix="" />
            <FieldRow label="Income Credited" name={`tds_income_${entry.id}`} value={entry.totalAmountCredited} onChange={() => {}} />
            <FieldRow label="Tax Deducted" name={`tds_deducted_${entry.id}`} value={entry.taxDeducted} onChange={() => {}} />
            <FieldRow label="Tax Claimed for this Return" name={`tds_claimed_${entry.id}`} value={entry.taxClaimed} onChange={() => {}} />
          </div>
        ))}

        <FieldRow label="Total TDS Claimed (Salary)" name="totalTDSSalary" value={data.totalTDSSalary || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">TDS on Other Income — Schedule TDS2</p>
        <FieldRow label="TDS on Interest / Other (as per 26AS)" name="tdsOtherIncome" value={data.tdsOtherIncome || ""} onChange={onChange} />
        <FieldRow label="TDS on Sale of Property u/s 194IA" name="tdsProperty194ia" value={data.tdsProperty194ia || ""} onChange={onChange} />
        <FieldRow label="TDS on Rent u/s 194IB" name="tdsRent194ib" value={data.tdsRent194ib || ""} onChange={onChange} />
        <FieldRow label="TDS on Payment to Resident Contractor u/s 194M" name="tdsContractor194m" value={data.tdsContractor194m || ""} onChange={onChange} />
        <FieldRow label="TDS u/s 194S (VDA)" name="tdsVDA194s" value={data.tdsVDA194s || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">TCS — Schedule TCS</p>
        <FieldRow label="Total TCS Claimed" name="totalTCS" value={data.totalTCS || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Advance Tax & Self-Assessment Tax — Schedule IT</p>
        <FieldRow label="Advance Tax Paid (BSR Code / Challan)" name="advanceTaxPaid" value={data.advanceTaxPaid || ""} onChange={onChange} />
        <FieldRow label="Self-Assessment Tax Paid" name="selfAssessmentTax" value={data.selfAssessmentTax || ""} onChange={onChange} />
        <FieldRow label="Regular Assessment Tax (if any)" name="regularAssessmentTax" value={data.regularAssessmentTax || ""} onChange={onChange} />

        <FieldRow label="Total Taxes Paid" name="totalTaxesPaid" value={data.totalTaxesPaid || ""} onChange={onChange} readOnly bold />

        <div className="mt-4 p-3 bg-gold-pale/60 rounded-md border border-primary/20">
          <FieldRow label="Tax Payable (+) / Refund (−)" name="balanceTaxRefund" value={data.balanceTaxRefund || ""} onChange={onChange} readOnly bold highlighted />
        </div>
      </div>
    </SectionCard>
  );
};

export default TaxPaidSection;
