package io.github.lishangbu.avalon.creature.domain;

import io.github.lishangbu.avalon.platform.BusinessException;

import java.util.Set;

/**
 * 当前赠送伙伴的空位分配规则。纯计算只决定位置，不保存伙伴实例。 调用者须在角色锁内读取 occupied 并保存结果，以防两个领取同时选择同一个位置。
 * 此规则保持已有赠宠顺序；捕捉另按替补、携带、仓库的顺序分配。
 */
public final class PartyRules {

	private PartyRules() {
	}

	/**
	 * 返回容量范围内最小空位；全部占用则拒绝整个领取事务，不自动挤出已有伙伴。
	 */
	public static int firstFree(Set<Integer> occupied, int capacity) {
		for (int position = 0; position < capacity; position++) {
			if (!occupied.contains(position))
				return position;
		}
		throw new BusinessException(409, "随身伙伴位置已满，请先整理");
	}

}
