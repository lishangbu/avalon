#!/usr/bin/env bash
# 日常模拟默认走正式74步和16级；所有状态由隔离服务中的真实HTTP操作取得。
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export AVALON_API_SIMULATION=1
exec mise exec -- ./mvnw test-compile failsafe:integration-test failsafe:verify \
    -Dskip.frontend=true -Dit.test=NaturalSimulationIT \
    -Dfailsafe.failIfNoSpecifiedTests=false "$@"
