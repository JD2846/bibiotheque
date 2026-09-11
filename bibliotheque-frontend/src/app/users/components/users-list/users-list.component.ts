import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Users } from '../../../_model/users';
import { UsersService } from '../../services/users.service';

@Component({
    selector: 'app-users-list',
    templateUrl: './users-list.component.html',
    styleUrls: ['./users-list.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterLink]
})
export class UsersListComponent implements OnInit {

  users: Users[] = [];
  loading = false;
  error = '';

  constructor(private usersService: UsersService,
    private router: Router) { }

  ngOnInit(): void {
    this.getUsers();
    // this.users = [{
    //   "userId": 1,
    //   "name": "tarun",
    //   "username": "tarungowda",
    //   "role": "STUDENT",
    //   "password": "sdklfjlakdsf"
    // }]
  }

  getUsers() {
    this.loading = true;
    this.error = '';
    this.usersService.getUsers().pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: data => this.users = data,
      error: error => this.error = error.message
    });
  }

  userDetails(userId: number) {
    this.router.navigate(['/users/details', userId ]);
  }

  updateUser(userId: number) {
    this.router.navigate(['/users/update', userId ]);
  }

  deleteUser(userId: number) {
    this.usersService.deleteUser(userId).subscribe({
      next: () => this.getUsers(),
      error: error => this.error = error.message
    });
  }

}
