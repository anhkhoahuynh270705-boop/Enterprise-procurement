import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RfqFormSaveEvent } from '../model/rfq-form-save.event';
import { RfqPageMapper } from '../rfq-mapper/rfq-page.mapper';
import {
  CreateRfqRequest,
  RfqCriterion,
  RfqItem,
  RfqRecord,
  RfqScoreWeights,
  SupplierQuote,
} from '../model/rfq.models';

import { RfqPageDataController } from '../rfq-data/rfq-page-data.controller';
import { CRITERION_FROM_KEY, isOpenStatus, SCORE_LABELS } from '../helper/rfq-page.helper';

export class RfqPageWorkflowController extends RfqPageDataController {
  canClone(rfq: RfqRecord): boolean {
    return (
      this.role() === 'MAKER' &&
      rfq.createdBy === this.username &&
      ['DRAFT', 'RETURNED'].includes(rfq.status)
    );
  }
  canEdit(rfq: RfqRecord): boolean {
    return this.canClone(rfq);
  }
  canCancel(rfq: RfqRecord): boolean {
    return (
      isOpenStatus(rfq.status) &&
      (this.role() === 'ADMIN' || (this.role() === 'MAKER' && rfq.createdBy === this.username))
    );
  }
  canCreatePo(rfq: RfqRecord): boolean {
    return this.role() === 'MAKER' && rfq.status === 'SELECTED_SUPPLIER' && !!rfq.chosenSupplierId;
  }
  canCurrentUserReview(rfq: RfqRecord): boolean {
    return (
      (this.role() === 'CHECKER' || this.role() === 'ADMIN') &&
      rfq.createdBy !== this.username &&
      ['PENDING_RFQ_APPROVAL', 'PENDING_RESULT_APPROVAL'].includes(rfq.status)
    );
  }

