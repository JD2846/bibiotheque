package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reservation")
public class Reservation {

    // @JsonProperty aligne le JSON expose sur les noms attendus par le modele
    // frontend (src/app/_model/reservation.model.ts) : id/reservationDate/
    // expirationDate, sans renommer les champs/colonnes JPA cote backend.
    @Id
    @SequenceGenerator(name = "reservation_seq", sequenceName = "reservation_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_seq")
    @Column(name = "reservation_id")
    @JsonProperty("id")
    private Integer reservationId;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReservationStatus status;

    @Column(name = "date_reservation")
    @JsonSerialize(using = JsonDataSerializer.class)
    @JsonProperty("reservationDate")
    private LocalDateTime dateReservation;

    @Column(name = "date_expiration")
    @JsonSerialize(using = JsonDataSerializer.class)
    @JsonProperty("expirationDate")
    private LocalDateTime dateExpiration;

    // Champs enrichis (non persistes) : titre du livre / nom de l'adherent,
    // renseignes par ReservationServiceImpl pour eviter que le frontend
    // n'affiche que des identifiants numeriques bruts.
    @Transient
    private String bookTitle;

    @Transient
    private String userName;

    // RG-04 : dateExpiration = dateReservation + 7 jours, calculée côté serveur
    @PrePersist
    protected void onCreate() {
        if (dateReservation == null) {
            dateReservation = LocalDateTime.now();
        }
        if (dateExpiration == null) {
            dateExpiration = dateReservation.plusDays(7);
        }
        if (status == null) {
            status = ReservationStatus.EN_ATTENTE;
        }
    }
}
