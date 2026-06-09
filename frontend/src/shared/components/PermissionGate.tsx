import type React from 'react';
import type { PermissionString } from '../types/api.types';

interface PermissionGateProps {
  permission: PermissionString;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export default function PermissionGate({
  permission,
  children,
  fallback = null
}: PermissionGateProps) {
  const userPermissions = JSON.parse(
    localStorage.getItem('userPermissions') || '[]'
  ) as PermissionString[];
  if (!userPermissions.includes(permission)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}
