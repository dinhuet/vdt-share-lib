import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'vdt-shared-lib',
  clientId: 'fe-app',
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
