package io.github.lishangbu.avalon.character.application.query;

import io.github.lishangbu.avalon.character.persistence.repository.CharacterRepository;
import io.github.lishangbu.avalon.character.persistence.repository.FriendshipRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

/** 好友读模型仅公开角色名、职业与关系方向，不公开账号、位置、背包或战斗存档。 */
@Component
@RequiredArgsConstructor
public class FriendshipQueryService {

	private final CharacterQueryService queries;

	private final CharacterRepository characters;

	private final FriendshipRepository friendships;

	@Transactional
	public Page page(long owner, long after) {
		if (after < 0)
			throw new BusinessException(400, "分页位置无效");
		long self = queries.requireId(owner);
		var rows = friendships.page(self, after);
		var entries = rows.stream().limit(50).map(row -> {
			long peer = row.getFirstCharacterId() == self ? row.getSecondCharacterId() : row.getFirstCharacterId();
			var character = characters.get(peer);
			String status = row.isAccepted() ? "ACCEPTED" : row.getRequesterId() == self ? "OUTGOING" : "INCOMING";
			return new Entry(row.getId().toString(), Long.toString(peer), character.getName(),
					character.getProfession(), status);
		}).toList();
		return new Page(entries, rows.size() > 50 ? entries.getLast().id() : null);
	}

	/** 跨模块邀请资格入口返回账号值而非实体；客户端的角色编号本身不构成授权。 */
	@Transactional
	public long requireFriendOwner(long owner, long peer) {
		Long friend = friendOwner(owner, peer);
		if (friend == null)
			throw new BusinessException(409, "请先成为好友");
		return friend;
	}

	/** 通知过滤允许好友关系已删除；用空值表达正常失效，避免捕获跨事务异常污染外层事务。 */
	@Transactional
	public Long friendOwner(long owner, long peer) {
		long self = queries.requireId(owner);
		if (!friendships.accepted(Math.min(self, peer), Math.max(self, peer)))
			return null;
		return characters.get(peer).getAccountId();
	}

	public record Entry(String id, String character, String name, String profession, String status) {
	}

	public record Page(List<Entry> entries, String next) {
	}

}
