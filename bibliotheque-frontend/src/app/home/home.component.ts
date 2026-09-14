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
import { ReservationStatus, ReservationStatusColors, ReservationStatusLabels } from '../_model/reservation-status.enum';

interface DashboardStat {
  label: string;
  value: number;
  icon: string;
  colorClass: string;
  link?: string;
}

interface BreakdownItem {
  label: string;
  value: number;
  colorClass: string;
  color: string;
  percent?: number;
}

interface QuickAction {
  label: string;
  icon: string;
  link: string;
}

// Actions rapides propres a chaque role : un BIBLIOTHECAIRE (Admin) n'a pas
// acces a /borrow (route reservee au role User), les proposer aurait menes
// vers la page "Acces refuse".
const ADHERENT_QUICK_ACTIONS: QuickAction[] = [
  { label: 'Emprunter un livre', icon: 'fa-hand-holding-heart', link: '/borrow' },
  { label: 'Rendre un livre', icon: 'fa-undo-alt', link: '/borrow/return' },
  { label: 'Réserver un livre', icon: 'fa-calendar-check', link: '/reservations' }
];

const BIBLIOTHECAIRE_QUICK_ACTIONS: QuickAction[] = [
  { label: 'Ajouter un livre', icon: 'fa-plus', link: '/books/create' },
  { label: 'Gérer les livres', icon: 'fa-book', link: '/books' },
  { label: 'Ajouter un utilisateur', icon: 'fa-user-plus', link: '/users/register' },
  { label: 'Gérer les utilisateurs', icon: 'fa-users', link: '/users' }
];

// Couleurs "fortes" dediees aux graphiques (distinctes des tons "soft" des badges)
const RESERVATION_CHART_COLORS: Record<ReservationStatus, string> = {
  [ReservationStatus.EN_ATTENTE]: '#d97706',
  [ReservationStatus.DISPONIBLE]: '#16a34a',
  [ReservationStatus.ANNULEE]: '#a1a1aa',
  [ReservationStatus.EXPIREE]: '#dc2626',
  [ReservationStatus.HONOREE]: '#18181b'
};

// Palette tournante pour le graphique "Livres par genre" (nombre de genres
// variable et non connu a l'avance, contrairement aux statuts de reservation)
const GENRE_CHART_COLORS = ['#8b5cf6', '#0ea5e9', '#f59e0b', '#22c55e', '#ec4899', '#14b8a6', '#f97316', '#6366f1'];

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
  quickActions: QuickAction[] = [];
  reservationBreakdown: BreakdownItem[] = [];
  reservationChartGradient = '';
  reservationChartTotal = 0;
  borrowSummary: BreakdownItem[] = [];
  borrowChartMax = 1;
  genreBreakdown: BreakdownItem[] = [];
  genreChartMax = 1;

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
      this.quickActions = this.isAdmin() ? BIBLIOTHECAIRE_QUICK_ACTIONS : ADHERENT_QUICK_ACTIONS;
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
          // /borrow/return est reservee au role User : un BIBLIOTHECAIRE n'y a pas acces
          link: admin ? undefined : '/borrow/return'
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

      // Répartition des réservations par statut (donut chart)
      this.reservationBreakdown = Object.values(ReservationStatus).map(status => ({
        label: ReservationStatusLabels[status],
        value: reservations.filter(r => r.status === status).length,
        colorClass: 'badge-' + ReservationStatusColors[status],
        color: RESERVATION_CHART_COLORS[status]
      }));
      this.reservationChartTotal = reservations.length;
      this.reservationChartGradient = this.buildConicGradient(this.reservationBreakdown, this.reservationChartTotal);

      // Résumé des emprunts (dans les délais / en retard / rendus) - bar chart
      const now = new Date();
      const enRetard = borrows.filter(b => !b.returnDate && b.dueDate && new Date(b.dueDate) < now).length;
      const enCours = activeBorrows - enRetard;
      const rendus = borrows.filter(b => !!b.returnDate).length;

      this.borrowSummary = [
        { label: 'Dans les délais', value: enCours, colorClass: 'badge-success', color: '#16a34a' },
        { label: 'En retard', value: enRetard, colorClass: 'badge-danger', color: '#dc2626' },
        { label: 'Rendus', value: rendus, colorClass: 'badge-secondary', color: '#18181b' }
      ];
      this.borrowChartMax = Math.max(1, ...this.borrowSummary.map(b => b.value));

      // Répartition des livres par genre - bar chart
      const genreCounts = new Map<string, number>();
      books.forEach(book => {
        const genre = book.bookGenre?.trim() || 'Autre';
        genreCounts.set(genre, (genreCounts.get(genre) || 0) + 1);
      });
      this.genreBreakdown = Array.from(genreCounts.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 6)
        .map(([label, value], index) => ({
          label,
          value,
          colorClass: '',
          color: GENRE_CHART_COLORS[index % GENRE_CHART_COLORS.length]
        }));
      this.genreChartMax = Math.max(1, ...this.genreBreakdown.map(g => g.value));

      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  barWidth(value: number): number {
    return this.borrowChartMax > 0 ? Math.round((value / this.borrowChartMax) * 100) : 0;
  }

  genreBarWidth(value: number): number {
    return this.genreChartMax > 0 ? Math.round((value / this.genreChartMax) * 100) : 0;
  }

  /** Construit un donut chart en CSS pur via conic-gradient, sans dependance externe. */
  private buildConicGradient(items: BreakdownItem[], total: number): string {
    if (total === 0) {
      return 'conic-gradient(var(--border) 0% 100%)';
    }
    let cursor = 0;
    const stops: string[] = [];
    for (const item of items) {
      if (item.value === 0) {
        continue;
      }
      const start = cursor;
      const end = cursor + (item.value / total) * 100;
      stops.push(`${item.color} ${start}% ${end}%`);
      cursor = end;
    }
    return `conic-gradient(${stops.join(', ')})`;
  }
}
