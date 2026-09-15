import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';

export interface ApiError {
  status: number;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService {
  private readonly errorMessages: Record<number, string> = {
    0: 'Impossible de contacter le serveur. Veuillez verifier votre connexion.',
    400: 'Requete invalide. Veuillez verifier les champs.',
    401: 'Vous devez etre connecte pour effectuer cette action.',
    403: 'Vous n avez pas les droits necessaires.',
    404: 'Ressource non trouvee.',
    409: 'Conflit avec les regles de gestion.',
    500: 'Erreur interne du serveur. Veuillez reessayer plus tard.'
  };

  handleError(error: HttpErrorResponse) {
    const serverMessage = this.extractServerMessage(error);
    const apiError: ApiError = {
      status: error.status,
      message: this.getErrorMessage(error.status, serverMessage)
    };

    return throwError(() => apiError);
  }

  getErrorMessage(status: number, defaultMessage?: string): string {
    // Statuts 0 (reseau) et 500 (erreur serveur) : on garde un message generique
    // pour ne pas exposer de details techniques a l'utilisateur.
    if (defaultMessage && status !== 0 && status !== 500) {
      return defaultMessage;
    }

    return this.errorMessages[status] || defaultMessage || 'Une erreur inattendue est survenue.';
  }

  private extractServerMessage(error: HttpErrorResponse): string | undefined {
    if (typeof error.error === 'string') {
      return error.error;
    }

    return error.error?.message || error.message;
  }
}
