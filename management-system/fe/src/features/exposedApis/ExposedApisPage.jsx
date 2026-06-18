import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { deleteExposedApi, disableApi, enableApi, getExposedApis, updateExposedApiLimits, useDefaultConfig } from '../../services/exposedApisService';
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
    enabled: apis.filter((api) => api.enabled).length,
    defaultConfig: apis.filter((api) => api.useDefaultConfig).length,
    lowLatency: apis.filter((api) => {
      const latencyThreshold = Number(api.latencyThresholdMs);
      return api.enabled && api.syncStatus === 'ACTIVE' && api.latencyThresholdMs != null && Number.isFinite(latencyThreshold) && latencyThreshold <= 100;
    }).length,
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

  async function handleDelete(api) {
    setBusyId(api.id);
    try {
      await deleteExposedApi(api.id);
      setApis((current) => current.filter((item) => item.id !== api.id));
      setSelectedApi((current) => (current?.id === api.id ? null : current));
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
          <p className="status-line"><span className="dot green" /> Active: {counts.active} <span className="dot orange" /> Stale: {counts.stale}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadApis} disabled={loading}>⟳ Refresh</Button>
          <Button disabled title="APIs are registered automatically from @SharedApi">⊕ Expose New API</Button>
        </div>
      </div>
      {usingSampleData ? <div className="notice">Showing sample data because backend is unavailable. {error}</div> : null}
      <ExposedApisFilters filters={filters} microservices={microservices} onChange={updateFilters} />
      <ExposedApisTable apis={displayedApis} busyId={busyId} onToggleEnabled={handleToggleEnabled} onConfigure={setSelectedApi} onResetDefault={handleResetDefault} onDelete={handleDelete} />
      <div className="table-footer"><span>Showing {displayedApis.length} of {apis.length} APIs</span><div><Button variant="ghost" disabled>Previous</Button><Button variant="ghost">Next</Button></div></div>
      <div className="insight-grid">
        <StatCard icon="✓" label="Enabled APIs" value={`${counts.enabled} APIs`} tone="green" meta="available for shared API consumers" />
        <StatCard icon="☷" label="Default Config APIs" value={`${counts.defaultConfig} APIs`} tone="purple" meta="using the global default configuration" />
        <StatCard icon="⌁" label="Low Latency Target" value={`${counts.lowLatency} APIs`} tone="blue" meta="active enabled APIs at or below 100ms" />
      </div>
      {selectedApi ? <ExposedApiConfigModal api={selectedApi} saving={busyId === selectedApi.id} onClose={() => setSelectedApi(null)} onSave={handleSaveLimits} onResetDefault={handleResetDefault} /> : null}
    </div>
  );
}
