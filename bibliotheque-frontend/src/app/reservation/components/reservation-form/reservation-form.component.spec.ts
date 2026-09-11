import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { ReservationFormComponent } from './reservation-form.component';
import { ReservationService } from '../../services/reservation.service';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { Reservation } from '../../../_model/reservation.model';
import { ReservationStatus } from '../../../_model/reservation-status.enum';
import { Books } from '../../../_model/books';
import { Users } from '../../../_model/users';

describe('ReservationFormComponent', () => {
  let component: ReservationFormComponent;
  let reservationService: jasmine.SpyObj<ReservationService>;
  let booksService: jasmine.SpyObj<BooksService>;
  let usersService: jasmine.SpyObj<UsersService>;
  let userAuthService: jasmine.SpyObj<UserAuthService>;

  const mockBooks: Books[] = [{ bookId: 1, bookName: 'Test Book', bookAuthor: 'Author', bookGenre: 'Roman', noOfCopies: 0 }];
  const mockUsers: Users[] = [Object.assign(new Users(), { userId: 1, username: 'adherent', name: 'Adhérent' })];

  beforeEach(() => {
    reservationService = jasmine.createSpyObj('ReservationService', ['createReservation']);
    booksService = jasmine.createSpyObj('BooksService', ['getBooks']);
    usersService = jasmine.createSpyObj('UsersService', ['getUsers']);
    userAuthService = jasmine.createSpyObj('UserAuthService', ['getRoles']);

    booksService.getBooks.and.returnValue(of(mockBooks));
    usersService.getUsers.and.returnValue(of(mockUsers));
    // Role Admin (BIBLIOTHECAIRE) pour que le selecteur d'adherent soit charge/requis
    userAuthService.getRoles.and.returnValue([{ roleName: 'Admin' }] as any);

    component = new ReservationFormComponent(
      new FormBuilder(),
      reservationService,
      booksService,
      usersService,
      userAuthService
    );
    component.ngOnInit();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger les livres et utilisateurs au démarrage', () => {
    expect(booksService.getBooks).toHaveBeenCalled();
    expect(usersService.getUsers).toHaveBeenCalled();
    expect(component.books.length).toBe(1);
    expect(component.users.length).toBe(1);
  });

  it('devrait considérer le formulaire invalide tant que bookId/adherentId ne sont pas renseignés', () => {
    expect(component.reservationForm.valid).toBeFalsy();
  });

  it('devrait considérer le formulaire valide une fois bookId/adherentId renseignés', () => {
    component.reservationForm.patchValue({ bookId: 1, adherentId: 1 });
    expect(component.reservationForm.valid).toBeTruthy();
  });

  it('devrait afficher le vrai message d erreur 409 renvoyé par le backend', () => {
    component.reservationForm.patchValue({ bookId: 1, adherentId: 1 });
    reservationService.createReservation.and.returnValue(
      throwError(() => ({ status: 409, message: 'RG-01: Impossible de réserver un livre disponible' }))
    );

    component.submit();

    expect(component.serverError).toBe('RG-01: Impossible de réserver un livre disponible');
    expect(component.loading).toBe(false);
  });

  it('devrait émettre reservationCreated et afficher un message de succès après création', () => {
    component.reservationForm.patchValue({ bookId: 1, adherentId: 1 });
    const mockReservation: Reservation = {
      id: 100,
      bookId: 1,
      bookTitle: 'Test Book',
      userId: 1,
      userName: 'Adhérent',
      reservationDate: '2026-08-28',
      expirationDate: '2026-09-04',
      status: ReservationStatus.EN_ATTENTE,
      active: true
    };
    reservationService.createReservation.and.returnValue(of(mockReservation));
    spyOn(component.reservationCreated, 'emit');

    component.submit();

    expect(component.successMessage).toBeTruthy();
    expect(component.reservationCreated.emit).toHaveBeenCalled();
  });

  it('ne devrait pas appeler le service si le formulaire est invalide', () => {
    component.submit();
    expect(reservationService.createReservation).not.toHaveBeenCalled();
  });
});
