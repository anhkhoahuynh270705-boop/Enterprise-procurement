import { SensitiveInput } from '../../../shared/sensitive/sensitive-input/sensitive-input';
import { ToastService } from '../../../core/services/toast.service';
import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SupplierService } from '../services/supplier.service';
import { CreateSupplierRequest } from '../models/supplier';
import {
  bankAccountValidator,
  bankNameValidator,
  bankDetailsValidator,
  contactRequiredValidator,
  normalizeSupplierCompanyName,
  phoneValidator,
  supplierCompanyNameValidator,
  supplierNameValidator,
  trimmed
} from '../shared/supplier-form.validators';

@Component({
  imports: [SensitiveInput, CommonModule, ReactiveFormsModule, RouterLink],
  selector: 'app-supplier-create',
  styleUrl: './supplier-create.scss',
  templateUrl: './supplier-create.html',
})
export class SupplierCreate implements OnInit {
  private readonly toast = inject(ToastService);

  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = false;
  openedFromProcurementWarning = false;

  ngOnInit(): void {
    const supplierName = this.route.snapshot.queryParamMap.get('name');
    const ticketId = this.route.snapshot.queryParamMap.get('ticketId');
    if (supplierName) {
      this.Createform.controls.name.setValue(normalizeSupplierCompanyName(supplierName));
      this.openedFromProcurementWarning = Boolean(ticketId);
    }
  }
  
  readonly Createform = this.fb.nonNullable.group({
    name: ['', [supplierCompanyNameValidator(true), trimmed(Validators.minLength(2)), trimmed(Validators.maxLength(200))]],
    contactPerson: ['', [supplierNameValidator(), trimmed(Validators.maxLength(200))]],
    email: ['', [trimmed(Validators.email), trimmed(Validators.maxLength(254))]],
    phone: ['', phoneValidator()],
    address: ['', trimmed(Validators.maxLength(255))],
    city: ['', [supplierNameValidator(), trimmed(Validators.maxLength(255))]],
    country: ['', [supplierNameValidator(), trimmed(Validators.maxLength(255))]],
    taxCode: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$|^[0-9]{13}$/)]],
    bankAccount: ['', bankAccountValidator()],
    bankName: ['', [bankNameValidator(), trimmed(Validators.maxLength(255))]],
    notes: ['', trimmed(Validators.maxLength(2000))]
  }, { validators: [contactRequiredValidator, bankDetailsValidator] });

  fieldError(name: keyof typeof this.Createform.controls): string {
    const field = this.Createform.controls[name];
    if (!field.touched || !field.errors) return '';
    if (field.hasError('required')) 
      return 'Vui lòng nhập thông tin này.';
    if (field.hasError('minlength')) 
      return `Tối thiểu ${field.errors['minlength'].requiredLength} ký tự.`;
    if (field.hasError('maxlength')) 
      return `Tối đa ${field.errors['maxlength'].requiredLength} ký tự.`;
    if (field.hasError('companyName'))
      return 'Tên nhà cung cấp chỉ được chứa chữ, dấu ngoặc đơn và khoảng trắng đơn giữa các từ.';
    if (field.hasError('name')) 
      return 'Chỉ nhập chữ và một khoảng trắng giữa các từ, không nhập số.';
    if (field.hasError('bankName'))
      return 'Tên ngân hàng chỉ nhập chữ; tên viết tắt có thể đặt trong ngoặc, ví dụ: Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank).';
    if (field.hasError('email')) 
      return 'Email không hợp lệ.';
    if (field.hasError('bankAccount'))
       return 'Số tài khoản gồm 6–50 chữ số, không có chữ hoặc khoảng trắng.';
    if (name === 'phone')
      return 'Số điện thoại gồm 8–15 chữ số; có thể chèn khoảng trắng.';
    return name === 'taxCode'
      ? 'Mã số thuế phải có 10 hoặc 13 chữ số.'
      : '';
  }

  onSubmit(): void {
    if (this.Createform.invalid) {
      this.Createform.markAllAsTouched();
      return;
    }

    this.loading = true;
    
    const formVal = this.Createform.getRawValue();
    const createData: CreateSupplierRequest = {
      name: normalizeSupplierCompanyName(formVal.name),
      contactPerson: formVal.contactPerson.trim() || undefined,
      email: formVal.email.trim() || undefined,
      phone: formVal.phone.trim() || undefined,
      address: formVal.address.trim() || undefined,
      city: formVal.city.trim() || undefined,
      country: formVal.country.trim() || undefined,
      taxCode: formVal.taxCode.trim() || undefined,
      bankAccount: formVal.bankAccount.trim() || undefined,
      bankName: formVal.bankName.trim() || undefined,
      notes: formVal.notes.trim() || undefined
    };

    this.supplierService.createSupplier(createData).subscribe({
      next: () => {
        this.loading = false;
        this.toast.success('Tạo nhà cung cấp thành công');
        const ticketId = this.route.snapshot.queryParamMap.get('ticketId');
        void this.router.navigate(ticketId ? ['/procurement', ticketId] : ['/suppliers']);
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.message || 'Tạo nhà cung cấp thất bại.');
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }
}
