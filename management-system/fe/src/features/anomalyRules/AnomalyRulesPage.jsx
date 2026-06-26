import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { createAnomalyRule, getAnomalyRules, updateAnomalyRule, updateAnomalyRuleEnabled } from '../../services/anomalyRulesService';
import AnomalyRuleModal from './AnomalyRuleModal';
import AnomalyRulesFilters from './AnomalyRulesFilters';
import AnomalyRulesTable from './AnomalyRulesTable';
import { filterRules, ruleStats } from './anomalyRules.helpers';

export default function AnomalyRulesPage() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [modalRule, setModalRule] = useState(undefined);
  const [filters, setFilters] = useState({ search: '', ruleType: '', severity: '', enabled: '', scopeType: '' });

  const displayedRules = useMemo(() => filterRules(rules, filters), [rules, filters]);
  const stats = useMemo(() => ruleStats(rules), [rules]);

  useEffect(() => { loadRules(); }, []);

  async function loadRules() {
    setLoading(true);
    setError('');
    try {
      setRules(await getAnomalyRules() || []);
    } catch (err) {
      setError(err.message);
      setRules([]);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(payload) {
    setBusyId(modalRule?.id || 'new');
    try {
      const saved = modalRule?.id ? await updateAnomalyRule(modalRule.id, payload) : await createAnomalyRule(payload);
      setRules((current) => current.some((rule) => rule.id === saved.id)
        ? current.map((rule) => (rule.id === saved.id ? saved : rule))
        : [saved, ...current]);
      setModalRule(undefined);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleToggle(rule) {
    setBusyId(rule.id);
    setError('');
    try {
      const saved = await updateAnomalyRuleEnabled(rule.id, !rule.enabled);
      setRules((current) => current.map((item) => (item.id === saved.id ? saved : item)));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  return (
    <div className="page-content">
      <div className="page-header">
        <div><h1>Anomaly Rules</h1><p>Manage anomaly severity, cooldown, and static/baseline detection config.</p></div>
        <div className="header-actions"><Button variant="ghost" disabled={loading} onClick={loadRules}>Refresh</Button><Button onClick={() => setModalRule(null)}>⊕ Create Rule</Button></div>
      </div>
      {error ? <div className="notice error">{error}</div> : null}
      <section className="stats-grid">
        <StatCard icon="◎" label="Total Rules" value={stats.total} />
        <StatCard icon="●" label="Enabled" value={stats.enabled} tone="success" />
        <StatCard icon="▣" label="Static" value={stats.staticCount} tone="info" />
        <StatCard icon="◈" label="Baseline / Hybrid" value={stats.baselineHybrid} tone="warning" />
      </section>
      <AnomalyRulesFilters filters={filters} onChange={(next) => setFilters((current) => ({ ...current, ...next }))} />
      <section className="table-card">
        <div className="table-card-header"><h2>Configured Anomaly Rules</h2><span>{displayedRules.length} shown</span></div>
        <AnomalyRulesTable rules={displayedRules} busyId={busyId} onEdit={setModalRule} onToggle={handleToggle} />
      </section>
      {modalRule !== undefined ? <AnomalyRuleModal rule={modalRule} saving={Boolean(busyId)} onClose={() => setModalRule(undefined)} onSave={handleSave} /> : null}
    </div>
  );
}
