import { useEffect, useMemo, useState } from 'react';
import Button from '../../components/Button';
import StatCard from '../../components/StatCard';
import { activateClient, createClient, deactivateClient, deleteClient, getClients, revokeClient, updateClient } from '../../services/clientsService';
import ClientCredentialsModal from './ClientCredentialsModal';
import ClientModal from './ClientModal';
import ClientPermissionsModal from './ClientPermissionsModal';
import ClientsFilters from './ClientsFilters';
import ClientsTable from './ClientsTable';
import RevokeClientModal from './RevokeClientModal';
import { filterClients } from './clients.helpers';

export default function ClientsPage() {
  const [clients, setClients] = useState([]);
  const [filters, setFilters] = useState({ status: '', search: '' });
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);

  const displayedClients = useMemo(() => filterClients(clients, filters), [clients, filters]);
  const counts = useMemo(() => ({
    active: clients.filter((client) => client.status === 'ACTIVE').length,
    inactive: clients.filter((client) => client.status === 'INACTIVE').length,
    revoked: clients.filter((client) => client.status === 'REVOKED').length,
  }), [clients]);

  useEffect(() => {
    loadClients();
  }, []);

  async function loadClients() {
    setLoading(true);
    setError('');
    try {
      const data = await getClients({ status: filters.status });
      setClients(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function updateFilters(next) {
    setFilters((current) => ({ ...current, ...next }));
  }

  function upsertClientInList(saved) {
    setClients((current) => {
      const exists = current.some((client) => client.id === saved.id);
      return exists ? current.map((client) => (client.id === saved.id ? saved : client)) : [saved, ...current];
    });
  }

  async function handleSave(currentClient, payload) {
    setBusyId(currentClient?.id || 'new');
    setError('');
    try {
      const saved = currentClient?.id ? await updateClient(currentClient.id, payload) : await createClient(payload);
      upsertClientInList(saved);
      setModal(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleActivate(client) {
    setBusyId(client.id);
    setError('');
    try {
      upsertClientInList(await activateClient(client.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDeactivate(client) {
    setBusyId(client.id);
    setError('');
    try {
      upsertClientInList(await deactivateClient(client.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleRevoke(client, reason) {
    setBusyId(client.id);
    setError('');
    try {
      upsertClientInList(await revokeClient(client.id, reason));
      setModal(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId('');
    }
  }

  async function handleDelete(client) {
    setBusyId(client.id);
    setError('');
    try {
      await deleteClient(client.id);
      setClients((current) => current.filter((item) => item.id !== client.id));
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
          <h1>Clients</h1>
          <p className="status-line"><span className="dot green" /> Active: {counts.active} <span className="dot orange" /> Inactive: {counts.inactive} <span className="dot red" /> Revoked: {counts.revoked}</p>
        </div>
        <div className="header-actions">
          <Button variant="secondary" onClick={loadClients} disabled={loading}>⟳ Refresh</Button>
          <Button onClick={() => setModal({ mode: 'create' })}>Create Client</Button>
        </div>
      </div>
      {error ? <div className="notice">{error}</div> : null}
      <ClientsFilters filters={filters} onChange={updateFilters} />
      <section className="table-card">
        <div className="table-card-header">
          <h2>Inbound Clients</h2>
          <div><Button variant="ghost" disabled={loading}>≡</Button><Button variant="ghost" onClick={loadClients}>⇩</Button></div>
        </div>
        <ClientsTable clients={displayedClients} busyId={busyId} onEdit={(client) => setModal({ mode: 'edit', client })} onCredentials={(client) => setModal({ mode: 'credentials', client })} onPermissions={(client) => setModal({ mode: 'permissions', client })} onActivate={handleActivate} onDeactivate={handleDeactivate} onRevoke={(client) => setModal({ mode: 'revoke', client })} onDelete={handleDelete} />
      </section>
      <div className="table-footer"><span>Showing {displayedClients.length} of {clients.length} clients</span><div><Button variant="ghost" disabled>Previous</Button><Button variant="ghost">Next</Button></div></div>
      <div className="insight-grid">
        <StatCard icon="♣" label="Active Clients" value={`${counts.active} clients`} tone="green" meta="can authenticate after credentials are generated" />
        <StatCard icon="◌" label="Inactive Clients" value={`${counts.inactive} clients`} tone="warning" meta="temporarily disabled by admins" />
        <StatCard icon="×" label="Revoked Clients" value={`${counts.revoked} clients`} tone="danger" meta="permanently revoked in this phase" />
      </div>
      {modal?.mode === 'create' || modal?.mode === 'edit' ? <ClientModal client={modal.client} saving={Boolean(busyId)} onClose={() => setModal(null)} onSave={handleSave} /> : null}
      {modal?.mode === 'revoke' ? <RevokeClientModal client={modal.client} saving={Boolean(busyId)} onClose={() => setModal(null)} onConfirm={handleRevoke} /> : null}
      {modal?.mode === 'credentials' ? <ClientCredentialsModal client={modal.client} onClose={() => setModal(null)} /> : null}
      {modal?.mode === 'permissions' ? <ClientPermissionsModal client={modal.client} onClose={() => setModal(null)} /> : null}
    </div>
  );
}
