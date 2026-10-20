package io.github.lishangbu.avalon.character.application.model;

/** 仅供服务器对战宿主使用；账号归属不得进入公开对战响应。 */
public record CharacterBattleParticipant(long owner, long character, String name, String location) {
}
