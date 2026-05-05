package com.github.swim_developer.validator.ed254.consumer.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Supplementary data selection flags per ED-254 Cap. 4.7.3")
public record Ed254SupplementaryData(
        @Schema(description = "Include delay information")
        boolean delay,

        @Schema(description = "Include landing sequence position")
        boolean landingSequencePosition,

        @Schema(description = "Include AMAN strategy information")
        boolean amanStrategy,

        @Schema(description = "Include departure aerodrome information")
        boolean departureAerodrome,

        @Schema(description = "Include proposed procedure information")
        boolean proposedProcedure
) {}
