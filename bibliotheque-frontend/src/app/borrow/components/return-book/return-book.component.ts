import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { Borrow } from '../../../_model/borrow';
import { Users } from '../../../_model/users';
import { UsersService } from '../../../users/services/users.service';
import { BorrowService } from '../../services/borrow.service';
import { UserAuthService } from '../../../_service/user-auth.service';

@Component({
    selector: 'app-return-book',
    templateUrl: './return-book.component.html',
    styleUrls: ['./return-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule]
})
export class ReturnBookComponent implements OnInit {

  borrow: Borrow[] = [];
  users: Users[] = [];
  loading = false;
  error = '';
  success = '';

  // Seul un BIBLIOTHECAIRE choisit l'adherent dont il consulte/rend les
  // emprunts (service au comptoir) ; un ADHERENT ne voit que les siens (EMP-05).
  selectedAdherentId: number | null = null;

  constructor(
    private borrowService: BorrowService,
    private usersService: UsersService,
    private userAuthService: UserAuthService,
    private cdr: ChangeDetectorRef
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    if (this.isBibliothecaire()) {
      this.loadUsers();
    }
    this.getBooksByUser();
  }

  isBibliothecaire(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  onAdherentChange(): void {
    this.getBooksByUser();
  }

  private loadUsers() {
    this.usersService.getUsers().subscribe({
      next: data => { this.users = data; this.cdr.detectChanges(); },
      error: error => { this.error = error.message; this.cdr.detectChanges(); }
    });
  }

  private getBooksByUser() {
    const targetUserId = this.isBibliothecaire() && this.selectedAdherentId
      ? this.selectedAdherentId
      : this.userId;

    this.borrowService.getBorrowsByUser(targetUserId).subscribe({
      next: data => { this.borrow = data; this.cdr.detectChanges(); },
      error: error => { this.error = error.message; this.cdr.detectChanges(); }
    })
  }

  public returnBook(borrowId: number) {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.borrowService.returnBook(borrowId).pipe(
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => {
        this.success = 'Livre rendu avec succes.';
        this.getBooksByUser();
      },
      error: error => this.error = error.message
    });
  }

  formatDate(value: string | Date): string {
    return value ? new Date(value).toLocaleDateString('fr-FR') : '';
  }
}
