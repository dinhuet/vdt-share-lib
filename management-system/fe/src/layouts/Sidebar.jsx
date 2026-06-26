import { ROUTES } from '../utils/constants';

const navItems = [
  ['Dashboard', '▦', ROUTES.DASHBOARD],
  ['Clients', '♣', ROUTES.CLIENTS],
  ['Shared APIs', '✥', ROUTES.SHARED_APIS],
  ['Client APIs', '▱', ROUTES.CLIENT_APIS],
  ['Default Configs', '⚙', ROUTES.DEFAULT_CONFIGS],
  ['Security Policies', '⬟', ROUTES.ACCESS_POLICIES],
  ['Security Alerts', '⚠', ROUTES.SECURITY_ALERTS],
];

function getDisplayName(user) {
  return user?.name || user?.username || 'Admin User';
}

function getInitials(user) {
  return getDisplayName(user)
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function Sidebar({ activeRoute, onRouteChange, user }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <strong>MicroLib Admin</strong>
        <span>Platform Engineering</span>
      </div>
      <nav className="side-nav">
        {navItems.map(([label, icon, route]) => (
          <button
            key={label}
            className={`nav-item ${activeRoute === route ? 'active' : ''}`}
            type="button"
            onClick={() => onRouteChange(route)}
          >
            <span>{icon}</span>
            {label}
          </button>
        ))}
      </nav>
      <div className="sidebar-user">
        <div>{getInitials(user)}</div>
        <span>{getDisplayName(user)}</span>
      </div>
    </aside>
  );
}
