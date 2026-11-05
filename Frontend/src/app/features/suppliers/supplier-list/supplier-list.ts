import { SensitiveValue } from '../../../shared/sensitive/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmationDialogService } from '../../../core/services/confirmation-dialog.service';
import { Component, OnInit, ChangeDetectorRef, DestroyRef, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { SupplierService } from '../services/supplier.service';
import { Supplier, SupplierImportResult, SupplierStatus } from '../models/supplier';
import { SupplierView } from '../supplier-view/supplier-view';
import { SupplierProposals } from '../supplier-proposals/supplier-proposals';
import { SupplierProposalService } from '../services/supplier-proposal.service';

@Component({
  imports: [
    SensitiveValue,
    CommonModule,
    ReactiveFormsModule,
    SupplierView,
    SupplierProposals,
    RouterLink,
  ],
  selector: 'app-supplier-list',
  styleUrl: './supplier-list.scss',
  templateUrl: './supplier-list.html',
})
export class SupplierList implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  private readonly dialogs = inject(ConfirmationDialogService);

  showProposals = false;
  @ViewChild('proposalDialog')
  proposalDialog!: ElementRef<HTMLDialogElement>;

  @ViewChild(SupplierProposals)
  proposalPanel?: SupplierProposals;

  pendingProposalCount: number | null = null;
  private readonly proposalService = inject(SupplierProposalService);
  supplierOptions: Supplier[] = [];
  cityOptions: { value: string; label: string }[] = [];
  pagedSuppliers: Supplier[] = [];

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  private readonly fb = inject(FormBuilder);
  readonly filtersForm = this.fb.nonNullable.group({
    searchKeyword: '',
    selectedStatus: 'ALL',
    supplierId: '',
    supplierCode: '',
    contactPerson: '',
    taxCode: '',
    email: '',
    phone: '',
    city: '',
  });
  isAdvancedSearchOpen = false;
  loading = false;
  downloadingTemplate = false;
  isImporting = false;
  isImportModalOpen = false;
  importFileName = '';
  importError = '';
  importResult: SupplierImportResult | null = null;
  deletingId: string | null = null;
  visibleIds = new Set<string | number>();

  get allPageIdsVisible(): boolean {
    return this.pagedSuppliers.length > 0 &&
      this.pagedSuppliers.every((supplier) => this.visibleIds.has(supplier.id));
  }

  @ViewChild(SupplierView)
  viewSupplierRef!: SupplierView;

  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.filtersForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.applyFilter();
        this.cdr.markForCheck();
      });
    this.loadSuppliers();
    this.loadPendingCount();
  }

  loadPendingCount(): void {
    this.proposalService.pendingCount().subscribe({
      next: (response) => {
        this.pendingProposalCount = response.count;
        this.cdr.markForCheck();
      },
      error: () => {
        this.pendingProposalCount = null;
        this.cdr.markForCheck();
      },
    });
  }

  openProposals(): void {
    this.showProposals = true;
    this.cdr.detectChanges();
    this.proposalDialog.nativeElement.showModal();
  }

  closeProposals(event?: Event): void {
    event?.preventDefault();
    if (this.proposalPanel?.reviewing) return;
    this.proposalDialog.nativeElement.close();
    this.showProposals = false;
  }

  onProposalReviewed(): void {
    this.loadSuppliers();
    this.loadPendingCount();
  }

  loadSuppliers(): void {
    this.loading = true;
    this.supplierService.getSuppliers().subscribe({
      next: (suppliers) => {
        const allSuppliers = suppliers || [];
        this.supplierOptions = [...allSuppliers].sort((a, b) =>
          a.name.localeCompare(b.name, 'vi'),
        );
        const cities = new Map<string, string>();
        allSuppliers.forEach((supplier) => {
          const city = supplier.city?.trim();
          const normalizedCity = this.normalizeSearch(city);
          if (city && normalizedCity && !cities.has(normalizedCity)) {
            cities.set(normalizedCity, city);
          }
        });
        this.cityOptions = [...cities].map(([value, label]) => ({ value, label }))
          .sort((a, b) => a.label.localeCompare(b.label, 'vi'));
        this.loadSupplierPage();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi khi tải danh sách nhà cung cấp:', err);
        this.loading = false;
        this.loadSupplierPage();
      },
    });
  }

  private loadSupplierPage(): void {
    const filters = this.filtersForm.getRawValue();
    this.loading = true;

    this.supplierService.searchPage({
      keyword: filters.searchKeyword || undefined,
      status: filters.selectedStatus === 'ALL'
        ? undefined
        : filters.selectedStatus as SupplierStatus,
      supplierId: filters.supplierId || undefined,
      supplierCode: filters.supplierCode || undefined,
      contactPerson: filters.contactPerson || undefined,
      taxCode: filters.taxCode || undefined,
      email: filters.email || undefined,
      phone: filters.phone || undefined,
      city: filters.city || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: ({ data }) => {
        const lastPage = Math.max(data.totalPages - 1, 0);
        if (this.currentPage > lastPage) {
          this.currentPage = lastPage;
          this.loadSupplierPage();
          return;
        }

        this.pagedSuppliers = data.content || [];
        this.totalElements = data.totalElements || 0;
        this.totalPages = data.totalPages || 0;
        this.visibleIds.clear();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Lỗi khi tải trang nhà cung cấp:', error);
        this.pagedSuppliers = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.loading = false;
        this.toast.error(error.error?.message || 'Không thể tải danh sách nhà cung cấp.');
        this.cdr.detectChanges();
      },
    });
  }

  toggleIdVisibility(id: string | number, event: MouseEvent): void {
    if (event) event.stopPropagation();
    if (this.visibleIds.has(id)) {
      this.visibleIds.delete(id);
    } else {
      this.visibleIds.add(id);
    }
    this.cdr.detectChanges();
  }
  isIdVisible(id: string | number): boolean {
    return this.visibleIds.has(id);
  }

  toggleAllIds(): void {
    const pageIds = this.pagedSuppliers.map((supplier) => supplier.id);
    if (pageIds.length > 0 && pageIds.every((id) => this.visibleIds.has(id))) {
      this.visibleIds.clear();
    } else {
      pageIds.forEach((id) => this.visibleIds.add(id));
    }
    this.cdr.detectChanges();
  }

  // switchTab(tab: 'ALL' | 'ACTIVE'): void {
  //   if (tab === 'ACTIVE') {
  //     this.selectedStatus = 'ACTIVE';
  //   } else {
  //     this.selectedStatus = 'ALL';
  //   }
  //   this.applyFilter();
  // }

  toggleAdvancedSearch(): void {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  resetFilters(): void {
    this.filtersForm.reset({
      searchKeyword: '',
      selectedStatus: 'ALL',
      supplierId: '',
      supplierCode: '',
      contactPerson: '',
      taxCode: '',
      email: '',
      phone: '',
      city: '',
    }, { emitEvent: false });
    this.currentPage = 0;
    this.loadSupplierPage();
  }

  applyFilter(): void {
    this.currentPage = 0;
    this.loadSupplierPage();
  }

  private normalizeSearch(value: string | null | undefined): string {
    return (value || '').trim().toLocaleLowerCase('vi-VN');
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadSupplierPage();
  }

  addSupplier(): void {
    this.router.navigate(['supplier/create']);
  }

  editSupplier(id: string): void {
    this.router.navigate(['supplier/edit', id]);
  }

  openViewSupplier(supplier: Supplier, event: MouseEvent): void {
    if (this.viewSupplierRef) {
      this.viewSupplierRef.open(supplier, event);
    }
  }

  async deleteSupplier(id: string): Promise<void> {
    if (this.deletingId) return;
    if (!(await this.dialogs.confirm({
      title: 'Xóa nhà cung cấp',
      message: 'Bạn có chắc chắn muốn xóa nhà cung cấp này không?',
      confirmText: 'Xóa nhà cung cấp',
      tone: 'danger',
    }))) return;

    this.deletingId = id;
    this.supplierService.deleteSupplier(id).subscribe({
      next: () => {
        this.toast.success('Xóa nhà cung cấp thành công');
        this.deletingId = null;
        this.loadSuppliers();
      },
      error: (err) => {
        console.error('Lỗi khi xóa nhà cung cấp:', err);
        this.toast.error('Không thể xóa nhà cung cấp. Có thể nhà cung cấp này đang được sử dụng.');
        this.deletingId = null;
        this.cdr.detectChanges();
      },
    });
  }

  exportExcel(): void {
    this.supplierService.exportAllSuppliers('EXCEL').subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute(
          'download',
          `Danh_sach_nha_cung_cap_${new Date().toISOString().slice(0, 10)}.xlsx`,
        );
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      },
      error: (err) => {
        console.error('Lỗi khi xuất file Excel:', err);
        this.toast.error('Lỗi khi tải file Excel từ máy chủ.');
      },
    });
  }

  downloadTemplate(): void {
    if (this.downloadingTemplate) return;
    this.downloadingTemplate = true;
    this.supplierService.downloadTemplate().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'Mau_nhap_nha_cung_cap.xlsx';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.downloadingTemplate = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Lỗi khi tải template nhà cung cấp:', err);
        this.toast.error(err?.error?.message || 'Không thể tải template nhà cung cấp.');
        this.downloadingTemplate = false;
        this.cdr.markForCheck();
      },
    });
  }

  exportSupplier(supplier: Supplier, format: 'PDF' | 'EXCEL'): void {
    this.supplierService.exportSupplier(supplier.id, format).subscribe({
      next: (blob) => {
        const extension = format === 'PDF' ? 'pdf' : 'xlsx';
        const code = (supplier.code || supplier.id).replace(/[<>:"/\\|?*]/g, '_');
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `NCC_${code}_${new Date().toISOString().slice(0, 10)}.${extension}`;
        document.body.appendChild(link);
        link.click();
        link.remove();

        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: (err) => {
        console.error('Lỗi khi xuất file PDF/Excel:', err);
      },
    });
  }

  /* Check valid file */
  importExcel(): void {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.xlsx,.xls';
    fileInput.onchange = (e: Event) => {
      const target = e.target as HTMLInputElement;
      const file = target.files?.[0];
      target.value = '';
      if (!file) return;
      if (!/\.(xlsx|xls)$/i.test(file.name) || file.size === 0) {
        this.toast.error(
          'File import không hợp lệ. Vui lòng chọn file Excel .xlsx hoặc .xls không rỗng.',
        );
        return;
      }
      this.isImportModalOpen = true;
      this.isImporting = true;
      this.importFileName = file.name;
      this.importError = '';
      this.importResult = null;
      this.supplierService.importSuppliers(file).subscribe({
        next: (response) => {
          this.importResult = response.data;
          this.isImporting = false;
          if (response.data?.successRows > 0) this.loadSuppliers();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.isImporting = false;
          this.importError =
            err?.error?.message || 'Import thất bại. Vui lòng kiểm tra định dạng file Excel.';
          this.cdr.markForCheck();
        },
      });
    };
    fileInput.click();
  }

  closeImportModal(): void {
    if (this.isImporting) return;
    this.isImportModalOpen = false;
    this.importFileName = '';
    this.importError = '';
    this.importResult = null;
  }

}
