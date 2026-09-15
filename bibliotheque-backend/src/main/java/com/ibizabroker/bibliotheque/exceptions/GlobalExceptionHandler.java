package com.ibizabroker.bibliotheque.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralise la traduction des exceptions metier/securite en reponses HTTP
 * avec un message clair, pour que le frontend puisse afficher le vrai message
 * au lieu du "No message available" par defaut de Spring Boot.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<String> handleInvalidCredentials(Exception e, HttpServletRequest request) {
        log.warn("Tentative de connexion refusee (401) sur {} : identifiants invalides", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nom d'utilisateur ou mot de passe incorrect.");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabledAccount(DisabledException e, HttpServletRequest request) {
        log.warn("Tentative de connexion refusee (401) sur {} : compte desactive", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ce compte est desactive.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Acces refuse (403) sur {} {} : {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'avez pas les droits necessaires pour effectuer cette action.");
    }
}
