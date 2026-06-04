export const ACCESS_POLICY_MATCH_TYPES = ['CLIENT_ID', 'IP', 'CIDR'];

export function splitPoliciesByType(policies) {
  return {
    white: policies.filter((policy) => policy.type === 'WHITE'),
    black: policies.filter((policy) => policy.type === 'BLACK'),
  };
}

export function policyFormToPayload(type, form) {
  return {
    type,
    matchType: form.matchType,
    matchValue: form.matchValue.trim(),
    temporary: Boolean(form.temporary),
    expiresAt: form.temporary ? form.expiresAt : null,
  };
}

export function createEmptyPolicyForm() {
  return {
    matchType: 'CLIENT_ID',
    matchValue: '',
    temporary: false,
    expiresAt: '',
  };
}

export function describePolicyTarget(policy, clients = []) {
  if (policy.matchType !== 'CLIENT_ID') {
    return policy.matchValue;
  }
  const client = clients.find((item) => item.id === policy.matchValue);
  if (!client) {
    return policy.matchValue;
  }
  return `${client.name} (${client.clientCode})`;
}

export function validatePolicyForm(form) {
  if (!form.matchType || !form.matchValue.trim()) {
    return 'Match type and value are required.';
  }
  if (form.temporary && !form.expiresAt) {
    return 'Expiry time is required for temporary rules.';
  }
  if (form.temporary && new Date(form.expiresAt).getTime() <= Date.now()) {
    return 'Expiry time must be in the future.';
  }
  return '';
}
