#!/usr/bin/env bash
cd "$(dirname "$0")"
nohup flatpak run com.jetbrains.IntelliJ-IDEA-Community "$PWD" > /tmp/rtcolony-idea.log 2>&1 &
disown
echo "Opening RTColony in IntelliJ. Log: /tmp/rtcolony-idea.log"
