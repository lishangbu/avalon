package io.github.lishangbu.avalon.world.quest.model;

/** 路线选项来自已开放定义，标题来自游戏步骤名，组与选项仅作为命令引用。 */
public record QuestBranchView(String group, String option, String title) {
}
