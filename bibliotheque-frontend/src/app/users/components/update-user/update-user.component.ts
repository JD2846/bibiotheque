import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Users } from '../../../_model/users';
import { UsersService } from '../../services/users.service';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-update-user',
    templateUrl: './update-user.component.html',
    styleUrls: ['./update-user.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule, RouterLink]
})
export class UpdateUserComponent implements OnInit {

  userId: number;
  user: Users = new Users();
  constructor(private usersService: UsersService,
    private route: ActivatedRoute,
    private router: Router) { }

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.usersService.getUserById(this.userId).subscribe(data => {
      this.user = data;
    })
  }

  onSubmit() {
    this.usersService.updateUser(this.userId, this.user).subscribe( data =>{
        this.goToUsersList();
    },
    error => console.log(error));
  }

  goToUsersList() {
    this.router.navigate(['/users']);
  }

}
