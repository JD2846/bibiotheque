import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Books } from '../../../_model/books';
import { Borrow } from '../../../_model/borrow';
import { Users } from '../../../_model/users';
import { BooksService } from '../../../books/services/books.service';
import { BorrowService } from '../../../borrow/services/borrow.service';
import { UsersService } from '../../services/users.service';

@Component({
    selector: 'app-user-details',
    templateUrl: './user-details.component.html',
    styleUrls: ['./user-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager
})
export class UserDetailsComponent implements OnInit {

  id: number;
  book: Books;
  borrow: Borrow[];
  user: Users;

  constructor(private route: ActivatedRoute,
    private bookService: BooksService,
    private borrowService: BorrowService,
    public userService: UsersService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['userId'];
    this.user = new Users();
    this.userService.getUserById(this.id).subscribe( data => {
      this.user = data;
      this.cdr.detectChanges();
    })

    this.getBorrowedByUser(this.id);

  }

  private getBorrowedByUser(userId: number) {
    this.borrowService.getBooksBorrowedByUser(userId).subscribe(data => {
      this.borrow = data;
      this.cdr.detectChanges();
    });
  }

}
