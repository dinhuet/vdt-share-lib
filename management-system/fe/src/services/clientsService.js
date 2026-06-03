import { apiRequest, buildQuery } from './apiClient';

const BASE_PATH = '/api/admin/clients';

export function getClients(filters = {}) {
  return apiRequest(`${BASE_PATH}${buildQuery(filters)}`);
}

export function getClient(id) {
  return apiRequest(`${BASE_PATH}/${id}`);
}

export function createClient(payload) {
  return apiRequest(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateClient(id, payload) {
  return apiRequest(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function activateClient(id) {
  return apiRequest(`${BASE_PATH}/${id}/activate`, { method: 'PATCH' });
}

export function deactivateClient(id) {
  return apiRequest(`${BASE_PATH}/${id}/deactivate`, { method: 'PATCH' });
}

export function revokeClient(id, reason) {
  return apiRequest(`${BASE_PATH}/${id}/revoke`, {
    method: 'PATCH',
    body: JSON.stringify({ reason }),
  });
}

export function deleteClient(id) {
  return apiRequest(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}
