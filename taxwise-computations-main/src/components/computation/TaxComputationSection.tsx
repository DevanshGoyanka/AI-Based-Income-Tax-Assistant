import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Calculator } from "lucide-react";

interface TaxComputationSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const TaxComputationSection = ({ data, onChange }: TaxComputationSectionProps) => {
  return (
    <SectionCard
      title="Tax Computation"
      subtitle="Total Income, Tax Liability, Rebate, Surcharge, Cess"
      icon={<Calculator className="w-4 h-4 text-secondary-foreground" />}
      badge="Part B-TI & TTI"
      badgeColor="bg-secondary text-secondary-foreground"
      totalLabel="Net Tax Payable / Refund"
      totalValue={`₹${data.netTaxPayable || "0"}`}
      defaultOpen
    >
      <div className="space-y-0.5">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">Gross Total Income</p>
        <FieldRow label="Income from Salary" name="gtiSalary" value={data.gtiSalary || ""} onChange={onChange} readOnly />
        <FieldRow label="Income from House Property" name="gtiHP" value={data.gtiHP || ""} onChange={onChange} readOnly />
        <FieldRow label="Income from Business / Profession" name="gtiBusiness" value={data.gtiBusiness || ""} onChange={onChange} readOnly />
        <FieldRow label="Income from Capital Gains" name="gtiCapGains" value={data.gtiCapGains || ""} onChange={onChange} readOnly />
        <FieldRow label="Income from Other Sources" name="gtiOther" value={data.gtiOther || ""} onChange={onChange} readOnly />
        <FieldRow label="Gross Total Income" name="grossTotalIncome" value={data.grossTotalIncome || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Set-Off of Losses</p>
        <FieldRow label="Current Year Loss Set-Off (Intra-Head)" name="currentYearLossIntra" value={data.currentYearLossIntra || ""} onChange={onChange} />
        <FieldRow label="Current Year Loss Set-Off (Inter-Head)" name="currentYearLossInter" value={data.currentYearLossInter || ""} onChange={onChange} />
        <FieldRow label="Brought Forward Losses Set-Off" name="bfLossSetoff" value={data.bfLossSetoff || ""} onChange={onChange} />
        <FieldRow label="Income after Set-Off" name="incomeAfterSetoff" value={data.incomeAfterSetoff || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Deductions & Total Income</p>
        <FieldRow label="Total Deductions under Chapter VI-A" name="totalDeductionsVI" value={data.totalDeductionsVI || ""} onChange={onChange} readOnly />
        <FieldRow label="Total Taxable Income" name="totalTaxableIncome" value={data.totalTaxableIncome || ""} onChange={onChange} readOnly bold highlighted />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Tax Computation</p>
        <FieldRow label="Tax on Normal Income (Slab Rate)" name="taxOnNormalIncome" value={data.taxOnNormalIncome || ""} onChange={onChange} readOnly />
        <FieldRow label="Tax on STCG u/s 111A (15%)" name="taxSTCG111a" value={data.taxSTCG111a || ""} onChange={onChange} readOnly />
        <FieldRow label="Tax on LTCG u/s 112A (12.5%)" name="taxLTCG112a" value={data.taxLTCG112a || ""} onChange={onChange} readOnly />
        <FieldRow label="Tax on LTCG u/s 112 (12.5%)" name="taxLTCG112" value={data.taxLTCG112 || ""} onChange={onChange} readOnly />
        <FieldRow label="Tax on Winnings / VDA (30%)" name="taxWinnings" value={data.taxWinnings || ""} onChange={onChange} readOnly />
        <FieldRow label="Gross Tax Liability" name="grossTaxLiability" value={data.grossTaxLiability || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Rebate, Surcharge & Cess</p>
        <FieldRow label="Rebate u/s 87A" name="rebate87a" value={data.rebate87a || ""} onChange={onChange} readOnly hint="Up to ₹60,000 (New) / ₹12,500 (Old)" />
        <FieldRow label="Tax after Rebate" name="taxAfterRebate" value={data.taxAfterRebate || ""} onChange={onChange} readOnly />
        <FieldRow label="Surcharge" name="surcharge" value={data.surcharge || ""} onChange={onChange} readOnly hint="Varies by income slab" />
        <FieldRow label="Health & Education Cess (4%)" name="cess" value={data.cess || ""} onChange={onChange} readOnly />
        <FieldRow label="Total Tax Liability" name="totalTaxLiability" value={data.totalTaxLiability || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Relief & Credit</p>
        <FieldRow label="Relief u/s 89 (Salary Arrears)" name="relief89" value={data.relief89 || ""} onChange={onChange} />
        <FieldRow label="Relief u/s 90/90A (DTAA)" name="relief90" value={data.relief90 || ""} onChange={onChange} />
        <FieldRow label="Relief u/s 91 (Foreign Tax Credit)" name="relief91" value={data.relief91 || ""} onChange={onChange} />
        <FieldRow label="MAT/AMT Credit u/s 115JAA/115JD" name="matCredit" value={data.matCredit || ""} onChange={onChange} />
        <FieldRow label="Net Tax Liability after Relief" name="netTaxAfterRelief" value={data.netTaxAfterRelief || ""} onChange={onChange} readOnly bold />
      </div>
    </SectionCard>
  );
};

export default TaxComputationSection;
