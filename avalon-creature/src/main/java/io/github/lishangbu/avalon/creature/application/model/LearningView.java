package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/** 学习确认页只展示服务端推导的资格与库存；概率为不遗忘、一、二、三个的直接百分比。 */
public record LearningView(String creatureId, List<String> current, List<Integer> probabilities,
		List<LearningViewOption> options) {

}
