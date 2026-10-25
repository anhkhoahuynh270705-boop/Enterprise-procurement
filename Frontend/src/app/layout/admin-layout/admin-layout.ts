import { Component, inject, ChangeDetectorRef, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';
import { ProfilePanel } from '../../features/profile-panel/profile-panel';
import { UserResponse } from '../../features/auth/model/auth';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ProfilePanel],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout implements OnInit {
  readonly authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private userService = inject(UserService);

  isNavMenuOpen = false;
  private openMenuSection: string | null = null;
  readonly headerProfile = signal<UserResponse | null>(null);
  readonly headerAvatarFailed = signal(false);

  get currentUser() {
    return this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.authService.restoreSession().subscribe(loggedIn => {
      if (loggedIn) this.loadHeaderAvatar();
    });
  }

  loadHeaderAvatar(): void {
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.headerProfile.set(profile);
        this.headerAvatarFailed.set(false);
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  refreshHeaderAvatar(): void {
    this.headerAvatarFailed.set(false);
    this.loadHeaderAvatar();
  }

  onAvatarUpdated(profile: UserResponse): void {
    this.headerProfile.set(profile);
    this.headerAvatarFailed.set(false);
  }

  get headerAvatarUrl(): string | null {
    if (this.headerAvatarFailed()) return null;
    return this.userService.resolveAvatarUrl(this.headerProfile()?.avatarUrl);
  }

  get headerInitials(): string {
    const p = this.headerProfile();
    if (!p) {
      const u = this.currentUser?.username ?? '';
      return u.charAt(0).toUpperCase();
    }
    const name = p.fullName?.trim() ||
      [p.firstName, p.lastName].filter(Boolean).join(' ') ||
      p.username;
    return name.split(/\s+/).filter(Boolean)
      .map(part => part.charAt(0))
      .filter((_, i, arr) => i === 0 || i === arr.length - 1)
      .join('').toUpperCase();
  }

  toggleNavMenu(): void {
    this.isNavMenuOpen = !this.isNavMenuOpen;
    if (this.isNavMenuOpen && !this.openMenuSection) {
      this.openMenuSection = this.getMenuSectionForUrl(this.router.url);
    }
    this.cdr.detectChanges();
  }

  closeNavMenu(): void {
    this.isNavMenuOpen = false;
    this.cdr.detectChanges();
  }

  toggleMenuSection(section: string): void {
    this.openMenuSection = this.openMenuSection === section ? null : section;
    this.cdr.detectChanges();
  }

  isMenuSectionOpen(section: string): boolean {
    return this.openMenuSection === section;
  }

  private getMenuSectionForUrl(url: string): string | null {
    const path = url.split(/[?#]/, 1)[0];
    if (path === '/dashboard') return 'overview';
    if (path.startsWith('/procurement') || path === '/purchase-orders') return 'procurement';
    if (path === '/suppliers/contracts') return 'documents';
    if (path.startsWith('/supplier') || path.startsWith('/suppliers')) return 'suppliers';
    if (path === '/company-documents') return 'documents';
    if (path.startsWith('/user') || path === '/users') return 'admin';
    return null;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
