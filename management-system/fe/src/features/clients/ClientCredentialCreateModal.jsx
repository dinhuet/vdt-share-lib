import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { credentialToCreatePayload } from './clientCredentials.helpers';

export default function ClientCredentialCreateModal({ client, microServices, saving, onClose, onCreate }) {
  const [form, setForm] = useState({ microServiceId: '', keyId: '', expiresAt: '' });
  const [error, setError] = useState('');

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    const payload = credentialToCreatePayload(form);
    if (!payload.microServiceId) {
      setError('Microservice is required.');
      return;
    }
    if (payload.expiresAt && new Date(payload.expiresAt).getTime() <= Date.now()) {
      setError('Expiry time must be in the future.');
      return;
    }
    setError('');
    onCreate(payload);
  }

  return (
    <Modal
      title="Create Credential"
      description={`Generate a service-scoped API key and HMAC secret for ${client?.name || client?.clientCode}.`}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving} onClick={submit}>{saving ? 'Generating...' : 'Generate Credential'}</Button>
        </>
      )}
    >
      <div className="form-grid wide">
        <label>
          <span>Microservice</span>
          <select value={form.microServiceId} onChange={(event) => updateField('microServiceId', event.target.value)}>
            <option value="">Select service</option>
            {microServices.map((service) => <option key={service.id} value={service.id}>{service.name}</option>)}
          </select>
        </label>
        <label>
          <span>Key ID</span>
          <input value={form.keyId} onChange={(event) => updateField('keyId', event.target.value)} placeholder="Optional, auto-generated if blank" />
        </label>
        <label>
          <span>Expires At</span>
          <input type="datetime-local" value={form.expiresAt} onChange={(event) => updateField('expiresAt', event.target.value)} />
        </label>
      </div>
      <p className="helper-text">The generated API key and signing secret are shown only once.</p>
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
