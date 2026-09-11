import { Component, EventEmitter, OnInit, Output, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { Users } from '../../../_model/users';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { ReservationService } from '../../services/reservation.service';

@Component({
    selector: 'app-reservation-form',
    templateUrl: './reservation-form.component.html',
    styleUrls: ['./reservation-form.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationFormComponent implements OnInit {
  @Output() reservationCreated = new EventEmitter<void>();

  reservationForm: FormGroup;
  books: Books[] = [];
  users: Users[] = [];
  loading = false;
  serverError = '';
  successMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService
  ) {
    this.reservationForm = this.formBuilder.group({
      bookId: ['', Validators.required],
      adherentId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadBooks();
    this.loadUsers();
  }

  submit(): void {
    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = '';
    this.successMessage = '';
    this.reservationService.createReservation(this.reservationForm.value).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: () => {
        this.reservationForm.reset();
        this.successMessage = 'Réservation créée avec succès.';
        this.reservationCreated.emit();
      },
      error: error => this.serverError = error.message
    });
  }

  private loadBooks(): void {
    this.booksService.getBooks().subscribe({
      next: books => this.books = books,
      error: error => this.serverError = error.message
    });
  }

  private loadUsers(): void {
    this.usersService.getUsers().subscribe({
      next: users => this.users = users,
      error: error => this.serverError = error.message
    });
  }
}
