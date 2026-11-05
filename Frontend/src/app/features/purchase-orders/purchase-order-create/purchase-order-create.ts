import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmationDialogService } from '../../../core/services/confirmation-dialog.service';
import { AuthService } from '../../auth/services/auth.service';
import { ProcurementItem } from '../../procurement/model/procurement';
import { ProcurementService } from '../../procurement/service/procurement.service';
import { Supplier } from '../../suppliers/models/supplier';
import { SupplierService } from '../../suppliers/services/supplier.service';
import { ApprovedTicket, CreatePurchaseOrderRequest } from '../models/purchase-order';
import { PurchaseOrderService } from '../services/purchase-order.service';

@Component({
  selector: 'app-purchase-order-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './purchase-order-create.html',
  styleUrl: './purchase-order-create.scss',
})
export class PurchaseOrderCreate implements OnInit {
  private readonly toast = inject(ToastService);
  private readonly dialogs = inject(ConfirmationDialogService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly procurementService = inject(ProcurementService);
  private readonly supplierService = inject(SupplierService);
  private readonly fb = inject(FormBuilder);

  @Output() readonly cancelled = new EventEmitter<void>();
  @Output() readonly created = new EventEmitter<void>();
  @Output() readonly creatingChange = new EventEmitter<boolean>();

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

  approvedTickets: ApprovedTicket[] = [];
  activeSuppliers: Supplier[] = [];
  loadingSources = false;
  loadingSuppliers = false;
  loadingAllocatedItems = false;
  creating = false;

  private allocatedItemIds = new Set<string>();
  private allocatedItemsSequence = 0;

  ngOnInit(): void {
    if (!this.auth.hasRole('USER', 'ADMIN')) return;
    this.loadApprovedTicketPage(0, []);
    this.loadActiveSuppliers();
  }

  get selectedTicket(): ApprovedTicket | undefined {
    const ticketId = this.createForm.controls.ticketId.value;
    return this.approvedTickets.find((ticket) => ticket.id === ticketId);
  }

  get availableItems(): ProcurementItem[] {
    const ticket = this.selectedTicket;
    if (!ticket) return [];
    return ticket.items.filter((item) => item.id && !this.allocatedItemIds.has(item.id));
  }

  close(): void {
    if (!this.creating) this.cancelled.emit();
  }

  onTicketSelected(): void {
    const sequence = ++this.allocatedItemsSequence;
    const ticketId = this.createForm.controls.ticketId.value;
    this.createForm.controls.procurementItemIds.setValue([]);
    this.allocatedItemIds = new Set<string>();
    if (!ticketId) return;

    this.loadingAllocatedItems = true;
    this.purchaseOrderService.getOrderedProcurementItemIds(ticketId).subscribe({
      next: (response) => {
        if (sequence !== this.allocatedItemsSequence) return;
        this.allocatedItemIds = new Set(response.data || []);
        const availableIds = this.availableItems
          .map((item) => item.id)
          .filter((id): id is string => Boolean(id));
        this.createForm.controls.procurementItemIds.setValue(availableIds);
        this.loadingAllocatedItems = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        if (sequence !== this.allocatedItemsSequence) return;
        this.toast.error(error.error?.message || 'Không thể kiểm tra hạng mục đã đặt.');
        this.loadingAllocatedItems = false;
        this.cdr.detectChanges();
      },
    });
  }

  toggleItem(itemId: string, checked: boolean): void {
    const itemIds = new Set(this.createForm.controls.procurementItemIds.value);
    if (checked) itemIds.add(itemId);
    else itemIds.delete(itemId);
    this.createForm.controls.procurementItemIds.setValue([...itemIds]);
  }

  async createOrder(): Promise<void> {
    if (!this.auth.hasRole('USER', 'ADMIN') || this.creating) return;
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.toast.warning('Chọn phiếu, nhà cung cấp và ít nhất một hạng mục để tạo đơn.');
      return;
    }

    const formValue = this.createForm.getRawValue();
    const ticket = this.approvedTickets.find((item) => item.id === formValue.ticketId);
    const supplier = this.activeSuppliers.find((item) => item.id === formValue.supplierId);
    if (!(await this.dialogs.confirm({
      title: 'Nộp đơn đặt hàng',
      message: `Tạo và gửi đơn đặt hàng cho ${supplier?.name ?? 'nhà cung cấp đã chọn'} từ phiếu ${ticket?.ticketCode ?? ''}?`,
      confirmText: 'Nộp duyệt',
    }))) return;

    const request: CreatePurchaseOrderRequest = {
      ticketId: formValue.ticketId,
      supplierId: formValue.supplierId,
      procurementItemIds: formValue.procurementItemIds,
      expectedDeliveryDate: formValue.expectedDeliveryDate || undefined,
      notes: formValue.notes.trim() || undefined,
    };

    this.setCreating(true);
    this.purchaseOrderService.createPurchaseOrder(request).subscribe({
      next: (response) => {
        this.toast.success(`Đã tạo đơn đặt hàng ${response.data.orderCode}.`);
        this.setCreating(false);
        this.cdr.detectChanges();
        this.created.emit();
      },
      error: (error) => {
        this.toast.error(error.error?.message || 'Tạo đơn đặt hàng thất bại.');
        this.setCreating(false);
        this.cdr.detectChanges();
      },
    });
  }

  private setCreating(value: boolean): void {
    this.creating = value;
    this.creatingChange.emit(value);
  }

  private loadActiveSuppliers(): void {
    this.loadingSuppliers = true;
    this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.activeSuppliers = (suppliers || []).filter((supplier) => supplier.status === 'ACTIVE');
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
}
