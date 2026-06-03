import { useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';

export default function RevokeCredentialModal({ credential, saving, onClose, onConfirm }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  function submit() {
    if (!reason.trim()) {
      setError('Revoke reason is required.');
      return;
    }
    setError('');
    onConfirm(credential, reason.trim());
  }

  return (
    <Modal
      title="Revoke Credential"
      description={`Revoke key ${credential?.keyId || ''}. Requests using this credential will fail.`}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger-ghost" disabled={saving} onClick={submit}>{saving ? 'Revoking...' : 'Revoke Credential'}</Button>
        </>
      )}
    >
      <label>
        <span>Reason</span>
        <textarea value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Rotation completed, credential leaked, etc." rows="4" />
      </label>
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
