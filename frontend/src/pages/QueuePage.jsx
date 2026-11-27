import { Clock, Play, RefreshCw, XCircle } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorBanner,
  LoadingState,
  SuccessBanner,
} from '../components/common/Feedback';
import {
  PriorityBadge,
  StatusBadge,
  formatTime,
  formatWait,
} from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function QueuePage() {
  const [queue, setQueue] = useState([]);
  const [stations, setStations] = useState([]);
  const [stationFilter, setStationFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadQueue = useCallback(async () => {
    try {
      setError('');
      const [q, st] = await Promise.all([
        evApi.getQueue(stationFilter || undefined),
        evApi.getStations(),
      ]);
      setQueue(q);
      setStations(st);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [stationFilter]);

  useEffect(() => {
    loadQueue();
    const timer = setInterval(loadQueue, 10000);
    return () => clearInterval(timer);
  }, [loadQueue]);

  const handleAllocate = async () => {
    try {
      setError('');
      const res = await evApi.runAllocation(stationFilter || undefined);
      const assigned = res.filter((r) => r.outcome === 'ASSIGNED').length;
      setNotice(`Priority allocation cycle complete: ${assigned} request(s) assigned.`);
      await loadQueue();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleCancel = async (requestId) => {
    if (!window.confirm(`Cancel queued request ${requestId}?`)) return;
    try {
      setError('');
      await evApi.cancelRequest(requestId);
      setNotice(`Request ${requestId} cancelled and removed from priority heap.`);
      await loadQueue();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading live priority queue..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="alert alert-info" style={{ marginBottom: 0 }}>
        <div>
          <strong>Custom Max Heap Ordering:</strong> Vehicles are ordered dynamically by{' '}
          <strong>Total Score (0–100)</strong> = Battery (max 50) + Departure Urgency (max 30) +
          Waiting Time (max 20), tie-broken by earlier arrival then Request ID. Estimated waits are
          non-guaranteed projections.
        </div>
      </div>

      <div className="row-between">
        <div className="row">
          <select
            className="form-select"
            style={{ width: 240 }}
            value={stationFilter}
            onChange={(e) => setStationFilter(e.target.value)}
            aria-label="Filter queue by station"
          >
            <option value="">All Stations</option>
            {stations.map((s) => (
              <option key={s.stationId} value={s.stationId}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadQueue}>
            <RefreshCw size={15} /> Refresh Queue
          </button>
          <button type="button" className="btn btn-primary" onClick={handleAllocate}>
            <Play size={15} /> Run Priority Allocation
          </button>
        </div>
      </div>

      {queue.length === 0 ? (
        <EmptyState
          title="Priority Queue is Empty"
          description="All eligible vehicles have been allocated or no charging requests are waiting."
        />
      ) : (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Vehicle & Request</th>
                  <th>Station & Connector</th>
                  <th>Priority Score Breakdown</th>
                  <th>Arrival / Departure</th>
                  <th>Est. Wait (Non-Guaranteed)</th>
                  <th>Status / Reason</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {queue.map((q) => (
                  <tr key={q.requestId}>
                    <td>
                      <span
                        style={{
                          display: 'inline-grid',
                          placeItems: 'center',
                          width: 30,
                          height: 30,
                          borderRadius: '50%',
                          background: 'var(--navy)',
                          color: 'white',
                          fontWeight: 700,
                          fontSize: '0.82rem',
                        }}
                      >
                        {q.queuePosition}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 700 }}>{q.registrationNumber}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                        {q.vehicleModel} • {q.currentBatteryPercent}% SoC
                      </div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--slate)' }}>
                        {q.requestId}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{q.stationName}</div>
                      <span className="badge badge-teal">{q.requiredConnector}</span>{' '}
                      <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                        {q.expectedDurationMinutes}m
                      </span>
                    </td>
                    <td>
                      <PriorityBadge score={q.priorityScore} />
                      <div className="breakdown-chips">
                        <span className="breakdown-chip">
                          Battery: {q.priorityBreakdown?.batteryScore ?? 0}/50
                        </span>
                        <span className="breakdown-chip">
                          Departure: {q.priorityBreakdown?.departureScore ?? 0}/30
                        </span>
                        <span className="breakdown-chip">
                          Wait: {q.priorityBreakdown?.waitingTimeScore ?? 0}/20
                        </span>
                      </div>
                    </td>
                    <td style={{ fontSize: '0.8rem' }}>
                      <div>In: {formatTime(q.arrivalTime)}</div>
                      <div style={{ color: 'var(--muted)' }}>
                        Dep: {formatTime(q.desiredDepartureTime)}
                      </div>
                    </td>
                    <td>
                      <div className="row" style={{ gap: 5 }}>
                        <Clock size={14} color="var(--muted)" />
                        <span>{formatWait(q.estimatedWaitMinutes)}</span>
                      </div>
                    </td>
                    <td style={{ maxWidth: 230 }}>
                      <StatusBadge status={q.status} />
                      <div style={{ fontSize: '0.76rem', color: 'var(--muted)', marginTop: 4 }}>
                        {q.statusReason}
                      </div>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => handleCancel(q.requestId)}
                      >
                        <XCircle size={13} /> Cancel
                      </button>
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
