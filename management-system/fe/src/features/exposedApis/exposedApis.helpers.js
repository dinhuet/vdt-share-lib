import { DEFAULT_LIMIT_FORM } from '../../utils/constants';
import { toFormValue, toNumberOrNull } from '../../utils/format';

export const limitFields = [
  ['maxRequests', 'Max Requests'],
  ['throttleWindowSec', 'Throttle Window Sec'],
  ['maxRequestKb', 'Max Request KB'],
  ['maxResponseKb', 'Max Response KB'],
  ['latencyThresholdMs', 'Latency Threshold MS'],
  ['timeoutMs', 'Timeout MS'],
  ['logRetentionDays', 'Log Retention Days'],
];

export function apiToLimitForm(api) {
  return limitFields.reduce((form, [key]) => ({
    ...form,
    [key]: toFormValue(api?.[key]),
  }), { ...DEFAULT_LIMIT_FORM });
}

export function limitFormToPayload(form) {
  return limitFields.reduce((payload, [key]) => ({
    ...payload,
    [key]: toNumberOrNull(form[key]),
  }), {});
}

export function getMicroservices(apis) {
  const map = new Map();
  apis.forEach((api) => {
    if (api.microServiceId && api.microServiceName) {
      map.set(api.microServiceId, api.microServiceName);
    }
  });
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
}

export function filterApis(apis, { microServiceId, syncStatus, search }) {
  const keyword = search.trim().toLowerCase();
  return apis.filter((api) => {
    const matchesService = !microServiceId || api.microServiceId === microServiceId;
    const matchesStatus = !syncStatus || api.syncStatus === syncStatus;
    const matchesSearch = !keyword
      || api.name?.toLowerCase().includes(keyword)
      || api.path?.toLowerCase().includes(keyword)
      || api.microServiceName?.toLowerCase().includes(keyword);
    return matchesService && matchesStatus && matchesSearch;
  });
}
