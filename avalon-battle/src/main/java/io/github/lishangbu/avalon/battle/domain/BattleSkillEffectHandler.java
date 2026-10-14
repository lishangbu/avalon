package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.CombatEffectHandler;
import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;

/** 游戏效果扩展端口：注册为组件即可加入目录；类型代码唯一，校验和执行均须由处理器提供。 */
public interface BattleSkillEffectHandler extends CombatEffectHandler<BattleSkillEffect, BattleEffectContext> {

}
