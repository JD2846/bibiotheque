import { ReservationListComponent } from './reservation-list.component';
import { Reservation } from '../../../_model/reservation.model';
import { ReservationStatus } from '../../../_model/reservation-status.enum';

describe('ReservationListComponent', () => {
  let component: ReservationListComponent;

  beforeEach(() => {
    component = new ReservationListComponent();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('devrait afficher le libellé du statut', () => {
    expect(component.getStatusLabel(ReservationStatus.EN_ATTENTE)).toBeTruthy();
    expect(component.getStatusLabel(ReservationStatus.DISPONIBLE)).toBeTruthy();
    expect(component.getStatusLabel(ReservationStatus.ANNULEE)).toBeTruthy();
  });

  function reservationWithStatus(status: ReservationStatus): Reservation {
    return {
      id: 1,
      bookId: 1,
      bookTitle: 'Test Book',
      userId: 1,
      userName: 'Adhérent 1',
      reservationDate: '2026-08-28T10:00:00',
      expirationDate: '2026-09-04T10:00:00',
      status,
      active: true
    };
  }

  it("devrait permettre l'annulation uniquement pour EN_ATTENTE ou DISPONIBLE", () => {
    expect(component.canCancel(reservationWithStatus(ReservationStatus.EN_ATTENTE))).toBe(true);
    expect(component.canCancel(reservationWithStatus(ReservationStatus.DISPONIBLE))).toBe(true);
    expect(component.canCancel(reservationWithStatus(ReservationStatus.ANNULEE))).toBe(false);
    expect(component.canCancel(reservationWithStatus(ReservationStatus.EXPIREE))).toBe(false);
    expect(component.canCancel(reservationWithStatus(ReservationStatus.HONOREE))).toBe(false);
  });

  it('devrait émettre l id de la réservation lors de la confirmation d annulation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(component.reservationCancelled, 'emit');

    component.cancelReservation(reservationWithStatus(ReservationStatus.EN_ATTENTE));

    expect(component.reservationCancelled.emit).toHaveBeenCalledWith(1);
  });

  it('ne devrait rien émettre si l utilisateur annule la confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    spyOn(component.reservationCancelled, 'emit');

    component.cancelReservation(reservationWithStatus(ReservationStatus.EN_ATTENTE));

    expect(component.reservationCancelled.emit).not.toHaveBeenCalled();
  });

  it('devrait formater la date correctement', () => {
    const formatted = component.formatDate('2026-08-28T10:00:00');
    expect(formatted).toContain('2026');
  });

  it('devrait retourner une chaîne vide pour une date absente', () => {
    expect(component.formatDate('')).toBe('');
  });
});
