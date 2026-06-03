import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { deleteClientApi, disableClientApi, enableClientApi, getClientApis, updateClientApi, useDefaultClientApiConfig } from '../../services/clientApisService';
import { getExposedApis } from '../../services/exposedApisService';
import { SAMPLE_APIS, SAMPLE_CLIENT_APIS } from '../../utils/constants';
import { getMicroservices } from '../exposedApis/exposedApis.helpers';
import ClientApiModal from './ClientApiModal';
import ClientApisFilters from './ClientApisFilters';
import ClientApisTable from './ClientApisTable';
import { filterClientApis, getMicroservicesFromClientApis, mergeServiceOptions } from './clientApis.helpers';

export default function ClientApisPage() {
  const [clientApis, setClientApis] = useState([]);
  const [exposedApis, setExposedApis] = useState([]);
  const [filters, setFilters] = useState({ microServiceId: '', enabled: '', syncStatus: '', search: '' });
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [usingSampleData, setUsingSampleData] = useState(false);
  const [modal, setModal] = useState(null);

  const displayedClientApis = useMemo(() => filterClientApis(clientApis, filters), [clientApis, filters]);
  const microservices = useMemo(() => mergeServiceOptions(
    getMicroservicesFromClientApis(clientApis),
    getMicroservices(exposedApis),
  ), [clientApis, exposedApis]);
  const counts = useMemo(() => ({
    active: clientApis.filter((api) => api.syncStatus === 'ACTIVE').length,
    stale: clientApis.filter((api) => api.syncStatus === 'STALE').length,
    disabled: clientApis.filter((api) => api.syncStatus === 'ACTIVE' && !api.enabled).length,
  }), [clientApis]);

  useEffect(() => {
    loadClientApis();
  }, []);

  async function loadClientApis() {
    setLoading(true);
    setError('');
    try {
      const [clientApiData, exposedApiData] = await Promise.all([
        getClientApis(),
        getExposedApis(),
      ]);
      setClientApis(clientApiData || []);
      setExposedApis(exposedApiData || []);
      setUsingSampleData(false);
    } catch (err) {
      setClientApis(SAMPLE_CLIENT_APIS);
      setExposedApis(SAMPLE_APIS);
      setUsingSampleData(true);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function updateFilters(next) {
    setFilters((current) => ({ ...current, ...next }));
  }

  function upsertClientApiInList(saved) {
    setClientApis((current) => {
      const exists = current.some((api) => api.id === saved.id);
      return exists ? current.map((api) => (api.id === saved.id ? saved : api)) : [saved, ...current];
    });
  }

  async function handleSave(currentApi, payload) {
    setBusyId(currentApi?.id || 'new');
    try {
      const saved = await updateClientApi(currentApi.id, payload);
      upsertClientApiInList(saved);
      setModal(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleToggleEnabled(api) {
    setBusyId(api.id);
    try {
      const saved = api.enabled ? await disableClientApi(api.id) : await enableClientApi(api.id);
      upsertClientApiInList(saved);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(api) {
    setBusyId(api.id);
    try {
      await deleteClientApi(api.id);
      setClientApis((current) => current.filter((item) => item.id !== api.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleUseDefault(api) {
    setBusyId(api.id);
    try {
      const saved = await useDefaultClientApiConfig(api.id);
      upsertClientApiInList(saved);
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
          <h1>Client APIs</h1>
          <p className="status-line"><span className="dot green" /> Active: {counts.active} <span className="dot orange" /> Stale: {counts.stale} <span className="dot red" /> Disabled: {counts.disabled}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadClientApis} disabled={loading}>⟳ Refresh</Button>
        </div>
      </div>
      {usingSampleData ? <div className="notice">Showing sample data because backend is unavailable. {error}</div> : null}
      {error && !usingSampleData ? <div className="notice">{error}</div> : null}
      <ClientApisFilters filters={filters} microservices={microservices} onChange={updateFilters} />
      <section className="table-card">
        <div className="table-card-header">
          <h2>Outbound API Dependencies</h2>
          <div><Button variant="ghost" disabled={loading}>≡</Button><Button variant="ghost" onClick={loadClientApis}>⇩</Button></div>
        </div>
        <ClientApisTable clientApis={displayedClientApis} busyId={busyId} onEdit={(clientApi) => setModal({ mode: 'edit', clientApi })} onToggleEnabled={handleToggleEnabled} onUseDefault={handleUseDefault} onDelete={handleDelete} />
      </section>
      <div className="table-footer"><span>Showing {displayedClientApis.length} of {clientApis.length} client APIs</span><div><Button variant="ghost" disabled>Previous</Button><Button variant="ghost">Next</Button></div></div>
      <div className="insight-grid">
        <StatCard icon="⇄" label="Retry Protected" value={`${clientApis.filter((api) => api.syncStatus === 'ACTIVE' && api.maxRetries > 0).length} APIs`} tone="purple" meta="client calls configured with retry policy" />
        <StatCard icon="⌁" label="Low Latency Target" value={`${clientApis.filter((api) => api.syncStatus === 'ACTIVE' && api.latencyThresholdMs && api.latencyThresholdMs <= 100).length} APIs`} tone="blue" meta="outbound calls under 100ms threshold" />
        <StatCard icon="▤" label="Stale Records" value={`${counts.stale} APIs`} tone="danger" meta="can be deleted after admin review" />
      </div>
      {modal ? <ClientApiModal clientApi={modal.clientApi} serviceOptions={microservices} saving={Boolean(busyId)} onClose={() => setModal(null)} onSave={handleSave} /> : null}
    </div>
  );
}
