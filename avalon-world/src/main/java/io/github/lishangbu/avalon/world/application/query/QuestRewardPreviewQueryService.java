package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
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

	public List<Item> items(long owner, String step) {
		var state = quests.state(owner);
		if (state.available().stream().noneMatch(action -> action.id().equals(step)) && state.journal()
			.stream()
			.flatMap(entry -> entry.steps().stream())
			.noneMatch(value -> value.id().equals(step)))
			throw new BusinessException(404, "任务尚未出现");
		return catalog.step(step)
			.effects()
			.stream()
			.filter(effect -> effect.kind() == QuestCatalog.Kind.GIVE_ITEM
					|| effect.kind() == QuestCatalog.Kind.GIVE_EQUIPMENT)
			.map(effect -> new Item(effect.reference(),
					items.hasItem(effect.reference()) ? items.item(effect.reference()).name()
							: items.equipment(effect.reference()).name(),
					effect.amount()))
			.toList();
	}

	public record Item(String code, String name, int quantity) {
	}

}
