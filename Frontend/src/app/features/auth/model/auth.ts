export type AppRole = 'ADMIN' | 'USER' | 'CHECKER';
export interface CurrentUser {
    username: string;
    email?: string;
    role?: AppRole;
}

export interface ForgotPasswordRequest {
    email: string;
}

export interface LoginResponse {
    access_token: string;
    expires_in: number;
    refresh_expires_in: number;
    token_type: string;
    scope: string;
}

export interface UserResponse {
    avatarUrl?: string | null;
    phone?: string | null;
    dateOfBirth?: string | null;
    gender?: Gender | null;
    address?: string | null;
    employeeId?: string | null;
    department?: string | null;
    jobTitle?: string | null;
    officeLocation?: string | null;
    manager?: string | null;
    employeeStatus?: 'ACTIVE' | 'INACTIVE' | null;
    fullName?: string;
    id: string;
    username: string;
    email: string;
    firstName?: string;
    lastName?: string;
    role: string;
    enabled: boolean;
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNDISCLOSED';
export interface UpdateMyProfileRequest {
    avatarUrl: string | null;
    phone: string | null;
    dateOfBirth: string | null;
    gender: Gender | null;
    address: string | null;
}
