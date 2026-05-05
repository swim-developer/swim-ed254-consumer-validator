package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Destination aerodrome filter with optional runway selection")
public record Ed254DestinationAerodrome(
        @Schema(description = "ICAO 4-letter aerodrome designator", required = true)
        String aerodromeDesignator,

        @Schema(description = "Assigned arrival runway designators")
        List<String> assignedArrivalRunway
) {}
