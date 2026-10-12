package io.github.lishangbu.avalon.battle.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 单位编号保持稳定，位置仅表示入场编队；新捕获幻兽不加入本场 units。
 */
@Getter
@Setter
public class BattleStateUnit {

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

	private BattleStateStats stats;

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

	private Map<String, BattleStateMark> marks = new HashMap<>();

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
