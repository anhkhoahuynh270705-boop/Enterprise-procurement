import { ToastService } from '../../../core/services/toast.service';
import { Component, inject, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Users } from '../models/user';
import { UserService } from '../services/user.service';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../auth/services/auth.service';
import { ViewUser } from '../view-user/view-user';

@Component({
  imports: [CommonModule, ReactiveFormsModule, ViewUser],
  selector: 'app-user-list',
  styleUrl: './user-list.scss',
  templateUrl: './user-list.html',
})
export class UserList implements OnInit {
  private readonly toast = inject(ToastService);

  users: Users[] = [];
  filteredUsers: Users[] = [];
  readonly roles = ['ADMIN', 'USER', 'CHECKER'] as const;
  selectedRole: 'ADMIN' | 'USER' | 'CHECKER' = 'ADMIN';

  selectRole(role: 'ADMIN' | 'USER' | 'CHECKER'): void {
    this.selectedRole = role;
    this.viewUserRef?.close();
    this.searchUser();
  }

  roleCount(role: string): number {
    return this.users.filter((user) => user.role === role).length;
  }

  get allVisibleIdsShown(): boolean {
    return (
      this.filteredUsers.length > 0 &&
      this.filteredUsers.every((user) => this.visibleIds.has(user.id))
    );
  }
  private readonly fb = inject(FormBuilder);
  readonly searchForm = this.fb.nonNullable.group({
    searchKeyWord: '',
  });
  visibleIds = new Set<string | number>();

  toggleIdVisibility(id: string | number, event?: Event): void {
    if (event) event.stopPropagation();
    if (this.visibleIds.has(id)) {
      this.visibleIds.delete(id);
    } else {
      this.visibleIds.add(id);
    }
    this.cdr.detectChanges();
  }

  isIdVisible(id: string | number): boolean {
    return this.visibleIds.has(id);
  }

  toggleAllIds(): void {
    if (this.allVisibleIdsShown) {
      this.filteredUsers.forEach((user) => this.visibleIds.delete(user.id));
    } else {
      this.filteredUsers.forEach((u) => this.visibleIds.add(u.id));
    }
  }

  @ViewChild(ViewUser)
  viewUserRef!: ViewUser;

  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  isSelf(user: Users): boolean {
    const current = this.authService.getCurrentUser();
    if (!current || !user) return false;
    const matchUsername =
      !!current.username &&
      !!user.username &&
      current.username.trim().toLowerCase() === user.username.trim().toLowerCase();
    const matchEmail =
      !!current.email &&
      !!user.email &&
      current.email.trim().toLowerCase() === user.email.trim().toLowerCase();
    return matchUsername || matchEmail;
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.searchUser();
        this.cdr.detectChanges();
      },
    });
  }

  addUser(): void {
    this.router.navigate(['user/create']);
  }

  searchUser(): void {
    const keyword = this.searchForm.controls.searchKeyWord.value.trim().toLowerCase();
    this.filteredUsers = this.users.filter(
      (user) =>
        user.role === this.selectedRole &&
        (!keyword ||
          [user.fullName, user.username, user.email, user.role].some((value) =>
            value?.toLowerCase().includes(keyword),
          )),
    );
  }

  editUser(id: string | number): void {
    this.router.navigate(['user/edit', id]);
  }

  openViewUser(user: Users, event: MouseEvent): void {
    this.viewUserRef.open(user, event);
  }

  deletingId: string | number | null = null;
  togglingId: string | number | null = null;

  deleteUser(id: string | number): void {
    if (this.deletingId) return;
    const target = this.users.find((u) => u.id === id);
    if (target && this.isSelf(target)) {
      this.toast.warning('Bạn không thể xóa chính mình!');
      return;
    }
    if (!confirm('Bạn có chắc chắn muốn xóa user này?')) return;

    this.deletingId = id;
    this.userService.deleteUser(id).subscribe({
      next: () => {
        this.users = this.users.filter((u) => u.id !== id);
        this.filteredUsers = this.filteredUsers.filter((u) => u.id !== id);
        this.deletingId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi khi xóa user:', err);
        this.deletingId = null;
        this.loadUsers();
      },
    });
  }

  toggleUser(id: string | number): void {
    if (this.togglingId) return;
    const target = this.users.find((u) => u.id === id);

    if (target && this.isSelf(target)) {
      return;
    }
    if (target?.enabled) {
      const msg = `Tài khoản "${target?.username}" sẽ bị vô hiệu hóa và đăng xuất khỏi hệ thống. Tiếp tục?`;
      if (!confirm(msg)) return;
    }
    this.togglingId = id;

    this.userService.toggleUserStatus(id).subscribe({
      next: (updated) => {
        const updateList = (list: Users[]) =>
          list.map((u) => (u.id === id ? { ...u, enabled: updated.enabled } : u));
        this.users = updateList(this.users);
        this.filteredUsers = updateList(this.filteredUsers);
        this.togglingId = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Lỗi khi toggle trạng thái user:', err);
        this.togglingId = null;
        const msg = err.error?.message || 'Có lỗi xảy ra khi cập nhật trạng thái người dùng.';
        this.toast.error(msg);
      },
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
