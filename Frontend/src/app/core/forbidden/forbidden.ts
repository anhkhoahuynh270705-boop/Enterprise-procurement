import { Component, inject } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';

@Component({
  standalone: true,
  template: `<main style="padding: 3rem; text-align: center">
    <h1>Không có quyền truy cập</h1>
    <p>Tài khoản chưa được cấp quyền phù hợp. Vui lòng liên hệ quản trị viên.</p>
    <button type="button" (click)="auth.logout()">Đăng xuất</button>
  </main>`,
})
export class Forbidden {
  readonly auth = inject(AuthService);
}
