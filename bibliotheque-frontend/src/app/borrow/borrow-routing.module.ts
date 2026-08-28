import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../_auth/auth.guard';
import { BorrowBookComponent } from './components/borrow-book/borrow-book.component';
import { ReturnBookComponent } from './components/return-book/return-book.component';

const routes: Routes = [
  { path: '', component: BorrowBookComponent, canActivate: [AuthGuard], data: { roles: ['User'] } },
  { path: 'return', component: ReturnBookComponent, canActivate: [AuthGuard], data: { roles: ['User'] } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BorrowRoutingModule { }
