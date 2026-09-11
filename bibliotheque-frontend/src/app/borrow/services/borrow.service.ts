import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../../_core/services/api-base.service';
import { ErrorHandlerService } from '../../_core/services/error-handler.service';
import { Borrow } from '../../_model/borrow';

@Injectable({
  providedIn: 'root'
})
export class BorrowService extends ApiBaseService {

  private readonly endpoint = '/borrow';

  constructor(http: HttpClient, errorHandler: ErrorHandlerService) {
    super(http, errorHandler);
  }

  getBorrows(): Observable<Borrow[]> {
    return this.get<Borrow[]>(this.endpoint);
  }

  getBorrowList(): Observable<Borrow[]> {
    return this.getBorrows();
  }

  getBorrowById(id: number): Observable<Borrow> {
    return this.get<Borrow>(`${this.endpoint}/${id}`);
  }

  getBorrowsByUser(userId: number): Observable<Borrow[]> {
    return this.get<Borrow[]>(`${this.endpoint}/user/${userId}`);
  }

  getBooksBorrowedByUser(userId: number): Observable<Borrow[]> {
    return this.getBorrowsByUser(userId);
  }

  getBorrowsByBook(bookId: number): Observable<Borrow[]> {
    return this.get<Borrow[]>(`${this.endpoint}/book/${bookId}`);
  }

  getBookBorrowHistory(bookId: number): Observable<Borrow[]> {
    return this.getBorrowsByBook(bookId);
  }

  borrowBook(bookId: number, userId: number): Observable<Borrow>;
  borrowBook(borrow: Borrow): Observable<Borrow>;
  borrowBook(bookOrBorrow: number | Borrow, userId?: number): Observable<Borrow> {
    const payload = typeof bookOrBorrow === 'number'
      ? { bookId: bookOrBorrow, userId }
      : bookOrBorrow;

    return this.post<Borrow>(this.endpoint, payload);
  }

  returnBook(bookId: number, userId: number): Observable<Borrow>;
  returnBook(borrow: Borrow): Observable<Borrow>;
  returnBook(bookOrBorrow: number | Borrow, userId?: number): Observable<Borrow> {
    const payload = typeof bookOrBorrow === 'number'
      ? { bookId: bookOrBorrow, userId }
      : bookOrBorrow;

    return this.put<Borrow>(this.endpoint, payload);
  }
}
