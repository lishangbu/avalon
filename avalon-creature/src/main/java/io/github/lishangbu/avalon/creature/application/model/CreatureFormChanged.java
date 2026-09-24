package io.github.lishangbu.avalon.creature.application.model;

/**
 * 形态变更的同步领域通知。发布者必须已取得角色锁、核验归属并完成形态修改。 只传稳定编号与变化前后的物种，不将幻兽实体或资源存档暴露给其他模块。
 * 观察者在同一事务内保留资源；资源更新失败时，材料扣除与形态变更也必须回滚。
 */
public record CreatureFormChanged(long characterId, long creatureId, int level, String previousSpecies,
		String species) {
	public CreatureFormChanged {
		if (characterId <= 0 || creatureId <= 0 || level < 1 || previousSpecies == null || previousSpecies.isBlank()
				|| species == null || species.isBlank() || species.equals(previousSpecies))
			throw new IllegalArgumentException("伙伴形态变更通知无效");
	}
}
