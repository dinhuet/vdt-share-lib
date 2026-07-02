import { useEffect, useState } from 'react';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import RowActions from '../../components/RowActions';
import { deleteClientPermission, disableClientPermission, enableClientPermission, getClientPermissions, grantClientPermission } from '../../services/clientPermissionsService';
import { getExposedApis } from '../../services/exposedApisService';
import { formatRelativeTime } from '../../utils/date';
import GrantPermissionModal from './GrantPermissionModal';

const columns = ['API', 'Service', 'Method', 'Path', 'Enabled', 'Updated', 'Actions'];

export default function ClientPermissionsModal({ client, onClose }) {
  const [permissions, setPermissions] = useState([]);
  const [exposedApis, setExposedApis] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [showGrantModal, setShowGrantModal] = useState(false);

  useEffect(() => {
    loadData();
  }, [client?.id]);

  async function loadData() {
    if (!client?.id) return;
    setLoading(true);
    setError('');
    try {
      const [permissionData, apiData] = await Promise.all([
        getClientPermissions(client.id),
        getExposedApis(),
      ]);
      setPermissions(permissionData || []);
      setExposedApis(apiData || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function upsertPermission(saved) {
    setPermissions((current) => {
      const exists = current.some((permission) => permission.id === saved.id);
      return exists ? current.map((permission) => (permission.id === saved.id ? saved : permission)) : [saved, ...current];
    });
  }

  async function handleGrant(payload) {
    setBusyId('grant');
    setError('');
    try {
      upsertPermission(await grantClientPermission(client.id, payload));
      setShowGrantModal(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleToggle(permission) {
    setBusyId(permission.id);
    setError('');
    try {
      const saved = permission.enabled
        ? await disableClientPermission(client.id, permission.id)
        : await enableClientPermission(client.id, permission.id);
      upsertPermission(saved);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(permission) {
    setBusyId(permission.id);
    setError('');
    try {
      await deleteClientPermission(client.id, permission.id);
      setPermissions((current) => current.filter((item) => item.id !== permission.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  return (
    <>
      <Modal
        title="Client Permissions"
        description={`Manage exposed API permissions for ${client?.name || client?.clientCode}.`}
        onClose={onClose}
        footer={(
          <>
            <Button variant="ghost" onClick={loadData} disabled={loading}>Refresh</Button>
            <Button disabled={client?.status !== 'ACTIVE'} onClick={() => setShowGrantModal(true)}>Grant Permission</Button>
          </>
        )}
      >
        {error ? <div className="notice">{error}</div> : null}
        <DataTable
          columns={columns}
          rows={permissions}
          emptyTitle="No permissions found"
          emptyDescription="Grant an exposed API permission so this client can call it at runtime."
          renderRow={(permission) => (
            <tr key={permission.id}>
              <td>{permission.exposedApiName || permission.exposedApiId}</td>
              <td>{permission.microServiceName || '-'}</td>
              <td><Badge tone="info" size="sm">{permission.method || '-'}</Badge></td>
              <td><code>{permission.path || '-'}</code></td>
              <td><Badge tone={permission.enabled ? 'success' : 'neutral'} size="sm">{permission.enabled ? 'ENABLED' : 'DISABLED'}</Badge></td>
              <td>{formatRelativeTime(permission.updatedAt || permission.createdAt)}</td>
              <td>
                <RowActions actions={[
                  { label: permission.enabled ? 'Disable' : 'Enable', disabled: busyId === permission.id || client?.status !== 'ACTIVE', onClick: () => handleToggle(permission) },
                  { label: 'Delete', danger: true, disabled: busyId === permission.id, onClick: () => handleDelete(permission) },
                ]} />
              </td>
            </tr>
          )}
        />
      </Modal>
      {showGrantModal ? <GrantPermissionModal client={client} exposedApis={exposedApis} permissions={permissions} saving={busyId === 'grant'} onClose={() => setShowGrantModal(false)} onGrant={handleGrant} /> : null}
    </>
  );
}
