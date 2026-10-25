import { CommonModule } from '@angular/common';
import { Component, input, output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProcurementTicket } from '../../procurement/model/procurement';
import { RfqItem, RfqScoreWeights, RfqSupplier } from '../model/rfq.models';
import { RfqFormSaveEvent } from '../model/rfq-form-save.event';
import { CRITERION_FROM_KEY, DEFAULT_WEIGHTS, dayString } from '../helper/rfq-page.helper';

@Component({
  selector: 'app-rfq-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rfq-form-dialog.html',
  styleUrl: './rfq-form-dialog.scss',
})
export class RfqFormDialog {
  private readonly fb = inject(FormBuilder);

  readonly approvedTickets = input<ProcurementTicket[]>([]);
  readonly departments = input<string[]>([]);
  readonly suppliers = input<RfqSupplier[]>([]);
  readonly isSaving = input(false);
  readonly serverError = input('');
  readonly close = output<void>();
  readonly save = output<RfqFormSaveEvent>();
  readonly clearServerError = output<void>();

  readonly items = signal<RfqItem[]>([]);
  readonly selectedSupplierIds = signal<string[]>([]);
  readonly supplierException = signal('');
  readonly formError = signal('');
  readonly weights = signal<RfqScoreWeights>({ ...DEFAULT_WEIGHTS });
  readonly form = this.fb.nonNullable.group({
    procurementTicketId: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    department: [''],
    deadline: [dayString(new Date(Date.now() + 7 * 86_400_000)), Validators.required],
    validityDays: [30, [Validators.required, Validators.min(1), Validators.max(365)]],
    deliveryLocation: [''],
    deliveryWindow: [''],
  });

  selectTicket(ticketId: string): void {
    const ticket = this.approvedTickets().find((item) => item.id === ticketId);
    if (!ticket) {
      this.items.set([]);
      return;
    }
    if (!this.form.controls.department.value && ticket.department)
      this.form.controls.department.setValue(ticket.department);
    this.items.set(
      (ticket.items ?? []).map((item, index) => this.fromProcurementItem(item, ticket.id, index)),
    );
  }

  toggleSupplier(supplierId: string, checked: boolean): void {
    const supplier = this.suppliers().find((item) => item.id === supplierId);
    if (!supplier || supplier.status !== 'ACTIVE') return;
    this.selectedSupplierIds.update((ids) =>
      checked ? [...new Set([...ids, supplierId])] : ids.filter((id) => id !== supplierId),
    );
  }

  updateItem(
    index: number,
    field: 'name' | 'quantity' | 'unit' | 'budgetUnitPrice',
    value: string,
  ): void {
    this.items.update((items) =>
      items.map((item, currentIndex) =>
        currentIndex !== index
          ? item
          : {
              ...item,
              procurementItemId: undefined,
              [field]:
                field === 'quantity' || field === 'budgetUnitPrice' ? Number(value) || 0 : value,
            },
      ),
    );
  }

  addItem(): void {
    this.items.update((items) => [
      ...items,
      {
        id: `new-${Date.now()}`,
        sku: '',
        name: '',
        quantity: 1,
        unit: '',
        budgetUnitPrice: 0,
        active: true,
      },
    ]);
  }

  removeItem(index: number): void {
    this.items.update((items) => items.filter((_, currentIndex) => currentIndex !== index));
  }

  async importItems(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.formError.set('Chỉ hỗ trợ CSV UTF-8.');
      return;
    }
    const rows = (await file.text())
      .split(/\r?\n/)
      .filter(Boolean)
      .slice(1)
      .map((line) => line.split(/[;,]/));
    const imported = rows
      .filter((row) => row.length >= 4)
      .map((row, index) => ({
        id: `csv-${Date.now()}-${index}`,
        sku: row[0]?.trim() ?? '',
        name: row[1]?.trim() ?? '',
        quantity: Number(row[2]) || 1,
        unit: row[3]?.trim() ?? '',
        budgetUnitPrice: Number((row[4] ?? '0').replaceAll('.', '').replace(',', '.')) || 0,
        active: true,
      }));
    if (!imported.length) {
      this.formError.set(
        'CSV không có dòng hợp lệ. Cột cần có: SKU, tên hàng, số lượng, đơn vị, giá ngân sách.',
      );
      return;
    }
    this.items.set(imported);
    this.formError.set('');
  }

  saveForm(submit: boolean): void {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (this.form.invalid) {
      this.formError.set('Vui lòng điền đầy đủ thông tin chung.');
      return;
    }
    if (
      !this.items().length ||
      this.items().some(
        (item) => !item.name.trim() || item.quantity <= 0 || item.budgetUnitPrice < 0,
      )
    ) {
      this.formError.set('Vui lòng nhập thông tin hợp lệ cho từng hạng mục.');
      return;
    }
    if (!this.selectedSupplierIds().length) {
      this.formError.set('Chọn ít nhất một nhà cung cấp.');
      return;
    }
    if (this.selectedSupplierIds().length < 3 && this.supplierException().trim().length < 10) {
      this.formError.set(
        'Chọn tối thiểu 3 nhà cung cấp hoặc nhập lý do ngoại lệ (tối thiểu 10 ký tự).',
      );
      return;
    }
    if (submit && !confirm('Trình duyệt yêu cầu báo giá này?')) return;
    this.clearServerError.emit();
    this.formError.set('');
    this.save.emit({
      rfqId: null,
      submit,
      request: {
        procurementTicketId: value.procurementTicketId,
        title: value.title.trim(),
        department: value.department || undefined,
        deadline: `${value.deadline}T23:59:00`,
        quoteValidityDays: value.validityDays,
        deliveryLocation: value.deliveryLocation || undefined,
        deliveryWindow: value.deliveryWindow || undefined,
        supplierIds: [...this.selectedSupplierIds()],
        exceptionReason: this.supplierException().trim() || undefined,
        items: this.items().map((item) => ({
          procurementItemId: item.procurementItemId || undefined,
          sku: item.sku || undefined,
          itemName: item.name,
          category: item.category || undefined,
          quantity: item.quantity,
          unit: item.unit || undefined,
          budgetUnitPrice: item.budgetUnitPrice,
        })),
        scoreCriteria: Object.entries(this.weights()).map(([key, weightPercent]) => ({
          criterion: CRITERION_FROM_KEY[key as keyof RfqScoreWeights],
          weightPercent,
        })),
        submitForApproval: submit,
      },
    });
  }

  dismiss(): void {
    this.close.emit();
  }

  trapDialogFocus(event: KeyboardEvent): void {
    if (event.key !== 'Tab') return;
    const dialog = event.currentTarget as HTMLElement;
    const focusable = [
      ...dialog.querySelectorAll<HTMLElement>(
        'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
      ),
    ].filter((element) => element.offsetParent !== null);
    if (!focusable.length) {
      event.preventDefault();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private fromProcurementItem(
    item: ProcurementTicket['items'][number],
    ticketId: string,
    index: number,
  ): RfqItem {
    return {
      id: item.id ?? `${ticketId}-${index}`,
      procurementItemId: item.id,
      sku: item.itemCode ?? '',
      name: item.itemName,
      category: item.category,
      quantity: item.quantity,
      unit: item.unit ?? '',
      budgetUnitPrice: item.unitPrice,
      active: true,
    };
  }
}
