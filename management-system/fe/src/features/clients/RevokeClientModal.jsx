import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';

export default function RevokeClientModal({ client, saving, onClose, onConfirm }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  function submit() {
    if (!reason.trim()) {
      setError('Revoke reason is required.');
      return;
    }
    setError('');
    onConfirm(client, reason.trim());
  }

  return (
    <Modal
      title="Revoke Client"
      description={`Revoking ${client?.name || client?.clientCode} is permanent in this phase.`}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger-ghost" disabled={saving} onClick={submit}>{saving ? 'Revoking...' : 'Revoke Client'}</Button>
        </>
      )}
    >
      <label>
        <span>Reason</span>
        <textarea value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Contract ended, security incident, etc." rows="4" />
      </label>
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
