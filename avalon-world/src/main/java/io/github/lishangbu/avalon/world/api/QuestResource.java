package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.application.command.QuestAbandonCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestTrackingCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAbandonRequest;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptResult;
import io.github.lishangbu.avalon.world.application.model.QuestConversation;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.application.model.QuestRewardItem;
import io.github.lishangbu.avalon.world.application.model.QuestState;
import io.github.lishangbu.avalon.world.application.model.QuestTrackingRequest;
import io.github.lishangbu.avalon.world.application.model.QuestTrackingView;
import io.github.lishangbu.avalon.world.application.query.QuestConversationQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestRewardPreviewQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestTrackingQueryService;
import io.github.lishangbu.avalon.world.quest.model.QuestApplied;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * NPC 交互仅接收步骤编号，账号归属由服务端身份提供，不接受客户端传入奖励或人物 ID。
 */
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestResource {

	private final QuestCommandService quests;

	private final io.github.lishangbu.avalon.world.quest.QuestRuntimeService runtime;

	/** 返回独立任务尝试，客户端可显示轮次、数量进度及分支选择。读取不发奖。 */
	@GetMapping("/instances")
	public List<QuestInstanceView> instances() {
		return runtime.views(owner());
	}

	/** 实例写入口携带请求号和轮次，避免网络重试误交付下一次委托。 */
	@PostMapping("/instances/commands")
	public QuestApplied command(@RequestBody QuestInstanceRequest request) {
		return runtime.command(owner(), request);
	}

	private final QuestQueryService queries;

	private final QuestAcceptCommandService acceptance;

	private final QuestAbandonCommandService abandonment;

	private final CurrentAccount identity;

	private final QuestTrackingQueryService trackingQueries;

	private final QuestTrackingCommandService trackingCommands;

	private final QuestConversationQueryService conversations;

	private final QuestRewardPreviewQueryService rewards;

	@GetMapping("/{step}/rewards")
	public List<QuestRewardItem> rewards(@PathVariable String step) {
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
	public void track(@RequestBody QuestTrackingRequest request) {
		trackingCommands.select(owner(), request);
	}

	private long owner() {
		return identity.id();
	}

	/** 参数为整项任务编号；放弃不触碰步骤回执或角色永久事实。 */
	@PostMapping("/{quest}/abandon")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void abandon(@PathVariable String quest, @RequestBody(required = false) QuestAbandonRequest request) {
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
			@RequestBody(required = false) QuestAcceptRequest body) {
		return acceptance.accept(owner(), step, body);
	}

}
