package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.SceneActionResult;
import io.github.lishangbu.avalon.world.application.query.QuestSceneAccess;
import io.github.lishangbu.avalon.world.persistence.entity.SceneActionReceipt;
import io.github.lishangbu.avalon.world.persistence.repository.SceneActionReceiptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/** 四类有限场景动作的唯一写入口，不接受客户端奖励、地点、完成事实或物品数量。 */
@Component
@RequiredArgsConstructor
public class SceneActionCommandService {

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final CharacterFactQueryService facts;

	private final CharacterFactCommandService factCommands;

	private final SceneActionCatalog catalog;

	private final InventoryCommandService inventory;

	private final CreatureQueryService creatures;

	private final SceneActionReceiptRepository receipts;

	private final ObjectMapper json;

	private final QuestSceneAccess questAccess;

	/** 先锁角色并查原回执，再检验新操作资格；保存失败将事实、物品及回执整体回滚。 */
	@Transactional
	public SceneActionResult execute(long owner, String actionId, Request request) {
		validate(request, actionId);
		var character = characters.lock(owner);
		String payload = json.writeValueAsString(new Payload(actionId, request.expectedRevision(), request.choiceId()));
		var prior = receipts.prior(character.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "同一请求号不能用于不同场景操作");
			return json.readValue(prior.getResponse(), SceneActionResult.class);
		}
		characters.requireOutsideDungeon(owner);
		var current = queries.find(owner);
		if (current.revision() != request.expectedRevision())
			throw new BusinessException(409, "位置已变化，请刷新地图");
		var action = catalog.action(actionId);
		if (action == null)
			throw new BusinessException(404, "场景动作不存在");
		if (!action.location().equals(character.location()))
			throw new BusinessException(409, "请先到达场景所在地点");
		boolean report = action.type() == SceneActionCatalog.Type.PRESENT_RECORD;
		var completed = facts.codes(owner);
		String fact = "SCENE:" + action.id();
		SceneActionResult result;
		if (completed.contains(fact)) {
			// 当前地点的已完成动作允许回看，不要求已经离开的接待人物重新出现，也绝不再次给物品。
			result = new SceneActionResult(actionId, true, action.success(), null, 0);
		}
		else {
			if (!questAccess.available(character.id()).contains(action.id()))
				throw new BusinessException(409, "请先接取并推进对应任务");
			if (report ? action.choices().stream().noneMatch(choice -> choice.id().equals(request.choiceId()))
					: request.choiceId() != null)
				throw new BusinessException(400, "场景选项无效");
			if (!catalog.available(action, completed::contains))
				throw new BusinessException(409, "场景尚未开放、接待人物缺席或前置行动未完成");
			if (report && !action.answers().contains(request.choiceId()))
				result = new SceneActionResult(actionId, false, action.incorrect(), null, 0);
			else {
				// 出航等准备动作读取实际编队；客户端勾选确认不能代替服务端校验。
				if (action.minimumActive() > 0 && creatures.list(owner)
					.stream()
					.filter(pet -> pet.partyPosition() >= 0 && pet.partyPosition() < 2)
					.count() < action.minimumActive())
					throw new BusinessException(409, "请先编入至少" + action.minimumActive() + "只出战伙伴");
				if (action.type() == SceneActionCatalog.Type.COLLECT_TASK)
					inventory.grant(owner, fact, action.itemCode(), action.quantity(), false);
				else if (action.type() == SceneActionCatalog.Type.DELIVER_TASK)
					inventory.take(owner, action.itemCode(), action.quantity());
				factCommands.record(owner, fact);
				result = new SceneActionResult(actionId, true, action.success(), action.itemCode(), action.quantity());
			}
		}
		var receipt = new SceneActionReceipt();
		receipt.setCharacterId(character.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setResponse(json.writeValueAsString(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	private void validate(Request request, String actionId) {
		if (request == null || actionId == null || actionId.isBlank() || request.requestId() == null
				|| request.expectedRevision() < 0)
			throw new BusinessException(400, "场景请求不完整");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "场景请求号必须为标准UUID");
		}
	}

	public record Request(String requestId, long expectedRevision, String choiceId) {
	}

	/** 固定序列化顺序便于请求比较；请求号自身不重复放入负载。 */
	private record Payload(String actionId, long expectedRevision, String choiceId) {
	}

}
