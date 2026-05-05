package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest;

import com.github.swim_developer.validator.core.domain.model.SubscriptionStatus;
import com.github.swim_developer.validator.consumer.domain.port.out.SubscriptionRepository;
import com.github.swim_developer.validator.core.infrastructure.console.ConsoleEvent;
import com.github.swim_developer.validator.core.infrastructure.console.ConsoleNotificationService;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.consumer.domain.port.in.XmlFileCachePort;
import com.github.swim_developer.validator.consumer.domain.port.in.LoadTestPort;
import com.github.swim_developer.validator.consumer.domain.port.in.ScenarioPreviewPort;
import com.github.swim_developer.validator.core.infrastructure.util.XmlEd254DateRandomizer;
import com.github.swim_developer.validator.core.infrastructure.fault.FaultInjectionService;
import com.github.swim_developer.validator.core.infrastructure.fault.FaultRequest;
import com.github.swim_developer.validator.consumer.domain.port.in.HeartbeatPort;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private static final String CSS_CLASS_TRIGGER_ERROR = "trigger-error";
    private static final String CSS_CLASS_TRIGGER_SUCCESS = "trigger-success";
    private static final String CSS_CLASS_SEND_ERROR = "send-error";
    private static final String CSS_CLASS_SEND_SUCCESS = "send-success";
    private static final String LITERAL_SUBSCRIPTIONS = " subscription(s)";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final EventGeneratorPort eventGeneratorService;
    private final XmlFileCachePort xmlEventFileLoader;
    private final LoadTestPort loadTestService;
    private final ScenarioPreviewPort scenarioService;
    private final SubscriptionRepository subscriptionRepository;
    private final ConsoleNotificationService consoleNotificationService;
    private final HeartbeatPort heartbeatPublisher;
    private final FaultInjectionService faultInjectionService;
    private final XmlEd254DateRandomizer xmlEd254DateRandomizer;

    @Inject
    public AdminResource(
            EventGeneratorPort eventGeneratorService,
            XmlFileCachePort xmlEventFileLoader,
            LoadTestPort loadTestService,
            ScenarioPreviewPort scenarioService,
            SubscriptionRepository subscriptionRepository,
            ConsoleNotificationService consoleNotificationService,
            HeartbeatPort heartbeatPublisher,
            FaultInjectionService faultInjectionService,
            XmlEd254DateRandomizer xmlEd254DateRandomizer) {
        this.eventGeneratorService = eventGeneratorService;
        this.xmlEventFileLoader = xmlEventFileLoader;
        this.loadTestService = loadTestService;
        this.scenarioService = scenarioService;
        this.subscriptionRepository = subscriptionRepository;
        this.consoleNotificationService = consoleNotificationService;
        this.heartbeatPublisher = heartbeatPublisher;
        this.faultInjectionService = faultInjectionService;
        this.xmlEd254DateRandomizer = xmlEd254DateRandomizer;
    }

    @Path("/refresh-cache") @DELETE
    public void refreshCache() { xmlEventFileLoader.clearXmlCache(); }

    @Path("/load") @GET @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS) @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> executeLoad(@QueryParam("duration") @DefaultValue("10s") String duration) {
        return loadTestService.executeLoad(duration).onItem().transform(s -> s + "\n");
    }

    @Path("/trigger") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String triggerSpecificEvent(
            @QueryParam("file") String filename,
            @QueryParam("count") @DefaultValue("1") int messageCount,
            @QueryParam("randomizeDates") @DefaultValue("false") boolean randomizeDates,
            @QueryParam("useCurrentUtc") @DefaultValue("false") boolean useCurrentUtc) {
        try {
            int effectiveCount = Math.clamp(messageCount, 1, 10000);
            String baseXml = xmlEventFileLoader.readEventFile(filename);
            int totalSent = 0;
            int subscriptionCount = 0;
            for (int i = 0; i < effectiveCount; i++) {
                String xml = baseXml;
                if (useCurrentUtc) {
                    xml = xmlEd254DateRandomizer.applyCurrentUtcDates(xml);
                } else if (randomizeDates) {
                    xml = xmlEd254DateRandomizer.randomizeDates(xml);
                }
                subscriptionCount = eventGeneratorService.sendScenarioEvent(xml, filename);
                if (subscriptionCount > 0) totalSent++;
            }
            if (subscriptionCount == 0) {
                consoleNotificationService.warning("Trigger: No active subscriptions");
                return div(CSS_CLASS_TRIGGER_ERROR, "No active subscriptions to receive the event");
            }
            String msg = effectiveCount == 1
                    ? "✓ Event sent to " + subscriptionCount + LITERAL_SUBSCRIPTIONS
                    : "✓ " + totalSent + " message(s) sent to " + subscriptionCount + LITERAL_SUBSCRIPTIONS;
            consoleNotificationService.success("Trigger: " + filename + " → " + totalSent + "x" + subscriptionCount);
            return div(CSS_CLASS_TRIGGER_SUCCESS, msg);
        } catch (Exception e) {
            log.error("Failed to trigger event", e);
            consoleNotificationService.error("Trigger failed: " + e.getMessage());
            return div(CSS_CLASS_TRIGGER_ERROR, "✗ " + e.getMessage());
        }
    }

    @Path("/scenario/malformed") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String triggerMalformedEvent() {
        return sendScenario(scenarioService.getMalformedXmlToSend(), "malformed", "Malformed event");
    }

    @Path("/scenario/duplicate") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String triggerDuplicateEvent() {
        try {
            eventGeneratorService.sendDuplicateScenarioEvent(scenarioService.getDuplicateXmlToSend());
            return div(CSS_CLASS_TRIGGER_SUCCESS, "✓ First message sent. Duplicate in 10 seconds...");
        } catch (Exception e) {
            log.error("Failed to trigger duplicate event", e);
            return div(CSS_CLASS_TRIGGER_ERROR, "✗ " + e.getMessage());
        }
    }

    @Path("/scenario/multiple") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String triggerMultipleMessages() {
        return sendScenario(scenarioService.getMultipleMessagesToSend(), "multiple", "Multiple messages");
    }

    @Path("/scenario/multiple-error") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String triggerMultipleMessagesWithError() {
        return sendScenario(scenarioService.getMultipleMessagesWithErrorToSend(), "multiple-error", "Multiple messages (1 error)");
    }

    @Path("/send-custom-event") @POST @Consumes(MediaType.APPLICATION_XML) @Produces(MediaType.TEXT_HTML)
    public String sendCustomEvent(String xmlContent) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) return div(CSS_CLASS_SEND_ERROR, "XML content cannot be empty");
        try {
            int count = eventGeneratorService.sendScenarioEvent(xmlContent, "custom");
            if (count == 0) return div(CSS_CLASS_SEND_ERROR, "No active subscriptions");
            return div(CSS_CLASS_SEND_SUCCESS, "✓ Event sent to " + count + LITERAL_SUBSCRIPTIONS);
        } catch (Exception e) {
            log.error("Failed to send custom event", e);
            return div(CSS_CLASS_SEND_ERROR, "✗ " + e.getMessage());
        }
    }

    @Path("/reset-subscriptions") @DELETE @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML) @Transactional
    public String resetSubscriptions() {
        long count = subscriptionRepository.count();
        subscriptionRepository.deleteAll();
        consoleNotificationService.warning("Reset: Cleared " + count + LITERAL_SUBSCRIPTIONS);
        return div(CSS_CLASS_TRIGGER_SUCCESS, "✓ Cleared " + count + LITERAL_SUBSCRIPTIONS);
    }

    @Path("/scheduler/status") @GET
    public String getSchedulerStatus() { return "{\"enabled\":" + eventGeneratorService.isSchedulerEnabled() + "}"; }

    @Path("/scheduler/toggle") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String toggleScheduler() {
        boolean s = eventGeneratorService.toggleScheduler();
        consoleNotificationService.info("Scheduler " + (s ? "enabled" : "disabled"));
        return div(s ? CSS_CLASS_TRIGGER_SUCCESS : CSS_CLASS_TRIGGER_ERROR, "Scheduler " + (s ? "enabled" : "disabled"));
    }

    @Path("/status/amqp") @GET @Produces(MediaType.TEXT_HTML)
    public String getAmqpStatusFragment() {
        boolean c = eventGeneratorService.isAmqpConnected();
        return "<span class=\"status-indicator " + (c ? "connected" : "disconnected") + "\"></span><span>AMQP: " + eventGeneratorService.getBrokerInfo() + "</span>";
    }

    @Path("/status/stats") @GET @Produces(MediaType.TEXT_HTML)
    public String getStatsFragment() {
        long active = subscriptionRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE).size();
        long total = subscriptionRepository.count();
        return "<div class=\"stat-card\"><div class=\"stat-value\">" + active + "</div><div class=\"stat-label\">Active</div></div>"
             + "<div class=\"stat-card\"><div class=\"stat-value\">" + total + "</div><div class=\"stat-label\">Total</div></div>";
    }

    @Path("/heartbeat/stop") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String stopHeartbeat() { heartbeatPublisher.stop(); return div(CSS_CLASS_TRIGGER_ERROR, "Heartbeat publisher stopped"); }

    @Path("/heartbeat/start") @POST @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String startHeartbeat() { heartbeatPublisher.start(); return div(CSS_CLASS_TRIGGER_SUCCESS, "Heartbeat publisher started"); }

    @Path("/heartbeat/status") @GET
    public String getHeartbeatStatus() { return "{\"running\":" + heartbeatPublisher.isRunning() + "}"; }

    @Path("/faults/inject") @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.TEXT_HTML)
    public String injectFault(FaultRequest request) {
        try {
            String faultId = faultInjectionService.addFault(request.pathPattern(), request.httpMethod(), request.httpStatus(), request.delayMs(), request.dropRate(), request.durationSeconds());
            consoleNotificationService.warning("Fault injected: " + faultId);
            return div(CSS_CLASS_TRIGGER_SUCCESS, "Fault injected: " + faultId);
        } catch (Exception e) {
            return div(CSS_CLASS_TRIGGER_ERROR, "✗ " + e.getMessage());
        }
    }

    @Path("/faults/clear") @DELETE @Consumes(MediaType.WILDCARD) @Produces(MediaType.TEXT_HTML)
    public String clearFaults() { faultInjectionService.clearAll(); return div(CSS_CLASS_TRIGGER_SUCCESS, "All faults cleared"); }

    @Path("/console/stream") @GET @Produces(MediaType.SERVER_SENT_EVENTS) @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> consoleStream() {
        return consoleNotificationService.getStream().onItem().transform(ConsoleEvent::toJson);
    }

    private String sendScenario(String xml, String label, String desc) {
        try {
            int count = eventGeneratorService.sendScenarioEvent(xml, label);
            if (count == 0) return div(CSS_CLASS_TRIGGER_ERROR, "No active subscriptions");
            return div(CSS_CLASS_TRIGGER_SUCCESS, "✓ " + desc + " sent to " + count + LITERAL_SUBSCRIPTIONS);
        } catch (Exception e) {
            log.error("Failed to trigger {}", label, e);
            return div(CSS_CLASS_TRIGGER_ERROR, "✗ " + e.getMessage());
        }
    }

    private String div(String cssClass, String text) {
        return "<div class=\"" + cssClass + "\"><span class=\"timestamp\">[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "]</span> " + text + "</div>";
    }
}
