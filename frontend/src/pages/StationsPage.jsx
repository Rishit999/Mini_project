import { Eye, Plus, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorBanner, LoadingState, SuccessBanner } from '../components/common/Feedback';
import { Modal } from '../components/common/Modal';
import { StatusBadge } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

export function StationsPage() {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    name: '',
    location: '',
    status: 'OPERATIONAL',
    maxPowerCapacityKw: 200,
  });

  const loadStations = useCallback(async () => {
    try {
      setError('');
      const res = await evApi.getStations();
      setStations(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStations();
  }, [loadStations]);

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      setError('');
      await evApi.createStation({
        ...form,
        maxPowerCapacityKw: Number(form.maxPowerCapacityKw),
      });
      setNotice(`Station "${form.name}" created.`);
      setModalOpen(false);
      setForm({ name: '', location: '', status: 'OPERATIONAL', maxPowerCapacityKw: 200 });
      await loadStations();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleStatusChange = async (stationId, status) => {
    try {
      setError('');
      await evApi.updateStationStatus(stationId, status);
      setNotice(`Station ${stationId} status updated to ${status}.`);
      await loadStations();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading charging stations..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          Monitor electrical capacity limits, active bay counts, and queue load per station.
        </p>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadStations}>
            <RefreshCw size={15} /> Refresh
          </button>
          <button type="button" className="btn btn-primary" onClick={() => setModalOpen(true)}>
            <Plus size={15} /> Add Station
          </button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Station</th>
                <th>Status</th>
                <th>Power Capacity & Load</th>
                <th>Chargers</th>
                <th>Queue</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {stations.map((st) => (
                <tr key={st.stationId}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{st.name}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                      {st.stationId} • {st.location}
                    </div>
                  </td>
                  <td>
                    <StatusBadge status={st.status} />
                  </td>
                  <td style={{ minWidth: 210 }}>
                    <div style={{ fontSize: '0.82rem', marginBottom: 4 }}>
                      <strong>{st.currentAllocatedPowerKw.toFixed(0)}</strong> /{' '}
                      {st.maxPowerCapacityKw.toFixed(0)} kW ({st.powerUtilizationPercent.toFixed(1)}
                      %)
                    </div>
                    <div className="progress-track">
                      <div
                        className="progress-fill"
                        style={{
                          width: `${Math.min(100, st.powerUtilizationPercent)}%`,
                          background:
                            st.powerUtilizationPercent > 85 ? 'var(--red)' : 'var(--teal)',
                        }}
                      />
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-green">{st.availableChargers} Free</span>{' '}
                    <span className="badge badge-blue">{st.occupiedChargers} Busy</span>{' '}
                    <span className="badge badge-slate">{st.totalChargers} Total</span>
                  </td>
                  <td>
                    <strong>{st.queuedVehicles}</strong> waiting
                  </td>
                  <td>
                    <div className="row">
                      <Link to={`/stations/${st.stationId}`} className="btn btn-outline btn-sm">
                        <Eye size={14} /> Inspect
                      </Link>
                      <select
                        className="form-select"
                        style={{ width: 'auto', padding: '4px 8px', fontSize: '0.78rem' }}
                        value={st.status}
                        onChange={(e) => handleStatusChange(st.stationId, e.target.value)}
                        aria-label={`Change status for ${st.name}`}
                      >
                        <option value="OPERATIONAL">OPERATIONAL</option>
                        <option value="MAINTENANCE">MAINTENANCE</option>
                        <option value="OFFLINE">OFFLINE</option>
                      </select>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={modalOpen} title="Add New Charging Station" onClose={() => setModalOpen(false)}>
        <form onSubmit={handleCreate} className="stack-md">
          <div className="form-grid">
            <div className="form-field full">
              <label className="form-label" htmlFor="st-name">
                Station Name
              </label>
              <input
                id="st-name"
                className="form-input"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g., Metro EV Hub"
              />
            </div>
            <div className="form-field full">
              <label className="form-label" htmlFor="st-loc">
                Location
              </label>
              <input
                id="st-loc"
                className="form-input"
                required
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
                placeholder="e.g., Koramangala 4th Block, Bengaluru"
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="st-power">
                Max Power Capacity (kW)
              </label>
              <input
                id="st-power"
                type="number"
                min="10"
                max="5000"
                step="5"
                className="form-input"
                required
                value={form.maxPowerCapacityKw}
                onChange={(e) => setForm({ ...form, maxPowerCapacityKw: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="st-status">
                Initial Status
              </label>
              <select
                id="st-status"
                className="form-select"
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
              >
                <option value="OPERATIONAL">OPERATIONAL</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
          </div>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Create Station
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
