import { toFormValue, toNumberOrNull } from '../../utils/format';

export const clientApiNumericFields = [
  ['latencyThresholdMs', 'Latency Threshold MS'],
  ['timeoutMs', 'Timeout MS'],
  ['maxRetries', 'Max Retries'],
  ['retryDelayMs', 'Retry Delay MS'],
  ['logRetentionDays', 'Log Retention Days'],
];

const defaultForm = {
  microServiceId: '',
  clientId: '',
  name: '',
  destinationUrl: '',
  method: 'GET',
  protocol: 'HTTP/1.1',
  latencyThresholdMs: '',
  timeoutMs: '30000',
  maxRetries: '3',
  retryDelayMs: '1000',
  failureAction: '',
  logRetentionDays: '30',
  notificationRuleId: '',
  enabled: true,
};

export function clientApiToForm(api) {
  if (!api) return defaultForm;
  return {
    microServiceId: api.microServiceId || '',
    clientId: api.clientId || '',
    name: api.name || '',
    destinationUrl: api.destinationUrl || '',
    method: api.method || 'GET',
    protocol: api.protocol || 'HTTP/1.1',
    latencyThresholdMs: toFormValue(api.latencyThresholdMs),
    timeoutMs: toFormValue(api.timeoutMs),
    maxRetries: toFormValue(api.maxRetries),
    retryDelayMs: toFormValue(api.retryDelayMs),
    failureAction: api.failureAction || '',
    logRetentionDays: toFormValue(api.logRetentionDays),
    notificationRuleId: api.notificationRuleId || '',
    enabled: Boolean(api.enabled),
  };
}

export function clientApiFormToPayload(form) {
  return {
    microServiceId: form.microServiceId || null,
    clientId: form.clientId || null,
    name: form.name.trim(),
    destinationUrl: form.destinationUrl.trim() || null,
    method: form.method || null,
    protocol: form.protocol.trim(),
    latencyThresholdMs: toNumberOrNull(form.latencyThresholdMs),
    timeoutMs: toNumberOrNull(form.timeoutMs),
    maxRetries: toNumberOrNull(form.maxRetries),
    retryDelayMs: toNumberOrNull(form.retryDelayMs),
    failureAction: form.failureAction.trim() || null,
    logRetentionDays: toNumberOrNull(form.logRetentionDays),
    notificationRuleId: form.notificationRuleId || null,
    enabled: Boolean(form.enabled),
  };
}

export function filterClientApis(clientApis, { microServiceId, enabled, deleted, search }) {
  const keyword = search.trim().toLowerCase();
  return clientApis.filter((api) => {
    const isDeleted = Boolean(api.deleted);
    const matchesService = !microServiceId || api.microServiceId === microServiceId;
    const matchesEnabled = enabled === '' || String(Boolean(api.enabled)) === enabled;
    const matchesDeleted = deleted === 'include' || (deleted === 'deleted' ? isDeleted : !isDeleted);
    const matchesSearch = !keyword
      || api.name?.toLowerCase().includes(keyword)
      || api.destinationUrl?.toLowerCase().includes(keyword)
      || api.microServiceName?.toLowerCase().includes(keyword)
      || api.protocol?.toLowerCase().includes(keyword);
    return matchesService && matchesEnabled && matchesDeleted && matchesSearch;
  });
}

export function getMicroservicesFromClientApis(clientApis) {
  const map = new Map();
  clientApis.forEach((api) => {
    if (api.microServiceId) {
      map.set(api.microServiceId, api.microServiceName || api.microServiceId);
    }
  });
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
}

export function mergeServiceOptions(...groups) {
  const map = new Map();
  groups.flat().forEach((service) => {
    if (service.id) {
      map.set(service.id, service.name || service.id);
    }
  });
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
}
