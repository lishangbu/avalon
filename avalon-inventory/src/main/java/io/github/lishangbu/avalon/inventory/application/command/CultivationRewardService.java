package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.CultivationRewardCatalog;
import io.github.lishangbu.avalon.inventory.application.model.CultivationRewardCompletedSource;
import io.github.lishangbu.avalon.inventory.application.model.CultivationRewardResult;
import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRules;
import io.github.lishangbu.avalon.inventory.persistence.entity.CultivationRewardReceipt;
import io.github.lishangbu.avalon.inventory.persistence.mapper.InventoryStorageCodec;
import io.github.lishangbu.avalon.inventory.persistence.repository.CultivationRewardReceiptRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.util.SplittableRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 仅供服务器已验证完成条件的委托／战斗结算调用，不提供HTTP任意领奖接口。 调用方负责具体内容资格、实际参与和胜利，传入持久化事件编号及种子，并与完成进度共用事务。
 * 副本按合格成员分别调用，eventId必须取整次进度编号，不能取每个阶段编号；本服务不改发起者额外奖励。
 */
@Component
@RequiredArgsConstructor
public class CultivationRewardService {

	private final CharacterCommandService characters;

	private final CultivationRewardCatalog catalog;

	private final InventoryCommandService inventory;

	private final CultivationRewardReceiptRepository receipts;

	private final InventoryStorageCodec codec;

	@Transactional(Transactional.TxType.MANDATORY)
	public CultivationRewardResult settle(long owner, CultivationRewardCompletedSource source) {
		if (source == null || source.kind() == null || source.eventId() == null
				|| !source.eventId().matches("[A-Za-z0-9_:-]{1,100}"))
			throw new IllegalArgumentException("培养奖励缺少有效来源事件");
		long character = characters.lock(owner).id();
		String key = source.kind() + ":" + source.eventId();
		var prior = receipts.prior(character, key);
		if (prior != null) {
			if (!source.equals(codec.decode(prior.getPayload(), CultivationRewardCompletedSource.class)))
				throw new BusinessException(409, "该来源已结算，不能更改路线、种子或强化石选择");
			return codec.decode(prior.getResult(), CultivationRewardResult.class);
		}
		var random = new SplittableRandom(source.seed());
		var rewards = CultivationRewardRules.rewards(catalog.definition(), source.kind(), source.route(),
				source.stone(), random.nextInt(100), random.nextInt(100));
		// 每条库存来源保持独立，背包溢出沿用奖励待领；整笔事件凭证仍约束改领及无掉落重试。
		for (int i = 0; i < rewards.size(); i++) {
			var reward = rewards.get(i);
			inventory.grantWithBinding(owner, "CULTIVATION:" + key + ":" + i, reward.code(), reward.quantity(), false,
					reward.bound());
		}
		var result = new CultivationRewardResult(rewards);
		var receipt = new CultivationRewardReceipt();

		receipt.setCharacterId(character);
		receipt.setEventKey(key);
		receipt.setPayload(codec.encode(source));
		receipt.setResult(codec.encode(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

}
