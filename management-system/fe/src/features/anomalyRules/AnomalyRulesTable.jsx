import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ToggleSwitch from '../../components/ToggleSwitch';

function severityTone(severity) {
  if (severity === 'CRITICAL') return 'danger';
  if (severity === 'HIGH') return 'warning';
  if (severity === 'MEDIUM') return 'info';
  return 'neutral';
}

export default function AnomalyRulesTable({ rules, busyId, onEdit, onToggle }) {
  return (
    <DataTable
      columns={['Rule code', 'Name', 'Type', 'Severity', 'Metric', 'Scope type', 'Enabled', 'Cooldown minutes', 'Actions']}
      rows={rules}
      emptyTitle="No anomaly rules"
      emptyDescription="Create a rule or adjust filters."
      renderRow={(rule) => (
        <tr key={rule.id}>
          <td><strong>{rule.ruleCode}</strong></td>
          <td>{rule.name}</td>
          <td><Badge tone="info">{rule.ruleType}</Badge></td>
          <td><Badge tone={severityTone(rule.severity)}>{rule.severity}</Badge></td>
          <td>{rule.metric}</td>
          <td>{rule.scopeType}</td>
          <td><ToggleSwitch checked={Boolean(rule.enabled)} disabled={busyId === rule.id} onChange={() => onToggle(rule)} label={`Toggle ${rule.ruleCode}`} /></td>
          <td>{rule.cooldownMinutes ?? 'Default'}</td>
          <td className="row-actions"><Button variant="ghost" disabled={busyId === rule.id} onClick={() => onEdit(rule)}>Edit</Button></td>
        </tr>
      )}
    />
  );
}
