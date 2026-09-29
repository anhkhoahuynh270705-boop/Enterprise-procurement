import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import {
  Component,
  HostListener,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Supplier } from '../models/supplier';

@Component({
  imports: [SensitiveValue, CommonModule],
  selector: 'app-supplier-view',
  styleUrl: './supplier-view.scss',
  templateUrl: './supplier-view.html',
})
export class SupplierView {
  selectedSupplier: Supplier | null = null;
  isOpen: boolean = false;
  popupStyle: { [key: string]: string } = {};
  arrowPosition: 'top' | 'bottom' = 'top';
  arrowStyle: { [key: string]: string } = {};

  private readonly cdr = inject(ChangeDetectorRef);
  @ViewChild('popupCard') private popupCard?: ElementRef<HTMLElement>;

  open(supplier: Supplier, event: MouseEvent): void {
    if (this.isOpen && this.selectedSupplier?.id === supplier.id) {
      this.close();
      return;
    }
    this.selectedSupplier = { ...supplier };
    const trigger = (event.currentTarget || event.target) as HTMLElement;
    // Render at the final width before measuring; wrapped text changes the height.
    this.popupStyle = {
      position: 'fixed',
      top: '12px',
      right: '12px',
      width: `${Math.min(560, window.innerWidth - 24)}px`,
      visibility: 'hidden',
    };
    this.isOpen = true;
    this.cdr.detectChanges();
    this.calculatePopupPosition(trigger.getBoundingClientRect());
    this.cdr.detectChanges();
  }

  close(): void {
    this.isOpen = false;
    this.selectedSupplier = null;
    this.cdr.markForCheck();
  }

  private calculatePopupPosition(rect: DOMRect): void {
    const popupWidth = Math.min(560, window.innerWidth - 24);
    const gap = 12;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    // offsetHeight is unaffected by the opening scale animation.
    const popupHeight = this.popupCard!.nativeElement.offsetHeight;
    const spaceBelow = Math.max(0, viewportHeight - rect.bottom - gap - 12);
    const spaceAbove = Math.max(0, rect.top - gap - 12);
    const openAbove = popupHeight > spaceBelow && spaceAbove > spaceBelow;
    const availableHeight = openAbove ? spaceAbove : spaceBelow;
    const renderedHeight = Math.min(popupHeight, availableHeight);
    const top = openAbove ? rect.top - gap - renderedHeight : rect.bottom + gap;
    const right = Math.max(
      12,
      Math.min(viewportWidth - rect.right, viewportWidth - popupWidth - 12),
    );

    this.popupStyle = {
      position: 'fixed',
      top: `${Math.max(12, top)}px`,
      right: `${right}px`,
      width: `${popupWidth}px`,
      'max-height': `${availableHeight}px`,
      'z-index': '1050',
    };
    this.arrowPosition = openAbove ? 'bottom' : 'top';
    const arrowRight = Math.max(
      16,
      Math.min(viewportWidth - rect.right + rect.width / 2 - right, popupWidth - 24),
    );
    this.arrowStyle = { right: `${arrowRight}px` };
  }

  @HostListener('window:resize')
  onWindowChange(): void {
    if (this.isOpen) {
      this.close();
    }
  }
}
