import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { ChargersPage } from './pages/ChargersPage';
import { DashboardPage } from './pages/DashboardPage';
import { QueuePage } from './pages/QueuePage';
import { RequestsPage } from './pages/RequestsPage';
import { ReservationsPage } from './pages/ReservationsPage';
import { SessionsPage } from './pages/SessionsPage';
import { SettingsPage } from './pages/SettingsPage';
import { StationDetailPage } from './pages/StationDetailPage';
import { StationsPage } from './pages/StationsPage';
import { VehiclesPage } from './pages/VehiclesPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/stations" element={<StationsPage />} />
          <Route path="/stations/:stationId" element={<StationDetailPage />} />
          <Route path="/chargers" element={<ChargersPage />} />
          <Route path="/vehicles" element={<VehiclesPage />} />
          <Route path="/queue" element={<QueuePage />} />
          <Route path="/requests" element={<RequestsPage />} />
          <Route path="/reservations" element={<ReservationsPage />} />
          <Route path="/sessions" element={<SessionsPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
