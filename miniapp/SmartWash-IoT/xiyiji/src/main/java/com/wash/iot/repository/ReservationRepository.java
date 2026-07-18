package com.wash.iot.repository;

import com.wash.iot.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationNo(String reservationNo);

    List<Reservation> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Reservation> findByDeviceIdAndReservedDateOrderByStartTime(Long deviceId, LocalDate date);

    List<Reservation> findByDeviceIdAndReservedDateAndStatusIn(Long deviceId, LocalDate date, List<String> statuses);

    List<Reservation> findByUserIdAndStatusIn(Long userId, List<String> statuses);

    List<Reservation> findByUserIdAndStatusOrderByReservationTime(Long userId, String status);

    int countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT r FROM Reservation r WHERE r.deviceId = :deviceId AND r.status = :status AND r.reservationTime BETWEEN :start AND :end")
    List<Reservation> findByDeviceIdAndStatusAndReservationTimeBetween(@Param("deviceId") Long deviceId,
                                                                          @Param("status") String status,
                                                                          @Param("start") LocalDateTime start,
                                                                          @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.reservationTime < :now")
    List<Reservation> findExpiredReservations(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.reservationTime BETWEEN :reminderTime AND :now")
    List<Reservation> findReservationsForReminder(@Param("status") String status, @Param("reminderTime") LocalDateTime reminderTime, @Param("now") LocalDateTime now);

    /**
     * 根据设备ID列表和预约时间范围统计预约数量（兼容 dashboard 使用）
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.deviceId IN :deviceIds AND r.reservationTime BETWEEN :start AND :end")
    long countByDeviceIdsAndReservationTimeBetween(@Param("deviceIds") List<Long> deviceIds,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
