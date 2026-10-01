import { ToastService } from '../../../core/services/toast.service';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPassword {
  private readonly toast = inject(ToastService);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  isLoading = false;

  isSubmitted = false;

  get f() {
    return this.forgotForm.controls;
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    const { email } = this.forgotForm.value;
    this.authService.forgotPassword({ email }).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSubmitted = true;
      },
      error: (err) => {
        this.isLoading = false;
        if (err.error?.message) {
          this.toast.error(err.error.message);
        } else {
          this.toast.error('Đã xảy ra lỗi. Vui lòng thử lại.');
        }
      },
    });
  }

  resend(): void {
    this.isSubmitted = false;
    this.forgotForm.reset();
  }
}
