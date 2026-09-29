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