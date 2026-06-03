import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';
import { formatRequests, formatSize } from '../../utils/format';

const columns = ['API Type', 'Scope', 'Service Name', 'Enabled', 'Policy', 'Retry', 'Latency', 'Timeout', 'Retention', 'Notification', 'Actions'];

function formatPolicy(config) {
  if (config.apiType === 'CLIENT') return config.failureAction || '-';
  return `${formatRequests(config.maxRequests, config.throttleWindowSec)} / ${formatSize(config.maxRequestKb)}`;
}

function formatRetry(config) {
  if (config.apiType !== 'CLIENT') return '-';
  const retries = config.maxRetries ?? '-';
  const delay = config.retryDelayMs ? `${config.retryDelayMs}ms` : '-';
  return `${retries} / ${delay}`;
}

export default function DefaultConfigsTable({ configs, busyId, onEdit, onToggle, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={configs}
      emptyTitle="No default configs found"
      emptyDescription="Create a global or service-level baseline configuration."
      renderRow={(config) => (
        <tr key={config.id}>
          <td><Badge tone={config.apiType === 'CLIENT' ? 'purple' : 'blue'} size="sm">{config.apiType || 'EXPOSED'}</Badge></td>
          <td><Badge tone={config.scope === 'GLOBAL' ? 'purple' : 'info'} size="sm">{config.scope}</Badge></td>
          <td><strong>{config.scope === 'GLOBAL' ? '-- All Services --' : config.microServiceName || config.microServiceId || '-'}</strong></td>
          <td><ToggleSwitch checked={Boolean(config.enabled)} disabled={busyId === config.id || config.id?.startsWith('default-')} onChange={() => onToggle(config)} /></td>
          <td>{formatPolicy(config)}</td>
          <td>{formatRetry(config)}</td>
          <td>{config.latencyThresholdMs ? `${config.latencyThresholdMs}ms` : '-'}</td>
          <td>{config.timeoutMs ? `${config.timeoutMs / 1000}s` : '-'}</td>
          <td>{config.logRetentionDays ? `${config.logRetentionDays}d` : '-'}</td>
          <td>{config.notificationRuleId || '-'}</td>
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
