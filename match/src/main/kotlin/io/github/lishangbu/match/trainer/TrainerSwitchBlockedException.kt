package io.github.lishangbu.match.trainer

/** Active Match 要求恢复原 Trainer，禁止切换到其他 Trainer。 */
class TrainerSwitchBlockedException : IllegalStateException("Active match trainer must be restored")
