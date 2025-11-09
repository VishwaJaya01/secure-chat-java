# SecureChat Web UI

This is the React + Vite front-end that replaces the old Spring/Thymeleaf
module. It talks to the Java services through the legacy `/send` (POST) and
`/stream` (SSE) endpoints.

## Getting Started

```bash
npm install          # once
npm run dev          # starts Vite on http://localhost:5173
npm run build        # type-check + production build
npm run preview      # serve the build output locally
```

The root `scripts/run-web.sh` helper wraps `npm run dev` and installs
dependencies if they are missing, which is handy for teammates that only need
the UI.

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_CHAT_API_URL` | `/api` | Base path used for `/send` & `/stream`. Point it to whichever Java gateway exposes those endpoints. |
| `VITE_CHAT_MAX_MESSAGES` | `200` | Client-side cap before older messages are trimmed. |

When `VITE_CHAT_API_URL` starts with `/api`, the Vite dev server proxies to
`http://localhost:8080` (configurable in `vite.config.ts`).

## Design Tokens (Figma MCP)

CSS tokens live in `src/styles/tokens.css`. Pull the latest variables from the
`Figma MCP` server configured in your `mcp.json` and paste them here whenever
the design team updates the palette or spacing scale.

## API Contract

The UI expects an HTTP server that implements:

- `POST /send` — accepts `application/x-www-form-urlencoded` with `username`
  and `text`, returns `204 No Content`.
- `GET  /stream?u=<username>` — Server-Sent Events feed that emits serialized
  `WebMessage` objects (same shape as the old Spring controller). The server can
  replay history by emitting events immediately after the connection opens.
