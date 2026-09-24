package io.github.lishangbu.avalon.creature.application.model;

/**
 * 托管交付的同步通知；双方角色锁由市场事务持有，资源归属必须与个体归属一起提交。 退货允许原归属与新归属相同，不能因此恢复生命或魔力。
 */
public record CreatureOwnershipChanged(long previousCharacterId, long characterId, long creatureId) {
	public CreatureOwnershipChanged {
		if (previousCharacterId <= 0 || characterId <= 0 || creatureId <= 0)
			throw new IllegalArgumentException("伙伴归属变更通知无效");
	}
}
