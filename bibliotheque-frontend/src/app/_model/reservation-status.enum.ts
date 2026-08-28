export enum ReservationStatus {
  EN_ATTENTE = 'EN_ATTENTE',
  DISPONIBLE = 'DISPONIBLE',
  ANNULEE = 'ANNULEE',
  EXPIREE = 'EXPIREE',
  HONOREE = 'HONOREE'
}

export const ReservationStatusLabels: Record<ReservationStatus | 'TOUS', string> = {
  TOUS: 'Tous',
  [ReservationStatus.EN_ATTENTE]: 'En attente',
  [ReservationStatus.DISPONIBLE]: 'Disponible',
  [ReservationStatus.ANNULEE]: 'Annulee',
  [ReservationStatus.EXPIREE]: 'Expiree',
  [ReservationStatus.HONOREE]: 'Honoree'
};

export const ReservationStatusColors: Record<ReservationStatus, string> = {
  [ReservationStatus.EN_ATTENTE]: 'warning',
  [ReservationStatus.DISPONIBLE]: 'success',
  [ReservationStatus.ANNULEE]: 'secondary',
  [ReservationStatus.EXPIREE]: 'danger',
  [ReservationStatus.HONOREE]: 'info'
};

export const ReservationStatusList: Array<ReservationStatus | 'TOUS'> = [
  'TOUS',
  ReservationStatus.EN_ATTENTE,
  ReservationStatus.DISPONIBLE,
  ReservationStatus.ANNULEE,
  ReservationStatus.EXPIREE,
  ReservationStatus.HONOREE
];
