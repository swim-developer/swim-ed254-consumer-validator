# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SWIM ED-254 Consumer Validator: simulates the Arrival Sequence Service provider role (mock ATSU/AISP) for testing ED-254 (Extended AMAN) consumer implementations. Publishes FIXM-based arrival sequence events via AMQP and exposes a SPEC-170 compliant Subscription Manager REST API.

This is what `swim-ed254-consumer` connects to during development — a consumer NEVER connects to a real provider, it always connects to its consumer-validator.

## Build & Run

```bash
# Prerequisites: Java 21, Maven 3.9+, Podman Desktop (podman-desktop.io)

# Install shared dependencies (required before first build)
make sync                       # pulls sibling repos + installs into local Maven repo

# Start infrastructure (Artemis AMQP broker + MariaDB)
podman compose up -d

# Run in dev mode (port 8085)
./mvnw quarkus:dev

# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Run all tests (uses Quarkus DevServices — no external infra needed)
./mvnw test

# Run a single test class
./mvnw test -Dtest=SomeTestClass

# Run a single test method
./mvnw test -Dtest=SomeTestClass#someMethod

# Coverage report
./mvnw test jacoco:report       # output: target/site/jacoco/index.html
```

## Dependencies

Parent POM comes from `swim-developer-validators` (groupId: `com.github.swim-developer`). Three sibling repos must be cloned and installed into local Maven before building:

- `swim-developer-root` — root POM (install with `-N`)
- `swim-developer-framework` — shared framework (core domain, ports, AMQP, heartbeat, fault injection)
- `swim-developer-validators` — parent POM + `swim-validator-core` + `swim-validator-consumer`

Run `make sync` to clone/pull and install all of them. Repeat when those repos are updated.

## Architecture

Hexagonal architecture (ports & adapters), layered as:

```
domain/           Value objects, inbound ports (topic catalog, event metadata)
application/      Port implementations (Ed254EventMetadataService, Ed254TopicService)
infrastructure/   REST resources (/swim/v1/*, /admin/*, /ui/*), config, DTOs
```

Most business logic lives in the shared `swim-validator-consumer` library (subscription lifecycle, event generation, AMQP publishing, heartbeat). This project adds ED-254-specific concerns: topic catalog, subscription filters (aerodrome designators, flight selectors), event metadata extraction, and XSD validation.

## Key Concepts

- **Subscription lifecycle**: POST /swim/v1/subscriptions creates a subscription + AMQP queue. PUT to activate/pause. Events flow only to ACTIVE subscriptions.
- **Event Generator**: cron-scheduled (default: every minute). Picks XML files from `ed254-events/` and `ed254-exceptions/`, publishes to subscriber queues via AMQP.
- **Heartbeat Publisher**: sends periodic heartbeats to each subscription's heartbeat queue.
- **Fault Injection**: runtime chaos testing via /admin/faults/inject (HTTP errors, delays, request drops).
- **Admin UI**: Qute-based HTML dashboard with HTMX for event triggers, scenarios, and subscription management.

## Test Profile

The `%test` Quarkus profile disables AMQP, event generation, and heartbeat. Database uses Quarkus DevServices (auto-provisioned MariaDB via Testcontainers). No external infrastructure needed for tests.

## Key Rules

- **Names must be unambiguous and semantically specific** — always qualify with the service domain (e.g., `ed254-consumer-validator`, never generic `consumer`).
