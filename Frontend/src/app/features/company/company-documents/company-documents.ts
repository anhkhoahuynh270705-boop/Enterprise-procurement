import {
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../auth/services/auth.service';
import { CompanyDocumentService } from '../services/company-document.service';
import { CompanyDocument } from '../model/company.model';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-company-documents',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './company-documents.html',
  styleUrl: './company-documents.scss',
})
export class CompanyDocuments implements OnInit, OnDestroy {
  private readonly service = inject(CompanyDocumentService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);
  readonly isAdmin = inject(AuthService).hasRole('ADMIN');
  readonly categories = [
    { value: 'POLICY', label: 'Nội quy & Chính sách' },
    { value: 'PROCEDURE', label: 'Quy trình' },
    { value: 'TEMPLATE', label: 'Biểu mẫu' },
    { value: 'TRAINING', label: 'Đào tạo' },
    { value: 'OTHER', label: 'Khác' },
  ];
  readonly documents = signal<CompanyDocument[]>([]);
  readonly search = signal('');
  readonly category = signal('');
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly busy = signal('');
  readonly error = signal('');
  readonly success = signal('');
  readonly showUpload = signal(false);
  readonly file = signal<File | null>(null);
  readonly preview = signal<CompanyDocument | null>(null);
  readonly previewUrl = signal<SafeResourceUrl | null>(null);
  private objectUrl = '';
  private readonly fb = inject(FormBuilder);
  private readonly previewDialog = viewChild<ElementRef<HTMLDialogElement>>('previewDialog');
  private readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    category: ['POLICY', Validators.required],
    description: ['', Validators.maxLength(2000)],
  });
  readonly filtered = computed(() => {
    const query = this.search().trim().toLocaleLowerCase('vi');
    return this.documents().filter(
      (doc) =>
        (!this.category() || doc.category === this.category()) &&
        `${doc.title} ${doc.filename} ${doc.description}`.toLocaleLowerCase('vi').includes(query),
    );
  });

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  rows: CompanyDocument[] = [];

  constructor() {
    effect(() => {
      const _ = this.filtered();
      this.currentPage = 0;
      this.updatePaging();
    });
  }

  updatePaging(): void {
    this.totalPages = Math.max(1, Math.ceil(this.filtered().length / this.pageSize));
    const start = this.currentPage * this.pageSize;
    this.rows = this.filtered().slice(start, start + this.pageSize);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.updatePaging();
  }

  ngOnInit(): void {
    this.load();
  }
  ngOnDestroy(): void {
    this.releasePreview();
  }

  load(): void {
    if (this.loading() || this.uploading() || this.busy()) return;
    this.loading.set(true);
    this.error.set('');
    this.service
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (docs) => {
          this.documents.set(docs);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Không tải được tài liệu. Vui lòng thử lại.');
        },
      });
  }

  categoryLabel(value: string): string {
    return this.categories.find((item) => item.value === value)?.label ?? value;
  }

  sizeLabel(size: number): string {
    return size >= 1048576
      ? `${(size / 1048576).toFixed(1)} MB`
      : `${Math.max(1, Math.ceil(size / 1024))} KB`;
  }

  canPreview(doc: CompanyDocument): boolean {
    return (
      doc.contentType === 'application/pdf' ||
      /^image\/(png|jpeg|gif|bmp|webp)$/.test(doc.contentType)
    );
  }

  chooseFile(event: Event): void {
    this.error.set('');
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (
      file &&
      (file.size === 0 ||
        file.size > 10 * 1024 * 1024 ||
        !/\.(pdf|docx?|xlsx?|png|jpe?g|gif|bmp|webp)$/i.test(file.name))
    ) {
      this.error.set('Chọn PDF, Word, Excel hoặc ảnh có nội dung, tối đa 10 MB.');
      input.value = '';
      this.file.set(null);
      return;
    }
    this.file.set(file);
    if (file && !this.form.controls.title.value.trim())
      this.form.controls.title.setValue(file.name.replace(/\.[^.]+$/, '').slice(0, 200));
  }

  upload(): void {
    if (!this.isAdmin || this.uploading() || this.loading() || this.busy()) return;
    this.form.markAllAsTouched();
    const value = this.form.getRawValue(),
      file = this.file();
    if (this.form.invalid || !value.title.trim() || !file) {
      this.toast.error('Vui lòng điền đầy đủ thông tin và chọn file hợp lệ trước khi đăng.');
      return;
    }
    this.uploading.set(true);
    this.error.set('');
    this.success.set('');
    this.service
      .upload(value.title.trim(), value.category, value.description.trim(), file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (doc) => {
          this.documents.update((docs) => [doc, ...docs]);
          this.uploading.set(false);
          this.form.reset({ title: '', category: 'POLICY', description: '' });
          this.file.set(null);
          if (this.fileInput()) this.fileInput()!.nativeElement.value = '';
          this.showUpload.set(false);
          this.search.set('');
          this.category.set('');
          this.toast.success('Đăng tài liệu thành công');
        },
        error: (err) => {
          this.uploading.set(false);
          this.toast.error(err?.error?.message || 'Đăng tài liệu thất bại. Vui lòng thử lại.');
        },
      });
  }

  download(doc: CompanyDocument, preview = false): void {
    if (this.busy() || this.loading() || this.uploading()) return;
    if (preview && !this.canPreview(doc)) return;
    this.busy.set(doc.id);
    this.error.set('');
    this.service
      .download(doc.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          this.busy.set('');
          if (preview) {
            this.releasePreview();
            this.objectUrl = URL.createObjectURL(new Blob([blob], { type: doc.contentType }));
            this.preview.set(doc);
            this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl));
            this.previewDialog()?.nativeElement.showModal();
          } else {
            const url = URL.createObjectURL(blob),
              link = document.createElement('a');
            link.href = url;
            link.download = doc.filename;
            link.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
          }
        },
        error: () => {
          this.busy.set('');
          this.toast.error(
            'Không lấy được file. Tài liệu có thể đã được gỡ hoặc bạn không còn quyền truy cập.',
          );
        },
      });
  }

  closePreview(): void {
    this.previewDialog()?.nativeElement.close();
    this.releasePreview();
  }
  private releasePreview(): void {
    this.preview.set(null);
    this.previewUrl.set(null);
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = '';
    }
  }

  remove(doc: CompanyDocument): void {
    if (
      !this.isAdmin ||
      this.busy() ||
      this.loading() ||
      this.uploading() ||
      !confirm(`Gỡ tài liệu “${doc.title}” khỏi thư viện công ty?`)
    )
      return;
    this.busy.set(doc.id);
    this.error.set('');
    this.success.set('');
    this.service
      .delete(doc.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.documents.update((docs) => docs.filter((item) => item.id !== doc.id));
          this.busy.set('');
          this.success.set('Đã gỡ tài liệu khỏi thư viện.');
        },
        error: () => {
          this.busy.set('');
          this.error.set('Không thể gỡ tài liệu. Vui lòng thử lại.');
        },
      });
  }
}
