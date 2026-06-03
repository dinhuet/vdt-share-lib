import { useEffect, useState } from 'react';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import { createClientCredential, getClientCredentials, revokeClientCredential } from '../../services/clientCredentialsService';
import { getMicroServices } from '../../services/microServicesService';
import { formatDateTime } from '../../utils/date';
import ClientCredentialCreateModal from './ClientCredentialCreateModal';
import CredentialCreatedModal from './CredentialCreatedModal';
import RevokeCredentialModal from './RevokeCredentialModal';
import { credentialStatusTone, expiryStateTone } from './clientCredentials.helpers';

const columns = ['Service', 'Key ID', 'Status', 'Expiry', 'Expires At', 'Revoked', 'Actions'];

export default function ClientCredentialsModal({ client, onClose }) {
  const [credentials, setCredentials] = useState([]);
  const [microServices, setMicroServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [childModal, setChildModal] = useState(null);

  useEffect(() => {
    loadData();
  }, [client?.id]);

  async function loadData() {
    if (!client?.id) return;
    setLoading(true);
    setError('');
    try {
      const [credentialData, serviceData] = await Promise.all([
        getClientCredentials(client.id),
        getMicroServices(),
      ]);
      setCredentials(credentialData || []);
      setMicroServices(serviceData || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function upsertCredential(saved) {
    setCredentials((current) => {
      const exists = current.some((credential) => credential.id === saved.id);
      return exists ? current.map((credential) => (credential.id === saved.id ? saved : credential)) : [saved, ...current];
    });
  }

  async function handleCreate(payload) {
    setBusy(true);
    setError('');
    try {
      const created = await createClientCredential(client.id, payload);
      upsertCredential(created);
      setChildModal({ mode: 'created', credential: created });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleRevoke(credential, reason) {
    setBusy(true);
    setError('');
    try {
      const saved = await revokeClientCredential(client.id, credential.id, reason);
      upsertCredential(saved);
      setChildModal(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Modal
        title="Client Credentials"
        description={`Manage service-scoped credentials for ${client?.name || client?.clientCode}.`}
        onClose={onClose}
        footer={(
          <>
            <Button variant="ghost" onClick={loadData} disabled={loading}>Refresh</Button>
            <Button disabled={client?.status !== 'ACTIVE'} onClick={() => setChildModal({ mode: 'create' })}>Create Credential</Button>
          </>
        )}
      >
        {error ? <div className="notice">{error}</div> : null}
        <DataTable
          columns={columns}
          rows={credentials}
          emptyTitle="No credentials found"
          emptyDescription="Create a credential to let this client authenticate to a microservice."
          renderRow={(credential) => (
            <tr key={credential.id}>
              <td>{credential.microServiceName || credential.microServiceId}</td>
              <td><code>{credential.keyId}</code></td>
              <td><Badge tone={credentialStatusTone(credential.status)} size="sm">{credential.status}</Badge></td>
              <td><Badge tone={expiryStateTone(credential.expiryState)} size="sm">{credential.expiryState}</Badge>{credential.daysUntilExpiry !== null && credential.daysUntilExpiry !== undefined ? <span className="helper-text"> {credential.daysUntilExpiry}d</span> : null}</td>
              <td>{formatDateTime(credential.expiresAt)}</td>
              <td>{credential.revokedAt ? `${formatDateTime(credential.revokedAt)} by ${credential.revokedBy || '-'}` : '-'}</td>
              <td>
                <div className="row-actions">
                  <Button variant="danger-ghost" disabled={busy || credential.status !== 'ACTIVE'} onClick={() => setChildModal({ mode: 'revoke', credential })}>Revoke</Button>
                </div>
              </td>
            </tr>
          )}
        />
      </Modal>
      {childModal?.mode === 'create' ? <ClientCredentialCreateModal client={client} microServices={microServices} saving={busy} onClose={() => setChildModal(null)} onCreate={handleCreate} /> : null}
      {childModal?.mode === 'created' ? <CredentialCreatedModal credential={childModal.credential} onClose={() => setChildModal(null)} /> : null}
      {childModal?.mode === 'revoke' ? <RevokeCredentialModal credential={childModal.credential} saving={busy} onClose={() => setChildModal(null)} onConfirm={handleRevoke} /> : null}
    </>
  );
}
