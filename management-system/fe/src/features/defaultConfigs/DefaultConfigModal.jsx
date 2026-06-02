import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { APPLY_MODES } from '../../utils/constants';
import { validatePositiveFields } from '../../utils/format';
import { configFields, configFormToPayload, configToForm } from './defaultConfigs.helpers';

export default function DefaultConfigModal({ mode, config, serviceOptions, saving, onClose, onSave }) {
  const [form, setForm] = useState(() => configToForm(config, mode));
  const [error, setError] = useState('');

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    const payload = configFormToPayload(form);
    if (payload.scope === 'SERVICE' && !payload.microServiceId) {
      setError('Service default config requires a microservice.');
      return;
    }
    const validationError = validatePositiveFields(payload, configFields.map(([key]) => key));
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    onSave(payload);
  }

  return (
    <Modal
      title={config ? 'Edit Default Config' : form.scope === 'SERVICE' ? 'Create Service Default' : 'Create Global Default'}
      description="Baseline config values are resolved by service default, then global default, then application fallback."
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || config?.id?.startsWith('default-')} onClick={submit}>{saving ? 'Saving...' : 'Save Default Config'}</Button>
        </>
      )}
    >
      <div className="form-grid wide">
        <label>
          <span>Scope</span>
          <select value={form.scope} disabled={Boolean(config)} onChange={(event) => updateField('scope', event.target.value)}>
            <option value="GLOBAL">GLOBAL</option>
            <option value="SERVICE">SERVICE</option>
          </select>
        </label>
        {form.scope === 'SERVICE' ? (
          <label>
            <span>Microservice</span>
            <select value={form.microServiceId} disabled={Boolean(config)} onChange={(event) => updateField('microServiceId', event.target.value)}>
              <option value="">Select service</option>
              {serviceOptions.map((service) => <option key={service.id} value={service.id}>{service.name}</option>)}
            </select>
          </label>
        ) : null}
        <label className="checkbox-label">
          <input type="checkbox" checked={form.enabled} onChange={(event) => updateField('enabled', event.target.checked)} />
          Enabled
        </label>
        {configFields.map(([key, label]) => (
          <label key={key}>
            <span>{label}</span>
            <input type="number" min="1" value={form[key]} onChange={(event) => updateField(key, event.target.value)} />
          </label>
        ))}
        <label>
          <span>Apply Mode</span>
          <select value={form.applyMode} onChange={(event) => updateField('applyMode', event.target.value)}>
            {APPLY_MODES.map((modeName) => <option key={modeName} value={modeName}>{modeName}</option>)}
          </select>
        </label>
      </div>
      {form.applyMode === 'FORCE_APPLY_ALL' ? <p className="warning-text">This may overwrite custom configs of existing APIs.</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
