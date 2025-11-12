package com.example.demo.service;

import org.springframework.stereotype.Service;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Service
public class TwoFactorService {

    // Configure tolerance window (default is 1 step each side in some versions)
    private final GoogleAuthenticator gAuth;

    public TwoFactorService() {
        GoogleAuthenticatorConfigBuilder configBuilder = new GoogleAuthenticatorConfigBuilder()
                .setWindowSize(1); // allow ±1 step (30s each) — tune as needed
        GoogleAuthenticatorConfig config = configBuilder.build();
        this.gAuth = new GoogleAuthenticator(config);
    }

    // Generate a new secret (Base32)
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey(); // base32 secret
    }

    // Build otpauth URL for QR code (RFC style)
    public String getOtpAuthURL(String issuer, String accountName, String secret) {
        String encIssuer = urlEncode(issuer);
        String encAccount = urlEncode(accountName);
        // include optional parameters to be explicit
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            encIssuer, encAccount, secret, encIssuer
        );
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()).replace("+","%20");
        } catch(Exception e){
            return s;
        }
    }

    // Verify provided code (6-digit)
    public boolean verifyCode(String secret, int code) {
        if (secret == null || secret.isBlank()) return false;
        try {
            return gAuth.authorize(secret, code);
        } catch(Exception e) {
            // don't log secret; log only the fact of failure if needed
            return false;
        }
    }
}
