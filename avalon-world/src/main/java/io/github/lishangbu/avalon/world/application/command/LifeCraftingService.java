package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.inventory.application.command.ProductionInventoryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.life.LifeProductionKernel;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingResult;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 即时逐件制造；工具全部报废或库存不足属于正常中断，异常则整批回滚。 */
@Component
@RequiredArgsConstructor
public class LifeCraftingService {

	private final CharacterCommandService characters;

	/** 制造任务读取真实产出事件，不能由购买、赠予或历史库存代替。 */
	private final io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService facts;

	private final LifeProductionKernel kernel;

	private final LifeOperationRepository receipts;

	private final LifeProductionCatalog catalog;

	private final InventoryQueryService inventory;

	private final ProductionInventoryService production;

	private final ObjectMapper json;

	@Transactional
	public LifeCraftingResult craft(long owner, LifeCraftingRequest request) {
		validate(request);
		var context = characters.lock(owner);
		var materials = new TreeMap<>(request.materials());
		String payload = json.writeValueAsString(
				new Payload("CRAFT", request.recipe(), request.quantity(), request.tool(), request.tools(), materials));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他生产操作");
			return json.readValue(prior.result(), LifeCraftingResult.class);
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		var recipe = catalog.recipe(request.recipe());
		if (recipe == null)
			throw new BusinessException(404, "配方不存在");
		if (!recipe.locations().contains(context.location()))
			throw new BusinessException(409, "请前往配方指定的制造设施");
		var skill = LifeSkillRulesSkill.valueOf(recipe.skill());
		var work = kernel.open(owner, context.id(), skill, recipe.level(), "专业熟练度不足");
		var owned = inventory.list(owner);
		if (recipe.blueprint() != null && owned.stream().noneMatch(item -> item.code().equals(recipe.blueprint())))
			throw new BusinessException(409, "请先取得并随身携带此配方的特殊图纸；图纸不会消耗");
		var byId = owned.stream().collect(Collectors.toMap(item -> item.id(), Function.identity()));
		var toolItem = byId.get(request.tool());
		if (toolItem == null)
			throw new BusinessException(409, "请选择持有的工具");
		work.selectTool(toolItem, catalog.minimumToolTier(recipe), request.tools(), "工具类型、档位或选定备用数量无效");
		var budget = new TreeMap<Long, Integer>();
		var codes = new TreeMap<Long, String>();
		for (var entry : materials.entrySet()) {
			var item = byId.get(entry.getKey());
			if (item == null)
				throw new BusinessException(409, "所选材料不存在");
			if (!recipe.ingredients().containsKey(item.code()) || item.quantity() < entry.getValue() || item.equipped()
					|| item.enhancementLevel() > 0)
				throw new BusinessException(409, "材料选择或数量无效");
			budget.put(Long.parseLong(item.id()), entry.getValue());
			codes.put(Long.parseLong(item.id()), item.code());
		}
		int completed = 0, broken = 0;
		String stop = "COMPLETED";
		for (int index = 0; index < request.quantity(); index++) {
			if (work.toolsRemaining() == 0) {
				stop = "TOOLS_EXHAUSTED";
				break;
			}
			var consumed = new TreeMap<Long, Integer>();
			for (var ingredient : recipe.ingredients().entrySet()) {
				int remaining = ingredient.getValue();
				for (var entry : budget.entrySet()) {
					if (!codes.get(entry.getKey()).equals(ingredient.getKey()))
						continue;
					int take = Math.min(remaining, entry.getValue());
					if (take > 0)
						consumed.put(entry.getKey(), take);
					remaining -= take;
				}
				if (remaining > 0) {
					stop = "MATERIALS_EXHAUSTED";
					break;
				}
			}
			if (!stop.equals("COMPLETED"))
				break;
			if (!production.exchange(owner, "LIFE_CRAFT:" + request.requestId() + ":" + index, consumed,
					recipe.product(), recipe.quantity())) {
				stop = "BAG_FULL";
				break;
			}
			consumed.forEach((id, count) -> budget.compute(id, (key, value) -> value - count));
			completed++;
			if (work.complete(recipe.level(), recipe.points()))
				broken++;
			// 与当前件的扣料、完整产物和熟练度同事务；请求重放在进入制造循环前返回。
			facts.record(owner, "CRAFT_SUCCESS:" + recipe.skill());
			facts.record(owner, "CRAFT_PRODUCT:" + recipe.product());
		}
		var result = new LifeCraftingResult(completed, completed * recipe.quantity(), broken, stop,
				work.halfPoints() / 2.0);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(result));
		return result;
	}

	private void validate(LifeCraftingRequest request) {
		if (request == null || request.recipe() == null || request.materials() == null || request.materials().isEmpty()
		// 单份采集奖励可各有来源编号；来源数上限按背包可容纳的单份物品计算，不等于制造次数。
				|| request.materials().size() > BagCapacityRules.CAPACITY * BagCapacityRules.STACK_LIMIT
				|| request.quantity() < 1 || request.quantity() > 99 || request.tools() < 1 || request.tools() > 99)
			throw new BusinessException(400, "制造数量、工具或材料选择无效");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId())
					|| Long.parseLong(request.tool()) <= 0)
				throw new IllegalArgumentException();
			for (var entry : request.materials().entrySet())
				if (Long.parseLong(entry.getKey()) <= 0 || entry.getValue() == null || entry.getValue() < 1
						|| entry.getValue() > 9999)
					throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "制造请求号或物品选择无效");
		}
	}

	private record Payload(String action, String recipe, int quantity, String tool, int tools,
			Map<String, Integer> materials) {
	}

}
