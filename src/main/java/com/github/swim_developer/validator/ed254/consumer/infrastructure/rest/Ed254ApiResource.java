package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest;

import com.github.swim_developer.validator.core.infrastructure.rest.dto.ErrorResponse;
import com.github.swim_developer.validator.core.domain.model.SubscriptionResponse;
import com.github.swim_developer.validator.core.infrastructure.rest.dto.SubscriptionStatusUpdate;
import com.github.swim_developer.validator.core.infrastructure.rest.dto.TopicList;
import com.github.swim_developer.validator.consumer.domain.port.in.EventGeneratorPort;
import com.github.swim_developer.validator.consumer.domain.model.CreateSubscriptionCommand;
import com.github.swim_developer.validator.consumer.domain.port.in.ManageSubscriptionPort;
import com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto.Ed254DestinationAerodrome;
import com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto.Ed254SubscriptionRequest;
import com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto.Ed254SubscriptionResponse;
import com.github.swim_developer.validator.ed254.consumer.domain.port.in.Ed254TopicPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Collections;
import java.util.List;

@Slf4j
@Path("/swim/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "mTLS")
public class Ed254ApiResource {

    private static final String ERROR_CODE_NOT_FOUND = "NOT_FOUND";

    private final ManageSubscriptionPort subscriptionService;
    private final Ed254TopicPort topicService;
    private final EventGeneratorPort eventGeneratorService;

    @Inject
    public Ed254ApiResource(
            ManageSubscriptionPort subscriptionService,
            Ed254TopicPort topicService,
            EventGeneratorPort eventGeneratorService) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.eventGeneratorService = eventGeneratorService;
    }

    @POST
    @Path("/subscriptions")
    @Tag(name = "Subscriptions", description = "Subscription lifecycle management")
    @Operation(
            operationId = "subscribe",
            summary = "Create a new subscription",
            description = "Creates a subscription for ED-254 Arrival Sequence events. " +
                    "Request body follows ED-254 Cap. 4.7: subscriptionFilters + supplementaryData."
    )
    @APIResponse(responseCode = "201", description = "Subscription created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Ed254SubscriptionResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response createSubscription(
            @Valid @RequestBody(
                    description = "Subscription request per ED-254 Cap. 4.7",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Ed254SubscriptionRequest.class),
                            examples = {
                                    @ExampleObject(name = "lisbon", summary = "Subscribe to Lisbon arrivals",
                                            value = "{\"subscriptionFilters\":{\"destinationAerodrome\":[{\"aerodromeDesignator\":\"LPPT\"}]},\"supplementaryData\":{\"delay\":true,\"landingSequencePosition\":true,\"amanStrategy\":false,\"departureAerodrome\":false,\"proposedProcedure\":false}}"),
                                    @ExampleObject(name = "multi", summary = "Subscribe to Lisbon + Madrid with flight filter",
                                            value = "{\"subscriptionFilters\":{\"destinationAerodrome\":[{\"aerodromeDesignator\":\"LPPT\"},{\"aerodromeDesignator\":\"LEMD\"}],\"pointName\":[\"AMRAM\"],\"flightSelector\":[{\"arcid\":\"TAP1*\",\"ades\":\"LPPT\"}]},\"supplementaryData\":{\"delay\":true,\"landingSequencePosition\":true,\"amanStrategy\":true,\"departureAerodrome\":false,\"proposedProcedure\":false}}")
                            }
                    )
            ) Ed254SubscriptionRequest request) {

        List<String> aerodromes = extractAerodromeDesignators(request);

        SubscriptionResponse coreResponse = subscriptionService.createSubscription(
                new CreateSubscriptionCommand(
                        "ArrivalSequenceService",
                        null,
                        null,
                        true,
                        null,
                        aerodromes,
                        null,
                        null,
                        null,
                        buildDescription(request),
                        null
                ));

        Ed254SubscriptionResponse ed254Response = toEd254Response(coreResponse, "SUBSCRIPTION_SUCCESSFUL");
        return Response.status(Response.Status.CREATED).entity(ed254Response).build();
    }

    @GET
    @Path("/subscriptions")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "getSubscriptions", summary = "List subscriptions")
    @APIResponse(responseCode = "200", description = "List of subscriptions",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response getSubscriptions(
            @QueryParam("queueName") @Parameter(description = "Filter by queue name") String queueName,
            @QueryParam("subscriptionStatus") @Parameter(description = "Filter by status") String subscriptionStatusParam) {
        com.github.swim_developer.validator.core.domain.model.SubscriptionStatus status = null;
        if (subscriptionStatusParam != null) {
            try {
                status = com.github.swim_developer.validator.core.domain.model.SubscriptionStatus.valueOf(subscriptionStatusParam);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid subscription status parameter: {}", subscriptionStatusParam);
            }
        }
        List<SubscriptionResponse> coreList = subscriptionService.listSubscriptions(queueName, status);
        List<Ed254SubscriptionResponse> ed254List = coreList.stream()
                .map(r -> toEd254Response(r, null))
                .toList();
        return Response.ok(ed254List).build();
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "updateSubscriptionStatus", summary = "Update subscription status")
    @APIResponse(responseCode = "200", description = "Status updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Ed254SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found")
    public Response updateSubscriptionStatus(
            @PathParam("subscriptionId") @Parameter(description = "Subscription ID", required = true) String subscriptionId,
            @Valid SubscriptionStatusUpdate update) {
        SubscriptionResponse coreResponse = subscriptionService.updateSubscriptionStatus(subscriptionId, update.subscriptionStatus());
        return Response.ok(toEd254Response(coreResponse, null)).build();
    }

    @GET
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "getSubscription", summary = "Get subscription details")
    @APIResponse(responseCode = "200", description = "Subscription details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Ed254SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found")
    public Response getSubscriptionDetails(
            @PathParam("subscriptionId") @Parameter(description = "Subscription ID", required = true) String subscriptionId) {
        return subscriptionService.getSubscriptionDetails(subscriptionId)
                .map(r -> Response.ok(toEd254Response(r, null)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(ERROR_CODE_NOT_FOUND, "Subscription not found", "No subscription with id: " + subscriptionId))
                        .build());
    }

    @DELETE
    @Path("/subscriptions/{subscriptionId}")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "unsubscribe", summary = "Delete subscription")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "Not found")
    public Response deleteSubscription(
            @PathParam("subscriptionId") @Parameter(description = "Subscription ID", required = true) String subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/subscriptions/{subscriptionId}/renew")
    @Tag(name = "Subscriptions")
    @Operation(operationId = "renewSubscription", summary = "Renew subscription")
    @APIResponse(responseCode = "200", description = "Renewed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Ed254SubscriptionResponse.class)))
    @APIResponse(responseCode = "404", description = "Not found")
    public Response renewSubscription(
            @PathParam("subscriptionId") @Parameter(description = "Subscription ID", required = true) String subscriptionId) {
        return subscriptionService.renewSubscription(subscriptionId)
                .map(r -> Response.ok(toEd254Response(r, null)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(ERROR_CODE_NOT_FOUND, "Subscription not found", "No subscription with id: " + subscriptionId))
                        .build());
    }

    @POST
    @Path("/communicateProblems")
    @Tag(name = "Data Quality", description = "Data validation problem reporting")
    @Operation(operationId = "communicateProblems", summary = "Report data validation problems",
            description = "Endpoint for consumers to report data quality issues (ED-254 Section 7.3)")
    @APIResponse(responseCode = "200", description = "Problem received")
    public Response communicateProblems(Object validationResult) {
        log.info("communicateProblems received: {}", validationResult);
        return Response.ok().build();
    }

    @GET
    @Path("/topics")
    @Tag(name = "Topics")
    @Operation(summary = "List available topics")
    @APIResponse(responseCode = "200", description = "Available topics",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TopicList.class)))
    public Response getTopics() {
        return Response.ok(new TopicList(topicService.getAllTopics())).build();
    }

    @GET
    @Path("/topics/{topicId}")
    @Tag(name = "Topics")
    @Operation(summary = "Get topic details")
    @APIResponse(responseCode = "200", description = "Topic details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = com.github.swim_developer.validator.core.domain.model.TopicDetails.class)))
    @APIResponse(responseCode = "404", description = "Not found")
    public Response getTopicDetails(@PathParam("topicId") @Parameter(description = "Topic ID", required = true) String topicId) {
        return topicService.getTopicDetails(topicId)
                .<Response>map(details -> Response.ok(details).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(ERROR_CODE_NOT_FOUND, "Topic not found", "Topic ID: " + topicId))
                        .build());
    }

    @GET
    @Path("/trigger-event")
    @Produces(MediaType.APPLICATION_XML)
    @Tag(name = "Event Generator")
    @Operation(summary = "Manually trigger event generation")
    @APIResponse(responseCode = "200", description = "Event generated", content = @Content(mediaType = MediaType.APPLICATION_XML))
    @APIResponse(responseCode = "500", description = "Failed")
    public Response triggerEvent() {
        try {
            return eventGeneratorService.generateAndSendEventManually()
                    .<Response>map(xml -> Response.ok(xml).build())
                    .orElseGet(() -> Response.noContent().build());
        } catch (Exception e) {
            log.error("Failed to trigger event", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("EVENT_GENERATION_FAILED", "Failed to generate event", e.getMessage()))
                    .build();
        }
    }

    private Ed254SubscriptionResponse toEd254Response(SubscriptionResponse core, String subscriptionResult) {
        return new Ed254SubscriptionResponse(
                core.subscriptionId() != null ? core.subscriptionId().toString() : null,
                subscriptionResult,
                null,
                core.queue(),
                core.subscriptionStatus() != null ? core.subscriptionStatus().name() : null,
                core.subscriptionEnd(),
                core.providerName(),
                core.heartbeatQueue()
        );
    }

    private List<String> extractAerodromeDesignators(Ed254SubscriptionRequest request) {
        if (request.subscriptionFilters() == null || request.subscriptionFilters().destinationAerodrome() == null) {
            return Collections.emptyList();
        }
        return request.subscriptionFilters().destinationAerodrome().stream()
                .map(Ed254DestinationAerodrome::aerodromeDesignator)
                .toList();
    }

    private String buildDescription(Ed254SubscriptionRequest request) {
        List<String> aerodromes = extractAerodromeDesignators(request);
        if (aerodromes.isEmpty()) {
            return "ED-254 subscription (no aerodrome filter)";
        }
        return "ED-254 subscription for " + String.join(", ", aerodromes);
    }
}
