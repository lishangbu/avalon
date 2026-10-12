package io.github.lishangbu.avalon.character.application.model;

/** 不可变公开身份供聊天模块使用；角色归属仍由认证身份在服务端解析。 */
public record CharacterPublicPlayer(long id, String name) {
}
