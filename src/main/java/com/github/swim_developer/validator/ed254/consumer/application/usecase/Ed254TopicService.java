package com.github.swim_developer.validator.ed254.consumer.application.usecase;

import com.github.swim_developer.validator.core.domain.model.TopicData;
import com.github.swim_developer.validator.core.domain.model.TopicDetails;
import com.github.swim_developer.validator.core.domain.model.TopicSummary;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.github.swim_developer.validator.ed254.consumer.domain.port.in.Ed254TopicPort;

@ApplicationScoped
public class Ed254TopicService implements Ed254TopicPort {

    private static final Map<String, TopicData> TOPIC_CATALOG = new LinkedHashMap<>();

    static {
        TOPIC_CATALOG.put("ARRIVAL_SEQUENCE", new TopicData(
                "Arrival Sequence",
                "Real-time arrival sequence snapshots per aerodrome",
                "Complete arrival sequence published by the AMAN system, including flight identification, "
                        + "target landing times, sequence numbers, assigned runways, metering information, "
                        + "and controller advisories (speed, route, time constraints)",
                List.of("arrivalSequence", "arrivalManagementInformation", "meteringInformation"),
                List.of("Upstream ANSPs", "En-route ATC"),
                List.of("Cross-border E-AMAN coordination", "Speed advisory delivery", "Sequence monitoring")
        ));
        TOPIC_CATALOG.put("PROVIDER_EXCEPTIONS", new TopicData(
                "Provider Exceptions",
                "AMAN service status and degradation notifications",
                "Notifications when the AMAN system encounters operational issues: "
                        + "SEQUENCING_DISABLED (sequencing turned off), "
                        + "AMAN_UNAVAILABLE (system completely down), "
                        + "AMAN_DEGRADED (operating in reduced capability mode)",
                List.of("providerExceptions", "provException"),
                List.of("Upstream ANSPs", "Network Manager"),
                List.of("AMAN health monitoring", "Fallback procedure activation")
        ));
    }

    public List<TopicSummary> getAllTopics() {
        return TOPIC_CATALOG.entrySet().stream()
                .map(e -> new TopicSummary(e.getKey(), e.getValue().title(), e.getValue().summaryDescription()))
                .toList();
    }

    public Optional<TopicDetails> getTopicDetails(String topicId) {
        TopicData data = TOPIC_CATALOG.get(topicId);
        if (data == null) {
            return Optional.empty();
        }
        String messageType = switch (topicId) {
            case "ARRIVAL_SEQUENCE" -> "arrivalSequence";
            case "PROVIDER_EXCEPTIONS" -> "providerExceptions";
            default -> topicId;
        };
        return Optional.of(new TopicDetails(
                topicId,
                data.title(),
                data.detailedDescription(),
                messageType,
                data.features(),
                data.mandatoryFor(),
                data.useCases()
        ));
    }
}
