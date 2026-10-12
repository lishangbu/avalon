package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffectKind;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import io.github.lishangbu.avalon.world.application.query.QuestObjectiveQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 游戏内容到任务核心的唯一映射。旧任务无需迁移JSON即可使用实例引擎；新类型声明runtime和policy。
 * 跨任务依赖转换为宿主事实条件，同任务依赖保留在核心图中。构造时核验全部配置及注册扩展。
 */
@Component
public class QuestEngineCatalog {

	private final QuestCatalog catalog;

	private final QuestRulesCatalog rules;

	private final QuestExtensions extensions;

	/** 当前持有目标的引用同物品目录校验，不等到玩家接取后才发现拼写错误。 */
	private final ItemCatalog items;

	public QuestEngineCatalog(QuestCatalog catalog, QuestRulesCatalog rules, QuestExtensions extensions,
			ItemCatalog items) {
		this.catalog = catalog;
		this.rules = rules;
		this.extensions = extensions;
		this.items = items;
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
				QuestRepeat.valueOf(rule.repeatPolicy().name()), policy.timezone(), policy.limit(), policy.ringSize(),
				policy.timeout(), policy.retryable(), steps, policy.ringRewards());
		extensions.validate(definition);
		for (var step : definition.steps()) {
			for (var objective : step.objectives()) {
				if (objective.measure() == QuestMeasure.HOLDING
						&& (!objective.code().startsWith("ITEM:") || !items.hasItem(objective.code().substring(5))))
					throw new IllegalArgumentException("当前持有目标必须引用正式物品：" + quest + " / " + objective.code());
			}
		}
		return definition;
	}

	/** 保留既有事实的历史语义，同时允许显式配置数量、即时持有和新行为代码。 */
	private QuestStep step(QuestContentStep source, List<String> ids) {
		var runtime = source.runtime();
		List<QuestObjective> objectives = new ArrayList<>();
		for (String fact : source.facts())
			objectives.add(new QuestObjective(fact,
					QuestObjectiveQueryService.historical(fact) ? QuestMeasure.HISTORY : QuestMeasure.EVENT, fact, 1));
		List<QuestCondition> conditions = new ArrayList<>();
		List<QuestCondition> activation = new ArrayList<>();
		conditions.add(new QuestCondition("LOCATION", source.location(), 0));
		conditions.add(new QuestCondition("RECEPTION", source.id(), 0));
		conditions.add(new QuestCondition("DELIVERY", source.id(), 0));
		if (!source.acceptEffects().isEmpty())
			conditions.add(new QuestCondition("ACCEPTED_STEP", source.id(), 0));
		source.requires()
			.stream()
			.filter(required -> !ids.contains(required))
			.forEach(required -> activation.add(new QuestCondition("FACT", "QUEST:" + required, 0)));
		List<QuestEffect> effects = new ArrayList<>();
		source.effects()
			.forEach(effect -> effects.add(new QuestEffect(effect.kind().name(), effect.reference(), effect.amount())));
		if (runtime != null) {
			objectives.addAll(runtime.objectives());
			conditions.addAll(runtime.conditions());
			effects.addAll(runtime.effects());
		}
		// 显式扩展的消耗也必须合并检查，旧effects的合并由DELIVERY适配器处理。
		if (runtime != null) {
			Map<String, Long> consumed = new HashMap<>();
			runtime.effects()
				.stream()
				.filter(effect -> effect.kind().equals("TAKE_ITEM"))
				.forEach(effect -> consumed.merge(effect.reference(), effect.amount(), Math::addExact));
			source.effects()
				.stream()
				.filter(effect -> effect.kind() == QuestContentEffectKind.TAKE_ITEM)
				.forEach(effect -> consumed.merge(effect.reference(), (long) effect.amount(), Math::addExact));
			consumed.forEach((item, amount) -> conditions.add(new QuestCondition("INVENTORY", item, amount)));
		}
		return new QuestStep(source.id(), source.requires().stream().filter(ids::contains).toList(), objectives,
				conditions, effects,
				runtime == null ? source.onArrival() ? QuestDelivery.AUTO : QuestDelivery.MANUAL : runtime.delivery(),
				runtime == null ? null : runtime.branch(), runtime == null ? null : runtime.timeout(), activation);
	}

}
