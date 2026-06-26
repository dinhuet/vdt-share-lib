import { useMemo, useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { defaultBlacklistTarget, getAlertTitle } from './securityAlerts.helpers';

export default function TemporaryBlacklistModal({ alert, saving, onClose, onSubmit }) {
  const defaults = useMemo(() => defaultBlacklistTarget(alert), [alert]);
  const [form, setForm] = useState({ ...defaults, durationMinutes: 15, reason: '' });
  const [error, setError] = useState('');

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    if (!form.targetValue.trim()) {
      setError('Target value is required.');
      return;
    }
    if (!Number.isInteger(Number(form.durationMinutes)) || Number(form.durationMinutes) < 5) {
      setError('Duration must be at least 5 minutes.');
      return;
    }
    try {
      await onSubmit(alert, {
        targetType: form.targetType,
        targetValue: form.targetValue.trim(),
        durationMinutes: Number(form.durationMinutes),
        reason: form.reason.trim() || 'Temporary blacklist from Security Alerts UI',
      });
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <Modal
      title="Temporary Blacklist"
      description={getAlertTitle(alert)}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={saving}>{saving ? 'Applying...' : 'Apply Blacklist'}</Button>
        </>
      )}
    >
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>Target Type</span>
          <select value={form.targetType} onChange={(event) => updateField('targetType', event.target.value)}>
            <option value="CLIENT">CLIENT</option>
            <option value="IP">IP</option>
            <option value="CIDR">CIDR</option>
          </select>
        </label>
        <label>
          <span>Target Value</span>
          <input value={form.targetValue} onChange={(event) => updateField('targetValue', event.target.value)} placeholder="client-id or IP" />
        </label>
        <label>
          <span>Duration Minutes</span>
          <input type="number" min="5" max="1440" value={form.durationMinutes} onChange={(event) => updateField('durationMinutes', event.target.value)} />
        </label>
        <label>
          <span>Reason</span>
          <input value={form.reason} onChange={(event) => updateField('reason', event.target.value)} placeholder="Reason for blacklist" />
        </label>
      </form>
      {error ? <div className="form-error">{error}</div> : null}
      <p className="helper-text">The backend creates a temporary BLACK access policy scoped to this alert endpoint.</p>
    </Modal>
  );
}
