import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService, Book, Member, BorrowRecord } from '../../services/api.service';

@Component({
  selector: 'app-borrow',
  templateUrl: './borrow.component.html',
  styleUrls: ['./borrow.component.scss']
})
export class BorrowComponent implements OnInit {
  books: Book[] = [];
  members: Member[] = [];
  lastRecord: BorrowRecord | null = null;
  submitting = false;
  returning = false;

  borrowRecords: BorrowRecord[] = [];
  loadingRecords = false;
  deletingId: number | null = null;
  displayedColumns = ['id', 'book', 'member', 'borrowTime', 'returnTime', 'actions'];

  borrowForm: FormGroup;
  returnForm: FormGroup;

  @ViewChild('borrowFormDirective') borrowFormDirective!: FormGroupDirective;
  @ViewChild('returnFormDirective') returnFormDirective!: FormGroupDirective;

  constructor(private api: ApiService, private fb: FormBuilder, private snack: MatSnackBar) {
    this.borrowForm = this.fb.group({
      bookId: ['', Validators.required],
      memberId: ['', Validators.required]
    });
    this.returnForm = this.fb.group({
      borrowId: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.api.getBooks().subscribe(data => this.books = data);
    this.api.getMembers().subscribe(data => this.members = data);
    this.loadRecords();
  }

  loadRecords() {
    this.loadingRecords = true;
    this.api.getBorrowRecords().subscribe({
      next: data => { this.borrowRecords = data; this.loadingRecords = false; },
      error: () => {
        this.snack.open('Failed to load borrow records', 'Close', { duration: 3000, panelClass: 'snack-error' });
        this.loadingRecords = false;
      }
    });
  }

  borrow() {
    if (this.borrowForm.invalid) return;
    this.submitting = true;
    const { bookId, memberId } = this.borrowForm.value;
    this.api.borrowBook(bookId, memberId).subscribe({
      next: rec => {
        this.lastRecord = rec;
        this.snack.open('Book borrowed successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.borrowFormDirective.resetForm();
        this.submitting = false;
        this.loadRecords();
      },
      error: () => {
        this.snack.open('Failed to borrow book', 'Close', { duration: 3000, panelClass: 'snack-error' });
        this.submitting = false;
      }
    });
  }

  returnBook() {
    if (this.returnForm.invalid) return;
    this.returning = true;
    this.api.returnBook(this.returnForm.value.borrowId).subscribe({
      next: rec => {
        this.lastRecord = rec;
        this.snack.open('Book returned successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.returnFormDirective.resetForm();
        this.returning = false;
        this.loadRecords();
      },
      error: () => {
        this.snack.open('Failed to return book', 'Close', { duration: 3000, panelClass: 'snack-error' });
        this.returning = false;
      }
    });
  }

  deleteBorrow(record: BorrowRecord) {
    if (!record.id) return;
    if (!confirm(`Delete borrow record #${record.id}? This cannot be undone.`)) return;
    this.deletingId = record.id;
    this.api.deleteBorrow(record.id).subscribe({
      next: () => {
        this.snack.open('Borrow record deleted successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.deletingId = null;
        if (this.lastRecord?.id === record.id) this.lastRecord = null;
        this.loadRecords();
      },
      error: (err) => {
        const msg = err?.error?.error || 'Failed to delete borrow record';
        this.snack.open(msg, 'Close', { duration: 4000, panelClass: 'snack-error' });
        this.deletingId = null;
      }
    });
  }
}
