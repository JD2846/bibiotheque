package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin")
@Tag(name = "Livres", description = "Gestion des livres (CRUD)")
public class BooksController {

    @Autowired
    private BooksRepository booksRepository;

    @GetMapping("/books")
    @Operation(summary = "Lister tous les livres", description = "Retourne la liste de tous les livres disponibles. Accessible sans authentification.")
    public List<Books> getAllBooks(){
        return booksRepository.findAll();
    }

    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/books/{id}")
    @Operation(summary = "Consulter un livre par ID", description = "Retourne les détails d'un livre. Rôle Admin requis.")
    public ResponseEntity<Books> getBookById(@PathVariable Integer id) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));
        return ResponseEntity.ok(book);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/books")
    @Operation(summary = "Créer un livre", description = "Ajoute un nouveau livre à la bibliothèque. Rôle Admin requis.")
    public Books createBook(@RequestBody Books book) {
        return booksRepository.save(book);
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/books/{id}")
    @Operation(summary = "Modifier un livre", description = "Met à jour les informations d'un livre existant. Rôle Admin requis.")
    public ResponseEntity<Books> updateBook(@PathVariable Integer id, @RequestBody Books bookDetails) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        book.setBookName(bookDetails.getBookName());
        book.setBookAuthor(bookDetails.getBookAuthor());
        book.setBookGenre(bookDetails.getBookGenre());
        book.setNoOfCopies(bookDetails.getNoOfCopies());

        Books updatedBook = booksRepository.save(book);
        return ResponseEntity.ok(updatedBook);
    }

    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/books/{id}")
    @Operation(summary = "Supprimer un livre", description = "Supprime un livre de la bibliothèque. Rôle Admin requis.")
    public ResponseEntity<Map<String, Boolean>> deleteBook(@PathVariable Integer id) {
        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book with id "+ id +" does not exist."));

        booksRepository.delete(book);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }
}
