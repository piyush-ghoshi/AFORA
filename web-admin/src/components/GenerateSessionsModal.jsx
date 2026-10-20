import React, { useState } from 'react';
import { Sparkles, Calendar, CheckCircle2, AlertCircle, X } from 'lucide-react';

export default function GenerateSessionsModal({
  isOpen,
  onClose,
  classSections,
  onGenerate
}) {
  const [daysAhead, setDaysAhead] = useState(30);
  const [targetSectionId, setTargetSectionId] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const sectionId = targetSectionId ? parseInt(targetSectionId, 10) : null;
      const res = await onGenerate(daysAhead, sectionId);
      setResult(res);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(99, 102, 241, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary-500)',
              }}
            >
              <Sparkles size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.25rem' }}>Schedule Upcoming Lectures</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.825rem' }}>
                Batch generate dated lecture sessions from weekly timetable slots
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Informative Note */}
        <div
          style={{
            background: 'rgba(99, 102, 241, 0.08)',
            border: '1px solid rgba(99, 102, 241, 0.2)',
            borderRadius: 'var(--radius-md)',
            padding: '14px 16px',
            marginBottom: '20px',
            fontSize: '0.85rem',
            color: 'var(--text-secondary)',
            lineHeight: 1.5,
          }}
        >
          This scheduler converts recurring weekly timetable definitions (e.g. Wednesday 11:00-12:30)
          into dated calendar entries in the database. These sessions become visible on faculty Android devices
          where teachers can launch attendance sessions and perform AI face verification.
        </div>

        {result && (
          <div style={{ background: 'var(--success-bg)', border: '1px solid var(--success)', padding: '14px', borderRadius: 'var(--radius-md)', color: '#86efac', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <CheckCircle2 size={20} />
            <div>
              <div style={{ fontWeight: 700 }}>{result.message}</div>
              <div style={{ fontSize: '0.8rem' }}>Successfully created {result.generatedCount} lecture session records!</div>
            </div>
          </div>
        )}

        {error && (
          <div style={{ background: 'var(--danger-bg)', border: '1px solid var(--danger)', padding: '12px', borderRadius: 'var(--radius-md)', color: '#fca5a5', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertCircle size={20} />
            <div style={{ fontSize: '0.85rem' }}>{error}</div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '18px' }}>
            <label className="form-label">Target Class Section</label>
            <select
              className="form-select"
              value={targetSectionId}
              onChange={(e) => setTargetSectionId(e.target.value)}
            >
              <option value="">All Active Class Sections</option>
              {classSections.map(sec => (
                <option key={sec.id} value={sec.id}>
                  {sec.name} ({sec.subjectCode})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <label className="form-label">Generation Horizon (Days Ahead)</label>
              <span style={{ fontWeight: 700, color: 'var(--primary-500)', fontSize: '0.9rem' }}>
                {daysAhead} days
              </span>
            </div>
            <input
              type="range"
              min="7"
              max="90"
              step="7"
              value={daysAhead}
              onChange={(e) => setDaysAhead(parseInt(e.target.value, 10))}
              style={{ width: '100%', accentColor: 'var(--primary-500)' }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
              <span>1 Week (7d)</span>
              <span>2 Weeks (14d)</span>
              <span>1 Month (30d)</span>
              <span>3 Months (90d)</span>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={loading}
            >
              Close
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              <Sparkles size={16} />
              <span>{loading ? 'Generating...' : `Generate for Next ${daysAhead} Days`}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
