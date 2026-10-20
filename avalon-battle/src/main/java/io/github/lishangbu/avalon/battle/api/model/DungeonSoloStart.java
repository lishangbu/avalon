package io.github.lishangbu.avalon.battle.api.model;

/** 单人开本仅接收幂等请求号，成员由认证身份确定。 */
public record DungeonSoloStart(String requestId) {
}