  cloneRfq(rfq: RfqRecord): void {
    if (!this.canClone(rfq)) return;
    if (!rfq.items.length) {
      this.api
        .getById(rfq.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: ({ data }) => this.cloneRfq(RfqPageMapper.fromDetail(data)),
          error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
        });
      return;
    }
    const request: CreateRfqRequest = {
      procurementTicketId: rfq.procurementTicketId,
      title: `${rfq.title} (bản sao)`,
      department: rfq.department,
      deadline: this.defaultDeadline(),
      quoteValidityDays: rfq.validityDays,
      deliveryLocation: rfq.deliveryLocation,
      deliveryWindow: rfq.deliveryWindow,
      supplierIds: [...rfq.invitedSupplierIds],
      exceptionReason: rfq.exceptionReason,
      items: rfq.items.map((item) => this.toItemRequest(item)),
      scoreCriteria: this.toCriteria(rfq.weights),
      submitForApproval: false,
    };
    this.api
      .create(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          this.upsertRfq(RfqPageMapper.fromDetail(data));
          this.toast.success('Đã tạo bản nháp từ RFQ.');
          this.loadRfqs();
        },
        error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
      });
  }

  cancelRfq(rfq: RfqRecord): void {
    if (!this.canCancel(rfq) || !confirm(`Hủy ${rfq.code}? Thao tác này không thể hoàn tác.`))
      return;
    const reason = window.prompt('Nhập lý do hủy (tối thiểu 5 ký tự):')?.trim() ?? '';
    if (reason.length < 5) {
      this.toast.warning('Lý do hủy cần tối thiểu 5 ký tự.');
      return;
    }
    this.mutateDetail(this.api.cancel(rfq.id, { reason }), 'Đã hủy yêu cầu báo giá.');
  }

  requestRequote(rfq: RfqRecord): void {
    if (
      this.role() !== 'MAKER' ||
      rfq.createdBy !== this.username ||
      !['CLOSED_QUOTES', 'COMPARING'].includes(rfq.status)
    )
      return;
    if (!confirm(`Yêu cầu các nhà cung cấp đã mời chào giá lại cho ${rfq.code}?`)) return;
    const reason = window.prompt('Nhập lý do yêu cầu chào lại (tối thiểu 5 ký tự):')?.trim() ?? '';
    if (reason.length < 5) {
      this.toast.warning('Lý do yêu cầu chào lại cần tối thiểu 5 ký tự.');
      return;
    }
    this.api
      .requestRequote(rfq.id, rfq.invitedSupplierIds, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          this.upsertRfq(RfqPageMapper.fromDetail(data));
          this.toast.success(
            'Đã mở vòng nhận báo giá lại. Gửi link mới cho từng nhà cung cấp từ danh sách mời.',
          );
        },
        error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
      });
  }

  closeQuotes(rfq: RfqRecord): void {
    if (!confirm(`Đóng nhận báo giá cho ${rfq.code}?`)) return;
    const reason = window.prompt('Nhập lý do đóng nhận báo giá (tối thiểu 5 ký tự):')?.trim() ?? '';
    if (reason.length < 5) {
      this.toast.warning('Lý do cần tối thiểu 5 ký tự.');
      return;
    }
    this.mutateDetail(this.api.closeQuotes(rfq.id, { reason }), 'Đã đóng nhận báo giá.');
  }

  startComparison(rfq: RfqRecord): void {
    this.mutateDetail(this.api.startComparison(rfq.id), 'Đã bắt đầu so sánh báo giá.');
  }

  sendInvitation(rfq: RfqRecord, supplierId: string): void {
    this.api
      .sendInvitationLink(rfq.id, supplierId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => this.toast.success(`Đã gửi link báo giá tới ${data.supplierName}.`),
        error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
      });
  }

  createPurchaseOrder(rfq: RfqRecord): void {
    if (!this.canCreatePo(rfq) || !confirm(`Tạo đơn đặt hàng từ ${rfq.code}?`)) return;
    const itemIds = rfq.items
      .map((item) => item.procurementItemId)
      .filter((id): id is string => !!id);
    if (itemIds.length !== rfq.items.length) {
      this.toast.error(
        'Không thể tạo PO vì RFQ có hạng mục không liên kết với hạng mục trong phiếu mua sắm.',
      );
      return;
    }
    this.purchaseOrders
      .createPurchaseOrder({
        ticketId: rfq.procurementTicketId,
        supplierId: rfq.chosenSupplierId!,
        procurementItemIds: itemIds,
        notes: `Được tạo từ ${rfq.code}`,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          this.toast.success(`Đã tạo đơn đặt hàng ${data.orderCode}.`);
          void this.router.navigate(['/purchase-orders']);
        },
        error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
      });
  }

  chooseWinner(supplierId: string): void {
    this.winnerDraft.set(supplierId);
  }
  setWinnerReason(value: string): void {
    this.winnerReasonDraft.set(value);
  }
  setItemWinner(itemId: string, supplierId: string): void {
    this.itemWinnerDraft.update((winners) => ({ ...winners, [itemId]: supplierId }));
  }
  setItemReason(itemId: string, reason: string): void {
    this.itemReasonDraft.update((reasons) => ({ ...reasons, [itemId]: reason }));
  }
  globalWinnerNeedsReason(rfq: RfqRecord): boolean {
    const supplierId = this.winnerDraft();
    return (
      !!supplierId &&
      (supplierId !== this.lowestPriceSupplier(rfq)?.supplierId ||
        supplierId !== this.topScoreSupplier(rfq)?.supplierId)
    );
  }
  itemWinnerNeedsReason(rfq: RfqRecord, itemId: string): boolean {
    const winner = this.itemWinnerDraft()[itemId];
    const lowest = this.lowestPriceSupplierForItem(rfq, itemId);
    return !!winner && !!lowest && winner !== lowest;
  }

  saveWinner(rfq: RfqRecord): void {
    if (!this.winnerDraft()) return;
    if (this.globalWinnerNeedsReason(rfq) && this.winnerReasonDraft().trim().length < 10) return;
    const winners = this.buildItemWinners(rfq);
    if (winners === null) return;
    this.mutateDetail(
      this.api.selectWinners(rfq.id, {
        supplierId: this.winnerDraft(),
        reason: this.winnerReasonDraft().trim() || undefined,
        itemWinners: winners,
        submitForApproval: false,
      }),
      'Đã lưu đề xuất nhà cung cấp.',
    );
  }

  submitResultForApproval(rfq: RfqRecord): void {
    if (!this.winnerDraft() || this.weightTotal() !== 100) return;
    if (this.globalWinnerNeedsReason(rfq) && this.winnerReasonDraft().trim().length < 10) return;
    const winners = this.buildItemWinners(rfq);
    if (winners === null || !confirm(`Trình duyệt kết quả so sánh của ${rfq.code}?`)) return;
    this.mutateDetail(
      this.api.selectWinners(rfq.id, {
        supplierId: this.winnerDraft(),
        reason: this.winnerReasonDraft().trim() || undefined,
        itemWinners: winners,
        submitForApproval: true,
      }),
      'Đã trình duyệt kết quả lựa chọn nhà cung cấp.',
      'APPROVAL',
    );
  }

  decision(rfq: RfqRecord, action: 'APPROVE' | 'RETURN' | 'REJECT'): void {
    if (!this.canCurrentUserReview(rfq)) return;
    const reason = this.decisionReason().trim();
    if (reason.length < 10) return;
    const label = action === 'APPROVE' ? 'Duyệt' : action === 'RETURN' ? 'Trả lại' : 'Từ chối';
    if (!confirm(`${label} ${rfq.code}?`)) return;
    this.mutateDetail(
      this.api.decide(rfq.id, { decision: action, reason }),
      `Đã ghi nhận quyết định: ${label.toLowerCase()}.`,
    );
    this.decisionReason.set('');
  }

  getVisibleQuotes(rfq: RfqRecord): SupplierQuote[] {
    return this.visibleQuotes(rfq);
  }
  currentQuotes(rfq: RfqRecord): SupplierQuote[] {
    const latestBySupplier = new Map<string, SupplierQuote>();
    for (const quote of rfq.quotes) {
      const current = latestBySupplier.get(quote.supplierId);
      if (!current || quote.round > current.round) latestBySupplier.set(quote.supplierId, quote);
    }
    return [...latestBySupplier.values()].sort((a, b) =>
      a.supplierName.localeCompare(b.supplierName, 'vi'),
    );
  }
  priceAt(quote: SupplierQuote, itemId: string): number | null {
    return quote.unitPrices[itemId] ?? null;
  }
  lineTotal(quote: SupplierQuote, item: RfqItem): number | null {
    const price = this.priceAt(quote, item.id);
    return price === null ? null : price * item.quantity;
  }
  quoteSubtotal(rfq: RfqRecord, quote: SupplierQuote): number {
    return rfq.items.reduce((sum, item) => sum + (this.lineTotal(quote, item) ?? 0), 0);
  }
  totalCost(rfq: RfqRecord, quote: SupplierQuote): number {
    return (
      this.quoteSubtotal(rfq, quote) +
      (this.includeVat() ? quote.taxAmount : 0) +
      quote.shipping +
      quote.otherCost
    );
  }
  valueVsBudget(rfq: RfqRecord, quote: SupplierQuote): number {
    const budget = rfq.items.reduce((sum, item) => sum + item.budgetUnitPrice * item.quantity, 0);
    return budget ? ((this.quoteSubtotal(rfq, quote) - budget) / budget) * 100 : 0;
  }
  pctDiffFromLowest(rfq: RfqRecord, quote: SupplierQuote, itemId?: string): number | null {
    const prices = this.currentQuotes(rfq)
      .map((q) => (itemId ? this.priceAt(q, itemId) : this.quoteSubtotal(rfq, q)))
      .filter((price): price is number => price !== null && price > 0);
    if (!prices.length) return null;
    const current = itemId ? this.priceAt(quote, itemId) : this.quoteSubtotal(rfq, quote);
    return current === null ? null : ((current - Math.min(...prices)) / Math.min(...prices)) * 100;
  }
  quoteCellClass(rfq: RfqRecord, quote: SupplierQuote, itemId: string): string {
    const price = this.priceAt(quote, itemId);
    if (price === null) return 'no-quote';
    const item = rfq.items.find((candidate) => candidate.id === itemId);
    if (!item) return '';
    const prices = this.currentQuotes(rfq)
      .map((q) => this.priceAt(q, itemId))
      .filter((value): value is number => value !== null);
    if (price > item.budgetUnitPrice) return 'over-budget';
    if (price === Math.min(...prices)) return 'lowest';
    if (price === Math.max(...prices)) return 'highest';
    return '';
  }
  lowestPriceSupplier(rfq: RfqRecord): SupplierQuote | null {
    const complete = this.currentQuotes(rfq).filter((quote) =>
      rfq.items.every((item) => this.priceAt(quote, item.id) !== null),
    );
    return (
      [...complete].sort((a, b) => this.quoteSubtotal(rfq, a) - this.quoteSubtotal(rfq, b))[0] ??
      null
    );
  }
  lowestPriceSupplierForItem(rfq: RfqRecord, itemId: string): string | null {
    return (
      [...this.currentQuotes(rfq)]
        .filter((quote) => this.priceAt(quote, itemId) !== null)
        .sort((a, b) => this.priceAt(a, itemId)! - this.priceAt(b, itemId)!)[0]?.supplierId ?? null
    );
  }
  topScoreSupplier(rfq: RfqRecord): SupplierQuote | null {
    return this.rankedQuotes(rfq)[0] ?? null;
  }
  weightedScore(_rfq: RfqRecord, quote: SupplierQuote): number {
    return (
      quote.totalScore ??
      (this.scoreValue(_rfq, quote, 'price') * this.weights().price +
        quote.score.technical * this.weights().technical +
        quote.score.delivery * this.weights().delivery +
        quote.score.payment * this.weights().payment +
        quote.score.reputation * this.weights().reputation) /
        100
    );
  }
  rankedQuotes(rfq: RfqRecord): SupplierQuote[] {
    return [...this.currentQuotes(rfq)].sort(
      (a, b) =>
        (a.rank ?? Number.MAX_SAFE_INTEGER) - (b.rank ?? Number.MAX_SAFE_INTEGER) ||
        this.weightedScore(rfq, b) - this.weightedScore(rfq, a),
    );
  }
  updateQuoteScore(
    rfqCode: string,
    quoteId: string,
    key: keyof RfqScoreWeights,
    value: number,
  ): void {
    if (key === 'price') return;
    const rfq = this.rfqs().find((item) => item.code === rfqCode);
    const quote = rfq?.quotes.find((candidate) => candidate.id === quoteId);
    if (!rfq || !quote) return;
    const score = Math.max(0, Math.min(100, Number(value) || 0));
    const nextScore = { ...quote.score, [key]: score };
    const criteria = this.scoreKeys
      .filter((criterion) => criterion !== 'price')
      .map((criterion) => ({
        criterion: CRITERION_FROM_KEY[criterion],
        score: nextScore[criterion],
      }));
    this.api
      .updateScores(rfq.id, quote.id, { scores: criteria })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => this.upsertRfq(RfqPageMapper.fromDetail(data)),
        error: (error: HttpErrorResponse) => this.toast.error(this.errorMessage(error)),
      });
  }
  scoreLabel(key: keyof RfqScoreWeights): string {
    return SCORE_LABELS[key];
  }
  weightTotal(): number {
    return Object.values(this.weights()).reduce((sum, weight) => sum + weight, 0);
  }
  toggleSupplier(supplierId: string): void {
    this.hiddenSuppliers.update((hidden) =>
      hidden.includes(supplierId)
        ? hidden.filter((id) => id !== supplierId)
        : [...hidden, supplierId],
    );
  }
  toggleDifferences(): void {
    this.onlyDifferences.update((value) => !value);
  }
  toggleVat(): void {
    this.includeVat.update((value) => !value);
  }
  technicalLabel(status: SupplierQuote['technicalStatus']): string {
    return status === 'PASS' ? 'Đạt' : status === 'PARTIAL' ? 'Đạt một phần' : 'Không đạt';
  }
  technicalClass(status: SupplierQuote['technicalStatus']): string {
    return status === 'PASS' ? 'success' : status === 'PARTIAL' ? 'warning' : 'danger';
  }
  scoreValue(rfq: RfqRecord, quote: SupplierQuote, key: keyof RfqScoreWeights): number {
    return key === 'price' ? quote.score.price || this.priceScore(rfq, quote) : quote.score[key];
  }
  matrixItems(rfq: RfqRecord): RfqItem[] {
    return this.onlyDifferences()
      ? rfq.items.filter(
          (item) =>
            new Set(this.currentQuotes(rfq).map((quote) => this.priceAt(quote, item.id))).size > 1,
        )
      : rfq.items;
  }

  openCreate(): void {
    if (this.role() !== 'MAKER') return;
    this.editingRfq.set(null);
    this.createError.set('');
    this.isCreateOpen.set(true);
  }

  editRfq(rfq: RfqRecord): void {
    if (!this.canEdit(rfq)) return;
    this.editingRfq.set(rfq);
    this.createError.set('');
    this.isCreateOpen.set(true);
  }

  closeCreate(): void {
    this.isCreateOpen.set(false);
    this.editingRfq.set(null);
  }

  saveRfqForm(event: RfqFormSaveEvent): void {
    if (this.role() !== 'MAKER') return;
    const request$ = event.rfqId
      ? this.api.updateAndMaybeSubmit(
          event.rfqId,
          {
            title: event.request.title,
            department: event.request.department,
            deadline: event.request.deadline,
            quoteValidityDays: event.request.quoteValidityDays,
            deliveryLocation: event.request.deliveryLocation,
            deliveryWindow: event.request.deliveryWindow,
            supplierIds: event.request.supplierIds,
            exceptionReason: event.request.exceptionReason,
            items: event.request.items,
            scoreCriteria: event.request.scoreCriteria,
          },
          event.submit,
        )
      : this.api.create(event.request);

    this.isSaving.set(true);
    this.createError.set('');
    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ data }) => {
        this.isSaving.set(false);
        this.closeCreate();
        const rfq = RfqPageMapper.fromDetail(data);
        this.upsertRfq(rfq);
        this.toast.success(
          event.rfqId
            ? event.submit
              ? 'Đã cập nhật và trình duyệt RFQ.'
              : 'Đã cập nhật RFQ.'
            : event.submit
              ? 'Đã trình duyệt RFQ.'
              : 'Đã lưu bản nháp RFQ.',
        );
        this.activeTab.set('MINE');
        this.activeKpi.set(null);
        this.loadRfqs();
        this.openRfq(rfq);
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.createError.set(this.errorMessage(error));
      },
    });
  }

  protected visibleQuotes(rfq: RfqRecord): SupplierQuote[] {
    return this.currentQuotes(rfq).filter(
      (quote) => !this.hiddenSuppliers().includes(quote.supplierId),
    );
  }
  private priceScore(rfq: RfqRecord, quote: SupplierQuote): number {
    const totals = this.currentQuotes(rfq)
      .map((item) => this.quoteSubtotal(rfq, item))
      .filter((value) => value > 0);
    const current = this.quoteSubtotal(rfq, quote);
    const lowest = totals.length ? Math.min(...totals) : 0;
    return current && lowest ? Math.max(0, Math.min(100, (lowest / current) * 100)) : 0;
  }
  private buildItemWinners(
    rfq: RfqRecord,
  ): Array<{ rfqItemId: string; supplierId: string; reason?: string }> | null {
    const winners = this.itemWinnerDraft();
    const reasons = this.itemReasonDraft();
    for (const item of rfq.items) {
      if (
        winners[item.id] &&
        this.itemWinnerNeedsReason(rfq, item.id) &&
        (reasons[item.id] ?? '').trim().length < 10
      ) {
        this.toast.warning('Nhập lý do từ 10 ký tự khi chọn nhà cung cấp không có giá thấp nhất.');
        return null;
      }
    }
    return Object.entries(winners)
      .filter(([, supplierId]) => !!supplierId)
      .map(([rfqItemId, supplierId]) => ({
        rfqItemId,
        supplierId,
        reason: reasons[rfqItemId]?.trim() || undefined,
      }));
  }
}
