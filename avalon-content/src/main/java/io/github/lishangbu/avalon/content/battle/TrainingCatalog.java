package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.TrainingCourse;
import io.github.lishangbu.avalon.content.battle.model.TrainingDefinition;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 学院职业试题参数在启动时统一校验；入口和奖励分别由学院目录与任务目录管理。 */
@Component
public class TrainingCatalog {

	private final TrainingDefinition definition;

	@Autowired
	public TrainingCatalog(BattleCatalog battles) {
		this(ContentLoader.read("/content/battle/training.json", TrainingDefinition.class), battles);
	}

	/** 显式内容输入与生产资源共用同一校验，离线重放不读取当前配置。 */
	public TrainingCatalog(TrainingDefinition raw, BattleCatalog battles) {
		if (raw.level() < 1 || raw.level() > 15
				|| !raw.courses().keySet().equals(Set.of("WARRIOR", "MAGE", "PRIEST", "RANGER")))
			throw new IllegalArgumentException("职业训练配置无效");
		raw.courses().forEach((profession, course) -> {
			var skill = battles.skill(course.skill());
			var enemy = course.opponent();
			if (skill == null || !skill.primaryKind().equals(course.goal()) || course.rounds() < 1 || enemy.health() < 1
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
