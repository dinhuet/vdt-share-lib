import { apiRequest, buildQuery } from './apiClient';

const BASE_PATH = '/api/admin/client-apis';

export function getClientApis(filters = {}) {
  return apiRequest(`${BASE_PATH}${buildQuery(filters)}`);
}

export function getClientApi(id) {
  return apiRequest(`${BASE_PATH}/${id}`);
}

export function updateClientApi(id, payload) {
  return apiRequest(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function enableClientApi(id) {
  return apiRequest(`${BASE_PATH}/${id}/enable`, { method: 'PATCH' });
}

export function disableClientApi(id) {
  return apiRequest(`${BASE_PATH}/${id}/disable`, { method: 'PATCH' });
}

export function useDefaultClientApiConfig(id) {
  return apiRequest(`${BASE_PATH}/${id}/use-default-config`, { method: 'PATCH' });
}

export function deleteClientApi(id) {
  return apiRequest(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}
