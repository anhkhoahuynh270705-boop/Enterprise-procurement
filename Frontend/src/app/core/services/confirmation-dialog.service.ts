import { Injectable, signal } from '@angular/core';

export interface ConfirmationDialogOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  tone?: 'primary' | 'danger';
}

export interface PromptDialogOptions extends ConfirmationDialogOptions {
  label: string;
  placeholder?: string;
  minLength?: number;
  validationMessage?: string;
}

export type ActiveDialog =
  | (ConfirmationDialogOptions & {
      id: number;
      kind: 'confirm';
      title: string;
      confirmText: string;
      cancelText: string;
      tone: 'primary' | 'danger';
    })
  | (PromptDialogOptions & {
      id: number;
      kind: 'prompt';
      title: string;
      confirmText: string;
      cancelText: string;
      tone: 'primary' | 'danger';
      minLength: number;
    });

type DialogResult = boolean | string | null;
type QueuedDialog = {
  request: ConfirmationDialogOptions | PromptDialogOptions;
  kind: ActiveDialog['kind'];
  resolve: (result: DialogResult) => void;
};

@Injectable({ providedIn: 'root' })
export class ConfirmationDialogService {
  readonly active = signal<ActiveDialog | null>(null);

  private readonly queue: QueuedDialog[] = [];
  private activeResolver: ((result: DialogResult) => void) | null = null;
  private nextId = 0;

  confirm(options: ConfirmationDialogOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.enqueue({ kind: 'confirm', request: options, resolve: (result) => resolve(result === true) });
    });
  }

  prompt(options: PromptDialogOptions): Promise<string | null> {
    return new Promise<string | null>((resolve) => {
      this.enqueue({
        kind: 'prompt',
        request: options,
        resolve: (result) => resolve(typeof result === 'string' ? result : null),
      });
    });
  }

  resolveActive(result: DialogResult): void {
    const resolve = this.activeResolver;
    if (!resolve) return;

    this.activeResolver = null;
    this.active.set(null);
    resolve(result);
    queueMicrotask(() => this.activateNext());
  }

  private enqueue(dialog: QueuedDialog): void {
    this.queue.push(dialog);
    if (!this.active()) this.activateNext();
  }

  private activateNext(): void {
    if (this.active() || !this.queue.length) return;
    const next = this.queue.shift();
    if (!next) return;

    this.activeResolver = next.resolve;
    this.active.set({
      ...next.request,
      id: ++this.nextId,
      kind: next.kind,
      title: next.request.title ?? (next.kind === 'confirm' ? 'Xác nhận thao tác' : 'Nhập lý do'),
      confirmText: next.request.confirmText ?? (next.kind === 'confirm' ? 'Xác nhận' : 'Tiếp tục'),
      cancelText: next.request.cancelText ?? 'Hủy',
      tone: next.request.tone ?? 'primary',
      ...(next.kind === 'prompt'
        ? { minLength: (next.request as PromptDialogOptions).minLength ?? 1 }
        : {}),
    } as ActiveDialog);
  }
}
