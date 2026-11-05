package com.mbu.attendance.service;

import com.mbu.attendance.dto.AttendanceEntry;
import com.mbu.attendance.dto.DashboardStats;
import com.mbu.attendance.dto.StudentAttendanceSummary;
import com.mbu.attendance.model.AttendanceRecord;
import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.repository.AttendanceRecordRepository;
import com.mbu.attendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final double lowAttendanceThreshold;
    private final Clock clock;

    @Autowired
    public AttendanceService(StudentRepository studentRepository,
                             AttendanceRecordRepository attendanceRepository,
                             @Value("${attendance.low-threshold:75}") double lowAttendanceThreshold) {
        this(studentRepository, attendanceRepository, lowAttendanceThreshold, Clock.systemDefaultZone());
    }

    AttendanceService(StudentRepository studentRepository,
                      AttendanceRecordRepository attendanceRepository,
                      double lowAttendanceThreshold,
                      Clock clock) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.lowAttendanceThreshold = lowAttendanceThreshold;
        this.clock = clock;
    }

    public double getLowAttendanceThreshold() {
        return lowAttendanceThreshold;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Marks (or updates) attendance for several students on one date.
     *
     * @return number of records written
     */
    public int markAttendance(LocalDate date, Map<Long, AttendanceStatus> statuses) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (date.isAfter(today())) {
            throw new IllegalArgumentException("Attendance cannot be marked for a future date");
        }
        int written = 0;
        for (Map.Entry<Long, AttendanceStatus> entry : statuses.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Long studentId = entry.getKey();
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException(studentId));
            AttendanceRecord record = attendanceRepository.findByStudentIdAndDate(studentId, date)
                    .orElseGet(() -> new AttendanceRecord(student, date, entry.getValue()));
            record.setStatus(entry.getValue());
            attendanceRepository.save(record);
            written++;
        }
        return written;
    }

    @Transactional(readOnly = true)
    public Map<Long, AttendanceStatus> getStatusesForDate(LocalDate date) {
        return attendanceRepository.findByDate(date).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), AttendanceRecord::getStatus));
    }

    @Transactional(readOnly = true)
    public List<AttendanceEntry> getSheet(LocalDate date) {
        Map<Long, AttendanceStatus> statuses = getStatusesForDate(date);
        List<AttendanceEntry> sheet = new ArrayList<>();
        for (Student s : studentRepository.findAllByOrderByRollNumberAsc()) {
            sheet.add(new AttendanceEntry(s.getId(), s.getRollNumber(), s.getName(), date, statuses.get(s.getId())));
        }
        return sheet;
    }

    /** Per-student totals, ordered by roll number. */
    @Transactional(readOnly = true)
    public List<StudentAttendanceSummary> getSummaries() {
        Map<Long, long[]> counts = new HashMap<>(); // [total, attended]
        for (AttendanceRecord r : attendanceRepository.findAll()) {
            long[] c = counts.computeIfAbsent(r.getStudent().getId(), k -> new long[2]);
            c[0]++;
            if (r.getStatus().countsAsPresent()) {
                c[1]++;
            }
        }
        List<StudentAttendanceSummary> result = new ArrayList<>();
        for (Student s : studentRepository.findAllByOrderByRollNumberAsc()) {
            long[] c = counts.getOrDefault(s.getId(), new long[2]);
            result.add(new StudentAttendanceSummary(
                    s.getId(), s.getRollNumber(), s.getName(), s.getDepartment(), c[0], c[1], c[0] - c[1]));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceSummary> getLowAttendance() {
        return getSummaries().stream()
                .filter(s -> s.isBelow(lowAttendanceThreshold))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        List<StudentAttendanceSummary> summaries = getSummaries();
        long totalRecords = summaries.stream().mapToLong(StudentAttendanceSummary::totalClasses).sum();
        long totalAttended = summaries.stream().mapToLong(StudentAttendanceSummary::attended).sum();
        long low = summaries.stream().filter(s -> s.isBelow(lowAttendanceThreshold)).count();

        Map<AttendanceStatus, Long> today = attendanceRepository.findByDate(today()).stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));
        long present = today.getOrDefault(AttendanceStatus.PRESENT, 0L);
        long absent = today.getOrDefault(AttendanceStatus.ABSENT, 0L);
        long late = today.getOrDefault(AttendanceStatus.LATE, 0L);

        return new DashboardStats(
                summaries.size(),
                attendanceRepository.countDistinctDates(),
                present + absent + late,
                present,
                absent,
                late,
                StudentAttendanceSummary.calculatePercentage(totalAttended, totalRecords),
                low);
    }
}
