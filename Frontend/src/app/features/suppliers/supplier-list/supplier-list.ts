import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { Component, OnInit, ChangeDetectorRef, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SupplierService } from '../services/supplier.service';
import { Supplier } from '../models/supplier';
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
  private readonly toast = inject(ToastService);

  showProposals = false;
  @ViewChild('proposalDialog')
  proposalDialog!: ElementRef<HTMLDialogElement>;

  @ViewChild(SupplierProposals)
  proposalPanel?: SupplierProposals;

  pendingProposalCount: number | null = null;
  private readonly proposalService = inject(SupplierProposalService);
  suppliers: Supplier[] = [];
  filteredSuppliers: Supplier[] = [];
  private readonly fb = inject(FormBuilder);
  readonly filtersForm = this.fb.nonNullable.group({
    searchKeyword: '',
    selectedStatus: 'ALL',
  });
  loading = false;
  deletingId: string | null = null;
  visibleIds = new Set<string | number>();

  @ViewChild(SupplierView)
  viewSupplierRef!: SupplierView;

  private readonly supplierService = inject(SupplierService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
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
    this.supplierService.getSuppliers(this.filtersForm.controls.searchKeyword.value).subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers || [];
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

  onSearch(): void {
    this.applyFilter();
  }

  applyFilter(): void {
    const keyword = this.filtersForm.controls.searchKeyword.value.trim().toLowerCase();
    const selectedStatus = this.filtersForm.controls.selectedStatus.value;
    this.filteredSuppliers = this.suppliers.filter((supplier) => {
      const matchesKeyword =
        !keyword ||
        (supplier.name && supplier.name.toLowerCase().includes(keyword)) ||
        (supplier.code && supplier.code.toLowerCase().includes(keyword)) ||
        (supplier.contactPerson && supplier.contactPerson.toLowerCase().includes(keyword)) ||
        (supplier.taxCode && supplier.taxCode.toLowerCase().includes(keyword)) ||
        (supplier.email && supplier.email.toLowerCase().includes(keyword));

      const matchesStatus = selectedStatus === 'ALL' || supplier.status === selectedStatus;

      return matchesKeyword && matchesStatus;
    });
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
      if (target.files && target.files.length > 0) {
        const file = target.files[0];
        if (!/\.(xlsx|xls)$/i.test(file.name) || file.size === 0) {
          this.toast.error(
            'File import không hợp lệ. Vui lòng chọn file Excel .xlsx hoặc .xls không rỗng.',
          );
          return;
        }
        this.loading = true;
        this.supplierService.importSuppliers(file).subscribe({
          next: (res) => {
            this.loading = false;
            this.toast.success(res.message || 'Import danh sách nhà cung cấp thành công!');
            this.loadSuppliers();
          },
          error: (err) => {
            this.loading = false;
            console.error('Lỗi khi import file Excel:', err);
            this.toast.error(
              err?.error?.message || 'Import thất bại. Vui lòng kiểm tra định dạng file Excel.',
            );
          },
        });
      }
    };
    fileInput.click();
  }
}
