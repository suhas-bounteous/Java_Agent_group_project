import { Component, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService, Book } from '../../services/api.service';
import { SubmitErrorMatcher } from '../../services/submit-error-matcher';

@Component({
  selector: 'app-books',
  templateUrl: './books.component.html',
  styleUrls: ['./books.component.scss']
})
export class BooksComponent {
  books: Book[] = [];
  loading = false;
  submitting = false;
  submitted = false;
  displayedColumns = ['id', 'bookName', 'author', 'genre', 'actions'];
  deletingId: number | null = null;

  form: FormGroup;
  matcher: SubmitErrorMatcher;

  @ViewChild('bookFormDirective') formDirective!: FormGroupDirective;

  constructor(private api: ApiService, private fb: FormBuilder, private snack: MatSnackBar) {
    this.form = this.fb.group({
      bookName: ['', Validators.required],
      author: ['', Validators.required],
      genre: ['', Validators.required]
    });
    this.matcher = new SubmitErrorMatcher(() => this.submitted);
  }

  load() {
    this.loading = true;
    this.api.getBooks().subscribe({
      next: data => { this.books = data; this.loading = false; },
      error: () => {
        this.snack.open('Failed to load books', 'Close', { duration: 3000, panelClass: 'snack-error' });
        this.loading = false;
      }
    });
  }

  resetForm() {
    this.submitted = false;
    this.formDirective.resetForm();
  }

  submit() {
    this.submitted = true;
    if (this.form.invalid) return;
    this.submitting = true;
    this.api.createBook(this.form.value).subscribe({
      next: () => {
        this.snack.open('Book added successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.resetForm();
        this.submitting = false;
      },
      error: (err) => {
        const msg = err?.error?.error || 'Failed to add book';
        this.snack.open(msg, 'Close', { duration: 4000, panelClass: 'snack-error' });
        this.submitting = false;
      }
    });
  }

  deleteBook(book: Book) {
    if (!book.id) return;
    if (!confirm(`Delete "${book.bookName}"? This cannot be undone.`)) return;
    this.deletingId = book.id;
    this.api.deleteBook(book.id).subscribe({
      next: () => {
        this.snack.open('Book deleted successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.deletingId = null;
        this.load();
      },
      error: (err) => {
        const msg = err?.error?.error || 'Failed to delete book';
        this.snack.open(msg, 'Close', { duration: 4000, panelClass: 'snack-error' });
        this.deletingId = null;
      }
    });
  }
}
