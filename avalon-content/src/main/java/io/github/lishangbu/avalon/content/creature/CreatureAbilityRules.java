package io.github.lishangbu.avalon.content.creature;

import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/** 赠送与野生幻兽共用的生成规则；槽位随个体保存，捕获、重试和进化不得重新抽取。 */
@Component
public class CreatureAbilityRules {

	private final AbilitySlotsCatalog slots;

	public CreatureAbilityRules(AbilitySlotsCatalog slots) {
		this.slots = slots;
	}

	/** 普通A、普通B、隐藏H分别占45%、45%、10%；两个普通槽可以映射同一个特性。 */
	public Selection draw(String species, RandomGenerator random) {
		int roll = random.nextInt(100);
		String slot = roll < 45 ? "A" : roll < 90 ? "B" : "H";
		String ability = slots.ability(species, slot);
		return new Selection(slot, ability);
	}

	public record Selection(String slot, String ability) {
	}

}
