import { ROUTES } from '../utils/constants';

export default function Topbar({ activeRoute, onLogout }) {
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
        <button type="button" className="avatar-button" onClick={onLogout}>JD</button>
      </div>
    </header>
  );
}
