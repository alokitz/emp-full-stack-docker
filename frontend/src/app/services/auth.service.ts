// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  [x: string]: any;
  private baseUrl = 'http://localhost:8080/api/v1';

   private TOKEN_KEY = 'jwt_token';
  private PREAUTH_KEY = 'preauth_token';
  private ROLE_KEY = 'role';
  private USERNAME_KEY = 'adminName';

  constructor(private http: HttpClient) {}

  login(adminName: string, adminPassword: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/admin/login`, { adminName, adminPassword }).pipe(
      tap(res => {
        console.log('DEBUG: login response ->', res);
        if (res?.token) {
          this.saveToken(res.token);
          if (res.role) localStorage.setItem(this.ROLE_KEY, res.role);
          if (res.username) localStorage.setItem(this.USERNAME_KEY, res.username);
          this.clearPreAuthToken();
        } else if (res?.twoFactorRequired) {
          const pre = res.preAuthToken || res.preauthToken || res.preAuth;
          if (pre) this.savePreAuthToken(pre);
        }
      })
    );
  }

  // Step 2: verify preAuthToken + code -> get final JWT
   verifyTwoFactor(code: string) {
    const preAuthToken = this.getPreAuthToken();
    console.log('DEBUG: verifyTwoFactor sending', { preAuthToken, code });
    return this.http.post<any>(`${this.baseUrl}/2fa/verify`, { preAuthToken, code }).pipe(
     tap(res => {
  if (res?.token) {
    this.saveToken(res.token);
    if (res.username) localStorage.setItem(this.USERNAME_KEY, res.username);
    if (res.role) localStorage.setItem(this.ROLE_KEY, res.role);
    this.clearPreAuthToken();
  }
})

    );
  }

  // Generate secret + otpAuthUrl (authenticated endpoint)
    generateTwoFactor(): Observable<{ secret: string; otpAuthUrl: string }> {
    return this.http.post<any>(`${this.baseUrl}/2fa/generate`, {}).pipe(
      map(res => ({
        secret: res?.secret ?? res?.secretKey ?? res?.secret_key ?? '',
        otpAuthUrl: res?.otpAuthUrl ?? res?.otpAuthURL ?? res?.otpauth_url ?? res?.otp_url ?? ''
      }))
    );
  }

  // Confirm enabling 2FA by sending code
  confirmTwoFactor(code: string) {
    return this.http.post<any>(`${this.baseUrl}/2fa/confirm`, { code });
  }

  // register & forgot password (if your UI uses)
  register(adminData: { adminName: string; adminPassword: string; role: string }) {
    return this.http.post<any>(`${this.baseUrl}/admin/register`, adminData);
  }
  forgotPassword(adminName: string, newPassword: string) {
    return this.http.post<any>(`${this.baseUrl}/admin/forgot-password`, { adminName, newPassword });
  }

  // helpers
  saveToken(token: string) { localStorage.setItem(this.TOKEN_KEY, token); }
  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }
  public savePreAuthToken(pre: string) { localStorage.setItem(this.PREAUTH_KEY, pre); }
  getPreAuthToken(): string | null { return localStorage.getItem(this.PREAUTH_KEY); }
  clearPreAuthToken() { localStorage.removeItem(this.PREAUTH_KEY); }
  isTwoFactorPending(): boolean { return !!this.getPreAuthToken(); }
  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    localStorage.removeItem(this.USERNAME_KEY);
    this.clearPreAuthToken();
  }
  isLoggedIn(): boolean { return !!this.getToken(); }
  getRole(): string | null { return localStorage.getItem(this.ROLE_KEY); }
  getUsername(): string | null { return localStorage.getItem(this.USERNAME_KEY); }

  // in AuthService
isTwoFactorEnabledForCurrentUser(): Observable<boolean> {
  // requires JWT saved and interceptor to add Authorization header
  return this.http.get<any>(`${this.baseUrl}/admin/me`).pipe(
    map(res => !!res?.twoFactorEnabled)
  );
}

}