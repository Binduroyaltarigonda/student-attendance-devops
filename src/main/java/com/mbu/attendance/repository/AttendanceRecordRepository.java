package com.mbu.attendance.repository;

import com.mbu.attendance.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByDate(LocalDate date);

    Optional<AttendanceRecord> findByStudentIdAndDate(Long studentId, LocalDate date);

    @Query("select count(distinct a.date) from AttendanceRecord a")
    long countDistinctDates();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AttendanceRecord a where a.student.id = :studentId")
    void deleteAllForStudent(@Param("studentId") Long studentId);
}
