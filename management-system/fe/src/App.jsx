import { useEffect, useState } from 'react';
import AdminLayout from './layouts/AdminLayout';
import AppRoutes from './routes/AppRoutes';
import { initializeAuth, login, logout } from './services/authService';
import { useAppStore } from './store/appStore';

export default function App() {
  const { activeRoute, setActiveRoute } = useAppStore();
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    initializeAuth()
      .then((result) => {
        if (!mounted) return;
        setAuthenticated(result.authenticated);
        setUser(result.user);
      })
      .catch((err) => setError(`Failed to initialize Keycloak: ${err.message || err}`))
      .finally(() => mounted && setReady(true));

    return () => {
      mounted = false;
    };
  }, []);

  if (!ready) {
    return <div className="auth-screen"><div className="auth-card"><span className="spinner" />Checking session...</div></div>;
  }

  if (error) {
    return <div className="auth-screen"><div className="auth-card error-card">{error}</div></div>;
  }

  if (!authenticated) {
    return (
      <div className="auth-screen">
        <section className="auth-card">
          <div className="auth-mark">ML</div>
          <h1>MicroLib Manager</h1>
          <p>Sign in with Keycloak to manage exposed APIs and default config policies.</p>
          <button className="btn btn-primary" type="button" onClick={login}>Login with Keycloak</button>
        </section>
      </div>
    );
  }

  return (
    <AdminLayout activeRoute={activeRoute} onRouteChange={setActiveRoute} onLogout={logout} user={user}>
      <AppRoutes activeRoute={activeRoute} />
    </AdminLayout>
  );
}
