package io.github.lishangbu.avalon.market.application.model;

/** 金额与雪花编号输出字符串，避免浏览器大整数精度丢失。 */
public record MarketPurchaseResult(String listingId, String itemId, int quantity, String gross, String tax,
		String sellerNet) {
}
