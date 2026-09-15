import { ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ReturnBookComponent } from './return-book.component';
import { BorrowService } from '../../services/borrow.service';
import { UsersService } from '../../../users/services/users.service';
import { UserAuthService } from '../../../_service/user-auth.service';
import { Users } from '../../../_model/users';
import { Borrow } from '../../../_model/borrow';

describe('ReturnBookComponent', () => {
  let component: ReturnBookComponent;
  let borrowService: jasmine.SpyObj<BorrowService>;
  let usersService: jasmine.SpyObj<UsersService>;
  let userAuthService: jasmine.SpyObj<UserAuthService>;

  const mockUsers: Users[] = [Object.assign(new Users(), { userId: 5, username: 'adherent', name: 'Adhérent' })];
  const mockBorrows: Borrow[] = [
    Object.assign(new Borrow(), { borrowId: 1, bookId: 1, bookTitle: 'Livre A', userId: 3, returnDate: null })
  ];

  function createComponent(role: string) {
    borrowService = jasmine.createSpyObj('BorrowService', ['getBorrowsByUser', 'returnBook']);
    usersService = jasmine.createSpyObj('UsersService', ['getUsers']);
    userAuthService = jasmine.createSpyObj('UserAuthService', ['getRoles', 'getUserId']);

    borrowService.getBorrowsByUser.and.returnValue(of(mockBorrows));
    usersService.getUsers.and.returnValue(of(mockUsers));
    userAuthService.getRoles.and.returnValue([{ roleName: role }] as any);
    userAuthService.getUserId.and.returnValue(3);

    component = new ReturnBookComponent(
      borrowService,
      usersService,
      userAuthService,
      jasmine.createSpyObj('ChangeDetectorRef', ['detectChanges']) as ChangeDetectorRef
    );
    component.ngOnInit();
  }

  it('devrait être créé', () => {
    createComponent('User');
    expect(component).toBeTruthy();
  });

  it('un ADHERENT charge ses propres emprunts sans charger la liste des utilisateurs', () => {
    createComponent('User');
    expect(usersService.getUsers).not.toHaveBeenCalled();
    expect(borrowService.getBorrowsByUser).toHaveBeenCalledWith(3);
    expect(component.borrow.length).toBe(1);
  });

  it('un BIBLIOTHECAIRE charge la liste des adherents et ses propres emprunts par defaut', () => {
    createComponent('Admin');
    expect(usersService.getUsers).toHaveBeenCalled();
    expect(borrowService.getBorrowsByUser).toHaveBeenCalledWith(3);
  });

  it('changer d adherent recharge la liste des emprunts de l adherent choisi', () => {
    createComponent('Admin');
    component.selectedAdherentId = 5;

    component.onAdherentChange();

    expect(borrowService.getBorrowsByUser).toHaveBeenCalledWith(5);
  });

  it('rend un livre avec succès et recharge la liste', () => {
    createComponent('User');
    borrowService.returnBook.and.returnValue(of(mockBorrows[0]));

    component.returnBook(1);

    expect(borrowService.returnBook).toHaveBeenCalledWith(1);
    expect(component.success).toBeTruthy();
    expect(borrowService.getBorrowsByUser).toHaveBeenCalledTimes(2);
  });

  it('affiche le vrai message d erreur renvoyé par le backend (ex: déjà rendu)', () => {
    createComponent('User');
    borrowService.returnBook.and.returnValue(
      throwError(() => ({ status: 409, message: 'EMP-04: Cet emprunt a deja ete rendu' }))
    );

    component.returnBook(1);

    expect(component.error).toBe('EMP-04: Cet emprunt a deja ete rendu');
  });

  it('formatDate formate une date ISO en format fr-FR', () => {
    createComponent('User');
    const formatted = component.formatDate('2026-01-15T10:00:00');
    expect(formatted).toBeTruthy();
    expect(formatted).not.toContain('T');
  });
});
