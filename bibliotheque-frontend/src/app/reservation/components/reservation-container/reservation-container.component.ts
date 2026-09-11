import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../../../_core/services/loading.service';
import { Reservation, ReservationFilters } from '../../../_model/reservation.model';
import { ReservationStatus, ReservationStatusList } from '../../../_model/reservation-status.enum';
import { ReservationDataService } from '../../services/reservation-data.service';
import { ReservationService } from '../../services/reservation.service';

@Component({
    selector: 'app-reservation-container',
    templateUrl: './reservation-container.component.html',
    styleUrls: ['./reservation-container.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationContainerComponent implements OnInit, OnDestroy {
  reservations: Reservation[] = [];
  loading = false;
  error = '';
  selectedStatus: ReservationStatus | 'TOUS' = 'TOUS';
  statusList = ReservationStatusList;

  private readonly subscriptions = new Subscription();

  constructor(
    private reservationService: ReservationService,
    private reservationDataService: ReservationDataService,
    private loadingService: LoadingService
  ) { }

  ngOnInit(): void {
    this.subscriptions.add(this.reservationDataService.reservations$.subscribe(reservations => this.reservations = reservations));
    this.subscriptions.add(this.reservationDataService.loading$.subscribe(loading => this.loading = loading));
    this.subscriptions.add(this.reservationDataService.error$.subscribe(error => this.error = error));
    this.loadReservations();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
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
