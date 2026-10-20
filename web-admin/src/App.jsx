import React, { useState, useEffect } from 'react';
import { api } from './api';
import Sidebar from './components/Sidebar';
import Overview from './components/Overview';
import ClassSectionsView from './components/ClassSectionsView';
import TimetableView from './components/TimetableView';
import StudentsFaceView from './components/StudentsFaceView';
import GenerateSessionsModal from './components/GenerateSessionsModal';
import { RefreshCw, CheckCircle, AlertTriangle, Layers } from 'lucide-react';

export default function App() {
  const [currentTab, setCurrentTab] = useState('overview');
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Core Data
  const [teachers, setTeachers] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [students, setStudents] = useState([]);
  const [classSections, setClassSections] = useState([]);
  const [timetableSlots, setTimetableSlots] = useState([]);

  // Modals & UI States
  const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
  const [initialBatchEnrollSectionId, setInitialBatchEnrollSectionId] = useState(null);
  const [notification, setNotification] = useState(null);

  const showNotification = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => {
      setNotification(null);
    }, 4000);
  };

  const loadAllData = async () => {
    try {
      const [tList, subList, stList, secList, slotList] = await Promise.all([
        api.getTeachers(),
        api.getSubjects(),
        api.getStudents(),
        api.getClassSections(),
        api.getTimetableSlots(),
      ]);

      setTeachers(tList || []);
      setSubjects(subList || []);
      setStudents(stList || []);
      setClassSections(secList || []);
      setTimetableSlots(slotList || []);
      setIsConnected(true);
    } catch (err) {
      console.error('Failed to load data:', err);
      setIsConnected(false);
      showNotification(`Connection error: ${err.message}`, 'error');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadAllData();
    // Heartbeat every 20 seconds
    const interval = setInterval(() => {
      api.checkHealth().then(ok => setIsConnected(ok));
    }, 20000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    loadAllData();
  };

  // Handlers
  const handleCreateSection = async (data) => {
    const created = await api.createClassSection(data);
    showNotification(`Class Section "${created.name}" created successfully!`);
    await loadAllData();
  };

  const handleCreateSlot = async (data) => {
    const created = await api.createTimetableSlot(data);
    showNotification(`Recurring weekly slot added on ${created.dayOfWeekName} (${created.startTime}-${created.endTime})!`);
    await loadAllData();
  };

  const handleEnrollSampleFace = async (studentId) => {
    const updated = await api.registerSampleFace(studentId);
    showNotification(`192-d MobileFaceNet profile enrolled for ${updated.fullName} (${updated.rollNumber})!`);
    // update local student list
    setStudents(prev => prev.map(s => s.id === studentId ? updated : s));
  };

  const handleDeleteFaceProfile = async (studentId) => {
    await api.deleteFaceProfile(studentId);
    showNotification(`Biometric profile purged for student #${studentId}`);
    setStudents(prev => prev.map(s => s.id === studentId ? { ...s, hasFaceProfile: false, embeddingCount: 0 } : s));
  };

  const handleBatchEnroll = async (classSectionId, rollNumbers) => {
    const res = await api.batchEnroll(classSectionId, rollNumbers);
    showNotification(`Batch enrolled ${res.enrolledCount} students into ${res.classSectionName}!`);
    await loadAllData();
    return res;
  };

  const handleGenerateSessions = async (daysAhead, classSectionId) => {
    const res = await api.generateSessions(daysAhead, classSectionId);
    showNotification(`Generated ${res.generatedCount} lecture sessions for the next ${daysAhead} days!`);
    await loadAllData();
    return res;
  };

  const handleOpenBatchEnroll = (sectionId) => {
    setInitialBatchEnrollSectionId(sectionId);
    setCurrentTab('students');
  };

  const getPageTitle = () => {
    switch (currentTab) {
      case 'classes': return { title: 'Class Sections Studio', subtitle: 'Course offerings, room assignments, and faculty instructor bindings' };
      case 'timetable': return { title: 'Weekly Timetable Matrix', subtitle: 'Recurring lecture schedule matrix across weekdays' };
      case 'students': return { title: 'Students & Biometrics', subtitle: 'MobileFaceNet biometric face profiles and class section enrollment' };
      default: return { title: 'Dashboard Overview', subtitle: 'Unified academic operations and biometric system monitoring' };
    }
  };

  const pageInfo = getPageTitle();

  return (
    <div className="app-container">
      {/* Toast Notification */}
      {notification && (
        <div
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            background: notification.type === 'error' ? 'var(--danger-bg)' : 'var(--bg-surface-elevated)',
            border: `1px solid ${notification.type === 'error' ? 'var(--danger)' : 'var(--success)'}`,
            borderRadius: 'var(--radius-md)',
            padding: '14px 20px',
            boxShadow: 'var(--shadow-lg)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            zIndex: 100,
            maxWidth: '420px',
            animation: 'modalFadeIn 200ms ease-out',
          }}
        >
          {notification.type === 'error' ? (
            <AlertTriangle size={20} color="var(--danger)" />
          ) : (
            <CheckCircle size={20} color="var(--success)" />
          )}
          <span style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>{notification.msg}</span>
        </div>
      )}

      {/* Sidebar */}
      <Sidebar
        currentTab={currentTab}
        setCurrentTab={(tab) => {
          setInitialBatchEnrollSectionId(null);
          setCurrentTab(tab);
        }}
        isConnected={isConnected}
        onOpenGenerateModal={() => setIsGenerateModalOpen(true)}
      />

      {/* Main Content */}
      <main className="main-content">
        {/* Top Bar */}
        <header className="top-bar">
          <div>
            <div className="page-title">{pageInfo.title}</div>
            <div className="page-subtitle">{pageInfo.subtitle}</div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button
              className="btn btn-secondary"
              onClick={handleRefresh}
              disabled={refreshing}
              title="Refresh Data"
            >
              <RefreshCw size={16} className={refreshing ? 'spin' : ''} />
              <span>{refreshing ? 'Refreshing...' : 'Sync'}</span>
            </button>
          </div>
        </header>

        {/* Content Body */}
        <div className="content-body">
          {loading ? (
            <div style={{ textAlign: 'center', padding: '100px 0', color: 'var(--text-secondary)' }}>
              <div style={{ fontSize: '1.2rem', marginBottom: '8px' }}>Connecting to AFORA Core API...</div>
              <p style={{ fontSize: '0.85rem' }}>Loading teachers, timetable, and enrollment matrices.</p>
            </div>
          ) : (
            <>
              {currentTab === 'overview' && (
                <Overview
                  classSections={classSections}
                  teachers={teachers}
                  students={students}
                  timetableSlots={timetableSlots}
                  onOpenGenerateModal={() => setIsGenerateModalOpen(true)}
                  onNavigateToClasses={() => setCurrentTab('classes')}
                  onNavigateToTimetable={() => setCurrentTab('timetable')}
                  onNavigateToStudents={() => setCurrentTab('students')}
                />
              )}

              {currentTab === 'classes' && (
                <ClassSectionsView
                  classSections={classSections}
                  subjects={subjects}
                  teachers={teachers}
                  onCreateSection={handleCreateSection}
                  onOpenBatchEnroll={handleOpenBatchEnroll}
                />
              )}

              {currentTab === 'timetable' && (
                <TimetableView
                  timetableSlots={timetableSlots}
                  classSections={classSections}
                  onCreateSlot={handleCreateSlot}
                />
              )}

              {currentTab === 'students' && (
                <StudentsFaceView
                  students={students}
                  classSections={classSections}
                  onEnrollSampleFace={handleEnrollSampleFace}
                  onDeleteFaceProfile={handleDeleteFaceProfile}
                  onBatchEnroll={handleBatchEnroll}
                  initialSectionId={initialBatchEnrollSectionId}
                />
              )}
            </>
          )}
        </div>
      </main>

      {/* Generate Sessions Modal */}
      <GenerateSessionsModal
        isOpen={isGenerateModalOpen}
        onClose={() => setIsGenerateModalOpen(false)}
        classSections={classSections}
        onGenerate={handleGenerateSessions}
      />
    </div>
  );
}
