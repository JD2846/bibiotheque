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

  /**
   * userId optionnel : absent, le backend infere l'utilisateur depuis le token
   * (EMP-04). Seul un BIBLIOTHECAIRE peut fournir un userId different du sien.
   */
  borrowBook(bookId: number, userId?: number): Observable<Borrow> {
    return this.post<Borrow>(this.endpoint, { bookId, userId });
  }

  returnBook(borrowId: number): Observable<Borrow> {
    return this.put<Borrow>(this.endpoint, { borrowId });
  }
}
