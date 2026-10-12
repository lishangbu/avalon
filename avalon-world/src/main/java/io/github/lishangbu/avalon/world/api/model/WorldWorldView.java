package io.github.lishangbu.avalon.world.api.model;

import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;

/**
 * 世界首页响应，组合角色读模型与不可变地图内容。 character 为 null 表示尚未创建角色，不能解释为请求失败或默认生成角色。
 */
public record WorldWorldView(CharacterView character, WorldDefinition content) {
}
