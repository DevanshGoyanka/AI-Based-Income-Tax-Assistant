import React from 'react';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { useAppStore } from '../../store/useAppStore';
import Dashboard from '../../pages/Dashboard';
import ClientMaster from '../../pages/ClientMaster';
import ITRFiling from '../../pages/ITRFiling';
import Reconciliation from '../../pages/Reconciliation';
import Sync from '../../pages/Sync';
import Jobs from '../../pages/Jobs';
import Notices from '../../pages/Notices';
import Calendar from '../../pages/Calendar';
import Tasks from '../../pages/Tasks';
import Billing from '../../pages/Billing';
import Accounting from '../../pages/Accounting';
import Reports from '../../pages/Reports';
import Communication from '../../pages/Communication';
import ClientDetailPanel from '../domain/ClientDetailPanel';
import Computation from '../../pages/Computation';

const viewMap: Record<string, React.ReactNode> = {
  dashboard: <Dashboard />,
  clients: <ClientMaster />,
  filing: <ITRFiling />,
  reconciliation: <Reconciliation />,
  sync: <Sync />,
  jobs: <Jobs />,
  notices: <Notices />,
  calendar: <Calendar />,
  tasks: <Tasks />,
  billing: <Billing />,
  accounting: <Accounting />,
  reports: <Reports />,
  communication: <Communication />,
};

export default function Layout() {
  const { activeView, selectedClientId, setSelectedClientId, computationClientId } = useAppStore();
  return (
    <>
      <div className="h-screen overflow-hidden flex bg-bg">
        <Sidebar />
        <div className="flex flex-col flex-1 ml-[258px] overflow-hidden">
          <Topbar />
          <main className="flex-1 overflow-y-auto p-7 scrollbar-thin">
            {viewMap[activeView] ?? <Dashboard />}
          </main>
        </div>
        {selectedClientId !== null && (
          <ClientDetailPanel clientId={selectedClientId} onClose={() => setSelectedClientId(null)} />
        )}
      </div>
      {computationClientId !== null && <Computation />}
    </>
  );
}
