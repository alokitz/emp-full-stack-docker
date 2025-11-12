package com.example.demo.controller;

import com.example.demo.model.Employee;
import com.example.demo.model.Job;
import com.example.demo.model.ShortlistResult;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.JobRepository;
import com.example.demo.repository.ShortlistResultRepository;
import com.example.demo.service.AIShortlistingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ShortlistController {

    private final Logger logger = LoggerFactory.getLogger(ShortlistController.class);

    private final EmployeeRepository employeeRepo;
    private final JobRepository jobRepo;
    private final AIShortlistingService shortlistingService;
    private final ShortlistResultRepository shortlistResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShortlistController(EmployeeRepository employeeRepo,
                               JobRepository jobRepo,
                               AIShortlistingService shortlistingService,
                               ShortlistResultRepository shortlistResultRepository) {
        this.employeeRepo = employeeRepo;
        this.jobRepo = jobRepo;
        this.shortlistingService = shortlistingService;
        this.shortlistResultRepository = shortlistResultRepository;
    }

    /**
     * Compute AI-based shortlisting score for an employee against a job.
     * - Uses employee.resumeParsedText (expected to be the parsed JSON string from AI).
     * - Falls back to employee.resumeSkills or empty string if parsed JSON missing.
     * - Uses job.requiredSkills OR job.requiredSkillsJson OR job.description as job text.
     * - If job has a minExperienceYears field it will be used; otherwise null is passed.
     *
     * Returns the score breakdown produced by AIShortlistingService.
     */
    @PostMapping("/shortlist/{jobId}/employee/{employeeId}")
    public ResponseEntity<?> score(@PathVariable Long jobId, @PathVariable Long employeeId) {
        try {
            Optional<Job> jobOpt = jobRepo.findById(jobId);
            Optional<Employee> empOpt = employeeRepo.findById(employeeId);
            if (jobOpt.isEmpty() || empOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "job/employee not found"));
            }

            Job job = jobOpt.get();
            Employee emp = empOpt.get();

            // Prefer parsed JSON stored in resumeParsedText, else fallback to resumeSkills string
            String parsedJson = emp.getResumeParsedText() != null ? emp.getResumeParsedText() : "";
            String jobSkillsText = "";

            // Try common getters that different Job classes might have
            try {
                // prefer `getRequiredSkills()` if present
                jobSkillsText = (job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank())
                        ? job.getRequiredSkills()
                        : (job.getDescription() != null ? job.getDescription() : "");
            } catch (Throwable t) {
                // If Job class has different field name, try reflection-safe fallback
                jobSkillsText = job.getDescription() != null ? job.getDescription() : "";
            }

            // Try to find minExperienceYears if available on Job (many models don't have it)
            Integer minExpYears = null;
            try {
                // method may be named getMinExperienceYears or getMinExperience; handle both
                try {
                    java.lang.reflect.Method m = job.getClass().getMethod("getMinExperienceYears");
                    Object val = m.invoke(job);
                    if (val instanceof Number) minExpYears = ((Number) val).intValue();
                } catch (NoSuchMethodException nsme) {
                    java.lang.reflect.Method m2 = job.getClass().getMethod("getMinExperience");
                    Object val2 = m2.invoke(job);
                    if (val2 instanceof Number) minExpYears = ((Number) val2).intValue();
                }
            } catch (Throwable ignored) {
                // no minExpYears available — that's fine, pass null
            }

            // Compute score using AIShortlistingService
            Map<String, Object> scoreMap = shortlistingService.computeScore(parsedJson, jobSkillsText, minExpYears);

            // Ensure finalScore exists and is numeric
            double finalScore = 0.0;
            Object fs = scoreMap.get("finalScore");
            if (fs instanceof Number) {
                finalScore = ((Number) fs).doubleValue();
            } else if (fs instanceof String) {
                try {
                    finalScore = Double.parseDouble((String) fs);
                } catch (NumberFormatException nfe) {
                    logger.warn("finalScore from AI not numeric: {}", fs);
                }
            } else {
                logger.warn("AI shortlisting returned no recognizable finalScore, defaulting to 0");
            }

            // Persist ShortlistResult (optional) and update employee
            ShortlistResult r = new ShortlistResult();
            r.setEmployeeId(employeeId);
            r.setJobId(jobId);
            r.setScore(finalScore);
            r.setBreakdownJson(objectMapper.writeValueAsString(scoreMap));
            shortlistResultRepository.saveAndFlush(r);

            // Update employee
            emp.setResumeScore(finalScore);
            // our computeScore returns 0-100 scale — threshold 60 => shortlisted (adjust if your service returns 0-1)
            emp.setResumeStatus(finalScore >= 60.0 ? "SHORTLISTED" : "REJECTED");
            employeeRepo.save(emp);

            return ResponseEntity.ok(scoreMap);
        } catch (Exception e) {
            logger.error("Scoring failed for jobId={} employeeId={}: {}", jobId, employeeId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Scoring failed", "message", e.getMessage()));
        }
    }
}
