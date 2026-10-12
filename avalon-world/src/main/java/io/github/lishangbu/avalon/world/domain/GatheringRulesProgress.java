package io.github.lishangbu.avalon.world.domain;

import java.time.Instant;

/**
 * GatheringRules 使用的类型化规则数据，包含confirmedMillis、lastSeen、complete等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param lastSeen 上次可见时刻
 * @param complete 是否完成
 */
public record GatheringRulesProgress(long confirmedMillis, Instant lastSeen, boolean complete) {
}
