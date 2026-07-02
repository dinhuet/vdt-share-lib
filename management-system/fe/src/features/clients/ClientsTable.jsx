import Badge from '../../components/Badge';
import DataTable from '../../components/DataTable';
import RowActions from '../../components/RowActions';
import { formatRelativeTime } from '../../utils/date';
import { clientStatusTone } from './clients.helpers';

const columns = ['Client', 'Code', 'Email', 'Status', 'Revoked', 'Updated', 'Actions'];

export default function ClientsTable({ clients, busyId, onEdit, onCredentials, onPermissions, onActivate, onDeactivate, onRevoke, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={clients}
      emptyTitle="No clients found"
      emptyDescription="Create a client to allow external partners to integrate with exposed APIs."
      renderRow={(client) => {
        const revoked = client.status === 'REVOKED';
        return (
          <tr key={client.id}>
            <td><button className="link-button" type="button" disabled={revoked} onClick={() => onEdit(client)}>{client.name}</button></td>
            <td><code>{client.clientCode}</code></td>
            <td>{client.email || '-'}</td>
            <td><Badge tone={clientStatusTone(client.status)} size="sm">{client.status}</Badge></td>
            <td>{client.revokedAt ? `${formatRelativeTime(client.revokedAt)} by ${client.revokedBy || '-'}` : '-'}</td>
            <td>{formatRelativeTime(client.updatedAt || client.createdAt)}</td>
            <td>
              <RowActions actions={[
                { label: 'Edit', disabled: busyId === client.id || revoked, onClick: () => onEdit(client) },
                { label: 'Credentials', disabled: busyId === client.id || revoked, onClick: () => onCredentials(client) },
                { label: 'Permissions', disabled: busyId === client.id || revoked, onClick: () => onPermissions(client) },
                client.status === 'ACTIVE'
                  ? { label: 'Deactivate', disabled: busyId === client.id, onClick: () => onDeactivate(client) }
                  : { label: 'Activate', disabled: busyId === client.id || revoked, onClick: () => onActivate(client) },
                revoked
                  ? { label: 'Delete', danger: true, disabled: busyId === client.id, onClick: () => onDelete(client) }
                  : { label: 'Revoke', danger: true, disabled: busyId === client.id, onClick: () => onRevoke(client) },
              ]} />
            </td>
          </tr>
        );
      }}
    />
  );
}
