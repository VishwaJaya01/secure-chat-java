# SecureCollab - Upscaled Project

SecureCollab is a Java 17, Maven-driven workspace with Spring Boot backend and React frontend that demonstrates multiple network concepts through distinct features. Each team member owns a distinct web-app feature showcasing one core network concept.

---

## Project Overview

This project demonstrates:
- **Chat**: Live team conversation via API + SSE
- **Announcements**: Admin → everyone, real-time (NIO)
- **Tasks/Board**: CRUD + live updates, background workers (Multithreading)
- **File Transfer**: Secure point-to-point, non-HTTP (TCP)
- **Presence**: Online/idle/offline via UDP
- **Link Previews**: Server-side URL/URI fetch/unfurl
- **Security**: TLS for sockets + HTTPS for API

---

## Module Overview

| Module | Role | Owner | Network Concept |
|--------|------|-------|-----------------|
| `chat-core` | Shared models & registries | All | Domain primitives |
| `web-api` | Spring Boot REST API + SSE | All | HTTP/SSE endpoints |
| `announcement-nio` | NIO announcement gateway | Member C | NIO (Selector) |
| `chat-tcp` | TCP file transfer | Member A | TCP sockets |
| `chat-secure` | TLS/SSL security layer | Member D | TLS/SSL |
| `udp-presence` | UDP presence service | Member E | UDP |
| `web-app` | React frontend | All | React + Vite |

---

## Project Layout

```
secure-chat-java/
├─ chat-core/            # Domain primitives and registries
├─ web-api/              # Spring Boot REST API (main backend)
├─ announcement-nio/     # NIO announcement gateway (port 6001)
├─ chat-tcp/             # TCP file transfer (port 6000)
├─ chat-secure/          # TLS-enabled components
├─ udp-presence/         # UDP presence beacons (port 9090)
├─ web-app/              # React frontend (Vite + TypeScript)
├─ db/                   # Flyway SQL migrations (H2)
├─ scripts/              # Helper launch scripts
└─ pom.xml               # Maven parent (aggregator)
```

---

## Prerequisites

- Java 17 or newer (set `JAVA_HOME` accordingly)
- Maven 3.9+ (or use the bundled `./mvnw` wrapper)
- Node.js 18+ and npm
- `keytool` for generating local TLS assets (ships with the JDK)

---

## Quick Start

### 1. Build All Modules

```bash
./mvnw clean install
```

### 2. Start Backend

```bash
cd web-api
../mvnw spring-boot:run
```

The API will start on `http://localhost:8080`

### 3. Start Frontend

```bash
cd web-app
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173`

---

## Features & Endpoints

### Chat
- `POST /api/send` - Send a message
- `GET /api/stream?u=<username>` - SSE stream of messages

### Announcements (Member C - NIO)
- `POST /api/announcements` - Create announcement (admin only)
- `GET /api/announcements` - Get all announcements
- `GET /api/announcements/{id}` - Get announcement by ID
- `GET /api/announcements/stream` - SSE stream of announcements
- **NIO Gateway**: Port 6001 (non-blocking selector)

### Tasks (Member B - Multithreading)
- `GET /api/tasks` - Get all tasks
- `POST /api/tasks` - Create a task
- `PUT /api/tasks/{id}` - Update a task
- `DELETE /api/tasks/{id}` - Delete a task
- `GET /api/tasks/stream` - SSE stream of tasks
- **Background Workers**: ExecutorService (to be implemented)

### Files (Member A - TCP)
- `GET /api/files` - Get all files
- `GET /api/files/{id}` - Get file metadata
- `POST /api/files/meta` - Create file metadata
- **TCP Server**: Port 6000 (file transfer)

### Presence (Member E - UDP)
- `GET /api/users` - Get all users
- `GET /api/users/status` - Get user status map
- `POST /api/presence/beat` - Update user presence
- **UDP Server**: Port 9090 (presence beacons)

### Link Previews (Member E - URL/URI)
- `POST /api/link/preview?url=<url>` - Get link preview

---

## Port Configuration

| Service | Port | Notes |
|---------|------|-------|
| React dev server | 5173 | Vite + React frontend |
| Spring Boot API | 8080 | REST + SSE endpoints |
| NIO Gateway | 6001 | Announcement broadcasting |
| TCP File Server | 6000 | File transfer (Member A) |
| UDP Presence | 9090 | Presence beacons (Member E) |
| TLS File Server | 6443 | Secure file transfer (Member D) |
| HTTPS API | 8443 | Secure API (Member D) |

---

## Database

The project uses H2 database with Flyway migrations. The database file is stored at `./data/securecollab.mv.db`.

### Database Console

Access the H2 console at `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:file:./data/securecollab`
- Username: `sa`
- Password: (empty)

### Profiles

- **Default**: In-memory (no database)
- **db**: H2 database with Flyway migrations

To use the database profile:
```bash
cd web-api
../mvnw spring-boot:run -Dspring-boot.run.profiles=db
```

---

## Member Responsibilities

### Member A — File Transfer Service (TCP)
**Network Concept**: TCP sockets (java.net.ServerSocket/Socket)

**Deliverables**:
- File server on TCP port 6000
- File client for upload/download
- ChunkFramer (fixed-size or length-prefixed)
- REST metadata endpoints: `POST /api/files/meta`, `GET /api/files/:id`
- Corruption-free transfer (checksum), resumable or chunked

**Status**: Stubs provided, implementation needed

### Member B — Task Board & Workers (Multithreading)
**Network Concept**: Multithreading (ExecutorService, synchronization)

**Deliverables**:
- Task service with CRUD operations
- Worker pool (ExecutorService) + thread-safe queues
- Rate-limit task updates
- SSE stream for real-time updates
- Background workers for task events

