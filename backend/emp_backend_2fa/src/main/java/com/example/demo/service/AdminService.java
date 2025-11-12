package com.example.demo.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.adminModel.AdminModel;
import com.example.demo.adminRepository.AdminRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Login check using hashed password verification
    public Optional<AdminModel> login(String adminName, String adminPassword) {
        if (adminName == null || adminPassword == null) return Optional.empty();
        return adminRepo.findByAdminName(adminName)
                .filter(admin -> passwordEncoder.matches(adminPassword, admin.getAdminPassword()));
    }

    // Forgot password (reset) - securely hash new password
    public Optional<AdminModel> resetPassword(String adminName, String newPassword) {
        if (adminName == null || newPassword == null) return Optional.empty();
        return adminRepo.findByAdminName(adminName)
                .map(admin -> {
                    admin.setAdminPassword(passwordEncoder.encode(newPassword));
                    return adminRepo.save(admin);
                });
    }

    // Find admin by name
    public Optional<AdminModel> getByName(String adminName) {
        if (adminName == null) return Optional.empty();
        return adminRepo.findByAdminName(adminName);
    }

    // Register new admin - hash password before saving
    public AdminModel register(AdminModel admin) {
        if (admin == null) throw new IllegalArgumentException("admin must not be null");
        if (admin.getAdminPassword() == null) throw new IllegalArgumentException("password required");
        admin.setAdminPassword(passwordEncoder.encode(admin.getAdminPassword()));
        return adminRepo.save(admin);
    }

    // Save generated 2FA secret (do not enable yet)
    @Transactional
    public Optional<AdminModel> saveTwoFaSecret(String username, String secret) {
        if (username == null || secret == null) {
            log.warn("saveTwoFaSecret called with null username/secret");
            return Optional.empty();
        }
        Optional<AdminModel> userOpt = adminRepo.findByAdminName(username);
        if (userOpt.isEmpty()) {
            log.warn("saveTwoFaSecret: user not found -> '{}'", username);
            return Optional.empty();
        }
        AdminModel user = userOpt.get();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false);
        AdminModel saved = adminRepo.save(user);
        log.info("saveTwoFaSecret: saved secret for user='{}' id={}", username, saved.getAdminID());
        return Optional.of(saved);
    }


    // Enable two-factor after verification
    @Transactional
    public Optional<AdminModel> enableTwoFa(String username) {
        if (username == null) return Optional.empty();
        return adminRepo.findByAdminName(username).map(admin -> {
            admin.setTwoFactorEnabled(true);
            AdminModel saved = adminRepo.save(admin);
            log.info("2FA enabled for user={}", username); // do not log secret
            return saved;
        });
    }
}
