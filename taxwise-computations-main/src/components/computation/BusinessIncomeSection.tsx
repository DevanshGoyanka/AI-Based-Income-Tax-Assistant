import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { Store } from "lucide-react";

interface BusinessIncomeSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const BusinessIncomeSection = ({ data, onChange }: BusinessIncomeSectionProps) => {
  return (
    <SectionCard
      title="Income from Business & Profession"
      subtitle="Sections 28–44 — Business Income, Presumptive Taxation"
      icon={<Store className="w-4 h-4 text-warning" />}
      badge="Schedule BP"
      totalLabel="Net Business Income"
      totalValue={`₹${data.netBusinessIncome || "0"}`}
    >
      <div className="space-y-0.5">
        <FieldRow label="Nature of Business" name="natureOfBusiness" value={data.natureOfBusiness || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Business Code" name="businessCode" value={data.businessCode || ""} onChange={onChange} type="text" prefix="" />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Presumptive Income u/s 44AD</p>
        <FieldRow label="Gross Turnover / Receipts" name="grossTurnover44ad" value={data.grossTurnover44ad || ""} onChange={onChange} />
        <FieldRow label="Gross Receipts (Digital)" name="grossReceiptsDigital" value={data.grossReceiptsDigital || ""} onChange={onChange} hint="6% for digital receipts" />
        <FieldRow label="Gross Receipts (Cash/Other)" name="grossReceiptsCash" value={data.grossReceiptsCash || ""} onChange={onChange} hint="8% for cash receipts" />
        <FieldRow label="Presumptive Income u/s 44AD" name="presumptiveIncome44ad" value={data.presumptiveIncome44ad || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Presumptive Income u/s 44ADA (Profession)</p>
        <FieldRow label="Gross Receipts from Profession" name="grossReceipts44ada" value={data.grossReceipts44ada || ""} onChange={onChange} />
        <FieldRow label="Presumptive Income u/s 44ADA (50%)" name="presumptiveIncome44ada" value={data.presumptiveIncome44ada || ""} onChange={onChange} readOnly hint="50% of gross receipts" />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Presumptive Income u/s 44AE (Goods Carriage)</p>
        <FieldRow label="Number of Heavy Goods Vehicles" name="heavyGoodsVehicles" value={data.heavyGoodsVehicles || ""} onChange={onChange} prefix="" />
        <FieldRow label="Number of Other Vehicles" name="otherVehicles" value={data.otherVehicles || ""} onChange={onChange} prefix="" />
        <FieldRow label="Presumptive Income u/s 44AE" name="presumptiveIncome44ae" value={data.presumptiveIncome44ae || ""} onChange={onChange} readOnly />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Normal Business Income (Non-Presumptive)</p>
        <FieldRow label="Net Profit as per P&L Account" name="netProfitPL" value={data.netProfitPL || ""} onChange={onChange} />
        <FieldRow label="Additions as per Income Tax Act" name="additionsIT" value={data.additionsIT || ""} onChange={onChange} />
        <FieldRow label="Deductions as per Income Tax Act" name="deductionsIT" value={data.deductionsIT || ""} onChange={onChange} />
        <FieldRow label="Adjusted Net Profit" name="adjustedNetProfit" value={data.adjustedNetProfit || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Depreciation</p>
        <FieldRow label="Depreciation as per Books" name="depreciationBooks" value={data.depreciationBooks || ""} onChange={onChange} />
        <FieldRow label="Depreciation as per IT Act" name="depreciationIT" value={data.depreciationIT || ""} onChange={onChange} />
        <FieldRow label="Additional Depreciation u/s 32(1)(iia)" name="additionalDepreciation" value={data.additionalDepreciation || ""} onChange={onChange} />

        <FieldRow label="Net Income from Business / Profession" name="netBusinessIncome" value={data.netBusinessIncome || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default BusinessIncomeSection;
