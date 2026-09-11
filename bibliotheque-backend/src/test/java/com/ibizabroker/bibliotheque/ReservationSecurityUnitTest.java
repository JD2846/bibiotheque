package com.ibizabroker.bibliotheque;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito uniquement, sans contexte Spring) des regles de
 * securite RS-03/RS-04/RS-05 du module reservation.
 */
@ExtendWith(MockitoExtension.class)
class ReservationSecurityUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Users adherent1;
    private Users adherent2;

    @BeforeEach
    void setUp() {
        adherent1 = new Users();
        adherent1.setUserId(2);
        adherent1.setUsername("adherent1");
        adherent1.setName("Adherent Un");

        adherent2 = new Users();
        adherent2.setUserId(3);
        adherent2.setUsername("adherent2");
        adherent2.setName("Adherent Deux");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Users user, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        // lenient : un BIBLIOTHECAIRE court-circuite checkOwnership avant tout appel a
        // findByUsername, ce stub n'est donc pas toujours consomme selon le test
        lenient().when(usersRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // ================================================================
    // RS-03 : un ADHERENT n'accede qu'a ses propres reservations
    // ================================================================

    @Test
    @DisplayName("RS-03 : un ADHERENT qui consulte la reservation d'un autre recoit AccessDeniedException")
    void RS03_shouldThrowAccessDeniedWhenAdherentAccessesAnotherUsersReservation() {
        authenticateAs(adherent2, "User");

        Reservation reservation = new Reservation();
        reservation.setReservationId(10);
        reservation.setUserId(adherent1.getUserId());
        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> reservationService.getReservationById(10));
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT peut consulter sa propre reservation")
    void RS03_shouldAllowAdherentToAccessOwnReservation() {
        authenticateAs(adherent1, "User");

        Reservation reservation = new Reservation();
        reservation.setReservationId(11);
        reservation.setUserId(adherent1.getUserId());
        when(reservationRepository.findById(11)).thenReturn(Optional.of(reservation));

        Reservation result = reservationService.getReservationById(11);

        assertNotNull(result);
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT qui annule la reservation d'un autre recoit AccessDeniedException")
    void RS03_shouldThrowAccessDeniedWhenAdherentCancelsAnotherUsersReservation() {
        authenticateAs(adherent2, "User");

        Reservation reservation = new Reservation();
        reservation.setReservationId(12);
        reservation.setUserId(adherent1.getUserId());
        reservation.setStatus(ReservationStatus.EN_ATTENTE);
        when(reservationRepository.findById(12)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> reservationService.annulerReservation(12));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("RS-03 : un BIBLIOTHECAIRE peut consulter la reservation de n'importe quel adherent")
    void RS03_shouldAllowBibliothecaireToAccessAnyReservation() {
        authenticateAs(adherent2, "Admin");

        Reservation reservation = new Reservation();
        reservation.setReservationId(13);
        reservation.setUserId(adherent1.getUserId());
        when(reservationRepository.findById(13)).thenReturn(Optional.of(reservation));

        Reservation result = reservationService.getReservationById(13);

        assertNotNull(result);
    }

    // ================================================================
    // RS-04 : un ADHERENT ne peut pas reserver au nom d'un autre adherent
    // ================================================================

    @Test
    @DisplayName("RS-04 : l'adherentId fourni par un ADHERENT est ignore, l'identite vient du token")
    void RS04_shouldForceOwnerIdToAuthenticatedAdherentIgnoringProvidedAdherentId() {
        authenticateAs(adherent1, "User");

        Books book = new Books();
        book.setBookId(1);
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(adherent1.getUserId())).thenReturn(Optional.of(adherent1));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(eq(adherent1.getUserId()), eq(1), any()))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(eq(adherent1.getUserId()), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setBookId(1);
        request.setAdherentId(adherent2.getUserId()); // tentative d'usurpation

        Reservation result = reservationService.createReservation(request);

        assertEquals(adherent1.getUserId(), result.getUserId());
        verify(usersRepository, never()).findById(adherent2.getUserId());
    }

    @Test
    @DisplayName("RS-04 : un BIBLIOTHECAIRE peut reserver au nom d'un autre adherent")
    void RS04_shouldAllowBibliothecaireToCreateReservationForAnotherAdherent() {
        Users bibliothecaire = new Users();
        bibliothecaire.setUserId(1);
        bibliothecaire.setUsername("biblio");
        authenticateAs(bibliothecaire, "Admin");

        Books book = new Books();
        book.setBookId(1);
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(adherent2.getUserId())).thenReturn(Optional.of(adherent2));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(eq(adherent2.getUserId()), eq(1), any()))
                .thenReturn(false);
        when(reservationRepository.countByUserIdAndStatusIn(eq(adherent2.getUserId()), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setBookId(1);
        request.setAdherentId(adherent2.getUserId());

        Reservation result = reservationService.createReservation(request);

        assertEquals(adherent2.getUserId(), result.getUserId());
    }

    // ================================================================
    // RS-05 : GET /api/reservations filtre par role
    // ================================================================

    @Test
    @DisplayName("RS-05 : un ADHERENT ne voit que ses propres reservations, meme s'il fournit l'id d'un autre")
    void RS05_shouldOnlyReturnOwnReservationsForAdherent() {
        authenticateAs(adherent1, "User");
        when(reservationRepository.findByUserId(adherent1.getUserId())).thenReturn(List.of(new Reservation()));

        List<Reservation> result = reservationService.getReservations(null, adherent2.getUserId());

        assertEquals(1, result.size());
        verify(reservationRepository).findByUserId(adherent1.getUserId());
        verify(reservationRepository, never()).findByUserId(adherent2.getUserId());
    }

    @Test
    @DisplayName("RS-05 : un BIBLIOTHECAIRE voit toutes les reservations, le filtre fourni est respecte")
    void RS05_shouldReturnAllReservationsForBibliothecaire() {
        Users bibliothecaire = new Users();
        bibliothecaire.setUserId(1);
        bibliothecaire.setUsername("biblio");
        authenticateAs(bibliothecaire, "Admin");
        when(reservationRepository.findByUserId(adherent2.getUserId())).thenReturn(List.of(new Reservation()));

        List<Reservation> result = reservationService.getReservations(null, adherent2.getUserId());

        assertEquals(1, result.size());
        verify(reservationRepository).findByUserId(adherent2.getUserId());
    }
}
