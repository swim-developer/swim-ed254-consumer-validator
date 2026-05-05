package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "ED-254 Arrival Sequence subscription request (Cap. 4.7)")
public record Ed254SubscriptionRequest(
        @Schema(description = "Subscription filters per ED-254 Cap. 4.7.2")
        Ed254SubscriptionFilters subscriptionFilters,

        @Schema(description = "Supplementary data selection per ED-254 Cap. 4.7.3")
        Ed254SupplementaryData supplementaryData
) {}
