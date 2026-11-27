import { Plus, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { ErrorBanner, LoadingState, SuccessBanner } from '../components/common/Feedback';
import { Modal } from '../components/common/Modal';
import { StatusBadge } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

const CONNECTOR_OPTIONS = ['CCS', 'TYPE2', 'CHADEMO', 'NACS'];

export function ChargersPage() {
  const [chargers, setChargers] = useState([]);
  const [stations, setStations] = useState([]);
  const [stationFilter, setStationFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    stationId: '',
    name: '',
    chargerType: 'FAST_DC',
    powerRatingKw: 60,
    supportedConnectors: ['CCS'],
  });

  const loadData = useCallback(async () => {
    try {
      setError('');
      const [ch, st] = await Promise.all([
        evApi.getChargers(stationFilter || undefined),
        evApi.getStations(),
      ]);
      setChargers(ch);
      setStations(st);
      if (!form.stationId && st.length > 0) {
        setForm((prev) => ({ ...prev, stationId: st[0].stationId }));
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [stationFilter, form.stationId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleOpStatusChange = async (chargerId, operationalStatus) => {
    try {
      setError('');
      await evApi.updateChargerStatus(chargerId, operationalStatus);
      setNotice(`Charger ${chargerId} operational status changed to ${operationalStatus}.`);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const toggleConnector = (conn) => {
    setForm((prev) => {
      const exists = prev.supportedConnectors.includes(conn);
      const next = exists
        ? prev.supportedConnectors.filter((c) => c !== conn)
        : [...prev.supportedConnectors, conn];
      return { ...prev, supportedConnectors: next.length > 0 ? next : [conn] };
    });
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      setError('');
      await evApi.createCharger({
        ...form,
        powerRatingKw: Number(form.powerRatingKw),
      });
      setNotice(`Charger "${form.name}" added.`);
      setModalOpen(false);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading chargers..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <div className="row">
          <select
            className="form-select"
            style={{ width: 240 }}
            value={stationFilter}
            onChange={(e) => setStationFilter(e.target.value)}
            aria-label="Filter chargers by station"
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
          <button type="button" className="btn btn-outline" onClick={loadData}>
            <RefreshCw size={15} /> Refresh
          </button>
          <button type="button" className="btn btn-primary" onClick={() => setModalOpen(true)}>
            <Plus size={15} /> Add Charger
          </button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Charger</th>
                <th>Station</th>
                <th>Type & Power</th>
                <th>Connectors</th>
                <th>Availability</th>
                <th>Operational Status</th>
              </tr>
            </thead>
            <tbody>
              {chargers.map((c) => (
                <tr key={c.chargerId}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{c.name}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>{c.chargerId}</div>
                  </td>
                  <td>{c.stationName}</td>
                  <td>
                    <div>{c.chargerType}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                      {c.powerRatingKw} kW
                    </div>
                  </td>
                  <td>
                    <div className="row" style={{ gap: 4 }}>
                      {c.supportedConnectors.map((conn) => (
                        <span key={conn} className="badge badge-slate">
                          {conn}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>
                    <StatusBadge status={c.availabilityStatus} />
                    {c.currentVehicleId && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginTop: 3 }}>
                        Vehicle: {c.currentVehicleId}
                      </div>
                    )}
                  </td>
                  <td>
                    <select
                      className="form-select"
                      style={{ width: 165, padding: '5px 8px', fontSize: '0.8rem' }}
                      value={c.operationalStatus}
                      onChange={(e) => handleOpStatusChange(c.chargerId, e.target.value)}
                      aria-label={`Operational status for ${c.name}`}
                    >
                      <option value="OPERATIONAL">OPERATIONAL</option>
                      <option value="MAINTENANCE">MAINTENANCE</option>
                      <option value="OUT_OF_SERVICE">OUT_OF_SERVICE</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={modalOpen} title="Add Charger Bay" onClose={() => setModalOpen(false)}>
        <form onSubmit={handleCreate} className="stack-md">
          <div className="form-grid">
            <div className="form-field full">
              <label className="form-label" htmlFor="ch-station">
                Station
              </label>
              <select
                id="ch-station"
                className="form-select"
                value={form.stationId}
                onChange={(e) => setForm({ ...form, stationId: e.target.value })}
                required
              >
                {stations.map((s) => (
                  <option key={s.stationId} value={s.stationId}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field full">
              <label className="form-label" htmlFor="ch-name">
                Bay / Charger Name
              </label>
              <input
                id="ch-name"
                className="form-input"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g., Bay 5 - Rapid DC"
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="ch-type">
                Charger Type
              </label>
              <select
                id="ch-type"
                className="form-select"
                value={form.chargerType}
                onChange={(e) => setForm({ ...form, chargerType: e.target.value })}
              >
                <option value="STANDARD_AC">STANDARD_AC</option>
                <option value="FAST_DC">FAST_DC</option>
                <option value="RAPID_DC">RAPID_DC</option>
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="ch-power">
                Power Rating (kW)
              </label>
              <input
                id="ch-power"
                type="number"
                min="3.7"
                max="350"
                step="0.1"
                className="form-input"
                required
                value={form.powerRatingKw}
                onChange={(e) => setForm({ ...form, powerRatingKw: e.target.value })}
              />
            </div>
            <div className="form-field full">
              <span className="form-label">Supported Connectors</span>
              <div className="row">
                {CONNECTOR_OPTIONS.map((conn) => (
                  <label key={conn} className="row" style={{ gap: 6, fontSize: '0.86rem' }}>
                    <input
                      type="checkbox"
                      checked={form.supportedConnectors.includes(conn)}
                      onChange={() => toggleConnector(conn)}
                    />
                    {conn}
                  </label>
                ))}
              </div>
            </div>
          </div>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Add Charger
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
