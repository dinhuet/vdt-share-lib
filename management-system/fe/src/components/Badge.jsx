export default function Badge({ children, tone = 'neutral', size = 'md' }) {
  return <span className={`badge badge-${tone} badge-${size}`}>{children}</span>;
}

export function statusTone(status) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'STALE') return 'warning';
  return 'neutral';
}

export function methodTone(method) {
  if (method === 'GET') return 'success';
  if (method === 'POST') return 'info';
  if (method === 'PUT') return 'warning';
  if (method === 'DELETE') return 'danger';
  return 'neutral';
}
