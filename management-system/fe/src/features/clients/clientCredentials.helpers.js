export function credentialStatusTone(status) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'REVOKED') return 'danger';
  if (status === 'EXPIRED') return 'warning';
  return 'neutral';
}

export function expiryStateTone(expiryState) {
  if (expiryState === 'VALID' || expiryState === 'NO_EXPIRY') return 'success';
  if (expiryState === 'EXPIRING_SOON') return 'warning';
  if (expiryState === 'EXPIRED' || expiryState === 'REVOKED') return 'danger';
  return 'neutral';
}

export function credentialToCreatePayload(form) {
  return {
    microServiceId: form.microServiceId,
    keyId: form.keyId.trim() || null,
    expiresAt: form.expiresAt || null,
  };
}
