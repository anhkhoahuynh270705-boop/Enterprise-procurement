import { SensitiveInput } from '../../../shared/sensitive-value/sensitive-input';
import { SensitiveValue } from '../../../shared/sensitive-value/sensitive-value';
import { ToastService } from '../../../core/services/toast.service';
import { ChangeDetectorRef, Component, inject, OnInit, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../auth/services/auth.service';
import { SupplierProposalService } from '../services/supplier-proposal.service';
import { ProposalStatus, SupplierProposal } from '../models/supplier-proposal';
import {
  bankAccountValidator,
  bankDetailsValidator,
  contactRequiredValidator,
  phoneValidator,
  supplierNameValidator,
  trimmed,
} from '../shared/supplier-form.validators';

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
          supplierNameValidator(true),
          trimmed(Validators.minLength(2)),
          trimmed(Validators.maxLength(200)),
        ],
      ],
      taxCode: [
        '',
        [trimmed(Validators.required), trimmed(Validators.pattern(/^[0-9]{10}([0-9]{3})?$/))],
      ],
      contactPerson: ['', [supplierNameValidator(), trimmed(Validators.maxLength(200))]],
      email: ['', [trimmed(Validators.email), trimmed(Validators.maxLength(254))]],
      phone: ['', phoneValidator()],
      address: ['', trimmed(Validators.maxLength(255))],
      city: ['', [supplierNameValidator(), trimmed(Validators.maxLength(255))]],
      country: ['', [supplierNameValidator(), trimmed(Validators.maxLength(255))]],
      bankName: ['', [supplierNameValidator(), trimmed(Validators.maxLength(255))]],
      bankAccount: ['', bankAccountValidator()],
      notes: ['', trimmed(Validators.maxLength(2000))],
      reason: [
        '',
        [
          trimmed(Validators.required),
          trimmed(Validators.minLength(10)),
          trimmed(Validators.maxLength(2000)),
        ],
      ],
    },
    { validators: [contactRequiredValidator, bankDetailsValidator] },
  );

  ngOnInit() {
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

  fieldError(name: keyof typeof this.form.controls): string {
    if (this.fieldErrors[name]) return this.fieldErrors[name];
    const field = this.form.controls[name];
    if (!field.touched || !field.errors) return '';
    if (field.hasError('required')) return 'Vui lòng nhập thông tin, không chỉ khoảng trắng.';
    if (field.hasError('minlength'))
      return `Tối thiểu ${field.errors['minlength'].requiredLength} ký tự.`;
    if (field.hasError('maxlength'))
      return `Tối đa ${field.errors['maxlength'].requiredLength} ký tự.`;
    if (field.hasError('name'))
      return 'Chỉ nhập chữ và một khoảng trắng giữa các từ, không nhập số.';
    if (field.hasError('email')) return 'Email không hợp lệ.';
    if (field.hasError('bankAccount'))
      return 'Số tài khoản gồm 6–50 chữ số, không có chữ hoặc khoảng trắng.';
    return name === 'taxCode'
      ? 'Mã số thuế phải có 10 hoặc 13 chữ số.'
      : 'Điện thoại gồm 9–15 chữ số, không có chữ hoặc khoảng trắng.';
  }

  submit() {
    if (!this.canSubmit || this.submitting) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.submitting = true;
    this.fieldErrors = {};
    const raw = this.form.getRawValue();
    const request = {
      ...raw,
      name: raw.name.trim(),
      taxCode: raw.taxCode.trim(),
      contactPerson: raw.contactPerson.trim(),
      email: raw.email.trim(),
      phone: raw.phone.trim(),
      address: raw.address.trim(),
      city: raw.city.trim(),
      country: raw.country.trim(),
      bankName: raw.bankName.trim(),
      bankAccount: raw.bankAccount.trim(),
      notes: raw.notes.trim(),
      reason: raw.reason.trim(),
    };
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
        if (!this.embedded()) 
          this.toast.success(this.success);
        this.reviewed.emit();
        this.load();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.reviewing = false;
        this.reviewError = err?.error?.message || 'Không thể xử lý đề xuất.';
        if (!this.embedded()) 
          this.toast.error(this.reviewError);
        if (err.status === 409) 
          this.load();
        this.cdr.markForCheck();
      },
    });
  }
}
