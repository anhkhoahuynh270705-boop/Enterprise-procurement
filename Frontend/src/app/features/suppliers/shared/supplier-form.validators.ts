import { AbstractControl, ValidatorFn } from '@angular/forms';

const NAME_PATTERN = /^\p{L}+(?: \p{L}+)*$/u;
const PHONE_PATTERN = /^[0-9]{9,15}$/;
const BANK_ACCOUNT_PATTERN = /^[0-9]{6,50}$/;

export function supplierNameValidator(required = false): ValidatorFn {
  return control => {
    const value = String(control.value ?? '');
    const normalized = value.trim();
    if (!normalized) return required ? { required: true, name: true } : null;
    return NAME_PATTERN.test(normalized) && !/\s{2,}/.test(value) ? null : { name: true };
  };
}

export function phoneValidator(): ValidatorFn {
  return control => {
    const value = String(control.value ?? '');
    return !value || PHONE_PATTERN.test(value) ? null : { pattern: true };
  };
}

export function bankAccountValidator(): ValidatorFn {
  return control => {
    const value = String(control.value ?? '');
    return !value || BANK_ACCOUNT_PATTERN.test(value) ? null : { bankAccount: true };
  };
}

export const contactRequiredValidator: ValidatorFn = control => {
  const email = String(control.get('email')?.value ?? '').trim();
  const phone = String(control.get('phone')?.value ?? '').trim();
  return email || phone ? null : { contactRequired: true };
};

export const bankDetailsValidator: ValidatorFn = control => {
  const bankName = String(control.get('bankName')?.value ?? '').trim();
  const bankAccount = String(control.get('bankAccount')?.value ?? '').trim();
  return Boolean(bankName) === Boolean(bankAccount) ? null : { bankDetailsIncomplete: true };
};

export const trimmed = (validator: ValidatorFn): ValidatorFn => control =>
  validator({ value: String(control.value ?? '').trim() } as AbstractControl);
