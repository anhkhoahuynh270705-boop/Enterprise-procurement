import { Injectable } from "@angular/core";
import { environment } from '../../../../environments/environment';
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { CreateUserRequest, UpdateUserRequest, Users } from "../models/user";

@Injectable({
    providedIn: 'root'
})

export class UserService {
    private apiUrl = environment.apiBaseUrl + '/users';
    constructor(
        private http: HttpClient
    ) { }

    //get
    getUsers(): Observable<Users[]> {
        return this.http.get<Users[]>(`${this.apiUrl}`);
    };
    getUsersById(id: string): Observable<Users> {
        return this.http.get<Users>(`${this.apiUrl}/${id}`);
    }
    //create
    addUser(data: CreateUserRequest): Observable<Users> {
        return this.http.post<Users>(`${this.apiUrl}`, data);
    };
    //update
    updateUser(id: string | number, data: UpdateUserRequest): Observable<Users> {
        return this.http.put<Users>(`${this.apiUrl}/${id}`, data);
    };
    //delete
    deleteUser(id: string | number): Observable<any> {
        return this.http.delete(`${this.apiUrl}/${id}`);
    };

    // toggle enabled/disabled
    toggleUserStatus(id: string | number): Observable<Users> {
        return this.http.patch<Users>(`${this.apiUrl}/${id}/toggle-status`, {});
    };
}



