package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.creature.application.model.CreatureResources;
import io.github.lishangbu.avalon.creature.application.port.CreatureResourceCommand;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.ProductionInventoryService;
import io.github.lishangbu.avalon.inventory.application.model.ProductionInventoryProduct;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.life.LifeProductionKernel;
import io.github.lishangbu.avalon.world.application.model.GatheringBeat;
import io.github.lishangbu.avalon.world.application.model.GatheringSession;
import io.github.lishangbu.avalon.world.application.model.GatheringStart;
import io.github.lishangbu.avalon.world.application.model.GatheringStop;
import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import io.github.lishangbu.avalon.world.domain.GatheringSupplyRules;
import io.github.lishangbu.avalon.world.domain.GatheringSupplyRulesSupply;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.persistence.repository.GatheringSessionRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 角色锁串行化每轮采集，心跳版本与产物一起提交；客户端不提交经过时长或奖励数量。 */
@Component
@RequiredArgsConstructor
public class GatheringCommandService {

	private final PotionRecoveryRules potions;

	private final GatheringRareRoll rareRoll;

	private final CharacterCommandService characters;

	private final CreatureQueryService creatures;

	private final CreatureResources resources;

	private final CreatureResourceCommand resourceCommands;

	private final InventoryQueryService inventory;

	private final ProductionInventoryService production;

	private final LifeProductionKernel kernel;

	private final GatheringSessionRepository sessions;

	private final LifeOperationRepository receipts;

	private final GatheringCatalog catalog;

	private final LifeClock clock;

	private final ObjectMapper json;

