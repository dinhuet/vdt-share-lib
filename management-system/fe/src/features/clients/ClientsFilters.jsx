import SearchInput from '../../components/SearchInput';
import { CLIENT_STATUSES } from '../../utils/constants';

export default function ClientsFilters({ filters, onChange }) {
  return (
    <section className="filter-card">
      <label>
        <span>Status</span>
        <select value={filters.status} onChange={(event) => onChange({ status: event.target.value })}>
          <option value="">All Statuses</option>
          {CLIENT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
        </select>
      </label>
      <label className="filter-search">
        <span>Search Clients</span>
        <SearchInput value={filters.search} onChange={(search) => onChange({ search })} placeholder="Search by name, client code, email..." />
      </label>
    </section>
  );
}
