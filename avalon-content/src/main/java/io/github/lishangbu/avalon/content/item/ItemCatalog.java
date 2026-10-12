package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemEquipment;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 所有物品的唯一模板目录。分类决定专属字段，场景限制不改变玩家库存的绑定及堆叠规则。 */
@Component
public class ItemCatalog {

	private static final String RESOURCE = "/content/items.json";

	private static final Set<String> STAT_KEYS = Set.of("health", "mana", "physicalAttack", "magicAttack",
			"physicalDefense", "magicDefense", "speed");

	private final Map<String, ItemItem> items;

	private final Map<String, ItemEquipment> equipment;

	private final List<ItemItem> all;

	@Autowired
	public ItemCatalog(WorldCatalog world) {
		this(ContentLoader.read(RESOURCE, ItemDefinition.class), world);
	}

	public ItemCatalog(ItemDefinition definition, WorldCatalog world) {
		validate(definition, world.definition());
		all = definition.items();
		items = ContentChecks.index(RESOURCE, "items.id", all, ItemItem::id);
		var gears = new LinkedHashMap<String, ItemEquipment>();
		all.stream().filter(i -> i.category() == ItemCategory.EQUIPMENT).forEach(i -> {
			var g = i.equipment();
			gears.put(i.id(), new ItemEquipment(i.id(), i.name(), g.slot(), g.level(), g.names(), g.stats(),
					g.professionStats(), g.quality(), g.profession()));
		});
		equipment = Collections.unmodifiableMap(gears);
	}

	public static void validate(ItemDefinition definition, WorldDefinition world) {
		Set<String> ids = new HashSet<>(), jobs = new HashSet<>(), ballTiers = new HashSet<>(),
				bookSkills = new HashSet<>();
		world.professions().forEach(p -> jobs.add(p.id()));
		for (var item : definition.items()) {
			ContentChecks.checkId(RESOURCE, "items.id", ids, item.id());
			if (item.name() == null || item.name().isBlank() || item.description() == null
					|| item.description().isBlank())
				throw invalid(item, "text", item.name(), "物品名称和说明不能为空");
			if (item.category() == null || item.scope() == null)
				throw invalid(item, "category/scope", item.category(), "必须显式填写分类及使用范围");
			boolean consumable = item.category() == ItemCategory.CONSUMABLE;
			if (consumable != (item.type() != null))
				throw invalid(item, "type", item.type(), "仅消耗品必须填写子类型");
			boolean recovery = item.type() == ItemConsumableType.POTION || item.type() == ItemConsumableType.FOOD;
			if (recovery != (item.recovery() != null))
				throw invalid(item, "recovery", item.recovery(), "仅药品和料理必须填写恢复效果");
			if ((item.category() == ItemCategory.TOOL) != (item.tool() != null)
					|| (item.category() == ItemCategory.SKILL_BOOK) != (item.book() != null)
					|| (item.category() == ItemCategory.EQUIPMENT) != (item.equipment() != null)
					|| (item.type() == ItemConsumableType.CAPTURE_BALL) != (item.capture() != null))
				throw invalid(item, "category", item.category(), "专属属性缺失或不适用于当前分类");
			if (item.task() && (item.category() != ItemCategory.MATERIAL && item.category() != ItemCategory.TOKEN))
				throw invalid(item, "task", true, "任务物品不能获得可执行的普通物品用途");
			if (item.scope() == ItemScope.ACADEMY
					&& (item.type() != ItemConsumableType.POTION || item.task() || item.sellPrice() != null))
				throw invalid(item, "scope", item.scope(), "学院专用物品仅作为课程药品，不进入回收或任务库存");
			if (item.sellPrice() != null && (item.sellPrice() < 1 || item.task()))
				throw invalid(item, "sellPrice", item.sellPrice(), "生活回收价必须为正整数且不能用于任务物品");
			if (recovery && (item.recovery().health() < 0 || item.recovery().mana() < 0
					|| item.recovery().health() == 0 && item.recovery().mana() == 0))
				throw invalid(item, "recovery", item.recovery(), "恢复量不能为负数或全部为零");
			if (item.tool() != null) {
				var tool = item.tool();
				if (!LifeSkillRules.validTool(tool.kind()) || tool.tier() < 0
						|| tool.tier() > LifeSkillRules.MAX_TOOL_TIER || tool.breakDenominator() < 1)
					throw invalid(item, "tool", tool, "工具类型、档位或报废概率无效");
			}
			if (item.capture() != null) {
				var ball = item.capture();
				if (ball.level() < 1 || ball.level() > GrowthRules.LEVEL_CAP || !Double.isFinite(ball.multiplier())
						|| ball.multiplier() <= 0 || !ballTiers.add(ball.reinforced() + ":" + ball.level()))
					throw invalid(item, "capture", ball, "封印球档位重复或数值无效");
			}
			if (item.book() != null && (item.book().skill() == null || item.book().skill().isBlank()
					|| !bookSkills.add(item.book().skill()) || item.book().level() < 1
					|| item.book().level() > GrowthRules.LEVEL_CAP))
				throw invalid(item, "book", item.book(), "技能书对应技能重复或学习等级无效");
			if (item.equipment() != null) {
				var gear = item.equipment();
				if (gear.level() < 1 || gear.level() > GrowthRules.LEVEL_CAP || gear.slot() == null
						|| !Set.of("WEAPON", "BODY", "FEET", "HEAD", "HANDS", "LEGS", "ACCESSORY").contains(gear.slot())
						|| !jobs.containsAll(gear.names().keySet())
						|| !jobs.containsAll(gear.professionStats().keySet()) || gear.quality() == null
						|| !Set.of("普通", "优秀", "精良", "史诗", "传说").contains(gear.quality())
						|| gear.profession() != null && !jobs.contains(gear.profession()))
					throw invalid(item, "equipment", gear, "装备属性、品质或职业限制无效");
				if (gear.names().values().stream().anyMatch(String::isBlank))
					throw invalid(item, "equipment.names", gear.names(), "职业装备名称不能为空");
				validateStats(item, "equipment.stats", gear.stats());
				gear.professionStats().forEach((job, stats) -> {
					validateStats(item, "equipment.professionStats." + job, stats);
					stats.forEach((key, value) -> Math.addExact(gear.stats().getOrDefault(key, 0), value));
				});
			}
		}
	}

