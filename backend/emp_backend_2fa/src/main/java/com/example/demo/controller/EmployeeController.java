package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.model.Job;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.JobRepository;
import com.example.demo.service.AIParsingService;
import com.example.demo.service.AIShortlistingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:4200")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    // ✅ Central Upload Directory (Downloads/uploads)
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    @Autowired
    private EmployeeRepository employeeRepository;
    
    
    @Autowired
    private JobRepository jobRepository; // optional - if you have jobs table to get requirements
    
    @Autowired
    private AIParsingService aiParsingService; // optional - if you have jobs table to get requirements
    
    @Autowired
    private AIShortlistingService aiShortlistingService;

    
    
    
    
   // private final AIParsingService aiParsingService = null;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tika tika = new Tika();

	 
   


    // 🔹 Create upload folders at app startup
    @PostConstruct
    public void initUploadDirs() {
        String[] subDirs = { "photos", "resumes" };
        for (String subDir : subDirs) {
            File folder = new File(uploadDir + subDir);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (created) {
                    logger.info("✅ Created upload folder: {}", folder.getAbsolutePath());
                } else {
                    logger.error("❌ Failed to create folder: {}", folder.getAbsolutePath());
                }
            } else {
                logger.info("📁 Folder already exists: {}", folder.getAbsolutePath());
            }
        }
        logger.info("📂 Upload Base Directory: {}", uploadDir);
    }

    // 🔹 Get all employees
    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Employee> getAllEmployees() {
        logger.info("📌 Request: Fetch all employees");
        List<Employee> employees = employeeRepository.findAll();
        logger.info("✅ Found {} employees", employees.size());
        return employees;
    }

    // 🔹 Create new employee
    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public Employee createEmployee(@RequestBody Employee employee) {
        logger.info("📌 Request: Create employee for {}", employee.getEmail());
        Employee saved = employeeRepository.save(employee);
        logger.info("✅ Employee created with ID {}", saved.getId());
        return saved;
    }

    // 🔹 Get employee by ID
    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Employee> getByID(@PathVariable Long id) {
        logger.info("📌 Request: Get employee by ID {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));
        logger.info("✅ Found employee: {}", employee.getEmail());
        return ResponseEntity.ok(employee);
    }

    // 🔹 Update employee
    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> updateEmployeeByID(@PathVariable Long id,
                                                       @RequestBody Employee employeeDetails) {
        logger.info("📌 Request: Update employee ID {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        // Update fields
        employee.setFname(employeeDetails.getFname());
        employee.setLname(employeeDetails.getLname());
        employee.setEmail(employeeDetails.getEmail());
        employee.setDepartment(employeeDetails.getDepartment());
        employee.setDesignation(employeeDetails.getDesignation());
        employee.setJoiningDate(employeeDetails.getJoiningDate());
        employee.setSalary(employeeDetails.getSalary());

        Employee updatedEmployee = employeeRepository.save(employee);
        logger.info("✅ Employee updated -> ID {}", id);
        return ResponseEntity.ok(updatedEmployee);
    }

    // 🔹 Delete employee
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) {
        logger.info("📌 Request: Delete employee ID {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        employeeRepository.delete(employee);
        logger.info("✅ Employee deleted -> ID {}", id);

        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // 🔹 Upload Profile Photo
    @PostMapping("/employees/{id}/upload-photo")
    public ResponseEntity<String> uploadProfilePhoto(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        logger.info("📤 Uploading photo for employee ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String photoDir = uploadDir + "photos/";
        File folder = new File(photoDir);
        folder.mkdirs();

        String filePath = photoDir + id + "_" + Paths.get(file.getOriginalFilename()).getFileName().toString() /* sanitized */;
        File dest = new File(filePath);
        file.transferTo(dest);

        employee.setProfilePhotoPath(filePath);
        employeeRepository.save(employee);

        logger.info("✅ Photo uploaded: {}", filePath);
        return ResponseEntity.ok("✅ Profile photo uploaded successfully!");
    }

    // 🔹 Upload Resume
 // 🔹 Upload Resume (matches AIParsingService.parseResumeToJson signature)
    @PostMapping("/employees/{id}/upload-resume")
    public ResponseEntity<String> uploadResume(@PathVariable Long id,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "jobId", required = false) Long jobId) throws IOException, TikaException {
        logger.info("📤 Uploading resume for employee ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // canonical upload directory and ensure it exists
        Path resumeDirPath = Paths.get(uploadDir == null ? "./uploads" : uploadDir)
                .resolve("resumes")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(resumeDirPath);

        // sanitize original filename
        String original = file.getOriginalFilename() == null ? "resume" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        // create a unique safe filename to avoid collisions
        String safeName = id + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + original;
        Path destPath = resumeDirPath.resolve(safeName);

        // save file reliably using stream copy
        try (var in = file.getInputStream()) {
            Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Saved uploaded file to {}", destPath.toString());

        // --- Extract text from saved file using Tika ---
        String resumeText = "";
        try {
            resumeText = tika.parseToString(destPath.toFile());
        } catch (Exception e) {
            logger.warn("Tika failed to extract text; using fallback empty string. error={}", e.getMessage());
            resumeText = "";
        }

        // --- Prepare job description text if provided ---
        String jobReqText = "";
        if (jobId != null) {
            jobReqText = jobRepository.findById(jobId)
                    .map(Job::getDescription)
                    .orElse("");
        }

        // --- Call AI parsing service (it expects resumeText + filename) ---
        String modelJson = null;
        try {
            modelJson = aiParsingService.parseResumeToJson(resumeText, destPath.getFileName().toString());
            logger.debug("AIParsingService returned (len={}): {}", modelJson == null ? 0 : modelJson.length(),
                    (modelJson == null ? "null" : (modelJson.length() > 500 ? modelJson.substring(0, 500) + "...(truncated)" : modelJson)));
        } catch (Exception e) {
            logger.error("AIParsingService failed: {}", e.getMessage(), e);
        }

        // --- Convert JSON string returned by AI into Map (safe parsing) ---
        Map<String, Object> parsed = Collections.emptyMap();
        try {
            if (modelJson != null && !modelJson.isBlank()) {
                parsed = objectMapper.readValue(modelJson, new TypeReference<Map<String, Object>>() {});
            } else {
                // if model returned nothing, include raw extracted text as fallback
                parsed = Collections.singletonMap("rawText", resumeText == null ? "" : resumeText);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse model JSON to Map — storing raw text. error={}", e.getMessage());
            parsed = Collections.singletonMap("rawText", modelJson != null ? modelJson : resumeText);
        }

        // ----- Determine final score (use AIShortlistingService when job context provided) -----
        double finalScoreValue = 0.0; // 0..100

        // First attempt: if parsed contains a numeric 'score', prefer that (normalize if 0..1)
        try {
            Object scObj = parsed.getOrDefault("score", null);
            if (scObj instanceof Number) {
                double raw = ((Number) scObj).doubleValue();
                finalScoreValue = (raw <= 1.0) ? raw * 100.0 : raw;
            } else if (scObj != null) {
                try {
                    double raw = Double.parseDouble(String.valueOf(scObj));
                    finalScoreValue = (raw <= 1.0) ? raw * 100.0 : raw;
                } catch (Exception ex) {
                    // ignore, fallback below
                    finalScoreValue = 0.0;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not read 'score' from parsed map: {}", e.getMessage());
        }

        // If job context provided, call AIShortlistingService to compute authoritative score (preferred)
        if ((jobReqText != null && !jobReqText.isBlank())) {
            try {
                // prepare parsed JSON string for service: prefer modelJson (full), else convert parsed map to JSON
                String parsedJsonForService = (modelJson != null && !modelJson.isBlank())
                        ? modelJson
                        : objectMapper.writeValueAsString(parsed);

                Map<String, Object> scoreMap = aiShortlistingService.computeScore(parsedJsonForService, jobReqText, null);
                Object fs = scoreMap.get("finalScore");
                if (fs instanceof Number) {
                    finalScoreValue = ((Number) fs).doubleValue();
                } else if (fs != null) {
                    try {
                        finalScoreValue = Double.parseDouble(String.valueOf(fs));
                    } catch (Exception ex) {
                        logger.warn("AIShortlistingService returned non-numeric finalScore: {}", fs);
                    }
                }
                logger.info("AI shortlisting returned: finalScore={}, details={}", finalScoreValue, scoreMap);
            } catch (Exception ex) {
                logger.warn("AIShortlistingService.computeScore failed, falling back to parsed/heuristic score: {}", ex.getMessage());
                // keep finalScoreValue as whatever parsed/heuristic provided above (or 0)
            }
        } else {
            // no job context: if we don't already have a parsed score, compute simple heuristic using skills vs empty job (skip)
            if (finalScoreValue == 0.0) {
                // try basic heuristic if parsed contained skills and jobReqText absent -> keep 0 and mark UPLOADED
                // (we don't compute a job-match score without job description)
            }
        }

        // --- save parsed fields to employee entity (store full parsed JSON for auditing) ---
        String parsedJsonToStore;
        try {
            parsedJsonToStore = (modelJson != null && !modelJson.isBlank()) ? modelJson : objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            parsedJsonToStore = String.valueOf(parsed.getOrDefault("rawText", resumeText));
        }

        employee.setResumePath(destPath.toString());
        employee.setResumeParsedText(parsedJsonToStore);

        // normalize skills safely (parsed may contain array or comma string)
        List<String> skillsList = Collections.emptyList();
        try {
            Object skillsObj = parsed.getOrDefault("skills", Collections.emptyList());
            if (skillsObj instanceof List) {
                //noinspection unchecked
                skillsList = (List<String>) skillsObj;
            } else if (skillsObj instanceof String) {
                skillsList = Arrays.stream(((String) skillsObj).split("[,;]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        } catch (Exception ex) {
            logger.warn("Could not normalize skills from parsed JSON: {}", ex.getMessage());
        }
        employee.setResumeSkills(String.join(",", skillsList));

        // ensure finalScoreValue is within 0..100
        if (Double.isNaN(finalScoreValue) || finalScoreValue < 0.0) finalScoreValue = 0.0;
        if (finalScoreValue > 100.0) finalScoreValue = 100.0;

        employee.setResumeScore(finalScoreValue);
        employee.setResumeParsedAt(java.time.LocalDateTime.now());

        // bessere UX: if no job context provided, keep status 'UPLOADED'
        if (jobReqText == null || jobReqText.isBlank()) {
            employee.setResumeStatus("UPLOADED");
        } else {
            employee.setResumeStatus(finalScoreValue >= 60.0 ? "SHORTLISTED" : "REJECTED");
        }

        employeeRepository.save(employee);

        logger.info("✅ Resume uploaded + parsed: {} , normalizedScore={}", destPath.toString(), finalScoreValue);

        return ResponseEntity.ok("✅ Resume uploaded and parsed successfully!");
    }

    
        
    
    
    //preview
    
 // 🔹 Common Imports (अगर ऊपर नहीं हैं तो जोड़ो)
   

    // ✅ View / Download Profile Photo
    @GetMapping("/employees/{id}/photo")
    public ResponseEntity<Resource> viewEmployeePhoto(@PathVariable Long id) throws IOException {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.getProfilePhotoPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(employee.getProfilePhotoPath()).normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(Files.newInputStream(filePath)));
    }

    // ✅ View Resume in Browser (PDF inline preview)
    @GetMapping("/employees/{id}/resume/view")
    public ResponseEntity<Resource> viewResume(@PathVariable Long id) throws IOException {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.getResumePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(employee.getResumePath()).normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/pdf"; // fallback for resumes
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(Files.newInputStream(filePath)));
    }

    // ✅ Download Resume (Attachment)
    @GetMapping("/employees/{id}/resume/download")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id) throws IOException {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.getResumePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(employee.getResumePath()).normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(Files.newInputStream(filePath)));
    }

}
