package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;

import java.util.List;

public interface IReservationService {

    Reservation createReservation(ReservationRequest request);

    List<Reservation> getReservations(ReservationStatus status, Integer userId);

    Reservation getReservationById(Integer id);

    Reservation annulerReservation(Integer id);

    void deleteReservation(Integer id);
}
