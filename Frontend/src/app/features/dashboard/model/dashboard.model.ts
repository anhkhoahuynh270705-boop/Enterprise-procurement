import { ProcurementTicket } from '../../procurement/model/procurement';

export interface DashboardFilters {
  supplier?: string;
  product?: string;
  department?: string;
  status?: string;
  month?: string;
}
interface Point { key: string; label: string; value: number; }

export interface DashboardReport {
  totalUsers: number;
  activeUsers: number;
  totalTickets: number;
  pendingTickets: number;
  approvedTickets: number;
  rejectedTickets: number;
  totalProcurementAmount: number;
  approvalRate: number;
  statusCounts: number[];
  departmentSummary: Point[];
  monthlySummary: Point[];
  supplierSummary: Point[];
  recentTickets: ProcurementTicket[];
  currencies: string[];
}

export interface DashboardSummaryItem {
  key: string;
  label: string;
  value: number;
}