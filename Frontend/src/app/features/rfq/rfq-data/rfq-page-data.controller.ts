import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProcurementTicket } from '../../procurement/model/procurement';
import { RfqPageMapper } from '../rfq-mapper/rfq-page.mapper';
import {
  CRITERION_FROM_KEY,
  dayString,
  DetailTab,
  KpiFilter,
  RfqTab,
} from '../helper/rfq-page.helper';
import { RfqPageCore } from '../rfq-core/rfq-page-core';
import {
  RfqDetailDto,
  RfqItem,
  RfqRecord,
  RfqScoreWeights,
  RfqStatus,
  SupplierQuote,
} from '../model/rfq.models';

export abstract class RfqPageDataController extends RfqPageCore {
  private listLoadSequence = 0;
  protected loadReferenceData(): void {
    this.procurement
      .getTicketPage('APPROVED', 0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          const approvedTickets = data.content ?? [];
          const selectableTickets = this.role() === 'ADMIN'
            ? approvedTickets
            : approvedTickets.filter((ticket) => ticket.makerUsername === this.username);
          this.approvedTickets.set(selectableTickets);
          this.departments.set(
            [
              ...new Set(
                selectableTickets
                  .map((ticket) => ticket.department)
                  .filter((value): value is string => !!value),
              ),
            ].sort(),
          );
        },
        error: (error: HttpErrorResponse) =>
          this.toast.error(`Không thể tải phiếu đã duyệt: ${this.errorMessage(error)}`),
      });
    if (this.role() !== 'CHECKER') {
      this.supplierService
        .getSuppliers()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (suppliers) =>
            this.suppliers.set(suppliers.map((supplier) => RfqPageMapper.fromSupplier(supplier))),
          error: (error: HttpErrorResponse) =>
            this.toast.error(`Không thể tải nhà cung cấp: ${this.errorMessage(error)}`),
        });
    }
  }

  protected loadRfqs(deepLinkCode?: string): void {
    const sequence = ++this.listLoadSequence;
    this.isLoading.set(true);
    this.loadError.set('');
    const filters = this.filtersForm.getRawValue();
    const selectedStatus = (filters.status as RfqStatus) || undefined;
    const tabStatus = !deepLinkCode && !selectedStatus ? this.tabStatus(this.activeTab()) : undefined;
    const kpiStatus = !deepLinkCode && !selectedStatus && !tabStatus
      ? this.kpiStatus(this.activeKpi())
      : undefined;
    const groupedStatuses = !deepLinkCode && !selectedStatus && !tabStatus && !kpiStatus
      ? this.groupedStatuses()
      : [];
    const params = {
      keyword: deepLinkCode || filters.query || undefined,
      status: deepLinkCode ? undefined : selectedStatus || tabStatus || kpiStatus,
      department: deepLinkCode ? undefined : filters.department || undefined,
      creator: deepLinkCode ? undefined : filters.creator.trim() || undefined,
      kpi: deepLinkCode ? undefined : this.activeKpi() || undefined,
      excludeCreator: !deepLinkCode && (
        this.activeTab() === 'PENDING_ME' ||
        ['C_RFQ', 'C_RESULT', 'C_OVERDUE'].includes(this.activeKpi() || '')
      ),
      createdFrom: deepLinkCode ? undefined : filters.from || undefined,
      createdTo: deepLinkCode ? undefined : filters.to || undefined,
      page: deepLinkCode ? 0 : this.page() - 1,
      size: deepLinkCode ? 100 : this.pageSize(),
    };
    const finishLoading = (records: RfqRecord[], total: number) => {
      if (!deepLinkCode) {
        const lastPage = Math.max(1, Math.ceil(total / this.pageSize()));
        if (this.page() > lastPage) {
          this.page.set(lastPage);
          this.loadRfqs();
          return;
        }
      }
      this.rfqs.set(records);
      this.totalElements.set(total);
      this.isLoading.set(false);
      if (deepLinkCode) this.openDeepLinkFromList(records, deepLinkCode);
      else {
        const selected = records.find((rfq) => rfq.code === this.selectedCode());
        if (selected) this.loadDetail(selected.id);
      }
    };
    const showError = (error: HttpErrorResponse) => {
      this.isLoading.set(false);
      this.loadError.set(this.errorMessage(error));
    };
    this.api.search(params, groupedStatuses)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          if (sequence !== this.listLoadSequence) return;
          const records = (data.content ?? []).map((row) => RfqPageMapper.fromSummary(row));
          finishLoading(records, data.totalElements ?? records.length);
        },
        error: (error) => {
          if (sequence === this.listLoadSequence) showError(error);
        },
      });
  }

  protected loadDetail(id: string): void {
    this.isDetailLoading.set(true);
    this.api
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          const rfq = RfqPageMapper.fromDetail(data);
          this.upsertRfq(rfq);
          this.initializeWinnerDraft(rfq);
          this.isDetailLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.isDetailLoading.set(false);
          this.loadError.set(this.errorMessage(error));
        },
      });
  }

  protected mutateDetail(
    request: import('rxjs').Observable<{ data: RfqDetailDto }>,
    success: string,
    tab?: DetailTab,
  ): void {
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ data }) => {
        const rfq = RfqPageMapper.fromDetail(data);
        this.upsertRfq(rfq);
        this.initializeWinnerDraft(rfq);
        if (tab) this.detailTab.set(tab);
        this.toast.success(success);
        this.loadRfqs();
      },
      error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
    });
  }

  protected toItemRequest(item: RfqItem) {
    return {
      procurementItemId: item.procurementItemId || undefined,
      sku: item.sku || undefined,
      itemName: item.name,
      category: item.category || undefined,
      quantity: item.quantity,
      unit: item.unit || undefined,
      budgetUnitPrice: item.budgetUnitPrice,
    };
  }
  protected toCriteria(weights: RfqScoreWeights) {
    return this.scoreKeys.map((key) => ({
      criterion: CRITERION_FROM_KEY[key],
      weightPercent: weights[key],
    }));
  }
  protected defaultDeadline(): string {
    return `${dayString(new Date(Date.now() + 7 * 86_400_000))}T23:59:00`;
  }
  protected tabStatus(tab: RfqTab): RfqStatus | undefined {
    if (tab === 'DRAFT') return 'DRAFT';
    if (tab === 'COMPARING') return 'COMPARING';
    return undefined;
  }
  protected kpiStatus(kpi: KpiFilter | null): RfqStatus | undefined {
    if (kpi === 'M_DRAFT') return 'DRAFT';
    if (kpi === 'M_RETURNED') return 'RETURNED';
    if (kpi === 'C_RFQ') return 'PENDING_RFQ_APPROVAL';
    if (kpi === 'C_RESULT') return 'PENDING_RESULT_APPROVAL';
    return undefined;
  }
  protected groupedStatuses(): RfqStatus[] {
    switch (this.activeKpi()) {
      case 'M_WAITING':
        return ['SENT', 'RECEIVING_QUOTES', 'CLOSED_QUOTES'];
      case 'M_COMPARE':
        return ['COMPARING', 'PENDING_RESULT_APPROVAL'];
      default:
        break;
    }
    switch (this.activeTab()) {
      case 'PENDING':
      case 'PENDING_ME':
        return ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'];
      case 'RECEIVING':
        return ['SENT', 'RECEIVING_QUOTES', 'CLOSED_QUOTES'];
      case 'SELECTED':
        return ['SELECTED_SUPPLIER', 'PO_CREATED'];
      case 'CLOSED':
        return ['CANCELED', 'REJECTED', 'EXPIRED'];
      default:
        return [];
    }
  }
  protected openDeepLinkFromList(list: RfqRecord[], code: string): void {
    const match = list.find((rfq) => rfq.code === code);
    if (match) this.loadDetail(match.id);
    else this.loadError.set('Không tìm thấy RFQ theo mã trên đường dẫn.');
  }
  protected upsertRfq(rfq: RfqRecord): void {
    this.rfqs.update((items) =>
      items.some((item) => item.id === rfq.id)
        ? items.map((item) => (item.id === rfq.id ? rfq : item))
        : [rfq, ...items],
    );
  }
  protected initializeWinnerDraft(rfq: RfqRecord): void {
    this.winnerDraft.set(rfq.chosenSupplierId ?? '');
    this.winnerReasonDraft.set(rfq.selectionReason ?? '');
    this.itemWinnerDraft.set({ ...(rfq.itemWinners ?? {}) });
    this.itemReasonDraft.set({ ...(rfq.itemReasons ?? {}) });
    this.weights.set({ ...rfq.weights });
  }
  protected errorMessage(error: HttpErrorResponse): string {
    const body = error.error;
    if (typeof body?.message === 'string') return body.message;
    if (typeof body?.detail === 'string') return body.detail;
    return error.status === 0
      ? 'Không thể kết nối máy chủ. Vui lòng thử lại.'
      : 'Thao tác không thành công. Vui lòng thử lại.';
  }
}
