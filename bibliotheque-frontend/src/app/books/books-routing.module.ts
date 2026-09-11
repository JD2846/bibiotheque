import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../_auth/auth.guard';
import { BookDetailsComponent } from './components/book-details/book-details.component';
import { BooksListComponent } from './components/books-list/books-list.component';
import { CreateBookComponent } from './components/create-book/create-book.component';
import { UpdateBookComponent } from './components/update-book/update-book.component';

const routes: Routes = [
  { path: '', component: BooksListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'create', component: CreateBookComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update/:bookId', component: UpdateBookComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'details/:bookId', component: BookDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BooksRoutingModule { }
