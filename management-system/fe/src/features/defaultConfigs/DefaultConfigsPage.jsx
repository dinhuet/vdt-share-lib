import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import { getClientApis } from '../../services/clientApisService';
import { deleteDefaultConfig, getDefaultConfigs, upsertDefaultConfig } from '../../services/defaultConfigsService';
import { getExposedApis } from '../../services/exposedApisService';
import { SAMPLE_APIS, SAMPLE_CLIENT_APIS, SAMPLE_DEFAULT_CONFIGS } from '../../utils/constants';
import DefaultConfigsFilters from './DefaultConfigsFilters';
import DefaultConfigModal from './DefaultConfigModal';
import DefaultConfigStats from './DefaultConfigStats';
import DefaultConfigsTable from './DefaultConfigsTable';
import { filterDefaultConfigs, getServiceOptions } from './defaultConfigs.helpers';

export default function DefaultConfigsPage() {
  const [configs, setConfigs] = useState([]);
  const [apis, setApis] = useState([]);
  const [clientApis, setClientApis] = useState([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [usingSampleData, setUsingSampleData] = useState(false);
  const [modal, setModal] = useState(null);
  const [filters, setFilters] = useState({ apiType: '', scope: '', microServiceId: '', enabled: '', search: '' });

  const serviceOptions = useMemo(() => {
    const fromConfigs = getServiceOptions(configs);
    const fromApis = getServiceOptions(apis.map((api) => ({ microServiceId: api.microServiceId, microServiceName: api.microServiceName })));
    const fromClientApis = getServiceOptions(clientApis.map((api) => ({ microServiceId: api.microServiceId, microServiceName: api.microServiceName })));
    const map = new Map([...fromConfigs, ...fromApis, ...fromClientApis].map((service) => [service.id, service.name]));
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [configs, apis, clientApis]);

  const displayedConfigs = useMemo(() => filterDefaultConfigs(configs, filters), [configs, filters]);

  useEffect(() => {
    loadConfigs();
  }, []);

  async function loadConfigs() {
    setLoading(true);
    setError('');
    try {
      const [configData, apiData, clientApiData] = await Promise.all([getDefaultConfigs(), getExposedApis(), getClientApis()]);
      setConfigs(configData || []);
      setApis(apiData || []);
      setClientApis(clientApiData || []);
      setUsingSampleData(false);
    } catch (err) {
      setConfigs(SAMPLE_DEFAULT_CONFIGS);
      setApis(SAMPLE_APIS);
      setClientApis(SAMPLE_CLIENT_APIS);
      setUsingSampleData(true);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(payload) {
    setBusyId(modal?.config?.id || 'new');
    try {
      const saved = await upsertDefaultConfig(payload);
      setConfigs((current) => {
        const exists = current.some((config) => config.id === saved.id);
        return exists ? current.map((config) => (config.id === saved.id ? saved : config)) : [saved, ...current];
      });
      setModal(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleToggle(config) {
    setBusyId(config.id);
    try {
      const saved = await upsertDefaultConfig({
        ...config,
        enabled: !config.enabled,
        applyMode: 'NEW_ONLY',
      });
      setConfigs((current) => current.map((item) => (item.id === saved.id ? saved : item)));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(config) {
    setBusyId(config.id);
    try {
      await deleteDefaultConfig(config.id);
      setConfigs((current) => current.filter((item) => item.id !== config.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  function updateFilters(next) {
    setFilters((current) => ({ ...current, ...next }));
  }

  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h1>Default API Configs</h1>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={() => setModal({ mode: 'create-global' })}>◎ Create Global Default</Button>
          <Button onClick={() => setModal({ mode: 'create-service' })}>⊕ Create Service Default</Button>
        </div>
      </div>
      {usingSampleData ? <div className="notice">Showing sample data because backend is unavailable. {error}</div> : null}
      <DefaultConfigStats configs={configs} />
      <DefaultConfigsFilters filters={filters} serviceOptions={serviceOptions} onChange={updateFilters} />
      <section className="table-card">
        <div className="table-card-header">
          <h2>Active Default Configurations</h2>
          <div><Button variant="ghost" disabled={loading}>≡</Button><Button variant="ghost" onClick={loadConfigs}>⇩</Button></div>
        </div>
        <DefaultConfigsTable configs={displayedConfigs} busyId={busyId} onEdit={(config) => setModal({ mode: 'edit', config })} onToggle={handleToggle} onDelete={handleDelete} />
      </section>
      {modal ? <DefaultConfigModal mode={modal.mode} config={modal.config} serviceOptions={serviceOptions} saving={Boolean(busyId)} onClose={() => setModal(null)} onSave={handleSave} /> : null}
    </div>
  );
}
