import { ChangeDetectorRef, Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Books } from '../../../_model/books';
import { Borrow } from '../../../_model/borrow';
import { Users } from '../../../_model/users';
import { BooksService } from '../../services/books.service';
import { BorrowService } from '../../../borrow/services/borrow.service';
import { UsersService } from '../../../users/services/users.service';

@Component({
    selector: 'app-book-details',
    templateUrl: './book-details.component.html',
    styleUrls: ['./book-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager
})
export class BookDetailsComponent implements OnInit {

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
    this.id = this.route.snapshot.params['bookId'];
    this.book = new Books();
    this.bookService.getBookById(this.id).subscribe( data => {
      this.book = data;
      this.cdr.detectChanges();
    })

    this.getBorrowHistory(this.id);

  }

  private getBorrowHistory(bookId: number) {
    this.borrowService.getBookBorrowHistory(bookId).subscribe(data => {
      this.borrow = data;
      this.cdr.detectChanges();
    });
  }

  public getUserData(userId: number):string {
    this.user = new Users();
    this.userService.getUserById(userId).subscribe( data => {
      this.user = data;
      this.cdr.detectChanges();
    })
    return this.user.name;
  }
}
