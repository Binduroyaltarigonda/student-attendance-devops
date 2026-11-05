package com.mbu.attendance.dto;

import com.mbu.attendance.model.AttendanceStatus;

import java.time.LocalDate;

/**
 * One row of the attendance sheet returned by the REST API.
 * status is null when attendance has not been marked for that student on that date.
 */
public record AttendanceEntry(
        Long studentId,
        String rollNumber,
        String name,
        LocalDate date,
        AttendanceStatus status) {
}
