import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { Borrow } from '../../../_model/borrow';
import { BooksService } from '../../../books/services/books.service';
import { BorrowService } from '../../services/borrow.service';
import { UserAuthService } from '../../../_service/user-auth.service';

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css']
})
export class ReturnBookComponent implements OnInit {

  books: Books[] = [];
  borrow: Borrow[] = [];
  loading = false;
  error = '';
  success = '';

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getBooksByUser();
  }

  private getBooks() {
    this.booksService.getBooks().subscribe({
      next: data => this.books = data,
      error: error => this.error = error.message
    });
  }

  
  private getBooksByUser() {
    this.borrowService.getBorrowsByUser(this.userId).subscribe({
      next: data => this.borrow = data,
      error: error => this.error = error.message
    })
  }

  brw: Borrow = new Borrow();
  public returnBook(borrowId: number) {
    this.brw.borrowId = borrowId;
    this.loading = true;
    this.error = '';
    this.success = '';
    this.borrowService.returnBook(this.brw).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: () => {
        this.success = 'Livre rendu avec succes.';
        this.getBooksByUser();
      },
      error: error => this.error = error.message
    });
  }

}
