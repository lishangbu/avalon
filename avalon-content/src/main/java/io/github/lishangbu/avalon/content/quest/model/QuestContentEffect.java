package io.github.lishangbu.avalon.content.quest.model;

/** 校验奖励引用与步骤有向无环关系，缺少配置时拒绝开服，禁止静默忽略未知效果。 */

public record QuestContentEffect(QuestContentEffectKind kind, String reference, int amount) {
}
