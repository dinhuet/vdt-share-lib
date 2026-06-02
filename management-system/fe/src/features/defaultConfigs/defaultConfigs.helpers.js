import { DEFAULT_LIMIT_FORM } from '../../utils/constants';
import { toFormValue, toNumberOrNull } from '../../utils/format';

export const configFields = [
  ['maxRequests', 'Max Requests'],
  ['throttleWindowSec', 'Window Sec'],
  ['maxRequestKb', 'Max Request KB'],
  ['maxResponseKb', 'Max Response KB'],
  ['latencyThresholdMs', 'Latency MS'],
  ['timeoutMs', 'Timeout MS'],
  ['logRetentionDays', 'Retention Days'],
];

export function configToForm(config, mode) {
  return {
    ...DEFAULT_LIMIT_FORM,
    scope: mode === 'create-service' ? 'SERVICE' : config?.scope || 'GLOBAL',
    microServiceId: config?.microServiceId || '',
    microServiceName: config?.microServiceName || '',
    enabled: config?.enabled ?? true,
    applyMode: 'NEW_ONLY',
    ...configFields.reduce((form, [key]) => ({ ...form, [key]: toFormValue(config?.[key]) }), {}),
  };
}

export function configFormToPayload(form) {
  return {
    scope: form.scope,
    microServiceId: form.scope === 'SERVICE' ? form.microServiceId || null : null,
    enabled: Boolean(form.enabled),
    applyMode: form.applyMode || 'NEW_ONLY',
    ...configFields.reduce((payload, [key]) => ({ ...payload, [key]: toNumberOrNull(form[key]) }), {}),
  };
}

export function getServiceOptions(configs) {
  const map = new Map();
  configs.forEach((config) => {
    if (config.microServiceId && config.microServiceName) {
      map.set(config.microServiceId, config.microServiceName);
    }
  });
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
}
