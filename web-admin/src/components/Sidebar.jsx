import React from 'react';
import {
  LayoutDashboard,
  BookOpen,
  Calendar,
  Users,
  Sparkles,
  ExternalLink,
  ShieldCheck,
  Server
} from 'lucide-react';

export default function Sidebar({ currentTab, setCurrentTab, isConnected, onOpenGenerateModal }) {
  const navItems = [
    { id: 'overview', label: 'Dashboard Overview', icon: LayoutDashboard },
    { id: 'classes', label: 'Class Sections Studio', icon: BookOpen },
    { id: 'timetable', label: 'Weekly Timetable Matrix', icon: Calendar },
    { id: 'students', label: 'Students & Biometrics', icon: Users },
  ];

  return (
    <aside className="sidebar">
      {/* Brand Header */}
      <div className="sidebar-header">
        <div className="brand-badge">AF</div>
        <div>
          <div className="brand-title">AFORA Admin</div>
          <div className="brand-subtitle">Academic Suite</div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              className={`nav-item ${isActive ? 'active' : ''}`}
              onClick={() => setCurrentTab(item.id)}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </button>
          );
        })}

        <div style={{ margin: '16px 0', borderTop: '1px solid var(--border-subtle)' }} />

        {/* Quick Action Button in Sidebar */}
        <button
          className="btn btn-primary"
          style={{ width: '100%', justifyContent: 'center' }}
          onClick={onOpenGenerateModal}
        >
          <Sparkles size={18} />
          <span>Generate Sessions</span>
        </button>

        {/* Swagger UI link */}
        <a
          href="/api/swagger-ui/index.html"
          target="_blank"
          rel="noreferrer"
          className="nav-item"
          style={{ marginTop: 'auto', textDecoration: 'none' }}
        >
          <ExternalLink size={18} />
          <span>Swagger REST Docs</span>
        </a>
      </nav>

      {/* System Status Footer */}
      <div className="sidebar-footer">
        <div className="system-status">
          <div
            className="status-dot"
            style={{
              backgroundColor: isConnected ? 'var(--success)' : 'var(--danger)',
              boxShadow: isConnected ? '0 0 8px var(--success)' : '0 0 8px var(--danger)',
            }}
          />
          <span>{isConnected ? 'Backend Live (Port 8080)' : 'Connecting to API...'}</span>
        </div>
        <ShieldCheck size={18} color="var(--primary-500)" />
      </div>
    </aside>
  );
}
