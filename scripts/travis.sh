#!/usr/bin/env bash
set -xe

if [[ "$NATIVE" = 1 ]]; then
  SBT_COMMANDS="native/test"
elif [[ "$TRAVIS_SCALA_VERSION" == 2.13.* ]]; then
  SBT_COMMANDS="testsJVM/test testsJS/test"
else
  SBT_COMMANDS="test tut"
fi

sbt "++$TRAVIS_SCALA_VERSION" $SBT_COMMANDS
