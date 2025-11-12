import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin-login',
  templateUrl: './admin-login.component.html'
})
export class AdminLoginComponent implements OnInit {
  adminName = '';
  adminPassword = '';
  otp = '';
  showOtp = false;
  loading = false;
  errorMsg = '';
  remember: boolean = false;

  private sub?: Subscription;

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    if (this.auth.isTwoFactorPending()) this.showOtp = true;
  }

  // Clean up any subscriptions if component destroyed
  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  onLogin() {
    this.errorMsg = '';

    if (!this.adminName.trim() || !this.adminPassword) {
      this.errorMsg = 'Username & password required';
      return;
    }

    // start loading
    this.loading = true;

    this.sub = this.auth.login(this.adminName.trim(), this.adminPassword).subscribe({
      next: (res: any) => {
        console.log('DEBUG: login response ->', res);

        // CASE A: Backend asks for OTP immediately (user already had 2FA enabled)
        if (res?.twoFactorRequired) {
          const pre = res.preAuthToken || res.preauthToken || res.preAuth;
          if (pre) {
            // ensure AuthService.savePreAuthToken is public
            this.auth.savePreAuthToken(pre);
            console.log('Saved preauth token', pre);
          }
          this.showOtp = true;
          this.loading = false;
          return;
        }

        // CASE B: Backend returned final JWT token (normal login)
        if (res?.token) {
          // Save token first so subsequent profile call is authenticated
          this.auth.saveToken(res.token);

          // If backend explicitly includes twoFactorEnabled flag, use it
          if (res.hasOwnProperty('twoFactorEnabled')) {
            const enabled = !!res.twoFactorEnabled;
            this.loading = false;
            if (!enabled) {
              this.router.navigate(['/enable-2fa']).catch(err => console.error('Nav error', err));
            } else {
              this.router.navigate(['/home']).catch(err => console.error('Nav error', err));
            }
            return;
          }

          // Fallback: call profile endpoint to determine 2FA status
          this.auth.isTwoFactorEnabledForCurrentUser().subscribe({
    next: (enabled: boolean) => {
      this.loading = false;
      if (!enabled) this.router.navigate(['/enable-2fa']); else this.router.navigate(['/home']);
    },
    error: (err) => {
      // profile check failed — decide policy: redirect to enable-2fa OR home
      console.warn('Profile check failed', err);
      this.loading = false;
      // <-- safer choice: if profile check fails, assume enable-2fa needed (optional)
      this.router.navigate(['/enable-2fa']);
    }
  });

  return;
}

        // Any other response
        this.loading = false;
        this.errorMsg = res?.message || 'Login failed';
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Login error';
        console.error('Login error', err);
      }
    });
  }

  onVerifyOtp() {
    this.errorMsg = '';
    const otpTrim = this.otp?.trim() ?? '';
    if (!/^\d{6}$/.test(otpTrim)) {
      this.errorMsg = 'Invalid OTP';
      return;
    }

    this.loading = true;
    this.sub = this.auth.verifyTwoFactor(otpTrim).subscribe({
      next: (res: any) => {
        // auth.verifyTwoFactor() should save final token in AuthService on success
        this.auth.clearPreAuthToken();
        this.loading = false;
        this.router.navigate(['/home']).catch(err => console.error('Nav error', err));
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = err?.error?.message || 'Invalid OTP';
        console.error('Verify OTP error', err);
      }
    });
  }
  // add this inside the component class
isValidOtp(): boolean {
  return !!this.otp && /^[0-9]{6}$/.test(this.otp.trim());
}



  cancelOtp() {
    this.auth.clearPreAuthToken();
    this.showOtp = false;
    this.otp = '';
    this.errorMsg = '';
  }
}
