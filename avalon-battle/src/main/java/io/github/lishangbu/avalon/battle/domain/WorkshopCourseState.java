package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.creature.domain.SkillLearningRulesResult;
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

	private List<WorkshopCourseStateItem> items = new ArrayList<>();

	private List<WorkshopCourseStatePet> pets = new ArrayList<>();

	private Set<String> progress = new LinkedHashSet<>();

	private SkillLearningRulesResult learningResult;

}
