import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { Users } from '../../../_model/users';
import { UsersService } from '../../services/users.service';

@Component({
    selector: 'app-registration',
    templateUrl: './registration.component.html',
    styleUrls: ['./registration.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class RegistrationComponent implements OnInit {

  user: Users = new Users();
  constructor(private usersService: UsersService,
    private router: Router) { }

  ngOnInit(): void {
  }

  saveUser() {
    this.usersService.createUser(this.user).subscribe(data => {
      console.log(data);
      this.goToUsersList();
    },
    error => console.log(error));
  }

  goToUsersList() {
    this.router.navigate(['/users']);
  }

  onSubmit() {
    console.log(this.user);
    this.saveUser();
  }

}
