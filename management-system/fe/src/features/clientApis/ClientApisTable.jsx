import Badge, { methodTone } from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRelativeTime } from '../../utils/date';

const columns = ['API Name', 'Service', 'Method', 'Destination', 'Protocol', 'Enabled', 'Deleted', 'Timeout', 'Retries', 'Updated', 'Actions'];

export default function ClientApisTable({ clientApis, busyId, onEdit, onToggleEnabled, onDelete, onRestore }) {
  return (
    <DataTable
      columns={columns}
      rows={clientApis}
      emptyTitle="No client APIs found"
      emptyDescription="Create a client API or adjust the current filters."
      renderRow={(api) => {
        const isDeleted = Boolean(api.deleted);
        const isSample = api.id?.startsWith('client-api-sample');
        return (
          <tr key={api.id}>
            <td><button className="link-button" disabled={isDeleted} type="button" onClick={() => onEdit(api)}>{api.name}</button></td>
            <td>{api.microServiceName || api.microServiceId || '-'}</td>
            <td><Badge tone={methodTone(api.method)} size="sm">{api.method || '-'}</Badge></td>
            <td><code>{api.destinationUrl || '-'}</code></td>
            <td>{api.protocol || '-'}</td>
            <td><ToggleSwitch checked={Boolean(api.enabled)} disabled={busyId === api.id || isDeleted || isSample} onChange={() => onToggleEnabled(api)} /></td>
            <td><Badge tone={isDeleted ? 'danger' : 'success'} size="sm">{isDeleted ? 'YES' : 'NO'}</Badge></td>
            <td>{api.timeoutMs ? `${api.timeoutMs / 1000}s` : '-'}</td>
            <td>{api.maxRetries ?? '-'}</td>
            <td>{formatRelativeTime(api.updatedAt || api.deletedAt)}</td>
            <td>
              <div className="row-actions">
                {isDeleted ? (
                  <Button variant="ghost" disabled={busyId === api.id || isSample} onClick={() => onRestore(api)}>Restore</Button>
                ) : (
                  <>
                    <Button variant="ghost" onClick={() => onEdit(api)}>Edit</Button>
                    <Button variant="danger-ghost" disabled={busyId === api.id || isSample} onClick={() => onDelete(api)}>Delete</Button>
                  </>
                )}
              </div>
            </td>
          </tr>
        );
      }}
    />
  );
}
