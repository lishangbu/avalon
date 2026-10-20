package io.github.lishangbu.avalon.battle.application;

import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseViewBook;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseStateItem;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.creature.model.LearningBook;
import io.github.lishangbu.avalon.creature.domain.CreatureStorageRules;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRulesEntry;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 将课程领域状态转换为只读接口投影，避免领域规则层依赖应用层 DTO。 */
@Component
@RequiredArgsConstructor
public class WorkshopCourseProjector {

	private final LearningCatalog learning;

	public WorkshopCourseView project(long id, String course, String status, WorkshopCourseState state) {
		var available = new ArrayList<>(actions(state));
		if (status.equals("ACTIVE")) {
			if (state.getPhase().equals("DONE"))
				available.add("FINISH");
			available.add("EXIT");
		}
		var books = state.getBooks().entrySet().stream().map(entry -> {
			var book = learning.book(entry.getKey());
			String reason = null;
			try {
				SkillLearningRules.requireEligible(state.getSkills(), book.skill(),
						learning.pool("CLAW_CAT").contains(book), 7, book.level());
				if (entry.getValue() < 1)
					reason = "训练书已用完";
			}
			catch (BusinessException error) {
				reason = error.getMessage();
			}
			return new WorkshopCourseViewBook(entry.getKey(), book.skill(), entry.getValue(), reason == null, reason);
		}).toList();
		return new WorkshopCourseView(Long.toString(id), course, status, state.getRevision(), state.getPhase(),
				state.getMessage(), 7, state.getHp(), maxHp(state), state.getMp(), maxMp(state),
				List.copyOf(state.getSkills()),
				course.equals("WORKSHOP_SKILLS") && state.getProgress().contains("POOL_VIEWED")
						? learning.pool("CLAW_CAT").stream().map(LearningBook::skill).toList() : List.of(),
				books, List.copyOf(state.getItems()), List.copyOf(state.getPets()), List.copyOf(available),
				List.copyOf(state.getProgress()), state.getLearningResult(), BagCapacityRules.slots(entries(state)),
				BagCapacityRules.CAPACITY, CreatureStorageRules.STORAGE_CAPACITY,
				IntStream.rangeClosed(0, 7).mapToObj(SkillLearningRules::probabilities).toList());
	}

	private List<String> actions(WorkshopCourseState state) {
		return switch (state.getPhase()) {
			case "LEARNING" ->
				state.getProgress().contains("POOL_VIEWED") ? List.of("VIEW_POOL", "LEARN") : List.of("VIEW_POOL");
			case "EQUIPMENT" -> !state.getProgress().contains("EQUIPMENT_VIEWED") ? List.of("INSPECT_EQUIPMENT")
					: state.getProgress().contains("TEACHER_RESET") ? List.of("UNEQUIP")
							: state.getProgress().contains("EQUIPPED_B") ? List.of("EQUIP", "UNEQUIP", "RESET_VITALS")
									: List.of("EQUIP", "UNEQUIP");
			case "EQUIPMENT_DONE" -> List.of("START_BAG");
			case "BAG" -> List.of("USE_HEALTH", "CLAIM_MANA");
			case "STORAGE" -> List.of("DEPOSIT", "WITHDRAW", "LOCK");
			default -> List.of();
		};
	}

	private int maxHp(WorkshopCourseState state) {
		return state.getBaseHp() + state.getItems()
			.stream()
			.filter(WorkshopCourseStateItem::equipped)
			.mapToInt(WorkshopCourseStateItem::health)
			.sum();
	}

	private int maxMp(WorkshopCourseState state) {
		return state.getBaseMp() + state.getItems()
			.stream()
			.filter(WorkshopCourseStateItem::equipped)
			.mapToInt(WorkshopCourseStateItem::mana)
			.sum();
	}

	private List<BagCapacityRulesEntry> entries(WorkshopCourseState state) {
		return state.getItems()
			.stream()
			.map(item -> new BagCapacityRulesEntry(item.code(), item.equipment(), item.quantity(), item.equipment(),
					false))
			.toList();
	}

}
