package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
import io.github.lishangbu.avalon.world.api.model.WorldCreate;
import io.github.lishangbu.avalon.world.api.model.WorldMove;
import io.github.lishangbu.avalon.world.api.model.WorldWorldView;
import io.github.lishangbu.avalon.world.application.query.WorldMapQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 世界 API 从认证身份获取归属，只返回当前玩家视图；地图配置与实例状态分别维护。
 */
@RestController
@RequestMapping("/api/world")
@RequiredArgsConstructor
public class WorldResource {

	private final CharacterCommandService characters;

	private final CharacterQueryService characterQueries;

	private final WorldMapQueryService maps;

	private final WorldCatalog catalog;

	private final CurrentAccount identity;

	private long owner() {
		return identity.id();
	}

	@GetMapping
	/** 普通只读查询，不持角色写锁；内容目录在本次应用运行期间保持不变。 */
	public WorldWorldView get() {
		var character = characterQueries.find(owner());
		if (character == null)
			return new WorldWorldView(null, catalog.definition());
		return new WorldWorldView(character, maps.visible(owner()));
	}

	@PostMapping("/character")
	public CharacterView create(@RequestBody(required = false) WorldCreate body) {
		if (body == null)
			throw new BusinessException(400, "请填写角色信息");
		return characters.create(owner(), body.name(), body.profession());
	}

	@PostMapping("/move")
	public CharacterView move(@RequestBody(required = false) WorldMove body) {
		if (body == null)
			throw new BusinessException(400, "请选择目的地");
		return characters.move(owner(), body.destination(), body.expectedRevision());
	}

}
