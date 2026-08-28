import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../../_core/services/api-base.service';
import { ErrorHandlerService } from '../../_core/services/error-handler.service';
import { Reservation, ReservationFilters, ReservationRequest } from '../../_model/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class ReservationService extends ApiBaseService {
  private readonly endpoint = '/api/reservations';

  constructor(http: HttpClient, errorHandler: ErrorHandlerService) {
    super(http, errorHandler);
  }

  createReservation(request: ReservationRequest): Observable<Reservation> {
    return this.post<Reservation>(this.endpoint, request);
  }

  getReservations(filters?: ReservationFilters): Observable<Reservation[]> {
    return this.get<Reservation[]>(this.endpoint, filters);
  }

  getReservationById(id: number): Observable<Reservation> {
    return this.get<Reservation>(`${this.endpoint}/${id}`);
  }

  cancelReservation(id: number): Observable<Reservation> {
    return this.patch<Reservation>(`${this.endpoint}/${id}/cancel`, {});
  }

  deleteReservation(id: number): Observable<void> {
    return this.delete<void>(`${this.endpoint}/${id}`);
  }

  getReservationsByStatus(status: string): Observable<Reservation[]> {
    return this.get<Reservation[]>(`${this.endpoint}/status/${status}`);
  }

  getReservationsByUser(userId: number): Observable<Reservation[]> {
    return this.get<Reservation[]>(`${this.endpoint}/user/${userId}`);
  }

  getActiveReservationsByUser(userId: number): Observable<Reservation[]> {
    return this.get<Reservation[]>(`${this.endpoint}/user/${userId}/active`);
  }
}
