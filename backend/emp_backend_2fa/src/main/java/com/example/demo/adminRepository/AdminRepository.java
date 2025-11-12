package com.example.demo.adminRepository;

import com.example.demo.adminModel.AdminModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminModel, Long> {
    
    // Admin को username से ढूंढने के लिए
    Optional<AdminModel> findByAdminName(String adminName);
}
