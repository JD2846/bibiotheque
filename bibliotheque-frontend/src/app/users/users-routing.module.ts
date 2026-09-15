import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../_auth/auth.guard';
import { RegistrationComponent } from './components/registration/registration.component';
import { UpdateUserComponent } from './components/update-user/update-user.component';
import { UserDetailsComponent } from './components/user-details/user-details.component';
import { UsersListComponent } from './components/users-list/users-list.component';

const routes: Routes = [
  { path: '', component: UsersListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'register', component: RegistrationComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'details/:userId', component: UserDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update/:userId', component: UpdateUserComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UsersRoutingModule { }
