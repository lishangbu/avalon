package io.github.lishangbu.avalon.world.domain;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** 每次成功操作后抽取一次；抽取结果由外层生产回执持久化。 */
@Component
public class LifeToolRandom {

	public boolean breaks(int denominator) {
		return ThreadLocalRandom.current().nextInt(denominator) == 0;
	}

}
