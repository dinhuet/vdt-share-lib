import { apiRequest, buildQuery } from './apiClient';

const BASE_PATH = '/api/admin/exposed-apis';

export function getExposedApis(filters = {}) {
  return apiRequest(`${BASE_PATH}${buildQuery(filters)}`);
}

export function getExposedApi(id) {
  return apiRequest(`${BASE_PATH}/${id}`);
}

export function updateExposedApiLimits(id, payload) {
  return apiRequest(`${BASE_PATH}/${id}/limits`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function useDefaultConfig(id) {
  return apiRequest(`${BASE_PATH}/${id}/use-default-config`, { method: 'PATCH' });
}

export function enableApi(id) {
  return apiRequest(`${BASE_PATH}/${id}/enable`, { method: 'PATCH' });
}

export function disableApi(id) {
  return apiRequest(`${BASE_PATH}/${id}/disable`, { method: 'PATCH' });
}

export function deleteExposedApi(id) {
  return apiRequest(`${BASE_PATH}/${id}`, { method: 'DELETE' });
}

export function updateNotificationRule(id, notificationRuleId) {
  return apiRequest(`${BASE_PATH}/${id}/notification-rule`, {
    method: 'PATCH',
    body: JSON.stringify({ notificationRuleId }),
  });
}
