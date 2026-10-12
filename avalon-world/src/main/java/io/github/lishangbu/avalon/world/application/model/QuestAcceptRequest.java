package io.github.lishangbu.avalon.world.application.model;

/** 浏览器仅提交请求号和看见的位置修订，不提交角色、奖励或任务实例归属。 */
public record QuestAcceptRequest(String requestId, Long expectedRevision) {
}
