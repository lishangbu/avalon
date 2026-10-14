package io.github.lishangbu.avalon.combat;

import io.github.lishangbu.avalon.combat.model.CombatResourceChange;
import java.util.*;

/** 纯内存资源账本：基于回合开始快照计算可用数量并输出消耗，执行过程中绝不接触真实库存。 */
public final class CombatResources {

	private final Map<String, Map<String, Long>> available = new HashMap<>();

	private final List<CombatResourceChange> changes = new ArrayList<>();

	/** 深复制外部库存快照；负库存或缺失归属拒绝，不能让处理器修改原快照。 */
	public CombatResources(Map<String, Map<String, Long>> snapshot) {
		snapshot.forEach((owner, items) -> {
			if (owner == null || owner.isBlank())
				throw new IllegalArgumentException("资源快照归属不能为空");
			var copy = new HashMap<String, Long>();
			items.forEach((code, amount) -> {
				if (code == null || code.isBlank() || amount == null || amount < 0)
					throw new IllegalArgumentException("资源快照数量无效");
				copy.put(code, amount);
			});
			available.put(owner, copy);
		});
	}

	/** 返回本回合已预留消耗后的数量，同一物品多次使用不会重复读取初始库存。 */
	public long count(String owner, String code) {
		return available.getOrDefault(owner, Map.of()).getOrDefault(code, 0L);
	}

	/** 预留消耗并记录纯输出；不足时抛出异常，宿主丢弃整轮输出而不是提交前半轮消耗。 */
	public void consume(String owner, String code, long amount) {
		if (amount <= 0 || count(owner, code) < amount)
			throw new IllegalStateException("战斗物品不足");
		available.get(owner).put(code, count(owner, code) - amount);
		changes.add(new CombatResourceChange("CONSUME", owner, code, amount, null));
	}

	/** 捕获等非库存变更也只登记输出；调用前必须由游戏规则核验快照资格。 */
	public void change(CombatResourceChange change) {
		changes.add(Objects.requireNonNull(change));
	}

	/** 回合成功后由宿主在保存状态的同一事务执行，失败则全部回滚。 */
	public List<CombatResourceChange> changes() {
		return List.copyOf(changes);
	}

}
