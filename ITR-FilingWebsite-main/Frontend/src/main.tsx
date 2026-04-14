import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import Layout from './components/shell/Layout';
import Computation from './pages/Computation';
import Login from './pages/Login';
import { useAppStore } from './store/useAppStore';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: (failureCount, error: any) => {
        if (error?.response?.status === 404) return false;
        return failureCount < 2;
      },
    },
  },
});

function App() {
  const { computationClientId } = useAppStore();
  const token = localStorage.getItem('itr_erp_token');

  if (!token) {
    return <Login />;
  }

  return (
    <>
      <Layout />
      {computationClientId !== null && <Computation />}
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="bottom-right"
        toastOptions={{
          duration: 4000,
          style: { fontFamily: 'DM Sans, sans-serif', fontSize: '13px' },
          success: { iconTheme: { primary: '#15803D', secondary: '#DCFCE7' } },
          error: { iconTheme: { primary: '#B91C1C', secondary: '#FEE2E2' } },
        }}
      />
    </QueryClientProvider>
  </React.StrictMode>
);
