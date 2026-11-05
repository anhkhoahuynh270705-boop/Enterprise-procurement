import { CommonModule } from '@angular/common';
import { Component, ChangeDetectorRef, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmationDialogService } from '../../../core/services/confirmation-dialog.service';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierOption } from '../../suppliers/models/supplier-document';
import { SupplierDocumentService } from '../../suppliers/services/supplier-document.service';
import { PurchaseOrderCreate } from '../purchase-order-create/purchase-order-create';
import { PurchaseOrderView } from '../purchase-order-view/purchase-order-view';
import {
  PurchaseOrder,
  PurchaseOrderStatus,
} from '../models/purchase-order';
import { PurchaseOrderService } from '../services/purchase-order.service';

@Component({
  selector: 'app-purchase-order-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PurchaseOrderCreate, PurchaseOrderView],
  templateUrl: './purchase-order-list.html',
  styleUrl: './purchase-order-list.scss',
})
export class PurchaseOrderList implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  private readonly dialogs = inject(ConfirmationDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly supplierDocumentService = inject(SupplierDocumentService);
  private readonly fb = inject(FormBuilder);

  @ViewChild('poView') purchaseOrderView?: PurchaseOrderView;

  readonly filterForm = this.fb.nonNullable.group({
    searchQuery: '',
    selectedStatus: '' as '' | PurchaseOrderStatus,
    supplierId: '',
    ticket: '',
    orderCode: '',
    minimumAmount: '',
    maximumAmount: '',
    createdFrom: '',
    createdTo: '',
    deliveryFrom: '',
    deliveryTo: '',
  });
  orders: PurchaseOrder[] = [];
  supplierOptions: SupplierOption[] = [];
  loading = false;
  loadingSuppliers = false;
  creating = false;
  updatingOrderId: string | null = null;
  isCreateFormOpen = false;
  isAdvancedSearchOpen = false;
  private supplierOptionsLoaded = false;
  private appliedFilters = this.filterForm.getRawValue();
  private loadSequence = 0;
  currentPage = 0;
  readonly pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.appliedFilters = this.filterForm.getRawValue();
        this.currentPage = 0;
        this.loadOrders();
        this.cdr.markForCheck();
      });
    this.loadOrders();
    this.loadSupplierOptions();
  }

  get hasActiveFilters(): boolean {
    return Object.values(this.appliedFilters).some((value) => value !== '');
  }

  canViewOrder(order: PurchaseOrder): boolean {
    if (this.auth.hasRole('ADMIN', 'CHECKER')) return true;
    if (order.status === 'SENT' || order.status === 'RECEIVED') return true;
    const isOwnPendingOrder = order.createdBy === this.auth.getCurrentUser()?.username;
    return (
      isOwnPendingOrder &&
      (order.status === 'PENDING_APPROVAL' || order.status === 'DRAFT')
    );
  }

  resetFilters(): void {
    const emptyFilters: typeof this.appliedFilters = {
      searchQuery: '',
      selectedStatus: '' as '' | PurchaseOrderStatus,
      supplierId: '',
      ticket: '',
      orderCode: '',
      minimumAmount: '',
      maximumAmount: '',
      createdFrom: '',
      createdTo: '',
      deliveryFrom: '',
      deliveryTo: '',
    };
    this.filterForm.reset(emptyFilters, { emitEvent: false });
    this.appliedFilters = emptyFilters;
    this.currentPage = 0;
    this.loadOrders();
    this.cdr.markForCheck();
  }

  private parseAmount(value: string): number | null {
    if (!value.trim()) return null;
    const amount = Number(value);
    return Number.isFinite(amount) ? amount : null;
  }

  toggleAdvancedSearch(): void {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
    if (this.isAdvancedSearchOpen && !this.supplierOptionsLoaded && !this.loadingSuppliers) {
      this.loadSupplierOptions();
    }
  }

  loadOrders(): void {
    const sequence = ++this.loadSequence;
    this.loading = true;
    const filters = this.appliedFilters;
    this.purchaseOrderService.searchPurchaseOrders({
      keyword: filters.searchQuery.trim() || undefined,
      status: filters.selectedStatus || undefined,
      supplierId: filters.supplierId || undefined,
      ticket: filters.ticket.trim() || undefined,
      orderCode: filters.orderCode.trim() || undefined,
      minimumAmount: this.parseAmount(filters.minimumAmount) ?? undefined,
      maximumAmount: this.parseAmount(filters.maximumAmount) ?? undefined,
      createdFrom: filters.createdFrom || undefined,
      createdTo: filters.createdTo || undefined,
      deliveryFrom: filters.deliveryFrom || undefined,
      deliveryTo: filters.deliveryTo || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: (response) => {
        if (sequence !== this.loadSequence) return;
        const result = response.data;
        const lastPage = Math.max((result.totalPages || 0) - 1, 0);
        if (this.currentPage > lastPage) {
          this.currentPage = lastPage;
          this.loadOrders();
          return;
        }
        this.orders = result.content || [];
        this.totalElements = result.totalElements || 0;
        this.totalPages = result.totalPages || 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        if (sequence !== this.loadSequence) return;
        this.orders = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.toast.error(error.error?.message || 'Không thể tải danh sách đơn đặt hàng.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.loading) return;
    this.currentPage = page;
    this.loadOrders();
  }

  private loadSupplierOptions(): void {
    this.loadingSuppliers = true;
    this.supplierDocumentService.suppliers().subscribe({
      next: (suppliers) => {
        this.supplierOptions = [...(suppliers || [])].sort((a, b) =>
          a.name.localeCompare(b.name, 'vi'),
        );
        this.supplierOptionsLoaded = true;
        this.loadingSuppliers = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Không thể tải danh sách nhà cung cấp.');
        this.loadingSuppliers = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateForm(): void {
    if (!this.auth.hasRole('USER', 'ADMIN')) return;
    this.isCreateFormOpen = true;
  }

  closeCreateForm(): void {
    if (this.creating) return;
    this.isCreateFormOpen = false;
  }

  onCreateFormStateChange(creating: boolean): void {
    this.creating = creating;
  }

  onOrderCreated(): void {
    this.isCreateFormOpen = false;
    this.loadOrders();
  }

  async updateStatus(order: PurchaseOrder, status: PurchaseOrderStatus): Promise<void> {
    if (!this.auth.hasRole('ADMIN', 'CHECKER')) return;
    if (this.updatingOrderId) return;
    if (status === 'CANCELLED' && !(await this.dialogs.confirm({
      title: 'Hủy đơn đặt hàng',
      message: `Hủy đơn đặt hàng ${order.orderCode}?`,
      confirmText: 'Hủy đơn hàng',
      tone: 'danger',
    }))) return;
    if (status === 'RECEIVED' && !(await this.dialogs.confirm({
      title: 'Xác nhận đã nhận hàng',
      message: `Xác nhận đơn ${order.orderCode} đã được giao và nhận đủ hàng?`,
      confirmText: 'Xác nhận nhận hàng',
    }))) return;
    this.updatingOrderId = order.id;
    this.purchaseOrderService.updateStatus(order.id, status).subscribe({
      next: () => {
        this.toast.success(`Đã cập nhật trạng thái đơn ${order.orderCode}.`);
        this.updatingOrderId = null;
        this.loadOrders();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Không thể cập nhật trạng thái đơn đặt hàng.');
        this.updatingOrderId = null;
        this.cdr.detectChanges();
      },
    });
  }

  openView(order: PurchaseOrder): void {
    if (this.purchaseOrderView) {
      this.purchaseOrderView.open(order);
    }
    this.cdr.detectChanges();
  }

  onOrderReviewed(updatedOrder: PurchaseOrder): void {
    void updatedOrder;
    this.loadOrders();
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

  trackByOrderId(_: number, order: PurchaseOrder): string {
    return order.id;
  }
}
