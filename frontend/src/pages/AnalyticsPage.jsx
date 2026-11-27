import { RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ErrorBanner, LoadingState, StatCard } from '../components/common/Feedback';
import { StatusBadge } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAnalytics = useCallback(async () => {
    try {
      setError('');
      const [analyticsRes, auditRes] = await Promise.all([
        evApi.getAnalytics(),
        evApi.getAuditLogs(),
      ]);
      setData(analyticsRes);
      setLogs(auditRes.slice(0, 10));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAnalytics();
  }, [loadAnalytics]);

  if (loading) return <LoadingState label="Computing analytics..." />;
  if (!data) return <ErrorBanner message={error || 'Unable to load analytics.'} />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />

      <div className="row-between">
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          Operational metrics derived directly from Engine 1 persistence and Engine 2 scheduling
          logs.
        </p>
        <button type="button" className="btn btn-outline" onClick={loadAnalytics}>
          <RefreshCw size={15} /> Refresh
        </button>
      </div>

      <div className="grid-4">
        <StatCard
          label="Total Requests"
          value={data.totalRequests}
          helper={`${data.queuedRequests} queued • ${data.completedRequests} done`}
        />
        <StatCard
          label="Total Sessions"
          value={data.totalSessions}
          helper={`${data.completedSessions} completed • ${data.stoppedSessions} stopped`}
        />
        <StatCard
          label="Simulated Energy"
          value={`${data.totalSimulatedEnergyKwh.toFixed(1)} kWh`}
          helper="Software-simulated delivery"
        />
        <StatCard
          label="Avg Session / Wait"
          value={`${data.averageSessionDurationMinutes.toFixed(0)}m / ${data.averageQueueWaitMinutes.toFixed(0)}m`}
          helper="Duration vs Est. queue wait"
        />
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Station Power Utilization (%)</h2>
          </div>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={data.stationUtilization}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar
                  dataKey="powerUtilizationPercent"
                  name="Utilization (%)"
                  fill="#10b981"
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Queue Priority Breakdown</h2>
          </div>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={data.priorityDistribution}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="range" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" name="Requests" fill="#0f172a" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Charger Utilization Summary</h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Charger</th>
                <th>Station</th>
                <th>Rating</th>
                <th>Sessions</th>
                <th>Simulated Energy</th>
                <th>Current State</th>
              </tr>
            </thead>
            <tbody>
              {data.chargerUsage.map((ch) => (
                <tr key={ch.chargerId}>
                  <td>
                    <strong>{ch.chargerName}</strong> ({ch.chargerId})
                  </td>
                  <td>{ch.stationName}</td>
                  <td>{ch.powerRatingKw} kW</td>
                  <td>{ch.sessionCount}</td>
                  <td>{ch.totalSimulatedEnergyKwh.toFixed(2)} kWh</td>
                  <td>
                    <StatusBadge status={ch.currentAvailability} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Recent System Audit Events</h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Category</th>
                <th>Action</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.logId}>
                  <td style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td>
                    <span className="badge badge-slate">{log.category}</span>
                  </td>
                  <td>
                    <strong>{log.action}</strong>
                  </td>
                  <td style={{ fontSize: '0.85rem' }}>{log.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
