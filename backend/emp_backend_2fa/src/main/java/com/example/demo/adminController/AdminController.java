package com.example.demo.adminController;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.adminModel.AdminModel;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.service.AdminService;
import com.example.demo.service.TwoFactorService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final TwoFactorService twoFactorService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService service, JwtUtil jwtUtil, TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
        this.adminService = service;
        this.jwtUtil = jwtUtil;
    }

    // -----------------------
    // Login (2FA-aware)
    // -----------------------
    @PostMapping("/admin/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String name = credentials.get("adminName");
        String password = credentials.get("adminPassword");

        if (name == null || name.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "adminName and adminPassword are required"
            ));
        }

        Optional<AdminModel> optionalAdmin = adminService.login(name, password);
        if (optionalAdmin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Invalid credentials"
            ));
        }

        AdminModel admin = optionalAdmin.get();

        // If 2FA enabled -> return short-lived preAuth token instead of final JWT
        boolean twoFaEnabled = false;
        try {
            // support either isTwoFactorEnabled() or getTwoFactorEnabled() depending on your model
            // handle both possibilities defensively
            try {
                twoFaEnabled = (boolean) AdminModel.class.getMethod("isTwoFactorEnabled").invoke(admin);
            } catch (NoSuchMethodException e) {
                // fallback to getTwoFactorEnabled()
                try {
                    twoFaEnabled = (boolean) AdminModel.class.getMethod("getTwoFactorEnabled").invoke(admin);
                } catch (NoSuchMethodException ex) {
                    // if neither exists, assume false (or change accordingly)
                    twoFaEnabled = false;
                }
            }
        } catch (Exception ex) {
            // reflection failed — default to false (but log in real app)
            twoFaEnabled = false;
        }

        if (twoFaEnabled) {
            // generate a short-lived preAuth token (JwtUtil must implement this)
            String preAuthToken = jwtUtil.generatePreAuthToken(admin.getAdminName());
            return ResponseEntity.ok(Map.of(
                    "twoFactorRequired", true,
                    "preAuthToken", preAuthToken,
                    "message", "2FA required"
            ));
        }

        // Normal login — issue final JWT
        String token = jwtUtil.generateToken(admin.getAdminName(), admin.getRole());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Login successful",
                "token", token,
                "role", admin.getRole(),
                "username", admin.getAdminName()
        ));
    }

    // -----------------------
    // Register
    // -----------------------
    @PostMapping("/admin/register")
    public ResponseEntity<?> register(@RequestBody AdminModel admin) {
        if (admin == null || admin.getAdminName() == null || admin.getAdminName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid admin payload"));
        }

        Optional<AdminModel> existing = adminService.getByName(admin.getAdminName());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Username already exists!"
            ));
        }

        AdminModel savedAdmin = adminService.register(admin);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Registration successful",
                "id", savedAdmin.getAdminID(),
                "username", savedAdmin.getAdminName(),
                "role", savedAdmin.getRole()
        ));
    }

    // -----------------------
    // Forgot Password
    // -----------------------
    @PostMapping("/admin/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String adminName = payload.get("adminName");
        String newPassword = payload.get("newPassword");

        if (adminName == null || newPassword == null || adminName.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "adminName and newPassword required"));
        }

        return adminService.resetPassword(adminName, newPassword)
                .map(updatedAdmin -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Password reset successful",
                        "username", updatedAdmin.getAdminName()
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "error",
                        "message", "Admin not found"
                )));
    }

    // -----------------------
    // Generate 2FA secret + otpAuthUrl (authenticated)
    // -----------------------
    @PostMapping("/2fa/generate")
    public ResponseEntity<?> generate2FaSecret(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }

        String username = principal.getName();
        Optional<AdminModel> userOpt = adminService.getByName(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        String secret = twoFactorService.generateSecret();
        // Save secret (but do NOT enable until confirmed)
        adminService.saveTwoFaSecret(username, secret);

        String otpAuthUrl = twoFactorService.getOtpAuthURL("MyApp", username, secret);
        return ResponseEntity.ok(Map.of("secret", secret, "otpAuthUrl", otpAuthUrl));
    }

    
 // -----------------------
 // Confirm 2FA setup by verifying code (authenticated)
 // -----------------------
 @PostMapping("/2fa/confirm")
 public ResponseEntity<?> confirm2Fa(@RequestBody Map<String, String> body, Principal principal) {
     if (principal == null || principal.getName() == null) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
     }
     String username = principal.getName();

     String codeStr = body == null ? null : body.get("code");
     if (codeStr == null || codeStr.isBlank()) {
         return ResponseEntity.badRequest().body(Map.of("message", "code is required"));
     }

     Optional<AdminModel> userOpt = adminService.getByName(username);
     if (userOpt.isEmpty()) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
     }
     AdminModel user = userOpt.get();
     String secret = user.getTwoFactorSecret();
     if (secret == null || secret.isBlank()) {
         return ResponseEntity.badRequest().body(Map.of("message", "2FA not initialized for this user"));
     }

     int code;
     try {
         code = Integer.parseInt(codeStr.trim());
     } catch (NumberFormatException e) {
         return ResponseEntity.badRequest().body(Map.of("message", "Invalid code format"));
     }

     boolean ok = twoFactorService.verifyCode(secret, code);
     if (ok) {
         adminService.enableTwoFa(username);
         // return username + twoFactorEnabled flag to make frontend explicit
         return ResponseEntity.ok(Map.of(
             "message", "2FA enabled",
             "username", username,
             "twoFactorEnabled", true
         ));
     } else {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid 2FA code"));
     }
 }

 // -----------------------
 // Verify 2FA (preAuthToken + code) -> issue final JWT
 // -----------------------
 @PostMapping("/2fa/verify")
 public ResponseEntity<?> verify2Fa(@RequestBody(required = false) Map<String, String> body,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
     // Try body first
     String preAuthToken = (body != null) ? body.get("preAuthToken") : null;
     String codeStr = (body != null) ? body.get("code") : null;

     // If preAuthToken not provided in body, check Authorization header
     if ((preAuthToken == null || preAuthToken.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
         preAuthToken = authHeader.substring(7);
     }

     if (preAuthToken == null || preAuthToken.isBlank() || codeStr == null || codeStr.isBlank()) {
         return ResponseEntity.badRequest().body(Map.of("message", "preAuthToken and code are required"));
     }

     // validate preAuth token
     if (!jwtUtil.validatePreAuthToken(preAuthToken)) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired preAuth token"));
     }

     String username = jwtUtil.extractUsername(preAuthToken);
     Optional<AdminModel> userOpt = adminService.getByName(username);
     if (userOpt.isEmpty()) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
     }
     AdminModel user = userOpt.get();

     int code;
     try {
         code = Integer.parseInt(codeStr.trim());
     } catch (NumberFormatException e) {
         return ResponseEntity.badRequest().body(Map.of("message", "Invalid code format"));
     }

     boolean ok = twoFactorService.verifyCode(user.getTwoFactorSecret(), code);
     if (ok) {
         String token = jwtUtil.generateToken(username, user.getRole());
         // Return token plus username & role for frontend convenience
         return ResponseEntity.ok(Map.of(
             "token", token,
             "username", username,
             "role", user.getRole()
         ));
     } else {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid 2FA code"));
     }
 }

    
    @GetMapping("/admin/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }

        String username = principal.getName();
        Optional<AdminModel> userOpt = adminService.getByName(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        AdminModel user = userOpt.get();

        boolean twoFaEnabled = false;
        try {
            try {
                twoFaEnabled = (boolean) AdminModel.class.getMethod("isTwoFactorEnabled").invoke(user);
            } catch (NoSuchMethodException e) {
                try {
                    twoFaEnabled = (boolean) AdminModel.class.getMethod("getTwoFactorEnabled").invoke(user);
                } catch (NoSuchMethodException ex) {
                    twoFaEnabled = false;
                }
            }
        } catch (Exception ex) {
            twoFaEnabled = false;
        }

        return ResponseEntity.ok(Map.of(
            "username", user.getAdminName(),
            "role", user.getRole(),
            "twoFactorEnabled", twoFaEnabled
        ));
    }
}
