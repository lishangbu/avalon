package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.TrainingCourse;
import io.github.lishangbu.avalon.content.battle.model.TrainingDefinition;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 学院职业试题参数在启动时统一校验；入口和奖励分别由学院目录与任务目录管理。 */
@Component
public class TrainingCatalog {

	private final TrainingDefinition definition;

	public TrainingCatalog(BattleCatalog battles) {
		var raw = ContentLoader.read("/content/training.json", TrainingDefinition.class);
		if (raw.level() < 1 || raw.level() > 15
				|| !raw.courses().keySet().equals(Set.of("WARRIOR", "MAGE", "PRIEST", "RANGER")))
			throw new IllegalArgumentException("职业训练配置无效");
		raw.courses().forEach((profession, course) -> {
			var skill = battles.skill(course.skill());
			var enemy = course.opponent();
			if (skill == null || !skill.effect().equals(course.goal()) || course.rounds() < 1 || enemy.health() < 1
					|| enemy.attack() < 1 || enemy.defense() < 1 || enemy.speed() < 1 || enemy.power() < 0
					|| enemy.count() < 1 || enemy.count() > 3)
				throw new IllegalArgumentException("职业训练试题无效：" + profession);
		});
		definition = new TrainingDefinition(raw.level(), Map.copyOf(raw.courses()));
	}

	public TrainingDefinition definition() {
		return definition;
	}

	public TrainingCourse course(String profession) {
		return definition.courses().get(profession);
	}

}
