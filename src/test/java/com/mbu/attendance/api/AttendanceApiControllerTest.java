package com.mbu.attendance.api;

import com.mbu.attendance.model.Student;
import com.mbu.attendance.service.AttendanceService;
import com.mbu.attendance.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttendanceApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Test
    void createAndListStudents() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rollNumber":"25mca401","name":"Sana","department":"MCA","semester":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNumber").value("25MCA401"));

        mvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sana"));
    }

    @Test
    void invalidStudentReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rollNumber\":\"\",\"name\":\"X\",\"department\":\"MCA\",\"semester\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.rollNumber").exists());
    }

    @Test
    void duplicateStudentReturns409() throws Exception {
        studentService.create(new Student("25MCA402", "Tara", "MCA", 2, null));

        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rollNumber\":\"25MCA402\",\"name\":\"X\",\"department\":\"MCA\",\"semester\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownStudentReturns404() throws Exception {
        mvc.perform(get("/api/students/424242"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found: 424242"));
    }

    @Test
    void getAndDeleteStudent() throws Exception {
        Student s = studentService.create(new Student("25MCA405", "Usha", "MCA", 2, null));

        mvc.perform(get("/api/students/" + s.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("25MCA405"));

        mvc.perform(delete("/api/students/" + s.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAttendanceAndReadSummary() throws Exception {
        Student s = studentService.create(new Student("25MCA403", "Uma", "MCA", 2, null));
        LocalDate today = attendanceService.today();

        mvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + today + "\",\"statuses\":{\"" + s.getId() + "\":\"PRESENT\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(1));

        mvc.perform(get("/api/attendance").param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PRESENT"));

        mvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].percentage").value(100.0));

        mvc.perform(get("/api/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(1));
    }

    @Test
    void futureAttendanceReturns400() throws Exception {
        Student s = studentService.create(new Student("25MCA404", "Vani", "MCA", 2, null));
        LocalDate tomorrow = attendanceService.today().plusDays(1);

        mvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + tomorrow + "\",\"statuses\":{\"" + s.getId() + "\":\"PRESENT\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        mvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
