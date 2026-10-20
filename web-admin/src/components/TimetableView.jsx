import React, { useState } from 'react';
import { Plus, Calendar, Clock, MapPin, User, X } from 'lucide-react';

const DAYS_OF_WEEK = [
  { id: 1, name: 'Monday', short: 'Mon' },
  { id: 2, name: 'Tuesday', short: 'Tue' },
  { id: 3, name: 'Wednesday', short: 'Wed' },
  { id: 4, name: 'Thursday', short: 'Thu' },
  { id: 5, name: 'Friday', short: 'Fri' },
];

export default function TimetableView({
  timetableSlots,
  classSections,
  onCreateSlot
}) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedSectionFilter, setSelectedSectionFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [formData, setFormData] = useState({
    classSectionId: '',
    dayOfWeek: 1,
    startTime: '09:00',
    endTime: '10:30',
    roomNumber: ''
  });

  const filteredSlots = selectedSectionFilter
    ? timetableSlots.filter(s => s.classSectionId === parseInt(selectedSectionFilter, 10))
    : timetableSlots;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.classSectionId) {
      setError('Please select a class section');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await onCreateSlot({
        classSectionId: parseInt(formData.classSectionId, 10),
        dayOfWeek: parseInt(formData.dayOfWeek, 10),
        startTime: formData.startTime,
        endTime: formData.endTime,
        roomNumber: formData.roomNumber || null
      });
      setIsModalOpen(false);
      setFormData({
        classSectionId: classSections[0]?.id || '',
        dayOfWeek: 1,
        startTime: '09:00',
        endTime: '10:30',
        roomNumber: ''
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Controls & Filter Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Filter by Class Section:</label>
          <select
            className="form-select"
            style={{ width: '240px' }}
            value={selectedSectionFilter}
            onChange={(e) => setSelectedSectionFilter(e.target.value)}
          >
            <option value="">All Class Sections</option>
            {classSections.map(sec => (
              <option key={sec.id} value={sec.id}>{sec.name}</option>
            ))}
          </select>
        </div>

        <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
          <Plus size={18} />
          <span>Add Weekly Slot</span>
        </button>
      </div>

      {/* 5-Day Weekly Matrix */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '16px' }}>
        {DAYS_OF_WEEK.map((day) => {
          const daySlots = filteredSlots
            .filter(s => s.dayOfWeek === day.id)
            .sort((a, b) => a.startTime.localeCompare(b.startTime));

          return (
            <div
              key={day.id}
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)',
                padding: '16px',
                minHeight: '480px',
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              {/* Day Header */}
              <div
                style={{
                  paddingBottom: '12px',
                  marginBottom: '16px',
                  borderBottom: '1px solid var(--border-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>
                    {day.name}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    {daySlots.length} {daySlots.length === 1 ? 'lecture' : 'lectures'}
                  </div>
                </div>
                <div
                  style={{
                    background: 'var(--bg-surface-elevated)',
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.8rem',
                    fontWeight: 700,
                    color: 'var(--primary-500)',
                  }}
                >
                  {day.id}
                </div>
              </div>

              {/* Slots List for this day */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                {daySlots.length === 0 ? (
                  <div
                    style={{
                      flex: 1,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--text-muted)',
                      fontSize: '0.8rem',
                      fontStyle: 'italic',
                      border: '1px dashed rgba(255, 255, 255, 0.05)',
                      borderRadius: 'var(--radius-md)',
                      padding: '20px',
                      textAlign: 'center',
                    }}
                  >
                    No scheduled slots
                  </div>
                ) : (
                  daySlots.map((slot) => (
                    <div
                      key={slot.id}
                      style={{
                        background: 'var(--bg-surface-elevated)',
                        borderLeft: '4px solid var(--primary-500)',
                        borderTop: '1px solid var(--border-subtle)',
                        borderRight: '1px solid var(--border-subtle)',
                        borderBottom: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-sm)',
                        padding: '12px',
                        boxShadow: 'var(--shadow-sm)',
                        transition: 'transform var(--transition-fast)',
                      }}
                    >
                      {/* Time pill */}
                      <div
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '5px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          color: '#a5b4fc',
                          marginBottom: '6px',
                        }}
                      >
                        <Clock size={12} />
                        <span>{slot.startTime} - {slot.endTime}</span>
                      </div>

                      {/* Course / Section */}
                      <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'white', marginBottom: '2px' }}>
                        {slot.classSectionName}
                      </div>

                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '8px' }}>
                        {slot.subjectName} ({slot.subjectCode})
                      </div>

                      {/* Instructor and Room info */}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.75rem', color: 'var(--text-muted)', borderTop: '1px solid rgba(255, 255, 255, 0.05)', paddingTop: '6px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <User size={12} color="var(--primary-500)" />
                          <span>{slot.teacherName}</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <MapPin size={12} color="var(--primary-500)" />
                          <span>Room: {slot.roomNumber || 'TBD'}</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Add Slot Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.3rem' }}>Add Weekly Timetable Slot</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  Define a recurring weekly class session. The instructor is inherited automatically.
                </p>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {error && (
              <div style={{ background: 'var(--danger-bg)', border: '1px solid var(--danger)', padding: '10px 14px', borderRadius: 'var(--radius-md)', color: '#fca5a5', marginBottom: '16px', fontSize: '0.85rem' }}>
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label className="form-label">Class Section *</label>
                  <select
                    className="form-select"
                    value={formData.classSectionId}
                    onChange={(e) => {
                      const secId = e.target.value;
                      const sec = classSections.find(s => s.id === parseInt(secId, 10));
                      setFormData(prev => ({
                        ...prev,
                        classSectionId: secId,
                        roomNumber: sec?.roomNumber || prev.roomNumber
                      }));
                    }}
                    required
                  >
                    <option value="">Select Class Section</option>
                    {classSections.map(sec => (
                      <option key={sec.id} value={sec.id}>
                        {sec.name} ({sec.subjectCode}) - Teacher: {sec.teacherName}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label className="form-label">Day of Week *</label>
                  <select
                    className="form-select"
                    value={formData.dayOfWeek}
                    onChange={(e) => setFormData(prev => ({ ...prev, dayOfWeek: e.target.value }))}
                    required
                  >
                    {DAYS_OF_WEEK.map(d => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Start Time (HH:mm) *</label>
                  <input
                    type="time"
                    className="form-input"
                    value={formData.startTime}
                    onChange={(e) => setFormData(prev => ({ ...prev, startTime: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">End Time (HH:mm) *</label>
                  <input
                    type="time"
                    className="form-input"
                    value={formData.endTime}
                    onChange={(e) => setFormData(prev => ({ ...prev, endTime: e.target.value }))}
                    required
                  />
                </div>

                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label className="form-label">Room Number (Overrides default)</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.roomNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, roomNumber: e.target.value }))}
                    placeholder="e.g. A-101, Lab-3"
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setIsModalOpen(false)}
                  disabled={loading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                >
                  {loading ? 'Adding...' : 'Add Slot to Timetable'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
