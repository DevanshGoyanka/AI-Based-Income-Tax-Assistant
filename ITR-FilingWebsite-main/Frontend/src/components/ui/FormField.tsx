import React from 'react';

interface FormFieldProps {
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}

export default function FormField({ label, error, required, children }: FormFieldProps) {
  return (
    <div className="mb-4">
      <label className="block text-[13px] font-medium text-text-primary mb-1.5">
        {label}
        {required && <span className="text-danger ml-1">*</span>}
      </label>
      {children}
      {error && <p className="text-[12px] text-danger mt-1">{error}</p>}
    </div>
  );
}
