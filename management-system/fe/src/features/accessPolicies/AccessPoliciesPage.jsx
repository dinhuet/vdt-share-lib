import { useEffect, useMemo, useState } from 'react';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { createAccessPolicy, deleteAccessPolicy, getAccessPolicies, updateAccessPolicy } from '../../services/accessPoliciesService';
import { getClients } from '../../services/clientsService';
import { getExposedApis } from '../../services/exposedApisService';
import PolicyRuleModal from './PolicyRuleModal';
import PolicyRulesTable from './PolicyRulesTable';
import { splitPoliciesByType } from './accessPolicies.helpers';

export default function AccessPoliciesPage() {
  const [apis, setApis] = useState([]);
  const [clients, setClients] = useState([]);
  const [selectedApiId, setSelectedApiId] = useState('');
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [createModalType, setCreateModalType] = useState('');

  const selectedApi = useMemo(() => apis.find((api) => api.id === selectedApiId), [apis, selectedApiId]);
  const splitPolicies = useMemo(() => splitPoliciesByType(policies), [policies]);

  useEffect(() => {
    loadBaseData();
  }, []);

  useEffect(() => {
    if (selectedApiId) {
      loadPolicies(selectedApiId);
    } else {
      setPolicies([]);
    }
  }, [selectedApiId]);

  async function loadBaseData() {
    setLoading(true);
    setError('');
    try {
      const [apiData, clientData] = await Promise.all([getExposedApis(), getClients()]);
      setApis(apiData || []);
      setClients(clientData || []);
      setSelectedApiId((current) => current || apiData?.[0]?.id || '');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadPolicies(exposedApiId = selectedApiId) {
    if (!exposedApiId) return;
    setPolicyLoading(true);
    setError('');
    try {
      setPolicies(await getAccessPolicies(exposedApiId) || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setPolicyLoading(false);
    }
  }

  function upsertPolicy(saved) {
    setPolicies((current) => {
      const exists = current.some((policy) => policy.id === saved.id);
      return exists ? current.map((policy) => (policy.id === saved.id ? saved : policy)) : [saved, ...current];
    });
  }

  async function handleCreate(payload) {
    if (!selectedApiId) return;
    setBusyId(payload.type);
    setError('');
    try {
      upsertPolicy(await createAccessPolicy(selectedApiId, payload));
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setBusyId('');
    }
  }

  async function handleFlipType(policy) {
    const nextType = policy.type === 'WHITE' ? 'BLACK' : 'WHITE';
    setBusyId(policy.id);
    setError('');
    try {
      upsertPolicy(await updateAccessPolicy(selectedApiId, policy.id, {
        type: nextType,
        matchType: policy.matchType,
        matchValue: policy.matchValue,
        temporary: policy.temporary,
        expiresAt: policy.expiresAt,
      }));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(policy) {
    setBusyId(policy.id);
    setError('');
    try {
      await deleteAccessPolicy(selectedApiId, policy.id);
      setPolicies((current) => current.filter((item) => item.id !== policy.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  return (
    <div className="page-content access-policy-page">
      <div className="page-header">
        <div>
          <h1>Access Policies</h1>
          <p className="status-line"><span className="dot green" /> Whitelist: {splitPolicies.white.length} <span className="dot red" /> Blacklist: {splitPolicies.black.length}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadBaseData} disabled={loading}>⟳ Refresh APIs</Button>
          <Button variant="secondary" onClick={() => loadPolicies()} disabled={!selectedApiId || policyLoading}>⇩ Refresh Policies</Button>
        </div>
      </div>

      {error ? <div className="notice">{error}</div> : null}

      <section className="policy-hero-card">
        <div>
          <span className="policy-eyebrow">Selected Exposed API</span>
          <h2>{selectedApi?.name || 'Select an API'}</h2>
          <p>{selectedApi ? `${selectedApi.method || '-'} ${selectedApi.path || '-'} · ${selectedApi.microServiceName || 'unknown service'}` : 'Choose an API to manage WHITE and BLACK access policies.'}</p>
        </div>
        <label>
          <span>Exposed API</span>
          <select value={selectedApiId} onChange={(event) => setSelectedApiId(event.target.value)}>
            <option value="">Select exposed API</option>
            {apis.map((api) => <option key={api.id} value={api.id}>{api.name} · {api.method || '-'} {api.path || ''}</option>)}
          </select>
        </label>
        <div className="policy-hero-meta">
          <Badge tone={selectedApi?.enabled ? 'success' : 'neutral'} size="sm">{selectedApi?.enabled ? 'ENABLED' : 'NO API'}</Badge>
          <Badge tone="purple" size="sm">{selectedApi?.protocol || 'PROTOCOL'}</Badge>
        </div>
      </section>

      <div className="policy-board-grid">
        <section className="policy-board policy-board-black">
          <div className="policy-board-header">
            <div>
              <span className="policy-board-mark">BLACK</span>
              <h2>Blacklist</h2>
              <p>Matched callers are denied before allow rules.</p>
            </div>
            <div className="policy-board-actions">
              <strong>{splitPolicies.black.length}</strong>
              <Button disabled={!selectedApiId} onClick={() => setCreateModalType('BLACK')}>Add Blacklist</Button>
            </div>
          </div>
          <PolicyRulesTable policies={splitPolicies.black} clients={clients} emptyTitle="No blacklist rules" emptyDescription="Add CLIENT_ID, IP, or CIDR entries that should be blocked." busyId={busyId} onFlipType={handleFlipType} onDelete={handleDelete} />
        </section>

        <section className="policy-board policy-board-white">
          <div className="policy-board-header">
            <div>
              <span className="policy-board-mark">WHITE</span>
              <h2>Whitelist</h2>
              <p>Matched callers are allowed by policy.</p>
            </div>
            <div className="policy-board-actions">
              <strong>{splitPolicies.white.length}</strong>
              <Button disabled={!selectedApiId} onClick={() => setCreateModalType('WHITE')}>Add Whitelist</Button>
            </div>
          </div>
          <PolicyRulesTable policies={splitPolicies.white} clients={clients} emptyTitle="No whitelist rules" emptyDescription="Add CLIENT_ID, IP, or CIDR entries that should be allowed." busyId={busyId} onFlipType={handleFlipType} onDelete={handleDelete} />
        </section>
      </div>

      <div className="insight-grid">
        <StatCard icon="✓" label="Whitelist Rules" value={`${splitPolicies.white.length} rules`} tone="green" meta="visible allow-list for selected API" />
        <StatCard icon="!" label="Blacklist Rules" value={`${splitPolicies.black.length} rules`} tone="danger" meta="deny-list takes priority in runtime" />
      </div>
      {createModalType ? <PolicyRuleModal type={createModalType} clients={clients} saving={busyId === createModalType} onClose={() => setCreateModalType('')} onCreate={handleCreate} /> : null}
    </div>
  );
}
