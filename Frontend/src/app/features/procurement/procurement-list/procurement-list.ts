import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../auth/services/auth.service';
import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CamundaTask, ImportResult, ProcurementTicket } from '../model/procurement';
import { ProcurementService } from '../service/procurement.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-procurement-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './procurement-list.html',
  styleUrls: ['./procurement-list.scss'],
})
export class ProcurementList implements OnInit {
  private readonly toast = inject(ToastService);

  readonly auth = inject(AuthService);
  private procurementService = inject(ProcurementService);
  private readonly route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private readonly fb = inject(FormBuilder);

  private readonly destroyRef = inject(DestroyRef);
  isApprovalView = false;
  tickets: ProcurementTicket[] = [];
  checkerTasks: CamundaTask[] = [];
  filteredTickets: ProcurementTicket[] = [];
  visibleCodes = new Set<string>();

  toggleCodeVisibility(id: string, event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    if (this.visibleCodes.has(id)) {
      this.visibleCodes.delete(id);
    } else {
      this.visibleCodes.add(id);
    }
  }

  isCodeVisible(id: string): boolean {
    return this.visibleCodes.has(id);
  }

  toggleAllCodes(): void {
    const ids = this.isApprovalView
      ? this.checkerTasks.flatMap((task) =>
          [task.ticket?.id, task.taskId].filter((id): id is string => Boolean(id)),
        )
      : this.filteredTickets.map((ticket) => ticket.id);
    if (ids.length > 0 && ids.every((id) => this.visibleCodes.has(id))) {
      this.visibleCodes.clear();
    } else {
      ids.forEach((id) => this.visibleCodes.add(id));
    }
  }

  readonly filtersForm = this.fb.nonNullable.group({
    searchQuery: '',
    selectedStatus: '',
  });
  currentPage = 0;
  readonly pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  isLoading = false;

  isReviewModalOpen = false;
  selectedTask: CamundaTask | null = null;
  readonly reviewForm = this.fb.nonNullable.group({
    approved: true,
    comment: '',
  });
  reviewActionType: 'APPROVE' | 'REJECT' | null = null;
  reviewError = '';
  isReviewing = false;
  isImportModalOpen = false;
  importFile: File | null = null;
  isImporting = false;
  importResult: ImportResult | null = null;

