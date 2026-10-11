package io.github.lishangbu.avalon.character.application.model;

/** 战斗门禁实际变更后的同步事件；只携带角色编号，用于提交后唤醒同账号标签页重新读取权限。 */
public record CharacterBattleChanged(long characterId) {
}
