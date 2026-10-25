import { HttpErrorResponse } from '@angular/common/http';
import { computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime } from 'rxjs';
import { AuthService } from '../../auth/services/auth.service';
import { ProcurementTicket } from '../../procurement/model/procurement';
import { ProcurementService } from '../../procurement/service/procurement.service';
import { SupplierService } from '../../suppliers/services/supplier.service';
import { PurchaseOrderService } from '../../purchase-orders/services/purchase-order.service';
import { ToastService } from '../../../core/services/toast.service';
import { RfqApiService } from '../service/rfq-api.service';
import {
  DEFAULT_WEIGHTS,
  DetailTab,
  isOpenStatus,
  KpiFilter,
  RfqTab,
  STATUS_LABELS,
} from '../helper/rfq-page.helper';
import {
  RfqRecord,
  RfqRole,
  RfqScoreWeights,
  RfqStatus,
  RfqSupplier,
  SupplierQuote,
} from '../model/rfq.models';

export abstract class RfqPageCore {
  protected readonly route = inject(ActivatedRoute);
  protected readonly router = inject(Router);
  protected readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  protected readonly destroyRef = inject(DestroyRef);
  protected readonly api = inject(RfqApiService);
  protected readonly procurement = inject(ProcurementService);
  protected readonly supplierService = inject(SupplierService);
  protected readonly purchaseOrders = inject(PurchaseOrderService);
  protected readonly toast = inject(ToastService);

