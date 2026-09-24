package io.github.lishangbu.avalon.world.domain;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** 资源满足且容量已检查后才抽取，稀有机会随技能提升并封顶。 */
@Component
public class GatheringRareRoll {

	public static int chance(int skillLevel, int resourceLevel) {
		if (skillLevel < resourceLevel || skillLevel > 10 || resourceLevel < 5)
			throw new IllegalArgumentException("稀有采集等级无效");
		return Math.min(8, 5 + skillLevel - resourceLevel);
	}

	public boolean succeeds(int skillLevel, int resourceLevel) {
		return ThreadLocalRandom.current().nextInt(100) < chance(skillLevel, resourceLevel);
	}

}
