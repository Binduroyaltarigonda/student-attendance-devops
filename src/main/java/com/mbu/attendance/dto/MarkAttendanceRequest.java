package com.mbu.attendance.dto;

import com.mbu.attendance.model.AttendanceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST request body: { "date": "2026-11-05", "statuses": { "1": "PRESENT", "2": "ABSENT" } }
 */
public record MarkAttendanceRequest(
        @NotNull(message = "date is required") LocalDate date,
        @NotEmpty(message = "statuses must contain at least one student") Map<Long, AttendanceStatus> statuses) {
}
