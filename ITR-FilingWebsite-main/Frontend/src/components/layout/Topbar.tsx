import { useLocation } from 'react-router-dom';
import { useAY } from '../../contexts/AYContext';

export const Topbar = () => {
  const location = useLocation();
  const { ay, setAY } = useAY();

  const getBreadcrumb = () => {
    const pathMap: Record<string, string> = {
      '/dashboard': 'Dashboard',
      '/clients': 'Client Master',
      '/filing': 'ITR Filing',
      '/reconciliation': 'Reconciliation',
      '/sync': 'ITD Portal Sync',
      '/jobs': 'Background Jobs',
      '/notices': 'Notice Management',
      '/calendar': 'Compliance Calendar',
      '/tasks': 'Tasks & Work Queue',
      '/billing': 'Billing & Fees',
      '/accounting': 'Firm Accounting',
      '/reports': 'Reports & Analytics',
      '/communication': 'Communication'
    };
    return pathMap[location.pathname] || 'Dashboard';
  };

  return (
    <div style={{
      height: 'var(--header-h)',
      background: 'white',
      borderBottom: '1px solid var(--border)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px'
    }}>
      <div className="crimson" style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>
        {getBreadcrumb()}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <select
          value={ay}
          onChange={(e) => setAY(e.target.value)}
          style={{
            padding: '8px 12px',
            border: '1px solid var(--border)',
            borderRadius: 6,
            fontSize: 13,
            fontWeight: 500,
            color: 'var(--text-primary)',
            background: 'white',
            cursor: 'pointer',
            outline: 'none'
          }}
        >
          <option value="AY 2025–26">AY 2025–26</option>
          <option value="AY 2026–27">AY 2026–27</option>
          <option value="Both AYs">Both AYs</option>
        </select>

        <button style={{
          padding: '8px 16px',
          background: 'var(--gold)',
          color: 'white',
          border: 'none',
          borderRadius: 6,
          fontSize: 13,
          fontWeight: 500,
          cursor: 'pointer'
        }}>
          Run Bulk Sync
        </button>
      </div>
    </div>
  );
};
