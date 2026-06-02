import SearchInput from '../../components/SearchInput';
import { SYNC_STATUSES } from '../../utils/constants';

export default function ExposedApisFilters({ filters, microservices, onChange }) {
  return (
    <section className="filter-card">
      <label>
        <span>Microservice</span>
        <select value={filters.microServiceId} onChange={(event) => onChange({ microServiceId: event.target.value })}>
          <option value="">All Services</option>
          {microservices.map((service) => <option key={service.id} value={service.id}>{service.name}</option>)}
        </select>
      </label>
      <label>
        <span>Sync Status</span>
        <select value={filters.syncStatus} onChange={(event) => onChange({ syncStatus: event.target.value })}>
          <option value="">All Statuses</option>
          {SYNC_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
        </select>
      </label>
      <label className="filter-search">
        <span>Search API</span>
        <SearchInput value={filters.search} onChange={(search) => onChange({ search })} placeholder="Search by API name or path (e.g. /v1/orders)..." />
      </label>
    </section>
  );
}
