import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { UserAuthService } from '../_service/user-auth.service';
import { Injectable } from '@angular/core';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private userAuthService: UserAuthService,
    private router:Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.headers.get('No-Auth') === 'True') {
      return next.handle(req.clone());
    }

    const token = this.userAuthService.getToken();

    req = this.addToken(req, token);

    return next.handle(req).pipe(
        catchError(
            (err:HttpErrorResponse) => {
                // Un 401 signifie que le token est absent/invalide/expire : la session
                // entiere n'est plus valide, direction connexion.
                // Un 403 en revanche ne veut dire que "cette action precise vous est
                // refusee" (ex: un ADHERENT qui n'a pas accede a un widget reserve au
                // BIBLIOTHECAIRE) - rediriger toute la page vers /forbidden casserait
                // des ecrans ou l'utilisateur a par ailleurs parfaitement le droit
                // d'etre. On laisse chaque composant afficher son propre message
                // d'erreur (deja gere via error.message partout dans l'app). Seul
                // AuthGuard redirige vers /forbidden, au niveau route.
                if(err.status === 401) {
                    this.router.navigate(['/login']);
                }
                return throwError(() => err);
            }
        )
    );
  }

  private addToken(request:HttpRequest<any>, token:string) {
      return request.clone(
          {
              setHeaders: {
                  Authorization : `Bearer ${token}`
              }
          }
      );
  }
}