  ngOnInit(): void {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      this.isApprovalView = data['view'] === 'approvals';
      this.loadData();
    });
  }

  loadData(): void {
    this.isLoading = true;
    if (this.isApprovalView) {
      this.procurementService.getPendingCheckerTasks().subscribe({
        next: (res) => {
          this.checkerTasks = res.data || [];
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.toast.error(err.error?.message || 'Không thể tải danh sách nhiệm vụ chờ duyệt');
          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
      return;
    }

    this.procurementService
      .getTicketPage(
        this.filtersForm.controls.selectedStatus.value || undefined,
        this.currentPage,
        this.pageSize,
      )
      .subscribe({
        next: (res) => {
          this.tickets = res.data?.content || [];
          this.totalPages = res.data?.totalPages || 0;
          this.totalElements = res.data?.totalElements || 0;

          this.applyFilter();

          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.toast.error(err.error?.message || 'Không thể tải danh sách phiếu mua sắm');

          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  onStatusChange(): void {
    this.currentPage = 0;
    this.loadData();
    this.cdr.detectChanges();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.currentPage) return;
    this.currentPage = page;
    this.loadData();
    this.cdr.detectChanges();
  }

  applyFilter(): void {
    const q = this.filtersForm.controls.searchQuery.value.trim().toLowerCase();
    if (!q) {
      this.filteredTickets = this.tickets;
      return;
    }
    this.filteredTickets = this.tickets.filter(
      (t) =>
        t.ticketCode.toLowerCase().includes(q) ||
        t.title.toLowerCase().includes(q) ||
        (t.department && t.department.toLowerCase().includes(q)) ||
        t.makerUsername.toLowerCase().includes(q),
    );
    this.cdr.detectChanges();
  }

  submitTicket(ticket: ProcurementTicket): void {
    if (!confirm(`Bạn có chắc chắn muốn nộp phiếu lên quản lý phê duyệt?`)) {
      return;
    }
    this.isLoading = true;
    this.procurementService.submitTicket(ticket.id).subscribe({
      next: () => {
        this.toast.success(`Đã nộp duyệt phiếu thành công!`);
        this.loadData();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Nộp duyệt thất bại');
        this.isLoading = false;
      },
    });
    this.cdr.detectChanges();
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

  canDeleteTicket(ticket: ProcurementTicket): boolean {
    if (ticket.status === 'APPROVED' || ticket.status === 'PENDING_APPROVAL') {
      return false;
    }
    return (
      (ticket.status === 'DRAFT' || ticket.status === 'REJECTED') &&
      (this.auth.hasRole('ADMIN') ||
        (this.auth.hasRole('USER') &&
          this.auth.getCurrentUser()?.username === ticket.makerUsername))
    );
  }

  deleteTicket(ticket: ProcurementTicket): void {
    if (ticket.status === 'APPROVED') {
      this.toast.warning('Phiếu đã được duyệt không thể xóa.');
      return;
    }
    if (!this.canDeleteTicket(ticket)) {
      this.toast.warning(
        `Chỉ có thể xóa phiếu Nháp hoặc bị từ chối mà bạn có quyền quản lý. Phiếu đang có trạng thái: ${ticket.status}`,
      );
      return;
    }
    if (!confirm(`Xóa phiếu ? Thao tác này không thể hoàn tác.`)) {
      return;
    }
    this.procurementService.deleteTicket(ticket.id).subscribe({
      next: () => {
        this.toast.success(`Đã xóa phiếu`);
        this.loadData();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Xóa phiếu thất bại');
      },
    });
    this.cdr.detectChanges();
  }

  exportTicketPdf(ticket: ProcurementTicket): void {
    this.procurementService.exportTicket(ticket.id, 'PDF').subscribe({
      next: (blob) => this.downloadBlob(blob, `Voucher_${ticket.ticketCode}.pdf`),
      error: () => this.toast.error('Lỗi khi xuất file PDF bằng JasperReports'),
    });
  }

  exportTicketExcel(ticket: ProcurementTicket): void {
    this.procurementService.exportTicket(ticket.id, 'EXCEL').subscribe({
      next: (blob) => this.downloadBlob(blob, `Procurement_${ticket.ticketCode}.xlsx`),
      error: () => this.toast.error('Lỗi khi xuất file Excel'),
    });
  }

  exportAllExcel(): void {
    this.procurementService.exportAllTickets().subscribe({
      next: (blob) => this.downloadBlob(blob, 'Danh_sach_phieu_mua_sam.xlsx'),
      error: () => this.toast.error('Lỗi khi xuất danh sách phiếu ra Excel'),
    });
  }

  toggleReviewTask(task: CamundaTask): void {
    if (this.selectedTask?.taskId === task.taskId && this.isReviewModalOpen) {
      this.closeReviewModal();
    } else {
      this.openReviewModal(task);
    }
  }

  openReviewModal(task: CamundaTask): void {
    this.selectedTask = task;
    this.reviewError = '';
    this.reviewForm.reset({ approved: true, comment: '' });
    this.isReviewModalOpen = true;
    this.cdr.detectChanges();

    setTimeout(() => {
      const panelEl = document.getElementById('review-panel');
      if (panelEl) {
        panelEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    }, 60);
  }

  quickApprove(task: CamundaTask): void {
    if (this.isReviewing) return;
    this.selectedTask = task;
    this.reviewForm.reset({ approved: true, comment: 'Đã duyệt qua danh sách hàng chờ' });
    this.submitReview(true);
  }

  openReject(task: CamundaTask): void {
    if (this.isReviewing) return;
    this.selectedTask = task;
    this.reviewError = '';
    this.reviewForm.reset({ approved: false, comment: '' });
    this.isReviewModalOpen = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      const panelEl = document.getElementById('review-panel');
      if (panelEl) {
        panelEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
      document.querySelector<HTMLElement>('#review-panel textarea')?.focus();
    }, 80);
  }

  closeReviewModal(): void {
    this.isReviewModalOpen = false;
    this.selectedTask = null;
    this.reviewError = '';
    this.reviewActionType = null;
    this.cdr.detectChanges();
  }

  submitReview(approved: boolean): void {
    if (!this.selectedTask || this.isReviewing) return;

    const comment = this.reviewForm.controls.comment.value.trim();
    if (!approved && !comment) {
      this.reviewError = 'Vui lòng nhập lý do từ chối.';
      this.toast.error('Vui lòng nhập lý do từ chối.');
      return;
    }
    if (comment.length > 2000) {
      this.reviewError = 'Nhận xét tối đa 2000 ký tự.';
      return;
    }

    this.isReviewing = true;
    this.reviewActionType = approved ? 'APPROVE' : 'REJECT';
    this.reviewError = '';

    const submittedTaskId = this.selectedTask.taskId;

    this.procurementService
      .reviewTask(submittedTaskId, {
        approved,
        comment,
      })
      .subscribe({
        next: () => {
          this.isReviewing = false;
          this.reviewActionType = null;
          this.closeReviewModal();
          this.checkerTasks = this.checkerTasks.filter((t) => t.taskId !== submittedTaskId);
          this.toast.success(
            approved ? 'Đã phê duyệt phiếu thành công!' : 'Đã từ chối phiếu mua sắm!',
          );
          this.cdr.detectChanges();
          this.loadData();
        },
        error: (err) => {
          this.isReviewing = false;
          this.reviewActionType = null;
          this.cdr.detectChanges();
          this.toast.error(err.error?.message || 'Lỗi khi hoàn tất phê duyệt');
        },
      });
  }

  openImportModal(): void {
    this.importFile = null;
    this.importResult = null;
    this.isImportModalOpen = true;
  }

  closeImportModal(): void {
    this.isImportModalOpen = false;
    this.importFile = null;
    this.importResult = null;
  }

  onFileSelected(event: any): void {
    this.importFile = null;
    this.importResult = null;
    const file = event.target.files?.[0];
    if (file) {
      if (!/\.(xlsx|xls)$/i.test(file.name) || file.size === 0) {
        this.toast.error(
          'File import không hợp lệ. Vui lòng chọn file Excel .xlsx hoặc .xls không rỗng.',
        );
        event.target.value = '';
        return;
      }
      this.importFile = file;
    }
  }

  // downloadTemplate(): void {
  //   this.procurementService.downloadTemplate().subscribe({
  //     next: blob => this.downloadBlob(blob, 'Mau_nhap_phieu_mua_sam.xlsx'),
  //     error: err => alert('Lỗi khi tải file mẫu: ' + (err.error?.message || err.message))
  //   });
  // }

  /* Import Excel file and create procurement ticket */
  doImport(): void {
    if (!this.importFile || this.isImporting) return;
    this.isImporting = true;
    this.procurementService.importExcel(this.importFile).subscribe({
      next: (res) => {
        this.isImporting = false;
        this.importResult = res.data;
        if (this.importResult?.createdTicket) {
          this.toast.success('Đã nhập phiếu và gửi duyệt thành công.');
          this.loadData();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isImporting = false;
        this.toast.error(err.error?.message || 'Lỗi khi xử lý file import');
        this.cdr.detectChanges();
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
    this.cdr.detectChanges();
  }

  getStatusClass(status: string): string {
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

  getStatusText(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'APPROVED';
      case 'REJECTED':
        return 'REJECTED';
      case 'PENDING_APPROVAL':
        return 'PENDING';
      default:
        return 'DRAFT';
    }
  }

  getPriorityClass(priority: string): string {
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
