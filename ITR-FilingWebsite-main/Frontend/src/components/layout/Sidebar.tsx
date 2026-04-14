import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { tokenManager } from '../../lib/api/tokenManager';

export const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const isActive = (path: string) => location.pathname === path;

  const handleLogout = () => {
    tokenManager.clear();
    navigate('/login');
  };

  const navSections = [
    {
      title: 'Overview',
      items: [
        { path: '/dashboard', label: 'Dashboard', icon: '📊' }
      ]
    },
    {
      title: 'Clients & Filing',
      items: [
        { path: '/clients', label: 'Client Master', icon: '👥', badge: null },
        { path: '/filing', label: 'ITR Filing', icon: '📄', badge: null },
        { path: '/reconciliation', label: 'Reconciliation', icon: '🔄', badge: null }
      ]
    },
    {
      title: 'Automation',
      items: [
        { path: '/sync', label: 'ITD Portal Sync', icon: '🔗' },
        { path: '/jobs', label: 'Background Jobs', icon: '⚙️', badge: null }
      ]
    },
    {
      title: 'Compliance',
      items: [
        { path: '/notices', label: 'Notice Management', icon: '⚠️', badge: null },
        { path: '/calendar', label: 'Compliance Calendar', icon: '📅' },
        { path: '/tasks', label: 'Tasks & Work Queue', icon: '✓', badge: null }
      ]
    },
    {
      title: 'Finance',
      items: [
        { path: '/billing', label: 'Billing & Fees', icon: '💰' },
        { path: '/accounting', label: 'Firm Accounting', icon: '📊' }
      ]
    },
    {
      title: 'Analytics',
      items: [
        { path: '/reports', label: 'Reports & Analytics', icon: '📈' },
        { path: '/communication', label: 'Communication', icon: '💬' }
      ]
    }
  ];

  return (
    <div style={{
      width: 'var(--sidebar-w)',
      height: '100vh',
      background: 'var(--navy)',
      display: 'flex',
      flexDirection: 'column',
      position: 'fixed',
      left: 0,
      top: 0,
      overflowY: 'auto'
    }}>
      <div style={{ padding: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <div style={{
            width: 36, height: 36,
            background: 'var(--gold)',
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: 'Crimson Pro',
            fontWeight: 600,
            fontSize: 16,
            color: 'var(--navy)'
          }}>IT</div>
          <div>
            <div className="crimson" style={{ color: 'white', fontSize: 16, fontWeight: 600 }}>
              IncomeTax ERP
            </div>
            <div style={{ color: 'var(--gold-light)', fontSize: 10.5, textTransform: 'uppercase', letterSpacing: 0.5 }}>
              Advocate Practice
            </div>
          </div>
        </div>

        <input
          type="text"
          placeholder="Search clients, PAN…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{
            width: '100%',
            padding: '8px 12px',
            background: 'var(--navy-mid)',
            border: '1px solid var(--navy-light)',
            borderRadius: 6,
            color: 'white',
            fontSize: 13,
            outline: 'none'
          }}
        />
      </div>

      <nav style={{ flex: 1, padding: '0 8px' }}>
        {navSections.map((section, idx) => (
          <div key={idx} style={{ marginBottom: 20 }}>
            <div style={{
              fontSize: 10,
              fontWeight: 600,
              color: 'var(--text-muted)',
              textTransform: 'uppercase',
              letterSpacing: 1,
              padding: '8px 12px',
              marginBottom: 4
            }}>
              {section.title}
            </div>
            {section.items.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '10px 12px',
                  borderRadius: 6,
                  textDecoration: 'none',
                  color: isActive(item.path) ? 'var(--gold)' : 'var(--text-muted)',
                  background: isActive(item.path) ? 'rgba(201, 148, 58, 0.1)' : 'transparent',
                  borderLeft: isActive(item.path) ? '3px solid var(--gold)' : '3px solid transparent',
                  fontSize: 13.5,
                  fontWeight: isActive(item.path) ? 600 : 400,
                  marginBottom: 2,
                  transition: 'all 0.2s'
                }}
              >
                <span>{item.icon}</span>
                <span style={{ flex: 1 }}>{item.label}</span>
                {item.badge !== undefined && (
                  <span className="badge badge-info" style={{ fontSize: 10 }}>
                    {item.badge || 0}
                  </span>
                )}
              </Link>
            ))}
          </div>
        ))}
      </nav>

      <div style={{
        padding: 16,
        borderTop: '1px solid var(--navy-light)',
        display: 'flex',
        alignItems: 'center',
        gap: 12
      }}>
        <div style={{
          width: 36,
          height: 36,
          borderRadius: '50%',
          background: 'var(--gold)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--navy)',
          fontWeight: 600,
          fontSize: 14
        }}>
          {tokenManager.getEmail()?.[0]?.toUpperCase() || 'U'}
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ color: 'white', fontSize: 13, fontWeight: 500 }}>
            {tokenManager.getEmail() || 'User'}
          </div>
          <div style={{ color: 'var(--text-muted)', fontSize: 11 }}>Administrator</div>
        </div>
        <button
          onClick={handleLogout}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            fontSize: 18
          }}
          title="Logout"
        >
          🚪
        </button>
      </div>
    </div>
  );
};
