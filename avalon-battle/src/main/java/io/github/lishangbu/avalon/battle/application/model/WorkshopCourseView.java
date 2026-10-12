package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.WorkshopCourseStateItem;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseStatePet;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRulesResult;
import java.util.List;

/** 明确区分临时课程与正式资产的只读投影；列表不含可转交正式库存接口的数字主键。 */
public record WorkshopCourseView(String id, String course, String status, long revision, String phase, String message,
		int level, int hp, int maxHp, int mp, int maxMp, List<String> skills, List<String> skillPool,
		List<WorkshopCourseViewBook> books, List<WorkshopCourseStateItem> items, List<WorkshopCourseStatePet> pets,
		List<String> actions, List<String> progress, SkillLearningRulesResult learningResult, int bagSlots,
		int bagCapacity, int storageCapacity, List<List<Integer>> forgetProbabilities) {

}
