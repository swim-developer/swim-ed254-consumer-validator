package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "ED-254 subscription filter container")
public record Ed254SubscriptionFilters(
        @Schema(description = "Destination aerodrome filters")
        List<Ed254DestinationAerodrome> destinationAerodrome,

        @Schema(description = "Metering point name filters")
        List<String> pointName,

        @Schema(description = "Flight identity selectors")
        List<Ed254FlightSelector> flightSelector
) {}
