package io.github.lishangbu.avalon.character.api;

import io.github.lishangbu.avalon.character.application.command.FriendshipCommandService;
import io.github.lishangbu.avalon.character.application.query.FriendshipQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

/** 身份只来自认证令牌；HTTP 请求不能指定申请人或代替好友接受。 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipResource {

	private final CurrentAccount identity;

	private final FriendshipCommandService commands;

	private final FriendshipQueryService queries;

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public FriendshipQueryService.Page page(@RequestParam(value = "after", defaultValue = "0") long after) {
		return queries.page(owner(), after);
	}

	@PostMapping
	public Created request(@RequestBody(required = false) Request body) {
		if (body == null)
			throw new BusinessException(400, "请填写完整角色名");
		return new Created(Long.toString(commands.request(owner(), body.name())));
	}

	@PostMapping("/{id}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void accept(@PathVariable("id") long id) {
		commands.accept(owner(), id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable("id") long id) {
		commands.remove(owner(), id);
	}

	public record Request(String name) {
	}

	public record Created(String id) {
	}

}
