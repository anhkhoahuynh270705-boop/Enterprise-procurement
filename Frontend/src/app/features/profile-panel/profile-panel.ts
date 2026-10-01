import { SensitiveValue } from '../../shared/sensitive-value/sensitive-value';
import { Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { environment } from '../../../environments/environment';
import { Gender, UpdateMyProfileRequest, UserResponse } from '../../features/auth/model/auth';
import { FaceAuthService } from '../faceId/services/face-auth.service';
import { ToastService } from '../../core/services/toast.service';
import { FaceEnrollComponent } from '../faceId/face-enroll/face-enroll';

function pastDate(control: AbstractControl) {
  if (!control.value) return null;
  const value = control.value as string;
  const parsed = new Date(value + 'T00:00:00Z');
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  return /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    !isNaN(parsed.getTime()) &&
    parsed.toISOString().slice(0, 10) === value &&
    value <= today
    ? null
    : { date: true };
}

@Component({
  selector: 'app-profile-panel',
  standalone: true,
  imports: [SensitiveValue, ReactiveFormsModule, FaceEnrollComponent],
  templateUrl: './profile-panel.html',
  styleUrl: './profile-panel.scss',
})
export class ProfilePanel implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly faceAuth = inject(FaceAuthService);
  private readonly toast = inject(ToastService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private request?: Subscription;
  private faceCheckSub?: Subscription;
  readonly profile = signal<UserResponse | null>(null);
  readonly faceEnrolled = signal(false);
  readonly showEnrollDialog = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly uploadingAvatar = signal(false);
  readonly saveError = signal('');
  readonly saved = signal(false);
  readonly avatarFailed = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    phone: ['', [Validators.maxLength(30), Validators.pattern(/^[+\d\s().-]*$/)]],
    dateOfBirth: ['', pastDate],
    gender: ['', Validators.pattern(/^(MALE|FEMALE|OTHER|UNDISCLOSED)$/)],
    address: ['', Validators.maxLength(500)],
  });
  get today(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }

  edit(): void {
    const user = this.profile();
    if (!user || this.saving()) return;
    this.form.reset({
      phone: user.phone ?? '',
      dateOfBirth: user.dateOfBirth ?? '',
      gender: user.gender ?? '',
      address: user.address ?? '',
    });
    this.saveError.set('');
    this.saved.set(false);
    this.editing.set(true);
  }

  cancelEdit(): void {
    if (this.saving()) return;
    this.editing.set(false);
    this.saveError.set('');
  }

  save(): void {
    if (this.saving() || !this.editing()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: UpdateMyProfileRequest = {
      avatarUrl: this.profile()?.avatarUrl ?? null,
      phone: value.phone.trim() || null,
      dateOfBirth: value.dateOfBirth || null,
      gender: (value.gender || null) as Gender | null,
      address: value.address.trim() || null,
    };
    this.saveError.set('');
    this.saving.set(true);
    this.request = this.http
      .put<UserResponse>(environment.apiBaseUrl + '/users/me', body)
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.saving.set(false);
          this.editing.set(false);
          this.avatarFailed.set(false);
          this.saved.set(true);
        },
        error: () => {
          this.saving.set(false);
          this.saveError.set(
            'Không thể lưu hồ sơ. Thông tin bạn nhập vẫn được giữ lại. Vui lòng thử lại.',
          );
        },
      });
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    input.value = '';

    if (!file.type.startsWith('image/')) {
      this.toast.error('Chỉ được chọn file hình ảnh (JPG, PNG, WEBP, GIF).');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toast.error('Dung lượng file tối đa là 5MB.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.uploadingAvatar.set(true);
    this.http.post<UserResponse>(environment.apiBaseUrl + '/users/me/avatar', formData).subscribe({
      next: (updatedProfile) => {
        this.uploadingAvatar.set(false);
        this.avatarFailed.set(false);
        this.profile.set(updatedProfile);
        this.toast.success('Cập nhật ảnh đại diện thành công!');
      },
      error: (err: any) => {
        this.uploadingAvatar.set(false);
        const msg =
          err?.error?.message ||
          err?.error?.detail ||
          'Tải ảnh đại diện thất bại. Vui lòng thử lại.';
        this.toast.error(msg);
      },
    });
  }

  avatarUrl(profile: UserResponse): string | null {
    if (this.avatarFailed() || !profile.avatarUrl) return null;
    const raw = profile.avatarUrl.trim();
    if (raw.startsWith('/api/')) {
      const base = environment.apiBaseUrl.replace(/\/api\/?$/, '');
      return `${base}${raw}`;
    }
    if (
      raw.startsWith('http://') ||
      raw.startsWith('https://') ||
      raw.startsWith('data:image/')
    ) {
      return raw;
    }
    return null;
  }

  initials(profile: UserResponse): string {
    return this.fullName(profile)
      .split(/\s+/)
      .filter(Boolean)
      .map((part) => part.charAt(0))
      .filter((_, index, list) => index === 0 || index === list.length - 1)
      .join('')
      .toUpperCase();
  }

  genderLabel(gender?: string | null): string {
    return (
      (
        { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác', UNDISCLOSED: 'Không tiết lộ' } as Record<
          string,
          string
        >
      )[gender ?? ''] || 'Chưa cập nhật'
    );
  }

  open(): void {
    if (this.dialog().nativeElement.open) return;
    this.dialog().nativeElement.showModal();
    this.load();
  }

  load(): void {
    if (this.saving()) return;
    this.editing.set(false);
    this.saved.set(false);
    this.avatarFailed.set(false);
    this.request?.unsubscribe();
    this.profile.set(null);
    this.error.set('');
    this.loading.set(true);
    this.request = this.http
      .get<UserResponse | null>(environment.apiBaseUrl + '/users/me')
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.loading.set(false);
          if (!profile) {
            this.error.set('Không tìm thấy hồ sơ của bạn.');
          } else if (profile.username) {
            this.checkFaceEnrollment(profile.username);
          }
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Không thể tải hồ sơ. Vui lòng thử lại.');
        },
      });
  }

  checkFaceEnrollment(username: string): void {
    this.faceCheckSub?.unsubscribe();
    this.faceCheckSub = this.faceAuth.isEnrolled(username).subscribe({
      next: (res) => this.faceEnrolled.set(res.enrolled),
      error: () => this.faceEnrolled.set(false),
    });
  }

  close(): void {
    if (this.saving()) return;
    this.editing.set(false);
    this.request?.unsubscribe();
    this.dialog().nativeElement.close();
    this.profile.set(null);
    this.loading.set(false);
  }

  onCancel(event: Event): void {
    event.preventDefault();
    this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    const dialog = this.dialog().nativeElement;
    if (event.target !== dialog) return;
    const bounds = dialog.getBoundingClientRect();
    if (
      event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom
    )
      this.close();
  }

  fullName(profile: UserResponse): string {
    return (
      profile.fullName?.trim() ||
      [profile.firstName, profile.lastName].filter(Boolean).join(' ') ||
      profile.username
    );
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.faceCheckSub?.unsubscribe();
  }
}
