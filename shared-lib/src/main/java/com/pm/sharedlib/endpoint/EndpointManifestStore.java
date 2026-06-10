package com.pm.sharedlib.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class EndpointManifestStore {

    private final ObjectMapper objectMapper;
    private final Path manifestPath;

    public Optional<EndpointManifest> read() {
        if (!Files.exists(manifestPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(manifestPath.toFile(), EndpointManifest.class));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read endpoint manifest: " + manifestPath, e);
        }
    }

    public void write(EndpointManifest manifest) {
        try {
            var parent = manifestPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
            log.info("Wrote endpoint manifest: {}", manifestPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write endpoint manifest: " + manifestPath, e);
        }
    }
}
