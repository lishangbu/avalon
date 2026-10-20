package io.github.lishangbu.avalon.battle.domain;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * 仅在持锁事务中解码和修改的战局存档，不直接作为 HTTP 响应，避免泄露敌方特性及随机种子。
 */
@Getter
@Setter
public class BattleState {

	/**
	 * 由服务器生成的战局种子；每轮使用固定派生值，事务失败后的重试也不改变结果。
	 */
	private long seed;

	private int round = 1;

	/** 完整回合的乐观修订，成员单独提交不改变同一回合窗口。 */
	private int revision;

	private String encounter;

	/** 双方均由玩家提交指令；禁止生成AI意图及捕捉。 */
	private boolean pvp;

	/** 开战时人物实际穿戴的槽位快照，不能用战后换装补足装备实战目标。 */
	private List<String> equippedSlots = new ArrayList<>();

	private String status = "ACTIVE";

	private String weather;

	private int weatherUntil;

	private int weatherSourceLevel;

	private int electricUntil;

	private boolean captureAllowed;

	private List<BattleStateUnit> units = new ArrayList<>();

	private List<BattleStateOrder> enemyOrders = new ArrayList<>();

	private List<String> log = new ArrayList<>();

	/** 与实际发奖同事务保存的物品索引，只供战后查看用途；不能据此再次领取。 */
	private List<BattleStateLoot> loot = new ArrayList<>();

	/** 首领脚本游标独立于回合，阶段切换不能覆盖已经预告的攻击。 */

	private String telegraph;

	/** 非空表示学院隔离训练；虚拟单位永不写入真实资源表，任务奖励由任务执行器结算。 */
	private BattleStateTraining training;

	/** 非空时由副本进度管理战局；阶段编号引用只读副本目录。 */
	private String dungeonStage;

	/** 用完整回合隔离结果替换当前解码状态；失败时此方法不会调用，不允许部分字段提交。 */
	public void copyFrom(BattleState result) {
		this.mechanisms = result.mechanisms;
		this.seed = result.seed;
		this.round = result.round;
		this.revision = result.revision;
		this.encounter = result.encounter;
		this.pvp = result.pvp;
		this.equippedSlots = new ArrayList<>(result.equippedSlots);
		this.status = result.status;
		this.weather = result.weather;
		this.weatherUntil = result.weatherUntil;
		this.weatherSourceLevel = result.weatherSourceLevel;
		this.electricUntil = result.electricUntil;
		this.captureAllowed = result.captureAllowed;
		var originalUnits = units.stream().collect(Collectors.toMap(BattleStateUnit::getId, unit -> unit));
		this.units = new ArrayList<>();
		for (var next : result.units) {
			var prior = originalUnits.get(next.getId());
			if (prior != null) {
				prior.copyFrom(next);
				this.units.add(prior);
			}
			else
				this.units.add(next);
		}
		this.enemyOrders = result.enemyOrders;
		this.log = result.log;
		this.loot = result.loot;
		this.telegraph = result.telegraph;
		this.training = result.training;
		this.dungeonStage = result.dungeonStage;
	}

	/** 策略私有数据按策略代码分组，新增首领机制不再给公共战局新增游标字段。 */
	private Map<String, Map<String, String>> mechanisms = new HashMap<>();

}
