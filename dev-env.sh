#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${RTCOLONY_JAVA_HOME:-${HOME}/.local/opt/jdks/minecraft-java-21}"
if [[ ! -x "${JAVA_HOME}/bin/java" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
    echo "RTColony Java 21 was not found at ${JAVA_HOME}." >&2
    echo "Set RTCOLONY_JAVA_HOME to a Java 21 JDK directory and try again." >&2
    return 1 2>/dev/null || exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
FLITE_HOME="${RTCOLONY_FLITE_HOME:-${HOME}/.local/opt/flite}"
if [[ -d "${FLITE_HOME}/lib" ]]; then
    export LD_LIBRARY_PATH="${FLITE_HOME}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
fi

echo "JAVA_HOME=$JAVA_HOME"
echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH:-<unchanged>}"
java -version
javac -version
