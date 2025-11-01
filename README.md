# Secure Chat Java

Multi-module Maven project showcasing different approaches to delivering a secure chat platform. Each module provides a building block that can be composed into the final application.

## Modules
- `chat-core` – Shared domain objects and registries.
- `chat-tcp` – Blocking TCP server and client utilities.
- `chat-nio` – Non-blocking gateway based on Java NIO.
- `chat-secure` – TLS-enabled communication layer.
- `udp-presence` – UDP presence announcements.
- `web-app` – Spring Boot 3 web interface backed by Thymeleaf and H2.

## Getting Started
```bash
mvn clean install
```

### Running Components
- `scripts/run-plain.sh` – Starts the TCP chat server.
- `scripts/run-tls.sh` – Starts the TLS-enabled chat server (requires keystore in `certs/`).
- `scripts/run-web.sh` – Runs the Spring Boot web application.

## Certificates
Place keystore and truststore placeholders inside the `certs/` directory before running secure modules.

## Database Migrations
Flyway SQL migrations live under `db/migration/` and automatically run for the web application.
