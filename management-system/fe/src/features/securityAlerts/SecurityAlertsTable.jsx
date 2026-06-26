import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import { formatRelativeTime } from '../../utils/date';
import { getAlertTitle, severityTone, statusTone } from './securityAlerts.helpers';

const columns = ['Alert', 'Severity', 'Status', 'Target', 'Metric', 'Last Seen', 'Actions'];

export default function SecurityAlertsTable({ alerts, busyId, onView, onAck, onIgnore, onResolve, onBlacklist }) {
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
              <div className="row-actions">
                <Button variant="ghost" disabled={busyId === alert.id} onClick={() => onView(alert)}>View</Button>
                <Button variant="ghost" disabled={busyId === alert.id || alert.status !== 'OPEN'} onClick={() => onAck(alert)}>Ack</Button>
                <Button variant="ghost" disabled={busyId === alert.id || closed} onClick={() => onIgnore(alert)}>Ignore</Button>
                <Button variant="ghost" disabled={busyId === alert.id || alert.status === 'RESOLVED'} onClick={() => onResolve(alert)}>Resolve</Button>
                <Button variant="danger-ghost" disabled={busyId === alert.id || closed} onClick={() => onBlacklist(alert)}>Temp Blacklist</Button>
              </div>
            </td>
          </tr>
        );
      }}
    />
  );
}
