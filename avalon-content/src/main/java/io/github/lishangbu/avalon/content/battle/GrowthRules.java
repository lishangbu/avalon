package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 试玩成长规则的唯一计算入口。当前上限对应数据库约束，扩级需连同迁移一起发布。
 */
@Component
public class GrowthRules {

	public static final int LEVEL_CAP = 50;

	private final List<Long> thresholds;

	public GrowthRules() {
		thresholds = ContentLoader.read("/content/experience.json", Definition.class).thresholds();
		if (thresholds.size() != LEVEL_CAP || thresholds.getFirst() != 0)
			throw new IllegalArgumentException("成长门槛须从零开始并覆盖当前开放等级");
		for (int index = 1; index < thresholds.size(); index++)
			if (thresholds.get(index) <= thresholds.get(index - 1))
				throw new IllegalArgumentException("成长门槛必须逐级递增");
	}

	public record Definition(List<Long> thresholds) {
		public Definition {
			thresholds = List.copyOf(thresholds);
		}
	}

	/**
	 * 累计经验门槛从正式内容读取；升级不清空累计值，人物与幻兽共用同一张表。
	 */
	public long threshold(int level) {
		if (level < 1 || level > LEVEL_CAP)
			throw new IllegalArgumentException("等级超出试玩范围");
		return thresholds.get(level - 1);
	}

	/**
	 * 从累计经验寻找开放范围内最高可达等级。门槛相等即升级，上限50级；负经验属于程序参数错误。该计算与人物奖励和伙伴初始化共用，不在各领域重复公式。
	 */
	public int level(long experience) {
		if (experience < 0)
			throw new IllegalArgumentException("经验不能为负");
		int level = 1;
		while (level < LEVEL_CAP && experience >= threshold(level + 1))
			level++;
		return level;
	}

	/**
	 * 满级不囤积额外经验；奖励参数在进入数据库写操作前检查。
	 */
	public long addExperience(long current, int amount) {
		if (current < 0 || amount < 0)
			throw new IllegalArgumentException("经验奖励参数无效");
		return Math.min(threshold(LEVEL_CAP), Math.addExact(current, amount));
	}

	/**
	 * 每达到十的整数倍等级增加一个携带位，不改变出战和替补数量。
	 */
	public int carryingCapacity(int characterLevel) {
		threshold(characterLevel);
		return 5 + characterLevel / 10;
	}

}
