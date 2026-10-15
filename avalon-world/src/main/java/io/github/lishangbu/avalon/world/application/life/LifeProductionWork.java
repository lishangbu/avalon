package io.github.lishangbu.avalon.world.application.life;

import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;

/**
 * 单命令生产作业。活动先完成自己的扣料或产出，再通知成功；内核统一处理即时升级衰减和选定工具预算。
 * 播种只调用wearTool，收获只调用reward；作业不是持久化会话，不可跨事务或线程复用。
 */
public final class LifeProductionWork {

	private final long owner;

	private final long character;

	private final LifeSkillRulesSkill skill;

	private final LifeSkillRepository skills;

	private final LifeProductionCatalog catalog;

	private final InventoryCommandService items;

	private final LifeToolRandom random;

	private long halfPoints;

	private InventoryItemView selected;

	private LifeProductionTool tool;

	private int toolsRemaining;

	LifeProductionWork(long owner, long character, LifeSkillRulesSkill skill, long halfPoints,
			LifeSkillRepository skills, LifeProductionCatalog catalog, InventoryCommandService items,
			LifeToolRandom random) {
		this.owner = owner;
		this.character = character;
		this.skill = skill;
		this.halfPoints = halfPoints;
		this.skills = skills;
		this.catalog = catalog;
		this.items = items;
		this.random = random;
	}

	/** 只能选择一次；档位由活动目录决定，以保留升档工具配方的例外。 */
	public void selectTool(InventoryItemView item, int minimumTier, int quantity, String unavailableMessage) {
		if (selected != null)
			throw new IllegalStateException("生产作业不能更换选定工具");
		var definition = item == null ? null : catalog.tool(item.code());
		if (definition == null || !definition.kind().equals(skill.tool) || definition.tier() < minimumTier
				|| quantity < 1 || quantity > 99 || item.quantity() < quantity)
			throw new BusinessException(409, unavailableMessage);
		selected = item;
		tool = definition;
		toolsRemaining = quantity;
	}

	/** 成功产出后调用；先奖励熟练度，再判定本件使用的工具是否报废。 */
	public boolean complete(int contentLevel, int basePoints) {
		requireTool();
		reward(contentLevel, basePoints);
		return wearTool();
	}

	/** 延迟收获等无工具结算使用此入口；每件立即生效，后续奖励按升级后的等级衰减。 */
	public void reward(int contentLevel, int basePoints) {
		long next = LifeSkillRules.gain(halfPoints, contentLevel, basePoints);
		if (next != halfPoints) {
			skills.progress(character, skill, next);
			halfPoints = next;
		}
	}

	/** 播种等暂不增加熟练度的成功操作使用此入口；失败操作不得调用。 */
	public boolean wearTool() {
		requireTool();
		if (!random.breaks(tool.breakDenominator()))
			return false;
		items.consume(owner, Long.parseLong(selected.id()), selected.code(), 1);
		toolsRemaining--;
		return true;
	}

	private void requireTool() {
		if (selected == null || toolsRemaining == 0)
			throw new IllegalStateException("生产作业没有可用的选定工具");
	}

	public long halfPoints() {
		return halfPoints;
	}

	public int level() {
		return LifeSkillRules.level(halfPoints);
	}

	public int toolsRemaining() {
		return toolsRemaining;
	}

}
