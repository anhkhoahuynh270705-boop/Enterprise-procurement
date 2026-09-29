import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../auth/services/auth.service';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ProcurementTicket } from '../model/procurement';
import { ProcurementService } from '../service/procurement.service';

@Component({
  selector: 'app-procurement-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './procurement-detail.html',
  styleUrls: ['./procurement-detail.scss'],
})
export class ProcurementDetail implements OnInit {
  private readonly toast = inject(ToastService);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  readonly auth = inject(AuthService);
  private procurementService = inject(ProcurementService);
  private cdr = inject(ChangeDetectorRef);
  private fb = inject(FormBuilder);

  ticketId: string | null = null;
  ticket: ProcurementTicket | null = null;
  isLoading = true;

  isReviewModalOpen = false;
  isReviewing = false;

  visibleFields = new Set<string>();
  visibleItemCodes = new Set<number>();

  toggleFieldVisibility(fieldKey: string): void {
    if (this.visibleFields.has(fieldKey)) {
      this.visibleFields.delete(fieldKey);
    } else {
      this.visibleFields.add(fieldKey);
    }
    this.cdr.markForCheck();
  }

  isFieldVisible(fieldKey: string): boolean {
    return this.visibleFields.has(fieldKey);
  }

  toggleItemCodeVisibility(idx: number): void {
    if (this.visibleItemCodes.has(idx)) {
      this.visibleItemCodes.delete(idx);
    } else {
      this.visibleItemCodes.add(idx);
    }
    this.cdr.markForCheck();
  }

  isItemCodeVisible(idx: number): boolean {
    return this.visibleItemCodes.has(idx);
  }

  toggleAllItemCodes(): void {
    if (this.ticket?.items && this.visibleItemCodes.size === this.ticket.items.length) {
      this.visibleItemCodes.clear();
    } else if (this.ticket?.items) {
      this.ticket.items.forEach((_, idx) => this.visibleItemCodes.add(idx));
    }
    this.cdr.markForCheck();
  }

  reviewForm = this.fb.nonNullable.group({
    approved: [true, Validators.required],
    comment: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    this.ticketId = this.route.snapshot.paramMap.get('id');
    if (this.ticketId) {
      this.loadTicket(this.ticketId);
    } else {
      this.toast.error('Mã phiếu không hợp lệ');
      this.isLoading = false;
    }
  }

  loadTicket(id: string): void {
    this.isLoading = true;
    this.visibleFields.clear();
    this.visibleItemCodes.clear();

    this.procurementService.getTicketById(id).subscribe({
      next: (res) => {
        this.ticket = res.data;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Không thể tải chi tiết phiếu mua sắm');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  submitTicket(): void {
    if (!this.ticket) {
      return;
    }
    const confirmed = confirm(
      `Bạn có chắc chắn muốn nộp phiếu •••••••• lên cấp quản lý phê duyệt?`,
    );
    if (!confirmed) {
      return;
    }
    this.isLoading = true;
    this.procurementService.submitTicket(this.ticket.id).subscribe({
      next: (res) => {
        this.ticket = res.data;
        this.toast.success(`Đã nộp duyệt phiếu •••••••• thành công!`);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Lỗi khi nộp phiếu');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openReviewModal(): void {
    this.reviewForm.reset({
      approved: true,
      comment: '',
    });
    this.isReviewModalOpen = true;
  }
  closeReviewModal(): void {
    this.isReviewModalOpen = false;
    this.reviewForm.reset({
      approved: true,
      comment: '',
    });
  }

  submitReview(): void {
    if (!this.ticket) {
      return;
    }
    const taskId = this.ticket.camundaTaskId;
    if (!taskId) {
      this.toast.error('Không tìm thấy Task ID cho phiếu này.');
      return;
    }
    if (this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    const { approved, comment } = this.reviewForm.getRawValue();
    this.isReviewing = true;
    this.procurementService
      .reviewTask(taskId, { approved, comment })
      .pipe(
        finalize(() => {
          this.isReviewing = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.closeReviewModal();
          this.toast.success(approved ? 'Đã duyệt phiếu thành công!' : 'Đã từ chối phiếu mua sắm.');
          if (this.auth.hasRole('CHECKER')) {
            void this.router.navigate(['/procurement']);
            return;
          }
          if (this.ticketId) {
            this.loadTicket(this.ticketId);
          }
        },
        error: (err) => {
          this.toast.error(err.error?.message || 'Lỗi khi gửi phê duyệt');
        },
      });
  }

  exportPdf(): void {
    if (!this.ticket) {
      return;
    }
    this.procurementService.exportTicketDetail(this.ticket.id, 'PDF').subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `Enterprise_Detail_${this.ticket!.ticketCode}.pdf`);
      },
      error: () => {
        this.toast.error('Lỗi khi xuất phiếu Enterprise Procurement Detail PDF');
      },
    });
  }

  exportExcel(): void {
    if (!this.ticket) {
      return;
    }
    this.procurementService.exportTicketDetail(this.ticket.id, 'EXCEL').subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `Enterprise_Detail_${this.ticket!.ticketCode}.xlsx`);
      },
      error: () => {
        this.toast.error('Lỗi khi xuất bảng tính Excel');
      },
    });
  }

  canModifyTicket(ticket: ProcurementTicket): boolean {
    if (this.auth.hasRole('ADMIN') && ticket.status === 'APPROVED') return true;
    return (
      (ticket.status === 'DRAFT' || ticket.status === 'REJECTED') &&
      (this.auth.hasRole('ADMIN') ||
        (this.auth.hasRole('USER') &&
          this.auth.getCurrentUser()?.username === ticket.makerUsername))
    );
  }
  deleteTicket(): void {
    if (!this.ticket || !this.canModifyTicket(this.ticket)) {
      return;
    }
    const confirmed = confirm(`Xóa vĩnh viễn phiếu ?`);
    if (!confirmed) {
      return;
    }
    this.procurementService.deleteTicket(this.ticket.id).subscribe({
      next: () => {
        this.router.navigate(['/procurement']);
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Không thể xóa phiếu');
        this.cdr.markForCheck();
      },
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'APPROVED':
        return 'badge-approved';
      case 'REJECTED':
        return 'badge-rejected';
      case 'PENDING_APPROVAL':
        return 'badge-pending';
      default:
        return 'badge-draft';
    }
  }

  getStatusText(status?: string): string {
    switch (status) {
      case 'APPROVED':
        return 'APPROVED';
      case 'REJECTED':
        return 'REJECTED';
      case 'PENDING_APPROVAL':
        return 'PENDING_APPROVAL';
      default:
        return 'DRAFT';
    }
  }

  getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'URGENT':
        return 'badge-urgent';
      case 'HIGH':
        return 'badge-high';
      case 'LOW':
        return 'badge-low';
      default:
        return 'badge-medium';
    }
  }
}
