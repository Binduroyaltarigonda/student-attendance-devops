package com.mbu.attendance.service;

import com.mbu.attendance.dto.AttendanceEntry;
import com.mbu.attendance.dto.DashboardStats;
import com.mbu.attendance.dto.StudentAttendanceSummary;
import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AttendanceServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    private Student asha;
    private Student bala;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        asha = studentService.create(new Student("25MCA201", "Asha", "MCA", 2, null));
        bala = studentService.create(new Student("25MCA202", "Bala", "MCA", 2, null));
        today = attendanceService.today();
    }

    @Test
    void marksAttendanceForSeveralStudents() {
        int written = attendanceService.markAttendance(today, Map.of(
                asha.getId(), AttendanceStatus.PRESENT,
                bala.getId(), AttendanceStatus.ABSENT));

        assertThat(written).isEqualTo(2);
        assertThat(attendanceService.getStatusesForDate(today))
                .containsEntry(asha.getId(), AttendanceStatus.PRESENT)
                .containsEntry(bala.getId(), AttendanceStatus.ABSENT);
    }

    @Test
    void markingTwiceUpdatesInsteadOfDuplicating() {
        attendanceService.markAttendance(today, Map.of(asha.getId(), AttendanceStatus.ABSENT));
        attendanceService.markAttendance(today, Map.of(asha.getId(), AttendanceStatus.LATE));

        assertThat(attendanceService.getStatusesForDate(today))
                .hasSize(1)
                .containsEntry(asha.getId(), AttendanceStatus.LATE);
    }

    @Test
    void futureDatesAreRejected() {
        assertThatThrownBy(() -> attendanceService.markAttendance(today.plusDays(1),
                Map.of(asha.getId(), AttendanceStatus.PRESENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void missingDateIsRejected() {
        assertThatThrownBy(() -> attendanceService.markAttendance(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownStudentIsRejected() {
        assertThatThrownBy(() -> attendanceService.markAttendance(today, Map.of(12345L, AttendanceStatus.PRESENT)))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void summaryCountsLateAsAttended() {
        attendanceService.markAttendance(today.minusDays(3), Map.of(asha.getId(), AttendanceStatus.PRESENT, bala.getId(), AttendanceStatus.ABSENT));
        attendanceService.markAttendance(today.minusDays(2), Map.of(asha.getId(), AttendanceStatus.LATE, bala.getId(), AttendanceStatus.ABSENT));
        attendanceService.markAttendance(today.minusDays(1), Map.of(asha.getId(), AttendanceStatus.ABSENT, bala.getId(), AttendanceStatus.PRESENT));

        List<StudentAttendanceSummary> summaries = attendanceService.getSummaries();
        StudentAttendanceSummary a = summaries.get(0);
        StudentAttendanceSummary b = summaries.get(1);

        assertThat(a.rollNumber()).isEqualTo("25MCA201");
        assertThat(a.totalClasses()).isEqualTo(3);
        assertThat(a.attended()).isEqualTo(2);
        assertThat(a.percentage()).isEqualTo(66.7);
        assertThat(b.attended()).isEqualTo(1);
        assertThat(b.absent()).isEqualTo(2);

        assertThat(attendanceService.getLowAttendance()).hasSize(2);
    }

    @Test
    void sheetListsUnmarkedStudentsWithNullStatus() {
        attendanceService.markAttendance(today, Map.of(asha.getId(), AttendanceStatus.PRESENT));

        List<AttendanceEntry> sheet = attendanceService.getSheet(today);

        assertThat(sheet).hasSize(2);
        assertThat(sheet.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(sheet.get(1).status()).isNull();
    }

    @Test
    void dashboardStatsReflectToday() {
        attendanceService.markAttendance(today.minusDays(1), Map.of(asha.getId(), AttendanceStatus.PRESENT, bala.getId(), AttendanceStatus.PRESENT));
        attendanceService.markAttendance(today, Map.of(asha.getId(), AttendanceStatus.LATE, bala.getId(), AttendanceStatus.ABSENT));

        DashboardStats stats = attendanceService.getDashboardStats();

        assertThat(stats.totalStudents()).isEqualTo(2);
        assertThat(stats.classesHeld()).isEqualTo(2);
        assertThat(stats.todayMarked()).isEqualTo(2);
        assertThat(stats.todayLate()).isEqualTo(1);
        assertThat(stats.todayAbsent()).isEqualTo(1);
        assertThat(stats.todayPresent()).isZero();
        assertThat(stats.overallPercentage()).isEqualTo(75.0);
        assertThat(stats.lowAttendanceCount()).isEqualTo(1);
    }
}
