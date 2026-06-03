import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';
import { clientFormToPayload, clientToForm } from './clients.helpers';

export default function ClientModal({ client, saving, onClose, onSave }) {
  const [form, setForm] = useState(() => clientToForm(client));
  const [error, setError] = useState('');
  const editing = Boolean(client?.id);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    const payload = clientFormToPayload(form);
    if (!payload.name || !payload.clientCode) {
      setError('Name and client code are required.');
      return;
    }
    if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
      setError('Email format is invalid.');
      return;
    }
    setError('');
    onSave(client, payload);
  }

  return (
    <Modal
      title={editing ? 'Edit Client' : 'Create Client'}
      description="Manage external clients that call exposed APIs. Credentials are generated in a separate step."
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving} onClick={submit}>{saving ? 'Saving...' : editing ? 'Save Client' : 'Create Client'}</Button>
        </>
      )}
    >
      <div className="form-grid wide">
        <label>
          <span>Name</span>
          <input value={form.name} onChange={(event) => updateField('name', event.target.value)} placeholder="Partner A" />
        </label>
        <label>
          <span>Client Code</span>
          <input value={form.clientCode} onChange={(event) => updateField('clientCode', event.target.value)} placeholder="partner-a" />
        </label>
        <label>
          <span>Email</span>
          <input value={form.email} onChange={(event) => updateField('email', event.target.value)} placeholder="tech@partner-a.com" />
        </label>
        <label>
          <span>Description</span>
          <textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} placeholder="Integration owner or context" rows="4" />
        </label>
      </div>
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
