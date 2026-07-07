import { ErrorStateMatcher } from '@angular/material/core';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';

export class SubmitErrorMatcher implements ErrorStateMatcher {
  constructor(private isSubmitted: () => boolean) {}

  isErrorState(control: FormControl | null): boolean {
    return !!(control && control.invalid && this.isSubmitted());
  }
}
