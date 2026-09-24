package io.github.lishangbu.avalon.creature.domain;

import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.Set;

/** 正式仓库与隔离课程共用的位置规则；只处理已验证归属的个体，不访问正式资产。 */
public final class CreatureStorageRules {

	public static final int FIRST_STORAGE = 100;

	public static final int STORAGE_CAPACITY = 30;

	private CreatureStorageRules() {
	}

	/** 在场个体先调整到替补或携带位才能存仓；已有仓库位置保持不变。 */
	public static int deposit(int current, Set<Integer> occupied) {
		if (current >= FIRST_STORAGE && current < FIRST_STORAGE + STORAGE_CAPACITY)
			return current;
		if (current < 2)
			throw new BusinessException(409, "请先把出战伙伴调整到替补或携带位再存仓");
		for (int position = FIRST_STORAGE; position < FIRST_STORAGE + STORAGE_CAPACITY; position++)
			if (!occupied.contains(position))
				return position;
		throw new BusinessException(409, "仓库已满");
	}

	/** 取出和普通编队共享携带容量、人物等级与地点服务约束；交换占位由各自仓储执行。 */
	public static void requirePosition(int current, int target, int capacity, int level, int heroLevel,
			boolean storageAvailable) {
		if (target < 0 || target >= capacity)
			throw new BusinessException(400, "随身位置无效");
		if (level > heroLevel)
			throw new BusinessException(409, "伙伴等级高于人物");
		if (current >= FIRST_STORAGE && !storageAvailable)
			throw new BusinessException(409, "请到幻兽中心取出仓库伙伴");
	}

}
