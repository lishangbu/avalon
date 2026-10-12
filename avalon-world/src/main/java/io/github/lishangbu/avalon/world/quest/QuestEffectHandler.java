package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestGrant;
import java.util.List;

/**
 * 游戏效果扩展。Spring收集所有实现，注册类型必须唯一；validate负责上线前引用及参数校验， apply只能在持有角色锁的宿主事务中调用，返回玩家可读的结算说明。
 */
public interface QuestEffectHandler {

	String kind();

	void validate(QuestEffect effect);

	List<String> apply(long owner, QuestGrant grant);

}
