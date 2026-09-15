import { ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import { BorrowBookComponent } from './borrow-book.component';
import { BorrowService } from '../../services/borrow.service';
import { BooksService } from '../../../books/services/books.service';
import { UsersService } from '../../../users/services/users.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { Books } from '../../../_model/books';
import { Users } from '../../../_model/users';
import { Borrow } from '../../../_model/borrow';

describe('BorrowBookComponent', () => {
  let component: BorrowBookComponent;
  let booksService: jasmine.SpyObj<BooksService>;
  let usersService: jasmine.SpyObj<UsersService>;
  let userAuthService: jasmine.SpyObj<UserAuthService>;
  let borrowService: jasmine.SpyObj<BorrowService>;

  const mockBooks: Books[] = [
    { bookId: 1, bookName: 'Livre dispo', bookAuthor: 'Auteur', bookGenre: 'Roman', noOfCopies: 2 },
    { bookId: 2, bookName: 'Livre epuise', bookAuthor: 'Auteur', bookGenre: 'Roman', noOfCopies: 0 }
  ];
  const mockUsers: Users[] = [Object.assign(new Users(), { userId: 5, username: 'adherent', name: 'Adhérent' })];

  function createComponent(role: string) {
    booksService = jasmine.createSpyObj('BooksService', ['getBooks']);
    usersService = jasmine.createSpyObj('UsersService', ['getUsers']);
    userAuthService = jasmine.createSpyObj('UserAuthService', ['getRoles', 'getUserId']);
    borrowService = jasmine.createSpyObj('BorrowService', ['borrowBook']);

    booksService.getBooks.and.returnValue(of(mockBooks));
    usersService.getUsers.and.returnValue(of(mockUsers));
    userAuthService.getRoles.and.returnValue([{ roleName: role }] as any);
    userAuthService.getUserId.and.returnValue(3);

    component = new BorrowBookComponent(
      booksService,
      usersService,
      userAuthService,
      borrowService,
      jasmine.createSpyObj('ChangeDetectorRef', ['detectChanges']) as ChangeDetectorRef
    );
    component.ngOnInit();
  }

  it('devrait être créé', () => {
    createComponent('User');
    expect(component).toBeTruthy();
  });

  it('un ADHERENT ne charge pas la liste des utilisateurs', () => {
    createComponent('User');
    expect(usersService.getUsers).not.toHaveBeenCalled();
    expect(component.books.length).toBe(2);
  });

  it('un BIBLIOTHECAIRE charge la liste des adherents', () => {
    createComponent('Admin');
    expect(usersService.getUsers).toHaveBeenCalled();
    expect(component.users.length).toBe(1);
  });

  it('emprunte pour soi-même par défaut', () => {
    createComponent('User');
    const mockBorrow = Object.assign(new Borrow(), { borrowId: 1, bookId: 1, userId: 3 });
    borrowService.borrowBook.and.returnValue(of(mockBorrow));

    component.borrowBook(1);

    expect(borrowService.borrowBook).toHaveBeenCalledWith(1, 3);
    expect(component.success).toBeTruthy();
  });

  it('un BIBLIOTHECAIRE peut emprunter au nom de l adherent selectionne', () => {
    createComponent('Admin');
    component.selectedAdherentId = 5;
    const mockBorrow = Object.assign(new Borrow(), { borrowId: 1, bookId: 1, userId: 5 });
    borrowService.borrowBook.and.returnValue(of(mockBorrow));

    component.borrowBook(1);

    expect(borrowService.borrowBook).toHaveBeenCalledWith(1, 5);
  });

  it('affiche le vrai message d erreur renvoyé par le backend (ex: rupture de stock)', () => {
    createComponent('User');
    borrowService.borrowBook.and.returnValue(
      throwError(() => ({ status: 409, message: 'EMP-01: Aucun exemplaire disponible' }))
    );

    component.borrowBook(2);

    expect(component.error).toBe('EMP-01: Aucun exemplaire disponible');
    expect(component.success).toBe('');
  });
});
