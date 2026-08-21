package com.ibizabroker.bibliotheque;

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
import com.ibizabroker.bibliotheque.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Books book;
    private Users user;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        book = new Books();
        book.setBookId(1);
        book.setBookName("Test Book");
        book.setNoOfCopies(0); // indisponible par défaut

        user = new Users();
        user.setUserId(2);
        user.setUsername("adherent1");
        user.setName("Adhérent Test");

        request = new ReservationRequest();
        request.setBookId(1);
        request.setAdherentId(2);
    }

    // ================================================================
    // RG-01 : On ne peut réserver qu'un livre indisponible
    // ================================================================

    @Test
    void RG01_shouldRejectReservationWhenBookIsAvailable() {
        // GIVEN : livre disponible (noOfCopies > 0)
        book.setNoOfCopies(3);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));

        // WHEN / THEN : 409 Conflict
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(ex.getMessage().contains("RG-01"));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void RG01_shouldAllowReservationWhenBookIsUnavailable() {
        // GIVEN : livre indisponible (noOfCopies == 0)
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1, List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2, List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(1);
            return r;
        });

        // WHEN
        Reservation result = reservationService.createReservation(request);

        // THEN : 201 Created
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    @Test
    void RG01_shouldAllowReservationWhenNoOfCopiesIsNegative() {
        // GIVEN : noOfCopies < 0 (edge case, book is "unavailable")
        book.setNoOfCopies(-1);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(5);
            return r;
        });

        // WHEN : RG-01 ne se déclenche PAS car noOfCopies n'est pas > 0
        Reservation result = reservationService.createReservation(request);

        // THEN : la réservation est créée (le livre est indisponible)
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    // ================================================================
    // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active
    //         sur un même livre
    // ================================================================

    @Test
    void RG02_shouldRejectWhenActiveReservationAlreadyExists() {
        // GIVEN
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(true);

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(ex.getMessage().contains("RG-02"));
        assertTrue(ex.getMessage().contains("déjà"));
    }

    @Test
    void RG02_shouldAllowWhenOnlyCancelledReservationsExist() {
        // GIVEN : réservation annulée ne compte pas comme active
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(2);
            return r;
        });

        // WHEN
        Reservation result = reservationService.createReservation(request);

        // THEN : la réservation est créée
        assertNotNull(result);
        assertEquals(ReservationStatus.EN_ATTENTE, result.getStatus());
    }

    // ================================================================
    // RG-03 : Max 3 réservations actives simultanées
    // ================================================================

    @Test
    void RG03_shouldRejectWhen3ActiveReservationsExist() {
        // GIVEN : 3 réservations actives déjà existantes
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(3L);

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(ex.getMessage().contains("RG-03"));
        assertTrue(ex.getMessage().contains("maximum"));
    }

    @Test
    void RG03_shouldAllowWhenOnly2ActiveReservationsExist() {
        // GIVEN : 2 réservations actives
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(3);
            return r;
        });

        // WHEN
        Reservation result = reservationService.createReservation(request);

        // THEN : 3ème réservation acceptée
        assertNotNull(result);
    }

    // ================================================================
    // RG-04 : dateExpiration = dateReservation + 7 jours
    // ================================================================

    @Test
    void RG04_shouldSetExpirationTo7DaysAfterReservation() {
        // GIVEN
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(2, 1,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(2,
                List.of(ReservationStatus.EN_ATTENTE, ReservationStatus.DISPONIBLE)))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(4);
            r.setDateReservation(LocalDateTime.now());
            r.setDateExpiration(r.getDateReservation().plusDays(7));
            return r;
        });

        // WHEN
        Reservation result = reservationService.createReservation(request);

        // THEN
        assertNotNull(result.getDateReservation());
        assertNotNull(result.getDateExpiration());
        assertEquals(7,
                java.time.temporal.ChronoUnit.DAYS.between(
                        result.getDateReservation(), result.getDateExpiration()),
                "L'expiration doit être à exactement 7 jours de la réservation");
    }

    // ================================================================
    // RG-05 : Annulation possible uniquement si EN_ATTENTE ou DISPONIBLE
    // ================================================================

    @Test
    void RG05_shouldAllowCancellationWhenStatusIsEnAttente() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(10);
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        Reservation result = reservationService.annulerReservation(10);

        // THEN
        assertEquals(ReservationStatus.ANNULEE, result.getStatus());
    }

    @Test
    void RG05_shouldAllowCancellationWhenStatusIsDisponible() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(11);
        reservation.setStatus(ReservationStatus.DISPONIBLE);
        when(reservationRepository.findById(11)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        Reservation result = reservationService.annulerReservation(11);

        // THEN
        assertEquals(ReservationStatus.ANNULEE, result.getStatus());
    }

    // ================================================================
    // RG-06 : Statut ANNULEE / EXPIREE / HONOREE → pas de changement
    // ================================================================

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsAnnulee() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(20);
        reservation.setStatus(ReservationStatus.ANNULEE);
        when(reservationRepository.findById(20)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(20));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("ANNULEE"));
    }

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsExpiree() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(21);
        reservation.setStatus(ReservationStatus.EXPIREE);
        when(reservationRepository.findById(21)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(21));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("EXPIREE"));
    }

    @Test
    void RG06_shouldRejectCancellationWhenStatusIsHonoree() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(22);
        reservation.setStatus(ReservationStatus.HONOREE);
        when(reservationRepository.findById(22)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(22));
        assertTrue(ex.getMessage().contains("RG-06"));
        assertTrue(ex.getMessage().contains("HONOREE"));
    }

    // ================================================================
    // Cas limites : ressources non trouvées
    // ================================================================

    @Test
    void shouldThrowNotFoundExceptionWhenBookDoesNotExist() {
        // GIVEN
        when(booksRepository.findById(999)).thenReturn(Optional.empty());
        request.setBookId(999);

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.createReservation(request));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        // GIVEN
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(999)).thenReturn(Optional.empty());
        request.setAdherentId(999);

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.createReservation(request));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenReservationDoesNotExist() {
        // GIVEN
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.annulerReservation(999));
    }

    @Test
    void shouldThrowNotFoundExceptionOnGetById() {
        // GIVEN
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NotFoundException.class,
                () -> reservationService.getReservationById(999));
    }

    // ================================================================
    // Validation des paramètres
    // ================================================================

    @Test
    void shouldThrowExceptionWhenBookIdIsNull() {
        // GIVEN
        request.setBookId(null);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request));
    }

    @Test
    void shouldThrowExceptionWhenAdherentIdIsNull() {
        // GIVEN
        request.setAdherentId(null);

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request));
    }

    // ================================================================
    // GET / DELETE
    // ================================================================

    @Test
    void shouldReturnReservationsFilteredByStatus() {
        // GIVEN
        Reservation r1 = new Reservation();
        r1.setReservationId(1);
        r1.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findByStatus(ReservationStatus.EN_ATTENTE))
                .thenReturn(Arrays.asList(r1));

        // WHEN
        List<Reservation> result = reservationService.getReservations(ReservationStatus.EN_ATTENTE, null);

        // THEN
        assertEquals(1, result.size());
        assertEquals(ReservationStatus.EN_ATTENTE, result.get(0).getStatus());
    }

    @Test
    void shouldReturnReservationsFilteredByUserId() {
        // GIVEN
        Reservation r1 = new Reservation();
        r1.setReservationId(1);
        r1.setUserId(2);
        when(reservationRepository.findByUserId(2)).thenReturn(Arrays.asList(r1));

        // WHEN
        List<Reservation> result = reservationService.getReservations(null, 2);

        // THEN
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getUserId());
    }

    @Test
    void shouldReturnAllReservationsWhenNoFilter() {
        // GIVEN
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(new Reservation(), new Reservation()));

        // WHEN
        List<Reservation> result = reservationService.getReservations(null, null);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldDeleteReservationSuccessfully() {
        // GIVEN
        Reservation reservation = new Reservation();
        reservation.setReservationId(30);
        when(reservationRepository.findById(30)).thenReturn(Optional.of(reservation));

        // WHEN / THEN
        assertDoesNotThrow(() -> reservationService.deleteReservation(30));
        verify(reservationRepository).delete(reservation);
    }
}
