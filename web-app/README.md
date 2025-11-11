# SecureCollab Web UI

This is the React + Vite front-end for SecureCollab. It provides a modern UI for chat, announcements, tasks, and file management.

## Getting Started

```bash
npm install          # once
npm run dev          # starts Vite on http://localhost:5173
npm run build        # type-check + production build
npm run preview      # serve the build output locally
```

The root `scripts/run-web.sh` helper wraps `npm run dev` and installs dependencies if they are missing.

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_CHAT_API_URL` | `/api` | Base path for API endpoints. Defaults to `/api` which proxies to `http://localhost:8080` |

When `VITE_CHAT_API_URL` starts with `/api`, the Vite dev server proxies to `http://localhost:8080` (configurable in `vite.config.ts`).

## Features

- **Chat**: Real-time chat with SSE stream
- **Announcements**: Admin announcements with real-time updates
- **Tasks**: Task board with status columns
- **Files**: File metadata and management
- **Presence**: User presence sidebar with online/idle/offline status

## Pages

- `/chat` - Chat interface
- `/announcements` - Announcements (admin can create)
- `/tasks` - Task board
- `/files` - File list

## API Endpoints

The UI communicates with the Spring Boot backend at `http://localhost:8080`:

- `POST /api/send` - Send chat message
- `GET /api/stream` - SSE stream for chat messages
- `POST /api/announcements` - Create announcement (admin only)
- `GET /api/announcements` - Get all announcements
- `GET /api/announcements/stream` - SSE stream for announcements
- `GET /api/tasks` - Get all tasks
- `POST /api/tasks` - Create task
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `GET /api/tasks/stream` - SSE stream for tasks
- `GET /api/files` - Get all files
- `GET /api/users` - Get all users
- `GET /api/users/status` - Get user status
- `POST /api/presence/beat` - Update user presence
- `POST /api/link/preview` - Get link preview

## Design Tokens

CSS tokens live in `src/styles/tokens.css`. Use the Figma MCP server to sync updated variables from design.

## Project Structure

```
web-app/
├─ src/
│  ├─ components/     # Reusable components
│  ├─ pages/          # Page components
│  ├─ services/       # API service layer
│  ├─ styles/         # CSS styles
│  └─ types.ts        # TypeScript types
```
