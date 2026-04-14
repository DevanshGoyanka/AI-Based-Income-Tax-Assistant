import { useState, useCallback } from "react";
import ComputationHeader from "./ComputationHeader";
import PersonalInfoSection from "./PersonalInfoSection";
import SalaryIncomeSection from "./SalaryIncomeSection";
import HousePropertySection from "./HousePropertySection";
import BusinessIncomeSection from "./BusinessIncomeSection";
import CapitalGainsSection from "./CapitalGainsSection";
import OtherSourcesSection from "./OtherSourcesSection";
import LossSetOffSection from "./LossSetOffSection";
import DeductionsSection from "./DeductionsSection";
import TaxComputationSection from "./TaxComputationSection";
import TaxPaidSection from "./TaxPaidSection";

const ComputationWindow = () => {
  const [itrFormType, setItrFormType] = useState("ITR-1");
  const [formData, setFormData] = useState<Record<string, string>>({});
  const [mismatches] = useState<string[]>([
    "PAN mismatch: AIS shows ABCPK1234R, Form 16 shows ABCPK1234S",
    "TDS figure mismatch: 26AS ₹1,24,500 vs Form 16 ₹1,22,000",
  ]);

  const handleFieldChange = useCallback((name: string, value: string) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleImportPrefill = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".json";
    input.onchange = () => { /* TODO: Parse JSON */ };
    input.click();
  };

  const handleImportForm16 = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".pdf";
    input.onchange = () => { /* TODO: Parse PDF */ };
    input.click();
  };

  return (
    <div className="h-screen flex flex-col bg-background overflow-hidden">
      <ComputationHeader
        clientName="Suresh Kumar"
        pan="ABCPK1234R"
        clientType="Individual"
        assessmentYear="AY 2025-26"
        itrFormType={itrFormType}
        onItrFormChange={setItrFormType}
        onImportPrefill={handleImportPrefill}
        onImportForm16={handleImportForm16}
        onSave={() => {}}
        onValidate={() => {}}
        onDownloadPDF={() => {}}
        onExportJSON={() => {}}
        mismatches={mismatches}
      />

      {/* Scrollable Content */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-5xl mx-auto py-6 px-6 space-y-3">
          {/* Navigation Summary */}
          <div className="bg-card border border-border rounded-lg p-4">
            <div className="grid grid-cols-5 gap-3">
              {[
                { label: "Salary", value: formData.netSalary || "0", color: "border-accent-blue" },
                { label: "House Property", value: formData.netHPIncome || "0", color: "border-accent-teal" },
                { label: "Business", value: formData.netBusinessIncome || "0", color: "border-warning" },
                { label: "Capital Gains", value: formData.netCapitalGains || "0", color: "border-accent-rose" },
                { label: "Other Sources", value: formData.netOtherIncome || "0", color: "border-gold" },
              ].map((item) => (
                <div key={item.label} className={`border-l-2 ${item.color} pl-3`}>
                  <p className="text-[10px] uppercase tracking-wider text-muted-foreground">{item.label}</p>
                  <p className="font-display text-lg font-semibold text-foreground">₹{item.value}</p>
                </div>
              ))}
            </div>
          </div>

          {/* All Sections */}
          <PersonalInfoSection data={formData} onChange={handleFieldChange} />
          <SalaryIncomeSection data={formData} onChange={handleFieldChange} />
          <HousePropertySection data={formData} onChange={handleFieldChange} />
          <BusinessIncomeSection data={formData} onChange={handleFieldChange} />
          <CapitalGainsSection data={formData} onChange={handleFieldChange} />
          <OtherSourcesSection data={formData} onChange={handleFieldChange} />
          <LossSetOffSection data={formData} onChange={handleFieldChange} />
          <DeductionsSection data={formData} onChange={handleFieldChange} />
          <TaxComputationSection data={formData} onChange={handleFieldChange} />
          <TaxPaidSection data={formData} onChange={handleFieldChange} />
        </div>
      </div>
    </div>
  );
};

export default ComputationWindow;
