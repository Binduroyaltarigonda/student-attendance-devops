package com.mbu.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Attendance totals for one student across every recorded class day.
 */
public record StudentAttendanceSummary(
        Long studentId,
        String rollNumber,
        String name,
        String department,
        long totalClasses,
        long attended,
        long absent) {

    /** Attendance percentage rounded to one decimal place (0.0 when no classes recorded). */
    @JsonProperty("percentage")
    public double percentage() {
        return calculatePercentage(attended, totalClasses);
    }

    /** True when the student has at least one class recorded and is under the given threshold. */
    public boolean isBelow(double threshold) {
        return totalClasses > 0 && percentage() < threshold;
    }

    public static double calculatePercentage(long attended, long total) {
        if (total <= 0) {
            return 0.0;
        }
        if (attended < 0 || attended > total) {
            throw new IllegalArgumentException("attended must be between 0 and total");
        }
        return Math.round(attended * 1000.0 / total) / 10.0;
    }
}
