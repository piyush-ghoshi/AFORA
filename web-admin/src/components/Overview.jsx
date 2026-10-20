import React from 'react';
import {
  BookOpen,
  Users,
  Calendar,
  Sparkles,
  Award,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Clock
} from 'lucide-react';

export default function Overview({
  classSections,
  teachers,
  students,
  timetableSlots,
  onOpenGenerateModal,
  onNavigateToClasses,
  onNavigateToTimetable,
  onNavigateToStudents
}) {
  const totalClasses = classSections.length;
  const totalTeachers = teachers.length;
  const totalStudents = students.length;
  const totalSlots = timetableSlots.length;
  const enrolledFaces = students.filter(s => s.hasFaceProfile).length;

  return (
    <div>
      {/* Top Welcome Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          borderRadius: 'var(--radius-lg)',
          padding: '28px 32px',
          marginBottom: '32px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '20px'
        }}
      >
        <div>
          <h2 style={{ fontSize: '1.6rem', marginBottom: '8px' }}>
            Welcome to AFORA Academic Suite
          </h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '650px', fontSize: '0.95rem' }}>
            Manage lecture halls, assign faculty, orchestrate weekly timetable schedules,
            and monitor AI face-recognition biometric enrollments for Android attendance verification.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-primary" onClick={onOpenGenerateModal}>
            <Sparkles size={18} />
            <span>Generate Sessions</span>
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card" onClick={onNavigateToClasses} style={{ cursor: 'pointer' }}>
          <div>
            <div className="stat-label">Active Class Sections</div>
            <div className="stat-value">{totalClasses}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--primary-500)', marginTop: '6px' }}>
              Across all departments
            </div>
          </div>
          <div className="stat-icon">
            <BookOpen size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div>
            <div className="stat-label">Assigned Teachers</div>
            <div className="stat-value">{totalTeachers}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '6px' }}>
              Verified faculty profiles
            </div>
          </div>
          <div className="stat-icon">
            <Award size={24} />
          </div>
        </div>

        <div className="stat-card" onClick={onNavigateToTimetable} style={{ cursor: 'pointer' }}>
          <div>
            <div className="stat-label">Weekly Recurring Slots</div>
            <div className="stat-value">{totalSlots}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--success)', marginTop: '6px' }}>
              Scheduled Mon-Fri
            </div>
          </div>
          <div className="stat-icon" style={{ color: 'var(--success)' }}>
            <Calendar size={24} />
          </div>
        </div>

        <div className="stat-card" onClick={onNavigateToStudents} style={{ cursor: 'pointer' }}>
          <div>
            <div className="stat-label">Enrolled Students</div>
            <div className="stat-value">{totalStudents}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '6px' }}>
              Active admissions
            </div>
          </div>
          <div className="stat-icon">
            <Users size={24} />
          </div>
        </div>

        <div className="stat-card" onClick={onNavigateToStudents} style={{ cursor: 'pointer' }}>
          <div>
            <div className="stat-label">Face Biometrics Registered</div>
            <div className="stat-value" style={{ color: enrolledFaces > 0 ? 'var(--success)' : 'var(--warning)' }}>
              {enrolledFaces} / {totalStudents}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '6px' }}>
              192-dim MobileFaceNet
            </div>
          </div>
          <div className="stat-icon" style={{ color: enrolledFaces > 0 ? 'var(--success)' : 'var(--warning)' }}>
            <ShieldCheck size={24} />
          </div>
        </div>
      </div>

      {/* Two Column Layout: Active Classes and Quick Timetable */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))', gap: '24px' }}>
        {/* Class Sections Card */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Active Class Sections</div>
              <div className="card-description">Faculty instructors and classroom allocations</div>
            </div>
            <button className="btn btn-secondary" onClick={onNavigateToClasses}>
              <span>View All</span>
              <ArrowRight size={16} />
            </button>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Section</th>
                  <th>Subject</th>
                  <th>Instructor</th>
                  <th>Room</th>
                  <th>Capacity</th>
                </tr>
              </thead>
              <tbody>
                {classSections.length === 0 ? (
                  <tr>
                    <td colSpan="5" style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '24px' }}>
                      No class sections created yet. Click Class Sections Studio to create one!
                    </td>
                  </tr>
                ) : (
                  classSections.slice(0, 5).map((sec) => (
                    <tr key={sec.id}>
                      <td style={{ fontWeight: 600 }}>{sec.name}</td>
                      <td>
                        <span className="badge badge-info">{sec.subjectCode}</span>
                      </td>
                      <td>{sec.teacherName}</td>
                      <td>{sec.roomNumber || 'TBD'}</td>
                      <td>
                        <span className="badge badge-success">
                          {sec.enrolledCount} / {sec.maxStudents}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Timetable Snippet Card */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Weekly Schedule Highlights</div>
              <div className="card-description">Recurring slots driving daily attendance sessions</div>
            </div>
            <button className="btn btn-secondary" onClick={onNavigateToTimetable}>
              <span>Full Matrix</span>
              <ArrowRight size={16} />
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {timetableSlots.length === 0 ? (
              <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '32px' }}>
                No timetable slots defined. Add recurring slots in the Timetable Matrix tab!
              </div>
            ) : (
              timetableSlots.slice(0, 5).map((slot) => (
                <div
                  key={slot.id}
                  style={{
                    background: 'var(--bg-surface-elevated)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-md)',
                    padding: '14px 18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div
                      style={{
                        background: 'rgba(99, 102, 241, 0.15)',
                        color: 'var(--primary-500)',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-sm)',
                        fontWeight: 700,
                        fontSize: '0.85rem',
                      }}
                    >
                      {slot.dayOfWeekName.slice(0, 3)}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{slot.classSectionName}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'flex', gap: '8px' }}>
                        <span>{slot.teacherName}</span>
                        <span>•</span>
                        <span>Room {slot.roomNumber || 'TBD'}</span>
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: '#c7d2fe' }}>
                    <Clock size={16} />
                    <span>{slot.startTime} - {slot.endTime}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
