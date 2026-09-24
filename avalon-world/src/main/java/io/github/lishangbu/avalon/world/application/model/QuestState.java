package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * 任务进度读模型。available 是已满足步骤前置的交互，不表示玩家当前已在该地点； completed
 * 是整项任务编号，由同一批凭证派生。没有可用交互不等于所有游戏内容已完成。 背包和伙伴使用独立领域查询，不重新聚合进此响应；命令资格仍须服务端重新验证。
 */
public record QuestState(List<QuestAction> available, List<String> completed, List<QuestJournalEntry> journal) {
}
