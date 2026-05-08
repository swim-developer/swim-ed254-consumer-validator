# swim-ed254-consumer-validator — Knowledge Base


## What This Is

**Mock ATSU/AISP for testing the ED-254 Consumer.** Provides everything a real Extended AMAN provider (e.g., LFV Sweden / COOPANS) would expose.

This is what `swim-ed254-consumer` connects to during development.

## What It Provides

| Component | Purpose |
|-----------|---------|
| **Subscription Manager REST API** | Full ED-254 subscription lifecycle |
| **Artemis AMQP Broker** | Receives consumer connections, delivers arrival sequence events |
| **Event Generator** | Publishes FIXM 4.3 arrival sequence samples |
| **Heartbeat Publisher** | Per-subscription heartbeat (15s interval) |

## Consumer Connection Config

```yaml
providers:
  - providerId: "lfv-sweden"
    subscriptionManager:
      url: "https://ed254-consumer-validator-<namespace>.apps.<cluster>"
    amqpBroker:
      host: "ed254-consumer-validator-artemis-<namespace>.apps.<cluster>"
```

## Build & Run

```bash
./mvnw clean package -DskipTests
quarkus dev
```
