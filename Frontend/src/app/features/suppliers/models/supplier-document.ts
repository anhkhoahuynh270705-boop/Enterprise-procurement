export type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface SupplierOption {
    id: string;
    code: string;
    name: string;
}

export interface SupplierDocument {
    id: string;
    supplierId: string;
    supplierName: string;
    filename: string;
    contentType: string;
    size: number;
    status: DocumentStatus;
    submittedBy: string;
    submittedAt: string;
    reviewedBy: string | null;
    reviewedAt: string | null;
    reviewComment: string | null;
}