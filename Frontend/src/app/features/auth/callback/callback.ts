import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [RouterLink],
  template: `<main aria-live="polite">
    @if (failed()) {
      <h1>Không thể hoàn tất đăng nhập</h1>
      <p>Phiên đăng nhập đã hết hạn hoặc tài khoản chưa sẵn sàng. Vui lòng thử lại.</p>
      <a routerLink="/login">Quay lại đăng nhập</a>
    } @else {
      <p>Đang hoàn tất đăng nhập...</p>
    }
  </main>`,
  styles: [
    `
      main {
        max-width: 36rem;
        margin: 5rem auto;
        padding: 2rem;
      }
    `,
  ],
})
export class AuthCallback implements OnInit {
  readonly failed = signal(false);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly platform = inject(PLATFORM_ID);
  ngOnInit(): void {
    if (!isPlatformBrowser(this.platform)) return;
    this.auth.completeLogin().subscribe({
      next: (profile) => {
        if (!profile?.role) {
          this.failed.set(true);
          return;
        }
        void this.router.navigate([profile.role === 'ADMIN' ? '/dashboard' : '/procurement']);
      },
      error: () => this.failed.set(true),
    });
  }
}
