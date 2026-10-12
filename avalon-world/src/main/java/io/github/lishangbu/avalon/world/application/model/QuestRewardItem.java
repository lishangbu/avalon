package io.github.lishangbu.avalon.world.application.model;

/** 可见任务的物品奖励预览，数量只供展示，不授予领取资格。 */
public record QuestRewardItem(String code, String name, int quantity) {
}
