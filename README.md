# Secure Chat (Java)

Secure Chat is a Java 17, Maven-driven workspace that demonstrates multiple transport strategies (blocking TCP, NIO, TLS) alongside a Spring Boot + Thymeleaf web interface. Each module is intentionally lightweight so the system can be extended for coursework, demos, or experimentation with secure real-time messaging.

---

## Key Capabilities
- Blocking TCP server with multithreaded client handling.
- Non-blocking gateway using Java NIO selectors.
- TLS/SSL transport via JSSE with keystore/truststore wiring.
- Spring Boot web UI with Thymeleaf templates and SSE stream.
- In-memory persistence today, with H2 + Flyway scaffolding ready.
- Optional UDP presence broadcaster/listener for beaconing status.

---

## Module Overview
| Module | Role | Notes |
|--------|------|-------|
| `chat-core` | Shared models & registries | `Message`, `BroadcastHub`, `UserRegistry` |
| `chat-tcp` | Blocking socket server/client | Simple CLI harness for testing |
| `chat-nio` | Selector-based gateway | Entry point for non-blocking experimentation |
| `chat-secure` | TLS socket server/client | Demonstrates keystore & truststore usage |
| `udp-presence` | UDP beacon services | Optional presence broadcast/receive |
| `web-app` | Spring Boot 3 UI | Thymeleaf views, SSE feed, H2-ready wiring |

---

## Project Layout
```
secure-chat-java/
├─ chat-core/            # Domain primitives and registries
├─ chat-tcp/             # Blocking TCP server & CLI client
├─ chat-nio/             # Non-blocking gateway placeholder
├─ chat-secure/          # TLS-enabled chat components
├─ udp-presence/         # Optional UDP beacons
├─ web-app/              # Spring Boot web interface
├─ certs/                # Keystore/truststore placeholders
├─ db/                   # Flyway SQL migrations (H2 scaffold)
├─ scripts/              # Helper launch scripts
├─ pom.xml               # Maven parent (aggregator)
└─ .gitignore
```

---

## Prerequisites
- Java 17 or newer (set `JAVA_HOME` accordingly).
- Maven 3.9+ (or use the bundled `./mvnw` wrapper).
- `keytool` for generating local TLS assets (ships with the JDK).
- Optional tooling: `curl` for quick checks, Wireshark for TLS demos.

---

## Quick Start
```bash
git clone <your-repo-url> secure-chat-java
cd secure-chat-java
./mvnw clean install
```

This builds every module, runs unit tests, and installs artifacts to your local Maven cache.

---

## Running Components

### Spring Boot Web UI (in-memory)
```bash
./scripts/run-web.sh
# Visit http://localhost:8080
```
Messages are stored in-memory and published to the `/stream` SSE endpoint.

### Blocking TCP Server
```bash
./scripts/run-plain.sh          # defaults to port 9000
# In a second terminal
./mvnw -pl chat-tcp -am exec:java \
  -Dexec.mainClass=com.securechat.tcp.ChatCli \
  -Dexec.args="localhost 9000 alice"
```

### NIO Gateway
```bash
./mvnw -pl chat-nio -am exec:java \
  -Dexec.mainClass=com.securechat.nio.NioChatGateway \
  -Dexec.args="5001"
```

### TLS Chat Server & Client
1. Place keystore/truststore files in `certs/` (see below).
2. Start the server:
   ```bash
   ./scripts/run-tls.sh
   ```
3. Connect a client:
   ```bash
   ./mvnw -pl chat-secure -am exec:java \
     -Dexec.mainClass=com.securechat.secure.SecureChatCli \
     -Dexec.args="localhost 5443"
   ```

### UDP Presence (optional)
```bash
./mvnw -pl udp-presence -am exec:java \
  -Dexec.mainClass=com.securechat.udp.PresenceServer -Dexec.args="9090"
./mvnw -pl udp-presence -am exec:java \
  -Dexec.mainClass=com.securechat.udp.PresenceClient -Dexec.args="9090"
```

---

## TLS Assets
Place certificates under `certs/`. For local testing, a minimal `keytool` workflow is:

```bash
keytool -genkeypair -alias secure-chat-server -keyalg RSA -keysize 2048 \
  -dname "CN=localhost, OU=Dev, O=SecureChat, L=Colombo, S=Western, C=LK" \
  -validity 365 -storetype PKCS12 \
  -keystore certs/server.p12 -storepass changeit

keytool -exportcert -alias secure-chat-server -keystore certs/server.p12 \
  -storepass changeit -rfc > certs/server.crt

keytool -importcert -alias secure-chat-server -file certs/server.crt \
  -keystore certs/truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
```

> ⚠️ Never commit real certificates or credentials. Replace `changeit` with secure values in production.

---

## Common Port Map
| Service | Port | Notes |
|---------|------|-------|
| Spring Boot web UI | 8080 | HTTP + SSE stream |
| TCP server | 9000 | Blocking sockets (`run-plain.sh`) |
| NIO gateway | 5001 | Selector-driven placeholder |
| TLS server | 5443 | Requires keystore in `certs/` |
| UDP presence | 9090 | Broadcast/listener beacons |

---

## Useful Scripts
- `scripts/run-web.sh` — Launches the Spring Boot UI.
- `scripts/run-plain.sh` — Starts the blocking TCP server.
- `scripts/run-tls.sh` — Runs the TLS chat server (expects keystore).

Feel free to extend the `scripts/` directory with project-specific helpers.

---

## Troubleshooting
- **Port already in use**: Use `lsof -i :<port>` (macOS/Linux) or `netstat -ano | findstr :<port>` (Windows) to identify and stop conflicting processes.
- **SSE stream appears idle**: Verify the `/stream` endpoint is reachable and that at least one message has been logged.
- **TLS handshake fails**: Confirm the client trusts the server certificate and that keystore/truststore passwords match your launch parameters.
- **Persistent storage**: The current build keeps state in-memory. Add a `db` Spring profile with a file-based H2 datasource when you are ready to persist chat history.

---

## License & Attribution
- Suggested license: MIT (add the full text to a `LICENSE` file).
- Suggested ownership mapping:
  - TCP core — chat-tcp
  - Multithreading — chat-tcp / chat-nio
  - NIO gateway — chat-nio
  - TLS/SSL layer — chat-secure
  - Web app & bridge — web-app

---

If you use this project as a foundation, tailor the modules, ports, and certificates to match your deployment environment and security requirements.
