package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Service
public class AIParsingService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String openAiKey;

    @Value("${openai.model.parse:gpt-4o-mini}")
    private String parseModel;

    public AIParsingService(@Qualifier("openAiWebClient") WebClient openAiWebClient) {
        this.webClient = openAiWebClient;
    }

    // optional: confirm we have a key (masked) at startup
    @PostConstruct
    public void init() {
        String masked = (openAiKey == null || openAiKey.isBlank()) ? "<EMPTY>" :
                openAiKey.trim().substring(0, Math.min(8, openAiKey.length())) + "...[masked]";
        org.slf4j.LoggerFactory.getLogger(AIParsingService.class).info("AIParsingService OpenAI key (masked) = {}", masked);
    }

    public String parseResumeToJson(String resumeText, String filename) throws Exception {
        String prompt = buildPrompt(resumeText, filename);

        Map<String,Object> body = new HashMap<>();
        body.put("model", parseModel);
        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content","You are a JSON extractor for resumes. Output EXACT valid JSON with keys: personal_info, education, experience, skills, certifications."));
        messages.add(Map.of("role","user","content", prompt));
        body.put("messages", messages);

        // Prefer injected property, fallback to OS env (helps if property binding missed)
        String token = (openAiKey == null || openAiKey.isBlank()) ? System.getenv("OPENAI_API_KEY") : openAiKey;
        if (token != null) token = token.trim();

        if (token == null || token.isBlank()) {
            // fail fast with a clear message instead of sending an empty header
            throw new IllegalStateException("OpenAI API key missing. Set OPENAI_API_KEY env var or openai.api.key property.");
        }

        try {
            // Use the WebClient (OpenAiConfig already sets default Authorization header if present).
            // We set header explicitly here only as a fallback — it's safe because token is guaranteed non-empty.
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))
                    .block();

            if (response == null) throw new RuntimeException("No response from OpenAI");

            JsonNode root = objectMapper.readTree(response);
            String content = null;
            if (root.has("choices") && root.get("choices").isArray()) {
                JsonNode first = root.get("choices").get(0);
                if (first.has("message") && first.get("message").has("content")) {
                    content = first.get("message").get("content").asText();
                } else if (first.has("text")) {
                    content = first.get("text").asText();
                }
            }
            if (content == null) throw new RuntimeException("LLM returned no content");

            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            String json = (start>=0 && end>start) ? content.substring(start, end+1) : content;
            return json;

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            org.slf4j.LoggerFactory.getLogger(AIParsingService.class)
                    .error("OpenAI responded: status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AIParsingService.class)
                    .error("Call to OpenAI failed: {}", e.getMessage(), e);
            throw e;
        }
    }


    private String buildPrompt(String resumeText, String filename) {
        String schema = "Return ONLY JSON with keys: personal_info{name,email,phone,location}, education[], experience[], skills[], certifications[]";
        String instruction = "Normalize technology names and correct obvious typos. If field missing return empty array or null. Use ISO-like dates when possible.";
        return schema + "\n" + instruction + "\nFilename: " + filename + "\nResumeText:\n" + resumeText;
    }
}
