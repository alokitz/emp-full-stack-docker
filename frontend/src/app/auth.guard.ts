// src/app/auth.guard.ts
import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from './services/auth.service';
import { jwtDecode } from 'jwt-decode';   // ✅ सही import

interface DecodedToken {
  exp?: number;   // expiry field optional है
  [key: string]: any;
}

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    const token = this.auth.getToken();
    if (!token) {
      // 🔒 अगर token नहीं है → login पर भेज दो
      return this.router.createUrlTree(['/login']);
    }

    try {
      // ✅ token decode करके expiry check
      const decoded: DecodedToken = jwtDecode<DecodedToken>(token);
      const exp = decoded?.exp;

      if (exp && Date.now() >= exp * 1000) {
        // ❌ अगर token expire हो गया
        this.auth.logout();
        return this.router.createUrlTree(['/login']);
      }
    } catch (e) {
      console.error("❌ Invalid token:", e);
      this.auth.logout();
      return this.router.createUrlTree(['/login']);
    }

    return true; // ✅ सब ठीक है → route allow
  }
}
