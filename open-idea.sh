#!/usr/bin/env bash
cd "$(dirname "$0")"

export JAVA_HOME="/home/eru/.local/opt/jdks/minecraft-java-21"
export PATH="$JAVA_HOME/bin:$PATH"

nohup flatpak run com.jetbrains.IntelliJ-IDEA-Community "$PWD" > /tmp/rtcolony-idea.log 2>&1 &
disown
echo "Opening RTColony in IntelliJ. Log: /tmp/rtcolony-idea.log"
