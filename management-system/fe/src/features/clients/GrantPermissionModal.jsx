import { useMemo, useState } from 'react';
import Button from '../../components/Button';
import Modal from '../../components/Modal';

export default function GrantPermissionModal({ client, exposedApis, permissions, saving, onClose, onGrant }) {
  const [exposedApiId, setExposedApiId] = useState('');
  const [error, setError] = useState('');

  const grantedApiIds = useMemo(() => new Set(permissions.map((permission) => permission.exposedApiId)), [permissions]);
  const availableApis = useMemo(() => exposedApis.filter((api) => !grantedApiIds.has(api.id)), [exposedApis, grantedApiIds]);

  function submit() {
    if (!exposedApiId) {
      setError('Exposed API is required.');
      return;
    }
    setError('');
    onGrant({ exposedApiId });
  }

  return (
    <Modal
      title="Grant Permission"
      description={`Allow ${client?.name || client?.clientCode} to call one exposed API.`}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || !availableApis.length} onClick={submit}>{saving ? 'Granting...' : 'Grant Permission'}</Button>
        </>
      )}
    >
      <div className="form-grid">
        <label>
          <span>Exposed API</span>
          <select value={exposedApiId} onChange={(event) => setExposedApiId(event.target.value)}>
            <option value="">Select API</option>
            {availableApis.map((api) => <option key={api.id} value={api.id}>{api.name} · {api.method || '-'} {api.path || ''} · {api.microServiceName || 'unknown service'}</option>)}
          </select>
        </label>
      </div>
      {!availableApis.length ? <p className="warning-text">All exposed APIs are already granted to this client.</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
    </Modal>
  );
}
