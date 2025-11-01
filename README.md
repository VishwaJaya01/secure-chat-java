Secure Chat (Java) — README

A single-repo, Java-first implementation of a secure multi-client chat system.
Includes raw socket servers (TCP, NIO, TLS), a Spring Boot + Thymeleaf web UI, optional H2 persistence, and (optional) UDP presence.

🚀 Features at a glance

TCP chat server (blocking I/O)

Multithreaded client handling (ExecutorService)

NIO gateway (Selector, non-blocking)

TLS/SSL secure chat (JSSE, keystore/truststore)

Spring Boot web UI (Thymeleaf, SSE live feed)

H2 database (optional; message history)

(Optional) UDP presence (online/idle/offline)

📦 Repo layout
secure-chat-java/
├─ chat-core/         # Shared model & hubs (Message, BroadcastHub, UserRegistry)
├─ chat-tcp/          # Blocking TCP server + CLI client
├─ chat-nio/          # NIO non-blocking gateway (Selector)
├─ chat-secure/       # TLS/SSL servers & clients
├─ udp-presence/      # (optional) UDP presence beacons
├─ web-app/           # Spring Boot + Thymeleaf UI (+ H2 via profile)
├─ certs/             # Keystore/truststore (placeholders)
├─ db/                # Flyway SQL migrations & seeds (for H2)
├─ scripts/           # Helper scripts (run, gen-certs, load test)
├─ report/, docs/, assets/
├─ pom.xml            # Parent Maven (aggregator)
└─ .gitignore

✅ Prerequisites

Java 17+

Maven 3.9+ (or use ./mvnw if wrapper added)

(Optional) Wireshark (to show TLS encryption)

(Optional) curl (for quick HTTP checks)

🔧 First-time setup

Clone

git clone <your-repo-url> secure-chat-java
cd secure-chat-java


Build all modules

mvn -q -DskipTests clean install


Create TLS materials (self-signed)

If scripts/gen-certs.sh is present, use it; otherwise:

# Server keystore (PKCS12)
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 \
  -dname "CN=localhost, OU=Dev, O=SecureChat, L=Colombo, S=WP, C=LK" \
  -validity 365 -storetype PKCS12 -keystore certs/server.p12 -storepass changeit

# Client truststore that trusts the server cert
keytool -exportcert -keystore certs/server.p12 -alias server -storepass changeit -rfc > certs/server.crt
keytool -importcert -file certs/server.crt -alias server \
  -keystore certs/truststore.p12 -storetype PKCS12 -storepass changeit -noprompt


Keep keystore passwords out of Git. The defaults below use changeit for local demo only.

🔌 Ports (defaults)
Component	Port
Web UI (HTTP)	8080
TCP Server (blocking)	5000
NIO Gateway	5001
TLS Server	5443
UDP Presence (optional)	9090
▶️ How to run (common modes)
A) Web UI (no DB, in-memory) — recommended for quick demo
cd web-app
mvn spring-boot:run
# open http://localhost:8080


Live chat via /stream (SSE)

Messages & users held in-memory (clears on restart)

B) Web UI + H2 database (message history persists)
cd web-app
mvn spring-boot:run -Dspring-boot.run.profiles=db
# open http://localhost:8080


H2 file location (example): ./.data/securechat.mv.db

(Optional) H2 console if enabled in application-db.yml (e.g., /h2-console)

C) Blocking TCP Server
cd chat-tcp
# run the server (class name may differ in your codebase)
mvn -q exec:java -Dexec.mainClass=com.securechat.tcp.ChatServer


Connect a simple CLI client (another terminal):

mvn -q exec:java -Dexec.mainClass=com.securechat.tcp.ChatCli -Dexec.args="localhost 5000 alice"
mvn -q exec:java -Dexec.mainClass=com.securechat.tcp.ChatCli -Dexec.args="localhost 5000 bob"

D) NIO Gateway (non-blocking)
cd chat-nio
mvn -q exec:java -Dexec.mainClass=com.securechat.nio.NioChatGateway

E) TLS Secure Chat
cd chat-secure
# Server
mvn -q exec:java -Dexec.mainClass=com.securechat.secure.SecureChatServer \
  -Dexec.args="--keystore=../certs/server.p12 --storepass=changeit --port=5443"
# Client
mvn -q exec:java -Dexec.mainClass=com.securechat.secure.SecureChatCli \
  -Dexec.args="--truststore=../certs/truststore.p12 --storepass=changeit --host=localhost --port=5443 --user=alice"

F) UDP Presence (optional)
cd udp-presence
# Server
mvn -q exec:java -Dexec.mainClass=com.securechat.udp.PresenceServer
# Client (send periodic pings for a user)
mvn -q exec:java -Dexec.mainClass=com.securechat.udp.PresenceClient -Dexec.args="alice"

🌐 Web UI endpoints (for reference)
Method	Path	Purpose
GET	/	Login page (enter username)
POST	/join	Join chat, redirect to /chat
GET	/chat	Chat page (initial messages/users)
POST	/send	Send message (form/HTMX)
GET	/stream	SSE live message stream
GET	/users (optional)	JSON list for sidebar
GET	/h2-console (optional)	H2 console (db profile)

Exact controllers may vary slightly with your implementation; the above is the intended contract.

🔐 Config & profiles

Default (no DB):
web-app/src/main/resources/application.yml

In-memory state for BroadcastHub & UserRegistry

SSE enabled

DB mode:
web-app/src/main/resources/application-db.yml

H2 datasource (file mode), Flyway migrations under /db/migration

Spring Data repositories enabled

Run with:

mvn spring-boot:run -Dspring-boot.run.profiles=db


TLS for Web UI (optional):

You can serve the web app over HTTPS by adding server.ssl.* properties in application.yml and pointing to certs/server.p12. (Not required to demonstrate TLS between custom clients and the TLS socket server in chat-secure.)

🧰 Useful scripts (if included)

scripts/gen-certs.sh — generate keystore/truststore

scripts/run-plain.sh — start blocking TCP

scripts/run-nio.sh — start NIO gateway

scripts/run-tls.sh — start TLS chat

scripts/run-web.sh — start Spring Boot

scripts/loadtest.sh — spawn fake clients

If these are placeholders, adapt commands from the sections above.

🧱 Troubleshooting

Address already in use
Another process is using the port. Change the port or stop the process:

Windows: netstat -ano | findstr :5000, then taskkill /PID <pid> /F

macOS/Linux: lsof -i :5000 → kill -9 <pid>

Web UI loads but no live updates
Ensure /stream (SSE) is reachable; check browser console/network tab.

TLS client fails to connect
Truststore must include the server certificate; verify --truststore path and password, or re-run keytool steps.

H2 not persisting
Make sure you ran with -Dspring-boot.run.profiles=db and the datasource points to a file (not mem:). Confirm ./.data/ exists and is git-ignored.

📜 License

MIT (or your chosen license). Put the text in LICENSE.

👥 Credits / Ownership

A: TCP core (chat-tcp)

B: Multithreading (in tcp/nio servers)

C: NIO gateway (chat-nio)

D: TLS/SSL layer (chat-secure)

E: Web app & bridge + H2 (web-app)
