package io.github.lishangbu.avalon.battle.application.model;

/** 球档位独立展示，新增档位不增加固定字段；无库存或不可捕捉时不生成选项。 */
public record BattleViewCaptureOption(String code, String name, int level, double chance) {
}
