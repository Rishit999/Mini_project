import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export function extractErrorMessage(err) {
  if (err?.response?.data?.message) {
    const details = err.response.data.fieldErrors;
    if (details && Object.keys(details).length > 0) {
      return `${err.response.data.message}: ${Object.values(details).join(', ')}`;
    }
    return err.response.data.message;
  }
  return err?.message || 'Unexpected network error occurred.';
}

export const evApi = {
  // Dashboard & Analytics
  getHealth: () => apiClient.get('/health').then((r) => r.data),
  getDashboard: () => apiClient.get('/dashboard').then((r) => r.data),
  getAnalytics: () => apiClient.get('/analytics').then((r) => r.data),
  getAuditLogs: () => apiClient.get('/audit').then((r) => r.data),

  // Stations & Chargers
  getStations: () => apiClient.get('/stations').then((r) => r.data),
  getStationDetail: (stationId) => apiClient.get(`/stations/${stationId}`).then((r) => r.data),
  createStation: (data) => apiClient.post('/stations', data).then((r) => r.data),
  updateStation: (stationId, data) =>
    apiClient.put(`/stations/${stationId}`, data).then((r) => r.data),
  updateStationStatus: (stationId, status) =>
    apiClient.patch(`/stations/${stationId}/status`, { status }).then((r) => r.data),

  getChargers: (stationId) =>
    apiClient.get('/chargers', { params: stationId ? { stationId } : {} }).then((r) => r.data),
  createCharger: (data) => apiClient.post('/chargers', data).then((r) => r.data),
  updateChargerStatus: (chargerId, operationalStatus) =>
    apiClient.patch(`/chargers/${chargerId}/status`, { operationalStatus }).then((r) => r.data),

  // Vehicles
  getVehicles: () => apiClient.get('/vehicles').then((r) => r.data),
  createVehicle: (data) => apiClient.post('/vehicles', data).then((r) => r.data),
  updateVehicle: (vehicleId, data) =>
    apiClient.put(`/vehicles/${vehicleId}`, data).then((r) => r.data),
  deleteVehicle: (vehicleId) => apiClient.delete(`/vehicles/${vehicleId}`).then((r) => r.data),

  // Queue & Requests
  getQueue: (stationId) =>
    apiClient.get('/queue', { params: stationId ? { stationId } : {} }).then((r) => r.data),
  submitRequest: (data) => apiClient.post('/requests', data).then((r) => r.data),
  cancelRequest: (requestId) => apiClient.post(`/requests/${requestId}/cancel`).then((r) => r.data),
  runAllocation: (stationId) =>
    apiClient
      .post('/allocate', null, { params: stationId ? { stationId } : {} })
      .then((r) => r.data),

  // Reservations
  getReservations: (stationId) =>
    apiClient.get('/reservations', { params: stationId ? { stationId } : {} }).then((r) => r.data),
  createReservation: (data) => apiClient.post('/reservations', data).then((r) => r.data),
  cancelReservation: (reservationId) =>
    apiClient.post(`/reservations/${reservationId}/cancel`).then((r) => r.data),
  processReservations: () => apiClient.post('/reservations/process').then((r) => r.data),

  // Sessions
  getSessions: (stationId) =>
    apiClient.get('/sessions', { params: stationId ? { stationId } : {} }).then((r) => r.data),
  startSession: (requestId) => apiClient.post(`/sessions/start/${requestId}`).then((r) => r.data),
  startSessionFromReservation: (reservationId, expectedDurationMinutes = 0) =>
    apiClient
      .post(`/sessions/start-reservation/${reservationId}`, null, {
        params: { expectedDurationMinutes },
      })
      .then((r) => r.data),
  completeSession: (sessionId) =>
    apiClient.post(`/sessions/${sessionId}/complete`).then((r) => r.data),
  stopSession: (sessionId) => apiClient.post(`/sessions/${sessionId}/stop`).then((r) => r.data),

  // Wait Time & Settings
  estimateWaitTime: (params) =>
    apiClient.get('/wait-time/estimate', { params }).then((r) => r.data),
  getAllSettings: () => apiClient.get('/settings').then((r) => r.data),
  getStationSettings: (stationId) => apiClient.get(`/settings/${stationId}`).then((r) => r.data),
  updateStationSettings: (stationId, data) =>
    apiClient.put(`/settings/${stationId}`, data).then((r) => r.data),
};
