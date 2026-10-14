export type SupplierContractStatus = 'DRAFT' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';
export type EditableSupplierContractStatus = Exclude<SupplierContractStatus, 'EXPIRED'>;

export interface SupplierContract {
  id: string;
  contractNumber: string;
  title: string;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  signedDate?: string | null;
  startDate: string;
  endDate: string;
  amount: number;
  currency: string;
  status: SupplierContractStatus;
  notes?: string | null;
  attachmentName?: string | null;
  attachmentContentType?: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt?: string | null;
}

export interface SupplierContractRequest {
  title: string;
  supplierId: string;
  signedDate?: string;
  startDate: string;
  endDate: string;
  amount: number;
  currency: string;
  status: EditableSupplierContractStatus;
  notes?: string;
}
