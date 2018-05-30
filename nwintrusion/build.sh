#!/usr/bin/env bash

set -eux

HERE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"

cd ${HERE}
bats test/bin/*.bats

echo "nwintrusion: VERSION = $VERSION"
cd ${HERE}/source/core
for i in ingestPackage anomalyDetection batchKMeans
do
  sbt "set version in ThisBuild := \"$VERSION\"" "show version" $i/clean $i/docker
done

# Use this one to verify that the version is set correctly!
# sbt "set version in ThisBuild := \"$VERSION\"" "show version"
