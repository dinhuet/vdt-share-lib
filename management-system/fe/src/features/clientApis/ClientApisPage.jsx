import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { createClientApi, deleteClientApi, disableClientApi, enableClientApi, getClientApis, restoreClientApi, updateClientApi } from '../../services/clientApisService';
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
  const [filters, setFilters] = useState({ microServiceId: '', enabled: '', deleted: 'active', search: '' });
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
    active: clientApis.filter((api) => !api.deleted && api.enabled).length,
    disabled: clientApis.filter((api) => !api.deleted && !api.enabled).length,
    deleted: clientApis.filter((api) => api.deleted).length,
  }), [clientApis]);

  useEffect(() => {
    loadClientApis();
  }, []);

  async function loadClientApis() {
    setLoading(true);
    setError('');
    try {
      const [clientApiData, exposedApiData] = await Promise.all([
        getClientApis({ includeDeleted: true }),
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
      const saved = currentApi ? await updateClientApi(currentApi.id, payload) : await createClientApi(payload);
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
      setClientApis((current) => current.map((item) => (item.id === api.id ? {
        ...item,
        deleted: true,
        deletedAt: new Date().toISOString(),
        enabled: false,
      } : item)));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleRestore(api) {
    setBusyId(api.id);
    try {
      const saved = await restoreClientApi(api.id);
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
          <p className="status-line"><span className="dot green" /> Active: {counts.active} <span className="dot orange" /> Disabled: {counts.disabled} <span className="dot red" /> Deleted: {counts.deleted}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadClientApis} disabled={loading}>⟳ Refresh</Button>
          <Button onClick={() => setModal({ mode: 'create' })}>⊕ Create Client API</Button>
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
        <ClientApisTable clientApis={displayedClientApis} busyId={busyId} onEdit={(clientApi) => setModal({ mode: 'edit', clientApi })} onToggleEnabled={handleToggleEnabled} onDelete={handleDelete} onRestore={handleRestore} />
      </section>
      <div className="table-footer"><span>Showing {displayedClientApis.length} of {clientApis.length} client APIs</span><div><Button variant="ghost" disabled>Previous</Button><Button variant="ghost">Next</Button></div></div>
      <div className="insight-grid">
        <StatCard icon="⇄" label="Retry Protected" value={`${clientApis.filter((api) => !api.deleted && api.maxRetries > 0).length} APIs`} tone="purple" meta="client calls configured with retry policy" />
        <StatCard icon="⌁" label="Low Latency Target" value={`${clientApis.filter((api) => !api.deleted && api.latencyThresholdMs && api.latencyThresholdMs <= 100).length} APIs`} tone="blue" meta="outbound calls under 100ms threshold" />
        <StatCard icon="▤" label="Soft Deleted" value={`${counts.deleted} APIs`} tone="danger" meta="available for restore from filters" />
      </div>
      {modal ? <ClientApiModal clientApi={modal.clientApi} serviceOptions={microservices} saving={Boolean(busyId)} onClose={() => setModal(null)} onSave={handleSave} /> : null}
    </div>
  );
}
