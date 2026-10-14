import { AbstractControl } from "@angular/forms";

export function pastDate(control: AbstractControl) {
  if (!control.value) return null;
  const value = control.value as string;
  const parsed = new Date(value + 'T00:00:00Z');
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  return /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    !isNaN(parsed.getTime()) &&
    parsed.toISOString().slice(0, 10) === value &&
    value <= today
    ? null
    : { date: true };
}