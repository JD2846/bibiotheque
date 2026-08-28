import { Component, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../users/services/users.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  errorMessage = '';
  loading = false;

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private router: Router
  ) { }

  ngOnInit() {
  }

  login(loginForm: NgForm) {
    if (loginForm.invalid) {
      loginForm.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.userService.login(loginForm.value).subscribe(
      (response: any) => {
        this.userAuthSerivce.setRoles(response.user.role);
        this.userAuthSerivce.setToken(response.jwtToken);
        this.userAuthSerivce.setUserId(response.user.userId);
        this.userAuthSerivce.setName(response.user.name);

        const role = response.user.role[0].roleName;
        if (role === 'Admin') {
          this.router.navigate(['/books']);
        } else {
          this.router.navigate(['/borrow']);
        }
      },
      (error: any) => {
        this.loading = false;
        this.errorMessage = error?.error?.message
          || error?.message
          || 'Identifiants invalides. Veuillez réessayer.';
      },
      () => {
        this.loading = false;
      }
    );
  }

}
