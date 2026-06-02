export function formatSize(kb) {
  if (kb === null || kb === undefined || kb === '') return '-';
  const numeric = Number(kb);
  if (Number.isNaN(numeric)) return '-';
  if (numeric >= 1024) return `${Number((numeric / 1024).toFixed(1))}MB`;
  return `${numeric}KB`;
}

export function formatRequests(value, windowSec) {
  if (value === null || value === undefined || value === '') return '-';
  if (!windowSec || windowSec === 1) return `${value}/s`;
  return `${value}/${windowSec}s`;
}

export function toNumberOrNull(value) {
  if (value === '' || value === null || value === undefined) return null;
  return Number(value);
}

export function toFormValue(value) {
  return value === null || value === undefined ? '' : String(value);
}

export function validatePositiveFields(payload, fields) {
  for (const field of fields) {
    const value = payload[field];
    if (value !== null && value !== undefined && value !== '' && Number(value) <= 0) {
      return 'All numeric config values must be greater than 0.';
    }
  }
  return null;
}
