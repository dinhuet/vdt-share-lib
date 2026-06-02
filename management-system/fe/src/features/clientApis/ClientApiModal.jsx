import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { CLIENT_API_PROTOCOLS, HTTP_METHODS } from '../../utils/constants';
import { validatePositiveFields } from '../../utils/format';
import { clientApiFormToPayload, clientApiNumericFields, clientApiToForm } from './clientApis.helpers';

export default function ClientApiModal({ clientApi, serviceOptions, saving, onClose, onSave }) {
  const [form, setForm] = useState(() => clientApiToForm(clientApi));
  const [error, setError] = useState('');
  const isSample = clientApi?.id?.startsWith('client-api-sample');

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    const payload = clientApiFormToPayload(form);
    if (!payload.microServiceId || !payload.name || !payload.protocol) {
      setError('Microservice, name, and protocol are required.');
      return;
    }
    const validationError = validatePositiveFields(payload, clientApiNumericFields.map(([key]) => key));
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    onSave(clientApi, payload);
  }

  return (
    <Modal
      title={clientApi ? 'Edit Client API' : 'Create Client API'}
      description="Configure outbound API dependencies used by a microservice."
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || isSample} onClick={submit}>{saving ? 'Saving...' : 'Save Client API'}</Button>
        </>
      )}
    >
      <div className="form-grid wide">
        <label>
          <span>Microservice</span>
          <select value={form.microServiceId} onChange={(event) => updateField('microServiceId', event.target.value)}>
            <option value="">Select service</option>
            {serviceOptions.map((service) => <option key={service.id} value={service.id}>{service.name}</option>)}
          </select>
        </label>
        <label>
          <span>API Name</span>
          <input value={form.name} onChange={(event) => updateField('name', event.target.value)} placeholder="PaymentAuthorizeClient" />
        </label>
        <label>
          <span>Client ID</span>
          <input value={form.clientId} onChange={(event) => updateField('clientId', event.target.value)} placeholder="Optional UUID" />
        </label>
        <label>
          <span>Destination URL</span>
          <input value={form.destinationUrl} onChange={(event) => updateField('destinationUrl', event.target.value)} placeholder="http://service/api/path" />
        </label>
        <label>
          <span>Method</span>
          <select value={form.method} onChange={(event) => updateField('method', event.target.value)}>
            {HTTP_METHODS.map((method) => <option key={method} value={method}>{method}</option>)}
          </select>
        </label>
        <label>
          <span>Protocol</span>
          <select value={form.protocol} onChange={(event) => updateField('protocol', event.target.value)}>
            {CLIENT_API_PROTOCOLS.map((protocol) => <option key={protocol} value={protocol}>{protocol}</option>)}
          </select>
        </label>
        {clientApiNumericFields.map(([key, label]) => (
          <label key={key}>
            <span>{label}</span>
            <input type="number" min="1" value={form[key]} onChange={(event) => updateField(key, event.target.value)} />
          </label>
        ))}
        <label>
          <span>Failure Action</span>
          <input value={form.failureAction} onChange={(event) => updateField('failureAction', event.target.value)} placeholder="LOG_AND_FAIL" />
        </label>
        <label>
          <span>Notification Rule ID</span>
          <input value={form.notificationRuleId} onChange={(event) => updateField('notificationRuleId', event.target.value)} placeholder="Optional UUID" />
        </label>
        <label className="checkbox-label">
          <input type="checkbox" checked={form.enabled} onChange={(event) => updateField('enabled', event.target.checked)} />
          Enabled
        </label>
      </div>
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
