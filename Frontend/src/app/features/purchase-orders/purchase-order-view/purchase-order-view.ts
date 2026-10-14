import { CommonModule } from '@angular/common';
import {
  Component,
  ChangeDetectorRef,
  EventEmitter,
  HostListener,
  Output,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../auth/services/auth.service';
import { PurchaseOrder, PurchaseOrderStatus } from '../models/purchase-order';
import { PurchaseOrderService } from '../services/purchase-order.service';

@Component({
  selector: 'app-purchase-order-view',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './purchase-order-view.html',
  styleUrl: './purchase-order-view.scss',
})
export class PurchaseOrderView {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly toast = inject(ToastService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  readonly auth = inject(AuthService);

  @Output() 
  reviewed = new EventEmitter<PurchaseOrder>();

  order: PurchaseOrder | null = null;
  isOpen = false;
  isSubmitting = false;
  reviewError = '';

  readonly reviewForm = this.fb.nonNullable.group({
    comment: ['', Validators.maxLength(2000)],
  });

  open(order: PurchaseOrder): void {
    this.order = order;
    this.isOpen = true;
    this.reviewError = '';
    this.reviewForm.reset({ comment: '' });
    this.cdr.detectChanges();
    setTimeout(() => {
      document.querySelector('.panel.details, .po-view-panel')?.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
      });
    });
  }

  close(): void {
    if (this.isSubmitting) 
      return;
    this.isOpen = false;
    this.order = null;
    this.reviewError = '';
    this.reviewForm.reset({ comment: '' });
    this.cdr.detectChanges();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen && !this.isSubmitting) {
      this.close();
    }
  }

  submitReview(approved: boolean): void {
    const current = this.order;
    if (!current || !this.auth.hasRole('ADMIN', 'CHECKER') || this.isSubmitting) return;

    const comment = this.reviewForm.controls.comment.value.trim();
    if (!approved && !comment) {
      this.reviewError = 'Vui lòng nhập lý do từ chối.';
      this.toast.warning(this.reviewError);
      return;
    }
    if (comment.length > 2000) {
      this.reviewError = 'Nhận xét tối đa 2000 ký tự.';
      return;
    }

    this.isSubmitting = true;
    this.reviewError = '';

    this.purchaseOrderService.review(current.id, { approved, comment }).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.toast.success(
          approved
            ? `Đã duyệt đơn ${current.orderCode}; đơn đã chuyển sang trạng thái gửi nhà cung cấp.`
            : `Đã từ chối đơn ${current.orderCode}.`,
        );
        this.reviewed.emit(response.data);
        this.close();
      },
      error: (error) => {
        this.isSubmitting = false;
        this.reviewError = error.error?.message || 'Không thể xử lý duyệt đơn đặt hàng.';
        this.toast.error(this.reviewError);
        this.cdr.detectChanges();
      },
    });
  }

  statusLabel(status: PurchaseOrderStatus): string {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'Chờ duyệt';
      case 'DRAFT':
        return 'Chờ duyệt';
      case 'SENT':
        return 'Đã duyệt (Gửi NCC)';
      case 'RECEIVED':
        return 'Đã nhận hàng';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'REJECTED':
        return 'Đã từ chối';
    }
  }

  statusClass(status: PurchaseOrderStatus): string {
    return `status-${status.toLowerCase()}`;
  }
}
