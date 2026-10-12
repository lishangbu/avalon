package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.content.quest.model.QuestContentEffectKind;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 查询提示与交付命令共用的即时条件。只读取当前库存和编队，不提前扣除物品； 写命令在角色锁内再次调用，随后实际消耗仍属于原奖励事务。
 */
@Component
@RequiredArgsConstructor
public class QuestDeliveryRequirements {

	private final CreatureQueryService creatures;

	private final InventoryQueryService inventory;

	/** 返回未满足条件的说明；null表示即时条件齐全，仍须单独核验任务参与、事实及地点。 */
	public String missing(long owner, QuestContentStep step) {
		if (step.minimumActive() > 0 && creatures.list(owner)
			.stream()
			.filter(pet -> pet.partyPosition() >= 0 && pet.partyPosition() < 2)
			.count() < step.minimumActive())
			return "请先编入至少" + step.minimumActive() + "只出战伙伴";
		if (step.placement() != null
				&& !creatures.hasPlacement(owner, step.placement().sourcePrefix(), step.placement().position()))
			return "请将任务要求的伙伴编入第" + (step.placement().position() + 1) + "出战位后回报";
		// 合并同物品的多次消耗；待领物品不在可用库存中，不能提前用来交付。
		if (step.effects().stream().anyMatch(effect -> effect.kind() == QuestContentEffectKind.TAKE_ITEM)) {
			Map<String, Long> remaining = new HashMap<>();
			for (var item : inventory.list(owner))
				remaining.merge(item.code(), (long) item.quantity(), Long::sum);
			for (var effect : step.effects()) {
				if (effect.kind() == QuestContentEffectKind.TAKE_ITEM) {
					long quantity = remaining.getOrDefault(effect.reference(), 0L);
					if (quantity < effect.amount())
						return "缺少需要交付的任务物品";
					remaining.put(effect.reference(), quantity - effect.amount());
				}
			}
		}
		return null;
	}

}
