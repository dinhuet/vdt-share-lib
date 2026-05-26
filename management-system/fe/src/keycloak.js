import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'vdt-shared-lib',
  clientId: 'fe-app',
});

let initialized = false;

export async function initKeycloak() {
  if (initialized) return keycloak;
  initialized = true;
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      checkLoginIframe: false,
    });
    return { keycloak, authenticated };
  } catch (err) {
    initialized = false;
    throw err;
  }
}

export default keycloak;
