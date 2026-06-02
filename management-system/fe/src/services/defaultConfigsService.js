import { apiRequest } from './apiClient';

const BASE_PATH = '/api/admin/api-default-configs';

export function getDefaultConfigs() {
  return apiRequest(BASE_PATH);
}

export function getDefaultConfig(id) {
  return apiRequest(`${BASE_PATH}/${id}`);
}

export function upsertDefaultConfig(payload) {
  return apiRequest(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function deleteDefaultConfig(id) {
  return apiRequest(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}
