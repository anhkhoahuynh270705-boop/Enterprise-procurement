export interface CompanyDocument {
  id: string;
  title: string;
  category: string;
  description: string;
  filename: string;
  contentType: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface CompanyDocumentSearchParams {
  keyword?: string;
  category?: string;
  uploadedBy?: string;
  uploadedFrom?: string;
  uploadedTo?: string;
  fileType?: string;
  page: number;
  size: number;
}

export interface CompanyDocumentPage {
  content: CompanyDocument[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
