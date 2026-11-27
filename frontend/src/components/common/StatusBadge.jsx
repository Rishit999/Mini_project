const STATUS_CLASS = {
  OPERATIONAL: 'badge-green',
  AVAILABLE: 'badge-green',
  ASSIGNED: 'badge-teal',
  ACTIVE: 'badge-green',
  CHARGING: 'badge-green',
  COMPLETED: 'badge-blue',
  CONFIRMED: 'badge-teal',
  QUEUED: 'badge-amber',
  WAITING: 'badge-amber',
  RESERVED: 'badge-purple',
  OCCUPIED: 'badge-blue',
  MAINTENANCE: 'badge-amber',
  OUT_OF_SERVICE: 'badge-red',
  OFFLINE: 'badge-red',
  UNAVAILABLE: 'badge-red',
  CANCELLED: 'badge-slate',
  EXPIRED: 'badge-slate',
  REJECTED: 'badge-red',
  STOPPED: 'badge-amber',
};

export function StatusBadge({ status }) {
  const cls = STATUS_CLASS[status] || 'badge-slate';
  return <span className={`badge ${cls}`}>{status || 'UNKNOWN'}</span>;
}

export function PriorityBadge({ score }) {
  let cls = 'badge-slate';
  if (score >= 70) cls = 'badge-red';
  else if (score >= 45) cls = 'badge-amber';
  else if (score >= 25) cls = 'badge-teal';

  return <span className={`badge ${cls}`}>Priority {score} / 100</span>;
}

export function formatTime(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatWait(minutes) {
  if (minutes === null || minutes === undefined) return '—';
  if (minutes < 0) return 'Unavailable';
  if (minutes === 0) return '0 min (Ready)';
  return `~${Math.round(minutes)} min (Est.)`;
}
