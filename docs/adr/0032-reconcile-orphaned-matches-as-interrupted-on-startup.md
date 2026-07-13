# Reconcile orphaned Matches as interrupted on startup

Interrupted Match 记录 `START_FAILED`、`RUNTIME_LOST` 与 `RUNTIME_FAILED`：分别表示 Runtime 未能建立、已经不存在，以及仍存在但执行失败或状态无法继续信任，三者都不拥有 Match Result。应用启动时幂等扫描遗留状态，将 `STARTING` 收敛为 `INTERRUPTED / START_FAILED`、将 `ACTIVE` 收敛为 `INTERRUPTED / RUNTIME_LOST`；运行中的引擎异常或自动推进保护触发则使用 `RUNTIME_FAILED`，系统不尝试恢复或续接。
