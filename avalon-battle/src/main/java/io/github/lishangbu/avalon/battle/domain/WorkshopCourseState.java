package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** 战外课程的私有临时存档；所有编号只在此JSON内有效，永不解释为正式资产主键。 */
@Getter
@Setter
public class WorkshopCourseState {

	private String phase;

	private String message;

	private long revision;

	private int baseHp;

	private int baseMp;

	private int hp;

	private int mp;

	private List<String> skills = new ArrayList<>();

	private Map<String, Integer> books = new LinkedHashMap<>();

	private List<Item> items = new ArrayList<>();

	private List<Pet> pets = new ArrayList<>();

	private Set<String> progress = new LinkedHashSet<>();

	private SkillLearningRules.Result learningResult;

	/** 训练库存仍按正式背包的装备逐件、同物同绑定堆叠规则计算。 */
	public record Item(String id, String name, String code, int quantity, int pending, boolean equipment,
			boolean equipped, int requiredLevel, int health, int mana) {
		public Item equipped(boolean value) {
			return new Item(id, name, code, quantity, pending, equipment, value, requiredLevel, health, mana);
		}

		public Item stock(int available, int waiting) {
			return new Item(id, name, code, available, waiting, equipment, equipped, requiredLevel, health, mana);
		}
	}

	/** 个体身份、生命和魔力跨存取保持，不借仓库操作补血或创建副本。 */
	public record Pet(String id, String name, int position, boolean locked, int hp, int mp) {
		public Pet at(int value) {
			return new Pet(id, name, value, locked, hp, mp);
		}

		public Pet protectedPet() {
			return new Pet(id, name, position, true, hp, mp);
		}
	}

}
