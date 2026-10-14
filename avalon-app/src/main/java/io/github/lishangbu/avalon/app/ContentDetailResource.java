package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.ItemDescriptions;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开用途的只读详情，不提供物种槽位映射，不接受客户端提交效果或奖励。 */
@RestController
@RequestMapping("/api/content/details")
@RequiredArgsConstructor
public class ContentDetailResource {

	private final PotionRecoveryRules potions;

	private final ItemDescriptions itemDescriptions;

	private final ItemCatalog items;

	private final BattleCatalog battles;

	private final CaptureBallCatalog balls;

	private final CreatureCatalog creatureCatalog;

	private final AbilitySlotsCatalog abilitySlots;

	private final LearningCatalog learning;

	private final CharacterQueryService characters;

	private final CurrentAccount identity;

	private final CreatureQueryService creatures;

	private final SpeciesStatsCatalog speciesStats;

	@GetMapping("/{kind}/{code}")
	public ContentDetailDetail detail(@PathVariable String kind, @PathVariable String code) {
		var rules = new ArrayList<String>();
		if ("creature".equals(kind)) {
			// 个体详情只能读取本人当前持有的幻兽，不能靠猜测雪花编号查看野生隐藏槽位。
			var pet = creatures.list(identity.id())
				.stream()
				.filter(value -> value.id().equals(code))
				.findFirst()
				.orElseThrow(() -> new BusinessException(404, "伙伴不存在"));
			var stats = speciesStats.atLevel(pet.species(), pet.level());
			rules.add("生命上限：" + stats.health());
			rules.add("魔力上限：" + stats.mana());
			rules.add("物理攻击：" + stats.physicalAttack());
			rules.add("法术攻击：" + stats.magicAttack());
			rules.add("物理防御：" + stats.physicalDefense());
			rules.add("法术防御：" + stats.magicDefense());
			rules.add("基础速度：" + stats.speed());
			return new ContentDetailDetail(pet.name(), creatureCatalog.pet(pet.species()).description(), rules);
		}
		if ("ability".equals(kind)) {
			var text = abilitySlots.description(code);
			if (text == null)
				throw new BusinessException(404, "特性不存在");
			return new ContentDetailDetail(code, text, List.of());
		}
		if ("skill".equals(kind)) {
			var skill = battles.skill(code);
			if (skill == null)
				throw new BusinessException(404, "技能不存在");
			rules.add("消耗魔力：" + skill.mana());
			rules.add(skill.cooldown() == 0 ? "无冷却" : "使用后冷却" + skill.cooldown() + "个完整回合");
			if (skill.element() != null)
				rules.add("属性：" + skill.element());
			rules.add("目标：" + switch (skill.target()) {
				case "SELF" -> "自身";
				case "ALLY" -> "一名存活友方";
				case "OTHER" -> "另一名存活友方";
				case "ENEMIES" -> "手动选择最多三名敌人";
				default -> "一名敌人";
			});
			switch (skill.primaryKind()) {
				case "DAMAGE", "AREA", "SLOW" -> rules.add("威力：" + skill.power());
				case "HEAL" -> rules.add("基础治疗量：施放者法攻的" + skill.power() + "%");
				case "WALL" -> rules.add("直接伤害降低" + skill.power() + "%");
				case "RENEW" -> rules.add("每回合基础治疗量：施放时法攻的" + skill.power() + "%");
				case "MARK" -> rules.add("直接伤害提高" + skill.power() + "%");
				case "MAGIC_GUARD" -> rules.add("本回合法术直接伤害降低15%");
				case "SHIELD" -> rules.add("护盾吸收量：施放者法攻的200%");
				default -> {
				}
			}
			if (skill.lowHealthPower() != null)
				rules.add("命中时目标生命不高于" + skill.lowHealthPower().percent() + "%时，威力为" + skill.lowHealthPower().power());
			if ("SLOW".equals(skill.primaryKind()))
				rules.add("目标速度降低20%，持续本回合及随后两回合；本回合已经确定的行动顺序不重排");
			return new ContentDetailDetail(code, battles.skill(code).description(), rules);
		}
		if (!"item".equals(kind))
			throw new BusinessException(404, "详情类型不存在");
		if (items.hasEquipment(code)) {
			var equipment = items.equipment(code);
			var character = characters.find(identity.id());
			String profession = character == null ? "" : character.profession();
			var stats = new LinkedHashMap<>(equipment.stats());
			equipment.professionStats()
				.getOrDefault(profession, Map.of())
				.forEach((key, value) -> stats.merge(key, value, Integer::sum));
			var labels = Map.of("health", "生命", "mana", "魔力", "physicalAttack", "物理攻击", "magicAttack", "法术攻击",
					"physicalDefense", "物理防御", "magicDefense", "法术防御", "speed", "速度");
			rules.add("穿戴等级：" + equipment.level());
			stats.forEach((key, value) -> rules.add("基础" + labels.get(key) + "：+" + value));
			return new ContentDetailDetail(equipment.names().getOrDefault(profession, equipment.name()),
					"穿戴后提供装备属性。以下为适用当前职业的未强化基础属性；已有装备的强化、职业限制、绑定与回收价格以实例详情为准。", rules);
		}
		if (!items.hasItem(code))
			throw new BusinessException(404, "道具不存在");
		var item = items.item(code);
		var ball = balls.find(code);
		var book = learning.book(code);
		if (book != null) {
			rules.add("学习技能：" + book.skill() + "；幻兽至少达到" + book.level() + "级，且物种学习池包含此技能");
			rules.add("学习消耗一本技能书；可能随机遗忘旧技能，技能越多风险越高，最多保留七个技能");
		}
		if (potions.usablePotions().contains(code))
			rules.add("战斗中对存活友方使用并消耗一瓶；不能复活，超过上限的恢复量不保留");
		if (ball != null) {
			rules.add("适用目标等级上限：" + ball.level() + "级；超出仍可尝试，但成功率衰减");
			rules.add("封印球捕捉倍率：" + ball.multiplier());
			rules.add("仅用于允许捕捉的野外战斗；尝试消耗一枚，失败不返还");
			rules.add("成功率同时受物种基础捕捉率、剩余生命、减速状态与人物等级差影响");
		}
		if (item.task())
			rules.add("任务物品，由对应任务或现场行动使用；不占普通背包格");
		return new ContentDetailDetail(item.name(), itemDescriptions.description(code), rules);
	}

}
