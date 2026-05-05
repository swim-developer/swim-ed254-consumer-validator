package com.github.swim_developer.validator.ed254.consumer.application.usecase;

import com.github.swim_developer.validator.core.domain.util.XmlPatternExtractor;
import com.github.swim_developer.validator.ed254.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.ed254.consumer.domain.model.FilterOptions;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.github.swim_developer.validator.ed254.consumer.domain.port.in.Ed254EventMetadataPort;

@Slf4j
@ApplicationScoped
public class Ed254EventMetadataService implements Ed254EventMetadataPort {

    static final Pattern AERODROME_PATTERN = Pattern.compile("<aerodromeDesignator>([A-Z]{4})</aerodromeDesignator>");
    static final Pattern CALLSIGN_PATTERN = Pattern.compile("<arcid>([A-Z0-9]+)</arcid>");
    static final Pattern ARRIVAL_SEQUENCE_PATTERN = Pattern.compile("<arrivalSequence\\b");
    static final Pattern PROVIDER_EXCEPTION_PATTERN = Pattern.compile("<providerExceptions\\b");
    static final Pattern ENTRY_PATTERN = Pattern.compile("<arrivalManagementInformation\\b");

    @ConfigProperty(name = "event.generator.events.path", defaultValue = "/opt/events")
    String eventsPath;

    @ConfigProperty(name = "event.generator.exceptions.path", defaultValue = "/opt/exceptions")
    String exceptionsPath;

    @CacheResult(cacheName = "event-metadata")
    public List<EventFileMetadata> getAllEventMetadata() {
        List<EventFileMetadata> result = new ArrayList<>();
        result.addAll(scanDirectory(Paths.get(eventsPath)));
        result.addAll(scanDirectory(Paths.get(exceptionsPath)));
        result.sort(Comparator.comparing(EventFileMetadata::getFilename));
        return result;
    }

    private List<EventFileMetadata> scanDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warn("Directory not found: {}", dir);
            return List.of();
        }
        try {
            return Files.list(dir)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(this::extractMetadata)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public FilterOptions getFilterOptions() {
        List<EventFileMetadata> all = getAllEventMetadata();
        Set<String> messageTypes = new LinkedHashSet<>();
        Set<String> aerodromes = new LinkedHashSet<>();
        Set<String> callsigns = new LinkedHashSet<>();
        for (EventFileMetadata m : all) {
            if (m.getMessageType() != null && !m.getMessageType().isBlank()) {
                messageTypes.add(m.getMessageType());
            }
            if (m.getAerodromeDesignator() != null && !m.getAerodromeDesignator().isBlank()) {
                aerodromes.add(m.getAerodromeDesignator());
            }
            callsigns.addAll(m.getCallsigns());
        }
        return new FilterOptions(
                messageTypes.stream().sorted().toList(),
                aerodromes.stream().sorted().toList(),
                callsigns.stream().sorted().toList()
        );
    }

    private EventFileMetadata extractMetadata(Path file) {
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        boolean hasArrivalSequence = ARRIVAL_SEQUENCE_PATTERN.matcher(content).find();
        boolean hasProviderException = PROVIDER_EXCEPTION_PATTERN.matcher(content).find();
        String messageType;
        if (hasArrivalSequence) {
            messageType = "arrivalSequence";
        } else if (hasProviderException) {
            messageType = "providerExceptions";
        } else {
            messageType = "unknown";
        }
        String aerodrome = XmlPatternExtractor.extractFirst(AERODROME_PATTERN, content).orElse(null);
        Set<String> callsigns = new HashSet<>(XmlPatternExtractor.extractAll(CALLSIGN_PATTERN, content));
        int entries = XmlPatternExtractor.countMatches(ENTRY_PATTERN, content);
        return new EventFileMetadata(file.getFileName().toString(), messageType, aerodrome, entries, callsigns);
    }
}
