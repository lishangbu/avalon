package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRoll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 仅存在于测试类路径的两个确定性边界。替换成长和抽签策略，不替换数据库、事务或业务服务。 测试串行运行，每条用例前后恢复；真实 HTTP 线程读取同一 volatile 委托。
 */
@TestConfiguration
class TestDoubles {

	private static volatile GrowthRules growth = new GrowthRules();

	private static volatile EnhancementRoll roll = new EnhancementRoll();

	static void reset() {
		growth = new GrowthRules();
		roll = new EnhancementRoll();
	}

	static <T> void install(T value, Class<T> type) {
		if (type == GrowthRules.class)
			growth = (GrowthRules) value;
		else if (type == EnhancementRoll.class)
			roll = (EnhancementRoll) value;
		else
			throw new IllegalArgumentException("未知测试边界：" + type);
	}

	@Bean
	@Primary
	GrowthRules testGrowth() {
		return new GrowthRules() {
			@Override
			public long threshold(int level) {
				return growth.threshold(level);
			}

			@Override
			public int level(long experience) {
				return growth.level(experience);
			}

			@Override
			public long addExperience(long current, int amount) {
				return growth.addExperience(current, amount);
			}

			@Override
			public int carryingCapacity(int level) {
				return growth.carryingCapacity(level);
			}
		};
	}

	@Bean
	@Primary
	EnhancementRoll testRoll() {
		return new EnhancementRoll() {
			@Override
			public int next() {
				return roll.next();
			}
		};
	}

}
