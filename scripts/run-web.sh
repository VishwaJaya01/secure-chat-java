#!/usr/bin/env bash
set -euo pipefail

# Placeholder script to launch the Spring Boot web application.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."

( cd "${PROJECT_ROOT}/web-app" && mvn spring-boot:run )
