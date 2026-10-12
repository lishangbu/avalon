package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonRewardCatalog;
import io.github.lishangbu.avalon.content.battle.model.DungeonStage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 无持久化副作用的奖励计算；经验逐个敌人取整，固定通关经验不乘等级修正。 */
@Component
@RequiredArgsConstructor
public class DungeonRewardRules {

	private final DungeonCatalog dungeons;

	private final DungeonRewardCatalog rewards;

	public int experience(DungeonStage stage, int level, boolean complete) {
		int count = Math.max(1, stage.wildSpecies().size());
		int percent = Math.clamp(100 + 2 * (dungeons.definition().level() - level), 50, 150);
		return stage.experience() * percent / 100 * count
				+ (complete ? rewards.definition().completionExperience() : 0);
	}

	public LocalDate day(Instant instant) {
		return instant.atZone(ZoneId.of(dungeons.definition().zone()))
			.minusHours(dungeons.definition().resetHour())
			.toLocalDate();
	}

	public String supply(RandomGenerator random) {
		int roll = random.nextInt(100);
		for (var drop : rewards.definition().supplyPool()) {
			roll -= drop.weight();
			if (roll < 0)
				return drop.code();
		}
		throw new IllegalStateException("副本掉落权重目录损坏");
	}

}
