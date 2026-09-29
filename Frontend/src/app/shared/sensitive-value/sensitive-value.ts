import { Component, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-sensitive-value',
  standalone: true,
  template: `
    <span>{{ hasValue ? (visible ? value : '••••••••') : emptyText }}</span>
    @if (hasValue) {
      <button type="button" (click)="toggle($event)"
        [attr.aria-label]="(visible ? 'Ẩn ' : 'Hiện ') + label"
        [title]="(visible ? 'Ẩn ' : 'Hiện ') + label" [attr.aria-pressed]="visible">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
          <circle cx="12" cy="12" r="3" />
          @if (visible) { <path d="m3 3 18 18" /> }
        </svg>
      </button>
    }
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 6px; max-width: 100%; vertical-align: middle; }
    span { min-width: 0; overflow-wrap: anywhere; }
    button { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
      padding: 3px; border: 0; border-radius: 4px; background: transparent; color: inherit; cursor: pointer; }
    button:hover { background: rgb(127 127 127 / 15%); }
    button:focus-visible { outline: 2px solid #2365a8; outline-offset: 2px; }
  `,
})
export class SensitiveValue implements OnChanges {
  @Input() value: string | number | null | undefined;
  @Input() recordKey: string | number | null | undefined;
  @Input() label = 'thông tin';
  @Input() emptyText = '—';
  visible = false;

  get hasValue(): boolean {
    return this.value !== null && this.value !== undefined && String(this.value).trim() !== '';
  }

  ngOnChanges(): void {
    this.visible = false;
  }

  toggle(event: Event): void {
    event.stopPropagation();
    this.visible = !this.visible;
  }
}
