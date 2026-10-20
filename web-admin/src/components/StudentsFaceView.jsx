import React, { useState } from 'react';
import {
  Search,
  ShieldCheck,
  ShieldAlert,
  UserCheck,
  RefreshCw,
  Trash2,
  Users,
  CheckCircle2,
  X,
  AlertCircle
} from 'lucide-react';

export default function StudentsFaceView({
  students,
  classSections,
  onEnrollSampleFace,
  onDeleteFaceProfile,
  onBatchEnroll,
  initialSectionId
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedBatch, setSelectedBatch] = useState('');
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState(false);
  const [selectedSectionId, setSelectedSectionId] = useState(initialSectionId || '');
  const [selectedRolls, setSelectedRolls] = useState([]);
  const [actionLoading, setActionLoading] = useState({});
  const [enrollResult, setEnrollResult] = useState(null);
  const [enrollError, setEnrollError] = useState(null);

  // Filter students
  const filteredStudents = students.filter(s => {
    const matchesSearch =
      s.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.rollNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.department.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesBatch = selectedBatch ? s.batch === selectedBatch : true;
    return matchesSearch && matchesBatch;
  });

  const batches = [...new Set(students.map(s => s.batch).filter(Boolean))];

  const handleEnrollFace = async (studentId) => {
    setActionLoading(prev => ({ ...prev, [studentId]: 'enroll' }));
    try {
      await onEnrollSampleFace(studentId);
    } finally {
      setActionLoading(prev => ({ ...prev, [studentId]: null }));
    }
  };

  const handleDeleteFace = async (studentId) => {
    if (!window.confirm('Are you sure you want to delete this student\'s face biometric embeddings?')) {
      return;
    }
    setActionLoading(prev => ({ ...prev, [studentId]: 'delete' }));
    try {
      await onDeleteFaceProfile(studentId);
    } finally {
      setActionLoading(prev => ({ ...prev, [studentId]: null }));
    }
  };

  const handleToggleSelectAll = () => {
    if (selectedRolls.length === filteredStudents.length) {
      setSelectedRolls([]);
    } else {
      setSelectedRolls(filteredStudents.map(s => s.rollNumber));
    }
  };

  const handleToggleStudent = (roll) => {
    setSelectedRolls(prev =>
      prev.includes(roll) ? prev.filter(r => r !== roll) : [...prev, roll]
    );
  };

  const handleExecuteBatchEnroll = async () => {
    if (!selectedSectionId) {
      setEnrollError('Please choose a target Class Section');
      return;
    }
    if (selectedRolls.length === 0) {
      setEnrollError('Please select at least one student to enroll');
      return;
    }
    setEnrollError(null);
    try {
      const res = await onBatchEnroll(parseInt(selectedSectionId, 10), selectedRolls);
      setEnrollResult(res);
      setSelectedRolls([]);
    } catch (err) {
      setEnrollError(err.message);
    }
  };

  return (
    <div>
      {/* Top Banner & Quick Batch Bar */}
      <div
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px 24px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div>
          <div style={{ fontWeight: 700, fontSize: '1.1rem' }}>
            AI Biometric Face Profiles & Batch Enrollment
          </div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Register 192-dimensional embeddings for Android face match verification or assign batches of students to class sections.
          </div>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => {
            setIsEnrollModalOpen(true);
            setEnrollResult(null);
            setEnrollError(null);
          }}
        >
          <Users size={18} />
          <span>Batch Class Enrollment</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', width: '300px' }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-input"
              style={{ width: '100%', paddingLeft: '38px' }}
              placeholder="Search by name, roll number, dept..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <select
            className="form-select"
            style={{ width: '180px' }}
            value={selectedBatch}
            onChange={(e) => setSelectedBatch(e.target.value)}
          >
            <option value="">All Batches</option>
            {batches.map(b => (
              <option key={b} value={b}>Batch {b}</option>
            ))}
          </select>
        </div>

        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
          Showing {filteredStudents.length} of {students.length} students
        </div>
      </div>

      {/* Students Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Roll Number</th>
                <th>Student Full Name</th>
                <th>Department</th>
                <th>Batch</th>
                <th>Face Biometrics</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-secondary)' }}>
                    No students match the search criteria.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((st) => {
                  const isLoading = !!actionLoading[st.id];
                  return (
                    <tr key={st.id}>
                      <td style={{ fontWeight: 700, fontFamily: 'monospace', fontSize: '0.9rem' }}>
                        {st.rollNumber}
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{st.fullName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: #{st.id}</div>
                      </td>
                      <td>{st.department}</td>
                      <td>
                        <span className="badge badge-info">{st.batch}</span>
                      </td>
                      <td>
                        {st.hasFaceProfile ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span className="badge badge-success">
                              <ShieldCheck size={14} />
                              <span>Enrolled (192-d)</span>
                            </span>
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              {st.embeddingCount} vector
                            </span>
                          </div>
                        ) : (
                          <span className="badge badge-warning">
                            <ShieldAlert size={14} />
                            <span>Pending Biometrics</span>
                          </span>
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                          {!st.hasFaceProfile ? (
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '6px 12px', fontSize: '0.8rem', color: '#a5b4fc' }}
                              onClick={() => handleEnrollFace(st.id)}
                              disabled={isLoading}
                            >
                              <UserCheck size={14} />
                              <span>{isLoading ? 'Enrolling...' : 'Quick-Enroll Face (192-d)'}</span>
                            </button>
                          ) : (
                            <button
                              className="btn btn-secondary"
                              style={{ padding: '6px 12px', fontSize: '0.8rem', color: '#fca5a5' }}
                              onClick={() => handleDeleteFace(st.id)}
                              disabled={isLoading}
                              title="Purge Face Biometric Data"
                            >
                              <Trash2 size={14} />
                              <span>{isLoading ? 'Removing...' : 'Reset Biometrics'}</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Batch Enrollment Modal */}
      {isEnrollModalOpen && (
        <div className="modal-overlay" onClick={() => setIsEnrollModalOpen(false)}>
          <div className="modal-content" style={{ maxWidth: '750px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div>
                <h3 style={{ fontSize: '1.3rem' }}>Batch Class Enrollment</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  Select students to enroll into an active class section in a single operation.
                </p>
              </div>
              <button
                onClick={() => setIsEnrollModalOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Target Class Section Dropdown */}
            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Target Class Section *</label>
              <select
                className="form-select"
                value={selectedSectionId}
                onChange={(e) => setSelectedSectionId(e.target.value)}
              >
                <option value="">Select Class Section...</option>
                {classSections.map(sec => (
                  <option key={sec.id} value={sec.id}>
                    {sec.name} ({sec.subjectCode}) - Instructor: {sec.teacherName} (Enrolled: {sec.enrolledCount}/{sec.maxStudents})
                  </option>
                ))}
              </select>
            </div>

            {/* Success / Error Banners */}
            {enrollResult && (
              <div style={{ background: 'var(--success-bg)', border: '1px solid var(--success)', padding: '14px', borderRadius: 'var(--radius-md)', color: '#86efac', marginBottom: '16px', fontSize: '0.9rem' }}>
                <div style={{ fontWeight: 700, marginBottom: '4px' }}>
                  Successfully enrolled {enrollResult.enrolledCount} students into {enrollResult.classSectionName}!
                </div>
                {enrollResult.alreadyEnrolledCount > 0 && (
                  <div style={{ fontSize: '0.8rem', color: '#bbf7d0' }}>
                    ({enrollResult.alreadyEnrolledCount} were already enrolled)
                  </div>
                )}
              </div>
            )}

            {enrollError && (
              <div style={{ background: 'var(--danger-bg)', border: '1px solid var(--danger)', padding: '12px', borderRadius: 'var(--radius-md)', color: '#fca5a5', marginBottom: '16px', fontSize: '0.85rem' }}>
                {enrollError}
              </div>
            )}

            {/* Student Checklist */}
            <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Select Students ({selectedRolls.length} selected):
              </span>
              <button
                type="button"
                className="btn btn-secondary"
                style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                onClick={handleToggleSelectAll}
              >
                {selectedRolls.length === filteredStudents.length ? 'Deselect All' : 'Select All Filtered'}
              </button>
            </div>

            <div
              style={{
                maxHeight: '260px',
                overflowY: 'auto',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '8px',
                display: 'flex',
                flexDirection: 'column',
                gap: '6px',
                background: 'var(--bg-app)',
              }}
            >
              {filteredStudents.map(st => {
                const isSelected = selectedRolls.includes(st.rollNumber);
                return (
                  <div
                    key={st.id}
                    onClick={() => handleToggleStudent(st.rollNumber)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-sm)',
                      background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'var(--bg-surface-elevated)',
                      border: isSelected ? '1px solid var(--primary-500)' : '1px solid transparent',
                      cursor: 'pointer',
                      transition: 'all var(--transition-fast)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => {}} // handled by parent div
                        style={{ accentColor: 'var(--primary-500)', width: '16px', height: '16px' }}
                      />
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{st.fullName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                          {st.rollNumber} • {st.department}
                        </div>
                      </div>
                    </div>
                    {st.hasFaceProfile ? (
                      <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>Face Verified</span>
                    ) : (
                      <span className="badge badge-warning" style={{ fontSize: '0.7rem' }}>Pending Face</span>
                    )}
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsEnrollModalOpen(false)}
              >
                Close
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleExecuteBatchEnroll}
                disabled={selectedRolls.length === 0 || !selectedSectionId}
              >
                Enroll {selectedRolls.length} Student{selectedRolls.length !== 1 ? 's' : ''}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
