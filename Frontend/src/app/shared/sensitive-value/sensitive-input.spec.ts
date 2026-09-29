import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { SensitiveInput } from './sensitive-input';

@Component({
  imports: [SensitiveInput, ReactiveFormsModule],
  template: `<form [formGroup]="form"><app-sensitive-input label="số tài khoản">
    <input #sensitiveInput formControlName="account" />
  </app-sensitive-input></form>`,
})
class TestHost {
  form = new FormGroup({ account: new FormControl('0012345678') });
}

describe('SensitiveInput', () => {
  it('masks an editable value without changing the submitted data or losing leading zeros', () => {
    const fixture = TestBed.createComponent(TestHost);
    fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    expect(input.type).toBe('password');
    expect(input.value).toBe('0012345678');
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    input.value = '0098765432';
    input.dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(input.type).toBe('password');
    expect(fixture.componentInstance.form.getRawValue().account).toBe('0098765432');
  });
});
