import { apiRequest, buildQuery } from './apiClient';

function basePath(clientId) {
  return `/api/admin/clients/${clientId}/credentials`;
}

export function getClientCredentials(clientId, filters = {}) {
  return apiRequest(`${basePath(clientId)}${buildQuery(filters)}`);
}

export function createClientCredential(clientId, payload) {
  return apiRequest(basePath(clientId), {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function revokeClientCredential(clientId, credentialId, reason) {
  return apiRequest(`${basePath(clientId)}/${credentialId}/revoke`, {
    method: 'PATCH',
    body: JSON.stringify({ reason }),
  });
}
