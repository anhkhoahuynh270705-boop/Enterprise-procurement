import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  PLATFORM_ID,
  inject,
  signal,
} from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SupplierResponseService } from '../service/supplier-response.service';
import { RfqTechnicalStatus } from '../../model/rfq.models';
import {
  SupplierInvitationViewDto,
  SupplierQuoteSubmissionRequest,
} from '../model/supplier-response.models';

type SupplierQuoteItemForm = FormGroup<{
  rfqItemId: FormControl<string>;
  quoted: FormControl<boolean>;
  unitPrice: FormControl<number | null>;
  note: FormControl<string>;
}>;

@Component({
  selector: 'app-supplier-rfq-response',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-rfq-response.html',
  styleUrl: './supplier-rfq-response.scss',
})
export class SupplierRfqResponsePage {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly route = inject(ActivatedRoute);
  private readonly supplierResponse = inject(SupplierResponseService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly invitation = signal<SupplierInvitationViewDto | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly token = signal('');
  private readonly fb = inject(FormBuilder);
  readonly quoteForm = this.fb.nonNullable.group({
    validUntil: [this.defaultValidityDate(), Validators.required],
    deliveryDays: this.fb.control<number | null>(null, Validators.min(0)),
    paymentTerms: ['', Validators.maxLength(1000)],
    warranty: ['', Validators.maxLength(1000)],
    technicalStatus: 'PASS' as RfqTechnicalStatus,
    taxAmount: [0, Validators.min(0)],
    shippingCost: [0, Validators.min(0)],
    otherCost: [0, Validators.min(0)],
    items: new FormArray<SupplierQuoteItemForm>([]),
  });

  get itemForms(): FormArray<SupplierQuoteItemForm> {
    return this.quoteForm.controls.items;
  }

  constructor() {
    if (!isPlatformBrowser(this.platformId)) return;
    const token =
      new URLSearchParams(window.location.hash.slice(1)).get('token') ??
      this.route.snapshot.queryParamMap.get('token') ??
      '';
    this.token.set(token);
    if (!token) {
      this.isLoading.set(false);
      this.error.set('Link báo giá không có mã mời hợp lệ. Vui lòng mở lại link trong email.');
      return;
    }
    this.supplierResponse
      .resolveInvitation(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ data }) => {
          this.invitation.set(data);
          this.itemForms.clear();
          for (const item of data.items ?? []) {
            const itemForm = this.fb.nonNullable.group({
              rfqItemId: item.rfqItemId,
              quoted: true,
              unitPrice: this.fb.control<number | null>(null),
              note: ['', Validators.maxLength(1000)],
            });

            itemForm.controls.quoted.valueChanges
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe((quoted) => {
                if (quoted) itemForm.controls.unitPrice.enable({ emitEvent: false });
                else itemForm.controls.unitPrice.disable({ emitEvent: false });
              });

            this.itemForms.push(itemForm);
          }
          this.isLoading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.errorMessage(error));
          this.isLoading.set(false);
        },
      });
  }

  submitQuote(): void {
    this.quoteForm.markAllAsTouched();
    const value = this.quoteForm.getRawValue();
    const items = value.items;
    if (this.quoteForm.invalid) {
      this.error.set('Vui lòng kiểm tra lại ngày hiệu lực và các thông tin số liệu.');
      return;
    }
    if (!value.validUntil) {
      this.error.set('Vui lòng nhập ngày hết hiệu lực của báo giá.');
      return;
    }
    if (!items.some((item) => item.quoted)) {
      this.error.set('Báo giá cần chào ít nhất một hạng mục.');
      return;
    }
    if (items.some((item) => item.quoted && (item.unitPrice === null || item.unitPrice < 0))) {
      this.error.set('Vui lòng nhập đơn giá hợp lệ cho các hạng mục đã chào.');
      return;
    }
    this.error.set('');
    this.isSubmitting.set(true);
    const request: SupplierQuoteSubmissionRequest = {
      token: this.token(),
      quote: {
        validUntil: value.validUntil,
        deliveryDays: value.deliveryDays ?? undefined,
        paymentTerms: value.paymentTerms.trim() || undefined,
        warranty: value.warranty.trim() || undefined,
        technicalStatus: value.technicalStatus,
        taxAmount: Number(value.taxAmount) || 0,
        shippingCost: Number(value.shippingCost) || 0,
        otherCost: Number(value.otherCost) || 0,
        items: items.map((item) => ({
          rfqItemId: item.rfqItemId,
          quoted: item.quoted,
          unitPrice: item.quoted ? Number(item.unitPrice) : undefined,
          note: item.note.trim() || undefined,
        })),
      },
    };
    this.supplierResponse
      .submitQuote(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.submitted.set(true);
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.error.set(this.errorMessage(error));
        },
      });
  }

  private defaultValidityDate(): string {
    const date = new Date();
    date.setDate(date.getDate() + 30);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 10);
  }

  private errorMessage(error: HttpErrorResponse): string {
    const body = error.error;
    if (typeof body?.message === 'string') return body.message;
    if (typeof body?.detail === 'string') return body.detail;
    return error.status === 0
      ? 'Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối và thử lại.'
      : 'Link báo giá đã hết hạn, đã được sử dụng hoặc RFQ không còn nhận báo giá.';
  }
}
