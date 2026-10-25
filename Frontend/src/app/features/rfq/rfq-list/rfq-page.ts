import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RfqDetailPanel } from '../rfq-detail/rfq-detail-panel';
import { RfqFormDialog } from '../rfq-create/rfq-form-dialog';
import { RfqEditDialog } from '../rfq-edit/rfq-edit-dialog';
import { RfqPageWorkflowController } from '../rfq-workflow/rfq-page-workflow.controller';

@Component({
  selector: 'app-rfq-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RfqFormDialog, RfqEditDialog, RfqDetailPanel],
  templateUrl: './rfq-page.html',
  styleUrl: './rfq-page.scss',
})
export class RfqPage extends RfqPageWorkflowController {
  readonly pageContext = this;

  constructor() {
    super();
    this.initializePage();
  }
}
  