import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { profileTextFields, todayDate } from './profile-form';
import { SensitiveInput } from '../../../shared/sensitive-value/sensitive-input';

@Component({
  selector: 'app-user-profile-fields',
  imports: [ReactiveFormsModule, SensitiveInput],
  templateUrl: './profile-fields.html',
  styleUrl: './profile-fields.scss',
})
export class UserProfileFields {
  readonly form = input.required<FormGroup>();
  readonly fields = profileTextFields;
  readonly today = todayDate();
}
