package com.ibizabroker.bibliotheque.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "books")
public class Books {

    @Id
    @SequenceGenerator(name = "books_seq", sequenceName = "books_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "books_seq")
    @Column(name = "book_id")
    Integer bookId;

    @Column(name = "book_name")
    String bookName;

    @Column(name = "book_author")
    String bookAuthor;

    @Column(name = "book_genre")
    String bookGenre;

    @Column(name = "no_of_copies")
    Integer noOfCopies;

    public void borrowBook() {
        this.noOfCopies--;
    }

    public void returnBook() {
        this.noOfCopies++;
    }

}
