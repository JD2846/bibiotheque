import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReservationService } from './reservation.service';
import { Reservation, ReservationRequest } from '../../_model/reservation.model';
import { ReservationStatus } from '../../_model/reservation-status.enum';

describe('ReservationService', () => {
  let service: ReservationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReservationService]
    });
    service = TestBed.inject(ReservationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait récupérer la liste des réservations', () => {
    const mockReservations: Reservation[] = [
      {
        id: 1,
        bookId: 1,
        bookTitle: 'Test Book',
        userId: 1,
        userName: 'Adhérent 1',
        reservationDate: '2026-08-28T10:00:00',
        expirationDate: '2026-09-04T10:00:00',
        status: ReservationStatus.EN_ATTENTE,
        active: true
      }
    ];

    service.getReservations().subscribe(reservations => {
      expect(reservations.length).toBe(1);
      expect(reservations[0].bookTitle).toBe('Test Book');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reservations');
    expect(req.request.method).toBe('GET');
    req.flush(mockReservations);
  });

  it('devrait créer une réservation', () => {
    const request: ReservationRequest = {
      bookId: 1,
      adherentId: 1
    };

    const mockResponse: Reservation = {
      id: 100,
      bookId: 1,
      bookTitle: 'Test Book',
      userId: 1,
      userName: 'Adhérent 1',
      reservationDate: '2026-08-28T10:00:00',
      expirationDate: '2026-09-04T10:00:00',
      status: ReservationStatus.EN_ATTENTE,
      active: true
    };

    service.createReservation(request).subscribe(reservation => {
      expect(reservation.id).toBe(100);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reservations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('devrait annuler une réservation sur le bon endpoint (/annuler)', () => {
    service.cancelReservation(1).subscribe(reservation => {
      expect(reservation.status).toBe(ReservationStatus.ANNULEE);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reservations/1/annuler');
    expect(req.request.method).toBe('PATCH');
    req.flush({ status: ReservationStatus.ANNULEE });
  });

  it('devrait propager le vrai message backend sur une erreur 409', () => {
    const request: ReservationRequest = { bookId: 1, adherentId: 1 };

    service.createReservation(request).subscribe({
      next: () => fail('devrait échouer'),
      error: (error) => {
        expect(error.status).toBe(409);
        expect(error.message).toContain('RG-01');
      }
    });

    const req = httpMock.expectOne('http://localhost:8080/api/reservations');
    // Le backend (ReservationController/GlobalExceptionHandler) renvoie le message
    // d'erreur en texte brut, pas en JSON {message: ...}.
    req.flush('RG-01: Impossible de réserver un livre disponible', { status: 409, statusText: 'Conflict' });
  });
});
