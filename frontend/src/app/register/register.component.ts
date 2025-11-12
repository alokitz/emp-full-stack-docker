import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  adminName = '';
  adminPassword = '';
  role = '';

  constructor(private authService: AuthService, private router: Router) {}

  onRegister() {
    if (!this.role) {
      alert('❌ Please select a role!');
      return;
    }

    const newAdmin = {
      adminName: this.adminName,
      adminPassword: this.adminPassword,
      role: this.role
    };

    this.authService.register(newAdmin).subscribe({
      next: (res) => {
        alert('✅ Registration successful, please login now.');
        this.router.navigate(['/login']);
      },
      error: () => alert('❌ Registration failed, username may already exist.')
    });
  }
}
