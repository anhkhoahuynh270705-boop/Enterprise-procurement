import { TestBed } from '@angular/core/testing';
import { SensitiveValue } from './sensitive-value';

describe('SensitiveValue', () => {
  function setup(value: string | null = '0123456789') {
    const fixture = TestBed.createComponent(SensitiveValue);
    fixture.componentRef.setInput('value', value);
    fixture.componentRef.setInput('recordKey', 'record-1');
    fixture.detectChanges();
    return fixture;
  }

  it('hides the complete value until explicitly revealed and can hide it again', () => {
    const fixture = setup();
    expect(fixture.nativeElement.textContent).not.toContain('0123456789');
    expect(fixture.nativeElement.innerHTML).not.toContain('0123456789');
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('0123456789');
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('0123456789');
  });

  it('hides again when switching records, including identical values', () => {
    const fixture = setup();
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    fixture.componentRef.setInput('recordKey', 'record-2');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('0123456789');
  });

  it('hides again when the value changes', () => {
    const fixture = setup();
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    fixture.componentRef.setInput('value', '9876543210');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('9876543210');
  });

  it('does not offer reveal for missing data', () => {
    const fixture = setup(null);
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
    expect(fixture.nativeElement.textContent.trim()).toBe('—');
  });
});
