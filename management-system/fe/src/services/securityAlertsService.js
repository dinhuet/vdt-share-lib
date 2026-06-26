import { apiRequest, buildQuery } from './apiClient';

const basePath = '/api/security-alerts';

export function getSecurityAlerts(filters = {}) {
  return apiRequest(`${basePath}${buildQuery(filters)}`);
}

export function getSecurityAlert(id) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}`);
}

export function getAlertOccurrences(id) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/occurrences`);
}

export function getAlertNotifications(id) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/notifications`);
}

export function getAlertSummary() {
  return apiRequest(`${basePath}/summary`);
}

export function getRecentAlerts(limit) {
  return apiRequest(`${basePath}/recent${buildQuery({ limit })}`);
}

export function ackAlert(id, payload = {}) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/ack`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function ignoreAlert(id, payload = {}) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/ignore`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function resolveAlert(id, payload = {}) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/resolve`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function temporaryBlacklistAlert(id, payload) {
  return apiRequest(`${basePath}/${encodeURIComponent(id)}/blacklist-temporary`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
