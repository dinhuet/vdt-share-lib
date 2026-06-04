import Badge from '../../components/Badge';
import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import { formatDateTime, formatRelativeTime } from '../../utils/date';
import { describePolicyTarget } from './accessPolicies.helpers';

const columns = ['Target', 'Match Type', 'Expiry', 'Created', 'Actions'];

export default function PolicyRulesTable({ policies, clients, emptyTitle, emptyDescription, busyId, onFlipType, onDelete }) {
  return (
    <DataTable
      columns={columns}
      rows={policies}
      emptyTitle={emptyTitle}
      emptyDescription={emptyDescription}
      renderRow={(policy) => (
        <tr key={policy.id}>
          <td><code>{describePolicyTarget(policy, clients)}</code></td>
          <td><Badge tone={policy.matchType === 'CLIENT_ID' ? 'purple' : 'info'} size="sm">{policy.matchType}</Badge></td>
          <td>{policy.expiresAt ? <span>{formatDateTime(policy.expiresAt)} {policy.temporary ? <Badge tone="warning" size="sm">TEMP</Badge> : null}</span> : <Badge tone="success" size="sm">NO EXPIRY</Badge>}</td>
          <td>{formatRelativeTime(policy.createdAt)}</td>
          <td>
            <div className="row-actions">
              <Button variant="ghost" disabled={busyId === policy.id} onClick={() => onFlipType(policy)}>{policy.type === 'WHITE' ? 'Move to Black' : 'Move to White'}</Button>
              <Button variant="danger-ghost" disabled={busyId === policy.id} onClick={() => onDelete(policy)}>Delete</Button>
            </div>
          </td>
        </tr>
      )}
    />
  );
}
