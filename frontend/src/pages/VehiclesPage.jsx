import { Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { ErrorBanner, LoadingState, SuccessBanner } from '../components/common/Feedback';
import { Modal } from '../components/common/Modal';
import { evApi, extractErrorMessage } from '../services/api';

const EMPTY_VEHICLE = {
  registrationNumber: '',
  ownerName: '',
  model: '',
  connectorType: 'CCS',
  batteryCapacityKwh: 60,
  currentBatteryPercent: 25,
};

export function VehiclesPage() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_VEHICLE);

  const loadVehicles = useCallback(async () => {
    try {
      setError('');
      setVehicles(await evApi.getVehicles());
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadVehicles();
  }, [loadVehicles]);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_VEHICLE);
    setModalOpen(true);
  };

  const openEdit = (v) => {
    setEditingId(v.vehicleId);
    setForm({
      registrationNumber: v.registrationNumber,
      ownerName: v.ownerName,
      model: v.model,
      connectorType: v.connectorType,
      batteryCapacityKwh: v.batteryCapacityKwh,
      currentBatteryPercent: v.currentBatteryPercent,
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError('');
      const payload = {
        ...form,
        batteryCapacityKwh: Number(form.batteryCapacityKwh),
        currentBatteryPercent: Number(form.currentBatteryPercent),
      };
      if (editingId) {
        await evApi.updateVehicle(editingId, payload);
        setNotice(`Vehicle ${payload.registrationNumber} updated.`);
      } else {
        await evApi.createVehicle(payload);
        setNotice(`Vehicle ${payload.registrationNumber} registered.`);
      }
      setModalOpen(false);
      await loadVehicles();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleDelete = async (vehicleId) => {
    if (!window.confirm('Delete this vehicle record?')) return;
    try {
      setError('');
      await evApi.deleteVehicle(vehicleId);
      setNotice(`Vehicle ${vehicleId} removed.`);
      await loadVehicles();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading vehicles..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          Manage registered electric vehicles, connector types, and battery specifications.
        </p>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadVehicles}>
            <RefreshCw size={15} /> Refresh
          </button>
          <button type="button" className="btn btn-primary" onClick={openCreate}>
            <Plus size={15} /> Register Vehicle
          </button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Registration</th>
                <th>Model</th>
                <th>Owner</th>
                <th>Connector</th>
                <th>Battery</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {vehicles.map((v) => (
                <tr key={v.vehicleId}>
                  <td>
                    <div style={{ fontWeight: 700 }}>{v.registrationNumber}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>{v.vehicleId}</div>
                  </td>
                  <td>{v.model}</td>
                  <td>{v.ownerName}</td>
                  <td>
                    <span className="badge badge-teal">{v.connectorType}</span>
                  </td>
                  <td>
                    <div>
                      <strong>{v.currentBatteryPercent}%</strong> ({v.batteryCapacityKwh} kWh)
                    </div>
                  </td>
                  <td>
                    <div className="row">
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => openEdit(v)}
                      >
                        <Pencil size={13} /> Edit
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(v.vehicleId)}
                      >
                        <Trash2 size={13} /> Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={modalOpen}
        title={editingId ? 'Edit Vehicle' : 'Register New EV'}
        onClose={() => setModalOpen(false)}
      >
        <form onSubmit={handleSubmit} className="stack-md">
          <div className="form-grid">
            <div className="form-field">
              <label className="form-label" htmlFor="v-reg">
                Registration Number
              </label>
              <input
                id="v-reg"
                className="form-input"
                required
                value={form.registrationNumber}
                onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
                placeholder="KA-01-EV-9001"
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="v-owner">
                Owner Name
              </label>
              <input
                id="v-owner"
                className="form-input"
                required
                value={form.ownerName}
                onChange={(e) => setForm({ ...form, ownerName: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="v-model">
                Vehicle Model
              </label>
              <input
                id="v-model"
                className="form-input"
                required
                value={form.model}
                onChange={(e) => setForm({ ...form, model: e.target.value })}
                placeholder="Hyundai Ioniq 5"
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="v-conn">
                Connector Type
              </label>
              <select
                id="v-conn"
                className="form-select"
                value={form.connectorType}
                onChange={(e) => setForm({ ...form, connectorType: e.target.value })}
              >
                <option value="CCS">CCS</option>
                <option value="TYPE2">TYPE2</option>
                <option value="CHADEMO">CHADEMO</option>
                <option value="NACS">NACS</option>
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="v-cap">
                Battery Capacity (kWh)
              </label>
              <input
                id="v-cap"
                type="number"
                min="10"
                max="250"
                step="0.5"
                className="form-input"
                required
                value={form.batteryCapacityKwh}
                onChange={(e) => setForm({ ...form, batteryCapacityKwh: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="v-pct">
                Current Battery (%)
              </label>
              <input
                id="v-pct"
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
          </div>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              {editingId ? 'Save Changes' : 'Register Vehicle'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
