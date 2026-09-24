package io.github.lishangbu.avalon.world.api;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;
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
	public WorldView get() {
		var character = characterQueries.find(owner());
		if (character == null)
			return new WorldView(null, catalog.definition());
		return new WorldView(character, maps.visible(owner()));
	}

	@PostMapping("/character")
	public CharacterView create(@RequestBody(required = false) Create body) {
		if (body == null)
			throw new BusinessException(400, "请填写角色信息");
		return characters.create(owner(), body.name(), body.profession());
	}

	@PostMapping("/move")
	public CharacterView move(@RequestBody(required = false) Move body) {
		if (body == null)
			throw new BusinessException(400, "请选择目的地");
		return characters.move(owner(), body.destination(), body.expectedRevision());
	}

	/**
	 * 世界首页响应，组合角色读模型与不可变地图内容。 character 为 null 表示尚未创建角色，不能解释为请求失败或默认生成角色。
	 */
	public record WorldView(CharacterView character, WorldCatalog.Definition content) {
	}

	/**
	 * 创角请求仅包含名称与职业意图；初始经验、位置、编号均由后端赋值。
	 */
	public record Create(String name, String profession) {
	}

	/**
	 * 移动请求携带预期版本用于冲突检查，不包含可任意设置的角色坐标。
	 */
	public record Move(String destination, long expectedRevision) {
	}

}
