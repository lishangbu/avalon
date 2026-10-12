package io.github.lishangbu.avalon.world.application.model;

/** 放弃命令携带客户端看到的参与修订，服务端在角色锁内重新校验。 */
public record QuestAbandonRequest(Long expectedRevision) {
}
