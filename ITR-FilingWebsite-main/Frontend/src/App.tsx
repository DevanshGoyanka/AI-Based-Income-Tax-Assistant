import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AYProvider } from './contexts/AYContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ClientsPage from './pages/ClientsPage';
import FilingPage from './pages/FilingPage';
import ITRComputationPage from './pages/ITRComputationPage';
import ReconciliationPage from './pages/ReconciliationPage';
import SyncPage from './pages/SyncPage';
import JobsPage from './pages/JobsPage';
import NoticesPage from './pages/NoticesPage';
import CalendarPage from './pages/CalendarPage';
import TasksPage from './pages/TasksPage';
import BillingPage from './pages/BillingPage';
import AccountingPage from './pages/AccountingPage';
import ReportsPage from './pages/ReportsPage';
import CommunicationPage from './pages/CommunicationPage';

export default function App() {
  return (
    <AYProvider>
      <BrowserRouter>
        <Toaster position="top-right" />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/clients" element={<ClientsPage />} />
              <Route path="/filing" element={<FilingPage />} />
              <Route path="/filing/:clientId/:year" element={<ITRComputationPage />} />
              <Route path="/reconciliation" element={<ReconciliationPage />} />
              <Route path="/sync" element={<SyncPage />} />
              <Route path="/jobs" element={<JobsPage />} />
              <Route path="/notices" element={<NoticesPage />} />
              <Route path="/calendar" element={<CalendarPage />} />
              <Route path="/tasks" element={<TasksPage />} />
              <Route path="/billing" element={<BillingPage />} />
              <Route path="/accounting" element={<AccountingPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/communication" element={<CommunicationPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AYProvider>
  );
}
