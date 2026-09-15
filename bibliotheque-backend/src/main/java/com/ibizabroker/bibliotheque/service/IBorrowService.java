package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.BorrowRequest;

import java.util.List;

public interface IBorrowService {

    Borrow borrowBook(BorrowRequest request);

    List<Borrow> getBorrows();

    List<Borrow> getBorrowsByUser(Integer userId);

    List<Borrow> getBorrowsByBook(Integer bookId);

    Borrow returnBook(Integer borrowId);
}
