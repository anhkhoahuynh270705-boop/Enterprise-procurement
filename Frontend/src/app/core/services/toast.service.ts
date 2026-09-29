import { Injectable, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ToastMessage, ToastType } from '../model/toast';

@Injectable({ 
  providedIn: 'root' 
})
export class ToastService implements OnDestroy {
  private readonly platform = inject(PLATFORM_ID);
  private readonly messages = signal<ToastMessage[]>([]);
  readonly toasts = this.messages.asReadonly();
  private nextId = 0;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  success(message: string): void { 
    this.show(message, 'success'); 
  }
  error(message: string): void { 
    this.show(message, 'error'); 
  }
  warning(message: string): void { 
    this.show(message, 'warning'); 
  }

  private show(message: string, type: ToastType): void {
    if (!isPlatformBrowser(this.platform) || !message?.trim()) return;
    const id = ++this.nextId;
    this.messages.update(items => [...items, { id, message, type }]);
    this.timers.set(id, setTimeout(() => this.dismiss(id), 5000));
  }

  dismiss(id: number): void {
    clearTimeout(this.timers.get(id));
    this.timers.delete(id);
    this.messages.update(items => items.filter(item => item.id !== id));
  }

  ngOnDestroy(): void {
    this.timers.forEach(timer => clearTimeout(timer));
    this.timers.clear();
  }
}
