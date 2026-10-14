package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.AcademyTrainingCourseConfig;
import io.github.lishangbu.avalon.content.battle.model.AcademyTrainingDefinition;
import io.github.lishangbu.avalon.content.battle.model.AcademyTrainingPartnerConfig;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 学院借用战斗课程目录，只定义正式开篇中实际可执行的课程。 */
@Component
public class AcademyTrainingCatalog {

	private final AcademyTrainingDefinition definition;

	@Autowired
	public AcademyTrainingCatalog(WorldCatalog world, BattleCatalog battles, ItemCatalog items) {
		this(ContentLoader.read("/content/academy-training.json", AcademyTrainingDefinition.class), world, battles,
				items);
	}

	/** 显式内容输入与生产资源共用同一校验，离线重放不读取当前配置。 */
	public AcademyTrainingCatalog(AcademyTrainingDefinition raw, WorldCatalog world, BattleCatalog battles,
			ItemCatalog items) {
		definition = raw;
		world.place(definition.location());
		definition.capture().supplies().forEach((code, count) -> {
			var item = items.item(code);
			if (count < 1 || item.type() != ItemConsumableType.CAPTURE_BALL
					&& !(item.type() == ItemConsumableType.POTION && item.scope() == ItemScope.ACADEMY))
				throw new IllegalArgumentException("学院补给类型无效：" + code);
		});
		if (!definition.courses().keySet().equals(Set.of("BQ018", "BQ019", "BQ020", "BQ023"))
				|| definition.partners().size() != 5
				|| definition.partners().stream().map(AcademyTrainingPartnerConfig::id).distinct().count() != 5)
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

	public AcademyTrainingDefinition definition() {
		return definition;
	}

	public AcademyTrainingCourseConfig course(String id) {
		return definition.courses().get(id);
	}

}
