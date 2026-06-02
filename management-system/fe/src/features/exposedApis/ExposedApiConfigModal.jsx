import { useState } from 'react';
import Badge, { statusTone } from '../../components/Badge';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { formatDateTime } from '../../utils/date';
import { validatePositiveFields } from '../../utils/format';
import { apiToLimitForm, limitFields, limitFormToPayload } from './exposedApis.helpers';

export default function ExposedApiConfigModal({ api, saving, onClose, onSave, onResetDefault }) {
  const [form, setForm] = useState(() => apiToLimitForm(api));
  const [error, setError] = useState('');

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    const payload = limitFormToPayload(form);
    const validationError = validatePositiveFields(payload, limitFields.map(([key]) => key));
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    onSave(api, payload);
  }

  return (
    <Modal
      title="Exposed API Config"
      description={`${api.name} · ${api.method || '-'} ${api.path || ''}`}
      onClose={onClose}
      footer={(
        <>
          <Button variant="secondary" disabled={saving || api.id?.startsWith('sample')} onClick={() => onResetDefault(api)}>Reset to Default</Button>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || api.id?.startsWith('sample')} onClick={submit}>{saving ? 'Saving...' : 'Save Config'}</Button>
        </>
      )}
    >
      <div className="modal-grid">
        <section className="detail-card">
          <h3>Overview</h3>
          <dl>
            <dt>Service</dt><dd>{api.microServiceName || '-'}</dd>
            <dt>Protocol</dt><dd>{api.protocol || '-'}</dd>
            <dt>Registration</dt><dd>{api.registrationSource || '-'}</dd>
            <dt>Status</dt><dd><Badge tone={statusTone(api.syncStatus)} size="sm">{api.syncStatus || '-'}</Badge></dd>
            <dt>Use Default</dt><dd>{api.useDefaultConfig ? 'Yes' : 'No'}</dd>
            <dt>Last Synced</dt><dd>{formatDateTime(api.lastSyncedAt)}</dd>
          </dl>
        </section>
        <section className="detail-card">
          <h3>Limit Config</h3>
          <div className="form-grid">
            {limitFields.map(([key, label]) => (
              <label key={key}>
                <span>{label}</span>
                <input type="number" min="1" value={form[key]} onChange={(event) => updateField(key, event.target.value)} />
              </label>
            ))}
          </div>
          <p className="helper-text">Saving custom limits will set this API to custom config.</p>
          {error ? <p className="form-error">{error}</p> : null}
        </section>
      </div>
    </Modal>
  );
}
