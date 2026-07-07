import { Component, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService, Member } from '../../services/api.service';
import { SubmitErrorMatcher } from '../../services/submit-error-matcher';

@Component({
  selector: 'app-members',
  templateUrl: './members.component.html',
  styleUrls: ['./members.component.scss']
})
export class MembersComponent {
  members: Member[] = [];
  loading = false;
  submitting = false;
  submitted = false;
  displayedColumns = ['id', 'name', 'email', 'age', 'actions'];
  deletingId: number | null = null;

  form: FormGroup;
  matcher: SubmitErrorMatcher;

  @ViewChild('memberFormDirective') formDirective!: FormGroupDirective;

  constructor(private api: ApiService, private fb: FormBuilder, private snack: MatSnackBar) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      age: ['', [Validators.required, Validators.min(1)]]
    });
    this.matcher = new SubmitErrorMatcher(() => this.submitted);
  }

  load() {
    this.loading = true;
    this.api.getMembers().subscribe({
      next: data => { this.members = data; this.loading = false; },
      error: () => {
        this.snack.open('Failed to load members', 'Close', { duration: 3000, panelClass: 'snack-error' });
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
    this.api.createMember(this.form.value).subscribe({
      next: () => {
        this.snack.open('Member added successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.resetForm();
        this.submitting = false;
      },
      error: (err) => {
        const msg = err?.error?.error || 'Failed to add member';
        this.snack.open(msg, 'Close', { duration: 4000, panelClass: 'snack-error' });
        this.submitting = false;
      }
    });
  }

  deleteMember(member: Member) {
    if (!member.id) return;
    if (!confirm(`Delete member "${member.name}"? This cannot be undone.`)) return;
    this.deletingId = member.id;
    this.api.deleteMember(member.id).subscribe({
      next: () => {
        this.snack.open('Member deleted successfully!', 'Close', { duration: 3000, panelClass: 'snack-success' });
        this.deletingId = null;
        this.load();
      },
      error: (err) => {
        const msg = err?.error?.error || 'Failed to delete member';
        this.snack.open(msg, 'Close', { duration: 4000, panelClass: 'snack-error' });
        this.deletingId = null;
      }
    });
  }
}
