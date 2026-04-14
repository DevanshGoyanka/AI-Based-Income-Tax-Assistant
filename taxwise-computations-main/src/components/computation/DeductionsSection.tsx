import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";
import { ShieldCheck } from "lucide-react";

interface DeductionsSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const DeductionsSection = ({ data, onChange }: DeductionsSectionProps) => {
  return (
    <SectionCard
      title="Deductions under Chapter VI-A"
      subtitle="Sections 80C to 80U — Tax Saving Investments & Deductions"
      icon={<ShieldCheck className="w-4 h-4 text-success" />}
      badge="Schedule VIA"
      badgeColor="bg-success-bg text-success"
      totalLabel="Total Deductions"
      totalValue={`₹${data.totalDeductionsVIA || "0"}`}
    >
      <div className="space-y-0.5">
        <FieldRow label="Tax Regime" name="taxRegime" value={data.taxRegime || "new"} onChange={onChange} type="select"
          options={[
            { label: "New Tax Regime (Default)", value: "new" },
            { label: "Old Tax Regime", value: "old" },
          ]}
        />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-3 mb-2">Section 80C / 80CCC / 80CCD(1) — Max ₹1,50,000</p>
        <FieldRow label="Life Insurance Premium" name="lic80c" value={data.lic80c || ""} onChange={onChange} />
        <FieldRow label="PPF / SPF / RPF" name="ppf80c" value={data.ppf80c || ""} onChange={onChange} />
        <FieldRow label="NSC (incl. accrued interest)" name="nsc80c" value={data.nsc80c || ""} onChange={onChange} />
        <FieldRow label="ELSS / Tax Saving Mutual Funds" name="elss80c" value={data.elss80c || ""} onChange={onChange} />
        <FieldRow label="5-Year Tax Saving FD" name="fd80c" value={data.fd80c || ""} onChange={onChange} />
        <FieldRow label="Children Tuition Fees (Max 2 children)" name="tuition80c" value={data.tuition80c || ""} onChange={onChange} />
        <FieldRow label="Housing Loan Principal Repayment" name="hlPrincipal80c" value={data.hlPrincipal80c || ""} onChange={onChange} />
        <FieldRow label="Stamp Duty & Registration Charges" name="stampDuty80c" value={data.stampDuty80c || ""} onChange={onChange} />
        <FieldRow label="Sukanya Samriddhi Account" name="ssa80c" value={data.ssa80c || ""} onChange={onChange} />
        <FieldRow label="Contribution to Pension u/s 80CCC" name="pension80ccc" value={data.pension80ccc || ""} onChange={onChange} />
        <FieldRow label="Employee NPS u/s 80CCD(1)" name="nps80ccd1" value={data.nps80ccd1 || ""} onChange={onChange} />
        <FieldRow label="Other u/s 80C" name="other80c" value={data.other80c || ""} onChange={onChange} />
        <FieldRow label="Total 80C/80CCC/80CCD(1) (Max ₹1.5L)" name="total80c" value={data.total80c || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Additional NPS Deductions</p>
        <FieldRow label="Employer NPS u/s 80CCD(2)" name="nps80ccd2" value={data.nps80ccd2 || ""} onChange={onChange} hint="14% of salary (Central Govt) / 10% others" />
        <FieldRow label="Self Contribution NPS u/s 80CCD(1B)" name="nps80ccd1b" value={data.nps80ccd1b || ""} onChange={onChange} hint="Additional ₹50,000" />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Health Insurance — Section 80D</p>
        <FieldRow label="Premium for Self / Family" name="health80dSelf" value={data.health80dSelf || ""} onChange={onChange} hint="Max ₹25,000 (₹50,000 if senior)" />
        <FieldRow label="Preventive Health Check-up" name="healthCheckup80d" value={data.healthCheckup80d || ""} onChange={onChange} hint="Max ₹5,000 (within overall limit)" />
        <FieldRow label="Premium for Parents" name="health80dParents" value={data.health80dParents || ""} onChange={onChange} hint="Max ₹25,000 (₹50,000 if senior)" />
        <FieldRow label="Total 80D" name="total80d" value={data.total80d || ""} onChange={onChange} readOnly bold />

        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mt-4 mb-2">Other Deductions</p>
        <FieldRow label="Interest on Education Loan u/s 80E" name="educationLoan80e" value={data.educationLoan80e || ""} onChange={onChange} hint="No upper limit, max 8 years" />
        <FieldRow label="Donations u/s 80G" name="donations80g" value={data.donations80g || ""} onChange={onChange} />
        <FieldRow label="Rent Paid u/s 80GG" name="rent80gg" value={data.rent80gg || ""} onChange={onChange} hint="When HRA not received" />
        <FieldRow label="Interest on Savings u/s 80TTA" name="interest80tta" value={data.interest80tta || ""} onChange={onChange} hint="Max ₹10,000" />
        <FieldRow label="Interest on Deposits u/s 80TTB (Senior)" name="interest80ttb" value={data.interest80ttb || ""} onChange={onChange} hint="Max ₹50,000 for Sr. Citizens" />
        <FieldRow label="Deduction for Disabled u/s 80U" name="disabled80u" value={data.disabled80u || ""} onChange={onChange} hint="₹75,000 / ₹1,25,000" />
        <FieldRow label="Deduction u/s 80DD (Dependent Disabled)" name="dependent80dd" value={data.dependent80dd || ""} onChange={onChange} />
        <FieldRow label="Deduction u/s 80DDB (Medical Treatment)" name="medical80ddb" value={data.medical80ddb || ""} onChange={onChange} />
        <FieldRow label="Deduction u/s 80EEA (Interest on Affordable Housing)" name="housing80eea" value={data.housing80eea || ""} onChange={onChange} />
        <FieldRow label="Deduction u/s 80EEB (Interest on Electric Vehicle)" name="ev80eeb" value={data.ev80eeb || ""} onChange={onChange} />

        <FieldRow label="Total Deductions under Chapter VI-A" name="totalDeductionsVIA" value={data.totalDeductionsVIA || ""} onChange={onChange} readOnly bold highlighted />
      </div>
    </SectionCard>
  );
};

export default DeductionsSection;
