package io.github.lishangbu.avalon.game.battle.engine.mutation

/**
 * 动作执行后产生的结构化战斗变更。
 *
 * 设计意图：
 * - 将“动作解释结果”与“真正写回状态”解耦。
 * - 为后续日志、回放、状态提交提供统一中间层。
 */
interface BattleMutation
