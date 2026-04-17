---
name: avalon-testing-principles
description: Use when choosing, adding, reviewing, or restructuring tests in avalon, including unit, integration, HTTP smoke, contract, persistence, outbox, auth, and cross-context verification.
---

# Avalon 测试原则

## 目标

- 优先覆盖真实风险，而不是追求表面覆盖率
- 把验证放在最靠近规则的层
- 让每个测试只回答一个明确问题

## 先做判断

1. 先识别改动所属 bounded context。
2. 再判断规则主要落在 `domain`、`application`、`infrastructure` 还是 `interfaces`。
3. 若触及上下文边界、schema、outbox、认证、权限或持久化，先遵守 `avalon-guardrails`。
4. 如需快速判定测试层级，先看 [references/test-layers.md](references/test-layers.md)。

## 分层原则

- `domain`: 测不变量、状态迁移、值对象和纯判定逻辑。
- `application`: 测编排、事务边界、仓库协作、幂等、权限和 outbox 触发。
- `infrastructure`: 测 SQL 映射、持久化约束、调度、加密、适配器和序列化差异。
- `interfaces`: 测路由、请求/响应映射、输入校验、状态码和错误转换。
- `integration`: 只保留关键主流程、跨层回归和少量 smoke path，不把它当作万能补洞层。

## 选择规则

- 业务规则越稳定、越纯粹，测试越靠下。
- 真实事务、数据库约束、唯一性、级联、事务后通知、认证态，优先用真实基础设施。
- 只验证自己拥有且运行成本可接受的行为，避免为了隔离而把所有依赖 mock 掉。
- 一次修复一个 bug，先在最接近出错点的层补回归；如果问题跨层，再补一条端到端主路径。
- 接口测试保持短而精，不要把一个模块的全部 CRUD 和所有异常都堆进单个超长测试类。

## 严格执行标准

- 只要行为发生变化，就默认必须补测试。
- 新增功能默认至少补一条最贴近规则层的测试。
- 如果同一次改动同时影响 API、持久化或跨上下文协作，就补对应层的测试，不只靠一条高层测试兜底。
- 修复缺陷必须补回归测试，而且测试要尽量直接复现该缺陷。
- 触碰 `domain` / `application` 业务规则时，不能只靠 `HTTP` 测试。
- 触碰 `interfaces` 时，必须验证状态码、响应字段或错误映射。
- 触碰 `infrastructure` 时，优先真实数据库、真实事务、真实适配器，避免纯 mock 代替关键行为。
- 对关键模块的改动，默认要求至少一条低层测试加一条必要的边界测试。
- 只有在确认是无行为样板代码、且不会影响关键路径时，才考虑不补测试。
- 如果一个地方很难测，先判断是不是设计边界有问题，再决定是否调整实现，而不是先放弃测试。

## Fixture 规则

- 用最小可读的数据构造场景，优先 builder/fixture，而不是长串 `mapOf(...)`。
- 让测试数据显式表达意图，避免为了省行数牺牲可读性。
- 断言优先检查业务结果、状态转换和边界值，不要重复验证框架已经保证的细节。

## 不要做

- 不要把 `data class`、getter、简单 enum 映射当成主要测试目标。
- 不要用 HTTP 测试替代 domain/application 测试。
- 不要让单个测试类承担过多业务故事。
- 不要为追求覆盖率而补没有风险的测试。

## 收尾检查

- 这次测试是否覆盖了最可能出错的规则。
- 是否把测试放在了最合适的层。
- 是否存在重复验证、过度 mock 或过长场景。
- 是否补了足够的回归用例，而不是只补 happy path。
