import { apiRequest } from './apiClient';

const basePath = '/api/admin/anomaly-rules';

export function getAnomalyRules() {
  return apiRequest(basePath);
}

export function getAnomalyRule(id) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}`);
}

export function createAnomalyRule(payload) {
  return apiRequest(basePath, { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAnomalyRule(id, payload) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function updateAnomalyRuleEnabled(id, enabled) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/enabled`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled }),
  });
}
