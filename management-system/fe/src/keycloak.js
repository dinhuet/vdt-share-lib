import Keycloak from 'keycloak-js';

const appConfig = window.__APP_CONFIG__ || {};

const keycloak = new Keycloak({
  url: appConfig.KEYCLOAK_URL || import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
  realm: appConfig.KEYCLOAK_REALM || import.meta.env.VITE_KEYCLOAK_REALM || 'vdt-shared-lib',
  clientId: appConfig.KEYCLOAK_CLIENT_ID || import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'fe-app',
});

let initialized = false;
let initPromise = null;

export async function initKeycloak() {
  if (initialized) {
    return { keycloak, authenticated: Boolean(keycloak.authenticated) };
  }

  if (initPromise) {
    return initPromise;
  }

  initPromise = keycloak.init({
      onLoad: 'check-sso',
      checkLoginIframe: false,
    })
    .then((authenticated) => {
      initialized = true;
      return { keycloak, authenticated };
    })
    .catch((err) => {
      initialized = false;
      initPromise = null;
      throw err;
    });

  try {
    return await initPromise;
  } catch (err) {
    initialized = false;
    initPromise = null;
    throw err;
  }
}

export default keycloak;
