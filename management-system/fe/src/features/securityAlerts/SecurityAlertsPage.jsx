import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { ackAlert, deleteAlert, getAlertSummary, getSecurityAlerts, ignoreAlert, resolveAlert, temporaryBlacklistAlert } from '../../services/securityAlertsService';
import SecurityAlertDetailModal from './SecurityAlertDetailModal';
import SecurityAlertsFilters from './SecurityAlertsFilters';
import SecurityAlertsTable from './SecurityAlertsTable';
import TemporaryBlacklistModal from './TemporaryBlacklistModal';
import { buildActionPayload } from './securityAlerts.helpers';

const initialFilters = {
  status: '',
  severity: '',
  serviceName: '',
  endpointId: '',
  clientId: '',
  sourceIp: '',
  ruleCode: '',
};

export default function SecurityAlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [filters, setFilters] = useState(initialFilters);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);

  const counts = useMemo(() => ({
    open: summary?.openCount || 0,
    medium: summary?.mediumOpenCount || 0,
    high: summary?.highOpenCount || 0,
    critical: summary?.criticalOpenCount || 0,
    recent: summary?.recent24hCount || 0,
  }), [summary]);
  const deletableAlerts = useMemo(
    () => alerts.filter((alert) => alert.status === 'RESOLVED' || alert.status === 'IGNORED'),
    [alerts],
  );

  useEffect(() => {
    let mounted = true;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const [nextAlerts, nextSummary] = await Promise.all([
          getSecurityAlerts(filters),
          getAlertSummary(),
        ]);
        if (mounted) {
          setAlerts(nextAlerts || []);
          setSummary(nextSummary || null);
        }
      } catch (err) {
        if (mounted) setError(err.message);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => { mounted = false; };
  }, [filters]);

  function updateFilters(next) {
    setFilters((current) => ({ ...current, ...next }));
  }

  async function refreshData() {
    setLoading(true);
    setError('');
    try {
      const [nextAlerts, nextSummary] = await Promise.all([
        getSecurityAlerts(filters),
        getAlertSummary(),
      ]);
      setAlerts(nextAlerts || []);
      setSummary(nextSummary || null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function runAlertAction(alert, action) {
    setBusyId(alert.id);
    setError('');
    try {
      await action(alert.id, buildActionPayload());
      await refreshData();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleBlacklist(alert, payload) {
    setBusyId(alert.id);
    setError('');
    try {
      await temporaryBlacklistAlert(alert.id, payload);
      setModal(null);
      await refreshData();
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(alert) {
    const deletable = alert.status === 'RESOLVED' || alert.status === 'IGNORED';
    if (!deletable) {
      setError('Only resolved or ignored alerts can be deleted.');
      return;
    }
    if (!window.confirm(`Delete alert ${alert.alertType || alert.id}?`)) {
      return;
    }
    setBusyId(alert.id);
    setError('');
    try {
      await deleteAlert(alert.id);
      if (modal?.alert?.id === alert.id) {
        setModal(null);
      }
      await refreshData();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleCleanDeletable() {
    if (deletableAlerts.length === 0) {
      return;
    }
    if (!window.confirm(`Delete ${deletableAlerts.length} resolved/ignored alert(s) currently shown?`)) {
      return;
    }
    setBusyId('bulk-delete');
    setError('');
    try {
      for (const alert of deletableAlerts) {
        await deleteAlert(alert.id);
      }
      if (modal?.alert && deletableAlerts.some((alert) => alert.id === modal.alert.id)) {
        setModal(null);
      }
      await refreshData();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  return (
    <div className="page-content security-alert-page">
      <div className="page-header">
        <div>
          <h1>Security Alerts</h1>
          <p className="status-line"><span className="dot red" /> High/Critical alerts requiring admin review</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={refreshData} disabled={loading}>⟳ Refresh</Button>
          <Button variant="danger-ghost" onClick={handleCleanDeletable} disabled={loading || busyId === 'bulk-delete' || deletableAlerts.length === 0}>
            {busyId === 'bulk-delete' ? 'Cleaning...' : `Clean Deletable (${deletableAlerts.length})`}
          </Button>
          <Button variant="ghost" onClick={() => setFilters(initialFilters)} disabled={loading}>Clear Filters</Button>
        </div>
      </div>
      {error ? <div className="notice">{error}</div> : null}
      <div className="stats-grid security-alert-stats">
        <StatCard icon="●" label="Open" value={counts.open} tone="purple" meta="all open severities" />
        <StatCard icon="◆" label="Medium" value={counts.medium} tone="blue" meta="dashboard only" />
        <StatCard icon="▲" label="High" value={counts.high} tone="warning" meta="email notification" />
        <StatCard icon="!" label="Critical" value={counts.critical} tone="danger" meta="auto blacklist attempted" />
        <StatCard icon="24" label="Recent 24h" value={counts.recent} tone="neutral" meta="new alert activity" />
      </div>
      <SecurityAlertsFilters filters={filters} onChange={updateFilters} />
      <section className="table-card">
        <div className="table-card-header">
          <h2>Central Security Alerts</h2>
          <div><Button variant="ghost" disabled={loading}>≡</Button><Button variant="ghost" onClick={refreshData}>⇩</Button></div>
        </div>
        <SecurityAlertsTable
          alerts={alerts}
          busyId={busyId}
          onView={(alert) => setModal({ mode: 'detail', alert })}
          onAck={(alert) => runAlertAction(alert, ackAlert)}
          onIgnore={(alert) => runAlertAction(alert, ignoreAlert)}
          onResolve={(alert) => runAlertAction(alert, resolveAlert)}
          onBlacklist={(alert) => setModal({ mode: 'blacklist', alert })}
          onDelete={handleDelete}
        />
      </section>
      <div className="table-footer"><span>Showing {alerts.length} alerts</span><span>{loading ? 'Loading...' : 'Up to date'}</span></div>
      {modal?.mode === 'detail' ? (
        <SecurityAlertDetailModal
          alert={modal.alert}
          busyId={busyId}
          onClose={() => setModal(null)}
          onAck={(alert) => runAlertAction(alert, ackAlert)}
          onIgnore={(alert) => runAlertAction(alert, ignoreAlert)}
          onResolve={(alert) => runAlertAction(alert, resolveAlert)}
          onBlacklist={(alert) => setModal({ mode: 'blacklist', alert })}
        />
      ) : null}
      {modal?.mode === 'blacklist' ? (
        <TemporaryBlacklistModal alert={modal.alert} saving={busyId === modal.alert.id} onClose={() => setModal(null)} onSubmit={handleBlacklist} />
      ) : null}
    </div>
  );
}
