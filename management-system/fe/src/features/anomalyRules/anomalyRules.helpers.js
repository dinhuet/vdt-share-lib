export const RULE_TYPES = ['STATIC', 'BASELINE', 'HYBRID'];
export const RULE_SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const SCOPE_TYPES = ['GLOBAL', 'SERVICE', 'ENDPOINT', 'ENDPOINT_CLIENT', 'ENDPOINT_IP'];
export const OPERATORS = ['GT', 'GTE', 'LT', 'LTE'];
export const TIME_BUCKET_TYPES = ['GLOBAL', 'SAME_HOUR'];

export function filterRules(rules, filters) {
  const search = (filters.search || '').trim().toLowerCase();
  return (rules || []).filter((rule) => {
    const searchable = [rule.ruleCode, rule.name, rule.metric].filter(Boolean).join(' ').toLowerCase();
    return (!search || searchable.includes(search))
      && (!filters.ruleType || rule.ruleType === filters.ruleType)
      && (!filters.severity || rule.severity === filters.severity)
      && (filters.enabled === '' || String(Boolean(rule.enabled)) === filters.enabled)
      && (!filters.scopeType || rule.scopeType === filters.scopeType);
  });
}

export function ruleStats(rules) {
  const allRules = rules || [];
  return {
    total: allRules.length,
    enabled: allRules.filter((rule) => rule.enabled).length,
    staticCount: allRules.filter((rule) => rule.ruleType === 'STATIC').length,
    baselineHybrid: allRules.filter((rule) => rule.ruleType === 'BASELINE' || rule.ruleType === 'HYBRID').length,
  };
}

export function ruleToForm(rule) {
  return {
    ruleCode: rule?.ruleCode || '',
    name: rule?.name || '',
    description: rule?.description || '',
    ruleType: rule?.ruleType || 'STATIC',
    metric: rule?.metric || '',
    severity: rule?.severity || 'MEDIUM',
    scopeType: rule?.scopeType || 'GLOBAL',
    scopeId: rule?.scopeId || '',
    enabled: rule?.enabled ?? true,
    cooldownMinutes: rule?.cooldownMinutes ?? 15,
    operator: rule?.staticConfig?.operator || 'GTE',
    thresholdValue: rule?.staticConfig?.thresholdValue ?? '',
    staticWindowSeconds: rule?.staticConfig?.windowSeconds ?? 60,
    minCount: rule?.staticConfig?.minSampleCount ?? 1,
    historyDays: rule?.baselineConfig?.historyDays ?? 7,
    timeBucketType: rule?.baselineConfig?.timeBucketType || 'SAME_HOUR',
    percentile: rule?.baselineConfig?.percentile ?? 95,
    multiplier: rule?.baselineConfig?.multiplier ?? 2,
    minAbsoluteThreshold: rule?.baselineConfig?.minAbsoluteThreshold ?? 1,
    baselineWindowSeconds: rule?.baselineConfig?.windowSeconds ?? 300,
    baselineMinSampleCount: rule?.baselineConfig?.minSampleCount ?? 1,
    baselineConsecutiveWindows: rule?.baselineConfig?.consecutiveWindows ?? 1,
  };
}

export function formToPayload(form) {
  const payload = {
    ruleCode: form.ruleCode.trim(),
    name: form.name.trim(),
    description: form.description.trim() || null,
    ruleType: form.ruleType,
    metric: form.metric.trim(),
    severity: form.severity,
    scopeType: form.scopeType,
    scopeId: form.scopeType === 'GLOBAL' ? null : form.scopeId.trim(),
    enabled: Boolean(form.enabled),
    cooldownMinutes: toInteger(form.cooldownMinutes),
  };
  if (form.ruleType === 'STATIC' || form.ruleType === 'HYBRID') {
    payload.staticConfig = {
      operator: form.operator,
      thresholdValue: toNumber(form.thresholdValue),
      windowSeconds: toInteger(form.staticWindowSeconds),
      minSampleCount: toInteger(form.minCount),
      consecutiveWindows: 1,
    };
  }
  if (form.ruleType === 'BASELINE' || form.ruleType === 'HYBRID') {
    payload.baselineConfig = {
      historyDays: toInteger(form.historyDays),
      timeBucketType: form.timeBucketType,
      percentile: toNumber(form.percentile),
      multiplier: toNumber(form.multiplier),
      minAbsoluteThreshold: toNumber(form.minAbsoluteThreshold),
      maxAbsoluteThreshold: null,
      minSampleCount: toInteger(form.baselineMinSampleCount),
      consecutiveWindows: toInteger(form.baselineConsecutiveWindows),
      windowSeconds: toInteger(form.baselineWindowSeconds),
    };
  }
  return payload;
}

function toInteger(value) {
  return value === '' || value === null || value === undefined ? null : Number.parseInt(value, 10);
}

function toNumber(value) {
  return value === '' || value === null || value === undefined ? null : Number(value);
}
