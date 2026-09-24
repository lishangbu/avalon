package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 学院借用战斗课程目录，只定义正式开篇中实际可执行的课程。 */
@Component
public class AcademyTrainingCatalog {

	private final Definition definition;

	public AcademyTrainingCatalog(WorldCatalog world, BattleCatalog battles, ItemCatalog items) {
		definition = ContentLoader.read("/content/academy-training.json", Definition.class);
		world.place(definition.location());
		definition.capture().supplies().forEach((code, count) -> {
			var item = items.item(code);
			if (count < 1 || item.type() != ItemCatalog.ConsumableType.CAPTURE_BALL
					&& !(item.type() == ItemCatalog.ConsumableType.POTION && item.scope() == ItemCatalog.Scope.ACADEMY))
				throw new IllegalArgumentException("学院补给类型无效：" + code);
		});
		if (!definition.courses().keySet().equals(Set.of("BQ018", "BQ019", "BQ020", "BQ023"))
				|| definition.partners().size() != 5
				|| definition.partners().stream().map(Partner::id).distinct().count() != 5)
			throw new IllegalArgumentException("学院战斗课程或借用伙伴目录不完整");
		for (var pet : definition.partners()) {
			battles.growth(pet.species());
			for (var skill : pet.skills())
				if (battles.skill(skill) == null)
					throw new IllegalArgumentException("借用伙伴技能不存在：" + skill);
		}
		Map.of("BQ018", "DEFENSE", "BQ019", "SWAP", "BQ020", "CAPTURE", "BQ023", "PROFESSION").forEach((id, goal) -> {
			if (!definition.courses().get(id).goal().equals(goal))
				throw new IllegalArgumentException("学院课程目标不匹配：" + id);
		});
	}

	public Definition definition() {
		return definition;
	}

	public Course course(String id) {
		return definition.courses().get(id);
	}

	public record Definition(String location, int level, List<Partner> partners, Map<String, Course> courses,
			Capture capture) {
		public Definition {
			partners = List.copyOf(partners);
			courses = Map.copyOf(courses);
		}
	}

	/** 捕捉课程使用固定等级投影和独立药品库存；回复值不影响正式商店物品。 */
	public record Capture(int level, String species, Map<String, List<Integer>> stats, Map<String, Integer> supplies) {
		public Capture {
			stats = stats.entrySet()
				.stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
			supplies = Map.copyOf(supplies);
		}
	}

	/** 固定借用数据不读取真实装备和已学技能，不在真实幻兽表创建实体。 */
	public record Partner(String id, String species, String name, String element, List<Integer> stats,
			List<String> skills) {
		public Partner {
			stats = List.copyOf(stats);
			skills = List.copyOf(skills);
		}
	}

	public record Course(String name, String requires, String goal, int rounds, int enemyHealth, int enemyAttack,
			int enemyDefense, int enemySpeed, int power, String objective) {
	}

}
