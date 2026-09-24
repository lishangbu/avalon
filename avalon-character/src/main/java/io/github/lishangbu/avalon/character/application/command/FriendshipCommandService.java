package io.github.lishangbu.avalon.character.application.command;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.character.persistence.entity.Friendship;
import io.github.lishangbu.avalon.character.persistence.repository.CharacterRepository;
import io.github.lishangbu.avalon.character.persistence.repository.FriendshipRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 好友必须由收件人明确接受；双向同时申请也不推断同意，不改变战斗或组队占位。 */
@Component
@RequiredArgsConstructor
public class FriendshipCommandService {

	private final CharacterQueryService queries;

	private final CharacterRepository characters;

	private final FriendshipRepository friendships;

	/** 精确角色名用于找到目标，关系建立后只使用稳定编号。重复申请返回同一关系。 */
	@Transactional
	public long request(long owner, String name) {
		long self = queries.requireId(owner);
		if (name == null || !name.matches("[\\p{L}\\p{N}_]{2,16}"))
			throw new BusinessException(400, "请填写完整角色名");
		var target = characters.byName(name);
		if (target == null)
			throw new BusinessException(404, "角色不存在");
		if (target.getId() == self)
			throw new BusinessException(400, "不能添加自己为好友");
		long first = Math.min(self, target.getId());
		long second = Math.max(self, target.getId());
		// 即使关系尚不存在，也按固定顺序锁住两个角色，串行化同一对角色的并发首次申请。
		characters.lock(first);
		characters.lock(second);
		var existing = friendships.pair(first, second);
		if (existing != null)
			return existing.getId();
		var relation = new Friendship();

		relation.setFirstCharacterId(first);
		relation.setSecondCharacterId(second);
		relation.setRequesterId(self);
		friendships.saveAndFlush(relation);
		return relation.getId();
	}

	/** 只锁关系，不再取得角色写锁，避免与申请的“角色再关系”锁顺序构成环。 */
	@Transactional
	public void accept(long owner, long id) {
		long self = queries.requireId(owner);
		var relation = friendships.lockOwned(id, self);
		if (relation == null)
			throw new BusinessException(404, "好友申请不存在");
		if (relation.getRequesterId() == self)
			throw new BusinessException(409, "等待对方接受申请");
		relation.setAccepted(true);
	}

	/** 收件人拒绝、发件人撤回以及任一方删除好友共用移除操作；不存在的本人关系幂等返回。 */
	@Transactional
	public void remove(long owner, long id) {
		var relation = friendships.lockOwned(id, queries.requireId(owner));
		if (relation != null)
			friendships.delete(relation);
	}

}
