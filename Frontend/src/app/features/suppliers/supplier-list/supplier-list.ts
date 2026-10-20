import { SensitiveValue } from '../../../shared/sensitive/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { Component, OnInit, ChangeDetectorRef, DestroyRef, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { SupplierService } from '../services/supplier.service';
import { Supplier, SupplierImportResult } from '../models/supplier';
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

  showProposals = false;
  @ViewChild('proposalDialog')
  proposalDialog!: ElementRef<HTMLDialogElement>;

  @ViewChild(SupplierProposals)
  proposalPanel?: SupplierProposals;

  pendingProposalCount: number | null = null;
  private readonly proposalService = inject(SupplierProposalService);
  suppliers: Supplier[] = [];
  supplierOptions: Supplier[] = [];
  cityOptions: { value: string; label: string }[] = [];
  filteredSuppliers: Supplier[] = [];
  pagedSuppliers: Supplier[] = [];

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;

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

  @ViewChild(SupplierView)
  viewSupplierRef!: SupplierView;

  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.filtersForm.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
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
        this.suppliers = suppliers || [];
        this.supplierOptions = [...this.suppliers].sort((a, b) =>
          a.name.localeCompare(b.name, 'vi'),
        );
        const cities = new Map<string, string>();
        this.suppliers.forEach((supplier) => {
          const city = supplier.city?.trim();
          const normalizedCity = this.normalizeSearch(city);
          if (city && normalizedCity && !cities.has(normalizedCity)) {
            cities.set(normalizedCity, city);
          }
        });
        this.cityOptions = [...cities].map(([value, label]) => ({ value, label }))
          .sort((a, b) => a.label.localeCompare(b.label, 'vi'));
        this.applyFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi khi tải danh sách nhà cung cấp:', err);
        this.loading = false;
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
    if (
      this.filteredSuppliers.length > 0 &&
      this.visibleIds.size === this.filteredSuppliers.length
    ) {
      this.visibleIds.clear();
    } else {
      this.filteredSuppliers.forEach((s) => this.visibleIds.add(s.id));
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
    });
    this.applyFilter();
  }

  applyFilter(): void {
    const filters = this.filtersForm.getRawValue();
    const keyword = this.normalizeSearch(filters.searchKeyword);
    this.filteredSuppliers = this.suppliers.filter((supplier) => {
      const searchableValues = [
        supplier.name,
        supplier.code,
        supplier.contactPerson,
        supplier.taxCode,
        supplier.email,
        supplier.phone,
        supplier.address,
        supplier.city,
        supplier.country,
      ].map((value) => this.normalizeSearch(value));

      return (
        (!keyword || searchableValues.some((value) => value.includes(keyword))) &&
        (filters.selectedStatus === 'ALL' || supplier.status === filters.selectedStatus) &&
        (!filters.supplierId || supplier.id === filters.supplierId) &&
        this.matchesField(supplier.code, filters.supplierCode) &&
        this.matchesField(supplier.contactPerson, filters.contactPerson) &&
        this.matchesField(supplier.taxCode, filters.taxCode) &&
        this.matchesField(supplier.email, filters.email) &&
        this.matchesField(supplier.phone, filters.phone) &&
        (!filters.city || this.normalizeSearch(supplier.city) === filters.city)
      );
    });
    this.currentPage = 0;
    this.updatePaging();
  }

  private normalizeSearch(value: string | null | undefined): string {
    return (value || '').trim().toLocaleLowerCase('vi-VN');
  }

  private matchesField(value: string | undefined, filter: string): boolean {
    const query = this.normalizeSearch(filter);
    return !query || this.normalizeSearch(value).includes(query);
  }

  updatePaging(): void {
    this.totalPages = Math.ceil(this.filteredSuppliers.length / this.pageSize);
    const start = this.currentPage * this.pageSize;
    this.pagedSuppliers = this.filteredSuppliers.slice(start, start + this.pageSize);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.updatePaging();
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

  deleteSupplier(id: string): void {
    if (this.deletingId) return;
    if (!confirm('Bạn có chắc chắn muốn xóa nhà cung cấp này không?')) return;

    this.deletingId = id;
    this.supplierService.deleteSupplier(id).subscribe({
      next: () => {
        this.suppliers = this.suppliers.filter((s) => s.id !== id);
        this.toast.success('Xóa nhà cung cấp thành công');
        this.applyFilter();
        this.deletingId = null;
        this.cdr.detectChanges();
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
