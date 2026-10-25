export type RfqRole = 'MAKER' | 'CHECKER' | 'ADMIN';

export type RfqStatus =
  | 'DRAFT'
  | 'PENDING_RFQ_APPROVAL'
  | 'RETURNED'
  | 'SENT'
  | 'RECEIVING_QUOTES'
  | 'CLOSED_QUOTES'
  | 'COMPARING'
  | 'PENDING_RESULT_APPROVAL'
  | 'SELECTED_SUPPLIER'
  | 'PO_CREATED'
  | 'REJECTED'
  | 'CANCELED'
  | 'EXPIRED';

export type RfqCriterion = 'PRICE' | 'TECHNICAL' | 'DELIVERY' | 'PAYMENT' | 'REPUTATION';
export type RfqTechnicalStatus = 'PASS' | 'PARTIAL' | 'FAIL';

export interface RfqItem {
  id: string;
  procurementItemId?: string;
  sku: string;
  name: string;
  category?: string;
  quantity: number;
  unit: string;
  budgetUnitPrice: number;
  active: boolean;
}

export interface QuoteScore {
  price: number;
  technical: number;
  delivery: number;
  payment: number;
  reputation: number;
}

export interface SupplierQuote {
  id: string;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  round: number;
  submittedAt: string;
  validUntil: string;
  unitPrices: Record<string, number | null>;
  shipping: number;
  taxAmount: number;
  otherCost: number;
  deliveryDays: number;
  paymentTerms: string;
  warranty: string;
  validityDays: number;
  technicalStatus: RfqTechnicalStatus;
  score: QuoteScore;
  totalScore?: number;
  rank?: number;
}

export interface RfqHistoryEntry {
  actor: string;
  action: string;
  at: string;
  detail?: string;
}

export interface RfqInvite {
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  status: 'INVITED' | 'RESPONDED' | 'DECLINED' | 'REVOKED';
  invitedAt: string;
  respondedAt?: string;
}

export interface RfqRecord {
  id: string;
  code: string;
  title: string;
  procurementTicketId: string;
  prCode: string;
  department: string;
  createdBy: string;
  creatorName: string;
  createdAt: string;
  deadline: string;
  validityDays: number;
  deliveryLocation: string;
  deliveryWindow: string;
  estimatedValue: number;
  status: RfqStatus;
  invitedSupplierIds: string[];
  invitedSupplierCount: number;
  invitedSuppliers: RfqInvite[];
  respondedSupplierCount: number;
  items: RfqItem[];
  quotes: SupplierQuote[];
  history: RfqHistoryEntry[];
  reviewedAt?: string;
  reviewedBy?: string;
  chosenSupplierId?: string;
  chosenSupplierName?: string;
  selectionReason?: string;
  decisionReason?: string;
  itemWinners?: Record<string, string>;
  itemReasons?: Record<string, string>;
  exceptionReason?: string;
  weights: RfqScoreWeights;
}

export interface RfqSupplier {
  id: string;
  code: string;
  name: string;
  status: 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED';
  blacklisted: boolean;
}

export interface RfqScoreWeights {
  price: number;
  technical: number;
  delivery: number;
  payment: number;
  reputation: number;
}

export interface RfqSearchParams {
  keyword?: string;
  status?: RfqStatus | '';
  department?: string;
  createdBy?: string;
  createdFrom?: string;
  createdTo?: string;
  page: number;
  size: number;
}

export interface RfqSummaryDto {
  id: string;
  rfqCode: string;
  title: string;
  procurementTicketId: string;
  procurementTicketCode: string;
  department?: string;
  respondedSupplierCount: number;
  invitedSupplierCount: number;
  deadline: string;
  estimatedValue: number;
  status: RfqStatus;
  createdBy: string;
  creatorName: string;
  createdAt: string;
}

export interface RfqDetailDto extends RfqSummaryDto {
  procurementTicketTitle: string;
  quoteValidityDays: number;
  deliveryLocation?: string;
  deliveryWindow?: string;
  currency: string;
  submittedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewReason?: string;
  selectedSupplierId?: string;
  selectedSupplierName?: string;
  selectionReason?: string;
  exceptionReason?: string;
  items: Array<{
    id: string;
    procurementItemId?: string;
    sku?: string;
    itemName: string;
    category?: string;
    quantity: number;
    unit?: string;
    budgetUnitPrice: number;
    active: boolean;
  }>;
  invitedSuppliers: RfqInvite[];
  scoreCriteria: Array<{
    criterion: RfqCriterion;
    weightPercent: number;
  }>;
  quoteRounds: Array<{
    id: string;
    supplierId: string;
    supplierCode: string;
    supplierName: string;
    roundNumber: number;
    status: string;
    submittedAt: string;
    validUntil?: string;
    deliveryDays?: number;
    paymentTerms?: string;
    warranty?: string;
    technicalStatus?: RfqTechnicalStatus;
    subtotalBeforeTax: number;
    taxAmount: number;
    shippingCost: number;
    otherCost: number;
    totalAfterTax: number;
    totalScore?: number;
    rank?: number;
    incomplete: boolean;
    items: Array<{ 
      rfqItemId: string; 
      quoted: boolean; 
      unitPrice?: number; 
      note?: string 
    }>;
    scores: Array<{
      criterion: RfqCriterion;
      score: number;
      note?: string;
      updatedBy?: string;
      updatedAt?: string;
    }>;
  }>;
  itemAwards: Array<{
    rfqItemId: string;
    supplierId: string;
    supplierName: string;
    reason?: string;
    selectedBy: string;
    selectedAt: string;
  }>;
  history: Array<{
    id: string;
    actorUsername: string;
    action: string;
    details?: Record<string, unknown>;
    createdAt: string;
  }>;
}

export interface RfqItemRequest {
  procurementItemId?: string | null;
  sku?: string;
  itemName: string;
  category?: string;
  quantity: number;
  unit?: string;
  budgetUnitPrice: number;
}

export interface RfqScoreCriterionRequest {
  criterion: RfqCriterion;
  weightPercent: number;
}

export interface CreateRfqRequest {
  procurementTicketId: string;
  title: string;
  department?: string;
  deadline: string;
  quoteValidityDays: number;
  deliveryLocation?: string;
  deliveryWindow?: string;
  supplierIds: string[];
  exceptionReason?: string;
  items: RfqItemRequest[];
  scoreCriteria: RfqScoreCriterionRequest[];
  submitForApproval: boolean;
}

export type UpdateRfqRequest = Omit<CreateRfqRequest, 'procurementTicketId' | 'submitForApproval'>;
export interface RfqDecisionRequest {
  decision: 'APPROVE' | 'RETURN' | 'REJECT';
  reason: string;
}
export interface RfqActionRequest {
  reason: string;
}
export interface UpdateRfqQuoteScoresRequest {
  scores: Array<{ criterion: RfqCriterion; score: number; note?: string }>;
}
export interface SelectRfqWinnersRequest {
  supplierId?: string;
  reason?: string;
  itemWinners?: Array<{ rfqItemId: string; supplierId: string; reason?: string }>;
  submitForApproval: boolean;
}

