import { Toast } from './shared/toast/toast';
import { ConfirmationDialog } from './shared/confirmation-dialog/confirmation-dialog';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  imports: [RouterOutlet, Toast, ConfirmationDialog],
  standalone: true,
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
}
