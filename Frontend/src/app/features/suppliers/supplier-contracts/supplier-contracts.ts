import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../auth/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { SupplierDocumentService } from '../services/supplier-document.service';
import { SupplierContractService } from '../services/supplier-contract.service';
import { SupplierOption } from '../models/supplier-document';
import { contractDatesValidator, trimmed } from '../shared/supplier-form.validators';
import {
  EditableSupplierContractStatus,
  SupplierContract,
  SupplierContractRequest,
  SupplierContractStatus,
} from '../models/supplier-contract';

@Component({
  selector: 'app-supplier-contracts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-contracts.html',
  styleUrl: './supplier-contracts.scss',
})
export class SupplierContracts implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly contractService = inject(SupplierContractService);
  private readonly supplierDocumentService = inject(SupplierDocumentService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly isAdmin = this.auth.hasRole('ADMIN');
  readonly statuses: { value: SupplierContractStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Bản nháp' },
    { value: 'ACTIVE', label: 'Đang hiệu lực' },
    { value: 'EXPIRED', label: 'Hết hạn' },
    { value: 'TERMINATED', label: 'Đã chấm dứt' },
  ];
  readonly editableStatuses: { value: EditableSupplierContractStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Bản nháp' },
    { value: 'ACTIVE', label: 'Đang hiệu lực' },
    { value: 'TERMINATED', label: 'Đã chấm dứt' },
  ];

  readonly filterForm = this.fb.nonNullable.group({
    query: '',
    supplierId: '',
    status: '' as '' | SupplierContractStatus,
  });

  readonly contractForm = this.fb.nonNullable.group(
    {
      title: [
        '',
        [
          trimmed(Validators.required),
          trimmed(Validators.minLength(2)),
          trimmed(Validators.maxLength(255)),
        ],
      ],
      supplierId: ['', Validators.required],
      signedDate: '',
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      amount: [
        0,
        [
          Validators.required,
          Validators.min(0),
          Validators.max(9999999999999.99),
          Validators.pattern(/^\d+(?:\.\d{1,2})?$/),
        ],
      ],
      currency: [
        'VND',
        [trimmed(Validators.required), trimmed(Validators.pattern(/^[A-Za-z]{3}$/))],
      ],
      status: ['DRAFT' as EditableSupplierContractStatus, Validators.required],
      notes: ['', trimmed(Validators.maxLength(5000))],
    },
    { validators: contractDatesValidator },
  );

  contracts: SupplierContract[] = [];
  suppliers: SupplierOption[] = [];
  loading = false;
  loadingSuppliers = false;
  saving = false;
  deletingId = '';
  downloadingId = '';
  isFormOpen = false;
  editingContract: SupplierContract | null = null;
  selectedFile: File | null = null;
  fileInputError = '';
  submitAttempted = false;

  ngOnInit(): void {
    this.loadContracts();
    this.loadSuppliers();
  }

  get filteredContracts(): SupplierContract[] {
    const { query, supplierId, status } = this.filterForm.getRawValue();
    const normalizedQuery = query.trim().toLocaleLowerCase('vi');
    return this.contracts.filter(
      (contract) =>
        (!supplierId || contract.supplierId === supplierId) &&
        (!status || contract.status === status) &&
        (!normalizedQuery ||
          contract.contractNumber.toLocaleLowerCase('vi').includes(normalizedQuery) ||
          contract.title.toLocaleLowerCase('vi').includes(normalizedQuery) ||
          contract.supplierName.toLocaleLowerCase('vi').includes(normalizedQuery)),
    );
  }

  get contractNumberPreview(): string {
    return `HĐMB-${new Date().getFullYear()}-####`;
  }

  fieldError(name: keyof typeof this.contractForm.controls): string {
    const field = this.contractForm.controls[name];
    if (!field.touched || !field.errors) return '';
    if (field.hasError('required')) return 'Vui lòng nhập hoặc chọn thông tin này.';
    if (field.hasError('minlength'))
      return `Tối thiểu ${field.errors['minlength'].requiredLength} ký tự.`;
    if (field.hasError('maxlength'))
      return `Tối đa ${field.errors['maxlength'].requiredLength} ký tự.`;
    if (field.hasError('min')) return 'Giá trị hợp đồng không được âm.';
    if (field.hasError('max')) return 'Giá trị hợp đồng vượt quá giới hạn cho phép.';
    if (name === 'amount') return 'Giá trị phải là số hợp lệ, tối đa 2 chữ số thập phân.';
    if (name === 'currency') return 'Mã tiền tệ gồm đúng 3 chữ cái (ví dụ: VND, USD).';
    return 'Thông tin không hợp lệ.';
  }

  normalizeCurrency(): void {
    const control = this.contractForm.controls.currency;
    control.setValue(control.value.trim().toUpperCase());
  }

  loadContracts(): void {
    this.loading = true;
    this.contractService.list().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.toast.error(error.error?.message || 'Không tải được danh sách hợp đồng.');
      },
    });
  }

  private loadSuppliers(): void {
    this.loadingSuppliers = true;
    this.supplierDocumentService.suppliers().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;
        this.loadingSuppliers = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.loadingSuppliers = false;
        this.toast.error(error.error?.message || 'Không tải được danh sách nhà cung cấp.');
      },
    });
  }

  openCreate(): void {
    this.editingContract = null;
    this.selectedFile = null;
    this.fileInputError = '';
    this.submitAttempted = false;
    this.contractForm.reset({
      title: '',
      supplierId: '',
      signedDate: '',
      startDate: '',
      endDate: '',
      amount: 0,
      currency: 'VND',
      status: 'DRAFT',
      notes: '',
    });
    this.isFormOpen = true;
  }

  openEdit(contract: SupplierContract): void {
    this.editingContract = contract;
    this.selectedFile = null;
    this.fileInputError = '';
    this.submitAttempted = false;
    this.contractForm.reset({
      title: contract.title,
      supplierId: contract.supplierId,
      signedDate: contract.signedDate || '',
      startDate: contract.startDate,
      endDate: contract.endDate,
      amount: contract.amount,
      currency: contract.currency,
      status: contract.status === 'EXPIRED' ? 'ACTIVE' : contract.status,
      notes: contract.notes || '',
    });
    this.isFormOpen = true;
  }

  closeForm(): void {
    if (this.saving) return;
    this.isFormOpen = false;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = null;
    this.fileInputError = '';
    if (!file) return;
    if (!/\.(pdf|docx?|xlsx?|png|jpe?g|gif|bmp|webp)$/i.test(file.name)) {
      this.fileInputError = 'Loại file không được hỗ trợ.';
    } else if (file.size === 0 || file.size > 10 * 1024 * 1024) {
      this.fileInputError = 'File phải có nội dung và không vượt quá 10 MB.';
    } else {
      this.selectedFile = file;
    }
    if (this.fileInputError) input.value = '';
  }

  save(): void {
    if (!this.isAdmin || this.saving) return;
    this.submitAttempted = true;
    this.contractForm.markAllAsTouched();
    this.normalizeCurrency();
    if (this.contractForm.invalid || this.fileInputError) {
      if (this.contractForm.hasError('contractDateRange')) {
        this.toast.warning('Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.');
      } else if (this.contractForm.hasError('contractSignedDate')) {
        this.toast.warning('Ngày ký không được sau ngày bắt đầu hợp đồng.');
      } else {
        this.toast.warning('Vui lòng kiểm tra và điền đầy đủ thông tin hợp đồng.');
      }
      return;
    }

    const value = this.contractForm.getRawValue();
    const request: SupplierContractRequest = {
      title: value.title.trim(),
      supplierId: value.supplierId,
      signedDate: value.signedDate || undefined,
      startDate: value.startDate,
      endDate: value.endDate,
      amount: value.amount,
      currency: value.currency.trim().toUpperCase(),
      status: value.status,
      notes: value.notes.trim() || undefined,
    };
    this.saving = true;
    const request$ = this.editingContract
      ? this.contractService.update(this.editingContract.id, request, this.selectedFile)
      : this.contractService.create(request, this.selectedFile);

    request$.subscribe({
      next: (savedContract) => {
        this.saving = false;
        this.isFormOpen = false;
        this.toast.success(
          this.editingContract
            ? 'Đã cập nhật hợp đồng.'
            : `Đã tạo hợp đồng ${savedContract.contractNumber}.`,
        );
        this.loadContracts();
      },
      error: (error) => {
        this.saving = false;
        this.toast.error(error.error?.message || 'Không lưu được hợp đồng.');
      },
    });
  }

  delete(contract: SupplierContract): void {
    if (!this.isAdmin || this.deletingId) return;
    if (!confirm(`Xóa hợp đồng ${contract.contractNumber}? File đính kèm cũng sẽ bị xóa.`)) return;
    this.deletingId = contract.id;
    this.contractService.delete(contract.id).subscribe({
      next: () => {
        this.deletingId = '';
        this.toast.success('Đã xóa hợp đồng.');
        this.loadContracts();
      },
      error: (error) => {
        this.deletingId = '';
        this.toast.error(error.error?.message || 'Không xóa được hợp đồng.');
      },
    });
  }

  download(contract: SupplierContract): void {
    if (this.downloadingId || !contract.attachmentName) return;
    this.downloadingId = contract.id;
    this.contractService.download(contract.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = contract.attachmentName || contract.contractNumber;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.downloadingId = '';
      },
      error: (error) => {
        this.downloadingId = '';
        this.toast.error(error.error?.message || 'Không tải được file hợp đồng.');
      },
    });
  }

  statusLabel(status: SupplierContractStatus): string {
    return this.statuses.find((item) => item.value === status)?.label ?? status;
  }

  statusClass(status: SupplierContractStatus): string {
    return `status-${status.toLowerCase()}`;
  }
}
