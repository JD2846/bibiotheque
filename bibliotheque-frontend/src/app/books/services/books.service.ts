import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../../_core/services/api-base.service';
import { ErrorHandlerService } from '../../_core/services/error-handler.service';
import { Books } from '../../_model/books';

@Injectable({
  providedIn: 'root'
})
export class BooksService extends ApiBaseService {

  private readonly endpoint = '/admin/books';

  constructor(http: HttpClient, errorHandler: ErrorHandlerService) {
    super(http, errorHandler);
  }

  getBooks(): Observable<Books[]> {
    return this.get<Books[]>(this.endpoint);
  }

  getBooksList(): Observable<Books[]> {
    return this.getBooks();
  }

  getBookById(id: number): Observable<Books> {
    return this.get<Books>(`${this.endpoint}/${id}`);
  }

  createBook(book: Books): Observable<Books> {
    return this.post<Books>(this.endpoint, book);
  }

  updateBook(id: number, book: Books): Observable<Books> {
    return this.put<Books>(`${this.endpoint}/${id}`, book);
  }

  deleteBook(id: number): Observable<void> {
    return this.delete<void>(`${this.endpoint}/${id}`);
  }

  searchBooks(keyword: string): Observable<Books[]> {
    return this.get<Books[]>(`${this.endpoint}/search`, { keyword });
  }

  getBooksByGenre(genre: string): Observable<Books[]> {
    return this.get<Books[]>(`${this.endpoint}/genre/${genre}`);
  }
}
