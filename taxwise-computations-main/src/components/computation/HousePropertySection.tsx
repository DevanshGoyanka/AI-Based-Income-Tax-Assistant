import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Home } from "lucide-react";

interface HousePropertySectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const HousePropertySection = ({ data, onChange }: HousePropertySectionProps) => {
  return (
    <SectionCard
      title="Income from House Property"
      subtitle="Sections 22–27 — Rental Income, Self-Occupied, Let-Out"
      icon={<Home className="w-4 h-4 text-accent-teal" />}
      badge="Schedule HP"
      totalLabel="Net HP Income"
      totalValue={`₹${data.netHPIncome || "0"}`}
    >
      <div className="space-y-0.5">
        <FieldRow label="Type of Property" name="propertyType" value={data.propertyType || "let-out"} onChange={onChange} type="select"
          options={[
            { label: "Self-Occupied", value: "self-occupied" },
            { label: "Let Out", value: "let-out" },
            { label: "Deemed Let Out", value: "deemed-let-out" },
          ]}
        />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Let-Out Property</p>
        <FieldRow label="Gross Annual Value (Rent Received/Receivable)" name="grossAnnualValue" value={data.grossAnnualValue || ""} onChange={onChange} />
        <FieldRow label="Municipal Taxes Paid" name="municipalTaxes" value={data.municipalTaxes || ""} onChange={onChange} />
        <FieldRow label="Net Annual Value (GAV − Municipal Taxes)" name="netAnnualValue" value={data.netAnnualValue || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Deductions from HP Income</p>
        <FieldRow label="Standard Deduction u/s 24(a) — 30% of NAV" name="stdDeduction24a" value={data.stdDeduction24a || ""} onChange={onChange} readOnly hint="Auto-calculated" />
        <FieldRow label="Interest on Housing Loan u/s 24(b)" name="interestLoan24b" value={data.interestLoan24b || ""} onChange={onChange} hint="Max ₹2,00,000 for self-occupied" />
        <FieldRow label="Interest on Loan for Repair/Renewal" name="interestRepair" value={data.interestRepair || ""} onChange={onChange} />
        <FieldRow label="Total Deductions from HP" name="totalDeductionsHP" value={data.totalDeductionsHP || ""} onChange={onChange} readOnly bold />

        <FieldRow label="Net Income from House Property" name="netHPIncome" value={data.netHPIncome || ""} onChange={onChange} readOnly bold highlighted />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Arrears / Unrealised Rent</p>
        <FieldRow label="Arrears of Rent Received u/s 25A" name="arrearsRent25a" value={data.arrearsRent25a || ""} onChange={onChange} />
        <FieldRow label="Unrealised Rent Received u/s 25AA" name="unrealisedRent25aa" value={data.unrealisedRent25aa || ""} onChange={onChange} />
      </div>
    </SectionCard>
  );
};

export default HousePropertySection;
