#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
  fi
  echo "Open NoorAlIslamAndroid in Android Studio and press Build > Build APK(s)."
  exit 1
fi
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -Xmx64m -Xms64m -jar "$WRAPPER_JAR" "$@"
