import {
  AfterContentInit,
  Component,
  ContentChild,
  ElementRef,
  Input,
  OnChanges,
  Renderer2,
  inject,
} from '@angular/core';

@Component({
  selector: 'app-sensitive-input',
  standalone: true,
  templateUrl: './sensitive-input.html',
  styleUrl: './sensitive-input.scss',

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
      this.renderer.setProperty(
        this.input.nativeElement,
        'type',
        this.visible ? 'text' : 'password',
      );
    }
  }
}
