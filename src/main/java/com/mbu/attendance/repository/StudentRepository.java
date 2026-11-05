package com.mbu.attendance.repository;

import com.mbu.attendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRollNumberIgnoreCase(String rollNumber);

    List<Student> findAllByOrderByRollNumberAsc();
}
