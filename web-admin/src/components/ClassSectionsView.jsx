import React, { useState } from 'react';
import { Plus, Search, BookOpen, Users, MapPin, CheckCircle, X } from 'lucide-react';

export default function ClassSectionsView({
  classSections,
  subjects,
  teachers,
  onCreateSection,
  onOpenBatchEnroll
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    subjectId: '',
    teacherId: '',
    batch: '2024',
    section: 'A',
    roomNumber: 'A-101',
    maxStudents: 60
  });

  const filteredSections = classSections.filter(sec =>
    sec.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    sec.subjectCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
    sec.teacherName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.subjectId || !formData.teacherId) {
      setError('Please fill in all required fields (Name, Subject, Teacher)');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await onCreateSection({
        name: formData.name,
        subjectId: parseInt(formData.subjectId, 10),
        teacherId: parseInt(formData.teacherId, 10),
        batch: formData.batch,
        section: formData.section,
        roomNumber: formData.roomNumber || null,
        maxStudents: parseInt(formData.maxStudents, 10) || 60
      });
      setIsModalOpen(false);
      // Reset form
      setFormData({
        name: '',
        subjectId: subjects[0]?.id || '',
        teacherId: teachers[0]?.id || '',
        batch: '2024',
        section: 'A',
        roomNumber: 'A-101',
        maxStudents: 60
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubjectChange = (subjectId) => {
    const subj = subjects.find(s => s.id === parseInt(subjectId, 10));
    const sectionName = subj ? `${subj.code} Section ${formData.section}` : '';
    setFormData(prev => ({
      ...prev,
      subjectId,
      name: sectionName || prev.name
    }));
  };

  const handleSectionChange = (sectionLetter) => {
    const subj = subjects.find(s => s.id === parseInt(formData.subjectId, 10));
    const sectionName = subj ? `${subj.code} Section ${sectionLetter}` : '';
    setFormData(prev => ({
      ...prev,
      section: sectionLetter,
      name: sectionName || prev.name
    }));
  };

  return (
    <div>
      {/* Header with Search and New Button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ position: 'relative', width: '320px' }}>
          <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            className="form-input"
            style={{ width: '100%', paddingLeft: '38px' }}
            placeholder="Search by code, section or teacher..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
          <Plus size={18} />
          <span>New Class Section</span>
        </button>
      </div>

      {/* Class Sections Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Class Section</th>
                <th>Subject</th>
                <th>Assigned Teacher</th>
                <th>Room</th>
                <th>Batch</th>
                <th>Enrollment</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredSections.length === 0 ? (
                <tr>
                  <td colSpan="8" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-secondary)' }}>
                    No matching class sections found.
                  </td>
                </tr>
              ) : (
                filteredSections.map((sec) => (
                  <tr key={sec.id}>
                    <td>
                      <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{sec.name}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: #{sec.id}</div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{sec.subjectName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--primary-500)' }}>{sec.subjectCode}</div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{sec.teacherName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{sec.teacherEmployeeId}</div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <MapPin size={14} color="var(--text-secondary)" />
                        <span>{sec.roomNumber || 'TBD'}</span>
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-info">{sec.batch} (Sec {sec.section})</span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Users size={14} color="var(--text-secondary)" />
                        <span style={{ fontWeight: 600 }}>{sec.enrolledCount}</span>
                        <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>/ {sec.maxStudents}</span>
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${sec.isActive ? 'badge-success' : 'badge-danger'}`}>
                        {sec.isActive ? 'Active' : 'Archived'}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary"
                        style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                        onClick={() => onOpenBatchEnroll(sec.id)}
                      >
                        Enroll Students
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Section Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.3rem' }}>Create New Class Section</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  Assign an instructor, subject, room, and capacity
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
                  <label className="form-label">Subject *</label>
                  <select
                    className="form-select"
                    value={formData.subjectId}
                    onChange={(e) => handleSubjectChange(e.target.value)}
                    required
                  >
                    <option value="">Select Subject</option>
                    {subjects.map(s => (
                      <option key={s.id} value={s.id}>
                        {s.code} - {s.name} ({s.department})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label className="form-label">Assigned Teacher (Instructor) *</label>
                  <select
                    className="form-select"
                    value={formData.teacherId}
                    onChange={(e) => setFormData(prev => ({ ...prev, teacherId: e.target.value }))}
                    required
                  >
                    <option value="">Select Faculty Instructor</option>
                    {teachers.map(t => (
                      <option key={t.id} value={t.id}>
                        {t.fullName} ({t.employeeId}) - {t.department}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Section Identifier *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.section}
                    onChange={(e) => handleSectionChange(e.target.value.toUpperCase())}
                    placeholder="e.g. A, B, C"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Batch Year *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.batch}
                    onChange={(e) => setFormData(prev => ({ ...prev, batch: e.target.value }))}
                    placeholder="e.g. 2024"
                    required
                  />
                </div>

                <div className="form-group" style={{ gridColumn: 'span 2' }}>
                  <label className="form-label">Display Name *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.name}
                    onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                    placeholder="e.g. CS101 Section A"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Lecture Room</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.roomNumber}
                    onChange={(e) => setFormData(prev => ({ ...prev, roomNumber: e.target.value }))}
                    placeholder="e.g. A-101, Lab-2"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Max Student Capacity</label>
                  <input
                    type="number"
                    className="form-input"
                    value={formData.maxStudents}
                    onChange={(e) => setFormData(prev => ({ ...prev, maxStudents: e.target.value }))}
                    min="1"
                    max="500"
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
                  {loading ? 'Creating...' : 'Create Class Section'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
