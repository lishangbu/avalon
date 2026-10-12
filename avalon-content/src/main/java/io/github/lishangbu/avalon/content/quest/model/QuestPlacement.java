package io.github.lishangbu.avalon.content.quest.model;

/**
 * 声明式持有条件；来源前缀对应捕获事件，不会把幻兽作为任务交付物扣除。
 */
public record QuestPlacement(String sourcePrefix, int position) {
}
