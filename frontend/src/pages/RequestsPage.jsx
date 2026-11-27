import { Calculator, CheckCircle2, Clock, Send, Zap } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorBanner, LoadingState } from '../components/common/Feedback';
import { PriorityBadge, StatusBadge, formatWait } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function RequestsPage() {
  const [vehicles, setVehicles] = useState([]);
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [waitPreview, setWaitPreview] = useState(null);

  const [form, setForm] = useState({
    vehicleId: '',
    stationId: '',
    currentBatteryPercent: 20,
    targetBatteryPercent: 85,
    requiredConnector: 'CCS',
    expectedDurationMinutes: 40,
    desiredDepartureLocal: '',
  });

  const loadRefs = useCallback(async () => {
    try {
      setError('');
      const [vList, sList] = await Promise.all([evApi.getVehicles(), evApi.getStations()]);
      setVehicles(vList);
      setStations(sList);
      if (vList.length > 0 && sList.length > 0) {
        const firstV = vList[0];
        setForm((prev) => ({
          ...prev,
          vehicleId: prev.vehicleId || firstV.vehicleId,
          stationId: prev.stationId || sList[0].stationId,
          currentBatteryPercent: firstV.currentBatteryPercent,
          requiredConnector: firstV.connectorType,
        }));
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRefs();
  }, [loadRefs]);

  const handleVehicleChange = (vehicleId) => {
    const selected = vehicles.find((v) => v.vehicleId === vehicleId);
    if (selected) {
      setForm((prev) => ({
        ...prev,
        vehicleId,
        currentBatteryPercent: selected.currentBatteryPercent,
        requiredConnector: selected.connectorType,
      }));
    } else {
      setForm((prev) => ({ ...prev, vehicleId }));
    }
  };

  const handlePreviewWait = async () => {
    try {
      setError('');
      const params = {
        stationId: form.stationId,
        connectorType: form.requiredConnector,
        currentBatteryPercent: Number(form.currentBatteryPercent),
        expectedDurationMinutes: Number(form.expectedDurationMinutes),
      };
      if (form.desiredDepartureLocal) {
        params.desiredDepartureTime = new Date(form.desiredDepartureLocal).toISOString();
      }
      const res = await evApi.estimateWaitTime(params);
      setWaitPreview(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      setError('');
      setResult(null);
      const payload = {
        vehicleId: form.vehicleId,
        stationId: form.stationId,
        currentBatteryPercent: Number(form.currentBatteryPercent),
        targetBatteryPercent: form.targetBatteryPercent ? Number(form.targetBatteryPercent) : null,
        requiredConnector: form.requiredConnector,
        expectedDurationMinutes: Number(form.expectedDurationMinutes),
        desiredDepartureTime: form.desiredDepartureLocal
          ? new Date(form.desiredDepartureLocal).toISOString()
          : null,
      };
      const allocationResult = await evApi.submitRequest(payload);
      setResult(allocationResult);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingState label="Loading vehicles and stations..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />

      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Submit Charging Request</h2>
              <p className="card-subtitle">
                Engine 2 scores urgency (0–100) and allocates a compatible bay or queues the EV.
              </p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="stack-md">
            <div className="form-grid">
              <div className="form-field full">
                <label className="form-label" htmlFor="req-veh">
                  Select Vehicle
                </label>
                <select
                  id="req-veh"
                  className="form-select"
                  required
                  value={form.vehicleId}
                  onChange={(e) => handleVehicleChange(e.target.value)}
                >
                  {vehicles.map((v) => (
                    <option key={v.vehicleId} value={v.vehicleId}>
                      {v.registrationNumber} — {v.model} ({v.connectorType},{' '}
                      {v.currentBatteryPercent}%)
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field full">
                <label className="form-label" htmlFor="req-stn">
                  Target Charging Station
                </label>
                <select
                  id="req-stn"
                  className="form-select"
                  required
                  value={form.stationId}
                  onChange={(e) => setForm({ ...form, stationId: e.target.value })}
                >
                  {stations.map((s) => (
                    <option key={s.stationId} value={s.stationId}>
                      {s.name} ({s.status} • {s.availableChargers}/{s.totalChargers} bays free)
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label className="form-label" htmlFor="req-batt">
                  Current Battery (%)
                </label>
                <input
                  id="req-batt"
                  type="number"
                  min="0"
                  max="100"
                  step="1"
                  className="form-input"
                  required
                  value={form.currentBatteryPercent}
                  onChange={(e) => setForm({ ...form, currentBatteryPercent: e.target.value })}
                />
              </div>

              <div className="form-field">
                <label className="form-label" htmlFor="req-target">
                  Target Battery (%)
                </label>
                <input
                  id="req-target"
                  type="number"
                  min="1"
                  max="100"
                  step="1"
                  className="form-input"
                  value={form.targetBatteryPercent}
                  onChange={(e) => setForm({ ...form, targetBatteryPercent: e.target.value })}
                />
              </div>

              <div className="form-field">
                <label className="form-label" htmlFor="req-conn">
                  Required Connector
                </label>
                <select
                  id="req-conn"
                  className="form-select"
                  value={form.requiredConnector}
                  onChange={(e) => setForm({ ...form, requiredConnector: e.target.value })}
                >
                  <option value="CCS">CCS</option>
                  <option value="TYPE2">TYPE2</option>
                  <option value="CHADEMO">CHADEMO</option>
                  <option value="NACS">NACS</option>
                </select>
              </div>

              <div className="form-field">
                <label className="form-label" htmlFor="req-dur">
                  Expected Duration (min)
                </label>
                <input
                  id="req-dur"
                  type="number"
                  min="5"
                  max="480"
                  className="form-input"
                  required
                  value={form.expectedDurationMinutes}
                  onChange={(e) => setForm({ ...form, expectedDurationMinutes: e.target.value })}
                />
              </div>

              <div className="form-field full">
                <label className="form-label" htmlFor="req-dep">
                  Desired Departure Time (Optional)
                </label>
                <input
                  id="req-dep"
                  type="datetime-local"
                  className="form-input"
                  value={form.desiredDepartureLocal}
                  onChange={(e) => setForm({ ...form, desiredDepartureLocal: e.target.value })}
                />
                <span className="form-hint">
                  Providing a departure time within 4 hours increases Departure Urgency points (max
                  30).
                </span>
              </div>
            </div>

            <div className="row" style={{ justifyContent: 'space-between' }}>
              <button type="button" className="btn btn-outline" onClick={handlePreviewWait}>
                <Calculator size={15} /> Estimate Wait Time
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                <Send size={15} /> {submitting ? 'Processing...' : 'Submit & Allocate'}
              </button>
            </div>
          </form>

          {waitPreview && (
            <div className="alert alert-info" style={{ marginTop: 16, marginBottom: 0 }}>
              <Clock size={18} />
              <div>
                <strong>Projected Wait:</strong> {formatWait(waitPreview.estimatedWaitMinutes)}
                <div style={{ fontSize: '0.78rem', marginTop: 2 }}>{waitPreview.note}</div>
              </div>
            </div>
          )}
        </div>

        {/* Allocation Decision Output */}
        <div className="card">
          <div className="card-header">
            <div>
              <h2 className="card-title">Decision Support Output</h2>
              <p className="card-subtitle">Real-time Engine 2 evaluation result</p>
            </div>
          </div>

          {!result ? (
            <div style={{ padding: '24px 0', color: 'var(--muted)', fontSize: '0.9rem' }}>
              <p style={{ marginBottom: 12 }}>Submit a charging request on the left to inspect:</p>
              <ul style={{ paddingLeft: 20, lineHeight: 1.8 }}>
                <li>Priority score calculation (Battery + Departure + Wait = max 100)</li>
                <li>Immediate charger assignment or heap queue placement</li>
                <li>Station electrical power check & reservation conflict resolution</li>
                <li>Explainable wait-time projection</li>
              </ul>
            </div>
          ) : (
            <div className="stack-md">
              <div className="row-between">
                <StatusBadge status={result.outcome} />
                <PriorityBadge score={result.priorityBreakdown?.totalScore ?? 0} />
              </div>

              <div
                style={{
                  background: 'var(--surface-alt)',
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  padding: 14,
                }}
              >
                <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>Decision Reason</div>
                <div style={{ fontWeight: 600, marginTop: 4 }}>{result.reason}</div>
              </div>

              {result.priorityBreakdown && (
                <div className="grid-3">
                  <div className="card" style={{ padding: 12 }}>
                    <div className="stat-label">Battery</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>
                      {result.priorityBreakdown.batteryScore} / 50
                    </div>
                  </div>
                  <div className="card" style={{ padding: 12 }}>
                    <div className="stat-label">Departure</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>
                      {result.priorityBreakdown.departureScore} / 30
                    </div>
                  </div>
                  <div className="card" style={{ padding: 12 }}>
                    <div className="stat-label">Waiting</div>
                    <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>
                      {result.priorityBreakdown.waitingTimeScore} / 20
                    </div>
                  </div>
                </div>
              )}

              <div className="stack-sm" style={{ fontSize: '0.9rem' }}>
                <div>
                  <strong>Request ID:</strong> {result.requestId || 'N/A'}
                </div>
                {result.assignedChargerId && (
                  <div className="row" style={{ gap: 6, color: 'var(--green-dark)' }}>
                    <CheckCircle2 size={16} />
                    <span>
                      Assigned Charger: <strong>{result.assignedChargerName}</strong> (
                      {result.assignedChargerId})
                    </span>
                  </div>
                )}
                {result.queuePosition && (
                  <div className="row" style={{ gap: 6 }}>
                    <Zap size={16} color="var(--amber)" />
                    <span>
                      Queue Position: <strong>#{result.queuePosition}</strong> •{' '}
                      {formatWait(result.estimatedWaitMinutes)}
                    </span>
                  </div>
                )}
              </div>

              <div className="row" style={{ marginTop: 8 }}>
                {result.outcome === 'ASSIGNED' && (
                  <Link to="/sessions" className="btn btn-primary">
                    Start Session on Sessions Page
                  </Link>
                )}
                {result.outcome === 'WAITING' && (
                  <Link to="/queue" className="btn btn-secondary">
                    View Live Queue
                  </Link>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
