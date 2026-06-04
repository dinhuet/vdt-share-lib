import { apiRequest } from './apiClient';

function basePath(clientId) {
  return `/api/admin/clients/${clientId}/permissions`;
}

export function getClientPermissions(clientId) {
  return apiRequest(basePath(clientId));
}

export function grantClientPermission(clientId, payload) {
  return apiRequest(basePath(clientId), {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function enableClientPermission(clientId, permissionId) {
  return apiRequest(`${basePath(clientId)}/${permissionId}/enable`, { method: 'PATCH' });
}

export function disableClientPermission(clientId, permissionId) {
  return apiRequest(`${basePath(clientId)}/${permissionId}/disable`, { method: 'PATCH' });
}

export function deleteClientPermission(clientId, permissionId) {
  return apiRequest(`${basePath(clientId)}/${permissionId}`, { method: 'DELETE' });
}
