import SearchInput from '../../components/SearchInput';
import { API_CONFIG_TYPES } from '../../utils/constants';

export default function DefaultConfigsFilters({ filters, serviceOptions, onChange }) {
  return (
    <section className="filter-card">
      <label>
        <span>API Type</span>
        <select value={filters.apiType} onChange={(event) => onChange({ apiType: event.target.value })}>
          <option value="">All Types</option>
          {API_CONFIG_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
        </select>
      </label>
      <label>
        <span>Scope</span>
        <select value={filters.scope} onChange={(event) => onChange({ scope: event.target.value })}>
          <option value="">All Scopes</option>
          <option value="GLOBAL">GLOBAL</option>
          <option value="SERVICE">SERVICE</option>
        </select>
      </label>
      <label>
        <span>Microservice</span>
        <select value={filters.microServiceId} onChange={(event) => onChange({ microServiceId: event.target.value })}>
          <option value="">All Services</option>
          {serviceOptions.map((service) => <option key={service.id} value={service.id}>{service.name}</option>)}
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
      <label className="filter-search">
        <span>Search Default</span>
        <SearchInput value={filters.search} onChange={(search) => onChange({ search })} placeholder="Search by service, failure action, notification rule..." />
      </label>
    </section>
  );
}
