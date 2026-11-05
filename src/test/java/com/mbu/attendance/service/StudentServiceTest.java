package com.mbu.attendance.service;

import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.repository.AttendanceRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRecordRepository attendanceRepository;

    @Test
    void createNormalisesRollNumberAndBlankEmail() {
        Student saved = studentService.create(new Student("  25mca101 ", " Ravi ", " MCA ", 2, " "));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRollNumber()).isEqualTo("25MCA101");
        assertThat(saved.getName()).isEqualTo("Ravi");
        assertThat(saved.getDepartment()).isEqualTo("MCA");
        assertThat(saved.getEmail()).isNull();
    }

    @Test
    void duplicateRollNumberIsRejectedIgnoringCase() {
        studentService.create(new Student("25MCA102", "Sita", "MCA", 2, null));

        assertThatThrownBy(() -> studentService.create(new Student("25mca102", "Other", "MCA", 2, null)))
                .isInstanceOf(DuplicateRollNumberException.class)
                .hasMessageContaining("25MCA102");
    }

    @Test
    void findAllIsSortedByRollNumber() {
        studentService.create(new Student("25MCA199", "Zed", "MCA", 2, null));
        studentService.create(new Student("25MCA150", "Amy", "MCA", 2, null));

        assertThat(studentService.findAll())
                .extracting(Student::getRollNumber)
                .containsExactly("25MCA150", "25MCA199");
    }

    @Test
    void unknownStudentThrowsNotFound() {
        assertThatThrownBy(() -> studentService.findById(9999L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void deleteRemovesStudentAndAttendance() {
        Student s = studentService.create(new Student("25MCA103", "Gita", "MCA", 2, null));
        attendanceService.markAttendance(LocalDate.now().minusDays(1), Map.of(s.getId(), AttendanceStatus.PRESENT));
        assertThat(attendanceRepository.count()).isEqualTo(1);

        studentService.delete(s.getId());

        assertThat(studentService.count()).isZero();
        assertThat(attendanceRepository.count()).isZero();
    }

    @Test
    void blankRollNumberIsRejected() {
        assertThatThrownBy(() -> StudentService.normalizeRollNumber("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
