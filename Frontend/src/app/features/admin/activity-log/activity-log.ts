import { Component, OnInit, inject, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ToastService } from '../../../core/services/toast.service';
import { AuditLogService } from '../services/audit-log.service';
import { AuditLogItem, PagedResponse } from '../model/audit-log.model';

@Component({
  selector: 'app-activity-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './activity-log.html',
  styleUrl: './activity-log.scss',
})
export class ActivityLog implements OnInit, OnDestroy {
  private readonly toast = inject(ToastService);
  private readonly svc = inject(AuditLogService);
  private readonly destroy$ = new Subject<void>();
  private readonly cdr = inject(ChangeDetectorRef);

  activeCategory: string = 'ALL';
  searchKeyword: string = '';
  levelFilter: string = 'ALL';
  dateFilter: string = 'TODAY';
  isLoading: boolean = false;

  selectedLog: AuditLogItem | null = null;
  isDetailModalOpen = false;

  currentPage = 0;
  pageSize = 20;

  visibleIds = new Set<string>();

  get allVisibleIdsShown(): boolean {
    return this.logs.length > 0 && this.logs.every((log) => this.visibleIds.has(log.id));
  }

  toggleIdVisibility(id: string, event?: Event): void {
    if (event) event.stopPropagation();
    if (this.visibleIds.has(id)) {
      this.visibleIds.delete(id);
    } else {
      this.visibleIds.add(id);
    }
    this.cdr.detectChanges();
  }

  toggleAllIds(): void {
    if (this.allVisibleIdsShown) {
      this.visibleIds.clear();
    } else {
      this.logs.forEach((log) => this.visibleIds.add(log.id));
    }
    this.cdr.detectChanges();
  }

  isIdVisible(id: string): boolean {
    return this.visibleIds.has(id);
  }

  pagedData: PagedResponse<AuditLogItem> | null = null;
  get logs(): AuditLogItem[] {
    return this.pagedData?.content ?? [];
  }
  get totalElements(): number {
    return this.pagedData?.totalElements ?? 0;
  }
  get totalPages(): number {
    return Math.max(1, this.pagedData?.totalPages ?? 1);
  }

  get statTotal(): number {
    return this.totalElements;
  }
  get statAuth(): number {
    return this.logs.filter((l) => l.module === 'AUTH').length;
  }
  get statProcurement(): number {
    return this.logs.filter((l) => l.module === 'PROCUREMENT' || l.module === 'CONTRACT').length;
  }
  get statAlerts(): number {
    return this.logs.filter((l) => l.status === 'WARNING' || l.status === 'FAILED').length;
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.isLoading = true;
    this.svc
      .search({
        module: this.activeCategory,
        status: this.levelFilter,
        dateRange: this.dateFilter,
        keyword: this.searchKeyword,
        page: this.currentPage,
        size: this.pageSize,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.pagedData = data;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoading = false;
          this.toast.error('Không thể tải nhật ký hoạt động. Vui lòng thử lại.');
        },
      });
  }

  refreshLogs(): void {
    this.currentPage = 0;
    this.load();
    this.cdr.detectChanges();
    this.toast.success('Đã làm mới nhật ký hoạt động.');
  }

  setCategory(cat: string): void {
    this.activeCategory = cat;
    this.currentPage = 0;
    this.load();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  onSearch(): void {
    this.currentPage = 0;
    this.load();
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  popoverPosition: Record<string, string> = {};
  isFlippedBelow = false;

  openDetails(log: AuditLogItem, event?: MouseEvent): void {
    if (this.selectedLog?.id === log.id && this.isDetailModalOpen) {
      this.closeDetails();
      return;
    }
    this.selectedLog = log;
    this.isDetailModalOpen = true;

    if (event) {
      const target =
        (event.target as HTMLElement).closest('.btn-icon') || (event.currentTarget as HTMLElement);
      const rect = target.getBoundingClientRect();
      const rightDistance = Math.max(16, window.innerWidth - rect.right);
      if (rect.top > 320) {
        this.isFlippedBelow = false;
        this.popoverPosition = {
          bottom: `${Math.round(window.innerHeight - rect.top + 8)}px`,
          right: `${Math.round(rightDistance)}px`,
        };
      } else {
        this.isFlippedBelow = true;
        this.popoverPosition = {
          top: `${Math.round(rect.bottom + 8)}px`,
          right: `${Math.round(rightDistance)}px`,
        };
      }
    } else {
      this.popoverPosition = {
        bottom: '80px',
        right: '24px',
      };
    }
  }

  closeDetails(): void {
    this.selectedLog = null;
    this.isDetailModalOpen = false;
    this.popoverPosition = {};
  }

  exportCsv(): void {
    const headers = [
      'Mã Log',
      'Thời gian',
      'Username',
      'Họ tên',
      'Phân hệ',
      'Hành động',
      'Trạng thái',
      'Mô tả',
    ];
    const rows = this.logs.map((l) => [
      l.logCode,
      `"${l.occurredAt}"`,
      `"${l.username}"`,
      `"${l.userFullName}"`,
      `"${l.moduleLabel}"`,
      `"${l.action}"`,
      `"${l.status}"`,
      `"${(l.description ?? '').replace(/"/g, '""')}"`,
    ]);
    const csv = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Audit_Logs_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    this.toast.success('Đã xuất file CSV nhật ký hoạt động.');
  }
}
