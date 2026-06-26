import { useEffect, useMemo, useState } from 'react';
import { getAlertSummary } from '../services/securityAlertsService';
import { ROUTES } from '../utils/constants';

function getInitials(user) {
  const displayName = user?.name || user?.username || 'Admin User';
  return displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function Topbar({ activeRoute, onRouteChange, onLogout, user }) {
  const [summary, setSummary] = useState(null);

  const breadcrumbs = {
    [ROUTES.DASHBOARD]: 'Dashboard / Kibana',
    [ROUTES.CLIENTS]: 'Clients / Registry',
    [ROUTES.SHARED_APIS]: 'Shared APIs / Exposed APIs',
    [ROUTES.CLIENT_APIS]: 'Client APIs / Outbound APIs',
    [ROUTES.DEFAULT_CONFIGS]: 'Settings / Default Configs',
    [ROUTES.ACCESS_POLICIES]: 'Security / Policies',
    [ROUTES.SECURITY_ALERTS]: 'Security / Alerts',
  };
  const breadcrumb = breadcrumbs[activeRoute] || breadcrumbs[ROUTES.SHARED_APIS];
  const alertCount = useMemo(
    () => Number(summary?.highOpenCount || 0) + Number(summary?.criticalOpenCount || 0),
    [summary],
  );

  useEffect(() => {
    let mounted = true;

    async function loadSummary() {
      try {
        const data = await getAlertSummary();
        if (mounted) setSummary(data || null);
      } catch (err) {
        console.error('Failed to load security alert summary', err);
        if (mounted) setSummary(null);
      }
    }

    loadSummary();
    const timerId = window.setInterval(loadSummary, 30000);

    return () => {
      mounted = false;
      window.clearInterval(timerId);
    };
  }, []);

  return (
    <header className="topbar">
      <div>
        <strong>MicroLib Manager</strong>
        <span>{breadcrumb}</span>
      </div>
      <div className="topbar-actions">
        <span className="environment-pill">Environment: <strong>Production</strong></span>
        <button type="button" className="icon-button alert-icon-button" onClick={() => onRouteChange(ROUTES.SECURITY_ALERTS)} title="Security alerts">
          ♧
          {alertCount > 0 ? <span className="topbar-alert-badge">{alertCount > 99 ? '99+' : alertCount}</span> : null}
        </button>
        <button type="button" className="icon-button">⚙</button>
        <button type="button" className="avatar-button" onClick={onLogout} title="Logout">{getInitials(user)}</button>
      </div>
    </header>
  );
}