	private static void validateStats(ItemItem item, String field, Map<String, Integer> stats) {
		if (!STAT_KEYS.containsAll(stats.keySet()) || stats.values().stream().anyMatch(n -> n < 0))
			throw invalid(item, field, stats, "装备属性名或数值无效");
	}

	private static IllegalArgumentException invalid(ItemItem item, String field, Object value, String reason) {
		return ContentChecks.invalid(RESOURCE, item.id(), field, value, reason);
	}

	/** 正式库存可持有的非装备物品；学院临时补给不属于普通物品入口。 */
	public boolean hasItem(String id) {
		var item = items.get(id);
		return item != null && item.category() != ItemCategory.EQUIPMENT && item.scope() == ItemScope.GENERAL;
	}

	public List<ItemItem> allItems() {
		return all;
	}

	public List<ItemItem> ofCategory(ItemCategory category) {
		return all.stream().filter(i -> i.category() == category).toList();
	}

	public List<ItemItem> consumables(ItemConsumableType type, ItemScope scope) {
		return all.stream().filter(i -> i.type() == type && i.scope() == scope).toList();
	}

	public ItemItem ordinaryReference(String resource, String entry, String field, String code) {
		var value = items.get(code);
		if (!hasItem(code) || value.task())
			throw ContentChecks.invalid(resource, entry, field, code, "必须引用存在的非任务普通物品");
		return value;
	}

	public boolean hasEquipment(String id) {
		return equipment.containsKey(id);
	}

	public Set<String> equipmentCodes() {
		return equipment.keySet();
	}

	public ItemItem item(String id) {
		return ContentChecks.required(RESOURCE, "items.id", id, items.get(id));
	}

	public ItemEquipment equipment(String id) {
		return ContentChecks.required(RESOURCE, "equipment.id", id, equipment.get(id));
	}

}
