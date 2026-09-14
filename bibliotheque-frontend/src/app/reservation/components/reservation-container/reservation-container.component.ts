import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../../../_core/services/loading.service';
import { ReservationFilters } from '../../../_model/reservation.model';
import { ReservationStatus, ReservationStatusList } from '../../../_model/reservation-status.enum';
import { ReservationDataService } from '../../services/reservation-data.service';
import { ReservationService } from '../../services/reservation.service';
import { ReservationFormComponent } from '../reservation-form/reservation-form.component';
import { FormsModule } from '@angular/forms';
import { AsyncPipe } from '@angular/common';
import { ReservationListComponent } from '../reservation-list/reservation-list.component';

@Component({
    selector: 'app-reservation-container',
    templateUrl: './reservation-container.component.html',
    styleUrls: ['./reservation-container.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ReservationFormComponent, FormsModule, AsyncPipe, ReservationListComponent]
})
export class ReservationContainerComponent implements OnInit {
  selectedStatus: ReservationStatus | 'TOUS' = 'TOUS';
  statusList = ReservationStatusList;

  // Expose directement les observables du service au template (AsyncPipe) :
  // AsyncPipe declenche lui-meme un markForCheck() a chaque emission, ce qui
  // evite de devoir recopier l'etat dans des champs de composant mis a jour
  // via un subscribe() manuel (source d'un bug de rafraichissement observe
  // ou le tableau restait bloque sur "Chargement..." tant qu'aucun autre
  // evenement Angular ne survenait).
  readonly reservations$ = this.reservationDataService.reservations$;
  readonly loading$ = this.reservationDataService.loading$;
  readonly error$ = this.reservationDataService.error$;

  // Pre-selection du livre dans le formulaire quand on arrive depuis la page
  // "Emprunter" via le lien "Réserver" affiché sur un livre indisponible
  // (/reservations?bookId=...).
  presetBookId: number | null = null;

  constructor(
    private reservationService: ReservationService,
    private reservationDataService: ReservationDataService,
    private loadingService: LoadingService,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    const bookId = Number(this.route.snapshot.queryParamMap.get('bookId'));
    this.presetBookId = bookId > 0 ? bookId : null;
    this.loadReservations();
  }

  loadReservations(filters?: ReservationFilters): void {
    this.loadingService.startLoading('reservations');
    this.reservationDataService.setLoading(true);
    this.reservationDataService.clearError();

    this.reservationService.getReservations(filters).pipe(
      finalize(() => {
        this.loadingService.stopLoading('reservations');
        this.reservationDataService.setLoading(false);
      })
    ).subscribe({
      next: reservations => this.reservationDataService.setReservations(reservations),
      error: error => this.reservationDataService.setError(error.message)
    });
  }

  onStatusChange(status: ReservationStatus | 'TOUS'): void {
    this.selectedStatus = status;
    const filters = status === 'TOUS' ? undefined : { status };
    this.loadReservations(filters);
  }

  onReservationCreated(): void {
    this.loadReservations(this.currentFilters());
  }

  onReservationCancelled(id: number): void {
    this.loadingService.startLoading(`reservation-${id}`);
    this.reservationDataService.setLoading(true);
    this.reservationService.cancelReservation(id).pipe(
      finalize(() => {
        this.loadingService.stopLoading(`reservation-${id}`);
        this.reservationDataService.setLoading(false);
      })
    ).subscribe({
      next: () => this.loadReservations(this.currentFilters()),
      error: error => this.reservationDataService.setError(error.message)
    });
  }

  retry(): void {
    this.loadReservations(this.currentFilters());
  }

  private currentFilters(): ReservationFilters | undefined {
    return this.selectedStatus === 'TOUS' ? undefined : { status: this.selectedStatus };
  }
}
