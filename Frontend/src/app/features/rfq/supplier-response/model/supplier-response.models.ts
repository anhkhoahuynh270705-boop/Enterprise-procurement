import { RfqTechnicalStatus } from '../../model/rfq.models';

export interface SupplierInvitationViewDto {
  rfqCode: string;
  title: string;
  deadline: string;
  deliveryLocation?: string;
  deliveryWindow?: string;
  currency: string;
  items: Array<{
    rfqItemId: string;
    sku?: string;
    itemName: string;
    quantity: number;
    unit?: string;
  }>;
}

export interface SupplierQuoteItemDraft {
  rfqItemId: string;
  sku: string;
  itemName: string;
  quantity: number;
  unit: string;
  quoted: boolean;
  unitPrice: number | null;
  note: string;
}


export interface SupplierQuoteSubmissionRequest {
  token: string;
  quote: {
    validUntil: string;
    deliveryDays?: number;
    paymentTerms?: string;
    warranty?: string;
    technicalStatus: RfqTechnicalStatus;
    taxAmount: number;
    shippingCost: number;
    otherCost: number;
    items: Array<{
      rfqItemId: string;
      quoted: boolean;
      unitPrice?: number;
      note?: string;
    }>;
  };
}
