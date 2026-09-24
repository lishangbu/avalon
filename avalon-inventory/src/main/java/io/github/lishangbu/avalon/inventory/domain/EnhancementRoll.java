package io.github.lishangbu.avalon.inventory.domain;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

/** 每个真实强化命令只调用一次；单独的随机边界便于测试成功和失败事务而不依赖碰运气。 */
@Component
public class EnhancementRoll {

	private final SecureRandom random = new SecureRandom();

	public int next() {
		return random.nextInt(100);
	}

}
