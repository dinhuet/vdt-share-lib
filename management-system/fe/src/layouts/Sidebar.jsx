import { ROUTES } from '../utils/constants';

const navItems = [
  ['Dashboard', '▦', 'disabled-dashboard'],
  ['Clients', '♣', 'disabled-clients'],
  ['Shared APIs', '✥', ROUTES.SHARED_APIS],
  ['Client APIs', '▱', ROUTES.CLIENT_APIS],
  ['Default Configs', '⚙', ROUTES.DEFAULT_CONFIGS],
  ['Security Policies', '⬟', 'disabled-security'],
  ['Rate Limit', '◴', 'disabled-rate'],
  ['Access Control', '⬡', 'disabled-access'],
  ['Logs', '▤', 'disabled-logs'],
  ['Anomaly Detection', '⌁', 'disabled-anomaly'],
  ['Alerts', '♢', 'disabled-alerts'],
  ['Retry & Rollback', '↻', 'disabled-retry'],
  ['Kibana Reports', '▥', 'disabled-kibana'],
  ['Settings', '⚙', 'disabled-settings'],
];

export default function Sidebar({ activeRoute, onRouteChange, user }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <strong>MicroLib Admin</strong>
        <span>Platform Engineering</span>
      </div>
      <nav className="side-nav">
        {navItems.map(([label, icon, route]) => {
          const disabled = route.startsWith('disabled');
          return (
            <button
              key={label}
              className={`nav-item ${activeRoute === route ? 'active' : ''}`}
              disabled={disabled}
              type="button"
              onClick={() => onRouteChange(route)}
            >
              <span>{icon}</span>
              {label}
            </button>
          );
        })}
      </nav>
      <div className="sidebar-user">
        <div>{(user?.name || user?.username || 'JD').slice(0, 2).toUpperCase()}</div>
        <span>{user?.name || user?.username || 'John Doe'}</span>
      </div>
    </aside>
  );
}
