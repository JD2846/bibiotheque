import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { Users } from '../../../_model/users';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { BorrowService } from '../../services/borrow.service';
import { UserAuthService } from '../../../_service/user-auth.service';

@Component({
    selector: 'app-borrow-book',
    templateUrl: './borrow-book.component.html',
    styleUrls: ['./borrow-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule]
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];
  users: Users[] = [];
  loading = false;
  error = '';
  success = '';

  // Seul un BIBLIOTHECAIRE choisit l'adherent pour lequel il emprunte (service
  // au comptoir) ; le backend impose de toute facon l'identite du token pour
  // un ADHERENT (EMP-04).
  selectedAdherentId: number | null = null;

  constructor(
    private booksService: BooksService,
    private usersService: UsersService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
    private cdr: ChangeDetectorRef,
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    if (this.isBibliothecaire()) {
      this.loadUsers();
    }
  }

  isBibliothecaire(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  private getBooks() {
    this.booksService.getBooks().subscribe({
      next: data => { this.books = data; this.cdr.detectChanges(); },
      error: error => { this.error = error.message; this.cdr.detectChanges(); }
    });
  }

  private loadUsers() {
    this.usersService.getUsers().subscribe({
      next: data => { this.users = data; this.cdr.detectChanges(); },
      error: error => { this.error = error.message; this.cdr.detectChanges(); }
    });
  }

  borrowBook(bookId: number) {
    this.loading = true;
    this.error = '';
    this.success = '';
    const targetUserId = this.isBibliothecaire() && this.selectedAdherentId
      ? this.selectedAdherentId
      : this.userId;

    this.borrowService.borrowBook(bookId, targetUserId).pipe(
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => this.success = 'Livre emprunte avec succes.',
      error: error => this.error = error.message
    });
  }
}
