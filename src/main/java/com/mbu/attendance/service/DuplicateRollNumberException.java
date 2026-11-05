package com.mbu.attendance.service;

public class DuplicateRollNumberException extends RuntimeException {

    public DuplicateRollNumberException(String rollNumber) {
        super("A student with roll number " + rollNumber + " already exists");
    }
}
