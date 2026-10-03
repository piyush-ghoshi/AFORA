-- Migration V8: Extend the seeded session window forward
-- Phase A8.4 follow-up.
--
-- V6 generated sessions only for CURRENT_DATE - 13 .. CURRENT_DATE, so every
-- row landed in the past and was marked FINALIZED. That left the teacher
-- dashboard ("today's schedule") permanently empty and gave the app no
-- upcoming classes to show. This adds the forward half of the window.
--
-- Future sessions carry no attendance records, which is correct: they have not
-- happened. Because the student attendance percentage is derived from
-- attendance_records rather than session counts, adding these rows must not
-- change any existing percentage.

INSERT INTO lecture_sessions (
    class_section_id, timetable_slot_id, semester_id, session_date,
    start_time, end_time, room_number, status, total_students
)
SELECT
    ts.class_section_id,
    ts.id,
    cs.semester_id,
    d.session_date,
    ts.start_time,
    ts.end_time,
    ts.room_number,
    'SCHEDULED',
    (SELECT COUNT(*) FROM enrollments e
      WHERE e.class_section_id = ts.class_section_id AND e.status = 'ACTIVE')
FROM timetable_slots ts
JOIN class_sections cs ON cs.id = ts.class_section_id
CROSS JOIN (
    SELECT (CURRENT_DATE + offs)::date AS session_date
    FROM generate_series(0, 13) AS offs
) AS d
WHERE EXTRACT(ISODOW FROM d.session_date) = ts.day_of_week
ON CONFLICT (class_section_id, session_date, start_time) DO NOTHING;