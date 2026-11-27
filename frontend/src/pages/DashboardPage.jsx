import {
  Activity,
  ArrowRight,
  Clock,
  ListOrdered,
  Play,
  PlugZap,
  RefreshCw,
  Send,
  Zap,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ErrorBanner, LoadingState, StatCard, SuccessBanner } from '../components/common/Feedback';
import {
  PriorityBadge,
  StatusBadge,
  formatTime,
  formatWait,
} from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function DashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const loadDashboard = useCallback(async () => {
    try {
      setError('');
      const res = await evApi.getDashboard();
      setData(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
    const timer = setInterval(loadDashboard, 15000);
    return () => clearInterval(timer);
  }, [loadDashboard]);

  const handleRunAllocation = async () => {
    try {
      setBusy(true);
      setError('');
      const results = await evApi.runAllocation();
      const assignedCount = results.filter((r) => r.outcome === 'ASSIGNED').length;
      setNotice(`Allocation cycle completed: ${assignedCount} vehicle(s) assigned to chargers.`);
      await loadDashboard();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <LoadingState label="Loading operations dashboard..." />;
  if (!data) return <ErrorBanner message={error || 'Unable to load dashboard.'} />;

  const { metrics, stations, queuePreview, activeSessions, priorityDistribution } = data;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      {/* Quick Actions */}
      <div className="row-between">
        <div style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
          Last updated: {formatTime(data.timestamp)}
        </div>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadDashboard} disabled={busy}>
            <RefreshCw size={15} />
            Refresh
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleRunAllocation}
            disabled={busy}
          >
            <Play size={15} />
            Run Allocation Cycle
          </button>
          <Link to="/requests" className="btn btn-primary">
            <Send size={15} />
            New Charging Request
          </Link>
        </div>
      </div>

      {/* KPI Metrics */}
      <div className="grid-4">
        <StatCard
          label="Chargers Available"
          value={`${metrics.availableChargers} / ${metrics.totalChargers}`}
          helper={`${metrics.occupiedChargers} occupied • ${metrics.reservedChargers} reserved`}
          icon={PlugZap}
          accent="var(--green-dark)"
          softBg="var(--green-soft)"
        />
        <StatCard
          label="Queue Waiting"
          value={metrics.vehiclesWaiting}
          helper={`Avg Est. Wait: ${formatWait(metrics.averageEstimatedWaitMinutes)}`}
          icon={ListOrdered}
          accent="var(--amber)"
          softBg="var(--amber-soft)"
        />
        <StatCard
          label="Active Sessions"
          value={metrics.activeSessions}
          helper={`${metrics.activeOrConfirmedReservations} active/confirmed reservations`}
          icon={Activity}
          accent="var(--blue)"
          softBg="var(--blue-soft)"
        />
        <StatCard
          label="Station Power Load"
          value={`${metrics.powerUtilizationPercent.toFixed(1)}%`}
          helper={`${metrics.totalAllocatedPowerKw.toFixed(0)} / ${metrics.totalMaxPowerKw.toFixed(0)} kW`}
          icon={Zap}
          accent="var(--teal)"
          softBg="var(--teal-soft)"
        />
      </div>

      {/* Stations & Priority Chart */}
      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Station Power & Charger Status</h2>
              <p className="card-subtitle">Live load vs configured capacity</p>
            </div>
            <Link to="/stations" className="btn btn-outline btn-sm">
              All Stations <ArrowRight size={14} />
            </Link>
          </div>

          <div className="stack-md">
            {stations.map((st) => (
              <div
                key={st.stationId}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  padding: 14,
                }}
              >
                <div className="row-between" style={{ marginBottom: 8 }}>
                  <div>
                    <Link
                      to={`/stations/${st.stationId}`}
                      style={{ fontWeight: 700, color: 'var(--navy)' }}
                    >
                      {st.name}
                    </Link>
                    <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>{st.location}</div>
                  </div>
                  <StatusBadge status={st.status} />
                </div>

                <div className="row-between" style={{ fontSize: '0.8rem', marginBottom: 6 }}>
                  <span>
                    Power: <strong>{st.currentAllocatedPowerKw.toFixed(0)}</strong> /{' '}
                    {st.maxPowerCapacityKw.toFixed(0)} kW ({st.powerUtilizationPercent.toFixed(1)}%)
                  </span>
                  <span>
                    Bays: {st.availableChargers} free / {st.totalChargers} total •{' '}
                    <strong>{st.queuedVehicles}</strong> queued
                  </span>
                </div>

                <div className="progress-track">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${Math.min(100, st.powerUtilizationPercent)}%`,
                      background:
                        st.powerUtilizationPercent > 85
                          ? 'var(--red)'
                          : st.powerUtilizationPercent > 60
                            ? 'var(--amber)'
                            : 'var(--green)',
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Queue Priority Distribution</h2>
              <p className="card-subtitle">Score tiers across waiting vehicles (Max 100 pts)</p>
            </div>
            <Link to="/analytics" className="btn btn-outline btn-sm">
              Analytics <ArrowRight size={14} />
            </Link>
          </div>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={priorityDistribution}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="range" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" name="Queued Vehicles" fill="#0d9488" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Queue Preview & Active Sessions */}
      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Priority Queue Preview</h2>
              <p className="card-subtitle">Ordered by Custom Max Heap (Score → Arrival → ID)</p>
            </div>
            <Link to="/queue" className="btn btn-outline btn-sm">
              View Full Queue <ArrowRight size={14} />
            </Link>
          </div>

          {queuePreview.length === 0 ? (
            <p style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
              No vehicles currently waiting in the priority queue.
            </p>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Vehicle</th>
                    <th>Priority</th>
                    <th>Est. Wait</th>
                  </tr>
                </thead>
                <tbody>
                  {queuePreview.map((q) => (
                    <tr key={q.requestId}>
                      <td>
                        <strong>#{q.queuePosition}</strong>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{q.registrationNumber}</div>
                        <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                          {q.vehicleModel} • {q.currentBatteryPercent}% • {q.requiredConnector}
                        </div>
                      </td>
                      <td>
                        <PriorityBadge score={q.priorityScore} />
                        <div className="breakdown-chips">
                          <span className="breakdown-chip">
                            B:{q.priorityBreakdown?.batteryScore}
                          </span>
                          <span className="breakdown-chip">
                            D:{q.priorityBreakdown?.departureScore}
                          </span>
                          <span className="breakdown-chip">
                            W:{q.priorityBreakdown?.waitingTimeScore}
                          </span>
                        </div>
                      </td>
                      <td>
                        <div className="row" style={{ gap: 4 }}>
                          <Clock size={13} color="var(--muted)" />
                          <span>{formatWait(q.estimatedWaitMinutes)}</span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Active Charging Sessions</h2>
              <p className="card-subtitle">Live telemetry (Simulated EV power draw)</p>
            </div>
            <Link to="/sessions" className="btn btn-outline btn-sm">
              Manage Sessions <ArrowRight size={14} />
            </Link>
          </div>

          {activeSessions.length === 0 ? (
            <p style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
              No charging sessions currently active.
            </p>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Vehicle</th>
                    <th>Charger</th>
                    <th>Power / Energy (Sim)</th>
                    <th>Expected End</th>
                  </tr>
                </thead>
                <tbody>
                  {activeSessions.map((ses) => (
                    <tr key={ses.sessionId}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{ses.registrationNumber}</div>
                        <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                          {ses.stationName}
                        </div>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{ses.chargerName}</div>
                        <StatusBadge status={ses.status} />
                      </td>
                      <td>
                        <div>{ses.allocatedPowerKw} kW</div>
                        <div style={{ fontSize: '0.76rem', color: 'var(--teal)' }}>
                          {ses.energyConsumedKwh.toFixed(2)} kWh (Simulated)
                        </div>
                      </td>
                      <td style={{ fontSize: '0.82rem' }}>{formatTime(ses.expectedEndTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
