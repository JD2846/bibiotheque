package com.ibizabroker.bibliotheque;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.BorrowRequest;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceUnitTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private Books book;
    private Users user;
    private BorrowRequest request;

    @BeforeEach
    void setUp() {
        book = new Books();
        book.setBookId(1);
        book.setBookName("Test Book");
        book.setNoOfCopies(3); // disponible par défaut

        user = new Users();
        user.setUserId(2);
        user.setUsername("adherent1");
        user.setName("Adhérent Test");

        request = new BorrowRequest();
        request.setBookId(1);
        request.setUserId(2);

        // Identite par defaut : BIBLIOTHECAIRE (role Admin), voir BorrowSecurityUnitTest.java
        // pour les tests dedies aux regles de securite (EMP-04/EMP-05).
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bibliothecaire-test", null,
                        List.of(new SimpleGrantedAuthority("ROLE_Admin"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ================================================================
    // EMP-01 : le livre doit avoir un exemplaire disponible
    // ================================================================

    @Test
    void EMP01_shouldRejectBorrowWhenBookOutOfStock() {
        book.setNoOfCopies(0);
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> borrowService.borrowBook(request));
        assertTrue(ex.getMessage().contains("EMP-01"));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void EMP01_shouldAllowBorrowWhenBookAvailable() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(2, 1)).thenReturn(false);
        when(borrowRepository.countByUserIdAndReturnDateIsNull(2)).thenReturn(0L);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow result = borrowService.borrowBook(request);

        assertNotNull(result);
        assertEquals(2, result.getUserId());
        verify(booksRepository).save(book);
        assertEquals(2, book.getNoOfCopies());
    }

    // ================================================================
    // EMP-02 : pas de double emprunt du meme livre non rendu
    // ================================================================

    @Test
    void EMP02_shouldRejectBorrowWhenAlreadyBorrowedAndNotReturned() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(2, 1)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> borrowService.borrowBook(request));
        assertTrue(ex.getMessage().contains("EMP-02"));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    // ================================================================
    // EMP-03 : quota maximum d'emprunts actifs simultanes
    // ================================================================

    @Test
    void EMP03_shouldRejectBorrowWhenQuotaReached() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(2, 1)).thenReturn(false);
        when(borrowRepository.countByUserIdAndReturnDateIsNull(2)).thenReturn(3L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> borrowService.borrowBook(request));
        assertTrue(ex.getMessage().contains("EMP-03"));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    // ================================================================
    // 404 : livre / utilisateur / emprunt introuvable
    // ================================================================

    @Test
    void shouldThrowNotFoundWhenBookDoesNotExist() {
        when(booksRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> borrowService.borrowBook(request));
    }

    @Test
    void shouldThrowNotFoundWhenReturningUnknownBorrow() {
        when(borrowRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> borrowService.returnBook(99));
    }

    // ================================================================
    // EMP-04 : un emprunt deja rendu ne peut pas etre rendu a nouveau
    // ================================================================

    @Test
    void EMP04_shouldRejectReturnWhenAlreadyReturned() {
        Borrow borrow = new Borrow();
        borrow.setBorrowId(5);
        borrow.setBookId(1);
        borrow.setUserId(2);
        borrow.setReturnDate(java.time.LocalDateTime.now());
        when(borrowRepository.findById(5)).thenReturn(Optional.of(borrow));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> borrowService.returnBook(5));
        assertTrue(ex.getMessage().contains("EMP-04"));
        verify(booksRepository, never()).save(any(Books.class));
    }

    @Test
    void shouldReturnBookSuccessfullyAndIncrementCopies() {
        book.setNoOfCopies(1);
        Borrow borrow = new Borrow();
        borrow.setBorrowId(6);
        borrow.setBookId(1);
        borrow.setUserId(2);
        when(borrowRepository.findById(6)).thenReturn(Optional.of(borrow));
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow result = borrowService.returnBook(6);

        assertNotNull(result.getReturnDate());
        assertEquals(2, book.getNoOfCopies());
        verify(booksRepository).save(book);
    }

    // ================================================================
    // Enrichissement (titre du livre / nom de l'utilisateur)
    // ================================================================

    @Test
    void shouldEnrichBorrowWithBookTitleAndUserName() {
        when(booksRepository.findById(1)).thenReturn(Optional.of(book));
        when(usersRepository.findById(2)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(2, 1)).thenReturn(false);
        when(borrowRepository.countByUserIdAndReturnDateIsNull(2)).thenReturn(0L);
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        Borrow result = borrowService.borrowBook(request);

        assertEquals("Test Book", result.getBookTitle());
        assertEquals("Adhérent Test", result.getUserName());
    }

    @Test
    void getBorrowsShouldReturnAllForBibliothecaire() {
        when(borrowRepository.findAll()).thenReturn(List.of(new Borrow(), new Borrow()));

        List<Borrow> result = borrowService.getBorrows();

        assertEquals(2, result.size());
        verify(borrowRepository).findAll();
    }
}
