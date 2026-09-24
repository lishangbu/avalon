package io.github.lishangbu.avalon.creature.application.model;

/** 战斗模块提供只读资源能力；伙伴模块不反向依赖战斗实体或属性公式。 */
public interface CreatureResources {

	Resources read(long characterId, long creatureId, String species, int level);

	record Resources(int hp, int mp, int health, int mana, int physicalAttack, int magicAttack, int physicalDefense,
			int magicDefense, int speed) {
	}

}
