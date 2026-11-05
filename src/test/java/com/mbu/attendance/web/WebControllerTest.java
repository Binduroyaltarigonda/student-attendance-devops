package com.mbu.attendance.web;

import com.mbu.attendance.model.AttendanceStatus;
import com.mbu.attendance.model.Student;
import com.mbu.attendance.service.AttendanceService;
import com.mbu.attendance.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Test
    void dashboardRenders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("build #")));
    }

    @Test
    void studentsPageRenders() throws Exception {
        studentService.create(new Student("25MCA301", "Kiran", "MCA", 2, null));

        mvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("25MCA301")));
    }

    @Test
    void addingValidStudentRedirects() throws Exception {
        mvc.perform(post("/students")
                        .param("rollNumber", "25MCA302")
                        .param("name", "Lakshmi")
                        .param("department", "MCA")
                        .param("semester", "2")
                        .param("email", "lakshmi@example.edu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attributeExists("success"));

        assertThat(studentService.findAll()).extracting(Student::getRollNumber).contains("25MCA302");
    }

    @Test
    void invalidStudentShowsErrors() throws Exception {
        mvc.perform(post("/students")
                        .param("rollNumber", "")
                        .param("name", "")
                        .param("department", "MCA")
                        .param("semester", "12"))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attributeHasFieldErrors("student", "rollNumber", "name", "semester"));
    }

    @Test
    void duplicateStudentShowsError() throws Exception {
        studentService.create(new Student("25MCA303", "Mohan", "MCA", 2, null));

        mvc.perform(post("/students")
                        .param("rollNumber", "25mca303")
                        .param("name", "Someone")
                        .param("department", "MCA")
                        .param("semester", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("student", "rollNumber"));
    }

    @Test
    void deleteStudentRedirects() throws Exception {
        Student s = studentService.create(new Student("25MCA304", "Nisha", "MCA", 2, null));

        mvc.perform(post("/students/" + s.getId() + "/delete"))
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attributeExists("success"));

        mvc.perform(post("/students/99999/delete"))
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void attendancePageAndSave() throws Exception {
        Student s = studentService.create(new Student("25MCA305", "Om", "MCA", 2, null));
        LocalDate today = attendanceService.today();

        mvc.perform(get("/attendance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("status_" + s.getId())));

        mvc.perform(post("/attendance")
                        .param("date", today.toString())
                        .param("status_" + s.getId(), "LATE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/attendance?date=" + today))
                .andExpect(flash().attributeExists("success"));

        assertThat(attendanceService.getStatusesForDate(today)).containsEntry(s.getId(), AttendanceStatus.LATE);
    }

    @Test
    void savingFutureAttendanceShowsError() throws Exception {
        Student s = studentService.create(new Student("25MCA306", "Pooja", "MCA", 2, null));
        LocalDate tomorrow = attendanceService.today().plusDays(1);

        mvc.perform(post("/attendance")
                        .param("date", tomorrow.toString())
                        .param("status_" + s.getId(), "PRESENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void reportsPageRenders() throws Exception {
        Student s = studentService.create(new Student("25MCA307", "Ravi", "MCA", 2, null));
        attendanceService.markAttendance(attendanceService.today(), java.util.Map.of(s.getId(), AttendanceStatus.ABSENT));

        mvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Attendance Report")))
                .andExpect(content().string(containsString("25MCA307")));
    }
}
