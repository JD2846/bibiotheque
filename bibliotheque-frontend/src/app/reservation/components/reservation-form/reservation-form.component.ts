import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
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
export class ReservationFormComponent implements OnInit, OnChanges {
  @Input() presetBookId: number | null = null;
  @Output() reservationCreated = new EventEmitter<void>();

  reservationForm: FormGroup;
  // RG-01 : on ne peut reserver qu'un livre indisponible, inutile donc de
  // proposer les livres qui ont encore des exemplaires (evite une soumission
  // vouee a un 409).
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

    // Seul un BIBLIOTHECAIRE choisit l'adherent dans une liste ; un ADHERENT
    // n'a pas le champ a l'ecran mais le backend exige desormais que
    // adherentId corresponde exactement a son propre id (RS-04 : sinon 403),
    // donc on le pre-remplit nous-memes avec son id tire du token.
    if (this.isBibliothecaire()) {
      this.reservationForm.get('adherentId')?.addValidators(Validators.required);
    } else {
      this.reservationForm.patchValue({ adherentId: this.userAuthService.getUserId() });
    }
  }

  ngOnInit(): void {
    this.loadBooks();
    if (this.isBibliothecaire()) {
      this.loadUsers();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['presetBookId'] && !changes['presetBookId'].firstChange) {
      this.applyPresetBookId();
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
        if (!this.isBibliothecaire()) {
          this.reservationForm.patchValue({ adherentId: this.userAuthService.getUserId() });
        }
        this.successMessage = 'Réservation créée avec succès.';
        this.reservationCreated.emit();
      },
      error: error => this.serverError = error.message
    });
  }

  private loadBooks(): void {
    this.booksService.getBooks().subscribe({
      next: books => {
        // RG-01 : seuls les livres sans exemplaire disponible peuvent etre reserves
        this.books = books.filter(book => book.noOfCopies === 0);
        this.applyPresetBookId();
        this.cdr.detectChanges();
      },
      error: error => { this.serverError = error.message; this.cdr.detectChanges(); }
    });
  }

  private applyPresetBookId(): void {
    if (!this.presetBookId) {
      return;
    }
    const bookStillUnavailable = this.books.some(book => book.bookId === this.presetBookId);
    if (bookStillUnavailable) {
      this.reservationForm.patchValue({ bookId: this.presetBookId });
    }
  }

  private loadUsers(): void {
    this.usersService.getUsers().subscribe({
      next: users => { this.users = users; this.cdr.detectChanges(); },
      error: error => { this.serverError = error.message; this.cdr.detectChanges(); }
    });
  }
}
