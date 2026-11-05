package com.mbu.attendance.web;

import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.service.AttendanceService;
import com.mbu.attendance.service.DuplicateRollNumberException;
import com.mbu.attendance.service.StudentNotFoundException;
import com.mbu.attendance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-rendered pages: dashboard, students, mark attendance, reports.
 */
@Controller
public class WebController {

    static final String STATUS_PREFIX = "status_";

    private final StudentService studentService;
    private final AttendanceService attendanceService;

    public WebController(StudentService studentService, AttendanceService attendanceService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("today", attendanceService.today());
        model.addAttribute("stats", attendanceService.getDashboardStats());
        model.addAttribute("lowAttendance", attendanceService.getLowAttendance());
        model.addAttribute("threshold", attendanceService.getLowAttendanceThreshold());
        return "dashboard";
    }

    @GetMapping("/students")
    public String students(Model model) {
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new Student());
        }
        model.addAttribute("activePage", "students");
        model.addAttribute("students", studentService.findAll());
        return "students";
    }

    @PostMapping("/students")
    public String addStudent(@Valid @ModelAttribute("student") Student student,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirect) {
        if (!bindingResult.hasErrors()) {
            try {
                Student saved = studentService.create(student);
                redirect.addFlashAttribute("success", "Added " + saved.getName() + " (" + saved.getRollNumber() + ")");
                return "redirect:/students";
            } catch (DuplicateRollNumberException e) {
                bindingResult.rejectValue("rollNumber", "duplicate", e.getMessage());
            }
        }
        model.addAttribute("activePage", "students");
        model.addAttribute("students", studentService.findAll());
        return "students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            studentService.delete(id);
            redirect.addFlashAttribute("success", "Student removed");
        } catch (StudentNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/students";
    }

    @GetMapping("/attendance")
    public String attendanceForm(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        LocalDate selected = date != null ? date : attendanceService.today();
        model.addAttribute("activePage", "attendance");
        model.addAttribute("date", selected);
        model.addAttribute("maxDate", attendanceService.today());
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("statuses", attendanceService.getStatusesForDate(selected));
        model.addAttribute("allStatuses", AttendanceStatus.values());
        return "attendance";
    }

    @PostMapping("/attendance")
    public String saveAttendance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam Map<String, String> params,
                                 RedirectAttributes redirect) {
        try {
            Map<Long, AttendanceStatus> statuses = new LinkedHashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith(STATUS_PREFIX) && value != null && !value.isBlank()) {
                    Long studentId = Long.valueOf(key.substring(STATUS_PREFIX.length()));
                    statuses.put(studentId, AttendanceStatus.valueOf(value));
                }
            });
            int count = attendanceService.markAttendance(date, statuses);
            redirect.addFlashAttribute("success", "Saved attendance for " + count + " student(s) on " + date);
        } catch (IllegalArgumentException | StudentNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        redirect.addAttribute("date", date.toString());
        return "redirect:/attendance";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("summaries", attendanceService.getSummaries());
        model.addAttribute("threshold", attendanceService.getLowAttendanceThreshold());
        return "reports";
    }
}
