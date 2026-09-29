import { ToastService } from '../../../core/services/toast.service';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UpdateUserRequest, Users } from '../models/user';
import { UserService } from '../services/user.service';
import { AuthService } from '../../auth/services/auth.service';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { UserProfileFields } from '../profile-fields/profile-fields';
import { normalizeProfile, profileControls } from '../profile-fields/profile-form';

@Component({
  imports: [ReactiveFormsModule, RouterLink, UserProfileFields],
  selector: 'app-edit-user',
  styleUrl: './edit-user.scss',
  templateUrl: './edit-user.html',
})
export class EditUser implements OnInit {
  private readonly toast = inject(ToastService);

  users: Users[] = [];
  loading = false;
  userId!: string;

  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.userId = id;
    this.loadUsers();
  }
  isSelf = false;

  loadUsers(): void {
    this.loading = true;
    this.userService.getUsersById(this.userId).subscribe({
      next: (user) => {
        this.Userform.patchValue({ ...user, employeeStatus: user.employeeStatus || 'ACTIVE' });
        const current = this.authService.getCurrentUser();
        this.isSelf =
          !!current &&
          ((!!current.username &&
            !!user.username &&
            current.username.toLowerCase() === user.username.toLowerCase()) ||
            (!!current.email &&
              !!user.email &&
              current.email.toLowerCase() === user.email.toLowerCase()));
        if (this.isSelf) {
          this.Userform.controls.enabled.disable();
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
  Userform = this.fb.nonNullable.group({
    ...profileControls(this.fb),
    fullName: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    role: ['', Validators.required],
    enabled: [true, Validators.required],
  });
  onUpdate(): void {
    if (this.loading || this.Userform.invalid) {
      this.Userform.markAllAsTouched();
      return;
    }
    const data: UpdateUserRequest = normalizeProfile({
      ...this.Userform.getRawValue(),
      fullName: this.Userform.controls.fullName.value.trim(),
    });
    if (!this.userId) {
      return;
    }

    if (this.isSelf && data.enabled === false) {
      return;
    }

    this.loading = true;
    this.userService.updateUser(this.userId, data).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/users']);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || 'Có lỗi xảy ra khi cập nhật.';
        this.toast.error(msg);
      },
    });
  }
}
