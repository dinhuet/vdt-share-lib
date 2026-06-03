import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import { formatRelativeTime } from '../../utils/date';
import { clientStatusTone } from './clients.helpers';

const columns = ['Client', 'Code', 'Email', 'Status', 'Revoked', 'Updated', 'Actions'];

export default function ClientsTable({ clients, busyId, onEdit, onCredentials, onActivate, onDeactivate, onRevoke, onDelete }) {
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
              <div className="row-actions">
                <Button variant="ghost" disabled={busyId === client.id || revoked} onClick={() => onEdit(client)}>Edit</Button>
                <Button variant="ghost" disabled={busyId === client.id || revoked} onClick={() => onCredentials(client)}>Credentials</Button>
                {client.status === 'ACTIVE' ? (
                  <Button variant="ghost" disabled={busyId === client.id} onClick={() => onDeactivate(client)}>Deactivate</Button>
                ) : (
                  <Button variant="ghost" disabled={busyId === client.id || revoked} onClick={() => onActivate(client)}>Activate</Button>
                )}
                {revoked ? (
                  <Button variant="danger-ghost" disabled={busyId === client.id} onClick={() => onDelete(client)}>Delete</Button>
                ) : (
                  <Button variant="danger-ghost" disabled={busyId === client.id} onClick={() => onRevoke(client)}>Revoke</Button>
                )}
              </div>
            </td>
          </tr>
        );
      }}
    />
  );
}
