package com.ibizabroker.bibliotheque.entity;

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

    @Id
    @SequenceGenerator(name = "reservation_seq", sequenceName = "reservation_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_seq")
    @Column(name = "reservation_id")
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
    private LocalDateTime dateReservation;

    @Column(name = "date_expiration")
    @JsonSerialize(using = JsonDataSerializer.class)
    private LocalDateTime dateExpiration;

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
