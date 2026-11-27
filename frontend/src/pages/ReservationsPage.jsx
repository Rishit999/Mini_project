import { CalendarPlus, Play, RefreshCw, XCircle } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorBanner,
  LoadingState,
  SuccessBanner,
} from '../components/common/Feedback';
import { Modal } from '../components/common/Modal';
import { StatusBadge, formatTime } from '../components/common/StatusBadge';
import { evApi, extractErrorMessage } from '../services/api';

function toLocalInput(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function ReservationsPage() {
  const [reservations, setReservations] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [stations, setStations] = useState([]);
  const [chargers, setChargers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modalOpen, setModalOpen] = useState(false);

  const now = new Date();
  const [form, setForm] = useState({
    vehicleId: '',
    stationId: '',
    chargerId: '',
    startLocal: toLocalInput(new Date(now.getTime() + 30 * 60000)),
    endLocal: toLocalInput(new Date(now.getTime() + 90 * 60000)),
    notes: '',
  });

  const loadAll = useCallback(async () => {
    try {
      setError('');
      const [resList, vList, sList, cList] = await Promise.all([
        evApi.getReservations(),
        evApi.getVehicles(),
        evApi.getStations(),
        evApi.getChargers(),
      ]);
      setReservations(resList);
      setVehicles(vList);
      setStations(sList);
      setChargers(cList);
      if (vList.length > 0 && sList.length > 0) {
        const defaultStn = sList[0].stationId;
        const stChargers = cList.filter((c) => c.stationId === defaultStn);
        setForm((prev) => ({
          ...prev,
          vehicleId: prev.vehicleId || vList[0].vehicleId,
          stationId: prev.stationId || defaultStn,
          chargerId: prev.chargerId || (stChargers[0]?.chargerId ?? ''),
        }));
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const stationChargers = chargers.filter((c) => c.stationId === form.stationId);

  const handleStationChange = (stationId) => {
    const matching = chargers.filter((c) => c.stationId === stationId);
    setForm((prev) => ({
      ...prev,
      stationId,
      chargerId: matching[0]?.chargerId || '',
    }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      setError('');
      await evApi.createReservation({
        vehicleId: form.vehicleId,
        stationId: form.stationId,
        chargerId: form.chargerId,
        startTime: new Date(form.startLocal).toISOString(),
        endTime: new Date(form.endLocal).toISOString(),
        notes: form.notes,
      });
      setNotice('Reservation confirmed without overlap conflicts.');
      setModalOpen(false);
      await loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleCancel = async (reservationId) => {
    if (!window.confirm(`Cancel reservation ${reservationId}?`)) return;
    try {
      setError('');
      await evApi.cancelReservation(reservationId);
      setNotice(`Reservation ${reservationId} cancelled.`);
      await loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleProcess = async () => {
    try {
      setError('');
      const res = await evApi.processReservations();
      setNotice(`Processed reservations: ${res.updatedCount} transition(s) applied.`);
      await loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleStartSession = async (reservationId) => {
    try {
      setError('');
      const ses = await evApi.startSessionFromReservation(reservationId);
      setNotice(`Started simulated charging session ${ses.sessionId} from reservation.`);
      await loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  if (loading) return <LoadingState label="Loading reservations..." />;

  return (
    <div className="stack-lg">
      <ErrorBanner message={error} onDismiss={() => setError('')} />
      <SuccessBanner message={notice} onDismiss={() => setNotice('')} />

      <div className="row-between">
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>
          Overlap rule:{' '}
          <code>ExistingStart &lt; RequestedEnd AND RequestedStart &lt; ExistingEnd</code> (adjacent
          slots allowed).
        </p>
        <div className="row">
          <button type="button" className="btn btn-outline" onClick={loadAll}>
            <RefreshCw size={15} /> Refresh
          </button>
          <button type="button" className="btn btn-secondary" onClick={handleProcess}>
            <Play size={15} /> Process Windows
          </button>
          <button type="button" className="btn btn-primary" onClick={() => setModalOpen(true)}>
            <CalendarPlus size={15} /> New Reservation
          </button>
        </div>
      </div>

      {reservations.length === 0 ? (
        <EmptyState
          title="No Reservations Found"
          description="Create a charger reservation slot for an upcoming EV arrival."
        />
      ) : (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Vehicle</th>
                  <th>Station & Charger</th>
                  <th>Time Window</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {reservations.map((r) => (
                  <tr key={r.reservationId}>
                    <td>
                      <div style={{ fontWeight: 700 }}>{r.reservationId}</div>
                      {r.notes && (
                        <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>{r.notes}</div>
                      )}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{r.registrationNumber}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                        {r.vehicleModel}
                      </div>
                    </td>
                    <td>
                      <div>{r.stationName}</div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--teal)' }}>
                        {r.chargerName} ({r.chargerId})
                      </div>
                    </td>
                    <td style={{ fontSize: '0.82rem' }}>
                      <div>From: {formatTime(r.startTime)}</div>
                      <div>To: {formatTime(r.endTime)}</div>
                    </td>
                    <td>
                      <StatusBadge status={r.status} />
                    </td>
                    <td>
                      <div className="row">
                        {(r.status === 'CONFIRMED' || r.status === 'ACTIVE') && (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => handleStartSession(r.reservationId)}
                            >
                              <Play size={13} /> Check-In
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger btn-sm"
                              onClick={() => handleCancel(r.reservationId)}
                            >
                              <XCircle size={13} /> Cancel
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal open={modalOpen} title="Book Charger Reservation" onClose={() => setModalOpen(false)}>
        <form onSubmit={handleCreate} className="stack-md">
          <div className="form-grid">
            <div className="form-field full">
              <label className="form-label" htmlFor="res-veh">
                Vehicle
              </label>
              <select
                id="res-veh"
                className="form-select"
                required
                value={form.vehicleId}
                onChange={(e) => setForm({ ...form, vehicleId: e.target.value })}
              >
                {vehicles.map((v) => (
                  <option key={v.vehicleId} value={v.vehicleId}>
                    {v.registrationNumber} — {v.model} ({v.connectorType})
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="res-stn">
                Station
              </label>
              <select
                id="res-stn"
                className="form-select"
                required
                value={form.stationId}
                onChange={(e) => handleStationChange(e.target.value)}
              >
                {stations.map((s) => (
                  <option key={s.stationId} value={s.stationId}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="res-chg">
                Charger Bay
              </label>
              <select
                id="res-chg"
                className="form-select"
                required
                value={form.chargerId}
                onChange={(e) => setForm({ ...form, chargerId: e.target.value })}
              >
                {stationChargers.map((c) => (
                  <option key={c.chargerId} value={c.chargerId}>
                    {c.name} ({c.supportedConnectors.join(', ')})
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="res-start">
                Start Time
              </label>
              <input
                id="res-start"
                type="datetime-local"
                className="form-input"
                required
                value={form.startLocal}
                onChange={(e) => setForm({ ...form, startLocal: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label className="form-label" htmlFor="res-end">
                End Time
              </label>
              <input
                id="res-end"
                type="datetime-local"
                className="form-input"
                required
                value={form.endLocal}
                onChange={(e) => setForm({ ...form, endLocal: e.target.value })}
              />
            </div>
            <div className="form-field full">
              <label className="form-label" htmlFor="res-notes">
                Notes
              </label>
              <input
                id="res-notes"
                className="form-input"
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                placeholder="Optional driver note"
              />
            </div>
          </div>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Confirm Reservation
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
