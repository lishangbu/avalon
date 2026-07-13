# Use immutable display names as public Trainer identities

Trainer displayName 在创建时去除首尾空白并以 Unicode NFKC 和忽略英文字母大小写的规范化键实施全局唯一约束，创建后不可修改，Trainer 归档也不释放名称；内部关系只使用数据库 Identifier，对外精确查找和 Challenge 只使用规范化后的完整 displayName，不再生成 Trainer Code。名称规范化后须为 2 至 16 个 Unicode code point，只允许 Unicode 字母、数字、普通空格 U+0020、`_` 与 `-`，不限制分隔符位于首尾；该决定取代 ADR-0025、ADR-0044 与 ADR-0051。
