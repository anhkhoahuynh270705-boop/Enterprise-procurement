export type ProcurementStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type ProcurementPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface ProcurementItem {
  id?: string;
  itemCode?: string;
  itemName: string;
  category?: string;
  quantity: number;
  unit?: string;
  unitPrice: number;
  totalPrice?: number;
  supplierName?: string;
  notes?: string;
}

export interface ProcurementTicket {
  id: string;
  ticketCode: string;
  title: string;
  department?: string;
  reason?: string;
  totalAmount: number;
  currency: string;
  status: ProcurementStatus;
  priority: ProcurementPriority;
  makerUsername: string;
  checkerUsername?: string;
  checkerComment?: string;
  camundaProcessInstanceId?: string;
  camundaTaskId?: string;
  items: ProcurementItem[];
  createdAt: string;
  updatedAt?: string;
  approvedAt?: string;
}

export interface CreateProcurementTicketRequest {
  title: string;
  department?: string;
  reason?: string;
  currency?: string;
  priority?: ProcurementPriority;
  submitImmediately: boolean;
  items: ProcurementItem[];
}

export interface CheckerReviewRequest {
  approved: boolean;
  comment?: string;
}

export interface CamundaTask {
  taskId: string;
  taskName: string;
  processInstanceId: string;
  assignee?: string;
  createTime: string;
  ticket?: ProcurementTicket;
  processVariables?: Record<string, any>;
}

export interface ImportRowError {
  rowNumber: number;
  field: string;
  errorMessage: string;
}

export interface ImportResult {
  totalRows: number;
  successRows: number;
  errorRows: number;
  message: string;
  createdTicket?: ProcurementTicket;
  errors: ImportRowError[];
}

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResult<T> {
  content: T[];
  number: number;
  totalElements: number;
  totalPages: number;
  size: number;
}
