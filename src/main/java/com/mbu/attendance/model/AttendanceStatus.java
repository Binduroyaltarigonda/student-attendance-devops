package com.mbu.attendance.model;

/**
 * Status a student can be given for one class day.
 * LATE still counts towards the attendance percentage.
 */
public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE;

    public boolean countsAsPresent() {
        return this != ABSENT;
    }
}
