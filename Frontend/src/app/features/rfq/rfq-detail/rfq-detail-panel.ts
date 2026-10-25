import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import type { RfqRecord } from '../model/rfq.models';
import type { RfqPage } from '../rfq-list/rfq-page';

@Component({
  selector: 'app-rfq-detail-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rfq-detail-panel.html',
  styleUrl: './rfq-detail-panel.scss',
})
export class RfqDetailPanel {
  readonly record = input<RfqRecord | null>(null);
  readonly page = input.required<RfqPage>();
}
