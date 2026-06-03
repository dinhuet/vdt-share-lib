import StatCard from '../../components/StatCard';
import { formatRelativeTime } from '../../utils/date';

export default function DefaultConfigStats({ configs }) {
  const serviceConfigs = configs.filter((config) => config.scope === 'SERVICE' && config.enabled).length;
  const globalConfigs = configs.filter((config) => config.scope === 'GLOBAL').length;
  const clientConfigs = configs.filter((config) => config.apiType === 'CLIENT').length;
  const latest = configs
    .map((config) => config.updatedAt || config.createdAt)
    .filter(Boolean)
    .sort((a, b) => new Date(b) - new Date(a))[0];

  return (
    <div className="stats-grid">
      <StatCard icon="☷" label="Active Service Configs" value={String(serviceConfigs).padStart(2, '0')} tone="purple" />
      <StatCard icon="ϟ" label="Global Baselines" value={String(globalConfigs).padStart(2, '0')} tone="neutral" />
      <StatCard icon="⇄" label="Client Defaults" value={String(clientConfigs).padStart(2, '0')} tone="blue" />
      <StatCard icon="↻" label="Last Batch Update" value={latest ? formatRelativeTime(latest) : '-'} tone="blue" />
    </div>
  );
}
