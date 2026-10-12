package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.battle.model.CaptureBall;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** 球档位与倍率读取物品目录，普通目标超适用等级允许尝试并按公式衰减。 */
@Component
public class CaptureBallCatalog {

	private final List<CaptureBall> balls;

	public CaptureBallCatalog(ItemCatalog items) {
		balls = items.consumables(ItemConsumableType.CAPTURE_BALL, ItemScope.GENERAL)
			.stream()
			.map(i -> new CaptureBall(i.id(), i.capture().level(), i.capture().multiplier(), i.capture().reinforced()))
			.toList();
	}

	public CaptureBall find(String code) {
		return balls.stream().filter(ball -> ball.code().equals(code)).findFirst().orElse(null);
	}

	public List<CaptureBall> all() {
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
			.min(Comparator.comparingInt(CaptureBall::level))
			.orElseGet(() -> family.stream().max(Comparator.comparingInt(CaptureBall::level)).orElseThrow())
			.code();
	}

}
