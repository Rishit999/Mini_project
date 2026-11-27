import { CheckCircle2, RefreshCw, Square } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorBanner,
  LoadingState,
  SuccessBanner,
} from '../components/common/Feedback';
import { StatusBadge, formatTime } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadSessions = useCallback(async () => {
    try {
      setError('');
      setSessions(await evApi.getSessions());
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSessions();
    const timer = setInterval(loadSessions, 15000);
    return () => clearInterval(timer);
  }, [loadSessions]);

  const handleComplete = async (sessionId) => {
    try {
      setError('');
      await evApi.completeSession(sessionId);
      setNotice(`Session ${sessionId} completed. Charger released and waiting queue re-evaluated.`);
      await loadSessions();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleStop = async (sessionId) => {
    try {
      setError('');
      await evApi.stopSession(sessionId);
      setNotice(`Session ${sessionId} stopped early. Charger released.`);
      await loadSessions();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading charging sessions..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          All energy consumption metrics below are <strong>simulated</strong> in software based on
          charger rating, efficiency factor, and elapsed session time.
        </p>
        <button type="button" className="btn btn-outline" onClick={loadSessions}>
          <RefreshCw size={15} /> Refresh
        </button>
      </div>

      {sessions.length === 0 ? (
        <EmptyState
          title="No Charging Sessions Yet"
          description="Submit a charging request or check in a reservation to begin a session."
        />
      ) : (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Session</th>
                  <th>Vehicle</th>
                  <th>Station & Charger</th>
                  <th>Start / Expected End</th>
                  <th>Allocated Power</th>
                  <th>Energy (Simulated)</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.sessionId}>
                    <td>
                      <div style={{ fontWeight: 700 }}>{s.sessionId}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>
                        {s.requestId || s.reservationId || 'Direct'}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{s.registrationNumber}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                        {s.vehicleModel}
                      </div>
                    </td>
                    <td>
                      <div>{s.stationName}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--teal)' }}>
                        {s.chargerName}
                      </div>
                    </td>
                    <td style={{ fontSize: '0.8rem' }}>
                      <div>Start: {formatTime(s.startTime)}</div>
                      <div style={{ color: 'var(--muted)' }}>
                        {s.actualEndTime
                          ? `Ended: ${formatTime(s.actualEndTime)}`
                          : `Exp: ${formatTime(s.expectedEndTime)}`}
                      </div>
                    </td>
                    <td>{s.allocatedPowerKw} kW</td>
                    <td>
                      <strong>{s.energyConsumedKwh.toFixed(2)} kWh</strong>
                      <div style={{ fontSize: '0.74rem', color: 'var(--muted)' }}>
                        {s.durationMinutes} min {s.simulated ? '• Simulated' : ''}
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={s.status} />
                    </td>
                    <td>
                      {s.status === 'ACTIVE' && (
                        <div className="row">
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => handleComplete(s.sessionId)}
                          >
                            <CheckCircle2 size={13} /> Complete
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => handleStop(s.sessionId)}
                          >
                            <Square size={13} /> Stop
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