  readonly role = signal<RfqRole>(this.mapRole(this.auth.getCurrentUser()?.role));
  readonly username = this.auth.getCurrentUser()?.username ?? '';
  readonly suppliers = signal<RfqSupplier[]>([]);
  readonly departments = signal<string[]>([]);
  readonly approvedTickets = signal<ProcurementTicket[]>([]);
  readonly statusEntries = Object.entries(STATUS_LABELS) as [RfqStatus, string][];
  readonly scoreKeys: (keyof RfqScoreWeights)[] = [
    'price',
    'technical',
    'delivery',
    'payment',
    'reputation',
  ];
  readonly rfqs = signal<RfqRecord[]>([]);
  readonly selectedCode = signal<string | null>(null);
  readonly detailTab = signal<DetailTab>('OVERVIEW');
  readonly activeTab = signal<RfqTab>('ALL');
  readonly activeKpi = signal<KpiFilter | null>(null);
  readonly isLoading = signal(true);
  readonly isDetailLoading = signal(false);
  readonly isSaving = signal(false);
  readonly loadError = signal('');
  readonly isCreateOpen = signal(false);
  readonly editingRfq = signal<RfqRecord | null>(null);
  readonly createError = signal('');
  readonly decisionReason = signal('');
  readonly includeVat = signal(false);
  readonly onlyDifferences = signal(false);
  readonly hiddenSuppliers = signal<string[]>([]);
  readonly winnerDraft = signal('');
  readonly winnerReasonDraft = signal('');
  readonly itemWinnerDraft = signal<Record<string, string>>({});
  readonly itemReasonDraft = signal<Record<string, string>>({});
  readonly weights = signal<RfqScoreWeights>({ ...DEFAULT_WEIGHTS });
  readonly totalElements = signal(0);
  readonly page = signal(1);
  readonly pageSize = signal(8);
  readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.totalElements() / this.pageSize())),
  );
  readonly pageStart = computed(() =>
    this.totalElements() ? (this.page() - 1) * this.pageSize() + 1 : 0,
  );
  readonly pageEnd = computed(() => Math.min(this.page() * this.pageSize(), this.totalElements()));

  readonly filtersForm = this.fb.nonNullable.group({
    query: '',
    status: '',
    department: '',
    creator: '',
    from: '',
    to: '',
  });
  readonly selectedRfq = computed(
    () => this.rfqs().find((rfq) => rfq.code === this.selectedCode()) ?? null,
  );
  readonly tabs = computed(() => {
    const roleTab =
      this.role() === 'MAKER'
        ? [{ id: 'MINE' as const, label: 'Do tôi tạo' }]
        : this.role() === 'CHECKER'
          ? [{ id: 'PENDING_ME' as const, label: 'Chờ tôi duyệt' }]
          : [{ id: 'ALL' as const, label: 'Tất cả' }];
    const statusTabs: { id: RfqTab; label: string }[] = [
      { id: 'DRAFT', label: 'Nháp' },
      { id: 'PENDING', label: 'Chờ duyệt' },
      { id: 'RECEIVING', label: 'Đang nhận báo giá' },
      { id: 'COMPARING', label: 'Đang so sánh' },
      { id: 'SELECTED', label: 'Đã chọn NCC' },
      { id: 'CLOSED', label: 'Đã hủy/Từ chối' },
    ];
    return [
      ...roleTab,
      ...statusTabs.filter((tab) => !roleTab.some((roleItem) => roleItem.id === tab.id)),
    ];
  });

  readonly kpis = computed(() => {
    const rfqs = this.rfqs();
    if (this.role() === 'MAKER') {
      const mine = rfqs.filter((rfq) => rfq.createdBy === this.username);
      return [
        this.kpi(
          'M_DRAFT',
          'Nháp của tôi',
          mine.filter((r) => r.status === 'DRAFT').length,
          'fa-file-text-o',
          'blue',
        ),
        this.kpi(
          'M_RETURNED',
          'Bị trả lại',
          mine.filter((r) => r.status === 'RETURNED').length,
          'fa-reply',
          'red',
        ),
        this.kpi(
          'M_WAITING',
          'Đang chờ NCC báo giá',
          mine.filter((r) => ['SENT', 'RECEIVING_QUOTES', 'CLOSED_QUOTES'].includes(r.status))
            .length,
          'fa-hourglass-half',
          'blue',
        ),
        this.kpi(
          'M_COMPARE',
          'Chờ so sánh',
          mine.filter((r) => ['COMPARING', 'PENDING_RESULT_APPROVAL'].includes(r.status)).length,
          'fa-balance-scale',
          'green',
        ),
      ];
    }
    if (this.role() === 'CHECKER') {
      const pending = rfqs.filter((rfq) => this.canCurrentUserReview(rfq));
      return [
        this.kpi(
          'C_RFQ',
          'Chờ duyệt RFQ',
          pending.filter((r) => r.status === 'PENDING_RFQ_APPROVAL').length,
          'fa-inbox',
          'blue',
        ),
        this.kpi(
          'C_RESULT',
          'Chờ duyệt kết quả',
          pending.filter((r) => r.status === 'PENDING_RESULT_APPROVAL').length,
          'fa-check-square-o',
          'green',
        ),
        this.kpi(
          'C_OVERDUE',
          'Quá hạn duyệt (>24h)',
          pending.filter((r) => this.hoursSince(r.createdAt) > 24).length,
          'fa-exclamation-triangle',
          'red',
        ),
        this.kpi(
          'C_APPROVED',
          'Đã duyệt tháng này',
          rfqs.filter((r) => r.reviewedAt && this.isCurrentMonth(r.reviewedAt)).length,
          'fa-calendar-check-o',
          'green',
        ),
      ];
    }
    const open = rfqs.filter((rfq) => isOpenStatus(rfq.status));
    return [
      this.kpi('A_OPEN', 'RFQ đang mở', open.length, 'fa-folder-open-o', 'blue'),
      this.kpi(
        'A_PENDING',
        'Chờ phê duyệt',
        open.filter((r) => ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'].includes(r.status))
          .length,
        'fa-clock-o',
        'green',
      ),
      this.kpi(
        'A_OVERDUE',
        'Sắp/đã quá hạn',
        open.filter((r) => this.deadlineIsPast(r.deadline)).length,
        'fa-exclamation-triangle',
        'red',
      ),
      this.kpi(
        'A_VALUE',
        'Tổng giá trị đang xử lý',
        this.formatVnd(open.reduce((sum, r) => sum + r.estimatedValue, 0)),
        'fa-money',
        'blue',
      ),
    ];
  });

  readonly filteredRfqs = computed(() => {
    const filters = this.filtersForm.getRawValue();
    return this.rfqs().filter((rfq) => {
      const tabMatches = this.matchesTab(rfq, this.activeTab());
      const kpiMatches = !this.activeKpi() || this.matchesKpi(rfq, this.activeKpi()!);
      const creatorMatches =
        !filters.creator ||
        rfq.createdBy.toLowerCase().includes(filters.creator.trim().toLowerCase());
      return tabMatches && kpiMatches && creatorMatches;
    });
  });
  readonly pageRows = computed(() => this.filteredRfqs());

  abstract canCurrentUserReview(rfq: RfqRecord): boolean;
  abstract currentQuotes(rfq: RfqRecord): SupplierQuote[];
  protected abstract visibleQuotes(rfq: RfqRecord): SupplierQuote[];
  abstract priceAt(quote: SupplierQuote, itemId: string): number | null;
  abstract quoteSubtotal(rfq: RfqRecord, quote: SupplierQuote): number;
  abstract totalCost(rfq: RfqRecord, quote: SupplierQuote): number;
  protected abstract matchesTab(rfq: RfqRecord, tab: RfqTab): boolean;
  protected abstract matchesKpi(rfq: RfqRecord, kpi: KpiFilter): boolean;
  protected abstract loadReferenceData(): void;
  protected abstract loadRfqs(deepLinkCode?: string): void;
  protected abstract loadDetail(id: string): void;
  protected abstract initializeWinnerDraft(rfq: RfqRecord): void;

  initializePage(): void {
    this.activeTab.set(
      this.role() === 'MAKER' ? 'MINE' : this.role() === 'CHECKER' ? 'PENDING_ME' : 'ALL',
    );
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const code = params.get('rfq');
      this.selectedCode.set(code);
      if (code) {
        this.detailTab.set('OVERVIEW');
        const found = this.rfqs().find((rfq) => rfq.code === code);
        if (found?.items.length) this.initializeWinnerDraft(found);
        else if (found) this.loadDetail(found.id);
        else this.loadRfqs(code);
      }
    });
    this.filtersForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(1);
        this.loadRfqs();
      });
    this.loadReferenceData();
    this.loadRfqs();
  }

  setTab(tab: RfqTab): void {
    this.activeTab.set(tab);
    this.activeKpi.set(null);
    this.page.set(1);
    this.loadRfqs();
  }

  toggleKpi(id: KpiFilter): void {
    this.activeKpi.set(this.activeKpi() === id ? null : id);
    this.activeTab.set('ALL');
    this.page.set(1);
    this.loadRfqs();
  }

  previousPage(): void {
    if (this.page() > 1) {
      this.page.update((value) => value - 1);
      this.loadRfqs();
    }
  }
  nextPage(): void {
    if (this.page() < this.pageCount()) {
      this.page.update((value) => value + 1);
      this.loadRfqs();
    }
  }
  changePageSize(value: number): void {
    this.pageSize.set(value);
    this.page.set(1);
    this.loadRfqs();
  }

  openRfq(rfq: RfqRecord): void {
    this.detailTab.set('OVERVIEW');
    this.initializeWinnerDraft(rfq);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { rfq: rfq.code },
      queryParamsHandling: 'merge',
    });
    if (!rfq.items.length) this.loadDetail(rfq.id);
  }

  closeRfq(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { rfq: null },
      queryParamsHandling: 'merge',
    });
  }

  trapDialogFocus(event: KeyboardEvent): void {
    if (event.key !== 'Tab') return;
    const dialog = event.currentTarget as HTMLElement;
    const focusable = [
      ...dialog.querySelectorAll<HTMLElement>(
        'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
      ),
    ].filter((element) => element.offsetParent !== null);
    if (!focusable.length) {
      event.preventDefault();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  setDetailTab(tab: DetailTab): void {
    this.detailTab.set(tab);
  }
  statusLabel(status: RfqStatus): string {
    return STATUS_LABELS[status];
  }
  statusClass(status: RfqStatus): string {
    if (
      ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL', 'RECEIVING_QUOTES', 'SENT'].includes(
        status,
      )
    )
      return 'pending';
    if (['RETURNED', 'REJECTED', 'CANCELED', 'EXPIRED'].includes(status)) return 'danger';
    if (['SELECTED_SUPPLIER', 'PO_CREATED'].includes(status)) return 'success';
    if (status === 'COMPARING') return 'info';
    return 'neutral';
  }

  quoteRatio(rfq: RfqRecord): string {
    return `${rfq.respondedSupplierCount}/${rfq.invitedSupplierCount}`;
  }
  hasIncompleteQuote(rfq: RfqRecord, quote: SupplierQuote): boolean {
    return rfq.items.some((item) => quote.unitPrices[item.id] == null);
  }
  hasIncompleteQuotes(rfq: RfqRecord): boolean {
    return this.currentQuotes(rfq).some((quote) => this.hasIncompleteQuote(rfq, quote));
  }
  deadlineIsPast(value: string): boolean {
    return new Date(value).getTime() < Date.now();
  }
  countdown(value: string): string {
    const hours = Math.ceil((new Date(value).getTime() - Date.now()) / 3_600_000);
    if (hours <= 0) return `Quá hạn ${Math.max(1, Math.ceil(Math.abs(hours) / 24))} ngày`;
    return hours < 24 ? `Còn ${hours} giờ` : `Còn ${Math.ceil(hours / 24)} ngày`;
  }
  formatVnd(value: number): string {
    return `${Math.round(value || 0).toLocaleString('vi-VN')} ₫`;
  }

  refresh(): void {
    this.loadError.set('');
    this.loadRfqs();
  }
  exportExcel(): void {
    const filters = this.filtersForm.getRawValue();
    const statuses = this.exportStatusFilters();
    this.api
      .exportExcel(
        {
          keyword: filters.query || undefined,
          status: (filters.status as RfqStatus | '') || '',
          department: filters.department || undefined,
          createdBy: filters.creator || undefined,
          createdFrom: filters.from || undefined,
          createdTo: filters.to || undefined,
        },
        statuses,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) =>
          this.downloadBlob(blob, `Danh_sach_Rfq_${new Date().toISOString().slice(0, 10)}.xlsx`),
        error: (error: HttpErrorResponse) => this.toast.error(this.exportErrorMessage(error)),
      });
  }

  /** Kept as an alias for any cached RFQ template that still calls the old method name. */
  exportCsv(): void {
    this.exportExcel();
  }

  exportComparisonExcel(): void {
    const rfq = this.selectedRfq();
    if (!rfq) return;
    this.api
      .exportComparisonExcel(rfq.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => this.downloadBlob(blob, `${rfq.code}-so-sanh-bao-gia.xlsx`),
        error: (error: HttpErrorResponse) => this.toast.error(this.exportErrorMessage(error)),
      });
  }

  exportComparisonPdf(): void {
    const rfq = this.selectedRfq();
    if (!rfq) return;
    this.api
      .exportComparisonPdf(rfq.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => this.downloadBlob(blob, `${rfq.code}-so-sanh-bao-gia.pdf`),
        error: (error: HttpErrorResponse) => this.toast.error(this.exportErrorMessage(error)),
      });
  }

  protected kpi(
    id: KpiFilter,
    label: string,
    value: number | string,
    icon: string,
    color: 'blue' | 'green' | 'red',
  ) {
    return { id, label, value, icon, color };
  }
  protected mapRole(role?: string): RfqRole {
    return role === 'ADMIN' ? 'ADMIN' : role === 'CHECKER' ? 'CHECKER' : 'MAKER';
  }
  supplierName(id: string): string {
    return (
      this.suppliers().find((supplier) => supplier.id === id)?.name ??
      this.rfqs().flatMap((rfq) => [
        ...rfq.invitedSuppliers
          .filter((invite) => invite.supplierId === id)
          .map((invite) => invite.supplierName),
        ...rfq.quotes.filter((quote) => quote.supplierId === id).map((quote) => quote.supplierName),
        ...(rfq.chosenSupplierId === id && rfq.chosenSupplierName ? [rfq.chosenSupplierName] : []),
      ])[0] ??
      id
    );
  }
  protected hoursSince(value: string): number {
    return (Date.now() - new Date(value).getTime()) / 3_600_000;
  }
  protected isCurrentMonth(value: string): boolean {
    const date = new Date(value);
    const now = new Date();
    return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
  }
  private exportStatusFilters(): RfqStatus[] {
    if (this.filtersForm.controls.status.value) return [];
    const kpi = this.activeKpi();
    if (kpi) {
      switch (kpi) {
        case 'M_DRAFT':
          return ['DRAFT'];
        case 'M_RETURNED':
          return ['RETURNED'];
        case 'M_WAITING':
          return ['SENT', 'RECEIVING_QUOTES', 'CLOSED_QUOTES'];
        case 'M_COMPARE':
          return ['COMPARING', 'PENDING_RESULT_APPROVAL'];
        case 'C_RFQ':
          return ['PENDING_RFQ_APPROVAL'];
        case 'C_RESULT':
          return ['PENDING_RESULT_APPROVAL'];
        case 'C_OVERDUE':
          return ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'];
        case 'A_PENDING':
          return ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'];
        case 'A_OPEN':
        case 'A_VALUE':
        case 'A_OVERDUE':
          return [
            'DRAFT',
            'PENDING_RFQ_APPROVAL',
            'RETURNED',
            'SENT',
            'RECEIVING_QUOTES',
            'CLOSED_QUOTES',
            'COMPARING',
            'PENDING_RESULT_APPROVAL',
          ];
        case 'C_APPROVED':
          return [];
      }
    }
    switch (this.activeTab()) {
      case 'DRAFT':
        return ['DRAFT'];
      case 'PENDING':
      case 'PENDING_ME':
        return ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'];
      case 'RECEIVING':
        return ['SENT', 'RECEIVING_QUOTES', 'CLOSED_QUOTES'];
      case 'COMPARING':
        return ['COMPARING'];
      case 'SELECTED':
        return ['SELECTED_SUPPLIER', 'PO_CREATED'];
      case 'CLOSED':
        return ['CANCELED', 'REJECTED', 'EXPIRED'];
      default:
        return [];
    }
  }

  protected downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  private exportErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) return 'Bạn không có quyền xuất dữ liệu RFQ này.';
    if (error.status === 404) return 'Không tìm thấy RFQ cần xuất.';
    return 'Không thể tạo file Excel hoặc PDF. Vui lòng thử lại.';
  }
}
