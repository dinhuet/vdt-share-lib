import SearchInput from '../../components/SearchInput';
import { ALERT_SEVERITIES, ALERT_STATUSES } from './securityAlerts.helpers';

export default function SecurityAlertsFilters({ filters, onChange }) {
  return (
    <section className="filter-card security-alert-filters">
      <label>
        <span>Status</span>
        <select value={filters.status} onChange={(event) => onChange({ status: event.target.value })}>
          <option value="">All Statuses</option>
          {ALERT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
        </select>
      </label>
      <label>
        <span>Severity</span>
        <select value={filters.severity} onChange={(event) => onChange({ severity: event.target.value })}>
          <option value="">All Severities</option>
          {ALERT_SEVERITIES.map((severity) => <option key={severity} value={severity}>{severity}</option>)}
        </select>
      </label>
      <label>
        <span>Service</span>
        <SearchInput value={filters.serviceName} onChange={(serviceName) => onChange({ serviceName })} placeholder="service-name" />
      </label>
      <label>
        <span>Rule Code</span>
        <SearchInput value={filters.ruleCode} onChange={(ruleCode) => onChange({ ruleCode })} placeholder="AUTH_BRUTE_FORCE" />
      </label>
      <label>
        <span>Endpoint ID</span>
        <SearchInput value={filters.endpointId} onChange={(endpointId) => onChange({ endpointId })} placeholder="endpoint id" />
      </label>
      <label>
        <span>Client ID</span>
        <SearchInput value={filters.clientId} onChange={(clientId) => onChange({ clientId })} placeholder="client id" />
      </label>
      <label>
        <span>Source IP</span>
        <SearchInput value={filters.sourceIp} onChange={(sourceIp) => onChange({ sourceIp })} placeholder="10.0.0.1" />
      </label>
    </section>
  );
}
