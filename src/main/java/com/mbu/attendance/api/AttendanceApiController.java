package com.mbu.attendance.api;

import com.mbu.attendance.dto.AttendanceEntry;
import com.mbu.attendance.dto.DashboardStats;
import com.mbu.attendance.dto.MarkAttendanceRequest;
import com.mbu.attendance.dto.StudentAttendanceSummary;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.service.AttendanceService;
import com.mbu.attendance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * JSON API used by the smoke tests and handy for demos with curl / Postman.
 */
@RestController
@RequestMapping("/api")
public class AttendanceApiController {

    private final StudentService studentService;
    private final AttendanceService attendanceService;

    public AttendanceApiController(StudentService studentService, AttendanceService attendanceService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/students")
    public List<Student> students() {
        return studentService.findAll();
    }

    @GetMapping("/students/{id}")
    public Student student(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@Valid @RequestBody Student student) {
        return studentService.create(student);
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
    }

    @GetMapping("/attendance")
    public List<AttendanceEntry> attendance(@RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getSheet(date != null ? date : attendanceService.today());
    }

    @PostMapping("/attendance")
    public Map<String, Object> markAttendance(@Valid @RequestBody MarkAttendanceRequest request) {
        int marked = attendanceService.markAttendance(request.date(), request.statuses());
        return Map.of("date", request.date().toString(), "marked", marked);
    }

    @GetMapping("/reports/summary")
    public List<StudentAttendanceSummary> summary() {
        return attendanceService.getSummaries();
    }

    @GetMapping("/reports/dashboard")
    public DashboardStats dashboard() {
        return attendanceService.getDashboardStats();
    }
}
