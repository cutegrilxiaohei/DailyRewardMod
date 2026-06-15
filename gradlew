#!/bin/sh

#
# Gradle Wrapper
#

set -e

# 定位脚本目录
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

# 使用系统 Java 命令（要求 Java 25+）
JAVA_CMD=java

# 执行 Gradle 任务
exec "$JAVA_CMD" -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
