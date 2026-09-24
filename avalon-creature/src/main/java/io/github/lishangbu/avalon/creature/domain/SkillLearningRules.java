package io.github.lishangbu.avalon.creature.domain;

import io.github.lishangbu.avalon.platform.BusinessException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 幻兽学习的纯领域规则：根据学习前技能数量抽取遗忘数量，再无放回抽取旧技能。 物种资格、等级、技能书库存和战斗状态由应用层在同一事务内检查；此处不访问数据库。
 * 普攻、防御和独立特性不应传入本规则，人物永久技能也不使用此规则。
 */
public final class SkillLearningRules {

	private SkillLearningRules() {
	}

	/** 正式学习与隔离练习共用资格；必须在扣书和任何随机抽样之前调用。 */
	public static void requireEligible(List<String> current, String skill, boolean inPool, int level,
			int requiredLevel) {
		if (!inPool)
			throw new BusinessException(409, "该物种不能学习此技能");
		if (level < requiredLevel)
			throw new BusinessException(409, "伙伴等级不足");
		if (current.contains(skill))
			throw new BusinessException(409, "伙伴已经学会此技能");
	}

	/**
	 * 返回“不遗忘、遗忘一个、两个、三个”的直接百分比，同时供学习确认页展示。 不是触发遗忘以后的条件概率；已有七技能时不存在不遗忘分支。
	 */
	public static List<Integer> probabilities(int count) {
		return switch (count) {
			case 0, 1, 2 -> List.of(100, 0, 0, 0);
			case 3 -> List.of(80, 20, 0, 0);
			case 4 -> List.of(60, 30, 10, 0);
			case 5 -> List.of(40, 35, 20, 5);
			case 6 -> List.of(20, 40, 25, 15);
			case 7 -> List.of(0, 45, 35, 20);
			default -> throw new IllegalArgumentException("已有技能数量必须介于零至七之间");
		};
	}

	/**
	 * 仅合格命令执行一次抽签。调用者必须保存结果与请求凭证，重试读取凭证而不能再次调用抽签。 新技能最后加入，所以不会在本次学习被遗忘；保留的旧技能维持原有显示顺序。
	 */
	public static Result learn(List<String> current, String skill, RandomGenerator random) {
		var chances = probabilities(current.size());
		if (new HashSet<>(current).size() != current.size()
				|| current.stream().anyMatch(value -> value == null || value.isBlank()))
			throw new IllegalArgumentException("已有技能必须是无重复的有效技能名单");
		if (skill == null || skill.isBlank())
			throw new IllegalArgumentException("新技能不能为空");
		if (current.contains(skill))
			throw new BusinessException(409, "伙伴已经学会此技能");

		int roll = random.nextInt(100);
		int forgottenCount = 0;
		while (roll >= chances.get(forgottenCount)) {
			roll -= chances.get(forgottenCount);
			forgottenCount++;
		}
		var remaining = new ArrayList<>(current);
		var forgotten = new ArrayList<String>();
		for (int index = 0; index < forgottenCount; index++)
			forgotten.add(remaining.remove(random.nextInt(remaining.size())));
		remaining.add(skill);
		return new Result(skill, List.copyOf(forgotten), List.copyOf(remaining));
	}

	/** 返回本次新增、确实遗忘和最终持有的技能，供事务持久化及结果页面使用。 */
	public record Result(String learned, List<String> forgotten, List<String> remaining) {
	}

}
