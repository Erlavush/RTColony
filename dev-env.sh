#!/usr/bin/env bash
export JAVA_HOME="/home/eru/.local/opt/jdks/minecraft-java-21"
export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
java -version
javac -version

