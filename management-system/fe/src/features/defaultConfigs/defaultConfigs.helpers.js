import { DEFAULT_LIMIT_FORM } from '../../utils/constants';
import { toFormValue, toNumberOrNull } from '../../utils/format';

export const configFields = [
  ['latencyThresholdMs', 'Latency MS'],
  ['timeoutMs', 'Timeout MS'],
  ['logRetentionDays', 'Retention Days'],
  ['notificationRuleId', 'Notification Rule ID'],
];

export const exposedConfigFields = [
  ['maxRequests', 'Max Requests'],
  ['throttleWindowSec', 'Window Sec'],
  ['maxRequestKb', 'Max Request KB'],
  ['maxResponseKb', 'Max Response KB'],
  ...configFields,
];

export const clientConfigFields = [
  ['latencyThresholdMs', 'Latency MS'],
  ['timeoutMs', 'Timeout MS'],
  ['maxRetries', 'Max Retries'],
  ['retryDelayMs', 'Retry Delay MS'],
  ['failureAction', 'Failure Action'],
  ['logRetentionDays', 'Retention Days'],
  ['notificationRuleId', 'Notification Rule ID'],
];

export function getConfigFields(apiType) {
  return apiType === 'CLIENT' ? clientConfigFields : exposedConfigFields;
}

export function getNumericConfigFields(apiType) {
  return getConfigFields(apiType)
    .map(([key]) => key)
    .filter((key) => key !== 'failureAction' && key !== 'notificationRuleId');
}

export function configToForm(config, mode) {
  return {
    ...DEFAULT_LIMIT_FORM,
    apiType: config?.apiType || 'EXPOSED',
    scope: mode === 'create-service' ? 'SERVICE' : config?.scope || 'GLOBAL',
    microServiceId: config?.microServiceId || '',
    microServiceName: config?.microServiceName || '',
    enabled: config?.enabled ?? true,
    applyMode: 'NEW_ONLY',
    ...[...exposedConfigFields, ...clientConfigFields].reduce((form, [key]) => ({ ...form, [key]: toFormValue(config?.[key]) }), {}),
  };
}

export function configFormToPayload(form) {
  const fields = getConfigFields(form.apiType);
  return {
    apiType: form.apiType || 'EXPOSED',
    scope: form.scope,
    microServiceId: form.scope === 'SERVICE' ? form.microServiceId || null : null,
    enabled: Boolean(form.enabled),
    applyMode: form.applyMode || 'NEW_ONLY',
    ...fields.reduce((payload, [key]) => ({
      ...payload,
      [key]: key === 'failureAction' || key === 'notificationRuleId'
        ? form[key].trim() || null
        : toNumberOrNull(form[key]),
    }), {}),
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
