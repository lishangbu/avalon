package io.github.lishangbu.avalon.inventory.persistence.mapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.content.item.EquipmentCatalog;
import io.github.lishangbu.avalon.content.item.EnhancementCatalog;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRules;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 现有数据库 JSON 字符串适配器。损坏数据作为内部故障上报，绝不伪造空属性或空技能。
 */
@Component
@RequiredArgsConstructor
public class InventoryStorageCodec {

	private final ObjectMapper json;

	private final EquipmentCatalog equipment;

	private final EnhancementCatalog enhancement;

	/** 只供库存内部培养计算读取原始值，调用者不能将已经强化的展示属性写回原始值。 */
	public Map<String, Integer> originalStats(OwnedItem item) {
		return Map.copyOf(json.readValue(item.getStats(), new TypeReference<Map<String, Integer>>() {
		}));
	}

	public <T> T decode(String value, Class<T> type) {
		return json.readValue(value, type);
	}

	public String encode(Object value) {
		return json.writeValueAsString(value);
	}

	/**
	 * 只在 HTTP 展示边界转换雪花 ID；持久化数据异常不能静默丢弃。
	 */
	public InventoryItemView view(OwnedItem i) {
		Map<String, Integer> stats = json.readValue(i.getStats(), new TypeReference<Map<String, Integer>>() {
		});
		return new InventoryItemView(i.getId().toString(), i.getName(), i.getCode(), i.getQuantity(), i.getSlot(),
				i.getRequiredLevel(),
				i.getSlot() == null ? stats
						: EnhancementRules.stats(stats, i.getEnhancementLevel(), enhancement.definition()),
				i.isEquipped(), i.isBound(), i.getQuality(), i.getSellPrice(),
				i.getSlot() == null ? 0 : equipment.metadata(i.getCode()).price(),
				i.getSource().startsWith("EQUIPMENT_BUY:") ? "商店购买" : i.getSource().startsWith("MARKET:BUY:") ? "交易所购入"
						: i.getSource().startsWith("MARKET:RETURN:") ? "交易所退货" : i.getSource()
							.startsWith("EVOLUTION:TEACHING:")
									? "进化教学赠送"
									: i.getSource().startsWith("RESEARCH:EXCHANGE:") ? "研究凭证兑换" : i.getSource()
										.startsWith("CULTIVATION:RESEARCH_COMMISSION:")
												? "研究委托奖励"
												: i.getSource().startsWith("CULTIVATION:REGIONAL_ELITE:") ? "区域精英掉落" : i
													.getSource()
													.startsWith("CULTIVATION:ENHANCEMENT_COMMISSION:")
															? "强化委托奖励"
															: i.getSource()
																.startsWith("CULTIVATION:ENHANCEMENT_DUNGEON:")
																		? "强化副本奖励"
																		: i.getSource().startsWith("DUNGEON_CHEST:")
																				? "副本宝箱"
																				: i.getSource().startsWith("A")
																						? "冒险任务奖励" : "冒险奖励",
				i.getSlot() == null ? null : equipment.metadata(i.getCode()).profession(), i.getEnhancementLevel());
	}

}
