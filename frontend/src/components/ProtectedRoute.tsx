import { Navigate, Outlet } from 'react-router-dom';
import { tokenManager } from '../lib/api/tokenManager';

export const ProtectedRoute = () =>
  tokenManager.isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
