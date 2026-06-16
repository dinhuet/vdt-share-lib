import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function AdminLayout({ activeRoute, onRouteChange, onLogout, user, children }) {
  return (
    <div className="admin-shell">
      <Sidebar activeRoute={activeRoute} onRouteChange={onRouteChange} user={user} />
      <div className="admin-main">
        <Topbar activeRoute={activeRoute} onLogout={onLogout} user={user} />
        <main className="page-shell">{children}</main>
      </div>
    </div>
  );
}
