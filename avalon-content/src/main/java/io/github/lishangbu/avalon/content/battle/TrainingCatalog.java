package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

/** 学院职业试题参数在启动时统一校验；入口和奖励分别由学院目录与任务目录管理。 */
@Component
public class TrainingCatalog {

	private final Definition definition;

	public TrainingCatalog(BattleCatalog battles) {
		var raw = ContentLoader.read("/content/training.json", Definition.class);
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
		definition = new Definition(raw.level(), Map.copyOf(raw.courses()));
	}

	public Definition definition() {
		return definition;
	}

	public Course course(String profession) {
		return definition.courses().get(profession);
	}

	public record Definition(int level, Map<String, Course> courses) {
	}

	public record Course(String name, String skill, String goal, int rounds, Opponent opponent, String objective) {
	}

	public record Opponent(String name, int health, int attack, int defense, int speed, int power, int count) {
	}

}
