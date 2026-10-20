package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 游戏内容到任务核心的唯一映射。仅接受显式目标、条件、效果和生命周期配置。 跨任务依赖转换为宿主事实条件，同任务依赖保留在核心图中。构造时核验全部配置及注册扩展。
 */
@Component
public class QuestEngineCatalog {

	private final QuestCatalog catalog;

	private final QuestRulesCatalog rules;

	private final QuestExtensions extensions;

	/** 当前持有目标的引用同物品目录校验，不等到玩家接取后才发现拼写错误。 */
	private final ItemCatalog items;

	/** 展示读取当前拥有的正式物种，不以历史捕捉记录或图鉴解锁代替持有。 */
	private final CreatureCatalog creatures;

	public QuestEngineCatalog(QuestCatalog catalog, QuestRulesCatalog rules, QuestExtensions extensions,
			ItemCatalog items, CreatureCatalog creatures) {
		this.catalog = catalog;
		this.rules = rules;
		this.extensions = extensions;
		this.items = items;
		this.creatures = creatures;
		rules.definition().quests().forEach(rule -> definition(rule.id()));
	}

	/** 每次由当前部署的不可变内容构建定义，不读取旧版本或保存执行用快照。 */
	public QuestDefinition definition(String quest) {
		var rule = rules.rule(quest);
		var source = catalog.definition().steps().stream().filter(step -> step.quest().equals(quest)).toList();
		var ids = source.stream().map(QuestContentStep::id).toList();
		var steps = source.stream().map(step -> step(step, ids)).toList();
		var policy = rule.policy();
		var definition = new QuestDefinition(quest, rule.type() == QuestType.MAIN, rule.abandonable(),
				QuestRepeat.valueOf(rule.policy().repeatPolicy().name()), policy.timezone(), policy.limit(),
				policy.ringSize(), policy.timeout(), policy.retryable(), steps, policy.ringRewards());
		extensions.validate(definition);
		for (var step : definition.steps()) {
			for (var objective : step.objectives()) {
				if (objective.measure() == QuestMeasure.HOLDING) {
					// 人物装备也是道具：ITEM统一涵盖普通物品和装备实例，不要求装备来自指定途径。
					String code = objective.code();
					boolean valid = code.startsWith("ITEM:")
							&& (items.hasItem(code.substring(5)) || items.hasEquipment(code.substring(5)))
							|| code.startsWith("PET:")
									&& creatures.all().stream().anyMatch(pet -> pet.id().equals(code.substring(4)));
					if (!valid)
						throw new IllegalArgumentException("当前持有目标必须引用正式物品或幻兽：" + quest + " / " + code);
				}
			}
		}
		return definition;
	}

	/** 步骤直接使用唯一格式，跨任务依赖作为永久剧情条件，内部依赖进入引擎图。 */
	private QuestStep step(QuestContentStep source, List<String> ids) {
		List<QuestCondition> conditions = new ArrayList<>(source.conditions());
		conditions.add(new QuestCondition("LOCATION", source.location(), 0));
		conditions.add(new QuestCondition("RECEPTION", source.id(), 0));
		if (source.acceptance().explicit() || !source.acceptEffects().isEmpty())
			conditions.add(new QuestCondition("ACCEPTED_STEP", source.id(), 0));
		List<QuestCondition> activation = source.requires()
			.stream()
			.filter(required -> !ids.contains(required))
			.map(required -> new QuestCondition("FACT", "QUEST:" + required, 0))
			.toList();
		return new QuestStep(source.id(), source.requires().stream().filter(ids::contains).toList(),
				source.objectives(), conditions, source.effects(), source.delivery(), source.branch(), source.timeout(),
				activation);
	}

}
