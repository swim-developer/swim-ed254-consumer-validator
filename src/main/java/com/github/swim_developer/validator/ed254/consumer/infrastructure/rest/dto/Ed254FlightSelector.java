package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Flight identity selector with wildcard support")
public record Ed254FlightSelector(
        @Schema(description = "Aircraft identification (callsign), supports wildcards")
        String arcid,

        @Schema(description = "Destination aerodrome")
        String ades,

        @Schema(description = "Departure aerodrome")
        String adep,

        @Schema(description = "Estimated off-block time (ISO-8601)")
        String eobt,

        @Schema(description = "Estimated off-block date (ISO-8601)")
        String eobd,

        @Schema(description = "IFPL identifier")
        String ifplId
) {}
