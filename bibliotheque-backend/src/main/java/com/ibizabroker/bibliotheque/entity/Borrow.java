package com.ibizabroker.bibliotheque.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "borrow")
public class Borrow {

    @Id
    @SequenceGenerator(name = "borrow_seq", sequenceName = "borrow_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "borrow_seq")
    @Column(name = "borrow_id")
    Integer borrowId;

    @Column(name = "book_id")
    Integer bookId;

    @Column(name = "user_id")
    Integer userId;

    @Column(name = "issue_date")
    @JsonSerialize(using = JsonDataSerializer.class)
    LocalDateTime issueDate;

    @Column(name = "return_date")
    @JsonSerialize(using = JsonDataSerializer.class)
    LocalDateTime returnDate;

    @Column(name = "due_date")
    @JsonSerialize(using = JsonDataSerializer.class)
    LocalDateTime dueDate;

    @Transient
    String bookTitle;

    @Transient
    String userName;

    @PrePersist
    protected void onCreate() {
        if (issueDate == null) {
            issueDate = LocalDateTime.now();
        }
        if (dueDate == null) {
            dueDate = LocalDateTime.now().plusDays(7);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (returnDate == null) {
            returnDate = LocalDateTime.now();
        }
    }
}
