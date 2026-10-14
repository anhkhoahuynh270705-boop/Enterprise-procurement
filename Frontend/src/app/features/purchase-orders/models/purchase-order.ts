import { ProcurementTicket } from '../../procurement/model/procurement';

export type PurchaseOrderStatus =
  | 'PENDING_APPROVAL'
  | 'DRAFT'
  | 'SENT'
  | 'RECEIVED'
  | 'CANCELLED'
  | 'REJECTED';

export interface PurchaseOrderItem {
  id: string;
  procurementItemId: string;
  itemCode?: string;
  itemName: string;
  category?: string;
  quantity: number;
  unit?: string;
  unitPrice: number;
  totalPrice: number;
}

export interface PurchaseOrder {
  id: string;
  orderCode: string;
  ticketId: string;
  ticketCode: string;
  ticketTitle: string;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  status: PurchaseOrderStatus;
  expectedDeliveryDate?: string;
  notes?: string;
  totalAmount: number;
  currency: string;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewComment?: string;
  items: PurchaseOrderItem[];
}

export interface ReviewPurchaseOrderRequest {
  approved: boolean;
  comment?: string;
}

export interface CreatePurchaseOrderRequest {
  ticketId: string;
  supplierId: string;
  expectedDeliveryDate?: string;
  notes?: string;
  procurementItemIds: string[];
}

export interface PurchaseOrderApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

export type ApprovedTicket = ProcurementTicket;
