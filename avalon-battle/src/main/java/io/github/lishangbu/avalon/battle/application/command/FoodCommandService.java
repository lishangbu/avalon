package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.life.FoodCatalog;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.persistence.repository.LifeOperationRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 食用与库存、资源及回执同事务提交；不允许吃料理复活或打断派遣。 */
@Component
@RequiredArgsConstructor
public class FoodCommandService {

	private final CharacterCommandService characters;

	private final InventoryQueryService inventory;

	private final InventoryCommandService items;

	private final FoodCatalog foods;

	private final PartyFactory party;

	private final CombatVitalsRepository vitals;

	private final CombatVitalsStore store;

	private final LifeOperationRepository receipts;

	private final ObjectMapper json;

	@Transactional
	public Result eat(long owner, Request request) {
		try {
			if (request == null || !UUID.fromString(request.requestId()).toString().equals(request.requestId())
					|| Long.parseLong(request.item()) <= 0 || Long.parseLong(request.target()) <= 0)
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "食用请求无效");
		}
		var context = characters.lock(owner);
		String payload = "EAT:" + request.item() + ":" + request.target();
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.payload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他操作");
			return json.readValue(prior.result(), Result.class);
		}
		characters.requireOutsideDungeon(owner);
		characters.requireNoLifeActivity(owner);
		var item = inventory.list(owner)
			.stream()
			.filter(value -> value.id().equals(request.item()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "所选料理不存在"));
		var food = foods.food(item.code());
		if (food == null)
			throw new BusinessException(409, "该物品不能作为料理食用");
		var target = party.create(owner, true)
			.stream()
			.filter(unit -> unit.getId().equals(request.target()) && (unit.isHero() || unit.getPosition() < 100))
			.findFirst()
			.orElseThrow(() -> new BusinessException(409, "请选择人物或随身伙伴"));
		long id = Long.parseLong(target.getId());
		var current = vitals.get(id);
		if (current != null && !current.getCharacterId().equals(context.id()))
			throw new IllegalStateException("资源归属不一致");
		int hp = current == null ? target.getHp() : Math.min(current.getHp(), target.getHp());
		int mp = current == null ? target.getMp() : Math.min(current.getMp(), target.getMp());
		if (hp <= 0)
			throw new BusinessException(409, "倒下的目标需要先到幻兽中心恢复");
		int nextHp = (int) Math.min(target.getHp(), (long) hp + food.health());
		int nextMp = (int) Math.min(target.getMp(), (long) mp + food.mana());
		if (nextHp == hp && nextMp == mp)
			throw new BusinessException(409, "目标无需该料理恢复，不消耗料理");
		items.consume(owner, Long.parseLong(item.id()), item.code(), 1);
		store.saveVitals(context.id(), id, nextHp, nextMp);
		var result = new Result(nextHp, nextMp, nextHp - hp, nextMp - mp);
		receipts.record(context.id(), request.requestId(), payload, json.writeValueAsString(result));
		return result;
	}

	public record Request(String requestId, String item, String target) {
	}

	public record Result(int health, int mana, int restoredHealth, int restoredMana) {
	}

}
