import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BooksService } from '../books/services/books.service';
import { UsersService } from '../users/services/users.service';
import { BorrowService } from '../borrow/services/borrow.service';
import { ReservationService } from '../reservation/services/reservation.service';
import { UserAuthService } from '../_service/user-auth.service';
import { ReservationStatus } from '../_model/reservation-status.enum';

interface DashboardStat {
  label: string;
  value: number;
  icon: string;
  colorClass: string;
  link?: string;
}

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterLink, NgClass]
})
export class HomeComponent implements OnInit {

  loading = true;
  stats: DashboardStat[] = [];

  constructor(
    private booksService: BooksService,
    private usersService: UsersService,
    private borrowService: BorrowService,
    private reservationService: ReservationService,
    private userAuthService: UserAuthService,
    private cdr: ChangeDetectorRef
  ) { }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  isAdmin(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Utilisateur';
  }

  ngOnInit(): void {
    if (this.isLoggedIn()) {
      this.loadDashboard();
    } else {
      this.loading = false;
    }
  }

  private loadDashboard(): void {
    const admin = this.isAdmin();
    const userId = this.userAuthService.getUserId();

    forkJoin({
      books: this.booksService.getBooks().pipe(catchError(() => of([]))),
      users: admin ? this.usersService.getUsers().pipe(catchError(() => of([]))) : of(null),
      borrows: admin
        ? this.borrowService.getBorrows().pipe(catchError(() => of([])))
        : this.borrowService.getBorrowsByUser(userId).pipe(catchError(() => of([]))),
      reservations: this.reservationService.getReservations().pipe(catchError(() => of([])))
    }).subscribe(({ books, users, borrows, reservations }) => {
      const availableBooks = books.filter(b => b.noOfCopies > 0).length;
      const activeBorrows = borrows.filter(b => !b.returnDate).length;
      const activeReservations = reservations.filter(
        r => r.status === ReservationStatus.EN_ATTENTE || r.status === ReservationStatus.DISPONIBLE
      ).length;

      const stats: DashboardStat[] = [
        { label: 'Livres au catalogue', value: books.length, icon: 'fa-book', colorClass: 'stat-neutral', link: '/books' },
        { label: 'Exemplaires disponibles', value: availableBooks, icon: 'fa-check-circle', colorClass: 'stat-success', link: '/books' },
        {
          label: admin ? 'Emprunts en cours' : 'Mes emprunts en cours',
          value: activeBorrows,
          icon: 'fa-hand-holding-heart',
          colorClass: 'stat-warning',
          link: '/borrow/return'
        },
        {
          label: admin ? 'Réservations actives' : 'Mes réservations actives',
          value: activeReservations,
          icon: 'fa-calendar-check',
          colorClass: 'stat-info',
          link: '/reservations'
        }
      ];

      if (admin && users) {
        stats.push({ label: 'Utilisateurs inscrits', value: users.length, icon: 'fa-users', colorClass: 'stat-neutral', link: '/users' });
      }

      this.stats = stats;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }
}
