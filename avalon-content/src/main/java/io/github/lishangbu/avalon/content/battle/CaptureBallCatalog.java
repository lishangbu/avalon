package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.item.ItemCatalog;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Comparator;

/** 球档位与倍率读取物品目录，普通目标超适用等级允许尝试并按公式衰减。 */
@Component
public class CaptureBallCatalog {

	private final List<Ball> balls;

	public CaptureBallCatalog(ItemCatalog items) {
		balls = items.consumables(ItemCatalog.ConsumableType.CAPTURE_BALL, ItemCatalog.Scope.GENERAL)
			.stream()
			.map(i -> new Ball(i.id(), i.capture().level(), i.capture().multiplier(), i.capture().reinforced()))
			.toList();
	}

	public Ball find(String code) {
		return balls.stream().filter(ball -> ball.code().equals(code)).findFirst().orElse(null);
	}

	public List<Ball> all() {
		return balls;
	}

	/** 战斗掉落沿用球类别，按野怪等级选择最低适用档；非封印球掉落原样返回。 */
	public String dropForLevel(String code, int level) {
		var original = find(code);
		if (original == null)
			return code;
		if (level < 1 || level > GrowthRules.LEVEL_CAP)
			throw new IllegalArgumentException("野怪等级超出开放范围");
		var family = balls.stream().filter(ball -> ball.reinforced() == original.reinforced()).toList();
		return family.stream()
			.filter(ball -> ball.level() >= Math.max(level, original.level()))
			.min(Comparator.comparingInt(Ball::level))
			.orElseGet(() -> family.stream().max(Comparator.comparingInt(Ball::level)).orElseThrow())
			.code();
	}

	public record Ball(String code, int level, double multiplier, boolean reinforced) {
	}

}
