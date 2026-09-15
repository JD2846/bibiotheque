package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByUserId(Integer userId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByUserIdAndStatus(Integer userId, ReservationStatus status);

    long countByUserIdAndStatusIn(Integer userId, List<ReservationStatus> statuses);

    boolean existsByUserIdAndBookIdAndStatusIn(Integer userId, Integer bookId, List<ReservationStatus> statuses);
}
