import SearchInput from '../../components/SearchInput';

export default function ClientApisFilters({ filters, microservices, onChange }) {
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
        <span>Enabled</span>
        <select value={filters.enabled} onChange={(event) => onChange({ enabled: event.target.value })}>
          <option value="">All</option>
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </select>
      </label>
      <label>
        <span>Deleted</span>
        <select value={filters.deleted} onChange={(event) => onChange({ deleted: event.target.value })}>
          <option value="active">Active only</option>
          <option value="include">Include deleted</option>
          <option value="deleted">Deleted only</option>
        </select>
      </label>
      <label className="filter-search">
        <span>Search Client API</span>
        <SearchInput value={filters.search} onChange={(search) => onChange({ search })} placeholder="Search by name, service, URL or protocol..." />
      </label>
    </section>
  );
}
