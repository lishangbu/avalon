package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAbandonCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptResult;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.application.model.QuestState;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestTrackingQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestTrackingCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestTrackingView;
import io.github.lishangbu.avalon.world.application.query.QuestConversationQueryService;
import io.github.lishangbu.avalon.world.application.model.QuestConversation;
import io.github.lishangbu.avalon.world.application.query.QuestRewardPreviewQueryService;
import java.util.List;
import io.github.lishangbu.avalon.platform.CurrentAccount;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

/**
 * NPC 交互仅接收步骤编号，账号归属由服务端身份提供，不接受客户端传入奖励或人物 ID。
 */
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestResource {

	private final QuestCommandService quests;

	private final QuestQueryService queries;

	private final QuestAcceptCommandService acceptance;

	private final QuestAbandonCommandService abandonment;

	private final CurrentAccount identity;

	private final QuestTrackingQueryService trackingQueries;

	private final QuestTrackingCommandService trackingCommands;

	private final QuestConversationQueryService conversations;

	private final QuestRewardPreviewQueryService rewards;

	@GetMapping("/{step}/rewards")
	public List<QuestRewardPreviewQueryService.Item> rewards(@PathVariable String step) {
		return rewards.items(owner(), step);
	}

	/** 仅返回角色当前地点的对白，客户端不能指定未来步骤或冒充已完成状态。 */
	@GetMapping("/dialogues")
	public List<QuestConversation> dialogues() {
		return conversations.local(owner());
	}

	@GetMapping("/tracking")
	public QuestTrackingView tracking() {
		return trackingQueries.state(owner());
	}

	@PutMapping("/tracking")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void track(@RequestBody QuestTrackingCommandService.Request request) {
		trackingCommands.select(owner(), request);
	}

	private long owner() {
		return identity.id();
	}

	/** 参数为整项任务编号；放弃不触碰步骤回执或角色永久事实。 */
	@PostMapping("/{quest}/abandon")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void abandon(@PathVariable String quest,
			@RequestBody(required = false) QuestAbandonCommandService.Request request) {
		abandonment.abandon(owner(), quest, request);
	}

	@GetMapping
	public QuestState state() {
		return queries.state(owner());
	}

	@PostMapping("/{step}")
	public QuestResult interact(@PathVariable("step") String step) {
		return quests.interact(owner(), step);
	}

	/** 接取是独立写命令；读取任务列表从不执行补给，完成任务也不会补做接取。 */
	@PostMapping("/{step}/accept")
	public QuestAcceptResult accept(@PathVariable("step") String step,
			@RequestBody(required = false) QuestAcceptCommandService.Request body) {
		return acceptance.accept(owner(), step, body);
	}

}
