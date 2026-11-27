import { RefreshCw, Save } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { ErrorBanner, LoadingState, SuccessBanner } from '../components/common/Feedback';
import { evApi, extractErrorMessage } from '../services/api';

export function SettingsPage() {
  const [settingsList, setSettingsList] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadSettings = useCallback(async () => {
    try {
      setError('');
      const list = await evApi.getAllSettings();
      setSettingsList(list);
      if (list.length > 0) {
        const active = list.find((s) => s.stationId === selectedId) || list[0];
        setSelectedId(active.stationId);
        setForm({ ...active });
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  const handleSelectStation = (stationId) => {
    setSelectedId(stationId);
    const match = settingsList.find((s) => s.stationId === stationId);
    if (match) setForm({ ...match });
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form) return;
    try {
      setSaving(true);
      setError('');
      const updated = await evApi.updateStationSettings(selectedId, {
        maxPowerCapacityKw: Number(form.maxPowerCapacityKw),
        queueRefreshIntervalSec: Number(form.queueRefreshIntervalSec),
        reservationGracePeriodMinutes: Number(form.reservationGracePeriodMinutes),
        chargingEfficiencyFactor: Number(form.chargingEfficiencyFactor),
        defaultChargingDurationMinutes: Number(form.defaultChargingDurationMinutes),
        autoAllocationEnabled: Boolean(form.autoAllocationEnabled),
        simulationSpeedMultiplier: Number(form.simulationSpeedMultiplier),
      });
      setForm({ ...updated });
      setNotice(`Configuration saved for ${updated.stationName}.`);
      await loadSettings();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState label="Loading station configurations..." />;
  if (!form) return <ErrorBanner message={error || 'No station configurations found.'} />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <div className="row">
          <label htmlFor="cfg-station-select" className="form-label" style={{ margin: 0 }}>
            Station:
          </label>
          <select
            id="cfg-station-select"
            className="form-select"
            style={{ width: 280 }}
            value={selectedId}
            onChange={(e) => handleSelectStation(e.target.value)}
          >
            {settingsList.map((s) => (
              <option key={s.stationId} value={s.stationId}>
                {s.stationName} ({s.stationId})
              </option>
            ))}
          </select>
        </div>
        <button type="button" className="btn btn-outline" onClick={loadSettings}>
          <RefreshCw size={15} /> Reload
        </button>
      </div>

      <div className="card">
        <form onSubmit={handleSave} className="stack-md">
          <div className="form-grid">
            <div className="form-field">
              <label className="form-label" htmlFor="cfg-maxpwr">
                Station Max Power Capacity (kW)
              </label>
              <input
                id="cfg-maxpwr"
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
              <label className="form-label" htmlFor="cfg-refresh">
                Queue Refresh Interval (sec)
              </label>
              <input
                id="cfg-refresh"
                type="number"
                min="3"
                max="300"
                className="form-input"
                required
                value={form.queueRefreshIntervalSec}
                onChange={(e) => setForm({ ...form, queueRefreshIntervalSec: e.target.value })}
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="cfg-grace">
                Reservation Grace Period (min)
              </label>
              <input
                id="cfg-grace"
                type="number"
                min="5"
                max="60"
                className="form-input"
                required
                value={form.reservationGracePeriodMinutes}
                onChange={(e) =>
                  setForm({ ...form, reservationGracePeriodMinutes: e.target.value })
                }
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="cfg-eff">
                Charging Efficiency Factor (0.50–1.00)
              </label>
              <input
                id="cfg-eff"
                type="number"
                min="0.50"
                max="1.00"
                step="0.01"
                className="form-input"
                required
                value={form.chargingEfficiencyFactor}
                onChange={(e) => setForm({ ...form, chargingEfficiencyFactor: e.target.value })}
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="cfg-dur">
                Default Charging Duration (min)
              </label>
              <input
                id="cfg-dur"
                type="number"
                min="10"
                max="480"
                className="form-input"
                required
                value={form.defaultChargingDurationMinutes}
                onChange={(e) =>
                  setForm({ ...form, defaultChargingDurationMinutes: e.target.value })
                }
              />
            </div>

            <div className="form-field">
              <label className="form-label" htmlFor="cfg-sim">
                Simulation Speed Multiplier (0.5–20.0)
              </label>
              <input
                id="cfg-sim"
                type="number"
                min="0.5"
                max="20.0"
                step="0.5"
                className="form-input"
                required
                value={form.simulationSpeedMultiplier}
                onChange={(e) => setForm({ ...form, simulationSpeedMultiplier: e.target.value })}
              />
            </div>

            <div className="form-field full">
              <label className="row" style={{ gap: 8, fontSize: '0.9rem', fontWeight: 600 }}>
                <input
                  type="checkbox"
                  checked={form.autoAllocationEnabled}
                  onChange={(e) => setForm({ ...form, autoAllocationEnabled: e.target.checked })}
                />
                Enable automatic allocation when chargers become free
              </label>
            </div>
          </div>

          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={15} /> {saving ? 'Saving...' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
