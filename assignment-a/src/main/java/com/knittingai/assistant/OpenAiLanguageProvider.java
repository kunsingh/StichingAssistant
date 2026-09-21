package com.knittingai.assistant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenAiLanguageProvider implements KnittingAssistant.LanguageProvider {
    private static final String PHRASE_PROMPT =
        "You are a concise knitting assistant. Rephrase the supplied deterministic result as one or two warm "
            + "sentences. Mention only numbers listed in allowed_numbers, include every key result number, and "
            + "never convert units or invent, round, or add another number.";
    private static final String UNDERSTAND_PROMPT =
        "Extract the structured intent of a knitting question. Return ONLY JSON. Choose yarn_quantity with "
            + "width_cm, height_cm, yarn_weight, stitch_type, gauge; needle_size with yarn_weight, project_type, "
            + "desired_fabric; tension with target and actual; or unsupported. Never guess an unstated number.";
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])\\d+(?:\\.\\d+)?");
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private final String apiKey;
    private final String model;
    private final URI endpoint;

    public OpenAiLanguageProvider(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
    }

    public static Optional<OpenAiLanguageProvider> fromEnvironment() {
        Map<String, String> environment = DotEnv.load();
        String key = firstPresent(environment, "OPENAI_API_KEY", "AZURE_OPENAI_API_KEY");
        if (key == null || key.isBlank()) return Optional.empty();
        String model = environment.getOrDefault("OPENAI_CHAT_MODEL", "gpt-5-mini");
        String base = environment.get("OPENAI_BASE_URL");
        if (base == null || base.isBlank()) {
            String azure = environment.getOrDefault("AZURE_OPENAI_ENDPOINT",
                "https://kunsingh-openai-dalle.openai.azure.com");
            base = azure.replaceAll("/+$", "") + "/openai/v1";
        }
        return Optional.of(new OpenAiLanguageProvider(key, model, base));
    }

    @Override
    public String phrase(String question, String calculation, Map<String, Object> values) {
        try {
            List<Double> allowed = new ArrayList<>();
            Matcher matcher = NUMBER.matcher(mapper.writeValueAsString(values) + " " + question);
            while (matcher.find()) allowed.add(Double.valueOf(matcher.group()));
            allowed = allowed.stream().distinct().sorted(Comparator.naturalOrder()).toList();
            String user = mapper.writeValueAsString(Map.of(
                "question", question, "calculation", calculation, "result", values, "allowed_numbers", allowed,
                "instructions", "Rephrase warmly in one or two sentences using only allowed numbers."));
            return request(PHRASE_PROMPT, user);
        } catch (Exception exception) {
            throw new IllegalStateException("language provider unavailable: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Map<String, Object> understand(String question) {
        try {
            String text = request(UNDERSTAND_PROMPT, question);
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end < start) return null;
            return mapper.readValue(text.substring(start, end + 1), new TypeReference<>() {});
        } catch (Exception exception) {
            return null;
        }
    }

    private String request(String system, String user) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "model", model,
            "input", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user))));
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode payload = mapper.readTree(response.body());
        if (payload.hasNonNull("output_text")) return payload.get("output_text").asText().strip();
        for (JsonNode output : payload.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) return content.get("text").asText().strip();
            }
        }
        throw new IllegalArgumentException("Language response contained no output text");
    }

    private static String firstPresent(Map<String, String> values, String... keys) {
        for (String key : keys) if (values.containsKey(key) && !values.get(key).isBlank()) return values.get(key);
        return null;
    }
}
