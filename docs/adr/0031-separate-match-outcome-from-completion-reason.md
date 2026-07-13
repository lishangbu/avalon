# Separate Match outcome from completion reason

Completed Match 的 Match Result 分别保存 `outcome`、`reason`、可选 `winnerTrainerId` 与可选 `battleReason`：允许 `WIN + BATTLE`、`DRAW + BATTLE`、`WIN + FORFEIT`、`WIN + TIMEOUT` 与 `NO_CONTEST + TIMEOUT`，且只有 `WIN` 必须指定胜者，只有 `reason = BATTLE` 必须保存 Battle Session 的稳定原因。引擎 `winningSideId` 严格映射为对应 Trainer，为空映射为 `DRAW`，未知 side 则以 `RUNTIME_FAILED` 中断；Interrupted Match 不保存 Match Result。
