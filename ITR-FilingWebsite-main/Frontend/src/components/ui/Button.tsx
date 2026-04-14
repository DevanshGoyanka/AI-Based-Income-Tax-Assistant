import React from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: React.ReactNode;
  loading?: boolean;
}

const variantStyles: Record<ButtonVariant, string> = {
  primary: 'bg-gold text-navy hover:bg-gold-light border-gold',
  secondary: 'bg-bg-card text-text-primary hover:bg-bg border-border-strong',
  ghost: 'bg-transparent text-text-secondary hover:bg-bg border-transparent',
  danger: 'bg-danger text-white hover:bg-danger/90 border-danger',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'text-[12px] px-3 py-1.5 h-[30px]',
  md: 'text-[13px] px-4 py-2 h-[36px]',
  lg: 'text-[14px] px-5 py-2.5 h-[42px]',
};

export default function Button({
  variant = 'primary',
  size = 'md',
  icon,
  loading,
  children,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      className={`
        inline-flex items-center justify-center gap-2 font-medium rounded-lg border
        transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed
        ${variantStyles[variant]} ${sizeStyles[size]} ${className}
      `}
      {...props}
    >
      {loading && (
        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" opacity="0.25" />
          <path d="M12 2a10 10 0 0 1 10 10" opacity="0.75" />
        </svg>
      )}
      {!loading && icon}
      {children}
    </button>
  );
}
