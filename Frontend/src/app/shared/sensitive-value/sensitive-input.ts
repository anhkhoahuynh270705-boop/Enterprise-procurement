import { AfterContentInit, Component, ContentChild, ElementRef, Input, OnChanges, Renderer2, inject } from '@angular/core';

@Component({
  selector: 'app-sensitive-input',
  standalone: true,
  template: `
    <ng-content />
    <button type="button" (click)="toggle($event)"
      [attr.aria-label]="(visible ? 'Ẩn ' : 'Hiện ') + label"
      [title]="(visible ? 'Ẩn ' : 'Hiện ') + label" [attr.aria-pressed]="visible">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
        @if (visible) { <path d="m3 3 18 18" /> }
      </svg>
    </button>
  `,
  styles: `
    :host { display: flex; align-items: center; gap: 6px; min-width: 0; }
    button { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
      padding: 8px; border: 1px solid #bac3ce; border-radius: 6px; background: #fff; color: #444; cursor: pointer; }
    button:focus-visible { outline: 2px solid #2365a8; outline-offset: 2px; }
  `,
})
export class SensitiveInput implements AfterContentInit, OnChanges {
  @ContentChild('sensitiveInput', { read: ElementRef }) input?: ElementRef<HTMLInputElement>;
  @Input() label = 'thông tin';
  @Input() recordKey: unknown;
  visible = false;
  private readonly renderer = inject(Renderer2);

  ngAfterContentInit(): void {
    this.updateType();
  }

  ngOnChanges(): void {
    this.visible = false;
    this.updateType();
  }

  toggle(event: Event): void {
    event.stopPropagation();
    this.visible = !this.visible;
    this.updateType();
  }

  private updateType(): void {
    if (this.input) {
      this.renderer.setProperty(this.input.nativeElement, 'type', this.visible ? 'text' : 'password');
    }
  }
}
