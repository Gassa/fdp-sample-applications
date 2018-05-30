#!/usr/bin/env bash

set -eux

HERE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"

echo ${HERE}

. ${HERE}/version.sh

${HERE}/bigdl/build.sh

${HERE}/nwintrusion/build.sh

${HERE}/flink/build.sh

${HERE}/kstream/build.sh
