package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.quest.model.QuestCondition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestGrant;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Avalon内置适配器：游戏资产、职业、地点和NPC的语义全部在此注册，任务核心不需要知道它们。 处理器同时提供校验和执行，新增类型可以另写Spring
 * bean，无须修改已有执行switch。
 */
@Configuration(proxyBeanMethods = false)
public class QuestBuiltinExtensions {

	/** 经验同时奖励人物和可参战伙伴，保留现有游戏的成长规则。 */
	@Bean
	QuestEffectHandler questExperience(CharacterCommandService characters, CreatureCommandService creatures) {
		return effect("EXPERIENCE", value -> noReference(value), (owner, grant) -> {
			int amount = Math.toIntExact(grant.effect().amount());
			characters.grantExperience(owner, amount);
			creatures.grantPartyExperience(owner, amount);
			return List.of("经验 +" + amount);
		});
	}

	@Bean
	QuestEffectHandler questGold(CharacterCommandService characters) {
		return effect("GOLD", value -> noReference(value), (owner, grant) -> {
			characters.changeGold(owner, grant.effect().amount());
			return List.of("金币 +" + grant.effect().amount());
		});
	}

	/** 普通物品复用库存的满包待领机制，来源编号由任务实例提供。 */
	@Bean
	QuestEffectHandler questGiveItem(ItemCatalog items, InventoryCommandService inventory) {
		return effect("GIVE_ITEM", value -> require(items.hasItem(value.reference()), "物品引用无效"), (owner, grant) -> {
			int amount = Math.toIntExact(grant.effect().amount());
			inventory.grant(owner, grant.source(), grant.effect().reference(), amount, false);
			return List.of(items.item(grant.effect().reference()).name() + " ×" + amount);
		});
	}

	/** 消耗也属于效果，必须与其他奖励和任务进度在同一事务内执行。 */
	@Bean
	QuestEffectHandler questTakeItem(ItemCatalog items, InventoryCommandService inventory) {
		return effect("TAKE_ITEM", value -> require(items.hasItem(value.reference()), "交付物品引用无效"), (owner, grant) -> {
			inventory.take(owner, grant.effect().reference(), Math.toIntExact(grant.effect().amount()));
			return List.of();
		});
	}

	@Bean
	QuestEffectHandler questGivePet(CreatureCatalog catalog, CreatureCommandService creatures) {
		return effect("GIVE_PET",
				value -> require(value.amount() == 1 && catalog.obtainableDirectly(value.reference()), "伙伴奖励引用或数量无效"),
				(owner, grant) -> {
					creatures.grant(owner, grant.source(), grant.effect().reference());
					return List.of(catalog.pet(grant.effect().reference()).name() + "加入队伍");
				});
	}

	/** 装备显示名按持锁角色的真实职业确定，不能由配置或客户端指定职业。 */
	@Bean
	QuestEffectHandler questGiveEquipment(ItemCatalog items, InventoryCommandService inventory,
			CharacterCommandService characters) {
		return effect("GIVE_EQUIPMENT",
				value -> require(value.amount() == 1 && items.hasEquipment(value.reference()), "装备奖励引用或数量无效"),
				(owner, grant) -> {
					inventory.grant(owner, grant.source(), grant.effect().reference(), 1, true);
					var gear = items.equipment(grant.effect().reference());
					return List.of(gear.names().getOrDefault(characters.lock(owner).profession(), gear.name()));
				});
	}

	/** 地点条件仅检查当前实际位置；不会传送角色，也不会改变任务实例。 */
	@Bean
	QuestConditionHandler questLocation(CharacterQueryService characters, WorldCatalog world) {
		return condition("LOCATION",
				value -> require(
						world.definition().places().stream().anyMatch(place -> place.id().equals(value.reference())),
						"任务地点引用无效"),
				(owner, value) -> characters.find(owner).location().equals(value.reference()));
	}

	@Bean
	QuestConditionHandler questReception(QuestCatalog catalog, CharacterFactQueryService facts) {
		return condition("RECEPTION", value -> require(catalog.step(value.reference()) != null, "任务人物步骤引用无效"), (owner,
				value) -> catalog.receptionAvailable(catalog.step(value.reference()), facts.codes(owner)::contains));
	}

	/** 现有编队及合并物品消耗资格沿用同一查询，旧内容不用重写配置。 */
	@Bean
	QuestConditionHandler questDelivery(QuestCatalog catalog, QuestDeliveryRequirements delivery) {
		return condition("DELIVERY", value -> require(catalog.step(value.reference()) != null, "任务交付步骤引用无效"),
				(owner, value) -> delivery.missing(owner, catalog.step(value.reference())) == null);
	}

	/** 跨任务前置及永久知识通过正式事实查询，无需在核心中硬编码字符串前缀。 */
	@Bean
	QuestConditionHandler questFact(CharacterFactQueryService facts) {
		return condition("FACT", value -> require(value.reference() != null && !value.reference().isBlank(), "事实条件为空"),
				(owner, value) -> facts.codes(owner).contains(value.reference()));
	}

	/** 新扩展效果的物品消耗在结算前合并检查，避免多项消耗单独满足却总量不足。 */
	@Bean
	QuestConditionHandler questInventory(ItemCatalog items, InventoryQueryService inventory) {
		return condition("INVENTORY",
				value -> require(items.hasItem(value.reference()) && value.amount() > 0, "库存条件引用或数量无效"),
				(owner, value) -> inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals(value.reference()))
					.mapToLong(item -> item.quantity())
					.sum() >= value.amount());
	}

	/**
	 * 接取补给资格在运行时使用当前实例的接取回执判定；此适配器仅供目录校验和旧内容降级检查。 核心不会把永久QUEST_ACCEPT事实当作下一轮已经领取补给。
	 */
	@Bean
	QuestConditionHandler questAcceptedStep(QuestCatalog catalog, CharacterFactQueryService facts) {
		return condition("ACCEPTED_STEP", value -> require(catalog.step(value.reference()) != null, "接取步骤引用无效"),
				(owner, value) -> facts.codes(owner).contains("QUEST_ACCEPT:" + value.reference()));
	}

	/** 注册器使用函数封装内置行为；第三方扩展仍可直接实现公开处理器接口。 */
	private QuestEffectHandler effect(String kind, Consumer<QuestEffect> validator,
			BiFunction<Long, QuestGrant, List<String>> execute) {
		return new QuestEffectHandler() {
			public String kind() {
				return kind;
			}

			public void validate(QuestEffect value) {
				// 游戏现有资产接口使用int数量；上线前拒绝超过接口可表达范围的配置。
				Math.toIntExact(value.amount());
				validator.accept(value);
			}

			public List<String> apply(long owner, QuestGrant grant) {
				return execute.apply(owner, grant);
			}
		};
	}

	private QuestConditionHandler condition(String kind, Consumer<QuestCondition> validator,
			BiFunction<Long, QuestCondition, Boolean> test) {
		return new QuestConditionHandler() {
			public String kind() {
				return kind;
			}

			public void validate(QuestCondition value) {
				validator.accept(value);
			}

			public boolean test(long owner, QuestCondition value) {
				return test.apply(owner, value);
			}
		};
	}

	private void noReference(QuestEffect effect) {
		require(effect.reference() == null, "此奖励不能携带引用");
	}

	private void require(boolean valid, String message) {
		if (!valid)
			throw new IllegalArgumentException(message);
	}

}
