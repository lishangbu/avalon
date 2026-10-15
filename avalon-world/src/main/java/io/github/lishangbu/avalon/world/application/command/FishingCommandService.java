package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.ProductionInventoryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.life.LifeProductionKernel;
import io.github.lishangbu.avalon.world.application.model.FishingInput;
import io.github.lishangbu.avalon.world.application.model.FishingSession;
import io.github.lishangbu.avalon.world.application.model.FishingStart;
import io.github.lishangbu.avalon.world.domain.FishingRules;
import io.github.lishangbu.avalon.world.domain.FishingRulesState;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.persistence.repository.FishingSessionRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class FishingCommandService {

	private final CharacterCommandService characters;

	private final FishingCatalog catalog;

	private final LifeProductionCatalog tools;

	private final InventoryQueryService inventory;

	private final InventoryCommandService items;

	private final ProductionInventoryService production;

	private final LifeProductionKernel kernel;

	private final FishingSessionRepository sessions;

	private final LifeOperationRepository receipts;

	private final LifeClock clock;

	private final ObjectMapper json;

	private final SecureRandom entropy = new SecureRandom();

	@Transactional
	public FishingSession start(long owner, FishingStart request) {
		try {
			if (request == null || !UUID.fromString(request.requestId()).toString().equals(request.requestId())
					|| request.fish() == null || Long.parseLong(request.tool()) <= 0
					|| Long.parseLong(request.bait()) <= 0)
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "抛竿请求无效");
		}
		var context = characters.lock(owner);
		String payload = "FISH_START:"
				+ json.writeValueAsString(new FishingStart(null, request.fish(), request.tool(), request.bait()));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return json.readValue(prior.result(), FishingSession.class);
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		var fish = catalog.fish(request.fish());
		if (fish == null || !fish.locations().contains(context.location()))
			throw new BusinessException(409, "请到鱼种所在水域");
		var work = kernel.open(owner, context.id(), LifeSkillRulesSkill.FISHING, fish.level(), "钓鱼熟练度不足");
		var owned = inventory.list(owner);
		var rod = owned.stream()
			.filter(item -> item.id().equals(request.tool()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "鱼竿不存在"));
		work.selectTool(rod, LifeSkillRules.toolTier(fish.level()), 1, "鱼竿类型或档位不足");
		var tool = tools.tool(rod.code());
		var bait = owned.stream()
			.filter(item -> item.id().equals(request.bait()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "鱼饵不存在"));
		if (!bait.code().equals(catalog.definition().bait()))
			throw new BusinessException(409, "请选择鱼饵");
		String id = UUID.randomUUID().toString();
		characters.beginLifeActivity(owner, id, null);
		items.consume(owner, Long.parseLong(bait.id()), bait.code(), 1);
		var session = new FishingSession(id, fish.id(), rod.id(), rod.code(), bait.bound(), context.location(),
				entropy.nextLong(), work.level(), tool.tier(), 0, false, true,
				FishingRules.start(clock.now().toEpochMilli()), false);
		sessions.save(context.id(), session);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(session));
		return session;
	}

	@Transactional
	public FishingSession input(long owner, FishingInput request) {
		if (request == null || request.session() == null || request.revision() < 0)
			throw new BusinessException(400, "钓鱼操作无效");
		var context = characters.lock(owner);
		var current = sessions.find(context.id());
		if (current == null || !current.id().equals(request.session()))
			throw new BusinessException(409, "钓鱼会话已变更");
		if (request.revision() == current.revision() - 1 && request.held() == current.lastHeld()
				&& request.foreground() == current.lastForeground())
			return current;
		if (request.revision() != current.revision())
			throw new BusinessException(409, "钓鱼进度已更新，请刷新");
		if (!current.state().status().equals("PLAYING"))
			return current;
		characters.requireOutsideDungeon(owner);
		if (!current.location().equals(context.location()))
			throw new BusinessException(409, "已离开钓鱼水域");
		var fish = catalog.fish(current.fish());
		var state = FishingRules.advance(current.state(), clock.now().toEpochMilli(), request.held(),
				request.foreground(), current.seed(), fish.difficulty(), current.skillLevel(), current.rodTier());
		boolean broken = false;
		if (state.status().equals("CAUGHT")) {
			var rod = inventory.list(owner)
				.stream()
				.filter(item -> item.id().equals(current.tool()) && item.code().equals(current.toolCode()))
				.findFirst()
				.orElse(null);
			String stop = rod == null ? "TOOLS_EXHAUSTED"
					: !production.harvest(owner, "FISH_CATCH:" + current.id(), fish.id(), 1, current.bound())
							? "BAG_FULL" : null;
			if (stop != null)
				state = new FishingRulesState(state.lastTickMillis(), state.elapsedMillis(), state.caughtMillis(),
						state.position(), false, stop);
			else {
				var work = kernel.open(owner, context.id(), LifeSkillRulesSkill.FISHING, fish.level(), "钓鱼熟练度不足");
				work.selectTool(rod, LifeSkillRules.toolTier(fish.level()), 1, "鱼竿类型或档位不足");
				broken = work.complete(fish.level(), fish.points());
			}
		}
		if (!state.status().equals("PLAYING"))
			characters.endLifeActivity(owner, current.id());
		var result = new FishingSession(current.id(), current.fish(), current.tool(), current.toolCode(),
				current.bound(), current.location(), current.seed(), current.skillLevel(), current.rodTier(),
				current.revision() + 1, request.held(), request.foreground(), state, broken);
		sessions.save(context.id(), result);
		return result;
	}

}
