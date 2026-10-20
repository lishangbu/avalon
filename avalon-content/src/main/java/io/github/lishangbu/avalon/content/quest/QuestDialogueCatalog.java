package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestDialogue;
import io.github.lishangbu.avalon.content.quest.model.QuestDialoguesDefinition;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 任务阶段对白的正式内容目录。完成交付仍使用任务步骤的dialogue，再访不重放交付回执。 */
@Component
public class QuestDialogueCatalog {

	private final QuestDialoguesDefinition definition;

	public QuestDialogueCatalog(QuestCatalog quests) {
		definition = validate(QuestDocuments.current().dialogues(), quests);
	}

	static QuestDialoguesDefinition validate(QuestDialoguesDefinition definition, QuestCatalog quests) {
		Set<String> ids = quests.definition().steps().stream().map(QuestContentStep::id).collect(Collectors.toSet());
		if (!definition.steps().keySet().equals(ids))
			throw new IllegalArgumentException("阶段对白必须完整覆盖且仅覆盖正式任务步骤");
		for (var entry : definition.steps().entrySet()) {
			var value = entry.getValue();
			if (value == null || blank(value.offer()) || blank(value.accepted()) || blank(value.progress())
					|| blank(value.ready()) || blank(value.revisit()) || blank(value.resume()))
				throw new IllegalArgumentException("任务阶段对白不完整：" + entry.getKey());
		}
		return definition;
	}

	private static boolean blank(String text) {
		return text == null || text.isBlank();
	}

	public QuestDialogue step(String id) {
		var text = definition.steps().get(id);
		if (text == null)
			throw new IllegalArgumentException("缺少任务阶段对白：" + id);
		return text;
	}

}
