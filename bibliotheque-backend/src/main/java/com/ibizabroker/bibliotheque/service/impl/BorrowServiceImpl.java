package com.ibizabroker.bibliotheque.service.impl;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.BorrowRequest;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.IBorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowServiceImpl implements IBorrowService {

    private static final Logger log = LoggerFactory.getLogger(BorrowServiceImpl.class);

    private static final int MAX_EMPRUNTS_ACTIFS = 3;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public Borrow borrowBook(BorrowRequest request) {
        if (request.getBookId() == null) {
            throw new IllegalArgumentException("bookId est obligatoire");
        }

        // EMP-04 (souple pour BIBLIOTHECAIRE, comme RS-04 sur les reservations) :
        // un ADHERENT emprunte forcement pour lui-meme ; seul un BIBLIOTHECAIRE
        // peut emprunter au nom d'un autre adherent (service au comptoir).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer ownerId = isBibliothecaire(auth) && request.getUserId() != null
                ? request.getUserId()
                : getCurrentUser().getUserId();

        Books book = booksRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre non trouvé avec l'id: " + request.getBookId()));

        Users user = usersRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé avec l'id: " + ownerId));

        // EMP-01 : le livre doit avoir au moins un exemplaire disponible
        if (book.getNoOfCopies() == null || book.getNoOfCopies() < 1) {
            throw new ConflictException("EMP-01: Aucun exemplaire disponible pour le livre \"" + book.getBookName() + "\"");
        }

        // EMP-02 : pas de double emprunt du meme livre non rendu par le meme adherent
        boolean alreadyBorrowed = borrowRepository.existsByUserIdAndBookIdAndReturnDateIsNull(ownerId, request.getBookId());
        if (alreadyBorrowed) {
            throw new ConflictException("EMP-02: Vous avez deja un emprunt en cours pour ce livre");
        }

        // EMP-03 : quota maximum d'emprunts actifs simultanes
        long activeCount = borrowRepository.countByUserIdAndReturnDateIsNull(ownerId);
        if (activeCount >= MAX_EMPRUNTS_ACTIFS) {
            throw new ConflictException("EMP-03: Vous avez atteint le nombre maximum d'emprunts actifs (" + MAX_EMPRUNTS_ACTIFS + ")");
        }

        book.borrowBook();
        booksRepository.save(book);

        Borrow borrow = new Borrow();
        borrow.setBookId(request.getBookId());
        borrow.setUserId(ownerId);
        LocalDateTime now = LocalDateTime.now();
        borrow.setIssueDate(now);
        borrow.setDueDate(now.plusDays(7));

        return enrich(borrowRepository.save(borrow));
    }

    @Override
    public List<Borrow> getBorrows() {
        // EMP-05 : un ADHERENT ne voit que ses propres emprunts, quelle que soit la demande
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<Borrow> result = isBibliothecaire(auth)
                ? borrowRepository.findAll()
                : borrowRepository.findByUserId(getCurrentUser().getUserId());
        result.forEach(this::enrich);
        return result;
    }

    @Override
    public List<Borrow> getBorrowsByUser(Integer userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isBibliothecaire(auth)) {
            Users currentUser = getCurrentUser();
            if (!currentUser.getUserId().equals(userId)) {
                log.warn("EMP-05 : acces refuse - l'utilisateur '{}' (id={}) a tente de consulter l'historique d'emprunt de l'utilisateur id={}",
                        currentUser.getUsername(), currentUser.getUserId(), userId);
                throw new AccessDeniedException("EMP-05: Vous n'avez pas acces a cet historique d'emprunt");
            }
        }
        List<Borrow> result = borrowRepository.findByUserId(userId);
        result.forEach(this::enrich);
        return result;
    }

    @Override
    public List<Borrow> getBorrowsByBook(Integer bookId) {
        // EMP-05 : l'historique d'emprunt d'un livre revele l'identite d'autres
        // adherents, reserve au BIBLIOTHECAIRE
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isBibliothecaire(auth)) {
            log.warn("EMP-05 : acces refuse - un adherent a tente de consulter l'historique d'emprunt du livre id={}", bookId);
            throw new AccessDeniedException("EMP-05: Vous n'avez pas acces a cet historique d'emprunt");
        }
        List<Borrow> result = borrowRepository.findByBookId(bookId);
        result.forEach(this::enrich);
        return result;
    }

    @Override
    public Borrow returnBook(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new NotFoundException("Emprunt non trouvé avec l'id: " + borrowId));

        checkOwnership(borrow);

        // EMP-04 : un emprunt deja rendu ne peut pas etre rendu a nouveau
        if (borrow.getReturnDate() != null) {
            throw new ConflictException("EMP-04: Cet emprunt a deja ete rendu");
        }

        Books book = booksRepository.findById(borrow.getBookId())
                .orElseThrow(() -> new NotFoundException("Livre non trouvé avec l'id: " + borrow.getBookId()));
        book.returnBook();
        booksRepository.save(book);

        borrow.setReturnDate(LocalDateTime.now());
        return enrich(borrowRepository.save(borrow));
    }

    /**
     * Renseigne bookTitle/userName (champs non persistes) pour que le frontend
     * n'ait pas a afficher de simples identifiants numeriques.
     */
    private Borrow enrich(Borrow borrow) {
        booksRepository.findById(borrow.getBookId())
                .ifPresent(book -> borrow.setBookTitle(book.getBookName()));
        usersRepository.findById(borrow.getUserId())
                .ifPresent(user -> borrow.setUserName(user.getName()));
        return borrow;
    }

    private Users getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Utilisateur connecté non trouvé"));
    }

    private boolean isBibliothecaire(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
    }

    private void checkOwnership(Borrow borrow) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isBibliothecaire(auth)) {
            return;
        }
        Users currentUser = getCurrentUser();
        if (!currentUser.getUserId().equals(borrow.getUserId())) {
            log.warn("EMP-05 : acces refuse - l'utilisateur '{}' (id={}) a tente de rendre l'emprunt {} appartenant a l'utilisateur id={}",
                    currentUser.getUsername(), currentUser.getUserId(), borrow.getBorrowId(), borrow.getUserId());
            throw new AccessDeniedException("EMP-05: Vous n'avez pas acces a cet emprunt");
        }
    }
}
