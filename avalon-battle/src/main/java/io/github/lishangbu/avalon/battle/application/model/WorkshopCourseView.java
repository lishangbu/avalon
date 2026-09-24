package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState.Item;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState.Pet;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import java.util.List;

/** 明确区分临时课程与正式资产的只读投影；列表不含可转交正式库存接口的数字主键。 */
public record WorkshopCourseView(String id, String course, String status, long revision, String phase, String message,
		int level, int hp, int maxHp, int mp, int maxMp, List<String> skills, List<String> skillPool, List<Book> books,
		List<Item> items, List<Pet> pets, List<String> actions, List<String> progress,
		SkillLearningRules.Result learningResult, int bagSlots, int bagCapacity, int storageCapacity,
		List<List<Integer>> forgetProbabilities) {

	public record Book(String code, String skill, int quantity, boolean available, String reason) {
	}
}
