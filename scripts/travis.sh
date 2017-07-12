#!/usr/bin/env bash
set -xe

SBT_COMMANDS=("++${TRAVIS_SCALA_VERSION}")

if [[ "$NATIVE" = 1 ]]; then
  SBT_COMMANDS+=("native/test")
elif [[ "$TRAVIS_SCALA_VERSION" == 2.13.* ]]; then
  SBT_COMMANDS+=("testsJVM/test")
  SBT_COMMANDS+=("testsJS/test")
else
  SBT_COMMANDS+=("validate")
fi

if [[ "$TRAVIS_PULL_REQUEST" == "false" && "$JAVA_HOME" == "$(jdk_switcher home oraclejdk7)" && "$TRAVIS_BRANCH" == "master" ]]; then
  if [[ "$NATIVE" = 1 ]]; then
    SBT_COMMANDS+=("native/publish")
  else
    SBT_COMMANDS+=("publish")
  fi
fi

sbt ${SBT_COMMANDS[@]}
