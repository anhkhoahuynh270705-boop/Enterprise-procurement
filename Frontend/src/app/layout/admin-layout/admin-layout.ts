import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';
import { ProfilePanel } from '../../features/profile-panel/profile-panel';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ProfilePanel],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  readonly authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  isNavMenuOpen = false;
  isSupplierOpen = false;

  get currentUser() {
    return this.authService.getCurrentUser();
  }

  toggleNavMenu(): void {
    this.isNavMenuOpen = !this.isNavMenuOpen;
    this.cdr.detectChanges();
  }

  closeNavMenu(): void {
    this.isNavMenuOpen = false;
    this.cdr.detectChanges();
  }

  toggleSupplierMenu(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isSupplierOpen = !this.isSupplierOpen;
    this.cdr.detectChanges();
  }

  onSupplierClick(event: MouseEvent): void {
    this.isSupplierOpen = !this.isSupplierOpen;
    this.cdr.detectChanges();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
