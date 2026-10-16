package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestRewardItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 奖励预览只读取已经出现的任务，避免详情接口泄露未来标题或奖励；不会领取资源。 */
@Component
@RequiredArgsConstructor
public class QuestRewardPreviewQueryService {

	private final QuestQueryService quests;

	private final QuestCatalog catalog;

	private final ItemCatalog items;

	public List<QuestRewardItem> items(long owner, String step) {
		var state = quests.state(owner);
		if (state.available().stream().noneMatch(action -> action.id().equals(step)) && state.journal()
			.stream()
			.flatMap(entry -> entry.steps().stream())
			.noneMatch(value -> value.id().equals(step)))
			throw new BusinessException(404, "任务尚未出现");
		var definition = catalog.step(step);
		return definition.effects()
			.stream()
			.filter(effect -> effect.kind().equals("GIVE_ITEM") || effect.kind().equals("GIVE_EQUIPMENT"))
			.map(effect -> new QuestRewardItem(effect.reference(),
					items.hasItem(effect.reference()) ? items.item(effect.reference()).name()
							: items.equipment(effect.reference()).name(),
					Math.toIntExact(effect.amount())))
			.toList();
	}

}
