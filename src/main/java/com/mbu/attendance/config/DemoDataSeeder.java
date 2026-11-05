package com.mbu.attendance.config;

import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.service.AttendanceService;
import com.mbu.attendance.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a few sample students and two weeks of attendance on first start,
 * so the dashboard is not empty during a demo. Disable with app.seed-demo-data=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final StudentService studentService;
    private final AttendanceService attendanceService;

    public DemoDataSeeder(StudentService studentService, AttendanceService attendanceService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
    }

    @Override
    public void run(String... args) {
        if (studentService.count() > 0) {
            return;
        }
        List<Student> students = List.of(
                new Student("25MCA001", "Aarav Reddy", "MCA", 2, "aarav@example.edu"),
                new Student("25MCA002", "Bhavana Naidu", "MCA", 2, "bhavana@example.edu"),
                new Student("25MCA003", "Charan Kumar", "MCA", 2, "charan@example.edu"),
                new Student("25MCA004", "Divya Sree", "MCA", 2, "divya@example.edu"),
                new Student("25MCA005", "Eshwar Rao", "MCA", 2, "eshwar@example.edu"),
                new Student("25MCA006", "Farhana Begum", "MCA", 2, "farhana@example.edu"));
        List<Student> saved = students.stream().map(studentService::create).toList();

        // Deterministic pattern: student i misses every (i + 2)th class; the last student misses often.
        LocalDate day = attendanceService.today().minusDays(14);
        int classIndex = 0;
        while (day.isBefore(attendanceService.today())) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                Map<Long, AttendanceStatus> statuses = new HashMap<>();
                for (int i = 0; i < saved.size(); i++) {
                    int gap = (i == saved.size() - 1) ? 2 : i + 3;
                    AttendanceStatus status = (classIndex % gap == 0) ? AttendanceStatus.ABSENT
                            : (classIndex % 7 == i) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                    statuses.put(saved.get(i).getId(), status);
                }
                attendanceService.markAttendance(day, statuses);
                classIndex++;
            }
            day = day.plusDays(1);
        }
        log.info("Seeded {} demo students and {} class days", saved.size(), classIndex);
    }
}
