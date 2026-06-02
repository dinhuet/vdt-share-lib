import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRequests, formatSize } from '../../utils/format';

const columns = ['Scope', 'Service Name', 'Enabled', 'Max Requests', 'Window', 'Size Limit', 'Latency', 'Timeout', 'Retention', 'Actions'];

export default function DefaultConfigsTable({ configs, busyId, onEdit, onToggle, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={configs}
      emptyTitle="No default configs found"
      emptyDescription="Create a global or service-level baseline configuration."
      renderRow={(config) => (
        <tr key={config.id}>
          <td><Badge tone={config.scope === 'GLOBAL' ? 'purple' : 'info'} size="sm">{config.scope}</Badge></td>
          <td><strong>{config.scope === 'GLOBAL' ? '-- All Services --' : config.microServiceName || config.microServiceId || '-'}</strong></td>
          <td><ToggleSwitch checked={Boolean(config.enabled)} disabled={busyId === config.id || config.id?.startsWith('default-')} onChange={() => onToggle(config)} /></td>
          <td>{formatRequests(config.maxRequests, config.throttleWindowSec)}</td>
          <td>{config.throttleWindowSec ? `${config.throttleWindowSec}s` : '-'}</td>
          <td>{formatSize(config.maxRequestKb)}</td>
          <td>{config.latencyThresholdMs ? `${config.latencyThresholdMs}ms` : '-'}</td>
          <td>{config.timeoutMs ? `${config.timeoutMs / 1000}s` : '-'}</td>
          <td>{config.logRetentionDays ? `${config.logRetentionDays}d` : '-'}</td>
          <td>
            <div className="row-actions">
              <Button variant="ghost" onClick={() => onEdit(config)}>Edit</Button>
              <Button variant="danger-ghost" disabled={config.id?.startsWith('default-')} onClick={() => onDelete(config)}>Delete</Button>
            </div>
          </td>
        </tr>
      )}
    />
  );
}
