package com.ibizabroker.bibliotheque;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.BorrowRequest;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.impl.BorrowServiceImpl;
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
 * securite EMP-04/EMP-05 du module emprunt.
 */
@ExtendWith(MockitoExtension.class)
class BorrowSecurityUnitTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private BorrowServiceImpl borrowService;

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
        lenient().when(usersRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // ================================================================
    // EMP-04 : userId force pour un ADHERENT lors de l'emprunt
    // ================================================================

    @Test
    @DisplayName("EMP-04 : le userId fourni par un ADHERENT est ignore, l'identite vient du token")
    void EMP04_shouldForceOwnerIdToAuthenticatedAdherentIgnoringProvidedUserId() {
        authenticateAs(adherent1, "User");

        Books book = new Books();
        book.setBookId(1);
        book.setNoOfCopies(3);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(adherent1.getUserId())).thenReturn(Optional.of(adherent1));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(adherent1.getUserId(), 1)).thenReturn(false);
        when(borrowRepository.countByUserIdAndReturnDateIsNull(adherent1.getUserId())).thenReturn(0L);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(1);
        request.setUserId(adherent2.getUserId()); // tentative d'usurpation

        Borrow result = borrowService.borrowBook(request);

        assertEquals(adherent1.getUserId(), result.getUserId());
        verify(usersRepository, never()).findById(adherent2.getUserId());
    }

    @Test
    @DisplayName("EMP-04 : un BIBLIOTHECAIRE peut emprunter au nom d'un autre adherent")
    void EMP04_shouldAllowBibliothecaireToBorrowForAnotherAdherent() {
        Users bibliothecaire = new Users();
        bibliothecaire.setUserId(1);
        bibliothecaire.setUsername("biblio");
        authenticateAs(bibliothecaire, "Admin");

        Books book = new Books();
        book.setBookId(1);
        book.setNoOfCopies(3);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(adherent2.getUserId())).thenReturn(Optional.of(adherent2));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(adherent2.getUserId(), 1)).thenReturn(false);
        when(borrowRepository.countByUserIdAndReturnDateIsNull(adherent2.getUserId())).thenReturn(0L);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(1);
        request.setUserId(adherent2.getUserId());

        Borrow result = borrowService.borrowBook(request);

        assertEquals(adherent2.getUserId(), result.getUserId());
    }

    // ================================================================
    // EMP-05 : ownership sur le retour + filtrage des consultations
    // ================================================================

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui rend l'emprunt d'un autre recoit AccessDeniedException")
    void EMP05_shouldThrowAccessDeniedWhenAdherentReturnsAnotherUsersBorrow() {
        authenticateAs(adherent2, "User");

        Borrow borrow = new Borrow();
        borrow.setBorrowId(10);
        borrow.setUserId(adherent1.getUserId());
        when(borrowRepository.findById(10)).thenReturn(Optional.of(borrow));

        assertThrows(AccessDeniedException.class, () -> borrowService.returnBook(10));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT peut rendre son propre emprunt")
    void EMP05_shouldAllowAdherentToReturnOwnBorrow() {
        authenticateAs(adherent1, "User");

        Books book = new Books();
        book.setBookId(1);
        book.setNoOfCopies(0);
        Borrow borrow = new Borrow();
        borrow.setBorrowId(11);
        borrow.setBookId(1);
        borrow.setUserId(adherent1.getUserId());
        when(borrowRepository.findById(11)).thenReturn(Optional.of(borrow));
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow result = borrowService.returnBook(11);

        assertNotNull(result.getReturnDate());
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui consulte l'historique d'un autre recoit AccessDeniedException")
    void EMP05_shouldThrowAccessDeniedWhenAdherentReadsAnotherUsersHistory() {
        authenticateAs(adherent1, "User");

        assertThrows(AccessDeniedException.class, () -> borrowService.getBorrowsByUser(adherent2.getUserId()));
    }

    @Test
    @DisplayName("EMP-05 : un ADHERENT qui consulte l'historique d'un livre recoit AccessDeniedException")
    void EMP05_shouldThrowAccessDeniedWhenAdherentReadsBookHistory() {
        authenticateAs(adherent1, "User");

        assertThrows(AccessDeniedException.class, () -> borrowService.getBorrowsByBook(1));
    }

    @Test
    @DisplayName("EMP-05 : GET /borrow ne renvoie que ses propres emprunts pour un ADHERENT")
    void EMP05_shouldOnlyReturnOwnBorrowsForAdherent() {
        authenticateAs(adherent1, "User");
        when(borrowRepository.findByUserId(adherent1.getUserId())).thenReturn(List.of(new Borrow()));

        List<Borrow> result = borrowService.getBorrows();

        assertEquals(1, result.size());
        verify(borrowRepository).findByUserId(adherent1.getUserId());
        verify(borrowRepository, never()).findAll();
    }

    @Test
    @DisplayName("EMP-05 : un BIBLIOTHECAIRE peut consulter l'historique de n'importe quel adherent")
    void EMP05_shouldAllowBibliothecaireToReadAnyUsersHistory() {
        authenticateAs(adherent2, "Admin");
        when(borrowRepository.findByUserId(eq(adherent1.getUserId()))).thenReturn(List.of(new Borrow()));

        List<Borrow> result = borrowService.getBorrowsByUser(adherent1.getUserId());

        assertEquals(1, result.size());
    }
}