**Status**: Basic CRUD implemented, workers needed

### Member C — Announcements Broadcast (NIO) ✅
**Network Concept**: NIO (non-blocking I/O) with Selector

**Deliverables**:
- ✅ AnnouncementGateway (ServerSocketChannel + Selector)
- ✅ Bridge to API: gateway → BroadcastHub → SSE to React
- ✅ REST endpoints: `POST /api/announcements`, `GET /api/announcements`
- ✅ SSE stream: `GET /api/announcements/stream`
- ✅ Single-threaded selector handles many clients

**Status**: Complete

### Member D — Security Layer (TLS/SSL)
**Network Concept**: Secure network programming (TLS/SSL + HTTPS)

**Deliverables**:
- TLS file server on port 6443
- TLS announcement gateway (optional)
- HTTPS API on port 8443
- Keystores/truststores management
- Cipher/handshake policy

**Status**: Stubs provided, implementation needed

### Member E — Presence + Link Previews (UDP & URL/URI)
**Network Concept**: UDP + URL/URI / HttpURLConnection

**Deliverables**:
- ✅ Link preview service (implemented)
- PresenceServer (DatagramSocket on port 9090)
- PresenceService with lastSeen thresholds
- UDP beacons (ping:<user>)
- React presence sidebar

**Status**: Link previews complete, UDP presence needed

---

## NIO Announcements Gateway (Member C)

The NIO gateway demonstrates non-blocking I/O using Java NIO Selector.

### How It Works

1. Gateway starts on port 6001
2. Uses a single-threaded selector to handle multiple clients
3. When an announcement is created via REST API:
   - AnnouncementService saves to database
   - Broadcasts to AnnouncementBroadcastHub
   - Hub notifies all registered consumers (including NIO gateway)
   - Gateway broadcasts to all connected NIO clients
4. React frontend receives updates via SSE stream

### Testing

1. Start the application
2. Login as "admin" user
3. Go to Announcements page
4. Create an announcement
5. Check backend logs for selector events (READ/WRITE/ACCEPT)
6. All connected clients receive the broadcast

### Logs

The gateway logs selector events for debugging:
```
INFO  - NIO AnnouncementGateway started on port 6001
INFO  - Client connected: /127.0.0.1:54321 (total: 1)
INFO  - Broadcasted announcement to 1 clients
```

---

## Frontend Structure

The React app uses routing with the following pages:
- **Chat**: Real-time chat interface
- **Announcements**: Admin announcements with NIO gateway integration
- **Tasks**: Task board with drag-and-drop (to be enhanced)
- **Files**: File metadata and download (TCP transfer to be implemented)

### Components

- `Layout`: Main layout with navigation and presence sidebar
- `ChatPage`: Chat interface with SSE stream
- `AnnouncementsPage`: Announcements with admin form
- `TasksPage`: Task board with status columns
- `FilesPage`: File list and metadata
- `PresencePanel`: User presence sidebar

---

## Configuration

### Application Properties (`web-api/src/main/resources/application.properties`)

```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:file:./data/securecollab;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=

# NIO Gateway
nio.gateway.port=6001
nio.gateway.enabled=true

# UDP Presence
udp.presence.port=9090
udp.presence.enabled=true

# File Transfer
file.transfer.tcp.port=6000
file.transfer.enabled=true
```

---

## Troubleshooting

- **Port already in use**: Use `lsof -i :<port>` (macOS/Linux) or `netstat -ano | findstr :<port>` (Windows)
- **SSE stream appears idle**: Verify the endpoint is reachable and messages are being sent
- **Database errors**: Check that H2 database file has write permissions
- **NIO gateway not starting**: Check port 6001 is available and logs for errors
- **Admin user not found**: The admin user is created automatically in the database migration

---

## Implementation Status

### ✅ Member C - NIO Announcements (COMPLETE)

**What's Implemented**:
- ✅ NIO Gateway with Selector on port 6001
- ✅ Single-threaded selector handles multiple clients
- ✅ AnnouncementBroadcastHub for fan-out
- ✅ REST endpoints: POST/GET /api/announcements
- ✅ SSE stream: GET /api/announcements/stream
- ✅ Admin validation and database persistence
- ✅ React frontend integration

**Flow**: Admin creates announcement → Service → BroadcastHub → NIO Gateway (port 6001) + SSE (React)

### ✅ Other Services (INITIATED - Ready for Implementation)

**Member A - TCP File Transfer**:
- ✅ FileService, FileController, FileEntity created
- ✅ REST endpoints: POST /api/files/meta, GET /api/files
- ❌ Needs: TCP server on port 6000, file upload/download, ChunkFramer

**Member B - Task Board & Workers**:
- ✅ TaskService, TaskController, TaskEntity created
- ✅ CRUD operations and SSE stream
- ❌ Needs: ExecutorService worker pool, thread-safe queues, background workers

**Member D - Security Layer (TLS/SSL)**:
- ✅ chat-secure module structure
- ❌ Needs: TLS file server (port 6443), HTTPS API (port 8443), keystore management

**Member E - Presence & Link Previews**:
- ✅ LinkPreviewService (fully implemented)
- ✅ PresenceService with status calculation
- ✅ REST endpoints ready
- ❌ Needs: UDP server on port 9090, UDP beacons

---

## Development Notes

- The NIO gateway is fully implemented and integrated (Member C)
- Other services (TCP file transfer, UDP presence, TLS, multithreading workers) have stubs and endpoints ready
- All SSE streams are implemented for real-time updates
- Database migrations are set up for all entities
- React frontend is complete with routing and all pages

---

## License & Attribution

- Suggested license: MIT
- This project demonstrates various network programming concepts for educational purposes
