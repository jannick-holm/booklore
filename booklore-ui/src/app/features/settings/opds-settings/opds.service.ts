import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_CONFIG} from '../../../core/config/api-config';

export type OpdsSortOrder = 'RECENT' | 'TITLE_ASC' | 'TITLE_DESC' | 'AUTHOR_ASC' | 'AUTHOR_DESC' | 'SERIES_ASC' | 'SERIES_DESC' | 'RATING_ASC' | 'RATING_DESC';

export interface EpubOptimizationSettings {
  epubOptimizationEnabled: boolean;
  epubJpegQuality: number;
  epubEnableGrayscale: boolean;
  epubResizeImages: boolean;
  epubMaxImageWidth: number;
  epubMaxImageHeight: number;
  epubConversionLimitMb: number;
}

export interface OpdsUserV2CreateRequest {
  username: string;
  password: string;
  sortOrder?: OpdsSortOrder;
  epubOptimizationEnabled?: boolean;
  epubJpegQuality?: number;
  epubEnableGrayscale?: boolean;
  epubResizeImages?: boolean;
  epubMaxImageWidth?: number;
  epubMaxImageHeight?: number;
  epubConversionLimitMb?: number;
}

export interface OpdsUserV2UpdateRequest {
  sortOrder: OpdsSortOrder;
  epubOptimizationEnabled?: boolean;
  epubJpegQuality?: number;
  epubEnableGrayscale?: boolean;
  epubResizeImages?: boolean;
  epubMaxImageWidth?: number;
  epubMaxImageHeight?: number;
  epubConversionLimitMb?: number;
}

export interface OpdsUserV2 {
  id: number;
  userId: number;
  username: string;
  sortOrder?: OpdsSortOrder;
  epubOptimizationEnabled: boolean;
  epubJpegQuality: number;
  epubEnableGrayscale: boolean;
  epubResizeImages: boolean;
  epubMaxImageWidth: number;
  epubMaxImageHeight: number;
  epubConversionLimitMb: number;
}

@Injectable({
  providedIn: 'root'
})
export class OpdsService {

  private readonly baseUrl = `${API_CONFIG.BASE_URL}/api/v2/opds-users`;
  private http = inject(HttpClient);

  getUser(): Observable<OpdsUserV2[]> {
    return this.http.get<OpdsUserV2[]>(this.baseUrl);
  }

  createUser(user: OpdsUserV2CreateRequest): Observable<OpdsUserV2> {
    return this.http.post<OpdsUserV2>(this.baseUrl, user);
  }

  updateUser(id: number, request: OpdsUserV2UpdateRequest): Observable<OpdsUserV2> {
    return this.http.patch<OpdsUserV2>(`${this.baseUrl}/${id}`, request);
  }

  deleteCredential(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
