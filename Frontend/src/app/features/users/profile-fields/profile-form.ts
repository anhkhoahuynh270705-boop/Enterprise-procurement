import { FormBuilder, ValidatorFn, Validators } from '@angular/forms';

export const profileTextFields = [
  {
    name: 'avatarUrl',
    label: 'Avatar URL',
    max: 2048,
    type: 'url',
    error: 'Enter a valid HTTPS URL (maximum 2048 characters).',
  },
  {
    name: 'phone',
    label: 'Phone',
    max: 30,
    type: 'tel',
    error: 'Use digits, +, spaces, parentheses, periods or hyphens (maximum 30 characters).',
  },
  {
    name: 'employeeId',
    label: 'Employee ID',
    max: 50,
    type: 'text',
    error: 'Maximum 50 characters. Employee ID must be unique.',
  },
  {
    name: 'department',
    label: 'Department',
    max: 255,
    type: 'text',
    error: 'Maximum 255 characters.',
  },
  {
    name: 'jobTitle',
    label: 'Job title',
    max: 255,
    type: 'text',
    error: 'Maximum 255 characters.',
  },
  {
    name: 'officeLocation',
    label: 'Office location',
    max: 255,
    type: 'text',
    error: 'Maximum 255 characters.',
  },
  { name: 
    'manager', 
    label: 'Manager', 
    max: 255, 
    type: 'text',
    error: 'Maximum 255 characters.' 
  },
] as const;

export function todayDate(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

const httpsUrl: ValidatorFn = (control) => {
  const value = control.value?.trim();
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === 'https:' && !!url.hostname ? null : { httpsUrl: true };
  } catch {
    return { httpsUrl: true };
  }
};

const birthday: ValidatorFn = (control) => {
  const value = control.value;
  if (!value) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return { birthday: true };
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) &&
    date.toISOString().slice(0, 10) === value &&
    value <= todayDate()
    ? null
    : { birthday: true };
};

export function profileControls(fb: FormBuilder) {
  return {
    avatarUrl: fb.control('', [Validators.maxLength(2048), httpsUrl]),
    phone: fb.control('', [Validators.maxLength(30), Validators.pattern(/^[+\d\s().-]*$/)]),
    dateOfBirth: fb.control('', birthday),
    gender: fb.control<string | null>(
      null,
      Validators.pattern(/^(MALE|FEMALE|OTHER|UNDISCLOSED)$/),
    ),
    address: fb.control('', Validators.maxLength(500)),
    employeeId: fb.control('', Validators.maxLength(50)),
    department: fb.control('', Validators.maxLength(255)),
    jobTitle: fb.control('', Validators.maxLength(255)),
    officeLocation: fb.control('', Validators.maxLength(255)),
    manager: fb.control('', Validators.maxLength(255)),
    employeeStatus: fb.nonNullable.control('ACTIVE', [
      Validators.required,
      Validators.pattern(/^(ACTIVE|INACTIVE)$/),
    ]),
  };
}

export function normalizeProfile<T extends object>(value: T): T {
  const normalized = { ...value } as Record<string, unknown>;
  for (const field of [
    ...profileTextFields.map((field) => field.name),
    'dateOfBirth',
    'gender',
    'address',
  ]) {
    if (field in normalized)
      normalized[field] = (normalized[field] as string | null)?.trim() || null;
  }
  if ('employeeStatus' in normalized) normalized['employeeStatus'] ||= 'ACTIVE';
  return normalized as T;
}
