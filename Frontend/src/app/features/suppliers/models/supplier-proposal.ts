export interface SupplierProposalRequest {
  name: string;
  taxCode: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  bankName?: string;
  bankAccount?: string;
  notes?: string;
  reason: string;
}

export type ProposalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface SupplierProposal extends SupplierProposalRequest {
  id: string;
  status: ProposalStatus;
  submittedBy: string;
  submittedAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewComment?: string;
  supplierId?: string;
}
