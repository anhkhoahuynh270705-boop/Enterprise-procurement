import { SensitiveValue } from '../../shared/sensitive-value/sensitive-value';
import { Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { environment } from '../../../environments/environment';
import { Gender, UpdateMyProfileRequest, UserResponse } from '../../features/auth/model/auth';

function httpsUrl(control: AbstractControl) {
  if (!control.value?.trim()) return null;
  try {
    const url = new URL(control.value.trim());
    return url.protocol === 'https:' && !!url.hostname ? null : { https: true };
  } catch {
    return { https: true };
  }
}
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
  imports: [SensitiveValue, ReactiveFormsModule],
  templateUrl: './profile-panel.html',
  styleUrl: './profile-panel.scss',
})
export class ProfilePanel implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private request?: Subscription;
  readonly profile = signal<UserResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly saveError = signal('');
  readonly saved = signal(false);
  readonly avatarFailed = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    avatarUrl: ['', [Validators.maxLength(2048), httpsUrl]],
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
      avatarUrl: user.avatarUrl ?? '',
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
      avatarUrl: value.avatarUrl.trim() || null,
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

  avatarUrl(profile: UserResponse): string | null {
    if (this.avatarFailed() || !profile.avatarUrl) return null;
    try {
      const url = new URL(profile.avatarUrl);
      return url.protocol === 'https:' ? profile.avatarUrl : null;
    } catch {
      return null;
    }
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
          if (!profile) this.error.set('Không tìm thấy hồ sơ của bạn.');
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Không thể tải hồ sơ. Vui lòng thử lại.');
        },
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
  }
}
