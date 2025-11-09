# Secure Chat (Java)

Secure Chat is a Java 17, Maven-driven workspace that demonstrates multiple transport strategies (blocking TCP, NIO, TLS) alongside a React + Vite web interface that consumes the Java chat services via REST + SSE. Each module is intentionally lightweight so the system can be extended for coursework, demos, or experimentation with secure real-time messaging.

---

## Key Capabilities
- Blocking TCP server with multithreaded client handling.
- Non-blocking gateway using Java NIO selectors.
- TLS/SSL transport via JSSE with keystore/truststore wiring.
- React + Vite frontend that talks to the Java chat services over REST/SSE.
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
| `web-app` (React) | Front-end SPA | Vite + React client for `/send` + `/stream` |

---

## Project Layout
```
secure-chat-java/
├─ chat-core/            # Domain primitives and registries
├─ chat-tcp/             # Blocking TCP server & CLI client
├─ chat-nio/             # Non-blocking gateway placeholder
├─ chat-secure/          # TLS-enabled chat components
├─ udp-presence/         # Optional UDP beacons
├─ web-app/              # React chat interface (Vite + TypeScript)
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

### Verify in VS Code
After cloning, open the workspace in VS Code and use the integrated terminal:

```bash
mvn clean install
```

This confirms the multi-module structure compiles end-to-end. Next, spin up the React UI:

```bash
cd web-app
npm install
npm run dev
# or: ./scripts/run-web.sh
```

The Vite dev server listens on <http://localhost:5173> and proxies `/api/*`,
`/api/send`, and `/api/stream` calls to `http://localhost:8080` by default. Point
`VITE_CHAT_API_URL` to whichever Java service exposes those endpoints.

---

## Running Components

### React Frontend (Vite)
```bash
./scripts/run-web.sh
# Visit http://localhost:5173
```
Set `VITE_CHAT_API_URL` (defaults to `/api`) so the UI knows where to call
`/send` and `/stream`. For example, to point directly at a Java service running
on port 8080:

```bash
cd web-app
VITE_CHAT_API_URL=http://localhost:8080 npm run dev
```

> The repository no longer ships a Spring MVC layer. Expose REST + SSE endpoints
> from whichever Java transport module you prefer and keep them compatible with
> `/send` (POST form: username/text) plus `/stream?u=<name>` (SSE).

Design tokens for the UI live in `web-app/src/styles/tokens.css`. Use the
`Figma MCP` server (configured in your `mcp.json`) to sync updated variables
from design and drop them into that file.

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
| React dev server | 5173 | Vite + React front-end |
| Chat API gateway (your Java service) | 8080 | Expose `/send`, `/stream` for the UI |
| TCP server | 9000 | Blocking sockets (`run-plain.sh`) |
| NIO gateway | 5001 | Selector-driven placeholder |
| TLS server | 5443 | Requires keystore in `certs/` |
| UDP presence | 9090 | Broadcast/listener beacons |

---

## Useful Scripts
- `scripts/run-web.sh` — Launches the React/Vite dev server for the UI.
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
  - Web UI — web-app (React)

---

If you use this project as a foundation, tailor the modules, ports, and certificates to match your deployment environment and security requirements.
