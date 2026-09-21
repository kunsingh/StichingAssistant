package com.knittingai.swatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

public final class DiskCache {
    public record CachedValue(Path imagePath, Map<String, Object> metadata) {}
    private final Path directory;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public DiskCache(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    public static String key(SwatchInput value) {
        try {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            byte[] payload = mapper.writeValueAsString(value.cacheFields()).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new IllegalStateException("Unable to calculate cache key", exception);
        }
    }

    public Optional<CachedValue> get(SwatchInput value) {
        Path image = imagePath(key(value));
        Path metadata = metadataPath(key(value));
        if (!Files.isRegularFile(image) || !Files.isRegularFile(metadata)) return Optional.empty();
        try {
            Map<String, Object> contents = mapper.readValue(metadata.toFile(), new TypeReference<>() {});
            return Optional.of(new CachedValue(image, contents));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public CachedValue put(SwatchInput value, Path sourceImage, Map<String, Object> metadata) throws IOException {
        String key = key(value);
        Path image = imagePath(key);
        Path metadataFile = metadataPath(key);
        Path temporaryImage = image.resolveSibling(image.getFileName() + ".part");
        Path temporaryMetadata = metadataFile.resolveSibling(metadataFile.getFileName() + ".part");
        Files.copy(sourceImage, temporaryImage, StandardCopyOption.REPLACE_EXISTING);
        Files.move(temporaryImage, image, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("cache_key", key);
        complete.put("input", value.cacheFields());
        complete.putAll(metadata);
        mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryMetadata.toFile(), complete);
        Files.move(temporaryMetadata, metadataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new CachedValue(image, complete);
    }

    private Path imagePath(String key) {
        return directory.resolve(key + ".png");
    }

    private Path metadataPath(String key) {
        return directory.resolve(key + ".json");
    }
}
