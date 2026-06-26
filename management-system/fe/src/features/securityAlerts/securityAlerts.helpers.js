export const ALERT_STATUSES = ['OPEN', 'ACKNOWLEDGED', 'IGNORED', 'RESOLVED'];
export const ALERT_SEVERITIES = ['MEDIUM', 'HIGH', 'CRITICAL'];

export function severityTone(severity) {
  if (severity === 'CRITICAL') return 'danger';
  if (severity === 'HIGH') return 'warning';
  if (severity === 'MEDIUM') return 'info';
  return 'neutral';
}

export function statusTone(status) {
  if (status === 'OPEN') return 'danger';
  if (status === 'ACKNOWLEDGED') return 'warning';
  if (status === 'RESOLVED') return 'success';
  if (status === 'IGNORED') return 'neutral';
  return 'neutral';
}

export function buildActionPayload(reason) {
  return { reason: reason || 'Updated from Security Alerts UI' };
}

export function defaultBlacklistTarget(alert) {
  if (alert?.clientId) {
    return { targetType: 'CLIENT', targetValue: alert.clientId };
  }
  if (alert?.sourceIp) {
    return { targetType: 'IP', targetValue: alert.sourceIp };
  }
  return { targetType: 'IP', targetValue: '' };
}

export function getAlertTitle(alert) {
  return alert?.alertType || alert?.ruleCode || alert?.fingerprint || 'Security alert';
}
