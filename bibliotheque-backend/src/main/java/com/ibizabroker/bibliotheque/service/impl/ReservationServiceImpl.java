package com.ibizabroker.bibliotheque.service.impl;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.IReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ReservationServiceImpl implements IReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            Arrays.asList(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public Reservation createReservation(ReservationRequest request) {
        // Validation des paramètres obligatoires
        if (request.getBookId() == null || request.getAdherentId() == null) {
            throw new IllegalArgumentException("bookId et adherentId sont obligatoires");
        }

        // Vérifier que le livre existe
        Books book = booksRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre non trouvé avec l'id: " + request.getBookId()));

        // Vérifier que l'utilisateur existe
        Users user = usersRepository.findById(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé avec l'id: " + request.getAdherentId()));

        // RG-01 : On ne peut réserver qu'un livre indisponible
        if (book.getNoOfCopies() > 0) {
            throw new ConflictException("RG-01: Impossible de réserver un livre disponible. Exemplaires disponibles: " + book.getNoOfCopies());
        }

        // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre
        boolean hasActiveReservation = reservationRepository
                .existsByUserIdAndBookIdAndStatusIn(request.getAdherentId(), request.getBookId(), ACTIVE_STATUSES);
        if (hasActiveReservation) {
            throw new ConflictException("RG-02: Vous avez déjà une réservation active pour ce livre");
        }

        // RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées
        long activeCount = reservationRepository.countByUserIdAndStatusIn(request.getAdherentId(), ACTIVE_STATUSES);
        if (activeCount >= 3) {
            throw new ConflictException("RG-03: Vous avez atteint le nombre maximum de réservations actives (3)");
        }

        // Créer la réservation (RG-04 géré par @PrePersist dans l'entité)
        Reservation reservation = new Reservation();
        reservation.setBookId(request.getBookId());
        reservation.setUserId(request.getAdherentId());
        reservation.setStatus(ReservationStatus.EN_ATTENTE);

        return reservationRepository.save(reservation);
    }

    @Override
    public List<Reservation> getReservations(ReservationStatus status, Integer userId) {
        if (status != null && userId != null) {
            return reservationRepository.findByUserIdAndStatus(userId, status);
        } else if (status != null) {
            return reservationRepository.findByStatus(status);
        } else if (userId != null) {
            return reservationRepository.findByUserId(userId);
        }
        return reservationRepository.findAll();
    }

    @Override
    public Reservation getReservationById(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation non trouvée avec l'id: " + id));
    }

    @Override
    public Reservation annulerReservation(Integer id) {
        Reservation reservation = getReservationById(id);

        // RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état
        // RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE
        if (reservation.getStatus() == ReservationStatus.ANNULEE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est ANNULEE");
        }
        if (reservation.getStatus() == ReservationStatus.EXPIREE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est EXPIREE");
        }
        if (reservation.getStatus() == ReservationStatus.HONOREE) {
            throw new ConflictException("RG-06: Cette réservation ne peut pas être annulée car son statut est HONOREE");
        }

        reservation.setStatus(ReservationStatus.ANNULEE);
        return reservationRepository.save(reservation);
    }

    @Override
    public void deleteReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation non trouvée avec l'id: " + id));
        reservationRepository.delete(reservation);
    }
}
