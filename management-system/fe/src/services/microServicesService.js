import { apiRequest } from './apiClient';

const BASE_PATH = '/api/admin/micro-services';

export function getMicroServices() {
  return apiRequest(BASE_PATH);
}

export function getMicroService(id) {
  return apiRequest(`${BASE_PATH}/${id}`);
}
