import Badge, { methodTone, statusTone } from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRelativeTime } from '../../utils/date';

const columns = ['API Name', 'Service', 'Method', 'Path', 'Protocol', 'Enabled', 'Default Config', 'Sync Status', 'Last Sync', 'Actions'];

export default function ExposedApisTable({ apis, busyId, onToggleEnabled, onConfigure, onResetDefault, onRemove }) {
  return (
    <DataTable
      columns={columns}
      rows={apis}
      emptyTitle="No exposed APIs found"
      emptyDescription="Start a service with @SharedApi or adjust the current filters."
      renderRow={(api) => (
        <tr key={api.id}>
          <td><button className="link-button" type="button" onClick={() => onConfigure(api)}>{api.name}</button></td>
          <td>{api.microServiceName || '-'}</td>
          <td><Badge tone={methodTone(api.method)} size="sm">{api.method || '-'}</Badge></td>
          <td><code>{api.path || '-'}</code></td>
          <td>{api.protocol || '-'}</td>
          <td><ToggleSwitch checked={Boolean(api.enabled)} disabled={busyId === api.id || api.id?.startsWith('sample')} onChange={() => onToggleEnabled(api)} /></td>
          <td><Badge tone={api.useDefaultConfig ? 'info' : 'neutral'} size="sm">{api.useDefaultConfig ? 'YES' : 'NO'}</Badge></td>
          <td><Badge tone={statusTone(api.syncStatus)} size="sm">{api.syncStatus || '-'}</Badge></td>
          <td>{formatRelativeTime(api.lastSyncedAt || api.updatedAt)}</td>
          <td>
            <div className="row-actions">
              <Button variant="ghost" onClick={() => onConfigure(api)}>Config</Button>
              <Button variant="ghost" disabled={api.id?.startsWith('sample')} onClick={() => onResetDefault(api)}>Default</Button>
              <Button variant="danger-ghost" disabled={api.id?.startsWith('sample')} onClick={() => onRemove(api)}>Remove</Button>
            </div>
          </td>
        </tr>
      )}
    />
  );
}
