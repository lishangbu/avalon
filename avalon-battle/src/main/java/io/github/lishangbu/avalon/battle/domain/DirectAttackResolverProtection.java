package io.github.lishangbu.avalon.battle.domain;

/** 保护记录保留原技能是否支持群攻，同一目标的保护按施放顺序消耗。 */
public record DirectAttackResolverProtection(String protector, boolean acceptsArea) {
}
