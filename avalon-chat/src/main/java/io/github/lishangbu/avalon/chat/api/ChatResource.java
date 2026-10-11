package io.github.lishangbu.avalon.chat.api;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.chat.application.ChatRealtime;
import io.github.lishangbu.avalon.chat.application.command.ChatCommandService;
import io.github.lishangbu.avalon.chat.application.query.ChatQueryService;
import io.github.lishangbu.avalon.chat.application.model.ChatViews.*;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** HTTP适配只接收频道、角色名和消息，不接受发送者账号；所有操作使用Bearer解析的当前身份。 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatResource {

	private final CurrentAccount identity;

	private final ChatQueryService queries;

	private final ChatCommandService commands;

	private final CharacterQueryService characters;

	private final ChatRealtime realtime;

	/** 会话摘要只属于当前角色；没有账号参数，也不自动标记已读。 */
	@GetMapping
	public Summary summary() {
		return queries.summary(identity.id());
	}

	/** 获取当前角色可见的频道历史，before为排他向前游标，默认从最新页开始。 */
	@GetMapping("/messages")
	public Page history(@RequestParam String channel, @RequestParam(required = false) String peer,
			@RequestParam(defaultValue = "0") long before) {
		return queries.history(identity.id(), channel, peer, before);
	}

	/** 发件人由认证身份决定，SYSTEM发送与战斗发送在服务端拒绝。 */
	@PostMapping("/messages")
	public Message send(@RequestBody Send request) {
		return commands.send(identity.id(), request);
	}

	/** 前台实际展示后显式确认已读；战斗期间拒绝，不能用接收事件替代阅读。 */
	@PostMapping("/read")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void read(@RequestBody Read request) {
		commands.read(identity.id(), request);
	}

	/** 修改本人的陌生人拒收策略，保持原发送频率记录与历史不变。 */
	@PutMapping("/settings")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void settings(@RequestBody Settings request) {
		commands.settings(identity.id(), request.rejectStrangers());
	}

	/** 按角色名建立本人黑名单，不修改对方资料或双方好友关系。 */
	@PostMapping("/blocks")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void block(@RequestBody Block request) {
		commands.block(identity.id(), request.name());
	}

	/** 仅解除本人建立的黑名单，其他角色即使使用相同peer编号也不能被修改。 */
	@DeleteMapping("/blocks/{peer}")
	@ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
	public void unblock(@PathVariable long peer) {
		commands.unblock(identity.id(), peer);
	}

	/** 仅流提示允许战斗中连接；同源Bearer头认证，令牌绝不写URL或日志。 */
	@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> events() {
		// 禁止代理缓存或缓冲唤醒流，避免同源部署后消息提示堆积到连接关闭才到达。
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.header("X-Accel-Buffering", "no")
			.body(realtime.connect(characters.requireId(identity.id())));
	}

}
