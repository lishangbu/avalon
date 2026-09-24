package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.platform.BusinessException;

/**
 * 穿戴资格的纯业务规则，不读取数据库、不管理事务，也不依赖 JPA 实体。 应用层先在角色锁内确认物品归属，再将当前实例状态传入本规则。
 * 校验失败不得发生卸装；已经穿戴的物品仍须先通过资格校验，之后才按幂等返回。
 */
public final class EquipmentRules {

	private EquipmentRules() {
	}

	/**
	 * 当前规则只接受有装备部位、正库存且人物等级足够的物品。
	 */
	public static void requireWearable(String slot, int quantity, int requiredLevel, int level) {
		if (slot == null || quantity < 1 || level < requiredLevel) {
			throw new BusinessException(400, "该物品不可穿戴或等级不足");
		}
	}

}
