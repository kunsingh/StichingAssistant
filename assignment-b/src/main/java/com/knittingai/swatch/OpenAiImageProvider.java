package com.knittingai.swatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

public final class OpenAiImageProvider implements ImageProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;
    private final String apiKey;
    private final URI endpoint;
    private final String model;
    private final Duration timeout;

    public OpenAiImageProvider() {
        this(DotEnv.load());
    }

    private OpenAiImageProvider(Map<String, String> env) {
        this(apiKeyFrom(env), endpointFrom(env), modelFrom(env), Duration.ofSeconds(90));
    }

    public OpenAiImageProvider(String apiKey, URI endpoint, String model, Duration timeout) {
        this.apiKey = apiKey;
        boolean needEnv = endpoint == null || model == null;
        Map<String, String> env = needEnv ? DotEnv.load() : Map.of();
        this.endpoint = endpoint == null ? endpointFrom(env) : endpoint;
        this.model = model == null ? modelFrom(env) : model;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    static String apiKeyFrom(Map<String, String> env) {
        String key = env.get("OPENAI_API_KEY");
        if (key == null || key.isBlank()) key = env.get("AZURE_OPENAI_API_KEY");
        return key == null ? "" : key;
    }

    static URI endpointFrom(Map<String, String> env) {
        String explicit = env.get("OPENAI_IMAGE_URL");
        if (explicit != null && !explicit.isBlank()) return URI.create(explicit.strip());
        String base = env.get("OPENAI_BASE_URL");
        if (base == null || base.isBlank()) {
            String azure = env.get("AZURE_OPENAI_ENDPOINT");
            base = (azure == null || azure.isBlank()) ? "https://api.openai.com/v1" : azure;
        }
        base = base.replaceAll("/+$", "");
        if (base.contains(".openai.azure.com") && !base.contains("/openai/")) {
            base = base + "/openai/v1";
        }
        return URI.create(base + "/images/generations");
    }

    static Map<String, Object> requestPayload(String model, URI endpoint, String prompt) {
        boolean flux = model.toLowerCase(Locale.ROOT).contains("flux")
            || endpoint.toString().toLowerCase(Locale.ROOT).contains("blackforestlabs");
        if (flux) {
            return Map.of("model", model, "prompt", prompt, "n", 1, "width", 1024, "height", 1024);
        }
        return Map.of("model", model, "prompt", prompt, "n", 1, "size", "1024x1024");
    }

    static String modelFrom(Map<String, String> env) {
        String model = env.get("OPENAI_IMAGE_MODEL");
        return model == null || model.isBlank() ? "FLUX.2-pro" : model;
    }

    @Override
    public void generate(String prompt, Path outputPath) throws Exception {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("No image API key configured (set OPENAI_API_KEY or AZURE_OPENAI_API_KEY)");
        }
        String body = mapper.writeValueAsString(requestPayload(model, endpoint, prompt));
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
            .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = response.body();
            if (errorBody != null && errorBody.length() > 500) errorBody = errorBody.substring(0, 500);
            throw new IllegalStateException("Image provider returned HTTP " + response.statusCode()
                + " for " + endpoint + (errorBody == null || errorBody.isBlank() ? "" : " :: " + errorBody.strip()));
        }
        JsonNode first = mapper.readTree(response.body()).path("data").path(0);
        if (first.hasNonNull("b64_json")) {
            Files.write(outputPath, Base64.getDecoder().decode(first.get("b64_json").asText()));
        } else if (first.hasNonNull("url")) {
            HttpResponse<byte[]> image = client.send(HttpRequest.newBuilder(URI.create(first.get("url").asText()))
                .timeout(timeout).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (image.statusCode() < 200 || image.statusCode() >= 300) {
                throw new IllegalStateException("Image download returned HTTP " + image.statusCode());
            }
            Files.write(outputPath, image.body());
        } else {
            throw new IllegalArgumentException("Image response contained neither b64_json nor url");
        }
        if (ImageIO.read(outputPath.toFile()) == null) throw new IllegalArgumentException("Provider returned an invalid image");
    }
}
