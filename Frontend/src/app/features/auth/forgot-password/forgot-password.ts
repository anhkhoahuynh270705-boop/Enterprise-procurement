import { ToastService } from '../../../core/services/toast.service';
import { ConfirmationDialogService } from '../../../core/services/confirmation-dialog.service';
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
  private readonly dialogs = inject(ConfirmationDialogService);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  forgotForm: FormGroup = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  isLoading = false;
  isSubmitted = false;

  get f() {
    return this.forgotForm.controls;
  }

  async onSubmit(): Promise<void> {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }
    const email = String(this.forgotForm.get('email')?.value ?? '').trim();
    if (!(await this.dialogs.confirm({
      title: 'Gửi email đặt lại mật khẩu',
      message: `Gửi hướng dẫn đặt lại mật khẩu đến ${email}?`,
      confirmText: 'Gửi email',
    }))) return;
    this.isLoading = true;
    this.authService.forgotPassword({ email }).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSubmitted = true;
        this.toast.success('Yêu cầu đặt lại mật khẩu đã được tiếp nhận. Hãy kiểm tra email của bạn.');
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
