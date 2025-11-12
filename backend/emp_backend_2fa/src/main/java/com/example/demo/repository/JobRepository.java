package com.example.demo.repository;

import com.example.demo.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    // JpaRepository already provides: findById, findAll, save, delete, etc.

    // Extra helper (optional) - find by title
    Optional<Job> findByTitleIgnoreCase(String title);
}
