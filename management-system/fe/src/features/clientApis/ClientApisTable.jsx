import Badge, { methodTone, statusTone } from '../../components/Badge';
import DataTable from '../../components/DataTable';
import RowActions from '../../components/RowActions';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRelativeTime } from '../../utils/date';

const columns = ['API Name', 'Service', 'Method', 'Protocol', 'Enabled', 'Use Default', 'Sync Status', 'Updated', 'Actions'];

export default function ClientApisTable({ clientApis, busyId, onEdit, onToggleEnabled, onUseDefault, onDelete }) {
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
            <td>{api.protocol || '-'}</td>
            <td><ToggleSwitch checked={Boolean(api.enabled)} disabled={busyId === api.id || isSample} onChange={() => onToggleEnabled(api)} /></td>
            <td><Badge tone={api.useDefaultConfig ? 'info' : 'neutral'} size="sm">{api.useDefaultConfig ? 'YES' : 'NO'}</Badge></td>
            <td><Badge tone={statusTone(api.syncStatus)} size="sm">{api.syncStatus || '-'}</Badge></td>
            <td>{formatRelativeTime(api.lastSyncedAt || api.updatedAt)}</td>
            <td>
              <RowActions actions={[
                { label: 'Edit', onClick: () => onEdit(api) },
                { label: 'Default', disabled: busyId === api.id || isSample, onClick: () => onUseDefault(api) },
                { label: 'Delete', danger: true, disabled: busyId === api.id || api.syncStatus !== 'STALE' || isSample, onClick: () => onDelete(api) },
              ]} />
            </td>
          </tr>
        );
      }}
    />
  );
}
