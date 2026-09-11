import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Books } from '../../../_model/books';
import { BooksService } from '../../services/books.service';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-create-book',
    templateUrl: './create-book.component.html',
    styleUrls: ['./create-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule, RouterLink]
})
export class CreateBookComponent implements OnInit {

  book: Books = new Books();
  constructor(private booksService: BooksService,
    private router: Router) { }

  ngOnInit(): void {
  }

  saveBook() {
    this.booksService.createBook(this.book).subscribe(data => {
      console.log(data);
      this.goToBooksList();
    },
    error => console.log(error));
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

  onSubmit() {
    console.log(this.book);
    this.saveBook();
  }

}
