package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class AIShortlistingService {

    private static final Logger logger = LoggerFactory.getLogger(AIShortlistingService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String openAiKey;

    @Value("${openai.model.embeddings:text-embedding-3-large}")
    private String embeddingsModel;

    @Value("${ai.weight.skills:0.6}")
    private double weightSkills;
    @Value("${ai.weight.experience:0.25}")
    private double weightExperience;
    @Value("${ai.weight.education:0.15}")
    private double weightEducation;

    public AIShortlistingService(WebClient openAiWebClient) {
        this.webClient = openAiWebClient;
        logger.info("AIShortlistingService instantiated with WebClient: {}", openAiWebClient != null);

    }

    /**
     * Call embeddings endpoint. Returns null if input text is null/blank or on failure.
     */
    public double[] embed(String text) throws Exception {
        if (text == null || text.isBlank()) {
            return null;
        }
        Map<String,Object> body = Map.of("model", embeddingsModel, "input", text);
        String resp = webClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .block();

        if (resp == null || resp.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(resp);
        JsonNode dataNode = root.get("data");
        if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) return null;
        JsonNode vec = dataNode.get(0).get("embedding");
        if (vec == null || !vec.isArray()) return null;
        double[] arr = new double[vec.size()];
        for (int i=0;i<vec.size();i++) arr[i] = vec.get(i).asDouble();
        return arr;
    }

    public double cosine(double[] a, double[] b) {
        if (a==null || b==null) return 0.0;
        double dot=0, na=0, nb=0;
        for (int i=0;i<Math.min(a.length,b.length);i++) {
            dot += a[i]*b[i];
            na += a[i]*a[i];
            nb += b[i]*b[i];
        }
        if (na==0 || nb==0) return 0.0;
        return dot / (Math.sqrt(na)*Math.sqrt(nb));
    }

    /**
     * Compute score with robust fallbacks:
     * - Try embedding-based similarity.
     * - If embedding unavailable/failed or similarity near-zero, fallback to keyword matching.
     * - Compute experience/education components as before.
     * - Return finalScore in 0..100 scale.
     */
    public Map<String,Object> computeScore(String parsedResumeJson, String jobSkillsJson, Integer minExpYears) throws Exception {
        String candidateSkills = extractSkills(parsedResumeJson); // may be "" if none
        String jobSkills = jobSkillsJson == null ? "" : jobSkillsJson;

        double skillSim = 0.0;
        boolean usedEmbedding = false;

        // Attempt embedding-based similarity
        try {
            double[] embCand = null;
            double[] embJob = null;
            try {
                embCand = embed(candidateSkills);
            } catch (Exception e) {
                logger.warn("Embedding call for candidate failed: {}", e.getMessage());
            }
            try {
                embJob = embed(jobSkills);
            } catch (Exception e) {
                logger.warn("Embedding call for job failed: {}", e.getMessage());
            }

            if (embCand != null && embJob != null) {
                skillSim = cosine(embCand, embJob);
                usedEmbedding = true;
            } else {
                usedEmbedding = false;
            }
        } catch (Exception ex) {
            // If anything unexpected happens, fallback to keyword method below
            logger.warn("Embedding-based similarity failed unexpectedly: {}", ex.getMessage(), ex);
            usedEmbedding = false;
            skillSim = 0.0;
        }

        // Fallback: keyword/token based similarity if embedding not used or result is too small
        if (!usedEmbedding || skillSim <= 0.0001) {
            String fallbackText = candidateSkills;
            if (fallbackText == null || fallbackText.isBlank()) {
                // try to extract a 'text' field from parsedJson if present
                try {
                    JsonNode root = objectMapper.readTree(parsedResumeJson);
                    if (root.has("text")) fallbackText = root.get("text").asText("");
                    else {
                        // attempt to gather other fields like title/summary/experience descriptions
                        StringBuilder sb = new StringBuilder();
                        if (root.has("experience") && root.get("experience").isArray()) {
                            for (JsonNode e : root.get("experience")) {
                                if (e.has("title")) sb.append(" ").append(e.get("title").asText());
                                if (e.has("description")) sb.append(" ").append(e.get("description").asText());
                            }
                        }
                        if (sb.length() > 0) fallbackText = sb.toString();
                    }
                } catch (Exception ex) {
                    // ignore parse errors, fallbackText remains candidateSkills or ""
                }
            }
            skillSim = keywordSimilarityScore(fallbackText, jobSkills);
        }

        double expSim = computeExp(parsedResumeJson, minExpYears);
        double eduSim = computeEdu(parsedResumeJson);

        double combined = weightSkills * skillSim + weightExperience * expSim + weightEducation * eduSim;

        // ensure combined is in 0..1
        if (Double.isNaN(combined) || combined < 0) combined = 0.0;
        if (combined > 1.0) combined = 1.0;

        double finalScore = Math.round(combined * 10000.0) / 100.0; // scale 0..100

        Map<String,Object> res = new HashMap<>();
        res.put("finalScore", finalScore);
        res.put("skillSim", skillSim);
        res.put("expSim", expSim);
        res.put("eduSim", eduSim);
        res.put("usedEmbedding", usedEmbedding);

        logger.info("Shortlist computed -> finalScore={}, skillSim={}, expSim={}, eduSim={}, usedEmbedding={}",
                finalScore, skillSim, expSim, eduSim, usedEmbedding);

        return res;
    }

    private String extractSkills(String parsedJson) {
        try {
            if (parsedJson == null || parsedJson.isBlank()) return "";
            JsonNode root = objectMapper.readTree(parsedJson);
            JsonNode skills = root.get("skills");
            if (skills!=null && skills.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode s: skills) {
                    if (sb.length()>0) sb.append("; ");
                    sb.append(s.asText());
                }
                return sb.toString();
            }
        } catch (Exception e){
            logger.warn("extractSkills failed: {}", e.getMessage());
        }
        return "";
    }

    private double computeExp(String parsedJson, Integer minYears) {
        if (minYears==null || minYears<=0) return 1.0;
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            JsonNode exp = root.get("experience");
            int years=0;
            if (exp!=null && exp.isArray()) {
                for (JsonNode e: exp) {
                    String s = e.has("start")?e.get("start").asText():null;
                    String en = e.has("end")?e.get("end").asText():null;
                    try {
                        if (s!=null && s.length()>=4) {
                            int sy = Integer.parseInt(s.substring(0,4));
                            int ey = (en!=null && en.length()>=4) ? Integer.parseInt(en.substring(0,4)) : java.time.Year.now().getValue();
                            years += Math.max(0, ey - sy);
                        }
                    } catch (Exception ex){ }
                }
            }
            return Math.min(1.0, (double) years / (double) minYears);
        } catch (Exception e) {
            logger.warn("computeExp failed: {}", e.getMessage());
            return 0.0;
        }
    }

    private double computeEdu(String parsedJson) {
        try {
            JsonNode root = objectMapper.readTree(parsedJson);
            JsonNode edu = root.get("education");
            if (edu!=null && edu.isArray() && edu.size()>0) return 0.8;
        } catch (Exception e) {
            logger.warn("computeEdu failed: {}", e.getMessage());
        }
        return 0.0;
    }

    /**
     * Simple keyword/token overlap based similarity (0..1).
     * Counts how many distinct job tokens appear in candidate text.
     */
    private double keywordSimilarityScore(String candidateText, String jobSkills) {
        if (jobSkills == null || jobSkills.isBlank()) return 0.0;
        if (candidateText == null) candidateText = "";

        String cand = candidateText.toLowerCase().replaceAll("[^a-z0-9, ]", " ");
        String job = jobSkills.toLowerCase().replaceAll("[^a-z0-9, ]", " ");

        Set<String> jobTokens = new HashSet<>();
        for (String t : job.split("[,;\\s]+")) {
            String tk = t.trim();
            if (tk.length() > 1) jobTokens.add(tk);
        }
        if (jobTokens.isEmpty()) return 0.0;

        int matches = 0;
        for (String tk : jobTokens) {
            if (cand.contains(tk)) matches++;
        }
        double sim = (double) matches / (double) jobTokens.size();
        return sim; // 0..1
    }
}
