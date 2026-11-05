package com.mbu.attendance.service;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Student not found: " + id);
    }
}
