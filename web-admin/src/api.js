const API_BASE = '/api';

export async function request(path, options = {}) {
  const url = `${API_BASE}${path}`;
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const errJson = await response.json();
      if (errJson.message) errorMsg = errJson.message;
      else if (errJson.error) errorMsg = errJson.error;
    } catch {
      // ignore
    }
    throw new Error(errorMsg);
  }

  // Check if response has content
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  // Lookups
  getTeachers: () => request('/admin/teachers'),
  getSubjects: () => request('/admin/subjects'),
  getStudents: (batch, department) => {
    const params = new URLSearchParams();
    if (batch) params.append('batch', batch);
    if (department) params.append('department', department);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/admin/students${query}`);
  },

  // Class Sections
  getClassSections: () => request('/admin/class-sections'),
  createClassSection: (data) =>
    request('/admin/class-sections', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Timetable Slots
  getTimetableSlots: (classSectionId) => {
    const query = classSectionId ? `?classSectionId=${classSectionId}` : '';
    return request(`/admin/timetable-slots${query}`);
  },
  createTimetableSlot: (data) =>
    request('/admin/timetable-slots', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Batch Enrollment
  batchEnroll: (classSectionId, rollNumbers) =>
    request('/admin/enrollments/batch', {
      method: 'POST',
      body: JSON.stringify({ classSectionId, rollNumbers }),
    }),

  // Lecture Session Generation
  generateSessions: (daysAhead = 30, classSectionId = null) =>
    request('/admin/sessions/generate', {
      method: 'POST',
      body: JSON.stringify({ daysAhead, classSectionId }),
    }),

  // Face Registration & Biometrics
  registerSampleFace: (studentId) =>
    request(`/admin/students/${studentId}/face-profile`, {
      method: 'POST',
    }),
  deleteFaceProfile: (studentId) =>
    request(`/admin/students/${studentId}/face-profile`, {
      method: 'DELETE',
    }),

  // System Health
  checkHealth: async () => {
    try {
      await request('/admin/subjects');
      return true;
    } catch {
      return false;
    }
  },
};
