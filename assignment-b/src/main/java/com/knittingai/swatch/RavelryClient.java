package com.knittingai.swatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

public final class RavelryClient {
    public record RavelryResult(boolean matched, String patternName, Path imagePath, String message) {}
    private final String username;
    private final String accessKey;
    private final Duration timeout;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public RavelryClient() {
        this(System.getenv().getOrDefault("RAVELRY_USERNAME", ""),
            System.getenv().getOrDefault("RAVELRY_ACCESS_KEY", ""), Duration.ofSeconds(8));
    }

    public RavelryClient(String username, String accessKey) {
        this(username, accessKey, Duration.ofSeconds(8));
    }

    public RavelryClient(String username, String accessKey, Duration timeout) {
        this.username = username;
        this.accessKey = accessKey;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public RavelryResult findReference(String stitchType, Path outputDirectory) {
        if (username.isBlank() || accessKey.isBlank()) {
            return new RavelryResult(false, null, null, "Ravelry credentials not configured; skipped");
        }
        try {
            String query = URLEncoder.encode(stitchType, StandardCharsets.UTF_8);
            URI uri = URI.create("https://api.ravelry.com/patterns/search.json?query=" + query
                + "&page_size=20&sort=best");
            String token = Base64.getEncoder().encodeToString((username + ":" + accessKey).getBytes(StandardCharsets.UTF_8));
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(timeout)
                .header("Authorization", "Basic " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            for (JsonNode pattern : mapper.readTree(response.body()).path("patterns")) {
                JsonNode photo = pattern.path("first_photo");
                String imageUrl = photo.path("medium_url").asText(photo.path("small_url").asText(""));
                if (imageUrl.isBlank()) continue;
                Files.createDirectories(outputDirectory);
                Path destination = outputDirectory.resolve("ravelry-reference.jpg");
                HttpResponse<byte[]> image = client.send(HttpRequest.newBuilder(URI.create(imageUrl))
                    .timeout(timeout).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                if (image.statusCode() < 200 || image.statusCode() >= 300) {
                    throw new IllegalStateException("image HTTP " + image.statusCode());
                }
                Files.write(destination, image.body());
                return new RavelryResult(true, pattern.path("name").asText("Ravelry pattern"), destination,
                    "Loose keyword match from Ravelry");
            }
            return new RavelryResult(false, null, null, "No Ravelry photo matched '" + stitchType + "'");
        } catch (Exception exception) {
            return new RavelryResult(false, null, null, "Ravelry lookup failed gracefully: "
                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
}
