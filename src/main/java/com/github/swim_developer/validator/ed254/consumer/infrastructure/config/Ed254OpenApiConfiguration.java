package com.github.swim_developer.validator.ed254.consumer.infrastructure.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "ED-254 Arrival Sequence Service (Extended AMAN)",
                version = "1.0.0",
                description = """
                        ## Service Abstract

                        The Arrival Sequence Service provides arrival management information to upstream
                        Air Navigation Service Providers (ANSPs). It publishes real-time arrival sequence
                        data conforming to EUROCAE ED-254 standard, enabling cross-border Extended AMAN operations.

                        ## Operational Context

                        Extended AMAN extends arrival sequencing to 150-350 NM from the airport, allowing
                        upstream controllers to issue speed and route advisories while flights are still in cruise.

                        ## Service Interfaces

                        1. **Subscription Interface** (WS-Light/REST) - Manage subscriptions via REST API
                        2. **Distribution Interface** (AMQP 1.0) - Receive real-time arrival sequence via message broker
                        3. **Data Quality** (communicateProblems) - Report validation issues back to provider
                        """,
                contact = @Contact(
                        name = "SWIM Developer",
                        url = "https://github.com/swim-developer",
                        email = "swim@developer.io"
                )
        ),
        servers = {
                @Server(url = "https://swim-ed254-consumer-validator.apps.ocp4.masales.cloud/swim/v1", description = "ED-254 Consumer Validator (OpenShift)"),
                @Server(url = "http://localhost:8085/swim/v1", description = "Local Development")
        },
        tags = {
                @Tag(name = "Subscriptions", description = "Subscription lifecycle management"),
                @Tag(name = "Topics", description = "Arrival Sequence topic catalog"),
                @Tag(name = "Data Quality", description = "Data validation problem reporting"),
                @Tag(name = "Event Generator", description = "Test event generation and injection")
        }
)
@SecurityScheme(
        securitySchemeName = "mTLS",
        type = SecuritySchemeType.MUTUALTLS,
        description = "Mutual TLS authentication using EACP (European Aviation Common PKI) certificates."
)
public class Ed254OpenApiConfiguration extends Application {
}
