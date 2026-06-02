import keycloak, { initKeycloak } from '../keycloak';

export async function initializeAuth() {
  const { authenticated } = await initKeycloak();
  return {
    authenticated,
    user: authenticated ? getCurrentUser() : null,
  };
}

export function getCurrentUser() {
  return {
    username: keycloak.tokenParsed?.preferred_username || keycloak.tokenParsed?.sub || 'admin',
    email: keycloak.tokenParsed?.email || '',
    name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username || 'Admin User',
  };
}

export function login() {
  return keycloak.login();
}

export function logout() {
  return keycloak.logout({ redirectUri: window.location.origin });
}

export async function refreshToken() {
  await keycloak.updateToken(30);
  return keycloak.token;
}
