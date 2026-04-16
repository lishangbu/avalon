package io.github.lishangbu.avalon.battle.application

/**
 * Battle 上下文入口。
 *
 * 当前仍处于骨架阶段，后续负责承载对战会话、对战规则运行时装配和对战结果结算等能力。
 * 这里不直接拥有 Catalog 或 Player 的持久化模型，而应通过快照或 ACL 协作读取所需数据。
 */
object BattleModule