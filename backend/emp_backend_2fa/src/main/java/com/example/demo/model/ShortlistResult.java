package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="shortlist_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortlistResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long jobId;
    private Long candidateId;
    private Double score;
    @Lob
    private String breakdownJson;
    private LocalDateTime createdAt = LocalDateTime.now();
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getJobId() {
		return jobId;
	}
	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
	public Long getCandidateId() {
		return candidateId;
	}
	public void setCandidateId(Long candidateId) {
		this.candidateId = candidateId;
	}
	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	public String getBreakdownJson() {
		return breakdownJson;
	}
	public void setBreakdownJson(String breakdownJson) {
		this.breakdownJson = breakdownJson;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public Long getEmployeeId() {
	    return candidateId;
	}

	public void setEmployeeId(Long employeeId) {
	    this.candidateId = employeeId;
	}
}
