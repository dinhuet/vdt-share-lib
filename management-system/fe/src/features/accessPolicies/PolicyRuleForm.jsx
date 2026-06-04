import { useState } from 'react';
import Button from '../../components/Button';
import { ACCESS_POLICY_MATCH_TYPES, createEmptyPolicyForm, policyFormToPayload, validatePolicyForm } from './accessPolicies.helpers';

export default function PolicyRuleForm({ type, clients, saving, onCreate, onCreated }) {
  const [form, setForm] = useState(createEmptyPolicyForm);
  const [error, setError] = useState('');

  function updateField(key, value) {
    setForm((current) => ({
      ...current,
      [key]: value,
      ...(key === 'matchType' ? { matchValue: '' } : {}),
      ...(key === 'temporary' && !value ? { expiresAt: '' } : {}),
    }));
  }

  async function submit() {
    const validationError = validatePolicyForm(form);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    try {
      await onCreate(policyFormToPayload(type, form));
      setForm(createEmptyPolicyForm());
      onCreated?.();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="policy-form-card">
      <div className="policy-form-grid">
        <label>
          <span>Match Type</span>
          <select value={form.matchType} onChange={(event) => updateField('matchType', event.target.value)}>
            {ACCESS_POLICY_MATCH_TYPES.map((matchType) => <option key={matchType} value={matchType}>{matchType}</option>)}
          </select>
        </label>
        {form.matchType === 'CLIENT_ID' ? (
          <label>
            <span>Client</span>
            <select value={form.matchValue} onChange={(event) => updateField('matchValue', event.target.value)}>
              <option value="">Select client</option>
              {clients.map((client) => <option key={client.id} value={client.id}>{client.name} ({client.clientCode})</option>)}
            </select>
          </label>
        ) : (
          <label>
            <span>{form.matchType === 'IP' ? 'IP Address' : 'CIDR Range'}</span>
            <input value={form.matchValue} onChange={(event) => updateField('matchValue', event.target.value)} placeholder={form.matchType === 'IP' ? '203.0.113.10' : '203.0.113.0/24'} />
          </label>
        )}
        <label className="checkbox-label">
          <input checked={form.temporary} type="checkbox" onChange={(event) => updateField('temporary', event.target.checked)} />
          Temporary rule
        </label>
        {form.temporary ? (
          <label>
            <span>Expires At</span>
            <input type="datetime-local" value={form.expiresAt} onChange={(event) => updateField('expiresAt', event.target.value)} />
          </label>
        ) : null}
      </div>
      <div className="policy-form-actions">
        {error ? <p className="form-error">{error}</p> : <p className="helper-text">CLIENT_ID uses client UUID; IP/CIDR should be enforced by gateway/runtime later.</p>}
        <Button disabled={saving} onClick={submit}>{saving ? 'Saving...' : `Add ${type === 'WHITE' ? 'Whitelist' : 'Blacklist'}`}</Button>
      </div>
    </div>
  );
}
