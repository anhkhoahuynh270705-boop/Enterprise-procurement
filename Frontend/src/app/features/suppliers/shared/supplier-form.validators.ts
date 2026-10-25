import { AbstractControl, ValidatorFn } from '@angular/forms';

const NAME_PATTERN = /^\p{L}+(?: \p{L}+)*$/u;
const COMPANY_NAME_PATTERN =
  /^(?:\p{L}+|\(\p{L}+(?: \p{L}+)*\))(?: (?:\p{L}+|\(\p{L}+(?: \p{L}+)*\)))*$/u;
const BANK_NAME_PATTERN = /^\p{L}+(?: \p{L}+)*(?: \(\p{L}+(?: \p{L}+)*\))?$/u;
const PHONE_PATTERN = /^(?=(?:[^0-9]*[0-9]){8,15}[^0-9]*$)[0-9 ]+$/;
const BANK_ACCOUNT_PATTERN = /^[0-9]{6,50}$/;

export function normalizeSupplierCompanyName(value: string): string {
  return value.normalize('NFKC').replace(/\p{Zs}/gu, ' ').trim();
}

export function supplierNameValidator(required = false): ValidatorFn {
  return (control) => {
    const value = String(control.value ?? '');
    const normalized = value.trim();
    if (!normalized) return required ? { required: true, name: true } : null;
    return NAME_PATTERN.test(normalized) && !/\s{2,}/.test(value) ? null : { name: true };
  };
}

export function supplierCompanyNameValidator(required = false): ValidatorFn {
  return (control) => {
    const value = String(control.value ?? '');
    const normalized = normalizeSupplierCompanyName(value);
    if (!normalized) return required ? { required: true, companyName: true } : null;
    return COMPANY_NAME_PATTERN.test(normalized)
      ? null
      : { companyName: true };
  };
}

export function bankNameValidator(): ValidatorFn {
  return (control) => {
    const value = String(control.value ?? '');
    const normalized = value.trim();
    if (!normalized) return null;
    return BANK_NAME_PATTERN.test(normalized) && !/\s{2,}/.test(value) ? null : { bankName: true };
  };
}

export function phoneValidator(): ValidatorFn {
  return (control) => {
    const value = String(control.value ?? '');
    return !value.trim() || PHONE_PATTERN.test(value) ? null : { pattern: true };
  };
}

export function bankAccountValidator(): ValidatorFn {
  return (control) => {
    const value = String(control.value ?? '');
    return !value || BANK_ACCOUNT_PATTERN.test(value) ? null : { bankAccount: true };
  };
}

export const contactRequiredValidator: ValidatorFn = (control) => {
  const email = String(control.get('email')?.value ?? '').trim();
  const phone = String(control.get('phone')?.value ?? '').trim();
  return email || phone ? null : { contactRequired: true };
};

export const bankDetailsValidator: ValidatorFn = (control) => {
  const bankName = String(control.get('bankName')?.value ?? '').trim();
  const bankAccount = String(control.get('bankAccount')?.value ?? '').trim();
  return Boolean(bankName) === Boolean(bankAccount) ? null : { bankDetailsIncomplete: true };
};

export const trimmed =
  (validator: ValidatorFn): ValidatorFn =>
  (control) =>
    validator({ value: String(control.value ?? '').trim() } as AbstractControl);

export const contractDatesValidator: ValidatorFn = (control) => {
  const startDate = control.get('startDate')?.value as string | undefined;
  const endDate = control.get('endDate')?.value as string | undefined;
  const signedDate = control.get('signedDate')?.value as string | undefined;
  if (startDate && endDate && endDate < startDate)
    return {
      contractDateRange: true,
    };
  if (startDate && signedDate && signedDate > startDate)
    return {
      contractSignedDate: true,
    };
  return null;
};

export const trimmedValidators = (...Validators: ValidatorFn[]): ValidatorFn[] =>
  Validators.map((v) => trimmed(v));
