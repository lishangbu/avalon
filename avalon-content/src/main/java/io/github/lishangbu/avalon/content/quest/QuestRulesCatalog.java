package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 整项任务的规则与标签。步骤仍由任务目录定义，类型和标签不能从编号前缀猜测。 */
@Component
public class QuestRulesCatalog {

	private final QuestRulesDefinition definition;

	public QuestRulesCatalog(QuestCatalog quests) {
		definition = validate(QuestDocuments.current().rules(), quests.definition());
	}

	/** 所有任务必须显式声明规则；多余规则、未知标签和可放弃主线均阻止启动。 */
	static QuestRulesDefinition validate(QuestRulesDefinition definition, QuestContentDefinition quests) {
		Set<String> tags = new HashSet<>();
		for (var tag : definition.tags()) {
			if (tag.id() == null || tag.id().isBlank() || tag.name() == null || tag.name().isBlank()
					|| !tags.add(tag.id()))
				throw new IllegalArgumentException("任务标签编号或名称无效");
		}
		Set<String> configured = new HashSet<>();
		for (var rule : definition.quests()) {
			if (rule.id() == null || rule.id().isBlank() || !configured.add(rule.id()) || rule.title() == null
					|| rule.title().isBlank() || rule.type() == null || rule.policy().repeatPolicy() == null
					|| rule.type() == QuestType.MAIN && rule.abandonable() || !tags.containsAll(rule.tags())
					|| new HashSet<>(rule.tags()).size() != rule.tags().size())
				throw new IllegalArgumentException("任务规则或标签无效：" + rule.id());
		}
		Set<String> actual = new HashSet<>(quests.steps().stream().map(QuestContentStep::quest).toList());
		if (!actual.equals(configured))
			throw new IllegalArgumentException("任务规则必须完整覆盖且仅覆盖正式任务");
		return definition;
	}

	public QuestRulesDefinition definition() {
		return definition;
	}

	public QuestRule rule(String id) {
		return definition.quests().stream().filter(rule -> rule.id().equals(id)).findFirst().orElseThrow();
	}

}
