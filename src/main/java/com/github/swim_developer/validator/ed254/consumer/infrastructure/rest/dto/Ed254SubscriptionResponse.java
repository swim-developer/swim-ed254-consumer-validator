package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "ED-254 subscription response (spec Cap. 4.8.4 + framework extensions)")
public record Ed254SubscriptionResponse(
        @Schema(description = "Subscription reference (UUID)", required = true)
        String subscriptionId,

        @Schema(description = "Result of the subscription operation", required = true,
                enumeration = {"SUBSCRIPTION_SUCCESSFUL", "SUBSCRIPTION_REFUSED"})
        String subscriptionResult,

        @Schema(description = "Error reason when subscription is refused",
                enumeration = {"INVALID_FILTER", "NO_SUCH_ELEMENT", "ALREADY_SUBSCRIBED", "NOT_INTERPRETABLE"})
        String errorReason,

        @Schema(description = "AMQP queue name for event delivery")
        String queueName,

        @Schema(description = "Subscription lifecycle status", enumeration = {"PAUSED", "ACTIVE"})
        String subscriptionStatus,

        @Schema(description = "Subscription expiration timestamp")
        Instant subscriptionEnd,

        @Schema(description = "Provider name")
        String providerName,

        @Schema(description = "Per-subscription heartbeat queue name")
        String heartbeatQueue
) {}
