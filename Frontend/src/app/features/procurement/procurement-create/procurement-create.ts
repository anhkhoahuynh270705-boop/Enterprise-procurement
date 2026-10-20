import { SensitiveValue } from '../../../shared/sensitive/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CreateProcurementTicketRequest } from '../model/procurement';
import { ProcurementService } from '../service/procurement.service';
import { SupplierService } from '../../suppliers/services/supplier.service';
import { Supplier } from '../../suppliers/models/supplier';

const itemNamePattern = /^[\p{L}\p{M}\p{N}]+(?: +[\p{L}\p{M}\p{N}]+)*$/u;

@Component({
  selector: 'app-procurement-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './procurement-create.html',
  styleUrls: ['./procurement-create.scss'],
})
export class ProcurementCreate implements OnInit {
  private readonly toast = inject(ToastService);

  private fb = inject(FormBuilder);
  private procurementService = inject(ProcurementService);
  private supplierService = inject(SupplierService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  ticketForm!: FormGroup;
  isLoading = false;

  suppliers: Supplier[] = [];
  isLoadingSuppliers = true;
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
    this.addItem();
    this.addItem();
  }

  private loadSuppliers(): void {
    this.supplierService.getSuppliers().subscribe({
      next: (list) => {
        this.suppliers = list.filter((s) => s.status === 'ACTIVE');
        this.isLoadingSuppliers = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.suppliers = [];
        this.isLoadingSuppliers = false;
        this.toast.error('Không thể tải danh sách nhà cung cấp. Vui lòng tải lại trang.');
        this.cdr.detectChanges();
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

  onSubmit(submitImmediately: boolean): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      this.toast.error('Vui lòng kiểm tra lại các trường thông tin bắt buộc (*).');
      return;
    }

    this.isLoading = true;

    const formValue = this.ticketForm.value;
    const items = formValue.items.map((it: any) => ({
      ...it,
      quantity: Number(it.quantity),
      unitPrice: Number(it.unitPrice),
    }));

    const request: CreateProcurementTicketRequest = {
      title: formValue.title,
      department: formValue.department,
      priority: formValue.priority,
      currency: formValue.currency,
      reason: formValue.reason,
      submitImmediately: submitImmediately,
      items: items,
    };

    this.procurementService.createTicket(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.toast.success(
          submitImmediately
            ? 'Tạo và nộp phiếu lên Checker thành công!'
            : 'Lưu bản nháp phiếu thành công!',
        );
        this.router.navigate(['/procurement']);
      },
      error: (err) => {
        this.isLoading = false;
        this.toast.error(err.error?.message || 'Không thể tạo phiếu mua sắm');
      },
    });
  }
}
