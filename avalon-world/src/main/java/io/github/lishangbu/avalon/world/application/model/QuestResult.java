package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * 单个步骤首次执行成功的回执模型。step标识已执行步骤，dialogue和rewards用于玩家阅读。
 * 奖励文本不是资源账本，真实物品/经验已经在同一事务中保存；客户端不能根据文本领取第二次。 该结构用于玩法结果展示，重复请求由实例命令回执重放。
 */
public record QuestResult(String step, String dialogue, List<String> rewards) {
}
