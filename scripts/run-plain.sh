#!/usr/bin/env bash
set -euo pipefail

# Placeholder script to run the plain TCP chat server.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."

( cd "${PROJECT_ROOT}" && mvn -pl chat-tcp -am exec:java -Dexec.mainClass=com.securechat.tcp.ChatServer -Dexec.args="9000" )
