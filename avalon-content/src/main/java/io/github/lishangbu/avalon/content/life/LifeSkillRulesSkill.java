package io.github.lishangbu.avalon.content.life;

/** LifeSkillRules 的独立公开配置模型；保留原字段及约束，调用方直接引用此类型。 */
public enum LifeSkillRulesSkill {

	MINING("采矿", "PICKAXE", LifeSkillRulesCategory.GATHERING), LOGGING("伐木", "AXE", LifeSkillRulesCategory.GATHERING),
	HERBALISM("采药", "PINCERS", LifeSkillRulesCategory.GATHERING),
	HUNTING("狩猎", "TRAP", LifeSkillRulesCategory.GATHERING),
	ARTISAN("工匠", "HAMMER", LifeSkillRulesCategory.MANUFACTURING),
	LEATHERWORK("皮匠", "NEEDLE", LifeSkillRulesCategory.MANUFACTURING),
	ALCHEMY("制药", "ROD", LifeSkillRulesCategory.MANUFACTURING),
	COOKING("烹饪", "POT", LifeSkillRulesCategory.MANUFACTURING), FARMING("种植", "HOE", LifeSkillRulesCategory.GENERAL),
	FISHING("钓鱼", "FISHING_ROD", LifeSkillRulesCategory.GENERAL);

	public final String title;

	public final String tool;

	public final boolean profession;

	public final LifeSkillRulesCategory category;

	LifeSkillRulesSkill(String title, String tool, LifeSkillRulesCategory category) {
		this.title = title;
		this.tool = tool;
		this.category = category;
		this.profession = category != LifeSkillRulesCategory.GENERAL;
	}

}
