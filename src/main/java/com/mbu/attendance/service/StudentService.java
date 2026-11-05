package com.mbu.attendance.service;

import com.mbu.attendance.model.Student;
import com.mbu.attendance.repository.AttendanceRecordRepository;
import com.mbu.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;

    public StudentService(StudentRepository studentRepository,
                          AttendanceRecordRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAllByOrderByRollNumberAsc();
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public long count() {
        return studentRepository.count();
    }

    /**
     * Saves a new student. Roll numbers are trimmed and upper-cased so "25mca01" and "25MCA01" clash.
     */
    public Student create(Student student) {
        String roll = normalizeRollNumber(student.getRollNumber());
        if (studentRepository.existsByRollNumberIgnoreCase(roll)) {
            throw new DuplicateRollNumberException(roll);
        }
        student.setId(null);
        student.setRollNumber(roll);
        student.setName(student.getName().trim());
        student.setDepartment(student.getDepartment().trim());
        if (student.getEmail() != null && student.getEmail().isBlank()) {
            student.setEmail(null);
        }
        return studentRepository.save(student);
    }

    /** Deletes a student together with all of their attendance records. */
    public void delete(Long id) {
        Student student = findById(id);
        attendanceRepository.deleteAllForStudent(student.getId());
        studentRepository.delete(student);
    }

    static String normalizeRollNumber(String rollNumber) {
        if (rollNumber == null || rollNumber.isBlank()) {
            throw new IllegalArgumentException("Roll number is required");
        }
        return rollNumber.trim().toUpperCase(Locale.ROOT);
    }
}
