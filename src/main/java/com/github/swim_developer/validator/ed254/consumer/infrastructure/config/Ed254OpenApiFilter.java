package com.github.swim_developer.validator.ed254.consumer.infrastructure.config;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import java.util.Map;

public class Ed254OpenApiFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getExtensions() == null) {
            openAPI.setExtensions(new java.util.LinkedHashMap<>());
        }

        openAPI.addExtension("x-swim-service-type", "Arrival Sequence Service");
        openAPI.addExtension("x-swim-standard", "EUROCAE ED-254");
        openAPI.addExtension("x-swim-data-model", "FIXM 4.3 based");
        openAPI.addExtension("x-swim-profile", "EUROCONTROL SPEC-170 (Yellow Profile)");
        openAPI.addExtension("x-swim-regulation", "EU 2021/116 (CP1) — EAMAN-004, EAMAN-006");

        openAPI.addExtension("x-swim-glossary", Map.of(
                "E-AMAN", "Extended Arrival Manager — sequencing extended to 150-350 NM from airport",
                "AMAN", "Arrival Manager — calculates optimal arrival sequence at destination airport",
                "arrivalSequence", "Real-time snapshot of the current arrival sequence for an aerodrome",
                "providerExceptions", "Service status notifications: SEQUENCING_DISABLED, AMAN_UNAVAILABLE, AMAN_DEGRADED",
                "metering", "Target time at a fix where flights should arrive in sequence",
                "ATSU", "Air Traffic Service Unit",
                "ARCID", "Aircraft call sign identifier"
        ));

        openAPI.addExtension("x-swim-scope", Map.of(
                "geographicScope", "Upstream ANSPs within 150-350 NM of destination",
                "temporalScope", "Real-time with update interval typically 10-60 seconds",
                "messageTypes", "arrivalSequence (sequence snapshots), providerExceptions (AMAN status)"
        ));
    }
}
