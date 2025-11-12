import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  adminName = '';
  newPassword = '';

  constructor(private authService: AuthService, private router: Router) {}

  onResetPassword() {
    this.authService.forgotPassword(this.adminName, this.newPassword).subscribe({
      next: () => {
        alert('✅ Password reset successful, please login again.');
        this.router.navigate(['/login']);
      },
      error: () => alert('❌ User not found')
    });
  }
}
