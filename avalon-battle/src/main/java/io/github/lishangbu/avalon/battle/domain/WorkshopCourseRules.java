package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState.Item;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState.Pet;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.creature.domain.CreatureStorageRules;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.EquipmentRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 有限的课内操作；学习、背包容量、穿戴资格和仓库位置直接复用正式纯规则，不碰正式仓储。 */
@Component
@RequiredArgsConstructor
public class WorkshopCourseRules {

	private final LearningCatalog learning;

	private final BattleRules battle;

	public WorkshopCourseState create(String course, String profession) {
		var state = new WorkshopCourseState();
		// 课程基线包含7级教学基础装备预算，与玩家自己的装备完全无关。
		var stats = battle.stats(profession, 7, true, Map.of("health", 40));
		state.setBaseHp(stats.health());
		state.setBaseMp(stats.mana());
		state.setHp(stats.health() - 40);
		state.setMp(stats.mana() - 12);
		if (course.equals("BQ021")) {
			state.setPhase("LEARNING");
			state.setSkills(new ArrayList<>(List.of("水弹", "微疗", "惩击")));
			for (String code : List.of("BOOK_LIGHT_SCREEN", "BOOK_ROCK", "BOOK_WATER"))
				state.getBooks().put(code, 1);
			state.setMessage("先查看利爪猫技能池，再选择一本训练书；池外和已学技能不会扣书或抽签。");
		}
		else if (course.equals("BQ022")) {
			state.setPhase("EQUIPMENT");
			state.getItems().add(new Item("A", "练习布衣A", "TRAINING_BODY_A", 1, 0, true, false, 1, 0, 0));
			state.getItems().add(new Item("B", "练习外衣B", "TRAINING_BODY_B", 1, 0, true, false, 7, 60, 20));
			state.setMessage("先查看两件临时装备的需求与属性，再穿外衣B，观察增加上限不会治疗。");
		}
		else
			throw new BusinessException(400, "未知战外课程");
		return state;
	}

	/** 资格失败在任何扣书、抽样或资源变化前拒绝，应用层将成功状态和请求回执共同持久化。 */
	public void apply(WorkshopCourseState s, String type, String target, Integer position, RandomGenerator random) {
		if (!actions(s).contains(type))
			throw new BusinessException(409, "当前课程阶段不能执行该操作");
		switch (type) {
			case "VIEW_POOL" -> {
				s.getProgress().add("POOL_VIEWED");
				s.setMessage("技能池已查看；请选择可学习的训练光幕书。");
			}
			case "LEARN" -> learn(s, target, random);
			case "INSPECT_EQUIPMENT" -> {
				s.getProgress().add("EQUIPMENT_VIEWED");
				s.setMessage("布衣A需求1级；外衣B需求7级，增加60生命和20魔力上限，不增加当前资源。");
			}
			case "EQUIP" -> equip(s, target);
			case "RESET_VITALS" -> {
				if (s.getProgress().contains("TEACHER_RESET") || !item(s, "B").equipped())
					throw new BusinessException(409, "请先穿戴外衣B，本次教员重置只执行一次");
				s.setHp(maxHp(s));
				s.setMp(maxMp(s));
				s.getProgress().add("TEACHER_RESET");
				s.setMessage("教员明确重置：仅把临时资源恢复至当前上限。这不是换装治疗，请手动卸下外衣B。");
			}
			case "UNEQUIP" -> unequip(s);
			case "START_BAG" -> startBag(s);
			case "USE_HEALTH" -> {
				var potion = item(s, "HP");
				if (potion.quantity() != 1)
					throw new BusinessException(409, "训练生命药已经使用");
				replace(s, potion.stock(0, 0));
				s.setHp(Math.min(maxHp(s), s.getHp() + 120));
				s.getProgress().add("POTION_USED");
				s.setMessage("训练生命药恢复120生命并空出一格；真实角色资源未变。");
			}
			case "CLAIM_MANA" -> claim(s);
			case "DEPOSIT" -> deposit(s, target);
			case "WITHDRAW" -> withdraw(s, target, position);
			case "LOCK" -> protect(s, target);
			default -> throw new BusinessException(400, "课程操作无效");
		}
	}

