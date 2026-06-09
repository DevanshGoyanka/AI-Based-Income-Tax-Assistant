import { Outlet, Navigate } from 'react-router-dom';
import type { PermissionString } from '../types/api.types';

interface ProtectedRouteProps {
  requiredPermission?: PermissionString;
  redirectTo?: string;
}

export default function ProtectedRoute({
  requiredPermission,
  redirectTo = '/login'
}: ProtectedRouteProps) {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    return <Navigate to={redirectTo} replace />;
  }
  // Permission check placeholder — in real app, read from AuthContext
  if (requiredPermission) {
    const userPermissions = JSON.parse(
      localStorage.getItem('userPermissions') || '[]'
    ) as PermissionString[];
    if (!userPermissions.includes(requiredPermission)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }
  return <Outlet />;
}
