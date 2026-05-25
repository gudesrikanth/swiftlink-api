#!/bin/sh
# Maven Wrapper Script
# Licensed to the Apache Software Foundation (ASF)

set -e

if [ -z "$MAVEN_HOME" ]; then
  MAVEN_HOME=""
fi

BASEDIR=$(dirname "$(readlink -f "$0" 2>/dev/null || realpath "$0" 2>/dev/null || echo "$0")")

WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASEDIR/.mvn/wrapper/maven-wrapper.properties"

DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ -r "$WRAPPER_PROPERTIES" ]; then
  DISTRIBUTION_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Downloading Maven Wrapper..."
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  if command -v wget > /dev/null; then
    wget -q -O "$WRAPPER_JAR" "$DOWNLOAD_URL"
  elif command -v curl > /dev/null; then
    curl -sL -o "$WRAPPER_JAR" "$DOWNLOAD_URL"
  fi
fi

exec java \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$BASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
