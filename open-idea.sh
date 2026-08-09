#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IDEA_BIN="${RTCOLONY_IDEA_BIN:-${HOME}/.local/bin/idea}"
IDEA_LOG="${TMPDIR:-/tmp}/rtcolony-idea.log"

export JAVA_HOME="${RTCOLONY_JAVA_HOME:-${HOME}/.local/opt/jdks/minecraft-java-21}"
if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
    echo "RTColony Java 21 was not found at ${JAVA_HOME}." >&2
    exit 1
fi
if [[ ! -x "${IDEA_BIN}" ]]; then
    echo "IntelliJ IDEA was not found at ${IDEA_BIN}." >&2
    echo "Set RTCOLONY_IDEA_BIN to the IntelliJ launcher and try again." >&2
    exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
FLITE_HOME="${RTCOLONY_FLITE_HOME:-${HOME}/.local/opt/flite}"
if [[ -d "${FLITE_HOME}/lib" ]]; then
    export LD_LIBRARY_PATH="${FLITE_HOME}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
fi

nohup "${IDEA_BIN}" "${PROJECT_DIR}" > "${IDEA_LOG}" 2>&1 &
disown
echo "Opening RTColony in IntelliJ. Log: ${IDEA_LOG}"
