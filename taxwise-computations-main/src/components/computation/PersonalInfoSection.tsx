import { User } from "lucide-react";
import SectionCard from "./SectionCard";
import FieldRow from "./FieldRow";

interface PersonalInfoSectionProps {
  data: Record<string, string>;
  onChange: (name: string, value: string) => void;
}

const PersonalInfoSection = ({ data, onChange }: PersonalInfoSectionProps) => {
  return (
    <SectionCard
      title="Personal Information"
      subtitle="Part A — General Information"
      icon={<User className="w-4 h-4 text-primary" />}
      badge="Part A"
      defaultOpen={true}
    >
      <div className="space-y-1">
        {/* Basic Identity */}
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground px-3 pt-2 pb-1">
          Identity Details
        </p>
        <FieldRow label="First Name" name="firstName" value={data.firstName || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Middle Name" name="middleName" value={data.middleName || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Last Name / Surname" name="lastName" value={data.lastName || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="PAN" name="pan" value={data.pan || ""} onChange={onChange} type="text" prefix="" readOnly hint="Auto-populated from client profile" />
        <FieldRow label="Aadhaar Number / Enrolment ID" name="aadhaarNumber" value={data.aadhaarNumber || ""} onChange={onChange} type="text" prefix="" hint="12-digit Aadhaar or 28-digit Enrolment ID" />
        <FieldRow label="Date of Birth (DD/MM/YYYY)" name="dateOfBirth" value={data.dateOfBirth || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow
          label="Gender"
          name="gender"
          value={data.gender || "M"}
          onChange={onChange}
          type="select"
          options={[
            { label: "Male", value: "M" },
            { label: "Female", value: "F" },
            { label: "Transgender", value: "T" },
          ]}
        />
        <FieldRow label="Father's Name" name="fatherName" value={data.fatherName || ""} onChange={onChange} type="text" prefix="" />

        {/* Filing Status */}
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground px-3 pt-4 pb-1">
          Filing Status
        </p>
        <FieldRow
          label="Filing Status (Section 139)"
          name="filingStatus"
          value={data.filingStatus || "139(1)"}
          onChange={onChange}
          type="select"
          options={[
            { label: "On or before due date u/s 139(1)", value: "139(1)" },
            { label: "After due date u/s 139(4) — Belated", value: "139(4)" },
            { label: "Revised u/s 139(5)", value: "139(5)" },
            { label: "In response to notice u/s 139(9)", value: "139(9)" },
            { label: "In response to notice u/s 142(1)", value: "142(1)" },
            { label: "In response to notice u/s 148", value: "148" },
            { label: "In response to notice u/s 153A", value: "153A" },
            { label: "In response to notice u/s 153C", value: "153C" },
          ]}
        />
        <FieldRow
          label="Whether original or revised return?"
          name="returnType"
          value={data.returnType || "O"}
          onChange={onChange}
          type="select"
          options={[
            { label: "Original", value: "O" },
            { label: "Revised", value: "R" },
          ]}
        />
        <FieldRow label="Original Acknowledgement No. (if revised)" name="originalAckNo" value={data.originalAckNo || ""} onChange={onChange} type="text" prefix="" hint="Required if filing revised return" />
        <FieldRow label="Date of filing original return (DD/MM/YYYY)" name="originalFilingDate" value={data.originalFilingDate || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow
          label="Residential Status (Section 6)"
          name="residentialStatus"
          value={data.residentialStatus || "RES"}
          onChange={onChange}
          type="select"
          options={[
            { label: "Resident (RES)", value: "RES" },
            { label: "Resident but Not Ordinarily Resident (RNOR)", value: "RNOR" },
            { label: "Non-Resident (NR)", value: "NR" },
          ]}
        />
        <FieldRow
          label="Whether opting for new tax regime u/s 115BAC?"
          name="newRegime115BAC"
          value={data.newRegime115BAC || "Y"}
          onChange={onChange}
          type="select"
          options={[
            { label: "Yes", value: "Y" },
            { label: "No", value: "N" },
          ]}
          hint="Default regime from AY 2024-25 onwards"
        />

        {/* Contact Details */}
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground px-3 pt-4 pb-1">
          Contact & Address
        </p>
        <FieldRow label="Mobile Number" name="mobileNo" value={data.mobileNo || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Email Address" name="emailAddress" value={data.emailAddress || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Flat/Door/Block No." name="addrFlatNo" value={data.addrFlatNo || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Name of Premises/Building/Village" name="addrPremises" value={data.addrPremises || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Road/Street/Lane" name="addrRoad" value={data.addrRoad || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Area/Locality" name="addrArea" value={data.addrArea || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Town/City/District" name="addrCity" value={data.addrCity || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="State" name="addrState" value={data.addrState || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="PIN Code" name="addrPinCode" value={data.addrPinCode || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Country (if Non-Resident)" name="addrCountry" value={data.addrCountry || "India"} onChange={onChange} type="text" prefix="" />

        {/* Bank Account */}
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground px-3 pt-4 pb-1">
          Bank Account (for Refund)
        </p>
        <FieldRow label="Bank Name" name="bankName" value={data.bankName || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="IFSC Code" name="bankIFSC" value={data.bankIFSC || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Account Number" name="bankAccountNo" value={data.bankAccountNo || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow
          label="Account Type"
          name="bankAccountType"
          value={data.bankAccountType || "SB"}
          onChange={onChange}
          type="select"
          options={[
            { label: "Savings", value: "SB" },
            { label: "Current", value: "CA" },
          ]}
        />

        {/* Employer Details */}
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground px-3 pt-4 pb-1">
          Employer Details (if salaried)
        </p>
        <FieldRow label="Employer Category" name="employerCategory" value={data.employerCategory || ""} onChange={onChange} type="select" options={[
          { label: "Central Government", value: "CG" },
          { label: "State Government", value: "SG" },
          { label: "Public Sector Undertaking", value: "PSU" },
          { label: "Pensioner — CG", value: "PE_CG" },
          { label: "Pensioner — SG", value: "PE_SG" },
          { label: "Pensioner — PSU", value: "PE_PSU" },
          { label: "Pensioner — Others", value: "PE_OTH" },
          { label: "Others", value: "OTH" },
          { label: "Not Applicable", value: "NA" },
        ]} />
        <FieldRow label="Employer's TAN" name="employerTAN" value={data.employerTAN || ""} onChange={onChange} type="text" prefix="" />
        <FieldRow label="Employer's Name" name="employerName" value={data.employerName || ""} onChange={onChange} type="text" prefix="" />
      </div>
    </SectionCard>
  );
};

export default PersonalInfoSection;
