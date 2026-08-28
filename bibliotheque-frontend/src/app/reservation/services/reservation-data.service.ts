import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Reservation } from '../../_model/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class ReservationDataService {
  private readonly reservationsSubject = new BehaviorSubject<Reservation[]>([]);
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  private readonly errorSubject = new BehaviorSubject<string>('');

  readonly reservations$ = this.reservationsSubject.asObservable();
  readonly loading$ = this.loadingSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

  setReservations(reservations: Reservation[]): void {
    this.reservationsSubject.next(reservations);
  }

  setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  setError(error: string): void {
    this.errorSubject.next(error);
  }

  clearError(): void {
    this.errorSubject.next('');
  }

  getReservations(): Observable<Reservation[]> {
    return this.reservations$;
  }

  addReservation(reservation: Reservation): void {
    this.reservationsSubject.next([reservation, ...this.reservationsSubject.value]);
  }

  updateReservation(id: number, updated: Reservation): void {
    this.reservationsSubject.next(
      this.reservationsSubject.value.map(reservation => reservation.id === id ? updated : reservation)
    );
  }

  removeReservation(id: number): void {
    this.reservationsSubject.next(
      this.reservationsSubject.value.filter(reservation => reservation.id !== id)
    );
  }
}
