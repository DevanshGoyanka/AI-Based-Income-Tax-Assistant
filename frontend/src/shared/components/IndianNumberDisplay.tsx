import type React from 'react';

interface IndianNumberDisplayProps {
  paise: number;
  className?: string;
  showSymbol?: boolean;
}

/**
 * IndianNumberDisplay — shows amount in Indian format ₹X,XX,XXX (read-only).
 */
export default function IndianNumberDisplay({
  paise,
  className = '',
  showSymbol = true,
}: IndianNumberDisplayProps) {
  const formatIndian = (p: number): string => {
    const numStr = String(Math.abs(p));
    const len = numStr.length;
    if (len <= 3) return numStr;
    const last3 = numStr.slice(-3);
    const rest = numStr.slice(0, -3);
    const groups: string[] = [];
    for (let i = rest.length; i > 0; i -= 2) {
      groups.unshift(rest.slice(Math.max(0, i - 2), i));
    }
    return (p < 0 ? '-' : '') + groups.join(',') + ',' + last3;
  };

  const rupees = Math.floor(paise / 100);
  const remaining = Math.abs(paise % 100);
  const formatted = formatIndian(rupees);

  return (
    <span className={className}>
      {showSymbol && '₹'}{formatted}.{remaining.toString().padStart(2, '0')}
    </span>
  );
}
