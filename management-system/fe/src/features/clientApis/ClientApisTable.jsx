import Badge, { methodTone, statusTone } from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRelativeTime } from '../../utils/date';

const columns = ['API Name', 'Service', 'Method', 'Destination', 'Protocol', 'Enabled', 'Sync Status', 'Timeout', 'Retries', 'Updated', 'Actions'];

export default function ClientApisTable({ clientApis, busyId, onEdit, onToggleEnabled, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={clientApis}
      emptyTitle="No client APIs found"
        emptyDescription="Client APIs are synced from service registration events. Adjust the current filters if needed."
        renderRow={(api) => {
        const isSample = api.id?.startsWith('client-api-sample');
        return (
          <tr key={api.id}>
            <td><button className="link-button" type="button" onClick={() => onEdit(api)}>{api.name}</button></td>
            <td>{api.microServiceName || api.microServiceId || '-'}</td>
            <td><Badge tone={methodTone(api.method)} size="sm">{api.method || '-'}</Badge></td>
            <td><code>{api.destinationUrl || '-'}</code></td>
            <td>{api.protocol || '-'}</td>
            <td><ToggleSwitch checked={Boolean(api.enabled)} disabled={busyId === api.id || isSample} onChange={() => onToggleEnabled(api)} /></td>
            <td><Badge tone={statusTone(api.syncStatus)} size="sm">{api.syncStatus || '-'}</Badge></td>
            <td>{api.timeoutMs ? `${api.timeoutMs / 1000}s` : '-'}</td>
            <td>{api.maxRetries ?? '-'}</td>
            <td>{formatRelativeTime(api.lastSyncedAt || api.updatedAt)}</td>
            <td>
              <div className="row-actions">
                <Button variant="ghost" onClick={() => onEdit(api)}>Edit</Button>
                <Button variant="danger-ghost" disabled={busyId === api.id || api.syncStatus !== 'STALE' || isSample} onClick={() => onDelete(api)}>Delete</Button>
              </div>
            </td>
          </tr>
        );
      }}
    />
  );
}
