import { CommonModule } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { ConfirmationDialogService } from '../../core/services/confirmation-dialog.service';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.scss',
})
export class ConfirmationDialog {
  readonly dialogs = inject(ConfirmationDialogService);
  readonly promptValue = signal('');
  readonly promptError = signal('');

  constructor() {
    effect(() => {
      const dialog = this.dialogs.active();
      if (dialog?.kind === 'prompt') {
        this.promptValue.set('');
        this.promptError.set('');
      }
    });
  }

  cancel(): void {
    const dialog = this.dialogs.active();
    this.dialogs.resolveActive(dialog?.kind === 'prompt' ? null : false);
  }

  confirm(): void {
    const dialog = this.dialogs.active();
    if (dialog?.kind === 'confirm') this.dialogs.resolveActive(true);
  }

  submitPrompt(): void {
    const dialog = this.dialogs.active();
    if (dialog?.kind !== 'prompt') return;
    const value = this.promptValue().trim();
    if (value.length < dialog.minLength) {
      this.promptError.set(
        dialog.validationMessage ?? `Vui lòng nhập ít nhất ${dialog.minLength} ký tự.`,
      );
      return;
    }
    this.dialogs.resolveActive(value);
  }

  updatePrompt(value: string): void {
    this.promptValue.set(value);
    const dialog = this.dialogs.active();
    if (dialog?.kind === 'prompt' && value.trim().length >= dialog.minLength) {
      this.promptError.set('');
    }
  }

  trapFocus(event: KeyboardEvent): void {
    if (event.key !== 'Tab') return;
    const dialog = event.currentTarget as HTMLElement;
    const focusable = [
      ...dialog.querySelectorAll<HTMLElement>(
        'button:not(:disabled), textarea:not(:disabled), input:not(:disabled)',
      ),
    ];
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
}
