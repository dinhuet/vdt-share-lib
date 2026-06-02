import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { disableApi, enableApi, getExposedApis, removeApi, updateExposedApiLimits, useDefaultConfig } from '../../services/exposedApisService';
import { SAMPLE_APIS } from '../../utils/constants';
import ExposedApiConfigModal from './ExposedApiConfigModal';
import ExposedApisFilters from './ExposedApisFilters';
import ExposedApisTable from './ExposedApisTable';
import { filterApis, getMicroservices } from './exposedApis.helpers';

export default function ExposedApisPage() {
  const [apis, setApis] = useState([]);
  const [filters, setFilters] = useState({ microServiceId: '', syncStatus: '', search: '' });
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [usingSampleData, setUsingSampleData] = useState(false);
  const [selectedApi, setSelectedApi] = useState(null);

  const displayedApis = useMemo(() => filterApis(apis, filters), [apis, filters]);
  const microservices = useMemo(() => getMicroservices(apis), [apis]);
  const counts = useMemo(() => ({
    active: apis.filter((api) => api.syncStatus === 'ACTIVE').length,
    stale: apis.filter((api) => api.syncStatus === 'STALE').length,
    removed: apis.filter((api) => api.syncStatus === 'REMOVED').length,
  }), [apis]);

  useEffect(() => {
    loadApis();
  }, []);

  async function loadApis() {
    setLoading(true);
    setError('');
    try {
      const data = await getExposedApis();
      setApis(data || []);
      setUsingSampleData(false);
    } catch (err) {
      setApis(SAMPLE_APIS);
      setUsingSampleData(true);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function updateFilters(next) {
    setFilters((current) => ({ ...current, ...next }));
  }

  function updateApiInList(updated) {
    setApis((current) => current.map((api) => (api.id === updated.id ? updated : api)));
    setSelectedApi((current) => (current?.id === updated.id ? updated : current));
  }

  async function handleToggleEnabled(api) {
    setBusyId(api.id);
    try {
      const updated = api.enabled ? await disableApi(api.id) : await enableApi(api.id);
      updateApiInList(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleResetDefault(api) {
    setBusyId(api.id);
    try {
      const updated = await useDefaultConfig(api.id);
      updateApiInList(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleRemove(api) {
    setBusyId(api.id);
    try {
      const updated = await removeApi(api.id);
      updateApiInList(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleSaveLimits(api, payload) {
    setBusyId(api.id);
    try {
      const updated = await updateExposedApiLimits(api.id, payload);
      updateApiInList(updated);
      setSelectedApi(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Exposed APIs</h1>
          <p className="status-line"><span className="dot green" /> Active: {counts.active} <span className="dot orange" /> Stale: {counts.stale} <span className="dot red" /> Removed: {counts.removed}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadApis} disabled={loading}>⟳ Refresh</Button>
          <Button disabled title="APIs are registered automatically from @SharedApi">⊕ Expose New API</Button>
        </div>
      </div>
      {usingSampleData ? <div className="notice">Showing sample data because backend is unavailable. {error}</div> : null}
      <ExposedApisFilters filters={filters} microservices={microservices} onChange={updateFilters} />
      <ExposedApisTable apis={displayedApis} busyId={busyId} onToggleEnabled={handleToggleEnabled} onConfigure={setSelectedApi} onResetDefault={handleResetDefault} onRemove={handleRemove} />
      <div className="table-footer"><span>Showing {displayedApis.length} of {apis.length} APIs</span><div><Button variant="ghost" disabled>Previous</Button><Button variant="ghost">Next</Button></div></div>
      <div className="insight-grid">
        <StatCard icon="✣" label="Drift Detected" value="12 APIs" tone="purple" meta="registered definitions may need sync" />
        <StatCard icon="▣" label="Global Policy Sync" value="Active" tone="blue" meta="JWT validation policy applied" />
        <StatCard icon="⌁" label="New Library Version" value="Available" tone="neutral" meta="Shared-lib update ready" />
      </div>
      {selectedApi ? <ExposedApiConfigModal api={selectedApi} saving={busyId === selectedApi.id} onClose={() => setSelectedApi(null)} onSave={handleSaveLimits} onResetDefault={handleResetDefault} /> : null}
    </div>
  );
}