	private void learn(WorkshopCourseState s, String code, RandomGenerator random) {
		var book = learning.book(code);
		if (book == null || s.getBooks().getOrDefault(code, 0) < 1)
			throw new BusinessException(409, "训练技能书不存在或已用完");
		SkillLearningRules.requireEligible(s.getSkills(), book.skill(), learning.pool("CLAW_CAT").contains(book), 7,
				book.level());
		var result = SkillLearningRules.learn(s.getSkills(), book.skill(), random);
		s.getBooks().put(code, 0);
		s.setSkills(new ArrayList<>(result.remaining()));
		s.setLearningResult(result);
		s.getProgress().add("LEARNED");
		s.setPhase("DONE");
		s.setMessage("本次学习结果已保存；未遗忘和随机遗忘均通过，技能槽位仍为A。确认后结束课程。");
	}

	private void equip(WorkshopCourseState s, String id) {
		var chosen = item(s, id);
		EquipmentRules.requireWearable(chosen.equipment() ? "BODY" : null, chosen.quantity(), chosen.requiredLevel(),
				7);
		for (var old : List.copyOf(s.getItems()))
			replace(s, old.equipped(old.id().equals(id)));
		clamp(s);
		if (id.equals("B"))
			s.getProgress().add("EQUIPPED_B");
		s.setMessage("穿戴已完成；当前生命和魔力只向下截断，不因上限提高而增加。");
	}

	private void unequip(WorkshopCourseState s) {
		boolean wasB = item(s, "B").equipped();
		for (var old : List.copyOf(s.getItems()))
			replace(s, old.equipped(false));
		clamp(s);
		if (wasB && s.getProgress().contains("TEACHER_RESET")) {
			s.getProgress().add("UNEQUIP_CLAMPED");
			s.setPhase("EQUIPMENT_DONE");
		}
		s.setMessage("卸装后的资源已按新上限截断；完成教员重置后的卸装即可进入库存练习。");
	}

	private void startBag(WorkshopCourseState s) {
		s.getItems().clear();
		s.getItems().add(new Item("HP", "训练生命药I", "HEALTH_POTION", 1, 0, false, false, 1, 0, 0));
		for (int i = 1; i <= 59; i++)
			s.getItems().add(new Item("FILLER_" + i, "借用装备" + i, "TRAINING_FILLER", 1, 0, true, false, 1, 0, 0));
		s.getItems().add(new Item("MP", "训练魔力药I", "MANA_POTION", 0, 1, false, false, 1, 0, 0));
		s.setHp(s.getBaseHp() - 120);
		s.setMp(s.getBaseMp());
		String[] ids = { "T_CAT", "T_STONE", "T_SPROUT" };
		String[] species = { "CLAW_CAT", "STONE_GOLEM", "SPROUT" };
		String[] names = { "练习利爪猫", "练习小岩怪", "练习芽灵" };
		for (int i = 0; i < ids.length; i++) {
			var stats = battle.stats(species[i], 7, false, Map.of());
			s.getPets().add(new Pet(ids[i], names[i], i, false, stats.health() - 10, stats.mana() - 2));
		}
		s.setPhase("BAG");
		s.setMessage("新库存练习：60格已满，1瓶魔力药待领。使用训练生命药空出一格，再领取待领。");
	}

	private void claim(WorkshopCourseState s) {
		var mana = item(s, "MP");
		if (mana.pending() != 1)
			throw new BusinessException(409, "该训练待领已经领取");
		if (BagCapacityRules.fits(entries(s), new BagCapacityRules.Entry(mana.code(), false, 1, false, false)) != 1)
			throw new BusinessException(409, "背包空间不足，训练待领仍保留");
		replace(s, mana.stock(1, 0));
		s.getProgress().add("PENDING_CLAIMED");
		s.setPhase("STORAGE");
		s.setMessage("待领已装入空格；现在将替补芽灵存仓，再取回空替补位，最后手动锁定。");
	}

	private void deposit(WorkshopCourseState s, String id) {
		var pet = pet(s, id);
		int position = CreatureStorageRules.deposit(pet.position(),
				s.getPets().stream().map(Pet::position).collect(Collectors.toSet()));
		replacePet(s, pet.at(position));
		if (id.equals("T_SPROUT"))
			s.getProgress().add("PET_STORED");
		s.setMessage("同一个训练个体已存仓，身份与资源保持原值；存仓时不属于参战经验名单。");
	}

