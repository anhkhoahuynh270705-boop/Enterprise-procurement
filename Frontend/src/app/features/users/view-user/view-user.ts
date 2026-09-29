import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import { Component, HostListener, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Users } from '../models/user';
import { profileTextFields } from '../profile-fields/profile-form';

@Component({
  imports: [SensitiveValue, CommonModule],
  selector: 'app-view-user',
  styleUrl: './view-user.scss',
  templateUrl: './view-user.html',
})
export class ViewUser {
  readonly profileFields = [
    ...profileTextFields,
    { name: 'dateOfBirth', label: 'Date of birth' },
    { name: 'gender', label: 'Gender' },
    { name: 'address', label: 'Address' },
    { name: 'employeeStatus', label: 'Employee status' },
  ] as const;
  selectedUser: Users | null = null;
  isOpen: boolean = false;
  popupStyle: { [key: string]: string } = {};
  arrowPosition: 'right' | 'top' | 'bottom' = 'right';
  arrowStyle: { [key: string]: string } = {};
  isIdVisible: boolean = false;

  private readonly cdr = inject(ChangeDetectorRef);

  toggleIdVisibility(): void {
    this.isIdVisible = !this.isIdVisible;
    this.cdr.markForCheck();
  }

  open(user: Users, event: MouseEvent): void {
    if (this.isOpen && this.selectedUser?.id === user.id) {
      this.close();
      return;
    }
    this.selectedUser = { ...user };
    this.isIdVisible = false;
    const trigger = (event.currentTarget || event.target) as HTMLElement;
    this.calculatePopupPosition(trigger.getBoundingClientRect());
    this.isOpen = true;
    this.cdr.markForCheck();
  }

  close(): void {
    this.isOpen = false;
    this.selectedUser = null;
    this.cdr.markForCheck();
  }

  private calculatePopupPosition(rect: DOMRect): void {
    const popupWidth = Math.min(540, window.innerWidth - 24);
    const gap = 12;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const estimatedHeight = Math.min(640, viewportHeight - 24);

    if (rect.left >= popupWidth + gap + 10) {
      const right = viewportWidth - rect.left + gap;
      let top = rect.top + rect.height / 2 - 60;
      top = Math.max(12, Math.min(top, viewportHeight - estimatedHeight - 12));

      this.popupStyle = {
        position: 'fixed',
        top: `${top}px`,
        right: `${right}px`,
        width: `${popupWidth}px`,
        'z-index': '1050',
      };
      this.arrowPosition = 'right';
      const arrowTop = Math.max(
        16,
        Math.min(rect.top + rect.height / 2 - top, estimatedHeight - 24),
      );
      this.arrowStyle = { top: `${arrowTop}px` };
    } else {
      let top = rect.bottom + gap;
      if (top + estimatedHeight > viewportHeight && rect.top > estimatedHeight) {
        top = rect.top - estimatedHeight - gap;
        this.arrowPosition = 'bottom';
      } else {
        this.arrowPosition = 'top';
      }
      top = Math.max(12, Math.min(top, viewportHeight - estimatedHeight - 12));
      const right = Math.max(
        12,
        Math.min(viewportWidth - rect.right, viewportWidth - popupWidth - 12),
      );
      this.popupStyle = {
        position: 'fixed',
        top: `${top}px`,
        right: `${right}px`,
        width: `${popupWidth}px`,
        'z-index': '1050',
      };
      const arrowRight = Math.max(
        16,
        Math.min(viewportWidth - rect.right + rect.width / 2 - right, popupWidth - 24),
      );
      this.arrowStyle = { right: `${arrowRight}px` };
    }
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onWindowChange(): void {
    if (this.isOpen) {
      this.close();
    }
  }
}
