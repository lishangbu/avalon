package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 整项任务的规则与标签。步骤仍由任务目录定义，类型和标签不能从编号前缀猜测。 */
@Component
public class QuestRulesCatalog {

	private final Definition definition;

	public QuestRulesCatalog(QuestCatalog quests) {
		definition = validate(ContentLoader.read("/content/quest-rules.json", Definition.class), quests.definition());
	}

	/** 所有任务必须显式声明规则；多余规则、未知标签和可放弃主线均阻止启动。 */
	static Definition validate(Definition definition, QuestCatalog.Definition quests) {
		Set<String> tags = new HashSet<>();
		for (var tag : definition.tags()) {
			if (tag.id() == null || tag.id().isBlank() || tag.name() == null || tag.name().isBlank()
					|| !tags.add(tag.id()))
				throw new IllegalArgumentException("任务标签编号或名称无效");
		}
		Set<String> configured = new HashSet<>();
		for (var rule : definition.quests()) {
			if (rule.id() == null || rule.id().isBlank() || !configured.add(rule.id()) || rule.title() == null
					|| rule.title().isBlank() || rule.type() == null || rule.repeatPolicy() == null
					|| rule.type() == Type.MAIN && rule.abandonable() || !tags.containsAll(rule.tags())
					|| new HashSet<>(rule.tags()).size() != rule.tags().size())
				throw new IllegalArgumentException("任务规则或标签无效：" + rule.id());
		}
		Set<String> actual = new HashSet<>(quests.steps().stream().map(QuestCatalog.Step::quest).toList());
		if (!actual.equals(configured))
			throw new IllegalArgumentException("任务规则必须完整覆盖且仅覆盖正式任务");
		return definition;
	}

	public Definition definition() {
		return definition;
	}

	public Rule rule(String id) {
		return definition.quests().stream().filter(rule -> rule.id().equals(id)).findFirst().orElseThrow();
	}

	public enum Type {

		MAIN, SIDE

	}

	public enum RepeatPolicy {

		ONCE

	}

	public record Tag(String id, String name) {
	}

	/** 重复策略与放弃资格独立配置；标签仅用于显示和筛选，不授予执行资格。 */
	public record Rule(String id, String title, Type type, List<String> tags, RepeatPolicy repeatPolicy,
			Boolean abandonable) {
		public Rule {
			tags = List.copyOf(tags);
			// 主线默认不可放弃，支线默认可放弃；显式配置仍须经过主线约束校验。
			abandonable = abandonable == null ? type == Type.SIDE : abandonable;
		}
	}

	public record Definition(List<Tag> tags, List<Rule> quests) {
		public Definition {
			tags = List.copyOf(tags);
			quests = List.copyOf(quests);
		}
	}

}
