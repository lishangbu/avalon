package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.FarmState;
import io.github.lishangbu.avalon.world.domain.FarmingRules;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.FarmRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 农田浇水和扩建使用角色锁与生产回执；同一农田从两个城镇操作也不能重复扣款。 */
@Component
@RequiredArgsConstructor
public class FarmCommandService {

	private final CharacterCommandService characters;

	private final FarmingCatalog catalog;

	private final FarmRepository farms;

	private final LifeSkillRepository skills;

	private final LifeOperationRepository receipts;

	private final LifeClock clock;

	private final ObjectMapper json;

	@Transactional
	public FarmState manage(long owner, Request request) {
		if (request == null || request.action() == null || request.plots() == null || request.plots().size() > 24)
			throw new BusinessException(400, "农田操作信息无效");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "请求号无效");
		}
		if (request.plots().stream().anyMatch(index -> index == null || index < 0 || index >= 24)
				|| request.plots().stream().distinct().count() != request.plots().size())
			throw new BusinessException(400, "农田位置无效或重复");
		var context = characters.lock(owner);
		String payload = "FARM:"
				+ json.writeValueAsString(new Payload(request.action(), request.plots().stream().sorted().toList()));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return json.readValue(prior.result(), FarmState.class);
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		if (!catalog.definition().locations().contains(context.location()))
			throw new BusinessException(409, "请到村庄广场或诺姆城农田入口");
		if (!skills.owned(context.id()).containsKey(Skill.FARMING))
			throw new BusinessException(409, "请先学习种植");
		var state = farms.find(context.id());
		var crops = new TreeMap<>(state.crops());
		int count = state.plots();
		switch (request.action()) {
			case "WATER" -> {
				if (request.plots().isEmpty())
					throw new BusinessException(400, "请选择要浇水的地块");
				for (int index : request.plots()) {
					var crop = crops.get(index);
					if (crop == null)
						throw new BusinessException(409, "所选地块没有作物");
					crops.put(index, new FarmState.Crop(crop.crop(), crop.bound(),
							FarmingRules.water(crop.wateredAt(), clock.now())));
				}
			}
			case "EXPAND" -> {
				if (!request.plots().isEmpty())
					throw new BusinessException(400, "扩建不需要选择地块");
				if (count >= catalog.definition().maximumPlots())
					throw new BusinessException(409, "农田已扩建到24格");
				characters.changeGold(owner, -catalog.definition().expansionGold());
				count++;
			}
			default -> throw new BusinessException(400, "未知农田操作");
		}
		var result = new FarmState(count, crops);
		farms.save(context.id(), result);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(result));
		return result;
	}

	public record Request(String requestId, String action, List<Integer> plots) {
	}

	private record Payload(String action, List<Integer> plots) {
	}

}
