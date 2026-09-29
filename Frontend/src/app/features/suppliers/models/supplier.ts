export type SupplierStatus = 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED';

export interface Supplier {
  id: string;
  code: string;
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  taxCode?: string;
  bankAccount?: string;
  bankName?: string;
  notes?: string;
  status: SupplierStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateSupplierRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  taxCode?: string;
  bankAccount?: string;
  bankName?: string;
  notes?: string;
}

export interface UpdateSupplierRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  country?: string;
  taxCode?: string;
  bankAccount?: string;
  bankName?: string;
  notes?: string;
  status: SupplierStatus;
}
