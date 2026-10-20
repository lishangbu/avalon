package io.github.lishangbu.avalon.world.domain;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** 普通采集产出判定；先检查容量再抽取，空产出仍结算周期成本，不能通过重放心跳重抽。 */
@Component
public class GatheringYieldRoll {

	/** 同级资源基础成功率80%，熟练度优势每级增加2%，最高95%。 */
	public static int chance(int skillLevel, int resourceLevel) {
		if (resourceLevel < 1 || resourceLevel > skillLevel || skillLevel > 10)
			throw new IllegalArgumentException("采集产出等级无效");
		return Math.min(95, 80 + (skillLevel - resourceLevel) * 2);
	}

	public boolean succeeds(int skillLevel, int resourceLevel) {
		return ThreadLocalRandom.current().nextInt(100) < chance(skillLevel, resourceLevel);
	}

}
