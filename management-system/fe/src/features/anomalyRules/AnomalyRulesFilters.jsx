import SearchInput from '../../components/SearchInput';
import { RULE_SEVERITIES, RULE_TYPES, SCOPE_TYPES } from './anomalyRules.helpers';

export default function AnomalyRulesFilters({ filters, onChange }) {
  return (
    <section className="filter-card">
      <label>
        <span>Search</span>
        <SearchInput value={filters.search} onChange={(search) => onChange({ search })} placeholder="rule, name, metric" />
      </label>
      <label>
        <span>Type</span>
        <select value={filters.ruleType} onChange={(event) => onChange({ ruleType: event.target.value })}>
          <option value="">All Types</option>
          {RULE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
        </select>
      </label>
      <label>
        <span>Severity</span>
        <select value={filters.severity} onChange={(event) => onChange({ severity: event.target.value })}>
          <option value="">All Severities</option>
          {RULE_SEVERITIES.map((severity) => <option key={severity} value={severity}>{severity}</option>)}
        </select>
      </label>
      <label>
        <span>Enabled</span>
        <select value={filters.enabled} onChange={(event) => onChange({ enabled: event.target.value })}>
          <option value="">All States</option>
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </select>
      </label>
      <label>
        <span>Scope</span>
        <select value={filters.scopeType} onChange={(event) => onChange({ scopeType: event.target.value })}>
          <option value="">All Scopes</option>
          {SCOPE_TYPES.map((scope) => <option key={scope} value={scope}>{scope}</option>)}
        </select>
      </label>
    </section>
  );
}
