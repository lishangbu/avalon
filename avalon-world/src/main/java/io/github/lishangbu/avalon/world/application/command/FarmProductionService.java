package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.ProductionInventoryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.FarmProductionRequest;
import io.github.lishangbu.avalon.world.application.model.FarmProductionResult;
import io.github.lishangbu.avalon.world.application.model.FarmState;
import io.github.lishangbu.avalon.world.application.model.FarmStateCrop;
import io.github.lishangbu.avalon.world.domain.FarmingRules;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.FarmRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import jakarta.transaction.Transactional;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 逐格结算播种和收获，正常资源耗尽保留已完成地块，异常整批回滚。 */
@Component
@RequiredArgsConstructor
public class FarmProductionService {

	private final CharacterCommandService characters;

	private final FarmingCatalog catalog;

	private final LifeProductionCatalog tools;

	private final FarmRepository farms;

	private final LifeSkillRepository skills;

	private final LifeOperationRepository receipts;

	private final InventoryQueryService inventory;

	private final InventoryCommandService items;

	private final ProductionInventoryService production;

	private final LifeToolRandom random;

	private final LifeClock clock;

	private final ObjectMapper json;

	@Transactional
	public FarmProductionResult perform(long owner, FarmProductionRequest request) {
		validate(request);
		var context = characters.lock(owner);
		String payload = "FARM_PRODUCTION:" + json.writeValueAsString(
				new FarmProductionRequest(null, request.action(), request.plots().stream().sorted().toList(),
						request.crop(), request.seed(), request.tool(), request.tools()));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return json.readValue(prior.result(), FarmProductionResult.class);
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		if (!catalog.definition().locations().contains(context.location()))
			throw new BusinessException(409, "请到村庄广场或诺姆城农田入口");
		Long points = skills.owned(context.id()).get(LifeSkillRulesSkill.FARMING);
		if (points == null)
			throw new BusinessException(409, "请先学习种植");
		var farm = farms.find(context.id());
		var crops = new TreeMap<>(farm.crops());
		for (int index : request.plots()) {
			if (index >= farm.plots())
				throw new BusinessException(409, "所选地块尚未扩建");
			if (request.action().equals("PLANT")) {
				if (crops.containsKey(index))
					throw new BusinessException(409, "所选地块已有作物");
			}
			else {
				var planted = crops.get(index);
				if (planted == null || !FarmingRules.mature(planted.wateredAt(),
						catalog.crop(planted.crop()).growthSeconds(), clock.now()))
					throw new BusinessException(409, "所选作物尚未成熟");
			}
		}
		int completed = 0, produced = 0, broken = 0;
		String stop = "COMPLETED";
		if (request.action().equals("PLANT")) {
			var crop = catalog.crop(request.crop());
			if (crop == null || LifeSkillRules.level(points) < crop.level())
				throw new BusinessException(409, "种植熟练度不足或作物无效");
			var owned = inventory.list(owner);
			var seed = owned.stream()
				.filter(item -> item.id().equals(request.seed()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(409, "请选择持有的种子"));
			var toolItem = owned.stream()
				.filter(item -> item.id().equals(request.tool()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(409, "请选择持有的锄头"));
			var tool = tools.tool(toolItem.code());
			if (!seed.code().equals(crop.seed()) || tool == null
					|| !tool.kind().equals(LifeSkillRulesSkill.FARMING.tool)
					|| tool.tier() < LifeSkillRules.toolTier(crop.level()) || toolItem.quantity() < request.tools())
				throw new BusinessException(409, "种子、工具档位或备用数量无效");
			for (int index : request.plots().stream().sorted().toList()) {
				if (broken == request.tools()) {
					stop = "TOOLS_EXHAUSTED";
					break;
				}
				if (completed == seed.quantity()) {
					stop = "SEEDS_EXHAUSTED";
					break;
				}
				items.consume(owner, Long.parseLong(seed.id()), seed.code(), 1);
				crops.put(index, new FarmStateCrop(crop.id(), seed.bound(), null));
				completed++;
				if (random.breaks(tool.breakDenominator())) {
					items.consume(owner, Long.parseLong(toolItem.id()), toolItem.code(), 1);
					broken++;
				}
			}
		}
		else {
			for (int index : request.plots().stream().sorted().toList()) {
				var planted = crops.get(index);
				var crop = catalog.crop(planted.crop());
				if (!production.harvest(owner, "FARM_HARVEST:" + request.requestId() + ":" + index, crop.product(),
						crop.quantity(), planted.bound())) {
					stop = "BAG_FULL";
					break;
				}
				// 遗忘重学不会销毁已种作物，也不能利用高等级旧作物跳过熟练度。
				if (LifeSkillRules.level(points) >= crop.level())
					points = LifeSkillRules.gain(points, crop.level(), crop.points());
				crops.remove(index);
				completed++;
				produced += crop.quantity();
			}
			skills.progress(context.id(), LifeSkillRulesSkill.FARMING, points);
		}
		farms.save(context.id(), new FarmState(farm.plots(), crops));
		var result = new FarmProductionResult(completed, produced, broken, stop, points / 2.0);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(result));
		return result;
	}

	private void validate(FarmProductionRequest request) {
		if (request == null || !("PLANT".equals(request.action()) || "HARVEST".equals(request.action()))
				|| request.plots() == null || request.plots().isEmpty() || request.plots().size() > 24
				|| request.plots().stream().anyMatch(index -> index == null || index < 0 || index >= 24)
				|| request.plots().stream().distinct().count() != request.plots().size())
			throw new BusinessException(400, "农田操作或地块选择无效");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
			if (request.action().equals("PLANT") && (request.crop() == null || Long.parseLong(request.seed()) <= 0
					|| Long.parseLong(request.tool()) <= 0 || request.tools() < 1 || request.tools() > 99))
				throw new IllegalArgumentException();
			if (request.action().equals("HARVEST") && (request.crop() != null || request.seed() != null
					|| request.tool() != null || request.tools() != 0))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "请求号或种植材料选择无效");
		}
	}

}
