import { useState, useEffect, useCallback, useRef } from 'react';
import keycloak, { initKeycloak } from './keycloak';

export default function App() {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [apiResult, setApiResult] = useState(null);
  const [error, setError] = useState(null);
  const initRef = useRef(false);

  useEffect(() => {
    if (initRef.current) return;
    initRef.current = true;

    initKeycloak()
      .then(({ authenticated: auth }) => {
        setAuthenticated(auth);
        setReady(true);
        if (auth) {
          setUser({
            username: keycloak.tokenParsed?.preferred_username,
            email: keycloak.tokenParsed?.email,
            name: keycloak.tokenParsed?.name,
          });
        }
      })
      .catch((err) => {
        setError('Failed to initialize Keycloak: ' + (err.message || err));
        setReady(true);
      });
  }, []);

  const handleLogin = () => keycloak.login();

  const handleLogout = () => keycloak.logout({ redirectUri: window.location.origin });

  const refreshToken = useCallback(async () => {
    try {
      await keycloak.updateToken(30);
      return true;
    } catch {
      setAuthenticated(false);
      return false;
    }
  }, []);

  const callApi = useCallback(async (endpoint) => {
    const ok = await refreshToken();
    if (!ok) {
      setApiResult({ error: 'Token refresh failed, please login again' });
      return;
    }
    try {
      const res = await fetch(endpoint, {
        headers: { Authorization: `Bearer ${keycloak.token}` },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setApiResult(data);
    } catch (err) {
      setApiResult({ error: err.message });
    }
  }, [refreshToken]);

  if (!ready) return <div className="container"><p>Loading...</p></div>;
  if (error) return <div className="container"><p className="error">{error}</p></div>;

  return (
    <div className="container">
      <h1>Keycloak Demo</h1>

      {!authenticated ? (
        <div className="card">
          <p>You are not logged in.</p>
          <button onClick={handleLogin}>Login with Keycloak</button>
        </div>
      ) : (
        <>
          <div className="card">
            <h2>User Info</h2>
            <p><strong>Username:</strong> {user?.username}</p>
            <p><strong>Email:</strong> {user?.email}</p>
            <p><strong>Name:</strong> {user?.name}</p>
            <button onClick={handleLogout}>Logout</button>
          </div>

          <div className="card">
            <h2>API Demo</h2>
            <div className="api-buttons">
              <button onClick={() => callApi('/api/demo/public')}>Call Public API</button>
              <button onClick={() => callApi('/api/demo/secure')}>Call Secure API</button>
              <button onClick={() => callApi('/api/demo/user')}>Call User API</button>
            </div>
            {apiResult && (
              <pre className="result">{JSON.stringify(apiResult, null, 2)}</pre>
            )}
          </div>
        </>
      )}
    </div>
  );
}
