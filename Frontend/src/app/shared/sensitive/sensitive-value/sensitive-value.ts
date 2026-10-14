import { Component, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-sensitive-value',
  standalone: true,
  templateUrl: './sensitive-value.html',
  styleUrl: './sensitive-value.scss',
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
