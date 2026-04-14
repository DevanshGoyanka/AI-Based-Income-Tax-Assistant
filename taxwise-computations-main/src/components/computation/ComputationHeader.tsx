import { useState } from "react";
import { ChevronDown, Upload, Save, CheckCircle, FileDown, FileJson, AlertTriangle, User } from "lucide-react";

interface ComputationHeaderProps {
  clientName: string;
  pan: string;
  clientType: string;
  assessmentYear: string;
  itrFormType: string;
  onItrFormChange: (type: string) => void;
  onImportPrefill: () => void;
  onImportForm16: () => void;
  onSave: () => void;
  onValidate: () => void;
  onDownloadPDF: () => void;
  onExportJSON: () => void;
  mismatches?: string[];
}

const ITR_FORMS = ["ITR-1", "ITR-2", "ITR-3", "ITR-4", "ITR-5", "ITR-6", "ITR-7"];

const ComputationHeader = ({
  clientName,
  pan,
  clientType,
  assessmentYear,
  itrFormType,
  onItrFormChange,
  onImportPrefill,
  onImportForm16,
  onSave,
  onValidate,
  onDownloadPDF,
  onExportJSON,
  mismatches = [],
}: ComputationHeaderProps) => {
  const [showMismatches, setShowMismatches] = useState(false);

  return (
    <div className="flex-shrink-0">
      {/* Top Client Info Bar */}
      <div className="bg-secondary px-6 py-3 flex items-center gap-6">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-primary flex items-center justify-center">
            <User className="w-4 h-4 text-primary-foreground" />
          </div>
          <div>
            <h1 className="text-sm font-semibold text-secondary-foreground">{clientName}</h1>
            <span className="text-xs font-mono text-gold-light tracking-wider">{pan}</span>
          </div>
        </div>

        <div className="h-6 w-px bg-navy-light" />

        <div className="flex items-center gap-2">
          <span className="text-xs uppercase tracking-wider text-muted-foreground">Type</span>
          <span className="px-2.5 py-0.5 bg-navy-light rounded text-xs font-medium text-secondary-foreground">{clientType}</span>
        </div>

        <div className="h-6 w-px bg-navy-light" />

        <div className="flex items-center gap-2">
          <span className="text-xs uppercase tracking-wider text-muted-foreground">AY</span>
          <span className="px-2.5 py-0.5 bg-navy-light rounded text-xs font-medium text-gold-light">{assessmentYear}</span>
        </div>

        <div className="h-6 w-px bg-navy-light" />

        <div className="flex items-center gap-2">
          <span className="text-xs uppercase tracking-wider text-muted-foreground">Form</span>
          <select
            value={itrFormType}
            onChange={(e) => onItrFormChange(e.target.value)}
            className="bg-navy-light border border-navy-light text-secondary-foreground rounded px-2.5 py-1 text-xs font-medium focus:border-primary focus:outline-none cursor-pointer"
          >
            {ITR_FORMS.map((f) => (
              <option key={f} value={f}>{f}</option>
            ))}
          </select>
        </div>

        <div className="ml-auto flex items-center gap-1.5">
          <span className="text-[10px] uppercase tracking-widest text-muted-foreground mr-1">Status</span>
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-warning-bg text-warning">Draft</span>
        </div>
      </div>

      {/* Action Bar */}
      <div className="bg-card border-b border-border px-6 py-2.5 flex items-center gap-2">
        <button onClick={onImportPrefill} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-accent-blue/10 text-accent-blue hover:bg-accent-blue/20 transition-colors">
          <Upload className="w-3.5 h-3.5" />
          Import Prefill JSON (AIS/TIS/Form 16)
        </button>
        <button onClick={onImportForm16} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-accent-teal/10 text-accent-teal hover:bg-accent-teal/20 transition-colors">
          <FileDown className="w-3.5 h-3.5" />
          Import Form 16 PDF
        </button>

        {mismatches.length > 0 && (
          <button
            onClick={() => setShowMismatches(!showMismatches)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-danger-bg text-danger hover:bg-danger/10 transition-colors"
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            {mismatches.length} Mismatch{mismatches.length > 1 ? "es" : ""}
            <ChevronDown className="w-3 h-3" />
          </button>
        )}

        <div className="ml-auto flex items-center gap-2">
          <button onClick={onSave} className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md text-xs font-medium bg-muted text-foreground border border-border hover:bg-border transition-colors">
            <Save className="w-3.5 h-3.5" />
            Save
          </button>
          <button onClick={onValidate} className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md text-xs font-medium bg-success-bg text-success border border-success/20 hover:bg-success/10 transition-colors">
            <CheckCircle className="w-3.5 h-3.5" />
            Validate
          </button>
          <button onClick={onDownloadPDF} className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md text-xs font-medium bg-info-bg text-info border border-info/20 hover:bg-info/10 transition-colors">
            <FileDown className="w-3.5 h-3.5" />
            Statement of Income
          </button>
          <button onClick={onExportJSON} className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-md text-xs font-semibold bg-primary text-primary-foreground hover:bg-gold-light transition-colors">
            <FileJson className="w-3.5 h-3.5" />
            Export ITR JSON
          </button>
        </div>
      </div>

      {/* Mismatch Banner */}
      {showMismatches && mismatches.length > 0 && (
        <div className="bg-danger-bg border-b border-danger/20 px-6 py-2.5">
          <div className="flex items-start gap-2">
            <AlertTriangle className="w-4 h-4 text-danger mt-0.5 flex-shrink-0" />
            <div className="flex-1">
              <p className="text-xs font-semibold text-danger mb-1">Data Mismatches Detected</p>
              <ul className="space-y-0.5">
                {mismatches.map((m, i) => (
                  <li key={i} className="text-xs text-danger/80">• {m}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ComputationHeader;
