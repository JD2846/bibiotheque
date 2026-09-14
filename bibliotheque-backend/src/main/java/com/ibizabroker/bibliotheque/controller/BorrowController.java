package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.BorrowRequest;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.IBorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrow")
@Tag(name = "Emprunts", description = "Gestion des emprunts de livres")
public class BorrowController {

    @Autowired
    private IBorrowService borrowService;

    @PostMapping
    @Operation(
            summary = "Emprunter un livre",
            description = "Enregistre l'emprunt d'un livre par un adhérent. " +
                    "EMP-01 : le livre doit avoir un exemplaire disponible. " +
                    "EMP-02 : pas de double emprunt du même livre non rendu. " +
                    "EMP-03 : max 3 emprunts actifs simultanés.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Emprunt enregistré avec succès"),
                    @ApiResponse(responseCode = "400", description = "bookId manquant"),
                    @ApiResponse(responseCode = "404", description = "Livre ou utilisateur non trouvé"),
                    @ApiResponse(responseCode = "409", description = "Règle de gestion violée (EMP-01, EMP-02, EMP-03)")
            })
    public ResponseEntity<?> borrowBook(@RequestBody BorrowRequest request) {
        try {
            Borrow borrow = borrowService.borrowBook(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(borrow);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Lister les emprunts", description = "Retourne les emprunts : tous pour un bibliothécaire, uniquement les siens pour un adhérent.")
    public ResponseEntity<List<Borrow>> getAllBorrow() {
        return ResponseEntity.ok(borrowService.getBorrows());
    }

    @PutMapping
    @Operation(
            summary = "Retourner un livre",
            description = "Enregistre le retour d'un livre emprunté. Incrémente le nombre d'exemplaires disponibles. " +
                    "EMP-04 : un emprunt déjà rendu ne peut pas être rendu à nouveau.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Retour enregistré"),
                    @ApiResponse(responseCode = "404", description = "Emprunt non trouvé"),
                    @ApiResponse(responseCode = "409", description = "Emprunt déjà rendu (EMP-04)")
            })
    public ResponseEntity<?> returnBook(@RequestBody Borrow borrow) {
        try {
            return ResponseEntity.ok(borrowService.returnBook(borrow.getBorrowId()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("user/{id}")
    @Operation(summary = "Emprunts d'un utilisateur", description = "Retourne tous les emprunts d'un adhérent identifié par son ID. Un adhérent ne peut consulter que son propre historique.")
    public ResponseEntity<List<Borrow>> booksBorrowedByUser(@PathVariable Integer id) {
        return ResponseEntity.ok(borrowService.getBorrowsByUser(id));
    }

    @GetMapping("book/{id}")
    @Operation(summary = "Historique d'emprunt d'un livre", description = "Retourne l'historique complet des emprunts d'un livre identifié par son ID. Réservé au bibliothécaire.")
    public ResponseEntity<List<Borrow>> bookBorrowHistory(@PathVariable Integer id) {
        return ResponseEntity.ok(borrowService.getBorrowsByBook(id));
    }
}
