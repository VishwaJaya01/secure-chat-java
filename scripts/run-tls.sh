#!/usr/bin/env bash
set -euo pipefail

# Placeholder script to run the TLS-enabled chat server.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
KEYSTORE="${PROJECT_ROOT}/certs/server-keystore.jks"

if [[ ! -f "${KEYSTORE}" ]];
then
  echo "Missing keystore at ${KEYSTORE}." >&2
  exit 1
fi

( cd "${PROJECT_ROOT}" && mvn -pl chat-secure -am exec:java -Dexec.mainClass=com.securechat.secure.SecureChatServer -Dexec.args="9443" )
