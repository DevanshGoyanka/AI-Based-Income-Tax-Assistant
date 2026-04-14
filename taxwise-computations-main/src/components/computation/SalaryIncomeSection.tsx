import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Briefcase } from "lucide-react";

interface SalaryIncomeSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const SalaryIncomeSection = ({ data, onChange }: SalaryIncomeSectionProps) => {
  return (
    <SectionCard
      title="Income from Salary"
      subtitle="Section 15, 16, 17 — Salary, Perquisites, Allowances"
      icon={<Briefcase className="w-4 h-4 text-accent-blue" />}
      badge="Schedule S"
      totalLabel="Net Salary Income"
      totalValue={`₹${data.netSalary || "0"}`}
      defaultOpen
    >
      <div className="space-y-0.5">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">Gross Salary (Section 17(1))</p>
        <FieldRow label="Salary as per Section 17(1)" name="salary17_1" value={data.salary17_1 || ""} onChange={onChange} />
        <FieldRow label="Value of Perquisites u/s 17(2)" name="perquisites17_2" value={data.perquisites17_2 || ""} onChange={onChange} />
        <FieldRow label="Profits in lieu of Salary u/s 17(3)" name="profitsLieu17_3" value={data.profitsLieu17_3 || ""} onChange={onChange} />
        <FieldRow label="Total Gross Salary" name="grossSalary" value={data.grossSalary || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Allowances Exempt u/s 10</p>
        <FieldRow label="House Rent Allowance u/s 10(13A)" name="hra10_13a" value={data.hra10_13a || ""} onChange={onChange} />
        <FieldRow label="Leave Travel Allowance u/s 10(5)" name="lta10_5" value={data.lta10_5 || ""} onChange={onChange} />
        <FieldRow label="Gratuity u/s 10(10)" name="gratuity10_10" value={data.gratuity10_10 || ""} onChange={onChange} />
        <FieldRow label="Commuted Pension u/s 10(10A)" name="commutedPension" value={data.commutedPension || ""} onChange={onChange} />
        <FieldRow label="Leave Encashment u/s 10(10AA)" name="leaveEncashment" value={data.leaveEncashment || ""} onChange={onChange} />
        <FieldRow label="Other Exempt Allowances u/s 10" name="otherExempt10" value={data.otherExempt10 || ""} onChange={onChange} />
        <FieldRow label="Total Exempt Allowances" name="totalExemptAllowances" value={data.totalExemptAllowances || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Deductions u/s 16</p>
        <FieldRow label="Standard Deduction u/s 16(ia)" name="standardDeduction" value={data.standardDeduction || "75000"} onChange={onChange} hint="₹75,000 for AY 2025-26" />
        <FieldRow label="Entertainment Allowance u/s 16(ii)" name="entertainmentAllowance" value={data.entertainmentAllowance || ""} onChange={onChange} hint="Only Govt. employees" />
        <FieldRow label="Professional Tax u/s 16(iii)" name="professionalTax" value={data.professionalTax || ""} onChange={onChange} />
        <FieldRow label="Total Deductions u/s 16" name="totalDeductions16" value={data.totalDeductions16 || ""} onChange={onChange} readOnly bold />

        <FieldRow label="Net Salary Income" name="netSalary" value={data.netSalary || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default SalaryIncomeSection;
