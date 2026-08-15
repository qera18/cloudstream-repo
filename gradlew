#!/bin/sh
# Gradle wrapper bootstrap script.
# The canonical gradlew, gradlew.bat, and gradle-wrapper.jar are (re)generated
# by the build workflow via 'gradle wrapper --gradle-version 8.11.1'.
set -e

APP_HOME=$( cd "$( dirname "$0" )" > /dev/null && pwd -P ) || exit

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
    "-Dorg.gradle.appname=$( basename "$0" )" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
