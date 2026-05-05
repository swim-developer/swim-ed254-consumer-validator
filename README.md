# swim-ed254-consumer-validator

Simulates the Arrival Sequence Service provider role for testing ED-254 (Extended AMAN) Consumer implementations. It provides a SPEC-170 compliant Subscription Manager REST API, publishes FIXM-based arrival sequence events and provider exceptions via AMQP, and generates periodic heartbeats.

## What it does

- **Subscription Manager API**, ED-254 compliant REST endpoints for subscription lifecycle with flight selector filters
- **AMQP event publishing**, sends arrival sequence messages to subscriber queues via ActiveMQ Artemis
- **136 sample arrival sequences**, covering 24 European aerodromes (EDDF, EGLL, EHAM, LFPG, LEMD, LPPT, etc.)
- **3 provider exception scenarios**, AMAN unavailable, degraded mode, sequencing disabled
- **XSD validation**, validates published events against the ED-254 schema
- **Configurable event generation**, cron-based injection with schedule control
- **Heartbeat publishing**, periodic heartbeats to subscription heartbeat queues
- **Fault injection**, runtime chaos testing (HTTP errors, delays, request drops)

---

## GET STARTED

### Prerequisites

- Java 21
- Maven 3.9+
- Podman (or any OCI-compatible runtime with Compose support)
- Shared modules installed in local Maven repo (see below)

### 0. Install shared modules

This project depends on shared modules from [swim-developer-validators](https://github.com/swim-developer/swim-developer-validators). They must be installed in your local Maven repository before building or running this project.

Clone and install once:

```bash
git clone git@github.com:swim-developer/swim-developer-validators.git
cd swim-developer-validators
./mvnw clean install -DskipTests
```

You only need to repeat this step when `swim-developer-validators` is updated.

### 1. Start the infrastructure

```bash
podman compose up -d
```

Services started:

| Service | Port | Description |
|---------|------|-------------|
| `ed254-consumer-validator-artemis` | 5676, 8165 | AMQP broker for event delivery |
| `ed254-consumer-validator-mariadb` | 3309 | Validator persistence |

### 2. Run the validator

```bash
./mvnw quarkus:dev
```

- Subscription Manager API: http://localhost:8085
- Swagger UI: http://localhost:8085/swagger-ui
- Artemis console: http://localhost:8165 (admin / admin)

### 3. Point your consumer at it

Configure `swim-ed254-consumer` to use this validator as its provider:

```properties
swim.providers=[{
  "providerId":"validator",
  "subscriptionManager":{"url":"http://localhost:8085"},
  "amqpBroker":{"host":"localhost","port":5676,"sslEnabled":false,"username":"admin","password":"admin"}
}]
```

### Verify, happy path

```bash
# Validator health
curl -s http://localhost:8085/q/health | jq .status

# List topics
curl -s http://localhost:8085/swim/v1/topics | jq .

# Create and activate a subscription
SUBID=$(curl -s -X POST http://localhost:8085/swim/v1/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"topicName":"ArrivalSequenceService","subscriberId":"verify-01"}' | jq -r .id)
curl -s -X PUT "http://localhost:8085/swim/v1/subscriptions/${SUBID}" \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE"}' | jq .
```

After activation, the Artemis queue receives ED-254 arrival sequence events (visible in the Artemis console at http://localhost:8165).

---

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/swim/v1/subscriptions` | Create subscription (with flight selector filters) |
| `GET` | `/swim/v1/subscriptions` | List subscriptions |
| `GET` | `/swim/v1/subscriptions/{id}` | Get subscription details |
| `PUT` | `/swim/v1/subscriptions/{id}` | Update status (ACTIVE/PAUSED) |
| `DELETE` | `/swim/v1/subscriptions/{id}` | Delete subscription |
| `GET` | `/swim/v1/topics` | List available topics |
| `GET` | `/swim/v1/topics/{id}` | Get topic details |

---

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `EVENT_GENERATOR_ENABLED` | `true` | Enable automatic event generation |
| `EVENT_GENERATOR_SCHEDULE` | `0 */1 * * * ?` | Cron expression (default: every minute) |
| `EVENT_GENERATOR_EVENTS_PATH` | `/opt/events` | Path to ED-254 arrival sequence XML files |
| `EVENT_GENERATOR_EXCEPTIONS_PATH` | `/opt/exceptions` | Path to provider exception XML files |
| `AMQP_BROKER_HOST` | `localhost` | Artemis broker hostname |
| `AMQP_BROKER_PORT` | `5672` | AMQP 1.0 port |
| `AMQP_BROKER_USERNAME` | `admin` | Authentication username |
| `AMQP_BROKER_PASSWORD` | `admin` | Authentication password |
| `HEARTBEAT_PUBLISHER_ENABLED` | `true` | Enable heartbeat publishing |
| `HEARTBEAT_PUBLISHER_INTERVAL` | `15s` | Heartbeat publishing interval |
| `MARIADB_HOST` | `localhost` | MariaDB hostname |
| `MARIADB_PORT` | `3306` | MariaDB port |
| `MARIADB_DATABASE` | `swim_ed254_consumer_validator` | Database name |
| `MARIADB_USERNAME` | `swim` | Database username |
| `MARIADB_PASSWORD` | `swim` | Database password |
| `QUARKUS_HTTP_SSL_CERTIFICATE_FILES` | `/certs/server/tls.crt` | Server certificate |
| `QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES` | `/certs/server/tls.key` | Server private key |
| `QUARKUS_HTTP_SSL_CERTIFICATE_TRUST_STORE_FILE` | `/certs/ca/ca.crt` | CA certificate for client validation |

---

## Container images

```
quay.io/masales/swim-ed254-consumer-validator:latest
```

---

## Build

From the `swim-developer-validators/` repository root:

```bash
make ed254-consumer-validator-jvm              # JVM multi-arch, build + push

make ed254-consumer-validator-native-amd64     # Native amd64, build + push  (run on amd64)
make ed254-consumer-validator-native-arm64     # Native arm64, build + push  (run on arm64)
make ed254-consumer-validator-manifest         # Create multi-arch manifest
make ed254-consumer-validator-push             # Push manifest to registry
```

Override: `make ed254-consumer-validator-jvm REGISTRY=quay.io/myorg TAG=v1.2.3`

---

## Deployment

Includes a Helm chart under `src/main/helm/` with CRC and production values.

---

## License

Licensed under the [Apache License 2.0](LICENSE).
