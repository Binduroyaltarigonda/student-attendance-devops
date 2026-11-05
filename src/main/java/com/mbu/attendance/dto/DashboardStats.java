package com.mbu.attendance.dto;

/**
 * Numbers shown on the dashboard cards.
 */
public record DashboardStats(
        long totalStudents,
        long classesHeld,
        long todayMarked,
        long todayPresent,
        long todayAbsent,
        long todayLate,
        double overallPercentage,
        long lowAttendanceCount) {
}
