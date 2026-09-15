import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './_auth/auth.guard';
import { ForbiddenComponent } from './forbidden/forbidden.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent, data: { hideChrome: true } },
  { path: 'forbidden', component: ForbiddenComponent },
  {
    path: 'books',
    loadChildren: () => import('./books/books.module').then(m => m.BooksModule),
    canActivate: [AuthGuard],
    data: { roles: ['Admin'] }
  },
  {
    path: 'users',
    loadChildren: () => import('./users/users.module').then(m => m.UsersModule),
    canActivate: [AuthGuard],
    data: { roles: ['Admin'] }
  },
  {
    path: 'borrow',
    loadChildren: () => import('./borrow/borrow.module').then(m => m.BorrowModule),
    canActivate: [AuthGuard],
    data: { roles: ['Admin', 'User'] }
  },
  {
    path: 'reservations',
    loadChildren: () => import('./reservation/reservation.module').then(m => m.ReservationModule),
    canActivate: [AuthGuard],
    data: { roles: ['Admin', 'User'] }
  },
  { path: 'create-book', redirectTo: '/books/create' },
  { path: 'update-book/:bookId', redirectTo: '/books/update/:bookId' },
  { path: 'book-details/:bookId', redirectTo: '/books/details/:bookId' },
  { path: 'register-user', redirectTo: '/users/register' },
  { path: 'user-details/:userId', redirectTo: '/users/details/:userId' },
  { path: 'update-user/:userId', redirectTo: '/users/update/:userId' },
  { path: 'borrow-book', redirectTo: '/borrow' },
  { path: 'return-book', redirectTo: '/borrow/return' },
  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
