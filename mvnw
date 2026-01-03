#!/bin/sh
# Maven Wrapper script

set -e

# Determine the directory containing this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_WRAPPER_JAR="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"

# Download wrapper jar if not present
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    WRAPPER_URL=$(grep 'wrapperUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2)
    curl -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
fi

# Execute Maven
exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
