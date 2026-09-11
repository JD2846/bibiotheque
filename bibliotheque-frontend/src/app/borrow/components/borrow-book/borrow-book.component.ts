import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { Borrow } from '../../../_model/borrow';
import { BooksService } from '../../../books/services/books.service';
import { BorrowService } from '../../services/borrow.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { Users } from '../../../_model/users';
import { UsersService } from '../../../users/services/users.service';

@Component({
    selector: 'app-borrow-book',
    templateUrl: './borrow-book.component.html',
    styleUrls: ['./borrow-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];
  users: Users[] = [];
  loading = false;
  error = '';
  success = '';

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
    private usersService: UsersService,
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getUsers();
  }

  private getBooks() {
    this.booksService.getBooks().subscribe({
      next: data => this.books = data,
      error: error => this.error = error.message
    });
  }

  private getUsers() {
    this.usersService.getUsers().subscribe({
      next: data => this.users = data,
      error: error => this.error = error.message
    });
  }

  borrow: Borrow = new Borrow();

  borrowBook(bookId: number) {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.borrowService.borrowBook(bookId, this.userId).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: () => this.success = 'Livre emprunte avec succes.',
      error: error => this.error = error.message
    });
  }
}
