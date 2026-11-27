import { ArrowLeft, Play, PlugZap, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ErrorBanner, LoadingState, SuccessBanner } from '../components/common/Feedback';
import { PriorityBadge, StatusBadge, formatWait } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function StationDetailPage() {
  const { stationId } = useParams();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadDetail = useCallback(async () => {
    try {
      setError('');
      const res = await evApi.getStationDetail(stationId);
      setDetail(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [stationId]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  const handleAllocateStation = async () => {
    try {
      setError('');
      const results = await evApi.runAllocation(stationId);
      const assigned = results.filter((r) => r.outcome === 'ASSIGNED').length;
      setNotice(`Station allocation complete: ${assigned} request(s) assigned.`);
      await loadDetail();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label={`Loading station ${stationId}...`} />;
  if (!detail) return <ErrorBanner message={error || 'Station not found.'} />;

  const { station, chargers, queue, configuration } = detail;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <div className="row">
          <Link to="/stations" className="btn btn-outline btn-sm">
            <ArrowLeft size={14} /> Back
          </Link>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>{station.name}</h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>
              {station.stationId} • {station.location}
            </p>
          </div>
          <StatusBadge status={station.status} />
        </div>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadDetail}>
            <RefreshCw size={15} /> Refresh
          </button>
          <button type="button" className="btn btn-primary" onClick={handleAllocateStation}>
            <Play size={15} /> Allocate Waiting Queue
          </button>
        </div>
      </div>

      {/* Power & Config Summary */}
      <div className="card">
        <div className="row-between" style={{ marginBottom: 10 }}>
          <div>
            <strong>Electrical Power Allocation</strong>
            <div style={{ fontSize: '0.84rem', color: 'var(--muted)' }}>
              Currently allocated {station.currentAllocatedPowerKw.toFixed(1)} kW of{' '}
              {station.maxPowerCapacityKw.toFixed(1)} kW maximum capacity
            </div>
          </div>
          <div style={{ fontWeight: 700, fontSize: '1.1rem' }}>
            {station.powerUtilizationPercent.toFixed(1)}%
          </div>
        </div>
        <div className="progress-track">
          <div
            className="progress-fill"
            style={{
              width: `${Math.min(100, station.powerUtilizationPercent)}%`,
              background: station.powerUtilizationPercent > 85 ? 'var(--red)' : 'var(--green)',
            }}
          />
        </div>
        <div className="row" style={{ marginTop: 14, fontSize: '0.8rem', color: 'var(--slate)' }}>
          <span>Grace Period: {configuration.reservationGracePeriodMinutes} min</span>
          <span>•</span>
          <span>Efficiency: {(configuration.chargingEfficiencyFactor * 100).toFixed(0)}%</span>
          <span>•</span>
          <span>
            Auto-Allocation: {configuration.autoAllocationEnabled ? 'Enabled' : 'Disabled'}
          </span>
        </div>
      </div>

      {/* Chargers Grid */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Station Chargers ({chargers.length})</h3>
        </div>
        <div className="grid-3">
          {chargers.map((ch) => (
            <div
              key={ch.chargerId}
              style={{
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
                padding: 14,
                background: 'var(--surface-alt)',
              }}
            >
              <div className="row-between" style={{ marginBottom: 8 }}>
                <div className="row" style={{ gap: 6 }}>
                  <PlugZap size={16} color="var(--teal)" />
                  <strong>{ch.name}</strong>
                </div>
                <StatusBadge status={ch.availabilityStatus} />
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--slate)' }}>
                {ch.chargerId} • {ch.chargerType} • <strong>{ch.powerRatingKw} kW</strong>
              </div>
              <div className="row" style={{ marginTop: 8, gap: 6 }}>
                {ch.supportedConnectors.map((conn) => (
                  <span key={conn} className="badge badge-slate">
                    {conn}
                  </span>
                ))}
                <StatusBadge status={ch.operationalStatus} />
              </div>
              {ch.currentVehicleId && (
                <div style={{ fontSize: '0.78rem', marginTop: 8, color: 'var(--teal)' }}>
                  Active Vehicle: <strong>{ch.currentVehicleId}</strong>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Station Queue */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Station Waiting Queue ({queue.length})</h3>
        </div>
        {queue.length === 0 ? (
          <p style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
            No vehicles waiting in queue for this station.
          </p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Pos</th>
                  <th>Vehicle</th>
                  <th>Connector</th>
                  <th>Priority</th>
                  <th>Est. Wait</th>
                  <th>Reason</th>
                </tr>
              </thead>
              <tbody>
                {queue.map((q) => (
                  <tr key={q.requestId}>
                    <td>#{q.queuePosition}</td>
                    <td>
                      <strong>{q.registrationNumber}</strong> ({q.currentBatteryPercent}%)
                    </td>
                    <td>{q.requiredConnector}</td>
                    <td>
                      <PriorityBadge score={q.priorityScore} />
                    </td>
                    <td>{formatWait(q.estimatedWaitMinutes)}</td>
                    <td style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>{q.statusReason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
