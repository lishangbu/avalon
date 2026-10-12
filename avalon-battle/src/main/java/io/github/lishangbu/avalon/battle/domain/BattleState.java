package io.github.lishangbu.avalon.battle.domain;

import java.util.ArrayList;
import java.util.List;
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

	private String encounter;

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
	private int bossPhase = 1;

	private int bossCursor;

	private int basicTargetCursor;

	private boolean heavyExecuted;

	private String markedTarget;

	private String telegraph;

	/** 非空表示学院隔离训练；虚拟单位永不写入真实资源表，任务奖励由任务执行器结算。 */
	private BattleStateTraining training;

	/** 非空时由副本进度管理战局；阶段编号引用只读副本目录。 */
	private String dungeonStage;

}
