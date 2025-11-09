#!/usr/bin/env bash
set -euo pipefail

# Launch the React development server for the chat UI.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
APP_DIR="${PROJECT_ROOT}/web-app"

if [ ! -d "${APP_DIR}" ]; then
  echo "React app not found at ${APP_DIR}" >&2
  exit 1
fi

cd "${APP_DIR}"

if [ ! -d "node_modules" ]; then
  echo "Installing dependencies..."
  npm install
fi

echo "Starting Vite dev server on http://localhost:5173"
npm run dev
