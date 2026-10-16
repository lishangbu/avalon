package io.github.lishangbu.avalon.battle.domain;

import java.util.ArrayList;
import io.github.lishangbu.avalon.combat.model.CombatStatus;
import io.github.lishangbu.avalon.combat.model.CombatStatusStacking;
import io.github.lishangbu.avalon.combat.CombatStatusRules;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

	/** 入场时固定的套装防御触发量，与消耗魔力的护盾分别结算。 */
	private int defendShield;

	/** 人物编号将印记收益限定到施法者及其幻兽，不按友方阵营整体增伤。 */
	private String ownerHero;

	public boolean alive() {
		return hp > 0 && !captured;
	}

	public boolean active() {
		return enemy || hero || position < 2;
	}

	/** 同一单位编号的隔离输出覆盖当前领域对象，保持调用方的单位身份引用；失败回合不调用。 */
	public void copyFrom(BattleStateUnit result) {
		this.statuses = result.statuses;
		this.id = result.id;
		this.species = result.species;
		this.name = result.name;
		this.element = result.element;
		this.abilitySlot = result.abilitySlot;
		this.ability = result.ability;
		this.copiedAbility = result.copiedAbility;
		this.entered = result.entered;
		this.enemy = result.enemy;
		this.elite = result.elite;
		this.mechanism = result.mechanism;
		this.hero = result.hero;
		this.captured = result.captured;
		this.position = result.position;
		this.level = result.level;
		this.hp = result.hp;
		this.mp = result.mp;
		this.stats = result.stats;
		this.skills = result.skills;
		this.ready = result.ready;
		this.relayGuardUsed = result.relayGuardUsed;
		this.relayGuardUntil = result.relayGuardUntil;
		this.cleanseRound = result.cleanseRound;
		this.periodicResistance = result.periodicResistance;
		this.observedPoisonHealing = result.observedPoisonHealing;
		this.observedImmunities = result.observedImmunities;
		this.lightningRedirectRound = result.lightningRedirectRound;
		this.thunderCharge = result.thunderCharge;
		this.freeThunderCasts = result.freeThunderCasts;
		this.armorBreaks = result.armorBreaks;
		this.defenseCharge = result.defenseCharge;
		this.actionCharge = result.actionCharge;
		this.fireAbsorbed = result.fireAbsorbed;
		this.woodAbsorbed = result.woodAbsorbed;
		this.defendShield = result.defendShield;
		this.ownerHero = result.ownerHero;
	}

	/** 所有短期状态统一保存；新增状态只增加定义和处理器，不增加单位JSON字段。 */
	private Map<String, CombatStatus> statuses = new HashMap<>();

	/** 现有规则的类型化访问只读统一状态参数，不是旧存档导入或双份状态。 */
	private long statusValue(String kind, String parameter) {
		var status = statuses.get(kind);
		if (status == null)
			return 0;
		return switch (parameter) {
			case "started" -> status.started();
			case "until" -> status.until();
			case "strength" -> status.strength();
			default -> status.values().getOrDefault(parameter, 0L);
		};
	}

	/** 修改统一实例的数值或期限；起始回合由installStatus整体安装，不再保留单独写入口。唯一存档是statuses。 */
	private void statusValue(String kind, String parameter, long value) {
		var current = statuses.getOrDefault(kind,
				new CombatStatus(kind, id == null ? "unassigned" : id, 0, 0, 0, Map.of(), Map.of()));
		if ((parameter.equals("until") || parameter.equals("active")) && value == 0) {
			statuses.remove(kind);
			return;
		}
		var values = new HashMap<>(current.values());
		if (!List.of("until", "strength").contains(parameter))
			values.put(parameter, value);
		statuses.put(kind,
				new CombatStatus(kind, current.source(), current.started(),
						parameter.equals("until") ? Math.toIntExact(value) : current.until(),
						parameter.equals("strength") ? value : current.strength(), values, current.references()));
	}

	/** 规则显式提供来源和期限，通用算法处理叠加；不得仅通过单位自身编号推断施法者。 */
	public void installStatus(CombatStatus status, CombatStatusStacking stacking, long maximum) {
		new CombatStatusRules().install(statuses, status.kind(), status, stacking, maximum);
	}

	/** POISON状态的strength参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getPoisonStrength() {
		return Math.toIntExact(statusValue("POISON", "strength"));
	}

	/** 在当前隔离状态上更新POISON的strength，不修改真实角色资产。 */
	@JsonIgnore
	public void setPoisonStrength(int value) {
		statusValue("POISON", "strength", value);
	}

	/** POISON状态的started参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getPoisonStarted() {
		return Math.toIntExact(statusValue("POISON", "started"));
	}

	/** POISON状态的until参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getPoisonUntil() {
		return Math.toIntExact(statusValue("POISON", "until"));
	}

	/** 在当前隔离状态上更新POISON的until，不修改真实角色资产。 */
	@JsonIgnore
	public void setPoisonUntil(int value) {
		statusValue("POISON", "until", value);
	}

	/** SLOW状态的until参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getSlowUntil() {
		return Math.toIntExact(statusValue("SLOW", "until"));
	}

	/** 在当前隔离状态上更新SLOW的until，不修改真实角色资产。 */
	@JsonIgnore
	public void setSlowUntil(int value) {
		statusValue("SLOW", "until", value);
	}

	/** SHIELD状态的amount参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getShield() {
		return Math.toIntExact(statusValue("SHIELD", "amount"));
	}

	/** 在当前隔离状态上更新SHIELD的amount，不修改真实角色资产。 */
	@JsonIgnore
	public void setShield(int value) {
		statusValue("SHIELD", "amount", value);
	}

	/** SHIELD状态的until参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getShieldUntil() {
		return Math.toIntExact(statusValue("SHIELD", "until"));
	}

	/** WALL状态的until参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getWallUntil() {
		return Math.toIntExact(statusValue("WALL", "until"));
	}

	/** 在当前隔离状态上更新WALL的until，不修改真实角色资产。 */
	@JsonIgnore
	public void setWallUntil(int value) {
		statusValue("WALL", "until", value);
	}

	/** WALL状态的percent参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getWallPercent() {
		return Math.toIntExact(statusValue("WALL", "percent"));
	}

	/** RENEW状态的amount参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getRenewAmount() {
		return Math.toIntExact(statusValue("RENEW", "amount"));
	}

	/** RENEW状态的until参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getRenewUntil() {
		return Math.toIntExact(statusValue("RENEW", "until"));
	}

	/** 在当前隔离状态上更新RENEW的until，不修改真实角色资产。 */
	@JsonIgnore
	public void setRenewUntil(int value) {
		statusValue("RENEW", "until", value);
	}

	/** RENEW状态的started参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getRenewStarted() {
		return Math.toIntExact(statusValue("RENEW", "started"));
	}

	/** MAGIC_GUARD状态的active参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public boolean isMagicGuard() {
		return statusValue("MAGIC_GUARD", "active") != 0;
	}

	/** 在当前隔离状态上更新MAGIC_GUARD的active，不修改真实角色资产。 */
	@JsonIgnore
	public void setMagicGuard(boolean value) {
		statusValue("MAGIC_GUARD", "active", value ? 1 : 0);
	}

	/** SET_SHIELD状态的amount参数；序列化仅使用统一statuses集合。 */
	@JsonIgnore
	public int getSetShield() {
		return Math.toIntExact(statusValue("SET_SHIELD", "amount"));
	}

	/** 在当前隔离状态上更新SET_SHIELD的amount，不修改真实角色资产。 */
	@JsonIgnore
	public void setSetShield(int value) {
		statusValue("SET_SHIELD", "amount", value);
	}

}
