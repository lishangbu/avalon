package io.github.lishangbu.avalon.character.application.query;

import io.github.lishangbu.avalon.character.application.model.CharacterAdventureState;
import io.github.lishangbu.avalon.character.application.model.CharacterPublicPlayer;
import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.character.persistence.mapper.CharacterViews;
import io.github.lishangbu.avalon.character.persistence.repository.CharacterRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 查询只使用普通 SELECT，不阻塞移动与领奖。每次只读取一条角色记录。 同一账号的归属条件在 Repository 中执行，不能把外部角色 ID 当作授权凭据。
 */
@Component
@RequiredArgsConstructor
public class CharacterQueryService {

	private final CharacterRepository characters;

	private final CharacterViews views;

	/**
	 * 按账号读取单个角色并转换不可变视图；尚未创角返回 null，由路由选择创角页面。普通 SELECT 不取得角色写锁，允许在另一事务移动或发经验时读取先前已提交状态。
	 */
	@Transactional
	public CharacterView find(long owner) {
		var character = characters.owned(owner);
		return character == null ? null : views.from(character);
	}

	/** 公开角色名用于幻兽原捕获者署名，不返回登录账号、金币或其他私有角色资料。 */
	@Transactional
	public String publicName(long characterId) {
		var character = characters.get(characterId);
		return character == null ? null : character.getName();
	}

	/** 其他领域查询只需解析归属，不返回实体，也不取得悲观写锁。 */
	/**
	 * 解析账号已拥有的角色编号，不暴露实体。当前无角色删除和转移归属，因此后续同领域列表查询可安全按此编号过滤；未来引入删除/转移时应重新评估一致性边界。
	 */
	@Transactional
	public Long requireId(long owner) {
		var character = characters.owned(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		return character.getId();
	}

	/**
	 * 聊天按精确角色名发起联系，只提供稳定角色编号和显示名。 不返回账号、位置、战斗或资产信息；允许陌生玩家联系不等于授权读取其私有资料。
	 */
	@Transactional
	public CharacterPublicPlayer publicPlayer(String name) {
		if (name == null || name.length() > 32)
			throw new BusinessException(400, "请输入有效角色名");
		var character = characters.byName(name);
		if (character == null)
			throw new BusinessException(404, "角色不存在");
		return new CharacterPublicPlayer(character.getId(), character.getName());
	}

	@Transactional
	public java.util.List<CharacterPublicPlayer> nearbyBattleCandidates(long owner) {
		var self = characters.owned(owner);
		if (self == null)
			throw new BusinessException(404, "请先创建角色");
		return characters.battleCandidates(self.getLocation(), self.getId(),
				org.springframework.data.domain.PageRequest.of(0, 100));
	}

	/** 内部进度查询需要的门禁信息，不返回持久化实体。 */
	@Transactional
	public CharacterAdventureState adventure(long owner) {
		var character = characters.owned(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		return new CharacterAdventureState(character.getActiveDungeonId(), character.getActiveBattleId(),
				character.getPreparationRevision(), character.getActiveCourseId());
	}

	@Transactional
	public Long lobby(long owner) {
		var character = characters.owned(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		return character.getDungeonLobbyId();
	}

}
