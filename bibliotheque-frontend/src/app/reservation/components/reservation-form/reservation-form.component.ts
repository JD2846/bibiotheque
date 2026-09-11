import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books';
import { Users } from '../../../_model/users';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { ReservationService } from '../../services/reservation.service';

@Component({
    selector: 'app-reservation-form',
    templateUrl: './reservation-form.component.html',
    styleUrls: ['./reservation-form.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule, ReactiveFormsModule]
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
    private usersService: UsersService,
    private userAuthService: UserAuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.reservationForm = this.formBuilder.group({
      bookId: ['', Validators.required],
      adherentId: ['']
    });

    // Seul un BIBLIOTHECAIRE choisit l'adherent : le backend impose de toute
    // facon l'identite du token pour un ADHERENT (RS-04), inutile de lui
    // demander de se choisir lui-meme dans une liste.
    if (this.isBibliothecaire()) {
      this.reservationForm.get('adherentId')?.addValidators(Validators.required);
    }
  }

  ngOnInit(): void {
    this.loadBooks();
    if (this.isBibliothecaire()) {
      this.loadUsers();
    }
  }

  isBibliothecaire(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
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
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
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
      next: books => { this.books = books; this.cdr.detectChanges(); },
      error: error => { this.serverError = error.message; this.cdr.detectChanges(); }
    });
  }

  private loadUsers(): void {
    this.usersService.getUsers().subscribe({
      next: users => { this.users = users; this.cdr.detectChanges(); },
      error: error => { this.serverError = error.message; this.cdr.detectChanges(); }
    });
  }
}
