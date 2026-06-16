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

export default function Topbar({ activeRoute, onLogout, user }) {
  const breadcrumbs = {
    [ROUTES.DASHBOARD]: 'Dashboard / Kibana',
    [ROUTES.CLIENTS]: 'Clients / Registry',
    [ROUTES.SHARED_APIS]: 'Shared APIs / Exposed APIs',
    [ROUTES.CLIENT_APIS]: 'Client APIs / Outbound APIs',
    [ROUTES.DEFAULT_CONFIGS]: 'Settings / Default Configs',
    [ROUTES.ACCESS_POLICIES]: 'Security / Policies',
  };
  const breadcrumb = breadcrumbs[activeRoute] || breadcrumbs[ROUTES.SHARED_APIS];

  return (
    <header className="topbar">
      <div>
        <strong>MicroLib Manager</strong>
        <span>{breadcrumb}</span>
      </div>
      <div className="topbar-actions">
        <span className="environment-pill">Environment: <strong>Production</strong></span>
        <button type="button" className="icon-button">♧</button>
        <button type="button" className="icon-button">⚙</button>
        <button type="button" className="avatar-button" onClick={onLogout} title="Logout">{getInitials(user)}</button>
      </div>
    </header>
  );
}
