import { CommonModule } from '@angular/common';
import { Component, ChangeDetectorRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../auth/services/auth.service';
import { ProcurementService } from '../../procurement/service/procurement.service';
import { Supplier } from '../../suppliers/models/supplier';
import { SupplierService } from '../../suppliers/services/supplier.service';
import { PurchaseOrderView } from '../purchase-order-view/purchase-order-view';
import {
  ApprovedTicket,
  CreatePurchaseOrderRequest,
  PurchaseOrder,
  PurchaseOrderStatus,
} from '../models/purchase-order';
import { PurchaseOrderService } from '../services/purchase-order.service';

@Component({
  selector: 'app-purchase-order-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PurchaseOrderView],
  templateUrl: './purchase-order-list.html',
  styleUrl: './purchase-order-list.scss',
})
export class PurchaseOrderList implements OnInit {
  private readonly toast = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly procurementService = inject(ProcurementService);
  private readonly supplierService = inject(SupplierService);
  private readonly fb = inject(FormBuilder);

  @ViewChild('poView') purchaseOrderView?: PurchaseOrderView;

  readonly filterForm = this.fb.nonNullable.group({
    searchQuery: '',
    selectedStatus: '' as '' | PurchaseOrderStatus,
  });
  readonly createForm = this.fb.nonNullable.group({
    ticketId: ['', Validators.required],
    supplierId: ['', Validators.required],
    expectedDeliveryDate: '',
    notes: ['', Validators.maxLength(1000)],
    procurementItemIds: this.fb.nonNullable.control<string[]>(
      [],
      [Validators.required, Validators.minLength(1)],
    ),
  });

  orders: PurchaseOrder[] = [];
  approvedTickets: ApprovedTicket[] = [];
  activeSuppliers: Supplier[] = [];
  loading = false;
  loadingSources = false;
  creating = false;
  updatingOrderId: string | null = null;
  isCreateFormOpen = false;

  ngOnInit(): void {
    this.loadOrders();
    if (this.auth.hasRole('USER', 'ADMIN')) {
      this.loadActiveSuppliers();
      this.loadApprovedTicketPage(0, []);
    }
  }

  get filteredOrders(): PurchaseOrder[] {
    const query = this.filterForm.controls.searchQuery.value.trim().toLowerCase();
    const selectedStatus = this.filterForm.controls.selectedStatus.value;
    const isStaffOrChecker = this.auth.hasRole('ADMIN', 'CHECKER');
    return this.orders.filter((order) => {
      if (!isStaffOrChecker && order.status !== 'SENT' && order.status !== 'RECEIVED') {
        return false;
      }
      return (
        (!selectedStatus || order.status === selectedStatus) &&
        (!query ||
          order.orderCode.toLowerCase().includes(query) ||
          order.ticketCode.toLowerCase().includes(query) ||
          order.ticketTitle.toLowerCase().includes(query) ||
          order.supplierName.toLowerCase().includes(query))
      );
    });
  }

  get selectedTicket(): ApprovedTicket | undefined {
    const ticketId = this.createForm.controls.ticketId.value;
    return this.approvedTickets.find((ticket) => ticket.id === ticketId);
  }

  get availableItems() {
    const ticket = this.selectedTicket;
    if (!ticket) return [];
    const allocatedIds = new Set(
      this.orders
        .filter((order) => order.status !== 'CANCELLED' && order.status !== 'REJECTED')
        .flatMap((order) => order.items.map((item) => item.procurementItemId)),
    );
    return ticket.items.filter((item) => item.id && !allocatedIds.has(item.id));
  }

  loadOrders(): void {
    this.loading = true;
    this.purchaseOrderService.getPurchaseOrders().subscribe({
      next: (response) => {
        this.orders = response.data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Không thể tải danh sách đơn đặt hàng.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadActiveSuppliers(): void {
    this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.activeSuppliers = (suppliers || []).filter((supplier) => supplier.status === 'ACTIVE');
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Không thể tải danh sách nhà cung cấp.');
      },
    });
  }

  private loadApprovedTicketPage(page: number, loaded: ApprovedTicket[]): void {
    this.loadingSources = true;
    this.procurementService.getTicketPage('APPROVED', page, 100).subscribe({
      next: (response) => {
        const result = [...loaded, ...(response.data?.content || [])];
        if (page + 1 < (response.data?.totalPages || 0)) {
          this.loadApprovedTicketPage(page + 1, result);
          return;
        }
        this.approvedTickets = result;
        this.loadingSources = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Không thể tải các phiếu đã được duyệt.');
        this.loadingSources = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateForm(): void {
    if (!this.auth.hasRole('USER', 'ADMIN')) return;
    this.createForm.reset({
      ticketId: '',
      supplierId: '',
      expectedDeliveryDate: '',
      notes: '',
      procurementItemIds: [],
    });
    this.isCreateFormOpen = true;
  }

  closeCreateForm(): void {
    if (this.creating) return;
    this.isCreateFormOpen = false;
  }

  onTicketSelected(): void {
    const itemIds = this.availableItems
      .map((item) => item.id)
      .filter((id): id is string => Boolean(id));
    this.createForm.controls.procurementItemIds.setValue(itemIds);
  }

  toggleItem(itemId: string, checked: boolean): void {
    const itemIds = new Set(this.createForm.controls.procurementItemIds.value);
    if (checked) itemIds.add(itemId);
    else itemIds.delete(itemId);
    this.createForm.controls.procurementItemIds.setValue([...itemIds]);
  }

  createOrder(): void {
    if (!this.auth.hasRole('USER', 'ADMIN')) return;
    if (this.creating) return;
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.toast.warning('Chọn phiếu, nhà cung cấp và ít nhất một hạng mục để tạo đơn.');
      return;
    }
    const formValue = this.createForm.getRawValue();
    const request: CreatePurchaseOrderRequest = {
      ticketId: formValue.ticketId,
      supplierId: formValue.supplierId,
      procurementItemIds: formValue.procurementItemIds,
      expectedDeliveryDate: formValue.expectedDeliveryDate || undefined,
      notes: formValue.notes.trim() || undefined,
    };
    this.creating = true;
    this.purchaseOrderService.createPurchaseOrder(request).subscribe({
      next: (response) => {
        this.toast.success(`Đã tạo đơn đặt hàng ${response.data.orderCode}.`);
        this.isCreateFormOpen = false;
        this.creating = false;
        this.loadOrders();
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Tạo đơn đặt hàng thất bại.');
        this.creating = false;
        this.cdr.detectChanges();
      },
    });
  }

  updateStatus(order: PurchaseOrder, status: PurchaseOrderStatus): void {
    if (!this.auth.hasRole('ADMIN', 'CHECKER')) return;
    if (this.updatingOrderId) return;
    if (status === 'CANCELLED' && !confirm(`Hủy đơn đặt hàng ${order.orderCode}?`)) return;
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
    this.orders = this.orders.map((existing) =>
      existing.id === updatedOrder.id ? updatedOrder : existing,
    );
    this.cdr.detectChanges();
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
