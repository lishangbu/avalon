package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.creature.application.model.EvolutionTeachingRequest;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 一次性路线自选教学：角色锁、整套绑定材料及教学资格原子提交，不能换路线重复领取。 */
@Component
@RequiredArgsConstructor
public class EvolutionTeachingCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService queries;

	private final CharacterFactCommandService facts;

	private final EvolutionCatalog catalog;

	private final InventoryCommandService inventory;

	@Transactional
	public void learn(long owner, EvolutionTeachingRequest request) {
		if (request == null || !request.acceptBinding() || catalog.route(request.source()) == null)
			throw new BusinessException(400, "请选择教学路线并确认使用绑定材料会绑定伙伴");
		var context = characters.lock(owner);
		var recorded = queries.codes(owner);
		String choice = "EVOLUTION:ROUTE:" + request.source();
		if (recorded.contains("EVOLUTION:TUTORIAL")) {
			if (!recorded.contains(choice))
				throw new BusinessException(409, "教学奖励已经选择其他路线");
			return;
		}
		characters.requireOutsideDungeon(owner);
		if (context.level() < catalog.definition().playerLevel())
			throw new BusinessException(409, "人物等级尚未达到进化教学要求");
		if (!catalog.definition().locations().contains(context.location()))
			throw new BusinessException(409, "请到城镇进化研究设施学习");
		// 每种材料的来源固定，角色级路线事实进一步禁止另选路线领取；满包按普通任务奖励进入待领。
		catalog.route(request.source())
			.materials()
			.forEach((code, quantity) -> inventory.grantWithBinding(owner, "EVOLUTION:TEACHING:" + code, code, quantity,
					false, true));
		facts.record(owner, choice);
		facts.record(owner, "EVOLUTION:TUTORIAL");
	}

}
