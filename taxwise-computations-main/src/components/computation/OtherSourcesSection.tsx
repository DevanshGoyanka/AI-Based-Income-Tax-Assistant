import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Coins } from "lucide-react";

interface OtherSourcesSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const OtherSourcesSection = ({ data, onChange }: OtherSourcesSectionProps) => {
  return (
    <SectionCard
      title="Income from Other Sources"
      subtitle="Sections 56–59 — Interest, Dividends, Gifts, etc."
      icon={<Coins className="w-4 h-4 text-gold" />}
      badge="Schedule OS"
      totalLabel="Net Other Income"
      totalValue={`₹${data.netOtherIncome || "0"}`}
    >
      <div className="space-y-0.5">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">Interest Income</p>
        <FieldRow label="Interest from Savings Bank Account" name="interestSavings" value={data.interestSavings || ""} onChange={onChange} />
        <FieldRow label="Interest from Fixed Deposits" name="interestFD" value={data.interestFD || ""} onChange={onChange} />
        <FieldRow label="Interest from Income Tax Refund" name="interestITRefund" value={data.interestITRefund || ""} onChange={onChange} />
        <FieldRow label="Interest from NSC / KVP / PPF (Taxable)" name="interestNSC" value={data.interestNSC || ""} onChange={onChange} />
        <FieldRow label="Interest from Securities / Debentures" name="interestSecurities" value={data.interestSecurities || ""} onChange={onChange} />
        <FieldRow label="Other Interest Income" name="interestOther" value={data.interestOther || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Dividend Income</p>
        <FieldRow label="Dividend Income (Taxable)" name="dividendIncome" value={data.dividendIncome || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Other Taxable Income</p>
        <FieldRow label="Rental Income from Machinery / Plant" name="rentalMachinery" value={data.rentalMachinery || ""} onChange={onChange} />
        <FieldRow label="Income from letting out of Furniture / Fittings" name="rentalFurniture" value={data.rentalFurniture || ""} onChange={onChange} />
        <FieldRow label="Gifts / Aggregate value u/s 56(2)(x)" name="gifts56_2x" value={data.gifts56_2x || ""} onChange={onChange} hint="Taxable if > ₹50,000" />
        <FieldRow label="Winnings from Lottery / Crossword / Races" name="winningsLottery" value={data.winningsLottery || ""} onChange={onChange} hint="Taxed at 30% flat" />
        <FieldRow label="Income from Virtual Digital Assets" name="incomeVDA" value={data.incomeVDA || ""} onChange={onChange} hint="Taxed at 30% u/s 115BBH" />
        <FieldRow label="Agricultural Income (for rate purposes)" name="agriculturalIncome" value={data.agriculturalIncome || ""} onChange={onChange} hint="Exempt but used for rate" />
        <FieldRow label="Family Pension (after deduction)" name="familyPension" value={data.familyPension || ""} onChange={onChange} hint="Deduction: ₹15,000 or 1/3rd" />
        <FieldRow label="Any Other Income" name="anyOtherIncome" value={data.anyOtherIncome || ""} onChange={onChange} />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Deductions from Other Sources</p>
        <FieldRow label="Deduction u/s 57 (Expenses against income)" name="deductions57" value={data.deductions57 || ""} onChange={onChange} />

        <FieldRow label="Net Income from Other Sources" name="netOtherIncome" value={data.netOtherIncome || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default OtherSourcesSection;
