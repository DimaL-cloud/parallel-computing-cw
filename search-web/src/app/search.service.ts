import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080';

  searchFiles(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/filePaths`, {
      params: { search: query }
    });
  }
}
