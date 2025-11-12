package com.example.demo.adminModel;

import jakarta.persistence.*;

@Entity
@Table(name="admin")
public class AdminModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminID;

    @Column(nullable = false, unique = true)
    private String adminName;

    @Column(nullable = false)
    private String adminPassword;

    @Column(nullable = false)
    private String role; // ✅ ROLE_ADMIN, ROLE_HR, ROLE_EMPLOYEE

    // 🔹 Default constructor
    public AdminModel() {}

    // 🔹 Constructor with all fields
    public AdminModel(String adminName, String adminPassword, String role) {
        this.adminName = adminName;
        this.adminPassword = adminPassword;
        this.role = role;
    }

    // ✅ Getters & Setters
    public Long getAdminID() {
        return adminID;
    }

    public void setAdminID(Long adminID) {
        this.adminID = adminID;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Two-factor auth fields
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

}