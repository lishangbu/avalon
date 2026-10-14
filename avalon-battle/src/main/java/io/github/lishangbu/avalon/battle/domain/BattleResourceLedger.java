package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.CombatResources;
import io.github.lishangbu.avalon.combat.model.CombatResourceChange;
import java.util.List;

/** Avalon纯资源账本：执行接口只操作内存，所有真实资产写入必须由应用事务消费changes。 */
final class BattleResourceLedger implements BattleEngineResources {

	private final CombatResources resources;

	private final BattleResourceSnapshot snapshot;

	private String captureActor;

	/** 根据不可变快照初始化，不保留原始库存服务引用。 */
	BattleResourceLedger(BattleResourceSnapshot snapshot) {
		this.snapshot = snapshot;
		resources = new CombatResources(snapshot.items());
	}

	/** 多人/单人统一按行动者查询，缺失归属不沿用其他玩家库存。 */
	public int count(BattleStateUnit actor, String code) {
		return Math.toIntExact(resources.count(actor.getId(), code));
	}

	/** 消耗只生成变更；多个物品按本回合剩余数量检查，事务失败时整个账本丢弃。 */
	public void take(BattleStateUnit actor, String code) {
		resources.consume(actor.getId(), code, 1);
		captureActor = actor.getId();
	}

	/** 宿主预先核验等级与容量；已登记捕获后不能再次使用同一捕获资格。 */
	public boolean room(BattleStateUnit target) {
		return snapshot.capturable().contains(target.getId())
				&& resources.changes().stream().noneMatch(change -> change.kind().equals("CAPTURE"));
	}

	/** 捕获只记录目标与行动者，培养个体和图鉴均由宿主在同事务创建。 */
	public void capture(BattleStateUnit target) {
		resources.change(new CombatResourceChange("CAPTURE", captureActor, target.getSpecies(), 1, target.getId()));
	}

	/** 成功回合的资产输出不可变，不能在提交后追加新消耗。 */
	List<CombatResourceChange> changes() {
		return resources.changes();
	}

}
