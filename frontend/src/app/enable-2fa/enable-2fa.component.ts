import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-enable-2fa',
  templateUrl: './enable-2fa.component.html',
  styleUrls: ['./enable-2fa.component.css']
})
export class Enable2faComponent implements OnInit {
  secret = '';
  otpAuthUrl = '';
  code: string = '';
  qrData = '';
  qrImage = '';
  loading = false;
  confirming = false;
  errorMsg = '';

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.errorMsg = 'आपको 2FA सक्षम करने के लिए पहले लॉगिन करना होगा।';
      this.loading = false;
      return;
    }

    // TEMP: mock test — set to false for real backend
    const mockQrTest = false;
    if (mockQrTest) {
      this.secret = 'JBSWY3DPEHPK3PXP';
      this.otpAuthUrl = 'otpauth://totp/TestApp:admin?secret=JBSWY3DPEHPK3PXP&issuer=TestApp';
      this.qrData = this.otpAuthUrl;
      QRCode.toDataURL(this.qrData, { width: 200 }).then(d => this.qrImage = d);
      return;
    }

    this.loading = true;
    this.auth.generateTwoFactor().subscribe({
      next: async (res: any) => {
        this.loading = false;
        this.secret = res.secret || '';
        this.otpAuthUrl = res.otpAuthUrl || '';
        this.qrData = this.otpAuthUrl || this.secret || '';
        if (this.qrData) {
          try {
            this.qrImage = await QRCode.toDataURL(this.qrData, { width: 200 });
          } catch (e) {
            console.error('Failed to create QR image', e);
            this.qrImage = '';
            this.errorMsg = 'QR बनाते समय त्रुटि हुई — secret नीचे दिया गया है।';
          }
        } else {
          this.errorMsg = 'Server response में otpAuthUrl/secret उपलब्ध नहीं है।';
        }
      },
      error: (err: any) => {
        this.loading = false;
        console.error('generateTwoFactor failed', err);
        this.errorMsg = err?.error?.message || '2FA key बनाते समय त्रुटि हुई।';
      }
    });
  }

  copySecret() {
    if (!this.secret) return;
    navigator.clipboard?.writeText(this.secret).then(() => alert('Secret copied')).catch(() => alert('Copy failed'));
  }

  isValidCode(): boolean {
    if (!this.code) return false;
    return /^\d{6}$/.test(this.code.trim());
  }

  confirm() {
    if (!this.isValidCode()) { alert('कृपया 6 अंकों का कोड डालें'); return; }
    this.confirming = true;
    this.auth.confirmTwoFactor(this.code.trim()).subscribe({
      next: () => { this.confirming = false; alert('2FA enabled'); },
      error: (err) => { this.confirming = false; alert(err?.error?.message || 'Invalid code'); }
    });
  }
}
