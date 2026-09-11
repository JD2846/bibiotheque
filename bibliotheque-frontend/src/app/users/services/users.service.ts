import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiBaseService } from '../../_core/services/api-base.service';
import { ErrorHandlerService } from '../../_core/services/error-handler.service';
import { Users } from '../../_model/users';
import { UserAuthService } from '../../_service/user-auth.service';

@Injectable({
  providedIn: 'root'
})
export class UsersService extends ApiBaseService {

  private readonly endpoint = '/admin/users';
  requestHeader = new HttpHeaders(
    { 'No-Auth': 'True' }
  );

  constructor(
    http: HttpClient,
    errorHandler: ErrorHandlerService,
    private userAuthService: UserAuthService
  ) {
    super(http, errorHandler);
  }

  public login(loginData: NgForm) {
    return this.http.post(`${this.apiUrl}/authenticate`, loginData, {
      headers: this.requestHeader,
    }).pipe(catchError(error => this.errorHandler.handleError(error)));
  }

  public roleMatch(allowedRoles: any): boolean {
    const userRoles: any = this.userAuthService.getRoles();

    if (userRoles != null && userRoles) {
      for (let i = 0; i < userRoles.length; i++) {
        for (let j = 0; j < allowedRoles.length; j++) {
          if (userRoles[i].roleName === allowedRoles[j]) {
            return true;
          }
        }
      }
    }

    return false;
  }

  getUsers(): Observable<Users[]> {
    return this.get<Users[]>(this.endpoint);
  }

  getUsersList(): Observable<Users[]> {
    return this.getUsers();
  }

  getUserById(id: number): Observable<Users> {
    return this.get<Users>(`${this.endpoint}/${id}`);
  }

  createUser(user: Users): Observable<Users> {
    return this.post<Users>(this.endpoint, user);
  }

  updateUser(id: number, user: Users): Observable<Users> {
    return this.put<Users>(`${this.endpoint}/${id}`, user);
  }

  deleteUser(id: number): Observable<void> {
    return this.delete<void>(`${this.endpoint}/${id}`);
  }

  searchUsers(keyword: string): Observable<Users[]> {
    return this.get<Users[]>(`${this.endpoint}/search`, { keyword });
  }

}
