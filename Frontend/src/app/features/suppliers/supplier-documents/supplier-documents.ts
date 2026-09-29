import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierDocumentService } from '../services/supplier-document.service';
import { SupplierDocument, SupplierOption, DocumentStatus } from '../models/supplier-document';

@Component({
  selector: 'app-supplier-documents',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-documents.html',
  styleUrl: './supplier-documents.scss',
})
export class SupplierDocuments implements OnInit {
  readonly auth = inject(AuthService);
  private readonly service = inject(SupplierDocumentService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
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

  readonly uploadForm = this.fb.nonNullable.group({
    supplierId: ['', Validators.required],
  });

  readonly filterForm = this.fb.nonNullable.group({
    filterSupplier: '',
    filterStatus: '',
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

  ngOnInit() {
    this.load();
  }

  get filtered() {
    const { filterSupplier, filterStatus } = this.filterForm.getRawValue();
    return this.documents.filter(
      (d) =>
        (!filterSupplier || d.supplierId === filterSupplier) &&
        (!filterStatus || d.status === filterStatus),
    );
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
      this.error = 'Vui lòng chọn nhà cung cấp và file hợp lệ.';
      return;
    }
    this.uploading = true;
    this.error = '';
    this.success = '';
    this.service.upload(supplierId, this.file).subscribe({
      next: (document) => {
        this.uploading = false;
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
        this.fail(err, 'Upload thất bại. Vui lòng thử lại.');
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
        this.fail(err, 'Không tải được file. Vui lòng thử lại.');
      },
    });
  }

  view(document: SupplierDocument) {
    if (this.viewingId) return;
    this.viewingId = document.id;
    this.error = '';
    this.service.download(document.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 30000);
        this.viewingId = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.viewingId = '';
        this.fail(err, 'Không xem được file. Vui lòng thử lại.');
      },
    });
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
        this.fail(err, 'Không thể duyệt tài liệu.');
      },
    });
  }

  private fail(err: { error?: { message?: string }; status?: number }, fallback: string) {
    this.error = err.status === 413 ? 'File tối đa 10 MB.' : err.error?.message || fallback;
    this.cdr.markForCheck();
  }
}
