interface AvatarProps {
  name: string;
  size?: 'sm' | 'md' | 'lg';
  color?: string;
}

export default function Avatar({ name, size = 'md', color }: AvatarProps) {
  const initials = name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  const sizeStyles = {
    sm: 'w-8 h-8 text-[11px]',
    md: 'w-10 h-10 text-[13px]',
    lg: 'w-12 h-12 text-[15px]',
  };

  const bgColor = color || '#C9943A';

  return (
    <div
      className={`${sizeStyles[size]} rounded-full flex items-center justify-center font-bold text-navy flex-shrink-0`}
      style={{ backgroundColor: bgColor }}
    >
      {initials}
    </div>
  );
}
