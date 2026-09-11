import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { Reservation } from '../../../_model/reservation.model';
import { ReservationStatus, ReservationStatusColors, ReservationStatusLabels } from '../../../_model/reservation-status.enum';

@Component({
    selector: 'app-reservation-list',
    templateUrl: './reservation-list.component.html',
    styleUrls: ['./reservation-list.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReservationListComponent {
  @Input() reservations: Reservation[] = [];
  @Input() loading = false;
  @Input() error = '';

  @Output() retry = new EventEmitter<void>();
  @Output() reservationCancelled = new EventEmitter<number>();

  getStatusLabel(status: ReservationStatus): string {
    return ReservationStatusLabels[status] || status;
  }

  getStatusColor(status: ReservationStatus): string {
    return ReservationStatusColors[status] || 'secondary';
  }

  canCancel(reservation: Reservation): boolean {
    return reservation.status === ReservationStatus.EN_ATTENTE || reservation.status === ReservationStatus.DISPONIBLE;
  }

  cancelReservation(reservation: Reservation): void {
    if (confirm('Confirmer l annulation de cette reservation ?')) {
      this.reservationCancelled.emit(reservation.id);
    }
  }

  formatDate(value: string | Date): string {
    return value ? new Date(value).toLocaleDateString('fr-FR') : '';
  }
}
