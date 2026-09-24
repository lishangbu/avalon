package io.github.lishangbu.avalon.content.life;

import java.util.Arrays;
import java.util.List;

/** 生活成长独立于人物等级。以半点保存熟练度，低一级的奖励不丢失小数。 */
public final class LifeSkillRules {

	public static final int LEARNING_LEVEL = 6;

	public static final int PROFESSION_LIMIT = 3;

	public static final long MAX_HALF_POINTS = 130816;

	public static final int MAX_LEVEL = 10;

	public static final int LEVELS_PER_TOOL = 2;

	public static final int MAX_TOOL_TIER = MAX_LEVEL / LEVELS_PER_TOOL - 1;

	private LifeSkillRules() {
	}

	public enum Skill {

		MINING("采矿", "PICKAXE", Category.GATHERING), LOGGING("伐木", "AXE", Category.GATHERING),
		HERBALISM("采药", "PINCERS", Category.GATHERING), HUNTING("狩猎", "TRAP", Category.GATHERING),
		ARTISAN("工匠", "HAMMER", Category.MANUFACTURING), LEATHERWORK("皮匠", "NEEDLE", Category.MANUFACTURING),
		ALCHEMY("制药", "ROD", Category.MANUFACTURING), COOKING("烹饪", "POT", Category.MANUFACTURING),
		FARMING("种植", "HOE", Category.GENERAL), FISHING("钓鱼", "FISHING_ROD", Category.GENERAL);

		public final String title;

		public final String tool;

		public final boolean profession;

		public final Category category;

		Skill(String title, String tool, Category category) {
			this.title = title;
			this.tool = tool;
			this.category = category;
			this.profession = category != Category.GENERAL;
		}

	}

	public enum Category {

		GATHERING, MANUFACTURING, GENERAL

	}

	public static boolean belongsTo(String skill, Category category) {
		return skills().stream().anyMatch(value -> value.name().equals(skill) && value.category == category);
	}

	public static boolean validTool(String kind) {
		return skills().stream().anyMatch(value -> value.tool.equals(kind));
	}

	public static int firstToolLevel(int tier) {
		if (tier < 0 || tier > MAX_TOOL_TIER)
			throw new IllegalArgumentException("生活工具档位超出范围");
		return tier * LEVELS_PER_TOOL + 1;
	}

	public static int lastToolLevel(int tier) {
		return firstToolLevel(tier) + LEVELS_PER_TOOL - 1;
	}

	public static List<Skill> skills() {
		return List.copyOf(Arrays.asList(Skill.values()));
	}

	public static long threshold(int level) {
		if (level < 1 || level > MAX_LEVEL)
			throw new IllegalArgumentException("生活技能等级必须在1至10之间");
		return 128L * ((1L << (level - 1)) - 1);
	}

	public static int level(long halfPoints) {
		if (halfPoints < 0 || halfPoints > MAX_HALF_POINTS)
			throw new IllegalArgumentException("熟练度超出范围");
		int level = 1;
		while (level < MAX_LEVEL && halfPoints >= threshold(level + 1) * 2)
			level++;
		return level;
	}

	public static int toolTier(int level) {
		threshold(level);
		return (level - 1) / LEVELS_PER_TOOL;
	}

	/** 仅成功产出调用；配方等级高于当前技能时拒绝，不允许以奖励函数绕过资格。 */
	public static long gain(long current, int contentLevel, int basePoints) {
		threshold(contentLevel);
		int level = level(current);
		if (basePoints < 0 || contentLevel > level)
			throw new IllegalArgumentException("熟练度奖励或内容资格无效");
		int difference = level - contentLevel;
		long reward = difference == 0 ? basePoints * 2L : difference == 1 ? basePoints : 0;
		return Math.min(MAX_HALF_POINTS, current + reward);
	}

}
