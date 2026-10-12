package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.quest.model.QuestType;

/** 任务追踪选择及期望修订；角色归属从登录账号解析。 */
public record QuestTrackingRequest(QuestType type, String quest, Long expectedRevision) {
}
