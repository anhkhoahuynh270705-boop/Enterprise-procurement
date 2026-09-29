import { SensitiveInput } from '../../../shared/sensitive-value/sensitive-input';
import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SupplierService } from '../services/supplier.service';
import { SupplierStatus, UpdateSupplierRequest } from '../models/supplier';

@Component({
  imports: [SensitiveInput, SensitiveValue, CommonModule, ReactiveFormsModule, RouterLink],
  selector: 'app-supplier-edit',
  styleUrl: './supplier-edit.scss',
  templateUrl: './supplier-edit.html',
})
export class SupplierEdit implements OnInit {
  private readonly toast = inject(ToastService);

  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = false;
  supplierId!: string;
  supplierCode: string = '';

  editform = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    contactPerson: [''],
    email: ['', [Validators.email]],
    phone: [''],
    address: [''],
    city: [''],
    country: [''],
    taxCode: [''],
    bankAccount: [''],
    bankName: [''],
    notes: [''],
    status: ['ACTIVE' as SupplierStatus, [Validators.required]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/suppliers']);
      return;
    }
    this.supplierId = id;
    this.loadSupplier(id);
  }

  loadSupplier(id: string): void {
    this.loading = true;
    this.supplierService.getSupplierById(id).subscribe({
      next: (supplier) => {
        this.supplierCode = supplier.code;
        this.editform.patchValue({
          name: supplier.name,
          contactPerson: supplier.contactPerson || '',
          email: supplier.email || '',
          phone: supplier.phone || '',
          address: supplier.address || '',
          city: supplier.city || '',
          country: supplier.country || '',
          taxCode: supplier.taxCode || '',
          bankAccount: supplier.bankAccount || '',
          bankName: supplier.bankName || '',
          notes: supplier.notes || '',
          status: supplier.status || 'ACTIVE',
        });
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.toast.error('Không thể tải thông tin nhà cung cấp.');
        console.error('Lỗi khi tải nhà cung cấp:', err);
        this.cdr.detectChanges();
      },
    });
  }

  onUpdate(): void {
    if (this.loading) return;

    if (this.editform.invalid) {
      this.editform.markAllAsTouched();
      return;
    }
    const formVal = this.editform.getRawValue();
    const optional = (text: string) => text.trim() || undefined;

    const updateData: UpdateSupplierRequest = {
      name: formVal.name.trim(),
      contactPerson: optional(formVal.contactPerson),
      email: optional(formVal.email),
      phone: optional(formVal.phone),
      address: optional(formVal.address),
      city: optional(formVal.city),
      country: optional(formVal.country),
      taxCode: optional(formVal.taxCode),
      bankAccount: optional(formVal.bankAccount),
      bankName: optional(formVal.bankName),
      notes: optional(formVal.notes),
      status: formVal.status,
    };

    this.loading = true;

    this.supplierService.updateSupplier(this.supplierId, updateData).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/suppliers']);
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.message || 'Cập nhật nhà cung cấp thất bại.');
        console.error(err);
        this.cdr.detectChanges();
      },
    });
  }
}
