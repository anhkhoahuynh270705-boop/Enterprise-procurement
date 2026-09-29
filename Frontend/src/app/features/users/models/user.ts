export interface UserProfileFields {
    avatarUrl?: string | null,
    phone?: string | null,
    dateOfBirth?: string | null,
    gender?: string | null,
    address?: string | null,
    employeeId?: string | null,
    department?: string | null,
    jobTitle?: string | null,
    officeLocation?: string | null,
    manager?: string | null,
    employeeStatus?: string | null,
}

export interface Users extends UserProfileFields {
    fullName?: string,
    id: string | number,
    username: string,
    email: string,
    firstName: string,
    lastName: string,
    role: string,
    enabled: boolean,
    createdAt?: string
}

export interface CreateUserRequest extends UserProfileFields {
    fullName?: string,
    username: string,
    email: string,
    password: string,
    firstName: string,
    lastName: string,
    role: string,
    enabled: boolean
}

export interface UpdateUserRequest extends UserProfileFields {
    fullName?: string,
    username: string,
    email: string,
    firstName: string,
    lastName: string,
    role: string,
    enabled: boolean,
    password?: string
}
