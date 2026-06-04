import { apiRequest } from './apiClient';

function basePath(exposedApiId) {
  return `/api/admin/exposed-apis/${exposedApiId}/access-policies`;
}

export function getAccessPolicies(exposedApiId) {
  return apiRequest(basePath(exposedApiId));
}

export function createAccessPolicy(exposedApiId, payload) {
  return apiRequest(basePath(exposedApiId), {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateAccessPolicy(exposedApiId, policyId, payload) {
  return apiRequest(`${basePath(exposedApiId)}/${policyId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteAccessPolicy(exposedApiId, policyId) {
  return apiRequest(`${basePath(exposedApiId)}/${policyId}`, { method: 'DELETE' });
}
