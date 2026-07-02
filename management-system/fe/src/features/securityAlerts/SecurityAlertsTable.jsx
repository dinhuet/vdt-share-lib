import Badge from '../../components/Badge';
import DataTable from '../../components/DataTable';
import RowActions from '../../components/RowActions';
import { formatRelativeTime } from '../../utils/date';
import { getAlertTitle, severityTone, statusTone } from './securityAlerts.helpers';

const columns = ['Alert', 'Severity', 'Status', 'Target', 'Metric', 'Last Seen', 'Actions'];

export default function SecurityAlertsTable({ alerts, busyId, onView, onAck, onIgnore, onResolve, onBlacklist, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={alerts}
      emptyTitle="No security alerts found"
      emptyDescription="Security alerts matching the current filters will appear here."
      renderRow={(alert) => {
        const closed = alert.status === 'RESOLVED' || alert.status === 'IGNORED';
        return (
          <tr key={alert.id}>
            <td>
              <button className="link-button" type="button" onClick={() => onView(alert)}>{getAlertTitle(alert)}</button>
              <div className="muted-cell">{alert.serviceName || '-'} {alert.endpointName ? `/ ${alert.endpointName}` : ''}</div>
            </td>
            <td><Badge tone={severityTone(alert.severity)} size="sm">{alert.severity || '-'}</Badge></td>
            <td><Badge tone={statusTone(alert.status)} size="sm">{alert.status || '-'}</Badge></td>
            <td>{alert.clientId || alert.sourceIp || '-'}</td>
            <td>{alert.metric || '-'} {alert.currentValue ?? '-'} / {alert.thresholdValue ?? '-'}</td>
            <td>{formatRelativeTime(alert.lastSeenAt || alert.updatedAt || alert.createdAt)}</td>
            <td>
              <RowActions actions={[
                { label: 'View', disabled: busyId === alert.id, onClick: () => onView(alert) },
                { label: 'Ack', disabled: busyId === alert.id || alert.status !== 'OPEN', onClick: () => onAck(alert) },
                { label: 'Ignore', disabled: busyId === alert.id || closed, onClick: () => onIgnore(alert) },
                { label: 'Resolve', disabled: busyId === alert.id || alert.status === 'RESOLVED', onClick: () => onResolve(alert) },
                { label: 'Temp Blacklist', danger: true, disabled: busyId === alert.id || closed, onClick: () => onBlacklist(alert) },
                { label: 'Delete', danger: true, disabled: busyId === alert.id || !closed, onClick: () => onDelete(alert) },
              ]} />
            </td>
          </tr>
        );
      }}
    />
  );
}
