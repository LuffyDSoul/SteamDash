import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserLite { id: string; displayName: string; avatarUrl?: string; }

@Injectable({ providedIn: 'root' })
export class UsersService {
  private http = inject(HttpClient);
  private baseUrl = '/bff';

  searchUsers(q: string): Observable<UserLite[]> {
    const params = new HttpParams().set('q', q).set('limit', 10);
    return this.http.get<UserLite[]>(`${this.baseUrl}/users/search`, { params });
  }
  
}
