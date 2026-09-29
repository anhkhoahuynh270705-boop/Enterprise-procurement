import { ToastService } from '../../../core/services/toast.service';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../services/user.service';
import { Router, RouterLink } from '@angular/router';
import type { CreateUserRequest } from '../models/user';
import { UserProfileFields } from '../profile-fields/profile-fields';
import { normalizeProfile, profileControls } from '../profile-fields/profile-form';
@Component({
  imports: [ReactiveFormsModule, RouterLink, UserProfileFields],
  selector: 'app-create-user',
  styleUrl: './create-user.scss',
  templateUrl: './create-user.html',
})
export class CreateUser {
  private readonly toast = inject(ToastService);

  loading = false;
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  form = this.fb.nonNullable.group({
    ...profileControls(this.fb),
    fullName: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    username: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: ['USER', [Validators.required]],
    enabled: [true, [Validators.required]],
  });

  Submit(): void {
    if (this.loading || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const data: CreateUserRequest = normalizeProfile({
      ...this.form.getRawValue(),
      fullName: this.form.controls.fullName.value.trim(),
    });
    this.loading = true;
    this.userService.addUser(data).subscribe({
      next: (res) => {
        this.loading = false;
        this.router.navigate(['/users']);
        console.log(res);
        this.form.reset({
          enabled: true,
          role: 'USER',
          employeeStatus: 'ACTIVE',
        });
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        this.toast.error(err.error?.message || 'Failed to create user');
      },
    });
  }
}
