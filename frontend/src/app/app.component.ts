import { Component } from '@angular/core';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router'; // ✅ Router import करना जरूरी है

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'frontend';
  year = new Date().getFullYear();

  constructor(
    public authService: AuthService,
    private router: Router // ✅ Router inject करो
  ) {}

  onLogout(): void {
    // 🔹 AuthService से logout call
    this.authService.logout();

    // 🔹 अब redirect Home Page पर
    this.router.navigate(['/home']);

    // Optional: Console log (debug purpose)
    console.log('✅ Logged out successfully. Redirecting to Home Page...');
  }
}
