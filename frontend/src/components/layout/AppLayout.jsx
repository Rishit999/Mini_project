import {
  Activity,
  BarChart3,
  BatteryCharging,
  CalendarClock,
  Car,
  LayoutDashboard,
  ListOrdered,
  Menu,
  PlugZap,
  Send,
  Settings,
  Zap,
} from 'lucide-react';
import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';

const NAV_GROUPS = [
  {
    label: 'Operations',
    items: [
      { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
      { to: '/requests', label: 'Request Charging', icon: Send },
      { to: '/queue', label: 'Priority Queue', icon: ListOrdered },
      { to: '/sessions', label: 'Sessions', icon: Activity },
    ],
  },
  {
    label: 'Infrastructure',
    items: [
      { to: '/stations', label: 'Stations', icon: Zap },
      { to: '/chargers', label: 'Chargers', icon: PlugZap },
      { to: '/reservations', label: 'Reservations', icon: CalendarClock },
      { to: '/vehicles', label: 'Vehicles', icon: Car },
    ],
  },
  {
    label: 'Insights & Config',
    items: [
      { to: '/analytics', label: 'Analytics', icon: BarChart3 },
      { to: '/settings', label: 'Station Settings', icon: Settings },
    ],
  },
];

const ROUTE_TITLES = {
  '/': 'Operations Dashboard',
  '/requests': 'Charging Request Submission',
  '/queue': 'Live Priority Queue',
  '/sessions': 'Charging Sessions',
  '/stations': 'Charging Stations',
  '/chargers': 'Charger Hardware Grid',
  '/reservations': 'Bay Reservations',
  '/vehicles': 'Registered Vehicles',
  '/analytics': 'Utilization & Priority Analytics',
  '/settings': 'Station Configuration',
};

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const title =
    ROUTE_TITLES[location.pathname] ||
    (location.pathname.startsWith('/stations/') ? 'Station Detail Monitor' : 'EVChargeFlow');

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <div className="brand-icon">
            <BatteryCharging size={20} />
          </div>
          <div>
            <div className="brand-title">EVChargeFlow</div>
            <div className="brand-sub">Queue & Allocation DSS</div>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Main navigation">
          {NAV_GROUPS.map((group) => (
            <div key={group.label}>
              <div className="nav-section-label">{group.label}</div>
              {group.items.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    onClick={() => setSidebarOpen(false)}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    <Icon size={17} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div>4-Engine Architecture</div>
          <div style={{ color: '#64748b', marginTop: 2 }}>Simulated Telemetry Mode</div>
        </div>
      </aside>

      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close navigation menu"
          onClick={() => setSidebarOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(15,23,42,0.45)',
            border: 'none',
            zIndex: 35,
          }}
        />
      )}

      <div className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="mobile-menu-btn"
              onClick={() => setSidebarOpen((v) => !v)}
              aria-label="Toggle navigation"
            >
              <Menu size={20} />
            </button>
            <h1 className="page-title">{title}</h1>
          </div>
          <div
            className="sim-pill"
            title="Energy and session progression are simulated in software"
          >
            <Activity size={14} />
            <span>Simulated EV Environment</span>
          </div>
        </header>

        <main className="content-container">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
