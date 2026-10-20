package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.quest.model.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** 任务链是唯一编辑来源；启动时展开为运行时使用的不可变目录。 */
public record QuestDocuments(QuestContentDefinition content, QuestRulesDefinition rules,
		QuestDialoguesDefinition dialogues, Map<String, String> sources) {
	private static class Holder {

		private static final QuestDocuments VALUE = load();

	}

	public static QuestDocuments current() {
		return Holder.VALUE;
	}

	private static QuestDocuments load() {
		return fromDocuments(ContentLoader.documents("quests"));
	}

	public static QuestDocuments fromDocuments(Map<String, JsonNode> documents) {
		var steps = new ArrayList<QuestContentStep>();
		var rules = new ArrayList<QuestRule>();
		var dialogues = new LinkedHashMap<String, QuestDialogue>();
		var sources = new LinkedHashMap<String, String>();
		var tags = new ArrayList<QuestTag>();
		for (var entry : new java.util.TreeMap<>(documents).entrySet()) {
			String path = entry.getKey();
			var document = entry.getValue();
			ContentLoader.validate(path, document);
			if (path.equals("/content/quests/tags.json")) {
				for (var tag : document.path("tags"))
					tags.add(ContentLoader.convert(tag, QuestTag.class));
				continue;
			}
			for (var raw : document.path("quests")) {
				ObjectNode task = (ObjectNode) raw.deepCopy();
				var taskSteps = task.remove("steps");
				var rule = ContentLoader.convert(task, QuestRule.class);
				register(sources, "quest:" + rule.id(), path);
				rules.add(rule);
				for (var rawStep : taskSteps) {
					ObjectNode step = (ObjectNode) rawStep.deepCopy();
					var text = ContentLoader.convert(step.remove("conversations"), QuestDialogue.class);
					var definition = ContentLoader.convert(step, QuestContentStep.class);
					if (!definition.quest().equals(rule.id()))
						throw new IllegalArgumentException(
								path + " quests[" + rule.id() + "].steps[" + definition.id() + "].quest：必须属于所在任务");
					register(sources, "step:" + definition.id(), path);
					steps.add(definition);
					dialogues.put(definition.id(), text);
				}
			}
		}
		return new QuestDocuments(new QuestContentDefinition(List.copyOf(steps)),
				new QuestRulesDefinition(List.copyOf(tags), List.copyOf(rules)),
				new QuestDialoguesDefinition(dialogues), Map.copyOf(sources));
	}

	private static void register(Map<String, String> sources, String key, String path) {
		String previous = sources.putIfAbsent(key, path);
		if (previous != null)
			throw new IllegalArgumentException("重复编号 " + key + "：" + previous + "；" + path);
	}
}
