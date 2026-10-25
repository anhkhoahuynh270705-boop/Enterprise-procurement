import { SensitiveValue } from '../../../shared/sensitive/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { ChangeDetectorRef, Component, inject, OnInit, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierProposalService } from '../services/supplier-proposal.service';
import { ProposalStatus, SupplierProposal } from '../models/supplier-proposal';
import {
  bankAccountValidator,
  bankNameValidator,
  bankDetailsValidator,
  contactRequiredValidator,
  phoneValidator,
  normalizeSupplierCompanyName,
  supplierCompanyNameValidator,
  supplierNameValidator,
  trimmedValidators,
} from '../shared/supplier-form.validators';
import { SensitiveInput } from '../../../shared/sensitive/sensitive-input/sensitive-input';

@Component({
  selector: 'app-supplier-proposals',
  imports: [SensitiveInput, SensitiveValue, CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-proposals.html',
  styleUrl: './supplier-proposals.scss',
})
export class SupplierProposals implements OnInit {
  readonly embedded = input(false);
  readonly reviewed = output<void>();
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);
  private readonly service = inject(SupplierProposalService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  readonly isAdmin = this.auth.hasRole('ADMIN');
  readonly isReviewer = this.auth.hasRole('ADMIN', 'CHECKER');
  readonly canSubmit = this.auth.hasRole('USER', 'CHECKER');
  readonly labels: Record<ProposalStatus, string> = {
    PENDING: 'Chờ duyệt',
    APPROVED: 'Đã chấp thuận',
    REJECTED: 'Đã từ chối',
  };
  proposals: SupplierProposal[] = [];
  selected: SupplierProposal | null = null;
  showForm = false;
  loading = false;
  submitting = false;
  reviewing = false;
  error = '';
  submitError = '';
  reviewError = '';
  success = '';
  readonly reviewForm = this.fb.nonNullable.group({
    filter: this.fb.nonNullable.control<ProposalStatus | 'ALL'>(
      this.isReviewer ? 'PENDING' : 'ALL',
    ),
    reviewComment: '',
  });
  fieldErrors: Record<string, string> = {};

  readonly form = this.fb.nonNullable.group(
    {
      name: [
        '',
        [
          supplierCompanyNameValidator(true),
          ...trimmedValidators(Validators.minLength(2), Validators.maxLength(200)),
        ],
      ],
      taxCode: [
        '',
        trimmedValidators(Validators.required, Validators.pattern(/^[0-9]{10}([0-9]{3})?$/)),
      ],
      contactPerson: [
        '',
        [supplierNameValidator(), ...trimmedValidators(Validators.maxLength(200))],
      ],
      email: ['', trimmedValidators(Validators.email, Validators.maxLength(254))],
      phone: ['', phoneValidator()],
      address: ['', trimmedValidators(Validators.maxLength(255))],
      city: ['', [supplierNameValidator(), ...trimmedValidators(Validators.maxLength(255))]],
      country: ['', [supplierNameValidator(), ...trimmedValidators(Validators.maxLength(255))]],
      bankName: ['', [bankNameValidator(), ...trimmedValidators(Validators.maxLength(255))]],
      bankAccount: ['', bankAccountValidator()],
      notes: ['', trimmedValidators(Validators.maxLength(2000))],
      reason: [
        '',
        trimmedValidators(
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(2000),
        ),
      ],
    },
    { validators: [contactRequiredValidator, bankDetailsValidator] },
  );

  ngOnInit() {
    const supplierName = this.route.snapshot.queryParamMap.get('name');
    if (supplierName && this.canSubmit) {
      const ticketCode = this.route.snapshot.queryParamMap.get('ticketCode');
      this.form.patchValue({
        name: normalizeSupplierCompanyName(supplierName),
        reason: ticketCode
          ? `Bổ sung nhà cung cấp cho phiếu mua sắm ${ticketCode}.`
          : 'Bổ sung thông tin nhà cung cấp vào hệ thống.',
      });
      this.showForm = true;
    }
    this.load();
  }

  get pendingCount() {
    return this.proposals.filter((p) => p.status === 'PENDING').length;
  }
  get filter(): ProposalStatus | 'ALL' {
    return this.reviewForm.controls.filter.value;
  }
  set filter(value: ProposalStatus | 'ALL') {
    this.reviewForm.controls.filter.setValue(value);
  }
  get reviewComment(): string {
    return this.reviewForm.controls.reviewComment.value;
  }
  set reviewComment(value: string) {
    this.reviewForm.controls.reviewComment.setValue(value);
  }
  get filtered() {
    return this.proposals.filter((p) => this.filter === 'ALL' || p.status === this.filter);
  }

  load() {
    this.loading = true;
    this.error = '';
    this.service.list().subscribe({
      next: (proposals) => {
        this.proposals = proposals;
        if (this.selected)
          this.selected = proposals.find((p) => p.id === this.selected?.id) || null;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Không tải được đề xuất. Vui lòng thử lại.';
        this.toast.error(this.error);
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  submit() {
    if (!this.canSubmit || this.submitting) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.submitting = true;
    this.fieldErrors = {};
    const raw = this.form.getRawValue();
    const request = Object.fromEntries(
      Object.entries(raw).map(([key, value]) => [key, value.trim()]),
    ) as typeof raw;
    request.name = normalizeSupplierCompanyName(raw.name);
    this.service.submit(request).subscribe({
      next: () => {
        this.submitting = false;
        this.showForm = false;
        this.form.reset();
        this.filter = 'ALL';
        this.toast.success('Gửi đề xuất thành công và đang chờ duyệt.');
        this.load();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.submitting = false;
        this.toast.error(err?.error?.message || 'Gửi đề xuất thất bại.');
        this.fieldErrors = err?.error?.details || {};
        this.cdr.markForCheck();
      },
    });
  }

  open(proposal: SupplierProposal) {
    if (this.reviewing) return;
    this.selected = proposal;
    this.reviewComment = '';
    this.reviewError = '';
  }

  canReviewProposal(proposal: SupplierProposal) {
    return (
      this.isReviewer &&
      proposal.status === 'PENDING' &&
      (this.isAdmin || proposal.submittedBy !== this.auth.getCurrentUser()?.username)
    );
  }

  quickApprove(proposal: SupplierProposal) {
    if (!this.canReviewProposal(proposal) || this.reviewing) return;
    this.selected = proposal;
    this.reviewComment = '';
    this.reviewError = '';
    this.review(true);
  }

  openReject(proposal: SupplierProposal) {
    if (!this.canReviewProposal(proposal) || this.reviewing) return;
    this.selected = proposal;
    this.reviewComment = '';
    this.reviewError = '';
    // Cuộn đến panel chi tiết để nhập lý do từ chối
    setTimeout(() => {
      document.querySelector<HTMLElement>('.details textarea')?.focus();
    }, 100);
  }

  review(approved: boolean) {
    if (!this.selected || !this.canReviewProposal(this.selected) || this.reviewing) return;
    const comment = this.reviewComment.trim();
    if (!approved && !comment) {
      this.reviewError = 'Vui lòng nhập lý do từ chối.';
      return;
    }
    if (comment.length > 2000) {
      this.reviewError = 'Nhận xét tối đa 2000 ký tự.';
      return;
    }
    this.reviewing = true;
    this.reviewError = '';
    this.success = '';
    this.service.review(this.selected.id, approved, comment).subscribe({
      next: () => {
        this.reviewing = false;
        this.selected = null;
        this.success = approved ? 'Đã chấp thuận và tạo nhà cung cấp.' : 'Đã từ chối đề xuất.';
        if (!this.embedded()) this.toast.success(this.success);
        this.reviewed.emit();
        this.load();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.reviewing = false;
        this.reviewError = err?.error?.message || 'Không thể xử lý đề xuất.';
        if (!this.embedded()) this.toast.error(this.reviewError);
        if (err.status === 409) this.load();
        this.cdr.markForCheck();
      },
    });
  }
}
