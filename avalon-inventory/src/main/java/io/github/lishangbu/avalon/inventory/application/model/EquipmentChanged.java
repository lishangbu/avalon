package io.github.lishangbu.avalon.inventory.application.model;

import java.util.Map;

/**
 * 同步换装通知，携带换装前的完整装备属性；不暴露库存实体。 观察者与库存命令共用事务，资源截断失败时穿戴变更也必须回滚。
 */
public record EquipmentChanged(long owner, Map<String, Integer> previousStats) {
	public EquipmentChanged {
		previousStats = Map.copyOf(previousStats);
	}
}
