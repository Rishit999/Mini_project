import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';

export function LoadingState({ label = 'Loading data...' }) {
  return (
    <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14, padding: 28 }}>
      <div className="spinner" />
      <span style={{ color: 'var(--muted)', fontWeight: 500 }}>{label}</span>
    </div>
  );
}

export function ErrorBanner({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div className="alert alert-error" role="alert">
      <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 2 }} />
      <div style={{ flex: 1 }}>{message}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
          aria-label="Dismiss error"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
}

export function SuccessBanner({ message, onDismiss }) {
  if (!message) return null;
  return (
    <output className="alert alert-success">
      <CheckCircle2 size={18} style={{ flexShrink: 0, marginTop: 2 }} />
      <div style={{ flex: 1 }}>{message}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
          aria-label="Dismiss notification"
        >
          <X size={16} />
        </button>
      )}
    </output>
  );
}

export function EmptyState({ title, description, action }) {
  return (
    <div
      className="card"
      style={{ textAlign: 'center', padding: '40px 24px', color: 'var(--muted)' }}
    >
      <Info size={28} style={{ margin: '0 auto 10px', opacity: 0.7 }} />
      <h3 style={{ color: 'var(--navy)', marginBottom: 6, fontSize: '1rem' }}>{title}</h3>
      <p style={{ fontSize: '0.88rem', marginBottom: action ? 16 : 0 }}>{description}</p>
      {action}
    </div>
  );
}

export function StatCard({
  label,
  value,
  helper,
  icon: Icon,
  accent = 'var(--green)',
  softBg = 'var(--green-soft)',
}) {
  return (
    <div className="stat-card">
      <div>
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
        {helper && <div className="stat-helper">{helper}</div>}
      </div>
      {Icon && (
        <div className="stat-icon" style={{ background: softBg, color: accent }}>
          <Icon size={20} />
        </div>
      )}
    </div>
  );
}