	private void withdraw(WorkshopCourseState s, String id, Integer position) {
		var pet = pet(s, id);
		if (position == null || position < 2 || position > 3)
			throw new BusinessException(400, "本课请取回空替补位2或3");
		CreatureStorageRules.requirePosition(pet.position(), position, 5, 7, 7, true);
		if (pet.position() < 100 || s.getPets().stream().anyMatch(other -> other.position() == position))
			throw new BusinessException(409, "请从仓库取回到空替补位");
		replacePet(s, pet.at(position));
		if (id.equals("T_SPROUT") && s.getProgress().contains("PET_STORED"))
			s.getProgress().add("PET_WITHDRAWN");
		s.setMessage("取回的是同一只训练芽灵，没有治疗、重复图鉴或复制个体。");
	}

	private void protect(WorkshopCourseState s, String id) {
		var pet = pet(s, id);
		if (!id.equals("T_SPROUT") || !s.getProgress().contains("PET_WITHDRAWN") || pet.position() >= 100)
			throw new BusinessException(409, "请先完成芽灵存仓与取回，再设置保护");
		replacePet(s, pet.protectedPet());
		s.getProgress().add("PET_LOCKED");
		s.setPhase("DONE");
		s.setMessage("保护已开启：锁定伙伴不可直接上交、放归或寄售。所有练习完成，不要求实际放归。");
	}

	public List<String> actions(WorkshopCourseState s) {
		return switch (s.getPhase()) {
			case "LEARNING" ->
				s.getProgress().contains("POOL_VIEWED") ? List.of("VIEW_POOL", "LEARN") : List.of("VIEW_POOL");
			case "EQUIPMENT" -> !s.getProgress().contains("EQUIPMENT_VIEWED") ? List.of("INSPECT_EQUIPMENT")
					: s.getProgress().contains("TEACHER_RESET") ? List.of("UNEQUIP")
							: s.getProgress().contains("EQUIPPED_B") ? List.of("EQUIP", "UNEQUIP", "RESET_VITALS")
									: List.of("EQUIP", "UNEQUIP");
			case "EQUIPMENT_DONE" -> List.of("START_BAG");
			case "BAG" -> List.of("USE_HEALTH", "CLAIM_MANA");
			case "STORAGE" -> List.of("DEPOSIT", "WITHDRAW", "LOCK");
			default -> List.of();
		};
	}

	/** 出课即销毁可操作库存与个体，只保留学习结果和完成标记供审计与刷新结果页。 */
	public void discard(WorkshopCourseState s, boolean passed) {
		s.getItems().clear();
		s.getPets().clear();
		s.getBooks().clear();
		s.getSkills().clear();
		clamp(s);
		s.setPhase(passed ? "PASSED" : "EXITED");
		s.setMessage(passed ? "课程通过；临时资源已归还，仅记录课程事实，不发任务经验。" : "已退出课程，临时资源已全部归还。");
	}

	private int maxHp(WorkshopCourseState s) {
		return s.getBaseHp() + s.getItems().stream().filter(Item::equipped).mapToInt(Item::health).sum();
	}

	private int maxMp(WorkshopCourseState s) {
		return s.getBaseMp() + s.getItems().stream().filter(Item::equipped).mapToInt(Item::mana).sum();
	}

	private void clamp(WorkshopCourseState s) {
		s.setHp(EquipmentResourceRules.clamp(s.getHp(), maxHp(s)));
		s.setMp(EquipmentResourceRules.clamp(s.getMp(), maxMp(s)));
	}

	private List<BagCapacityRules.Entry> entries(WorkshopCourseState s) {
		return s.getItems()
			.stream()
			.map(item -> new BagCapacityRules.Entry(item.code(), item.equipment(), item.quantity(), item.equipment(),
					false))
			.toList();
	}

	private Item item(WorkshopCourseState s, String id) {
		return s.getItems()
			.stream()
			.filter(item -> item.id().equals(id))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "训练物品不存在"));
	}

	private Pet pet(WorkshopCourseState s, String id) {
		return s.getPets()
			.stream()
			.filter(pet -> pet.id().equals(id))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "训练伙伴不存在"));
	}

	private void replace(WorkshopCourseState s, Item value) {
		s.getItems().replaceAll(item -> item.id().equals(value.id()) ? value : item);
	}

	private void replacePet(WorkshopCourseState s, Pet value) {
		s.getPets().replaceAll(pet -> pet.id().equals(value.id()) ? value : pet);
	}

}
