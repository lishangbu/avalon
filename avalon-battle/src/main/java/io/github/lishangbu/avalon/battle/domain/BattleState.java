package io.github.lishangbu.avalon.battle.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private List<Unit> units = new ArrayList<>();

	private List<Order> enemyOrders = new ArrayList<>();

	private List<String> log = new ArrayList<>();

	/** 与实际发奖同事务保存的物品索引，只供战后查看用途；不能据此再次领取。 */
	private List<Loot> loot = new ArrayList<>();

	public record Loot(long owner, String code, int quantity) {
	}

	/** 首领脚本游标独立于回合，阶段切换不能覆盖已经预告的攻击。 */
	private int bossPhase = 1;

	private int bossCursor;

	private int basicTargetCursor;

	private boolean heavyExecuted;

	private String markedTarget;

	private String telegraph;

	/** 非空表示学院隔离训练；虚拟单位永不写入真实资源表，任务奖励由任务执行器结算。 */
	private Training training;

	/** 非空时由副本进度管理战局；阶段编号引用只读副本目录。 */
	private String dungeonStage;

	@Getter
	@Setter
	public static class Training {

		/** 学院课程标识和入场选择用于精确重放；空值表示原职业练习。 */
		private String academyCourse;

		/** 捕捉练习的唯一临时库存与公开资料确认；随战局保存，绝不落真实资产表。 */
		private Map<String, Integer> supplies = new HashMap<>();

		private boolean captureInfoViewed;

		private String selectedBall;

		private boolean capturedAfterDamage;

		public boolean capturePractice() {
			return "BQ020".equals(academyCourse);
		}

		private List<String> selectedPartners = new ArrayList<>();

		private List<String> actedUnits = new ArrayList<>();

		private boolean defendedHit;

		private String announcedTarget;

		/** 换怪练习复用隔离存档，但不使用职业试题和首通奖励。 */
		private boolean swapPractice;

		private boolean swapped;

		private String profession;

		private List<Long> rewardPets = new ArrayList<>();

		private int wallHits;

		private boolean areaCompleted;

		private Map<String, Integer> healTicks = new HashMap<>();

		private List<String> markedAttackers = new ArrayList<>();

	}

	/**
	 * 单位编号保持稳定，位置仅表示入场编队；新捕获幻兽不加入本场 units。
	 */
	@Getter
	@Setter
	public static class Unit {

		private String id;

		private String species;

		private String name;

		private String element = "无";

		private String abilitySlot;

		private String ability;

		/** 临时复制不覆盖原生槽位及捕获身份。 */
		private String copiedAbility;

		public String effect() {
			return copiedAbility == null ? ability : copiedAbility;
		}

		private boolean entered;

		private boolean enemy;

		/** 精英是本次遭遇的增幅，不写入可培养个体档案。 */
		private boolean elite;

		/** 护兽等附属目标由首领策略调度，不独立计奖励或阻止首领败北后的通关。 */
		private boolean mechanism;

		private boolean hero;

		private boolean captured;

		private int position;

		private int level;

		private int hp;

		private int mp;

		private Stats stats;

		private List<String> skills = new ArrayList<>();

		/**
		 * 保存下一次可用回合，CD1 在第 R 回合使用后，R+2 才能再次使用。
		 */
		private Map<String, Integer> ready = new HashMap<>();

		/** 主动换下触发次数随个体保存，接替者护持只持续至下一回合末。 */
		private boolean relayGuardUsed;

		private int relayGuardUntil;

		private int cleanseRound;

		private int poisonStrength;

		private int poisonStarted;

		private int poisonUntil;

		private int periodicResistance;

		private boolean observedPoisonHealing;

		private List<String> observedImmunities = new ArrayList<>();

		private int lightningRedirectRound;

		private boolean thunderCharge;

		private int freeThunderCasts;

		private Map<String, Integer> armorBreaks = new HashMap<>();

		private boolean defenseCharge;

		private boolean actionCharge;

		private boolean fireAbsorbed;

		private boolean woodAbsorbed;

		private int slowUntil;

		private int shield;

		private int shieldUntil;

		/** 入场时固定的套装防御触发量，与消耗魔力的护盾分别结算。 */
		private int defendShield;

		/** 套装护盾只保留到本轮结束，换下或倒下立即清除。 */
		private int setShield;

		/** 光幕只保护施放后的本回合法术直接攻击，布尔状态保证同名效果不叠加。 */
		private boolean magicGuard;

		/** 人物编号将印记收益限定到施法者及其幻兽，不按友方阵营整体增伤。 */
		private String ownerHero;

		private int wallUntil;

		private int wallPercent;

		private Map<String, Mark> marks = new HashMap<>();

		/** 恢复记录施法时的治疗量；每轮末最多一跳，同名覆盖而不叠加。 */
		private int renewAmount;

		private int renewUntil;

		private int renewStarted;

		public boolean alive() {
			return hp > 0 && !captured;
		}

		public boolean active() {
			return enemy || hero || position < 2;
		}

	}

	public record Stats(int health, int mana, int physicalAttack, int magicAttack, int physicalDefense,
			int magicDefense, int speed) {
	}

	/**
	 * 客户端只提交行动与目标，不能携带伤害、概率或随机数。
	 */
	public record Mark(int until, int percent) {
	}

	public record Order(String actor, String action, String target, List<String> targets) {
		public Order(String actor, String action, String target) {
			this(actor, action, target, List.of());
		}
	}

}
