import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierDocumentService } from '../services/supplier-document.service';
import { SupplierDocument, SupplierOption, DocumentStatus } from '../models/supplier-document';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-supplier-documents',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-documents.html',
  styleUrl: './supplier-documents.scss',
})
export class SupplierDocuments implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly service = inject(SupplierDocumentService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
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

  ngOnInit() {
    this.load();
  }

  ngOnDestroy() {
    this.releasePreview();
  }

  get filtered() {
    const filters = this.filterForm.getRawValue();
    const query = this.normalizeSearch(filters.searchQuery);
    const submittedBy = this.normalizeSearch(filters.submittedBy);
    const reviewedBy = this.normalizeSearch(filters.reviewedBy);
    const reviewComment = this.normalizeSearch(filters.reviewComment);
    return this.documents.filter((document) => {
      const submittedDate = document.submittedAt?.slice(0, 10) || '';
      const reviewedDate = document.reviewedAt?.slice(0, 10) || '';
      const searchableText = this.normalizeSearch(
        [
          document.filename,
          document.supplierName,
          document.submittedBy,
          document.reviewedBy,
          document.reviewComment,
        ]
          .filter(Boolean)
          .join(' '),
      );

      return (
        (!filters.filterSupplier || document.supplierId === filters.filterSupplier) &&
        (!filters.filterStatus || document.status === filters.filterStatus) &&
        (!query || searchableText.includes(query)) &&
        (!submittedBy || this.normalizeSearch(document.submittedBy).includes(submittedBy)) &&
        (!reviewedBy || this.normalizeSearch(document.reviewedBy).includes(reviewedBy)) &&
        (!reviewComment || this.normalizeSearch(document.reviewComment).includes(reviewComment)) &&
        (!filters.submittedFrom || submittedDate >= filters.submittedFrom) &&
        (!filters.submittedTo || submittedDate <= filters.submittedTo) &&
        (!filters.reviewedFrom || (!!reviewedDate && reviewedDate >= filters.reviewedFrom)) &&
        (!filters.reviewedTo || (!!reviewedDate && reviewedDate <= filters.reviewedTo))
      );
    });
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

  private normalizeSearch(value: string | null | undefined): string {
    return (value || '').trim().toLocaleLowerCase('vi');
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
    this.loading = true;
    this.error = '';
    forkJoin({
      documents: this.service.list(),
      suppliers: this.service.suppliers(),
    }).subscribe({
      next: (result) => {
        this.documents = result.documents;
        this.suppliers = result.suppliers;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.fail(err, 'Không tải được tài liệu. Vui lòng thử lại.');
      },
    });
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

  upload() {
    if (this.uploading) return;
    const supplierId = this.uploadForm.controls.supplierId.value;
    if (!supplierId || !this.file) {
      this.toast.error('Vui lòng chọn nhà cung cấp và file hợp lệ trước khi tải lên.');
      return;
    }
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
        this.filterForm.patchValue({ filterSupplier: document.supplierId, filterStatus: '' });
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

  review(approved: boolean) {
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
