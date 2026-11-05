package com.mbu.attendance.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentAttendanceSummaryTest {

    @Test
    void percentageIsZeroWhenNoClassesHeld() {
        assertThat(StudentAttendanceSummary.calculatePercentage(0, 0)).isEqualTo(0.0);
    }

    @Test
    void percentageIsRoundedToOneDecimal() {
        assertThat(StudentAttendanceSummary.calculatePercentage(2, 3)).isEqualTo(66.7);
        assertThat(StudentAttendanceSummary.calculatePercentage(3, 4)).isEqualTo(75.0);
        assertThat(StudentAttendanceSummary.calculatePercentage(10, 10)).isEqualTo(100.0);
    }

    @Test
    void attendedGreaterThanTotalIsRejected() {
        assertThatThrownBy(() -> StudentAttendanceSummary.calculatePercentage(5, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isBelowOnlyFlagsStudentsWithClasses() {
        StudentAttendanceSummary noClasses = new StudentAttendanceSummary(1L, "R1", "A", "MCA", 0, 0, 0);
        StudentAttendanceSummary low = new StudentAttendanceSummary(2L, "R2", "B", "MCA", 4, 2, 2);
        StudentAttendanceSummary ok = new StudentAttendanceSummary(3L, "R3", "C", "MCA", 4, 3, 1);

        assertThat(noClasses.isBelow(75)).isFalse();
        assertThat(low.isBelow(75)).isTrue();
        assertThat(ok.isBelow(75)).isFalse();
    }
}
