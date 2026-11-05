import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierDocumentService } from '../services/supplier-document.service';
import { SupplierDocument, SupplierOption, DocumentStatus } from '../models/supplier-document';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmationDialogService } from '../../../core/services/confirmation-dialog.service';

@Component({
  selector: 'app-supplier-documents',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-documents.html',
  styleUrl: './supplier-documents.scss',
})
export class SupplierDocuments implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly dialogs = inject(ConfirmationDialogService);
  private readonly service = inject(SupplierDocumentService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);
  readonly isAdmin = this.auth.hasRole('ADMIN');
  readonly isReviewer = this.auth.hasRole('ADMIN', 'CHECKER');
  readonly labels: Record<DocumentStatus, string> = {
    PENDING: 'Chờ duyệt',
    APPROVED: 'Chính thức',
    REJECTED: 'Từ chối',
  };
  readonly accept = '.pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.bmp,.webp';
  @ViewChild('fileInput')
  fileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('previewDialog')
  previewDialog?: ElementRef<HTMLDialogElement>;

  readonly uploadForm = this.fb.nonNullable.group({
    supplierId: ['', Validators.required],
  });

  readonly filterForm = this.fb.nonNullable.group({
    filterSupplier: '',
    filterStatus: '',
    searchQuery: '',
    submittedBy: '',
    reviewedBy: '',
    reviewComment: '',
    submittedFrom: '',
    submittedTo: '',
    reviewedFrom: '',
    reviewedTo: '',
  });

  readonly reviewForm = this.fb.nonNullable.group({
    comment: ['', [Validators.maxLength(2000)]],
  });

  documents: SupplierDocument[] = [];
  suppliers: SupplierOption[] = [];
  file: File | null = null;
  selected: SupplierDocument | null = null;
  error = '';
  success = '';
  loading = false;
  uploading = false;
  reviewing = false;
  downloadingId = '';
  viewingId = '';
  preview: SupplierDocument | null = null;
  previewUrl: SafeResourceUrl | null = null;
  showUploadForm = false;
  showAdvancedSearch = false;
  private objectUrl = '';
  currentPage = 0;
  readonly pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  private loadSequence = 0;

  ngOnInit() {
    this.filterForm.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadDocuments();
      });
    this.loadSuppliers();
    this.load();
  }

  ngOnDestroy() {
    this.releasePreview();
  }

  get filtered() {
    return this.documents;
  }

  toggleAdvancedSearch(): void {
    this.showAdvancedSearch = !this.showAdvancedSearch;
  }

  resetFilters(): void {
    this.filterForm.reset({
      filterSupplier: '',
      filterStatus: '',
      searchQuery: '',
      submittedBy: '',
      reviewedBy: '',
      reviewComment: '',
      submittedFrom: '',
      submittedTo: '',
      reviewedFrom: '',
      reviewedTo: '',
    });
  }

  toggleUploadForm(): void {
    if (this.uploading) return;
    if (this.showUploadForm) {
      this.file = null;
      this.uploadForm.reset({ supplierId: '' });
      if (this.fileInput) this.fileInput.nativeElement.value = '';
    }
    this.showUploadForm = !this.showUploadForm;
    this.error = '';
  }

  canReview(document: SupplierDocument) {
    return (
      document.status === 'PENDING' &&
      this.isReviewer &&
      (this.isAdmin || document.submittedBy !== this.auth.getCurrentUser()?.username)
    );
  }

  load() {
    this.loadDocuments();
  }

  private loadSuppliers(): void {
    this.service.suppliers().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;
        this.cdr.markForCheck();
      },
      error: (err) => this.fail(err, 'Không tải được danh sách nhà cung cấp.'),
    });
  }

  private loadDocuments(): void {
    const sequence = ++this.loadSequence;
    const filters = this.filterForm.getRawValue();
    this.loading = true;
    this.error = '';
    this.service.searchPage({
      supplierId: filters.filterSupplier || undefined,
      status: (filters.filterStatus || undefined) as DocumentStatus | undefined,
      keyword: filters.searchQuery.trim() || undefined,
      submittedBy: filters.submittedBy.trim() || undefined,
      reviewedBy: filters.reviewedBy.trim() || undefined,
      reviewComment: filters.reviewComment.trim() || undefined,
      submittedFrom: filters.submittedFrom || undefined,
      submittedTo: filters.submittedTo || undefined,
      reviewedFrom: filters.reviewedFrom || undefined,
      reviewedTo: filters.reviewedTo || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
      next: (result) => {
        if (sequence !== this.loadSequence) return;
        const lastPage = Math.max((result.totalPages || 0) - 1, 0);
        if (this.currentPage > lastPage) {
          this.currentPage = lastPage;
          this.loadDocuments();
          return;
        }
        this.documents = result.content || [];
        this.totalPages = result.totalPages || 0;
        this.totalElements = result.totalElements || 0;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (sequence !== this.loadSequence) return;
        this.documents = [];
        this.totalPages = 0;
        this.totalElements = 0;
        this.loading = false;
        this.fail(err, 'Không tải được tài liệu. Vui lòng thử lại.');
      },
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.loading) return;
    this.currentPage = page;
    this.loadDocuments();
  }

  selectFile(event: Event) {
    this.error = '';
    this.success = '';
    this.file = null;

    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (file.size === 0) this.error = 'File không được rỗng.';
    else if (file.size > 10 * 1024 * 1024) this.error = 'File tối đa 10 MB.';
    else if (!/\.(pdf|docx?|xlsx?|png|jpe?g|gif|bmp|webp)$/i.test(file.name))
      this.error = 'Chỉ nhận PDF, Word, Excel và ảnh PNG, JPG, GIF, BMP, WebP.';
    else this.file = file;
  }

  async upload(): Promise<void> {
    if (this.uploading) return;
    const supplierId = this.uploadForm.controls.supplierId.value;
    if (!supplierId || !this.file) {
      this.toast.error('Vui lòng chọn nhà cung cấp và file hợp lệ trước khi tải lên.');
      return;
    }
    if (!(await this.dialogs.confirm({
      title: this.isAdmin ? 'Tải tài liệu nhà cung cấp' : 'Nộp đề xuất tài liệu',
      message: this.isAdmin
        ? `Xác nhận tải tài liệu "${this.file.name}" lên hồ sơ nhà cung cấp?`
        : `Xác nhận gửi tài liệu "${this.file.name}" để chờ duyệt?`,
      confirmText: this.isAdmin ? 'Tải lên' : 'Nộp đề xuất',
    }))) return;
    this.uploading = true;
    this.error = '';
    this.success = '';
    this.service.upload(supplierId, this.file).subscribe({
      next: (document) => {
        this.uploading = false;
        this.showUploadForm = false;
        this.file = null;
        this.uploadForm.reset({ supplierId: '' });
        if (this.fileInput) this.fileInput.nativeElement.value = '';
        this.success = this.isAdmin
          ? 'Đã tải lên tài liệu chính thức.'
          : 'Đã gửi đề xuất tài liệu, đang chờ duyệt.';
        this.filterForm.patchValue(
          { filterSupplier: document.supplierId, filterStatus: '' },
          { emitEvent: false },
        );
        this.currentPage = 0;
        this.load();
      },
      error: (err) => {
        this.uploading = false;
        this.toast.error('Upload thất bại. Vui lòng thử lại.');
      },
    });
  }

  download(document: SupplierDocument) {
    if (this.downloadingId) return;
    this.downloadingId = document.id;
    this.error = '';
    this.service.download(document.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = window.document.createElement('a');
        link.href = url;
        link.download = document.filename;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.downloadingId = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.downloadingId = '';
        this.toast.error('Không tải được file. Vui lòng thử lại.');
      },
    });
  }

  view(document: SupplierDocument) {
    if (this.viewingId || !this.canPreview(document)) return;
    this.viewingId = document.id;
    this.error = '';
    this.service.download(document.id).subscribe({
      next: (blob) => {
        this.releasePreview();
        this.objectUrl = URL.createObjectURL(new Blob([blob], { type: document.contentType }));
        this.preview = document;
        this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
        this.previewDialog?.nativeElement.showModal();
        this.viewingId = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.viewingId = '';
        this.toast.error('Không xem được file. Vui lòng thử lại.');
      },
    });
  }

  canPreview(document: SupplierDocument): boolean {
    return (
      document.contentType === 'application/pdf' ||
      /^image\/(png|jpeg|gif|bmp|webp)$/.test(document.contentType)
    );
  }

  closePreview(): void {
    this.previewDialog?.nativeElement.close();
    this.releasePreview();
  }

  private releasePreview(): void {
    this.preview = null;
    this.previewUrl = null;
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = '';
    }
  }

  openReview(document: SupplierDocument) {
    if (this.reviewing || !this.canReview(document)) return;
    this.selected = document;
    this.reviewForm.reset({ comment: '' });
    this.error = '';
    this.success = '';
  }

  async review(approved: boolean): Promise<void> {
    if (!this.selected || this.reviewing || !this.canReview(this.selected)) return;
    const comment = this.reviewForm.controls.comment.value.trim();
    if (!approved && !comment) {
      this.error = 'Vui lòng nhập lý do từ chối.';
      return;
    }
    if (comment.length > 2000) {
      this.error = 'Nhận xét tối đa 2000 ký tự.';
      return;
    }
    if (!(await this.dialogs.confirm({
      title: approved ? 'Duyệt tài liệu' : 'Từ chối tài liệu',
      message: approved
        ? `Duyệt tài liệu “${this.selected.filename}” thành tài liệu chính thức?`
        : `Xác nhận từ chối tài liệu “${this.selected.filename}”?`,
      confirmText: approved ? 'Duyệt tài liệu' : 'Từ chối',
      tone: approved ? 'primary' : 'danger',
    }))) return;
    this.reviewing = true;
    this.error = '';
    this.service.review(this.selected.id, approved, comment).subscribe({
      next: () => {
        this.reviewing = false;
        this.selected = null;
        this.reviewForm.reset({ comment: '' });
        this.success = approved ? 'Đã duyệt tài liệu chính thức.' : 'Đã từ chối đề xuất tài liệu.';
        this.load();
      },
      error: (err) => {
        this.reviewing = false;
        if (err.status === 409) {
          this.selected = null;
          this.load();
        }
        this.toast.error('Không thể duyệt tài liệu.');
      },
    });
  }

  private fail(err: { error?: { message?: string }; status?: number }, fallback: string) {
    this.error = err.status === 413 ? 'File tối đa 10 MB.' : err.error?.message || fallback;
    this.cdr.markForCheck();
  }
}
