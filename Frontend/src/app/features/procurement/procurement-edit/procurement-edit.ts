import { SensitiveValue } from '../../../shared/sensitive/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProcurementService } from '../service/procurement.service';
import { SupplierService } from '../../suppliers/services/supplier.service';
import { Supplier } from '../../suppliers/models/supplier';

const itemNamePattern = /^[\p{L}\p{M}\p{N}]+(?: +[\p{L}\p{M}\p{N}]+)*$/u;

@Component({
  selector: 'app-procurement-edit',
  standalone: true,
  imports: [SensitiveValue, CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './procurement-edit.html',
  styleUrls: ['./procurement-edit.scss'],
})
export class ProcurementEdit implements OnInit {
  private readonly toast = inject(ToastService);

  private fb = inject(FormBuilder);
  private procurementService = inject(ProcurementService);
  private supplierService = inject(SupplierService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  ticketForm!: FormGroup;
  isLoading = false;

  ticketId: string | null = null;
  editingTicketCode = '';
  suppliers: Supplier[] = [];
  unitOptions = [
    'Chiếc',
    'Cái',
    'Bộ',
    'Hộp',
    'Thùng',
    'Gói',
    'Kg',
    'Mét',
    'Cuộn',
    'Tấm',
    'Quyển',
    'Lít',
    'Chai',
    'Giờ',
    'Ngày',
    'Tháng',
    'Dịch vụ',
  ];

  ngOnInit(): void {
    this.initForm();
    this.loadSuppliers();
    this.ticketId = this.route.snapshot.paramMap.get('id');

    if (this.ticketId) {
      this.loadTicketForEdit(this.ticketId);
    }
  }

  private loadSuppliers(): void {
    this.supplierService.getSuppliers().subscribe({
      next: (list) => {
        this.suppliers = list.filter((s) => s.status === 'ACTIVE');
        this.cdr.markForCheck();
      },
      error: () => {
        this.suppliers = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadTicketForEdit(id: string): void {
    this.isLoading = true;
    this.procurementService.getTicketById(id).subscribe({
      next: (res) => {
        const ticket = res.data;
        if (ticket.status === 'APPROVED') {
          this.isLoading = false;
          this.toast.warning('Phiếu đã được duyệt, chỉ có thể xem chi tiết.');
          void this.router.navigate(['/procurement', ticket.id], { replaceUrl: true });
          return;
        }
        this.editingTicketCode = ticket.ticketCode;
        this.ticketForm.patchValue({
          title: ticket.title,
          department: ticket.department,
          priority: ticket.priority,
          currency: ticket.currency || 'VND',
          reason: ticket.reason || '',
        });

        this.items.clear();
        if (ticket.items && ticket.items.length > 0) {
          this.unitOptions = [
            ...new Set([
              ...this.unitOptions,
              ...ticket.items.map((item) => item.unit).filter((unit): unit is string => !!unit),
            ]),
          ];
          ticket.items.forEach((it) => {
            this.items.push(
              this.fb.group({
                itemCode: [it.itemCode || ''],
                itemName: [it.itemName, [Validators.required, Validators.pattern(itemNamePattern)]],
                category: [it.category || 'Hardware'],
                quantity: [it.quantity, [Validators.required, Validators.min(1)]],
                unit: [it.unit || 'Chiếc'],
                unitPrice: [it.unitPrice, [Validators.required, Validators.min(1)]],
                supplierName: [it.supplierName || '', [Validators.required]],
                notes: [it.notes || ''],
              }),
            );
          });
        } else {
          this.addItem();
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.toast.error(err.error?.message || 'Không thể tải thông tin phiếu để chỉnh sửa');
      },
    });
  }

  private initForm(): void {
    this.ticketForm = this.fb.nonNullable.group({
      title: ['', [Validators.required]],
      department: ['Phòng Kỹ thuật & R&D', Validators.required],
      priority: ['MEDIUM', Validators.required],
      currency: ['VND', Validators.required],
      reason: ['', [Validators.required]],
      items: this.fb.array([]),
    });
  }

  get items(): FormArray {
    return this.ticketForm.get('items') as FormArray;
  }

  createItemRow(): FormGroup {
    return this.fb.group({
      itemCode: [''],
      itemName: ['', [Validators.required, Validators.pattern(itemNamePattern)]],
      category: ['Hardware'],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unit: ['Chiếc', [Validators.required]],
      unitPrice: [0, [Validators.required, Validators.min(1)]],
      supplierName: ['', Validators.required],
      notes: [''],
    });
  }

  addItem(): void {
    this.items.push(this.createItemRow());
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    } else {
      this.toast.warning('Đề nghị mua sắm phải có ít nhất 1 mặt hàng.');
    }
  }

  getItemTotal(index: number): number {
    const row = this.items.at(index);
    const qty = Number(row.get('quantity')?.value) || 0;
    const price = Number(row.get('unitPrice')?.value) || 0;
    return qty * price;
  }

  get grandTotal(): number {
    let sum = 0;
    for (let i = 0; i < this.items.length; i++) {
      sum += this.getItemTotal(i);
    }
    return sum;
  }

  onSubmit(): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      this.toast.error('Vui lòng kiểm tra lại các trường thông tin bắt buộc (*).');
      return;
    }

    if (!this.ticketId) return;

    this.isLoading = true;

    const formValue = this.ticketForm.value;
    const items = formValue.items.map((it: any) => ({
      ...it,
      quantity: Number(it.quantity),
      unitPrice: Number(it.unitPrice),
    }));

    const updateData = {
      title: formValue.title,
      department: formValue.department,
      priority: formValue.priority,
      currency: formValue.currency,
      reason: formValue.reason,
      items: items,
    };

    this.procurementService.updateTicket(this.ticketId, updateData).subscribe({
      next: () => {
        this.isLoading = false;
        this.toast.success(`Cập nhật phiếu •••••••• thành công!`);
        this.router.navigate(['/procurement']);
      },
      error: (err) => {
        this.isLoading = false;
        this.toast.error(err.error?.message || 'Cập nhật phiếu thất bại');
      },
    });
  }
}
