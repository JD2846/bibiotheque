import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Books } from '../../../_model/books'
import { BooksService } from '../../services/books.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];
  loading = false;
  error = '';

  constructor(private booksService: BooksService,
    private router: Router) { }

  ngOnInit(): void {
    this.getBooks();
  }

  getBooks() {
    this.loading = true;
    this.error = '';
    this.booksService.getBooks().pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: data => this.books = data,
      error: error => this.error = error.message
    });
  }

  updateBook(bookId: number) {
    this.router.navigate(['/books/update', bookId ]);
  }

  deleteBook(bookId: number) {
    this.booksService.deleteBook(bookId).subscribe({
      next: () => this.getBooks(),
      error: error => this.error = error.message
    });
  }

  bookDetails(bookId: number) {
    this.router.navigate(['/books/details', bookId ]);
  }

}
