import { ReservationStatus } from './reservation-status.enum';

export interface Reservation {
  id: number;
  bookId: number;
  bookTitle: string;
  userId: number;
  userName: string;
  reservationDate: string | Date;
  expirationDate: string | Date;
  status: ReservationStatus;
  active: boolean;
}

export interface ReservationRequest {
  bookId: number;
  adherentId: number;
}

export interface ReservationFilters {
  status?: ReservationStatus | string;
  userId?: number;
}
