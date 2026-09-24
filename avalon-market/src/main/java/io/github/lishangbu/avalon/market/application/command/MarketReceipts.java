package io.github.lishangbu.avalon.market.application.command;

import io.github.lishangbu.avalon.market.persistence.mapper.MarketReceiptCodec;
import io.github.lishangbu.avalon.market.persistence.entity.MarketReceipt;
import io.github.lishangbu.avalon.market.persistence.repository.MarketReceiptRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 以 JSON 值比较请求内容，不因对象键顺序改变而误拒绝合法重试。调用者必须持账号角色锁。 */
@Component
@RequiredArgsConstructor
public class MarketReceipts {

	private final MarketReceiptRepository repository;

	private final MarketReceiptCodec codec;

	public void validate(String requestId) {
		try {
			if (!UUID.fromString(requestId).toString().equals(requestId))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException | NullPointerException error) {
			throw new BusinessException(400, "市场请求号无效");
		}
	}

	public <T> T replay(long owner, String request, String operation, Object payload, Class<T> type) {
		var prior = repository.prior(owner, request);
		if (prior == null)
			return null;
		if (!prior.getOperation().equals(operation) || !codec.matches(prior.getPayload(), payload))
			throw new BusinessException(409, "请求号已用于其他市场操作");
		return codec.decode(prior.getResult(), type);
	}

	public void save(long owner, String request, String operation, Object payload, Object result) {
		var receipt = new MarketReceipt();

		receipt.setAccountId(owner);
		receipt.setRequestId(request);
		receipt.setOperation(operation);
		receipt.setPayload(codec.encode(payload));
		receipt.setResult(codec.encode(result));
		repository.saveAndFlush(receipt);
	}

}
