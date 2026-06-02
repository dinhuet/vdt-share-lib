import { ROUTES } from '../utils/constants';

export default function Topbar({ activeRoute, onLogout }) {
  const breadcrumb = activeRoute === ROUTES.DEFAULT_CONFIGS
    ? 'Settings / Default Configs'
    : 'Shared APIs / Exposed APIs';

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