	@Transactional
	public GatheringSession begin(long owner, GatheringStart request) {
		if (request == null || request.node() == null || request.tools() < 1 || request.tools() > 99)
			throw new BusinessException(400, "请选择资源点、伙伴与工具");
		uuid(request.requestId());
		long petId = number(request.creature()), toolId = number(request.tool());
		var context = characters.lock(owner);
		String payload = "GATHER_START:"
				+ json.writeValueAsString(new GatheringStart(request.requestId(), request.node(), request.creature(),
						request.tool(), request.tools(), new TreeMap<>(request.supplies())));
		var previous = receipts.prior(context.id(), request.requestId());
		if (previous != null) {
			if (!previous.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return json.readValue(previous.result(), GatheringSession.class);
		}
		characters.requireOutsideDungeon(owner);
		var node = catalog.node(request.node());
		if (node == null || !node.location().equals(context.location()))
			throw new BusinessException(409, "请到资源点所在的野外地点");
		var work = kernel.open(owner, context.id(), LifeSkillRulesSkill.valueOf(node.skill()), node.level(),
				"尚未学习或采集熟练度不足");
		var pet = creatures.list(owner)
			.stream()
			.filter(value -> value.id().equals(Long.toString(petId)))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "伙伴不存在"));
		if (pet.partyPosition() >= 100 || pet.level() > context.level())
			throw new BusinessException(409, "请选择随身且等级不高于人物的伙伴");
		var item = inventory.list(owner)
			.stream()
			.filter(value -> value.id().equals(Long.toString(toolId)))
			.findFirst()
			.orElseThrow(() -> new BusinessException(404, "工具不存在"));
		work.selectTool(item, LifeSkillRules.toolTier(node.level()), request.tools(), "工具档位、类型或备用数量无效");
		if (request.supplies().size() > 2)
			throw new BusinessException(400, "最多选择两种补给");
		for (var entry : request.supplies().entrySet()) {
			number(entry.getKey());
			if (entry.getValue() == null || entry.getValue() < 1 || entry.getValue() > 99)
				throw new BusinessException(400, "补给上限无效");
			var supply = inventory.list(owner)
				.stream()
				.filter(value -> value.id().equals(entry.getKey()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(409, "所选补给不存在"));
			if (potions.recovery(supply.code()) == null || supply.quantity() < entry.getValue())
				throw new BusinessException(409, "补给种类或数量无效");
		}
		String id = UUID.randomUUID().toString();
		characters.beginLifeActivity(owner, id, petId);
		var state = new GatheringSession(id, node.id(), pet.id(), item.id(), item.code(), request.tools(),
				context.location(), GatheringRules.roundMillis(pet.level()), 0, null, 0, 0, "ACTIVE", true,
				request.supplies());
		sessions.save(context.id(), state);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(state));
		return state;
	}

	@Transactional
	public GatheringSession heartbeat(long owner, GatheringBeat request) {
		if (request == null || request.revision() < 0)
			throw new BusinessException(400, "采集心跳无效");
		var context = characters.requireOutsideDungeon(owner);
		var state = require(context.id(), request.session());
		if (request.revision() == state.revision() - 1 && request.foreground() == state.lastForeground())
			return state;
		if (request.revision() != state.revision())
			throw new BusinessException(409, "采集进度已变化，请刷新");
		if (!context.location().equals(state.location()))
			throw new BusinessException(409, "人物已离开资源点");
		var progress = GatheringRules.heartbeat(state.progressMillis(), state.roundMillis(),
				state.lastSeenMillis() == null ? null : Instant.ofEpochMilli(state.lastSeenMillis()), clock.now(),
				request.foreground());
		long millis = progress.confirmedMillis(), completed = state.completed();
		int remainingTools = state.toolsRemaining();
		var suppliesRemaining = new TreeMap<>(state.supplies());
		String status = request.foreground() ? "ACTIVE" : "PAUSED";
		Long lastSeen = progress.lastSeen() == null ? null : progress.lastSeen().toEpochMilli();
		if (request.foreground() && remainingTools == 0) {
			status = "TOOLS_EXHAUSTED";
			lastSeen = null;
		}
		else if (progress.complete()) {
			var node = catalog.node(state.node());
			var pet = creatures.list(owner)
				.stream()
				.filter(value -> value.id().equals(state.creature()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(409, "派遣伙伴已变化"));
			var toolItem = inventory.list(owner)
				.stream()
				.filter(value -> value.id().equals(state.tool()) && value.code().equals(state.toolCode()))
				.findFirst()
				.orElse(null);
			var cost = GatheringRules.cost(LifeSkillRules.toolTier(node.level()));
			var current = resources.read(context.id(), Long.parseLong(pet.id()), pet.species(), pet.level());
			var availableSupplies = new ArrayList<GatheringSupplyRulesSupply>();
			for (var supply : inventory.list(owner)) {
				int budget = suppliesRemaining.getOrDefault(supply.id(), 0);
				var recovery = potions.recovery(supply.code());
				if (budget > 0 && recovery != null)
					availableSupplies.add(new GatheringSupplyRulesSupply(Long.parseLong(supply.id()),
							Math.min(budget, supply.quantity()), recovery.health(), recovery.mana()));
			}
			var plan = GatheringSupplyRules.plan(current.hp(), current.mp(), current.health(), current.mana(),
					cost.health(), cost.mana(), availableSupplies);
			var possible = new ArrayList<ProductionInventoryProduct>();
			possible.add(new ProductionInventoryProduct(node.product(), node.quantity()));
			if (node.rare() != null)
				possible.add(new ProductionInventoryProduct(node.rare(), 1));
			if (toolItem == null || toolItem.quantity() < 1)
				status = "TOOLS_EXHAUSTED";
			else if (!plan.sufficient())
				status = "RESOURCES_LOW";
			else if (!production.fits(owner, plan.consumed(), possible))
				status = "BAG_FULL";
			else {
				var skill = LifeSkillRulesSkill.valueOf(node.skill());
				var work = kernel.open(owner, context.id(), skill, node.level(), "尚未学习或采集熟练度不足");
				work.selectTool(toolItem, LifeSkillRules.toolTier(node.level()), 1, "工具档位、类型或备用数量无效");
				if (node.rare() != null && !rareRoll.succeeds(work.level(), node.level()))
					possible.removeLast();
				if (!production.exchangeAll(owner, "GATHER:" + state.id() + ":" + completed, plan.consumed(), possible))
					throw new IllegalStateException("角色锁内背包容量发生变化");
				if (!plan.consumed().isEmpty()) {
					resourceCommands.restoreForProduction(owner, Long.parseLong(pet.id()), plan.health(), plan.mana());
					plan.consumed()
						.forEach((id, count) -> suppliesRemaining.compute(Long.toString(id),
								(key, budget) -> budget - count));
				}
				if (!resourceCommands.spendForProduction(owner, Long.parseLong(pet.id()), cost.health(), cost.mana()))
					throw new IllegalStateException("角色锁内采集资源发生变化");
				if (work.complete(node.level(), 1))
					remainingTools--;
				completed++;
				millis = 0;
				if (remainingTools == 0)
					status = "TOOLS_EXHAUSTED";
			}
			if (!status.equals("ACTIVE"))
				lastSeen = null;
		}
		var next = new GatheringSession(state.id(), state.node(), state.creature(), state.tool(), state.toolCode(),
				remainingTools, state.location(), state.roundMillis(), millis, lastSeen, state.revision() + 1,
				completed, status, request.foreground(), suppliesRemaining);
		sessions.save(context.id(), next);
		return next;
	}

	@Transactional
	public void stop(long owner, GatheringStop request) {
		if (request == null)
			throw new BusinessException(400, "缺少停止确认");
		uuid(request.requestId());
		var context = characters.lock(owner);
		String payload = "GATHER_STOP:" + request.session();
		var previous = receipts.prior(context.id(), request.requestId());
		if (previous != null) {
			if (!previous.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return;
		}
		require(context.id(), request.session());
		characters.endLifeActivity(owner, request.session());
		sessions.delete(context.id());
		receipts.record(context.id(), request.requestId(), payload, "{}");
	}

	private GatheringSession require(long character, String id) {
		var state = sessions.find(character);
		if (state == null || !state.id().equals(id))
			throw new BusinessException(409, "采集会话已结束或发生变化");
		return state;
	}

	private void uuid(String value) {
		try {
			if (!UUID.fromString(value).toString().equals(value))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "请求号无效");
		}
	}

	private long number(String value) {
		try {
			long id = Long.parseLong(value);
			if (id <= 0)
				throw new IllegalArgumentException();
			return id;
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "伙伴或工具编号无效");
		}
	}

}
