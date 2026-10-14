export interface AuditLogDetails {
  userAgent?: string;
  method?: string;
  statusCode?: number;
  responseTimeMs?: number;
  payloadSummary?: string;
}

export interface AuditLogItem {
  id: string;
  logCode: string;
  occurredAt: string;
  username: string;
  userFullName: string;
  action: string;
  module: 'AUTH' | 'PROCUREMENT' | 'CONTRACT' | 'SUPPLIER' | 'SYSTEM' | 'SECURITY';
  moduleLabel: string;
  status: 'SUCCESS' | 'FAILED' | 'WARNING';
  description: string;
  details?: AuditLogDetails;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

export interface AuditSearchParams {
  module?: string;
  status?: string;
  dateRange?: string;
  keyword?: string;
  page?: number;
  size?: number;
}