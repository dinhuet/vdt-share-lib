import { useEffect, useState } from 'react';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import { getAlertNotifications, getAlertOccurrences, getSecurityAlert } from '../../services/securityAlertsService';
import { formatDateTime } from '../../utils/date';
import { getAlertTitle, severityTone, statusTone } from './securityAlerts.helpers';

export default function SecurityAlertDetailModal({ alert, busyId, onClose, onAck, onIgnore, onResolve, onBlacklist }) {
  const [detail, setDetail] = useState(alert);
  const [occurrences, setOccurrences] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;

    async function loadDetail() {
      setLoading(true);
      setError('');
      try {
        const [nextDetail, nextOccurrences, nextNotifications] = await Promise.all([
          getSecurityAlert(alert.id),
          getAlertOccurrences(alert.id),
          getAlertNotifications(alert.id),
        ]);
        if (mounted) {
          setDetail(nextDetail || alert);
          setOccurrences(nextOccurrences || []);
          setNotifications(nextNotifications || []);
        }
      } catch (err) {
        if (mounted) setError(err.message);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    loadDetail();
    return () => { mounted = false; };
  }, [alert]);

  const current = detail || alert;
  const closed = current.status === 'RESOLVED' || current.status === 'IGNORED';

  return (
    <Modal
      title="Security Alert Detail"
      description={getAlertTitle(current)}
      onClose={onClose}
      footer={(
        <>
          <Button variant="ghost" disabled={busyId === current.id || current.status !== 'OPEN'} onClick={() => onAck(current)}>Ack</Button>
          <Button variant="ghost" disabled={busyId === current.id || closed} onClick={() => onIgnore(current)}>Ignore</Button>
          <Button variant="ghost" disabled={busyId === current.id || current.status === 'RESOLVED'} onClick={() => onResolve(current)}>Resolve</Button>
          <Button variant="danger-ghost" disabled={busyId === current.id || closed} onClick={() => onBlacklist(current)}>Temp Blacklist</Button>
        </>
      )}
    >
      {error ? <div className="notice">{error}</div> : null}
      {loading ? <p className="helper-text">Loading alert details...</p> : null}
      <div className="modal-grid security-alert-detail-grid">
        <section className="detail-card">
          <h3>Metadata</h3>
          <dl>
            <dt>Severity</dt><dd><Badge tone={severityTone(current.severity)} size="sm">{current.severity || '-'}</Badge></dd>
            <dt>Status</dt><dd><Badge tone={statusTone(current.status)} size="sm">{current.status || '-'}</Badge></dd>
            <dt>Service</dt><dd>{current.serviceName || '-'}</dd>
            <dt>Endpoint</dt><dd>{current.endpointName || current.endpointId || '-'}</dd>
            <dt>Client</dt><dd>{current.clientId || '-'}</dd>
            <dt>Source IP</dt><dd>{current.sourceIp || '-'}</dd>
            <dt>First Seen</dt><dd>{formatDateTime(current.firstSeenAt)}</dd>
            <dt>Last Seen</dt><dd>{formatDateTime(current.lastSeenAt)}</dd>
          </dl>
        </section>
        <section className="detail-card">
          <h3>Message & Metrics</h3>
          <p className="alert-message">{current.message || '-'}</p>
          <dl>
            <dt>Metric</dt><dd>{current.metric || '-'}</dd>
            <dt>Current</dt><dd>{current.currentValue ?? '-'}</dd>
            <dt>Threshold</dt><dd>{current.thresholdValue ?? '-'}</dd>
            <dt>Window</dt><dd>{current.windowSeconds ? `${current.windowSeconds}s` : '-'}</dd>
            <dt>Count</dt><dd>{current.count ?? '-'}</dd>
            <dt>Fingerprint</dt><dd><code>{current.fingerprint || '-'}</code></dd>
          </dl>
        </section>
      </div>
      <section className="detail-card alert-detail-section">
        <h3>Occurrences</h3>
        <DataTable
          columns={['Rule', 'Metric', 'Current', 'Threshold', 'Window', 'Event Time']}
          rows={occurrences}
          emptyTitle="No occurrences"
          renderRow={(item) => (
            <tr key={item.id}>
              <td>{item.ruleCode || '-'}</td>
              <td>{item.metric || '-'}</td>
              <td>{item.currentValue ?? '-'}</td>
              <td>{item.thresholdValue ?? '-'}</td>
              <td>{item.windowSeconds ? `${item.windowSeconds}s` : '-'}</td>
              <td>{formatDateTime(item.eventTimestamp || item.createdAt)}</td>
            </tr>
          )}
        />
      </section>
      <section className="detail-card alert-detail-section">
        <h3>Notification Delivery History</h3>
        <DataTable
          columns={['Channel', 'Recipient', 'Status', 'Attempts', 'Sent', 'Error']}
          rows={notifications}
          emptyTitle="No notification deliveries"
          renderRow={(item) => (
            <tr key={item.id}>
              <td>{item.channel || '-'}</td>
              <td>{item.recipient || '-'}</td>
              <td>{item.status || '-'}</td>
              <td>{item.attemptCount ?? '-'}</td>
              <td>{formatDateTime(item.sentAt || item.updatedAt || item.createdAt)}</td>
              <td>{item.lastError || '-'}</td>
            </tr>
          )}
        />
      </section>
    </Modal>
  );
}
