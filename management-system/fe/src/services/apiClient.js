import { refreshToken } from './authService';

export async function apiRequest(path, options = {}) {
  const token = await refreshToken();
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
    ...options.headers,
  };

  const response = await fetch(path, {
    ...options,
    headers,
  });

  const text = await response.text();
  const body = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(body?.message || `HTTP ${response.status}`);
  }

  if (body && body.code !== undefined && body.code !== 1000) {
    throw new Error(body.message || `API error ${body.code}`);
  }

  return body?.result ?? null;
}

export function buildQuery(params) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value);
    }
  });
  const queryString = query.toString();
  return queryString ? `?${queryString}` : '';
}
