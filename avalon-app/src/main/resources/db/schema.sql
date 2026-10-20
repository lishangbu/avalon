-- 当前 PostgreSQL 数据库结构；仅初始化空库，不包含业务数据或版本升级逻辑。
CREATE TABLE public.account (
    id bigint NOT NULL,
    username character varying(32) NOT NULL,
    password_hash character varying(255) NOT NULL,
    roles character varying(255) NOT NULL,
    navigation character varying(128) DEFAULT 'COMPANIONS,INVENTORY,CHARACTER'::character varying NOT NULL
);

COMMENT ON TABLE public.account IS '账号写模型。仅保存密码哈希，禁止直接作为 HTTP 响应。';

COMMENT ON COLUMN public.account.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.account.username IS '登录账号，数据库唯一约束处理并发注册竞争。密码认证和角色名是不同概念。';

COMMENT ON COLUMN public.account.password_hash IS 'bcrypt 密码摘要；不保存明文密码，不允许返回给 HTTP 客户端或写入业务日志。';

COMMENT ON COLUMN public.account.roles IS '逗号分隔的权限角色集合，供 Security JPA 和会话认证构建身份使用。';

COMMENT ON COLUMN public.account.navigation IS '三个自选导航标识按顺序保存；不存 URL，不能借配置注入任意跳转地址。';

CREATE VIEW public.account_credentials AS
 SELECT id,
    username,
    password_hash,
    roles
   FROM public.account;

COMMENT ON VIEW public.account_credentials IS 'Security JPA 的只读身份适配器。当前扩展只扫描应用根索引的 UserDefinition， 因此将认证投影放在启动模块；账号写模型与事务继续属于 identity 模块。 视图直接读取账号表，没有第二份密码数据，也不通过此实体更新账号。';

COMMENT ON COLUMN public.account_credentials.id IS '只读视图映射的账号主键，与账号表相同，不重新生成；本实体不得作为HTTP响应序列化。';

COMMENT ON COLUMN public.account_credentials.username IS '登录账号，数据库唯一约束处理并发注册竞争。密码认证和角色名是不同概念。';

COMMENT ON COLUMN public.account_credentials.password_hash IS 'bcrypt 密码摘要；不保存明文密码，不允许返回给 HTTP 客户端或写入业务日志。';

COMMENT ON COLUMN public.account_credentials.roles IS '逗号分隔的权限角色集合，供 Security JPA 和会话认证构建身份使用。';

CREATE TABLE public.battle (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(64) NOT NULL,
    active boolean NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.battle IS '一场独立遭遇。JSON 保存当场个体与资源，active 部分唯一索引阻止重复开战。';

COMMENT ON COLUMN public.battle.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.battle.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.battle.request_id IS '开战客户端请求号，仅用于幂等，不能用作随机种子。';

COMMENT ON COLUMN public.battle.active IS '是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。';

COMMENT ON COLUMN public.battle.state IS '完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。';

CREATE TABLE public.battle_turn (
    id bigint NOT NULL,
    battle_id bigint NOT NULL,
    round integer NOT NULL,
    response text NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL
);

COMMENT ON TABLE public.battle_turn IS '每个战局回合只保存一份公开结果。旧回合重发直接返回它，不执行本轮新指令。';

COMMENT ON COLUMN public.battle_turn.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.battle_turn.battle_id IS '所属战局主键；与独立副本进度编号区分。';

COMMENT ON COLUMN public.battle_turn.round IS '战局回合序号；同一战局内按回合保存，重试不得再次执行。';

COMMENT ON COLUMN public.battle_turn.response IS '该回合公开战斗结果的 JSON 文本；旧回合重发返回原结果，不重算战斗。';

COMMENT ON COLUMN public.battle_turn.request_id IS '完整回合命令的UUID请求号，重试保持不变';

COMMENT ON COLUMN public.battle_turn.payload IS '已确认的完整请求载荷，用于同号异参拒绝，不保存战斗日志';

CREATE TABLE public.book_recycling_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL
);

COMMENT ON TABLE public.book_recycling_receipt IS '记录玩家确认的书目、数量和报价，与库存及金币原子提交。';

COMMENT ON COLUMN public.book_recycling_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.book_recycling_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.book_recycling_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.book_recycling_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

CREATE TABLE public.character_fact (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.character_fact IS '角色已发生的游戏事实。只由实际完成对应行为的应用命令写入，客户端不能直接声明事实。';

COMMENT ON COLUMN public.character_fact.id IS '雪花主键，内部保持 Long，序列化时避免 JavaScript 精度丢失。';

COMMENT ON COLUMN public.character_fact.character_id IS '所属角色；不通过跨模块实体关联传播持久化状态。';

COMMENT ON COLUMN public.character_fact.code IS '稳定事实编号，例如在特定教学阶段完成恢复；显示文本不参与判定。';

COMMENT ON COLUMN public.character_fact.created_at IS '首次实际发生时间；重复操作保留原时间，不覆盖已经取得的资格。';

CREATE TABLE public.chat_block (
    character_id bigint NOT NULL,
    peer bigint NOT NULL,
    name character varying(32) NOT NULL,
    CONSTRAINT chat_block_self CHECK ((character_id <> peer))
);

COMMENT ON TABLE public.chat_block IS '本人主动建立的有方向黑名单；阻止后续私聊并过滤本人所见发言，不删除对方角色或历史。';

COMMENT ON COLUMN public.chat_block.character_id IS '执行拉黑的角色';

COMMENT ON COLUMN public.chat_block.peer IS '被拉黑角色';

COMMENT ON COLUMN public.chat_block.name IS '公开显示名快照，用于解除拉黑';

CREATE TABLE public.chat_message (
    id bigint NOT NULL,
    channel character varying(10) NOT NULL,
    sender bigint,
    sender_name character varying(32) NOT NULL,
    recipient bigint,
    recipient_name character varying(32),
    text text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    request_id character varying(100) NOT NULL,
    CONSTRAINT chat_message_length CHECK (((char_length(text) >= 1) AND (char_length(text) <= 500))),
    CONSTRAINT chat_message_shape CHECK (((((channel)::text = 'WORLD'::text) AND (sender IS NOT NULL) AND (recipient IS NULL)) OR (((channel)::text = 'PRIVATE'::text) AND (sender IS NOT NULL) AND (recipient IS NOT NULL) AND (sender <> recipient)) OR (((channel)::text = 'SYSTEM'::text) AND (sender IS NULL))))
);

COMMENT ON TABLE public.chat_message IS '聊天纯文本主表；玩家消息与可信系统通知共享持久化，私聊和个人通知长期保留，公共历史七天到期。';

COMMENT ON COLUMN public.chat_message.id IS '消息雪花主键；排序分页用字符串传输，不依赖主键表达提交先后';

COMMENT ON COLUMN public.chat_message.channel IS 'WORLD、PRIVATE或SYSTEM';

COMMENT ON COLUMN public.chat_message.sender IS '玩家消息的发送角色；系统消息为空';

COMMENT ON COLUMN public.chat_message.sender_name IS '发送时的角色显示名，系统显示系统';

COMMENT ON COLUMN public.chat_message.recipient IS '私聊或个人通知的接收角色；公开消息为空';

COMMENT ON COLUMN public.chat_message.recipient_name IS '私聊收件角色公开名快照';

COMMENT ON COLUMN public.chat_message.text IS '仅保存纯文本，页面禁止作为HTML渲染';

COMMENT ON COLUMN public.chat_message.created_at IS '数据库插入时刻；公共消息七天到期';

COMMENT ON COLUMN public.chat_message.request_id IS '玩家UUID或可信业务来源键，用于防重';

CREATE TABLE public.chat_seen (
    character_id bigint NOT NULL,
    message_id bigint NOT NULL,
    seen_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.chat_seen IS '角色逐消息已读凭证；仅显式展示确认后写入，避免事务提交乱序造成新消息误读。';

COMMENT ON COLUMN public.chat_seen.character_id IS '实际展示并确认消息的角色';

COMMENT ON COLUMN public.chat_seen.message_id IS '逐条消息已读凭证，避免雪花生成与事务提交乱序漏标';

COMMENT ON COLUMN public.chat_seen.seen_at IS '首次阅读时刻；重复确认幂等';

CREATE TABLE public.chat_settings (
    character_id bigint NOT NULL,
    reject_strangers boolean DEFAULT false NOT NULL,
    last_sent_at timestamp with time zone
);

COMMENT ON TABLE public.chat_settings IS '角色级聊天接收偏好与最近成功发送时刻；查询不创建默认行，设置变更不重置频率。';

COMMENT ON COLUMN public.chat_settings.character_id IS '角色级拒收设置；与角色锁内发送状态一起持久化';

COMMENT ON COLUMN public.chat_settings.reject_strangers IS '仅接受已经成为好友的角色私聊';

COMMENT ON COLUMN public.chat_settings.last_sent_at IS '跨频道最近一次成功发送时刻，失败发送不更新';

CREATE TABLE public.collection_receipt (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    code character varying(255) NOT NULL,
    gold integer NOT NULL,
    item_code character varying(255),
    quantity integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT collection_receipt_gold_check CHECK ((gold >= 0)),
    CONSTRAINT collection_receipt_quantity_check CHECK ((quantity >= 0))
);

COMMENT ON TABLE public.collection_receipt IS '账号级一次性图鉴结算；保存实际发放值，不因后来调整目录而改写历史收益。';

COMMENT ON COLUMN public.collection_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.collection_receipt.account_id IS '所属账号主键；凭证按账号隔离，不能由客户端越权指定。';

COMMENT ON COLUMN public.collection_receipt.code IS '账号级图鉴奖励的稳定编号；同账号同编号只结算一次。';

COMMENT ON COLUMN public.collection_receipt.gold IS '本次实际发放的金币数量；保存历史值，不随后续奖励配置调整。';

COMMENT ON COLUMN public.collection_receipt.item_code IS '本次奖励物品编号；无物品奖励时允许为空。';

COMMENT ON COLUMN public.collection_receipt.quantity IS '本次实际发放的物品数量；没有物品奖励时为零。';

COMMENT ON COLUMN public.collection_receipt.created_at IS '首次成功创建时间，使用服务端时间保存；重试不覆盖。';

CREATE TABLE public.combat_vitals (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    hp integer NOT NULL,
    mp integer NOT NULL,
    CONSTRAINT combat_vitals_hp_check CHECK ((hp >= 0)),
    CONSTRAINT combat_vitals_mp_check CHECK ((mp >= 0))
);

COMMENT ON TABLE public.combat_vitals IS '人物或伙伴的剩余生命魔力。上限来自当前成长与装备，升级时不自动增加剩余值。';

COMMENT ON COLUMN public.combat_vitals.id IS '资源所属战斗单位的主键，复用人物或伙伴编号，不另行生成资源记录编号。';

COMMENT ON COLUMN public.combat_vitals.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.combat_vitals.hp IS '当前剩余生命值；零表示倒下，成长或装备变化不能自动恢复生命。';

COMMENT ON COLUMN public.combat_vitals.mp IS '当前剩余魔力值；上限由成长和装备计算，上限增加不自动补满。';

CREATE TABLE public.crafting_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    product character varying(255) NOT NULL,
    bound boolean NOT NULL
);

COMMENT ON TABLE public.crafting_receipt IS '合成结果独立于成品库存数量保存，成品使用后重试旧请求也不会再发一本。';

COMMENT ON COLUMN public.crafting_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.crafting_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.crafting_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.crafting_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.crafting_receipt.product IS '首次合成发放的成品物品编号，重试不重新选择配方结果。';

COMMENT ON COLUMN public.crafting_receipt.bound IS '首次合成结果的绑定状态，独立于成品当前是否仍在背包保存。';

CREATE TABLE public.creature_discovery (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    species character varying(255) NOT NULL,
    ability_slot character varying(255) NOT NULL,
    ability character varying(255) NOT NULL
);

COMMENT ON TABLE public.creature_discovery IS '永久发现事实独立于可转手、可进化的个体；只增不删，不承载当前持有数量。';

COMMENT ON COLUMN public.creature_discovery.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.creature_discovery.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.creature_discovery.species IS '已发现物种的稳定内容编号；个体转手或进化不删除既有发现记录。';

COMMENT ON COLUMN public.creature_discovery.ability_slot IS '已发现的特性槽位 A、B 或 H；H 表示隐藏槽位，不表示必然更强。';

COMMENT ON COLUMN public.creature_discovery.ability IS '该次发现对应的特性名称；两个普通槽位允许使用相同特性。';

CREATE TABLE public.creature_evolution_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.creature_evolution_receipt IS '进化请求及已提交结果与个体、材料同事务持久化，响应丢失后返回原结果。';

COMMENT ON COLUMN public.creature_evolution_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.creature_evolution_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.creature_evolution_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.creature_evolution_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.creature_evolution_receipt.result IS '首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。';

CREATE TABLE public.creature_release_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(255) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.creature_release_receipt IS '放生审计保存玩家确认的完整个体信息；同一请求重试不能重复执行或改换名单。';

COMMENT ON COLUMN public.creature_release_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.creature_release_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.creature_release_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.creature_release_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.creature_release_receipt.created_at IS '首次成功创建时间，使用服务端时间保存；重试不覆盖。';

CREATE TABLE public.creature_rename_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL
);

COMMENT ON TABLE public.creature_rename_receipt IS '改名凭证与金币和名称同事务保存，避免响应丢失后重复收费。';

COMMENT ON COLUMN public.creature_rename_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.creature_rename_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.creature_rename_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.creature_rename_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

CREATE TABLE public.cultivation_reward_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    event_key character varying(160) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.cultivation_reward_receipt IS '同一来源完成事件对每名参与者仅结算一次；无掉落也保存凭证，不能重试刷书。';

COMMENT ON COLUMN public.cultivation_reward_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.cultivation_reward_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.cultivation_reward_receipt.event_key IS '来源类型与完成事件编号组成的幂等键；同一事件对每名参与者最多结算一次。';

COMMENT ON COLUMN public.cultivation_reward_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.cultivation_reward_receipt.result IS '首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。';

CREATE TABLE public.device_session (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    label character varying(80) NOT NULL,
    access_hash character varying(64) NOT NULL,
    refresh_hash character varying(64) NOT NULL,
    access_until timestamp with time zone NOT NULL,
    refresh_until timestamp with time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL
);

COMMENT ON TABLE public.device_session IS '每次登录独立创建设备会话。原始 Token 只在签发响应中出现，数据库仅保留摘要。';

COMMENT ON COLUMN public.device_session.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.device_session.account_id IS '不可变的账号归属。查询必须使用认证身份限定此字段，不能信任客户端提交的账号编号。';

COMMENT ON COLUMN public.device_session.label IS '设备显示名称，仅用于玩家辨认会话，不作为设备可信度或授权依据。';

COMMENT ON COLUMN public.device_session.access_hash IS '当前访问令牌的 SHA-256 摘要；轮换会替换摘要，旧访问令牌随即不能匹配。';

COMMENT ON COLUMN public.device_session.refresh_hash IS '当前刷新令牌摘要；被替换的摘要保存在刷新凭证表，用于检测严格重放。';

COMMENT ON COLUMN public.device_session.access_until IS '访问令牌截止时间，以服务端时钟判断；等于当前时间即视为过期。';

COMMENT ON COLUMN public.device_session.refresh_until IS '设备会话的绝对截止时间，刷新不会无限延长；访问令牌也不得越过此截止时间使用。';

COMMENT ON COLUMN public.device_session.revoked IS '显式撤销标记。严格刷新重放也会设置此标记，撤销必须在返回401前提交。';

CREATE TABLE public.dungeon_battle (
    id bigint NOT NULL,
    active boolean NOT NULL,
    deadline timestamp with time zone NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.dungeon_battle IS '一场多人遭遇；截止时间另存可索引列，存档仍是恢复完整回合的唯一来源。';

COMMENT ON COLUMN public.dungeon_battle.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_battle.active IS '是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。';

COMMENT ON COLUMN public.dungeon_battle.deadline IS '服务端处理超时的截止时间；独立列用于调度查询，避免扫描 JSON 存档。';

COMMENT ON COLUMN public.dungeon_battle.state IS '完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。';

CREATE TABLE public.dungeon_battle_member (
    battle_id bigint NOT NULL,
    character_id bigint NOT NULL
);

COMMENT ON TABLE public.dungeon_battle_member IS '副本战局与参战角色的关联表，用于归属校验和重连定位。';

COMMENT ON COLUMN public.dungeon_battle_member.battle_id IS '参与的副本战局主键。';

COMMENT ON COLUMN public.dungeon_battle_member.character_id IS '参战角色主键，与战局编号组成唯一成员关系。';

CREATE TABLE public.dungeon_chest (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    run_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    loot text NOT NULL,
    claimed boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.dungeon_chest IS '宝箱是绑定角色的领取凭证；抽取结果在胜利事务内保存，领取后仍保留防重记录。';

COMMENT ON COLUMN public.dungeon_chest.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_chest.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.dungeon_chest.run_id IS '所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。';

COMMENT ON COLUMN public.dungeon_chest.name IS '宝箱展示名称；防重使用副本进度编号，不依赖名称。';

COMMENT ON COLUMN public.dungeon_chest.loot IS '胜利时固定的奖励清单 JSON；领取只发放既定结果，不再次随机抽取。';

COMMENT ON COLUMN public.dungeon_chest.claimed IS '是否已经完成领取；领取和发奖同事务，保留记录防止重复领取。';

COMMENT ON COLUMN public.dungeon_chest.created_at IS '首次成功创建时间，使用服务端时间保存；重试不覆盖。';

CREATE TABLE public.dungeon_daily_start (
    character_id bigint NOT NULL,
    dungeon character varying(64) NOT NULL,
    day date NOT NULL,
    starts integer NOT NULL,
    CONSTRAINT dungeon_daily_start_starts_check CHECK ((starts > 0))
);

COMMENT ON TABLE public.dungeon_daily_start IS '角色每个副本的每日发起次数，仅记录发起者消耗。';

COMMENT ON COLUMN public.dungeon_daily_start.character_id IS '消耗每日发起次数的角色主键。';

COMMENT ON COLUMN public.dungeon_daily_start.dungeon IS '独立计数的副本标识。';

COMMENT ON COLUMN public.dungeon_daily_start.day IS '副本次数归属的业务日期，由服务端每日重置规则确定。';

COMMENT ON COLUMN public.dungeon_daily_start.starts IS '该角色当日已经使用的发起次数；普通队员不消耗此计数。';

CREATE TABLE public.dungeon_invitation (
    id bigint NOT NULL,
    lobby_id bigint NOT NULL,
    sender_id bigint NOT NULL,
    recipient_id bigint NOT NULL,
    request_id character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    status character varying(12) NOT NULL,
    CONSTRAINT dungeon_invitation_check CHECK ((sender_id <> recipient_id)),
    CONSTRAINT dungeon_invitation_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'DECLINED'::character varying, 'EXPIRED'::character varying])::text[])))
);

COMMENT ON TABLE public.dungeon_invitation IS '邀请收件箱与队伍占位分离；只有接受事务才调用正式入队命令。';

COMMENT ON COLUMN public.dungeon_invitation.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_invitation.lobby_id IS '受邀加入的副本队伍主键；接受时仍需校验队伍有效性和容量。';

COMMENT ON COLUMN public.dungeon_invitation.sender_id IS '邀请发起者的角色主键；用于验证发送资格与展示邀请来源。';

COMMENT ON COLUMN public.dungeon_invitation.recipient_id IS '邀请接收者的角色主键；仅该角色可接受或拒绝邀请。';

COMMENT ON COLUMN public.dungeon_invitation.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.dungeon_invitation.expires_at IS '邀请失效时间；达到该时间后不能接受，不因查看而延长。';

COMMENT ON COLUMN public.dungeon_invitation.status IS '邀请状态：PENDING 待处理、ACCEPTED 已接受、DECLINED 已拒绝、EXPIRED 已过期。';

CREATE TABLE public.dungeon_lobby (
    id bigint NOT NULL,
    leader_id bigint NOT NULL,
    request_id character varying(64) NOT NULL,
    open boolean NOT NULL,
    deadline timestamp with time zone,
    state text NOT NULL
);

COMMENT ON TABLE public.dungeon_lobby IS '最早成员截止时间独立索引，避免 Quartz 扫描全部 JSON 存档。';

COMMENT ON COLUMN public.dungeon_lobby.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_lobby.leader_id IS '发起副本或队伍的角色主键，不是账号编号。';

COMMENT ON COLUMN public.dungeon_lobby.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.dungeon_lobby.open IS '队伍是否仍开放；关闭后的历史记录不能继续接受入队。';

COMMENT ON COLUMN public.dungeon_lobby.deadline IS '最早成员的确认截止时间；无待超时占位时允许为空，用于定时清理查询。';

COMMENT ON COLUMN public.dungeon_lobby.state IS '完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。';

CREATE TABLE public.dungeon_run (
    id bigint NOT NULL,
    leader_id bigint NOT NULL,
    request_id character varying(64) NOT NULL,
    active boolean NOT NULL,
    active_battle_id bigint,
    state text NOT NULL
);

COMMENT ON TABLE public.dungeon_run IS '三场遭遇共用一个进度锁；单场战局仍有独立存档与提交凭证。';

COMMENT ON COLUMN public.dungeon_run.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_run.leader_id IS '发起副本或队伍的角色主键，不是账号编号。';

COMMENT ON COLUMN public.dungeon_run.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.dungeon_run.active IS '是否仍处于有效状态；结束后保留历史记录，不再允许继续操作。';

COMMENT ON COLUMN public.dungeon_run.active_battle_id IS '当前进行中的战局主键；战间为空，副本进度仍可保持有效。';

COMMENT ON COLUMN public.dungeon_run.state IS '完整运行状态的 JSON 文本，用于重连恢复；仅由对应应用命令更新。';

CREATE TABLE public.dungeon_submission (
    id bigint NOT NULL,
    battle_id bigint NOT NULL,
    character_id bigint NOT NULL,
    round integer NOT NULL,
    payload text NOT NULL,
    request_id character varying(36) NOT NULL,
    CONSTRAINT dungeon_submission_round_check CHECK ((round > 0))
);

COMMENT ON TABLE public.dungeon_submission IS '每人每轮的不可变提交凭证，保存完整输入以区分安全重试与异参覆盖。';

COMMENT ON COLUMN public.dungeon_submission.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_submission.battle_id IS '所属战局主键；与独立副本进度编号区分。';

COMMENT ON COLUMN public.dungeon_submission.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.dungeon_submission.round IS '战局回合序号；同一战局内按回合保存，重试不得再次执行。';

COMMENT ON COLUMN public.dungeon_submission.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.dungeon_submission.request_id IS '成员提交命令的UUID请求号，重试保持不变';

CREATE TABLE public.dungeon_supply (
    id bigint NOT NULL,
    scope_id bigint CONSTRAINT dungeon_supply_scope_id_not_null1 NOT NULL,
    item_id bigint NOT NULL,
    remaining integer NOT NULL,
    CONSTRAINT dungeon_supply_remaining_check CHECK ((remaining >= 0))
);

COMMENT ON TABLE public.dungeon_supply IS '实例级可用额度。实际能用多少还须取真实背包数量的较小值，待领数量不能直接使用。';

COMMENT ON COLUMN public.dungeon_supply.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_supply.scope_id IS '所属入本补给登记主键，同一登记内按物品实例保存额度。';

COMMENT ON COLUMN public.dungeon_supply.item_id IS '对应物品实例主键；不能用物品模板编号替代。';

COMMENT ON COLUMN public.dungeon_supply.remaining IS '入本额度中尚未使用的数量；实际可用量还受背包实时库存限制。';

CREATE TABLE public.dungeon_supply_scope (
    id bigint NOT NULL,
    run_id bigint NOT NULL,
    character_id bigint NOT NULL,
    active boolean NOT NULL,
    CONSTRAINT dungeon_supply_scope_run_id_check CHECK ((run_id > 0))
);

COMMENT ON TABLE public.dungeon_supply_scope IS '入本登记即使没有药品也必须存在，防止重连后按新库存重新登记。';

COMMENT ON COLUMN public.dungeon_supply_scope.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.dungeon_supply_scope.run_id IS '所属副本进度主键；多场遭遇共用同一进度，重试不另建领取资格。';

COMMENT ON COLUMN public.dungeon_supply_scope.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.dungeon_supply_scope.active IS '入本补给登记是否仍有效；退出副本后失效，重连不能按新库存补登记。';

CREATE TABLE public.enhancement_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.enhancement_receipt IS '强化结果与扣款扣料同事务保存，响应丢失时返回原结果，不重新抽签。';

COMMENT ON COLUMN public.enhancement_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.enhancement_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.enhancement_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.enhancement_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.enhancement_receipt.result IS '首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。';

CREATE TABLE public.equipment_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.equipment_receipt IS '买入、卖出及销毁的共同防重凭证；保存完整确认参数，禁止同请求号换物品或换动作。';

COMMENT ON COLUMN public.equipment_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.equipment_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.equipment_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.equipment_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.equipment_receipt.created_at IS '首次成功创建时间，使用服务端时间保存；重试不覆盖。';

CREATE TABLE public.fishing_session (
    character_id bigint NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.fishing_session IS '服务端权威钓鱼状态及最近一局结果';

COMMENT ON COLUMN public.fishing_session.character_id IS '钓鱼角色';

COMMENT ON COLUMN public.fishing_session.state IS '固定步长进度、操作序号、鱼种、鱼竿和终局结果';

CREATE TABLE public.friendship (
    id bigint NOT NULL,
    first_character_id bigint NOT NULL,
    second_character_id bigint NOT NULL,
    requester_id bigint NOT NULL,
    accepted boolean DEFAULT false NOT NULL,
    CONSTRAINT friendship_check CHECK ((first_character_id < second_character_id)),
    CONSTRAINT friendship_check1 CHECK (((requester_id = first_character_id) OR (requester_id = second_character_id)))
);

COMMENT ON TABLE public.friendship IS '一对角色只保存一条关系；申请与已接受状态共用记录，删除后重新申请会分配新的雪花编号。';

COMMENT ON COLUMN public.friendship.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.friendship.first_character_id IS '按角色编号排序，防止 A 到 B 与 B 到 A 各生成一条关系。';

COMMENT ON COLUMN public.friendship.second_character_id IS '关系双方中较大的角色主键；与 firstCharacterId 组成无方向的唯一角色对。';

COMMENT ON COLUMN public.friendship.requester_id IS '发起好友申请的角色主键，必须属于关系双方。';

COMMENT ON COLUMN public.friendship.accepted IS '是否已接受申请；false 为待处理申请，true 为双向好友关系。';

CREATE TABLE public.gathering_session (
    character_id bigint NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.gathering_session IS '当前在线采集会话，角色锁下推进，断线不补算';

COMMENT ON COLUMN public.gathering_session.character_id IS '唯一角色归属，一次只能派遣一只伙伴';

COMMENT ON COLUMN public.gathering_session.state IS '会话编号、资源点、伙伴工具、剩余备用、已确认进度及心跳版本';

CREATE TABLE public.inventory_claim (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    item_id bigint NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT inventory_claim_quantity_check CHECK ((quantity > 0))
);

COMMENT ON TABLE public.inventory_claim IS '部分领取的持久化凭证，刷新或重复提交同一请求号不得继续扣减待领数量。';

COMMENT ON COLUMN public.inventory_claim.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.inventory_claim.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.inventory_claim.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.inventory_claim.item_id IS '对应物品实例主键；不能用物品模板编号替代。';

COMMENT ON COLUMN public.inventory_claim.quantity IS '本次实际从待领转入可用背包的数量；重复请求不继续领取剩余部分。';

CREATE TABLE public.inventory_grant (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    source character varying(255) NOT NULL
);

COMMENT ON TABLE public.inventory_grant IS '原始发放凭证不随实例转手、拆分或托管改变；奖励防重不再依赖当前持有者。';

COMMENT ON COLUMN public.inventory_grant.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.inventory_grant.character_id IS '首次获得奖励的角色主键；实例转手后仍保留原始发放对象。';

COMMENT ON COLUMN public.inventory_grant.source IS '原始发放来源键；与首次获得角色组成唯一键，不因消耗、拆分或交易改变。';

CREATE TABLE public.life_operation_receipt (
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.life_operation_receipt IS '生活生产操作回执，结果与材料消耗和工具报废原子提交';

COMMENT ON COLUMN public.life_operation_receipt.character_id IS '操作角色';

COMMENT ON COLUMN public.life_operation_receipt.request_id IS '标准UUID请求号';

COMMENT ON COLUMN public.life_operation_receipt.payload IS '排序后规范化请求内容，防止同请求号更换材料';

COMMENT ON COLUMN public.life_operation_receipt.result IS '已提交结果，重试不重新抽取工具报废';

CREATE TABLE public.life_skill (
    character_id bigint NOT NULL,
    skill character varying(24) NOT NULL,
    half_points bigint DEFAULT 0 NOT NULL,
    CONSTRAINT life_skill_points_check CHECK (((half_points >= 0) AND (half_points <= 130816)))
);

COMMENT ON TABLE public.life_skill IS '角色生活技能；角色锁约束最多三个专业，半点熟练度保持衰减精度';

COMMENT ON COLUMN public.life_skill.character_id IS '角色归属';

COMMENT ON COLUMN public.life_skill.skill IS '生活技能稳定编号';

COMMENT ON COLUMN public.life_skill.half_points IS '累计半点熟练度，最高130816对应65408点';

CREATE TABLE public.life_skill_receipt (
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    action character varying(16) NOT NULL,
    skill character varying(24) NOT NULL
);

COMMENT ON TABLE public.life_skill_receipt IS '学习与遗忘请求回执，重放不误删重新学习的专业';

COMMENT ON COLUMN public.life_skill_receipt.character_id IS '经认证解析的角色归属';

COMMENT ON COLUMN public.life_skill_receipt.request_id IS '标准UUID幂等请求号';

COMMENT ON COLUMN public.life_skill_receipt.action IS 'LEARN或FORGET';

COMMENT ON COLUMN public.life_skill_receipt.skill IS '本次请求的技能编号';

CREATE TABLE public.market_listing (
    id bigint NOT NULL,
    seller_account_id bigint NOT NULL,
    asset_id bigint NOT NULL,
    kind character varying(16) NOT NULL,
    state character varying(24) NOT NULL,
    unit_price bigint NOT NULL,
    original_quantity integer NOT NULL,
    remaining integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    item_code character varying(255),
    item_name character varying(255),
    item_slot character varying(255),
    item_quality character varying(255),
    item_level integer,
    item_category character varying(16),
    creature_hidden boolean,
    enhancement_level integer,
    CONSTRAINT market_enhancement_range CHECK (((enhancement_level IS NULL) OR (((kind)::text = 'ITEM'::text) AND ((item_category)::text = 'EQUIPMENT'::text) AND ((enhancement_level >= 0) AND (enhancement_level <= 7))))),
    CONSTRAINT market_listing_values_check CHECK ((((kind)::text = ANY ((ARRAY['ITEM'::character varying, 'CREATURE'::character varying])::text[])) AND ((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SOLD'::character varying, 'RETURN_PENDING'::character varying, 'RETURNED'::character varying])::text[])) AND (unit_price >= 2) AND (original_quantity > 0) AND ((remaining >= 0) AND (remaining <= original_quantity)) AND (expires_at > created_at) AND (((kind)::text <> 'CREATURE'::text) OR (original_quantity = 1)) AND ((((state)::text = ANY ((ARRAY['SOLD'::character varying, 'RETURNED'::character varying])::text[])) AND (remaining = 0)) OR (((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RETURN_PENDING'::character varying])::text[])) AND (remaining > 0)))))
);

COMMENT ON TABLE public.market_listing IS '挂单保存托管数量和固定截止时间；部分成交只减少 remaining，不刷新期限。 实物保持所属模块管理，assetId 指向托管实例；跨模块不建立 JPA 关联。';

COMMENT ON COLUMN public.market_listing.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.market_listing.seller_account_id IS '卖家账号主键；资金结算和跨账号锁按账号编号处理。';

COMMENT ON COLUMN public.market_listing.asset_id IS '托管实物的实例主键；根据 kind 指向物品或伙伴，不建立跨模块实体关联。';

COMMENT ON COLUMN public.market_listing.kind IS '托管实物类型：ITEM 为物品或装备，CREATURE 为伙伴。';

COMMENT ON COLUMN public.market_listing.state IS '挂单状态：ACTIVE 在售、SOLD 售罄、RETURN_PENDING 待领退货、RETURNED 已领退货。';

COMMENT ON COLUMN public.market_listing.unit_price IS '每件商品的金币单价；成交按数量计算总价，再按规则扣税。';

COMMENT ON COLUMN public.market_listing.original_quantity IS '首次上架的总数量；部分成交不修改该历史值。';

COMMENT ON COLUMN public.market_listing.remaining IS '尚未售出或领取退货的托管数量；降为零后挂单进入终态。';

COMMENT ON COLUMN public.market_listing.created_at IS '首次成功创建时间，使用服务端时间保存；重试不覆盖。';

COMMENT ON COLUMN public.market_listing.expires_at IS '上架时固定的失效时间；部分成交和重连不刷新期限。';

COMMENT ON COLUMN public.market_listing.item_code IS '上架时从已核对实物建立查询索引；托管期间不能培养或修改实例属性。';

COMMENT ON COLUMN public.market_listing.item_name IS '上架时的物品名称或伙伴昵称，用于市场展示和名称检索。';

COMMENT ON COLUMN public.market_listing.item_slot IS '上架装备的部位编号；非装备为空，不能作为装备匹配。';

COMMENT ON COLUMN public.market_listing.item_quality IS '上架装备的品质标识；没有品质的商品为空。';

COMMENT ON COLUMN public.market_listing.item_level IS '上架装备的穿戴等级或伙伴等级；按商品类型解释，用于范围筛选。';

COMMENT ON COLUMN public.market_listing.item_category IS '检索分类：EQUIPMENT 装备、BOOK 技能书、FRAGMENT 碎片、MATERIAL 材料、CREATURE 伙伴。';

COMMENT ON COLUMN public.market_listing.creature_hidden IS '上架伙伴是否为 H 隐藏特性槽位；非伙伴商品为空。';

COMMENT ON COLUMN public.market_listing.enhancement_level IS '装备独有的上架时等级；非装备为空，不能混入“未强化装备”筛选。';

CREATE TABLE public.market_listing_item (
    listing_id bigint NOT NULL,
    asset_id bigint NOT NULL,
    remaining integer NOT NULL,
    CONSTRAINT market_listing_item_remaining_check CHECK ((remaining > 0))
);

COMMENT ON TABLE public.market_listing_item IS '物品挂单按奖励来源分别保存托管实例与剩余数量';

COMMENT ON COLUMN public.market_listing_item.listing_id IS '所属市场挂单';

COMMENT ON COLUMN public.market_listing_item.asset_id IS '库存模块托管实例，保留原奖励出处';

COMMENT ON COLUMN public.market_listing_item.remaining IS '该来源尚未成交或领取退货的数量';

CREATE TABLE public.market_listing_skill (
    listing_id bigint NOT NULL,
    skill character varying(255) NOT NULL
);

COMMENT ON TABLE public.market_listing_skill IS '上架伙伴的技能检索集合；按完整技能名匹配，随挂单实物建立。';

COMMENT ON COLUMN public.market_listing_skill.listing_id IS '对应市场挂单主键，挂单与技能名共同组成主键。';

COMMENT ON COLUMN public.market_listing_skill.skill IS '上架时已学技能的完整名称；用于精确匹配查询。';

CREATE TABLE public.market_receipt (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    operation character varying(255) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.market_receipt IS '市场操作回执和资金、实物同事务保存；同账号请求号跨操作唯一。';

COMMENT ON COLUMN public.market_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.market_receipt.account_id IS '所属账号主键；凭证按账号隔离，不能由客户端越权指定。';

COMMENT ON COLUMN public.market_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.market_receipt.operation IS '市场操作类型标识；与负载共同核验，禁止同一请求号重放为另一种操作。';

COMMENT ON COLUMN public.market_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.market_receipt.result IS '首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。';

CREATE TABLE public.owned_creature (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    source character varying(255) NOT NULL,
    species character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    element character varying(255) NOT NULL,
    level integer NOT NULL,
    experience bigint NOT NULL,
    ability_slot character varying(255) NOT NULL,
    ability character varying(255) NOT NULL,
    skills character varying(255) NOT NULL,
    party_position integer,
    locked boolean DEFAULT false NOT NULL,
    released_at timestamp with time zone,
    rename_count integer DEFAULT 0 NOT NULL,
    origin_character_id bigint,
    captor_character_id bigint,
    origin_kind character varying(16) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    bound boolean DEFAULT true NOT NULL,
    custom_name boolean DEFAULT false NOT NULL,
    in_custody boolean DEFAULT false NOT NULL,
    CONSTRAINT creature_origin_kind_check CHECK (((origin_kind)::text = ANY ((ARRAY['UNKNOWN'::character varying, 'GIFT'::character varying, 'CAPTURE'::character varying])::text[]))),
    CONSTRAINT creature_release_position CHECK ((((released_at IS NULL) AND (NOT in_custody) AND (party_position IS NOT NULL)) OR ((released_at IS NOT NULL) AND (NOT in_custody) AND (party_position IS NULL)) OR ((released_at IS NULL) AND in_custody AND (party_position IS NULL) AND (NOT bound) AND (NOT locked)))),
    CONSTRAINT owned_creature_level_check CHECK (((level >= 1) AND (level <= 50))),
    CONSTRAINT owned_creature_rename_count_check CHECK ((rename_count >= 0))
);

COMMENT ON TABLE public.owned_creature IS '契约宠物实例，特性槽位在生成时确定，技能独立保存以便后续学习与遗忘。';

COMMENT ON COLUMN public.owned_creature.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.owned_creature.character_id IS '所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。';

COMMENT ON COLUMN public.owned_creature.source IS '奖励来源幂等键，当前为步骤编号加效果顺序；同一角色与来源组合只能生成一次实例。';

COMMENT ON COLUMN public.owned_creature.species IS '物种目录的稳定编号。用于关联模板，实例等级、技能和特性仍独立持久化。';

COMMENT ON COLUMN public.owned_creature.name IS '实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。';

COMMENT ON COLUMN public.owned_creature.element IS '实例元素名称，来自启动校验后的模板。当前不在此实体中计算属性相克倍率。';

COMMENT ON COLUMN public.owned_creature.level IS '实例等级，赠宠创建时取模板值，不根据加入前人物经验追赶等级。';

COMMENT ON COLUMN public.owned_creature.experience IS '累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。';

COMMENT ON COLUMN public.owned_creature.ability_slot IS '生成时确定的 A/B/H 槽位。保存槽位本身，以便后续进化维持槽位语义。';

COMMENT ON COLUMN public.owned_creature.ability IS '该实例当前特性名称；不能仅凭是否隐藏槽位推断效果更强。';

COMMENT ON COLUMN public.owned_creature.skills IS '实例技能列表的 JSON 文本，由持久化 codec 转换，顺序和内容不能由客户端任意覆盖。';

COMMENT ON COLUMN public.owned_creature.party_position IS '从零开始的随身位置；同一角色不能重复占位，容量与出战数量是两种不同约束。';

COMMENT ON COLUMN public.owned_creature.locked IS '玩家主动保护的个体不能放生；锁定不限制正常编队和学习。';

COMMENT ON COLUMN public.owned_creature.released_at IS '放生只释放占位，保留来源、物种和已发现特性，防止历史来源重新发宠。';

COMMENT ON COLUMN public.owned_creature.rename_count IS '成功更名次数随个体保存；不由名称是否等于物种默认名推算免费资格。';

COMMENT ON COLUMN public.owned_creature.origin_character_id IS '首次获得者不随交易变更，用于保持来源防重。来源身份独立于当前所有权。';

COMMENT ON COLUMN public.owned_creature.captor_character_id IS '仅亲自捕获才保存捕获者；任务赠宠没有捕获者，不能把当前持有者冒充捕获者。';

COMMENT ON COLUMN public.owned_creature.origin_kind IS '初始来源类别在创建时固定；进化、交易不覆盖原始来源。';

COMMENT ON COLUMN public.owned_creature.bound IS '未知来源默认绑定；捕获入口明确设为未绑定，后续绑定材料只允许将其改为绑定。';

COMMENT ON COLUMN public.owned_creature.custom_name IS '显式记录是否自定义过昵称，进化不能凭名称与默认名相等就误覆盖玩家昵称。';

COMMENT ON COLUMN public.owned_creature.in_custody IS '市场托管不占编队或仓库位置，禁止所有普通培养和管理命令访问。';

CREATE TABLE public.owned_item (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    source character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    quantity integer NOT NULL,
    slot character varying(255),
    required_level integer NOT NULL,
    stats character varying(255) NOT NULL,
    equipped boolean NOT NULL,
    bound boolean NOT NULL,
    pending_quantity integer DEFAULT 0 NOT NULL,
    quality character varying(16),
    sell_price integer DEFAULT 0 NOT NULL,
    origin_grant_id bigint,
    custody_quantity integer DEFAULT 0 NOT NULL,
    enhancement_level integer DEFAULT 0 NOT NULL,
    CONSTRAINT owned_item_custody_check CHECK (((custody_quantity >= 0) AND ((custody_quantity = 0) OR ((quantity = 0) AND (pending_quantity = 0) AND (equipped = false) AND (bound = false))))),
    CONSTRAINT owned_item_enhancement_range CHECK ((((enhancement_level >= 0) AND (enhancement_level <= 7)) AND ((slot IS NOT NULL) OR (enhancement_level = 0)))),
    CONSTRAINT owned_item_pending_quantity_check CHECK ((pending_quantity >= 0)),
    CONSTRAINT owned_item_quantity_check CHECK ((quantity >= 0)),
    CONSTRAINT owned_item_sell_price_check CHECK ((sell_price >= 0))
);

COMMENT ON TABLE public.owned_item IS '按来源保存任务物品与装备实例；同一奖励来源唯一，防止重复发放。';

COMMENT ON COLUMN public.owned_item.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.owned_item.character_id IS '所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。';

COMMENT ON COLUMN public.owned_item.source IS '本次取得来源，任务发放为步骤加效果序号，转手后由成交来源替换；原始出处由 originGrantId 保留。';

COMMENT ON COLUMN public.owned_item.code IS '内容目录中的物品编号。用于交付匹配；显示名称不能替代稳定编号参与业务判断。';

COMMENT ON COLUMN public.owned_item.name IS '实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。';

COMMENT ON COLUMN public.owned_item.quantity IS '当前数量，数据库约束禁止负数。交付后为零仍保留来源记录，不删除防重依据。';

COMMENT ON COLUMN public.owned_item.slot IS '装备部位；任务物品为 null，不能通过穿戴命令转换为装备。';

COMMENT ON COLUMN public.owned_item.required_level IS '穿戴所需人物等级，校验时读取当前持锁角色等级而非客户端值。';

COMMENT ON COLUMN public.owned_item.stats IS '装备原始属性的 JSON 存储文本，强化不能覆盖此值；展示和穿戴按强化等级重新计算。';

COMMENT ON COLUMN public.owned_item.equipped IS '是否占用装备部位。切换装备须先 flush 卸下记录，再穿戴新实例以满足部分唯一索引。';

COMMENT ON COLUMN public.owned_item.bound IS '实例绑定状态；由实际来源和培养消耗决定，转手不能解除绑定。';

COMMENT ON COLUMN public.owned_item.pending_quantity IS '已固定但尚未进入背包的数量，不能消耗或穿戴；部分领取只在两个数量之间转移。';

COMMENT ON COLUMN public.owned_item.quality IS '获得时固定的品质标识，不另乘属性；卖价由来源决定，零表示不允许出售。';

COMMENT ON COLUMN public.owned_item.sell_price IS '该实例出售给 NPC 的金币价格；按取得来源固定，零表示禁止出售，不是市场售价。';

COMMENT ON COLUMN public.owned_item.origin_grant_id IS '原始发放凭证随转手或堆叠拆分保留，独立于本次取得来源；外部历史导入未知时允许为空。';

COMMENT ON COLUMN public.owned_item.custody_quantity IS '市场托管数量独立于可用和普通待领库存，任何普通消耗入口都不能使用。';

COMMENT ON COLUMN public.owned_item.enhancement_level IS '当前强化等级独立保存；失败掉级仍从原始属性计算，不累乘或反向除算。';

CREATE TABLE public.personal_farm (
    character_id bigint NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.personal_farm IS '每角色唯一农田，村庄与诺姆城共享，按现实时间生长';

COMMENT ON COLUMN public.personal_farm.character_id IS '农田所属角色';

COMMENT ON COLUMN public.personal_farm.state IS '已扩建格数、作物、种子绑定及首次浇水时间';

CREATE TABLE public.player_character (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    name character varying(32) NOT NULL,
    profession character varying(32) NOT NULL,
    location character varying(64) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    experience bigint DEFAULT 0 NOT NULL,
    active_battle_id bigint,
    gold bigint DEFAULT 0 NOT NULL,
    last_move_revision bigint,
    active_dungeon_id bigint,
    preparation_revision bigint DEFAULT 0 NOT NULL,
    dungeon_lobby_id bigint,
    dungeon_hold_until timestamp with time zone,
    active_course_id bigint,
    active_life_session character varying(36),
    dispatched_creature_id bigint,
    CONSTRAINT life_dispatch_requires_session CHECK (((dispatched_creature_id IS NULL) OR (active_life_session IS NOT NULL))),
    CONSTRAINT player_character_experience_check CHECK (((experience >= 0) AND (experience <= 23935620))),
    CONSTRAINT player_character_gold_check CHECK ((gold >= 0))
);

COMMENT ON TABLE public.player_character IS '角色持久化状态。职业创建后不提供修改入口，位置仅由校验相邻关系的服务变更。';

COMMENT ON COLUMN public.player_character.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.player_character.account_id IS '不可变的账号归属。查询必须使用认证身份限定此字段，不能信任客户端提交的账号编号。';

COMMENT ON COLUMN public.player_character.name IS '实例显示名称。是否要求唯一由对应数据库表约束决定，业务引用使用稳定编号或主键。';

COMMENT ON COLUMN public.player_character.profession IS '角色创建时选择的职业编号。当前设计不允许改职业，服务端不会信任客户端重选请求。';

COMMENT ON COLUMN public.player_character.location IS '当前地点的内容编号。移动仅允许经过地图出口，修改 URL 不改变此状态。';

COMMENT ON COLUMN public.player_character.version IS 'Hibernate 乐观版本。位置命令还会比较客户端预期 revision，拒绝旧页面重复移动。';

COMMENT ON COLUMN public.player_character.experience IS '累计经验；升级不清零。当前试玩成长门槛由 GrowthRules 唯一定义。';

COMMENT ON COLUMN public.player_character.active_battle_id IS '当前战局编号。只由战斗用例开启和清除，战外命令在角色锁内检查。';

COMMENT ON COLUMN public.player_character.gold IS '金币余额，奖励和消费必须与对应凭证处于同一事务。';

COMMENT ON COLUMN public.player_character.last_move_revision IS '最近成功移动的客户端版本；剧情救援或战败传送后清空，不能重放旧道路请求。';

COMMENT ON COLUMN public.player_character.active_dungeon_id IS '战间仍持有副本门禁；与单场战局编号分开，失败重试不会释放编队。';

COMMENT ON COLUMN public.player_character.preparation_revision IS '装备或战间资源发生变化时递增；副本准备记录必须与此修订号一致。';

COMMENT ON COLUMN public.player_character.dungeon_lobby_id IS '匹配占位与正式副本分离；确认期限内只限制新开个人战斗，不中断当前战局。';

COMMENT ON COLUMN public.player_character.dungeon_hold_until IS '副本组队确认占位的截止时间；没有占位时为空，超时由组队流程释放。';

COMMENT ON COLUMN public.player_character.active_course_id IS '当前隔离课程会话占用；课程结束或退出后清空';

COMMENT ON COLUMN public.player_character.active_life_session IS '当前在线采集或钓鱼会话，结束前禁止移动与开战';

COMMENT ON COLUMN public.player_character.dispatched_creature_id IS '当前派遣的伙伴编号，钓鱼会话为空';

CREATE TABLE public.pvp_battle (
    id bigint NOT NULL,
    attacker_id bigint NOT NULL,
    defender_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    active boolean NOT NULL,
    state text NOT NULL,
    CONSTRAINT ck_pvp_distinct CHECK ((attacker_id <> defender_id))
);

COMMENT ON TABLE public.pvp_battle IS '训练师共享回合对战';

COMMENT ON COLUMN public.pvp_battle.id IS '对战编号';

COMMENT ON COLUMN public.pvp_battle.attacker_id IS '发起方人物编号';

COMMENT ON COLUMN public.pvp_battle.defender_id IS '受袭方人物编号';

COMMENT ON COLUMN public.pvp_battle.request_id IS '发起请求幂等编号';

COMMENT ON COLUMN public.pvp_battle.active IS '是否仍在对战';

COMMENT ON COLUMN public.pvp_battle.state IS '双方战斗状态与本轮行动快照';

CREATE TABLE public.pvp_command_receipt (
    id bigint NOT NULL,
    battle_id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    response text NOT NULL
);

COMMENT ON TABLE public.pvp_command_receipt IS '训练师对战操作幂等回执';

COMMENT ON COLUMN public.pvp_command_receipt.id IS '回执编号';

COMMENT ON COLUMN public.pvp_command_receipt.battle_id IS '所属对战编号';

COMMENT ON COLUMN public.pvp_command_receipt.character_id IS '操作人物编号';

COMMENT ON COLUMN public.pvp_command_receipt.request_id IS '操作请求幂等编号';

COMMENT ON COLUMN public.pvp_command_receipt.payload IS '首次请求载荷';

COMMENT ON COLUMN public.pvp_command_receipt.response IS '首次响应快照';

CREATE TABLE public.pvp_protection (
    id bigint NOT NULL,
    expires_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.pvp_protection IS '人物免战保护';

COMMENT ON COLUMN public.pvp_protection.id IS '受保护人物编号';

COMMENT ON COLUMN public.pvp_protection.expires_at IS '保护到期时间';

CREATE TABLE public.pvp_protection_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    expires_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.pvp_protection_receipt IS '免战牌使用幂等回执';

COMMENT ON COLUMN public.pvp_protection_receipt.id IS '回执编号';

COMMENT ON COLUMN public.pvp_protection_receipt.character_id IS '使用人物编号';

COMMENT ON COLUMN public.pvp_protection_receipt.request_id IS '使用请求幂等编号';

COMMENT ON COLUMN public.pvp_protection_receipt.expires_at IS '首次使用确定的到期时间';

CREATE TABLE public.quest_accept_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    step character varying(255) NOT NULL,
    payload text NOT NULL,
    accepted_at timestamp with time zone NOT NULL,
    response text NOT NULL,
    runtime_id character varying(36) NOT NULL,
    runtime_round integer DEFAULT 1 NOT NULL
);

COMMENT ON TABLE public.quest_accept_receipt IS '任务显式接取回执，补给与接取事实同事务，独立于完成奖励';

COMMENT ON COLUMN public.quest_accept_receipt.id IS 'CosId雪花主键';

COMMENT ON COLUMN public.quest_accept_receipt.character_id IS '接取任务的角色归属';

COMMENT ON COLUMN public.quest_accept_receipt.request_id IS '同一角色内唯一标准UUID请求号';

COMMENT ON COLUMN public.quest_accept_receipt.step IS '接取的稳定任务步骤编号';

COMMENT ON COLUMN public.quest_accept_receipt.payload IS '原步骤与位置版本，重复请求需完全一致';

COMMENT ON COLUMN public.quest_accept_receipt.accepted_at IS '第一次接取时刻，同任务后续请求沿用，供捕获时间核验';

COMMENT ON COLUMN public.quest_accept_receipt.response IS '首次响应原文，包含原补给说明或已接取提示';

COMMENT ON COLUMN public.quest_accept_receipt.runtime_id IS '接取补给所属任务实例';

COMMENT ON COLUMN public.quest_accept_receipt.runtime_round IS '委托环内补给轮次，重接同轮不重复发放';

CREATE TABLE public.quest_journal_state (
    id bigint CONSTRAINT quest_participation_id_not_null NOT NULL,
    character_id bigint CONSTRAINT quest_participation_character_id_not_null NOT NULL,
    quest_code character varying(255) CONSTRAINT quest_participation_quest_code_not_null NOT NULL,
    status character varying(16) CONSTRAINT quest_participation_status_not_null NOT NULL,
    first_accepted_at timestamp with time zone CONSTRAINT quest_participation_first_accepted_at_not_null NOT NULL,
    active_since timestamp with time zone CONSTRAINT quest_participation_active_since_not_null NOT NULL,
    revision bigint CONSTRAINT quest_participation_revision_not_null NOT NULL,
    runtime_id character varying(36) NOT NULL
);

COMMENT ON TABLE public.quest_journal_state IS '角色整项任务参与状态，独立于步骤回执与永久事实';

COMMENT ON COLUMN public.quest_journal_state.id IS 'CosId雪花主键';

COMMENT ON COLUMN public.quest_journal_state.character_id IS '任务归属角色';

COMMENT ON COLUMN public.quest_journal_state.quest_code IS '整项任务编号，与步骤编号分离';

COMMENT ON COLUMN public.quest_journal_state.status IS '参与状态ACTIVE、ABANDONED或COMPLETED';

COMMENT ON COLUMN public.quest_journal_state.first_accepted_at IS '首次接取时间，重接保留';

COMMENT ON COLUMN public.quest_journal_state.active_since IS '最近恢复接取时间，仅激活状态记录新目标';

COMMENT ON COLUMN public.quest_journal_state.revision IS '状态转换版本，用于拒绝过期写操作';

COMMENT ON COLUMN public.quest_journal_state.runtime_id IS '当前任务实例编号';

CREATE TABLE public.quest_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    step character varying(255) NOT NULL,
    quest_code character varying(255) NOT NULL,
    completed boolean NOT NULL,
    response text NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.quest_receipt IS '步骤完成与回复一并持久化。重复请求返回首次战利品说明，不再次执行效果。';

COMMENT ON COLUMN public.quest_receipt.id IS 'CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。';

COMMENT ON COLUMN public.quest_receipt.character_id IS '所属角色的数据库主键。跨模块只传编号或不可变上下文，不建立可被外部修改的 JPA 实体引用。';

COMMENT ON COLUMN public.quest_receipt.step IS '已完成步骤编号。同一角色与步骤唯一，是整个步骤命令防重和回执重放的依据。';

COMMENT ON COLUMN public.quest_receipt.quest_code IS '步骤所属主线任务编号，用于查询已完成任务，不替代步骤级幂等键。';

COMMENT ON COLUMN public.quest_receipt.completed IS '本步骤是否标记整项任务完成；普通对话步骤也有凭证，但不会进入已完成任务列表。';

COMMENT ON COLUMN public.quest_receipt.response IS '首次成功结果的 JSON 回执。重试只读取它，不再执行发奖；损坏时应报内部错误。';

COMMENT ON COLUMN public.quest_receipt.created_at IS '首次完成时间，用于稳定展示顺序；相同时间由主键作为第二排序条件。';

CREATE TABLE public.quest_runtime_instance (
    id character varying(36) NOT NULL,
    character_id bigint NOT NULL,
    quest_code character varying(255) NOT NULL,
    attempt integer NOT NULL,
    accepted_at timestamp with time zone NOT NULL,
    state text NOT NULL
);

COMMENT ON TABLE public.quest_runtime_instance IS '独立任务尝试；JSON仅保存运行状态，不保存定义版本';

COMMENT ON COLUMN public.quest_runtime_instance.id IS '实例UUID、请求雪花编号或定义稳定编号，取决于所属表';

COMMENT ON COLUMN public.quest_runtime_instance.character_id IS '服务端登录身份解析的归属角色编号';

COMMENT ON COLUMN public.quest_runtime_instance.quest_code IS '稳定任务编号，多次尝试使用不同实例';

COMMENT ON COLUMN public.quest_runtime_instance.attempt IS '同角色同任务递增的尝试序号，相同接取时间仍可准确定位最新实例';

COMMENT ON COLUMN public.quest_runtime_instance.accepted_at IS '首次接取时间，历史按时间与实例编号稳定排序';

COMMENT ON COLUMN public.quest_runtime_instance.state IS '类型化任务实例JSON，保存进度、轮次、分支和结算回执';

CREATE TABLE public.quest_runtime_request (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    response text NOT NULL
);

COMMENT ON TABLE public.quest_runtime_request IS '实例写命令的首次响应及规范请求，奖励和回执同事务';

COMMENT ON COLUMN public.quest_runtime_request.id IS '实例UUID、请求雪花编号或定义稳定编号，取决于所属表';

COMMENT ON COLUMN public.quest_runtime_request.character_id IS '服务端登录身份解析的归属角色编号';

COMMENT ON COLUMN public.quest_runtime_request.request_id IS '角色内唯一的标准UUID请求号，重试重放首次响应';

COMMENT ON COLUMN public.quest_runtime_request.payload IS '规范请求JSON，拒绝相同请求号提交不同操作或参数';

COMMENT ON COLUMN public.quest_runtime_request.response IS '首次成功响应JSON，奖励与状态共事务保存';

CREATE TABLE public.quest_tracking (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    main_quest character varying(255),
    side_quest character varying(255),
    revision bigint DEFAULT 0 NOT NULL
);

COMMENT ON TABLE public.quest_tracking IS '角色主线及支线地图指引偏好，不影响任务目标累计';

COMMENT ON COLUMN public.quest_tracking.id IS 'CosId雪花主键';

COMMENT ON COLUMN public.quest_tracking.character_id IS '偏好所属角色，每个角色只保存一份';

COMMENT ON COLUMN public.quest_tracking.main_quest IS '自选主线任务编号，空值表示取消追踪';

COMMENT ON COLUMN public.quest_tracking.side_quest IS '自选支线任务编号，空值表示取消追踪';

COMMENT ON COLUMN public.quest_tracking.revision IS '追踪偏好变更版本，拒绝其他页面的过期请求';

CREATE TABLE public.refresh_receipt (
    token_hash character varying(64) NOT NULL,
    session_id bigint NOT NULL,
    expires_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.refresh_receipt IS '已消费的刷新摘要用于检测重放；保留至原令牌到期，撤销所属会话而不返回新凭据。';

COMMENT ON COLUMN public.refresh_receipt.token_hash IS '已消费刷新令牌的摘要主键。仅保留摘要，不保留任何可恢复的原令牌。';

COMMENT ON COLUMN public.refresh_receipt.session_id IS '对应设备会话主键，重放旧令牌时定位并撤销该设备。外键限制过早删除会话。';

COMMENT ON COLUMN public.refresh_receipt.expires_at IS '重放检测记录的保留截止时间，与原设备会话绝对寿命保持一致。';

CREATE TABLE public.research_exchange_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL
);

COMMENT ON TABLE public.research_exchange_receipt IS '兑换回执保存实际已核对的请求内容，金币不参与凭证兑换。';

COMMENT ON COLUMN public.research_exchange_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.research_exchange_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.research_exchange_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.research_exchange_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

CREATE TABLE public.scene_action_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    response text NOT NULL
);

COMMENT ON TABLE public.scene_action_receipt IS '场景动作幂等回执，与成功事实及任务物品同事务提交';

COMMENT ON COLUMN public.scene_action_receipt.id IS 'CosId生成的雪花主键';

COMMENT ON COLUMN public.scene_action_receipt.character_id IS '执行动作的角色归属';

COMMENT ON COLUMN public.scene_action_receipt.request_id IS '角色内唯一标准UUID请求号';

COMMENT ON COLUMN public.scene_action_receipt.payload IS '原动作编号、位置版本和报告选项，重试必须一致';

COMMENT ON COLUMN public.scene_action_receipt.response IS '原始成功或错误报告响应，重放不执行奖励';

CREATE TABLE public.skill_learning_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    result text NOT NULL
);

COMMENT ON TABLE public.skill_learning_receipt IS '学习请求的不可变结算凭证；负载用于识别同一请求号被拿来学习另一只伙伴或另一本书。';

COMMENT ON COLUMN public.skill_learning_receipt.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.skill_learning_receipt.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.skill_learning_receipt.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.skill_learning_receipt.payload IS '已核验请求的 JSON 文本；重试时比对原负载，拒绝同一请求号携带不同参数。';

COMMENT ON COLUMN public.skill_learning_receipt.result IS '首次结算结果的 JSON 文本；重试返回原结果，不重新扣费、抽取或发奖。';

CREATE TABLE public.supply_purchase (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(64) NOT NULL,
    code character varying(255) NOT NULL
);

COMMENT ON TABLE public.supply_purchase IS '购买凭证与扣款、道具发放共同提交，响应丢失重试时不重复扣款。';

COMMENT ON COLUMN public.supply_purchase.id IS 'CosId 分配的雪花主键；仅用于稳定标识记录，不承载业务顺序。';

COMMENT ON COLUMN public.supply_purchase.character_id IS '所属角色主键；操作时由认证账号定位角色并校验归属。';

COMMENT ON COLUMN public.supply_purchase.request_id IS '客户端幂等请求号；同一归属下重复提交须核对原请求，不重复执行结算。';

COMMENT ON COLUMN public.supply_purchase.code IS '本次购买的补给物品编号；与幂等请求一起核验，防止重试换购另一种物品。';

CREATE TABLE public.workshop_course_receipt (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    payload text NOT NULL,
    response text NOT NULL
);

COMMENT ON TABLE public.workshop_course_receipt IS '课程操作幂等回执；重试不重抽、不重复消费临时资源';

COMMENT ON COLUMN public.workshop_course_receipt.id IS 'CosId生成的回执主键';

COMMENT ON COLUMN public.workshop_course_receipt.character_id IS '角色归属';

COMMENT ON COLUMN public.workshop_course_receipt.request_id IS '操作请求UUID';

COMMENT ON COLUMN public.workshop_course_receipt.payload IS '原动作负载与版本';

COMMENT ON COLUMN public.workshop_course_receipt.response IS '首次操作响应';

CREATE TABLE public.workshop_course_session (
    id bigint NOT NULL,
    character_id bigint NOT NULL,
    request_id character varying(36) NOT NULL,
    course character varying(16) NOT NULL,
    status character varying(16) NOT NULL,
    state text NOT NULL,
    created_at timestamp with time zone NOT NULL
);

COMMENT ON TABLE public.workshop_course_session IS '学院战外课程的临时状态；退出后临时资源从JSON中清除';

COMMENT ON COLUMN public.workshop_course_session.id IS 'CosId生成的课程会话主键';

COMMENT ON COLUMN public.workshop_course_session.character_id IS '角色归属';

COMMENT ON COLUMN public.workshop_course_session.request_id IS '入课请求UUID';

COMMENT ON COLUMN public.workshop_course_session.course IS 'WORKSHOP_SKILLS或WORKSHOP_GEAR';

COMMENT ON COLUMN public.workshop_course_session.status IS 'ACTIVE、PASSED或EXITED';

COMMENT ON COLUMN public.workshop_course_session.state IS '隔离课程临时状态JSON';

COMMENT ON COLUMN public.workshop_course_session.created_at IS '入课时间';

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_username_key UNIQUE (username);

ALTER TABLE ONLY public.battle
    ADD CONSTRAINT battle_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.battle
    ADD CONSTRAINT battle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.battle_turn
    ADD CONSTRAINT battle_turn_battle_id_round_key UNIQUE (battle_id, round);

ALTER TABLE ONLY public.battle_turn
    ADD CONSTRAINT battle_turn_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.book_recycling_receipt
    ADD CONSTRAINT book_recycling_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.book_recycling_receipt
    ADD CONSTRAINT book_recycling_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.character_fact
    ADD CONSTRAINT character_fact_character_id_code_key UNIQUE (character_id, code);

ALTER TABLE ONLY public.character_fact
    ADD CONSTRAINT character_fact_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_block
    ADD CONSTRAINT chat_block_pkey PRIMARY KEY (character_id, peer);

ALTER TABLE ONLY public.chat_message
    ADD CONSTRAINT chat_message_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_seen
    ADD CONSTRAINT chat_seen_pkey PRIMARY KEY (character_id, message_id);

ALTER TABLE ONLY public.chat_settings
    ADD CONSTRAINT chat_settings_pkey PRIMARY KEY (character_id);

ALTER TABLE ONLY public.collection_receipt
    ADD CONSTRAINT collection_receipt_account_id_code_key UNIQUE (account_id, code);

ALTER TABLE ONLY public.collection_receipt
    ADD CONSTRAINT collection_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.combat_vitals
    ADD CONSTRAINT combat_vitals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.crafting_receipt
    ADD CONSTRAINT crafting_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.crafting_receipt
    ADD CONSTRAINT crafting_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.creature_discovery
    ADD CONSTRAINT creature_discovery_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.creature_discovery
    ADD CONSTRAINT creature_discovery_unique UNIQUE (character_id, species, ability_slot, ability);

ALTER TABLE ONLY public.creature_evolution_receipt
    ADD CONSTRAINT creature_evolution_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.creature_evolution_receipt
    ADD CONSTRAINT creature_evolution_request_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.creature_release_receipt
    ADD CONSTRAINT creature_release_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.creature_release_receipt
    ADD CONSTRAINT creature_release_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.creature_rename_receipt
    ADD CONSTRAINT creature_rename_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.creature_rename_receipt
    ADD CONSTRAINT creature_rename_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.cultivation_reward_receipt
    ADD CONSTRAINT cultivation_reward_event_key UNIQUE (character_id, event_key);

ALTER TABLE ONLY public.cultivation_reward_receipt
    ADD CONSTRAINT cultivation_reward_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.device_session
    ADD CONSTRAINT device_session_access_hash_key UNIQUE (access_hash);

ALTER TABLE ONLY public.device_session
    ADD CONSTRAINT device_session_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.device_session
    ADD CONSTRAINT device_session_refresh_hash_key UNIQUE (refresh_hash);

ALTER TABLE ONLY public.dungeon_battle_member
    ADD CONSTRAINT dungeon_battle_member_pkey PRIMARY KEY (battle_id, character_id);

ALTER TABLE ONLY public.dungeon_battle
    ADD CONSTRAINT dungeon_battle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_chest
    ADD CONSTRAINT dungeon_chest_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_chest
    ADD CONSTRAINT dungeon_chest_run_id_key UNIQUE (run_id);

ALTER TABLE ONLY public.dungeon_daily_start
    ADD CONSTRAINT dungeon_daily_start_pkey PRIMARY KEY (character_id, dungeon, day);

ALTER TABLE ONLY public.dungeon_invitation
    ADD CONSTRAINT dungeon_invitation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_invitation
    ADD CONSTRAINT dungeon_invitation_sender_id_request_id_key UNIQUE (sender_id, request_id);

ALTER TABLE ONLY public.dungeon_lobby
    ADD CONSTRAINT dungeon_lobby_leader_id_request_id_key UNIQUE (leader_id, request_id);

ALTER TABLE ONLY public.dungeon_lobby
    ADD CONSTRAINT dungeon_lobby_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_run
    ADD CONSTRAINT dungeon_run_leader_id_request_id_key UNIQUE (leader_id, request_id);

ALTER TABLE ONLY public.dungeon_run
    ADD CONSTRAINT dungeon_run_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_submission
    ADD CONSTRAINT dungeon_submission_battle_id_character_id_round_key UNIQUE (battle_id, character_id, round);

ALTER TABLE ONLY public.dungeon_submission
    ADD CONSTRAINT dungeon_submission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_supply
    ADD CONSTRAINT dungeon_supply_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_supply
    ADD CONSTRAINT dungeon_supply_scope_id_item_id_key UNIQUE (scope_id, item_id);

ALTER TABLE ONLY public.dungeon_supply_scope
    ADD CONSTRAINT dungeon_supply_scope_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.dungeon_supply_scope
    ADD CONSTRAINT dungeon_supply_scope_run_id_character_id_key UNIQUE (run_id, character_id);

ALTER TABLE ONLY public.enhancement_receipt
    ADD CONSTRAINT enhancement_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.enhancement_receipt
    ADD CONSTRAINT enhancement_request_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.equipment_receipt
    ADD CONSTRAINT equipment_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.equipment_receipt
    ADD CONSTRAINT equipment_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.fishing_session
    ADD CONSTRAINT fishing_session_pkey PRIMARY KEY (character_id);

ALTER TABLE ONLY public.friendship
    ADD CONSTRAINT friendship_first_character_id_second_character_id_key UNIQUE (first_character_id, second_character_id);

ALTER TABLE ONLY public.friendship
    ADD CONSTRAINT friendship_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.gathering_session
    ADD CONSTRAINT gathering_session_pkey PRIMARY KEY (character_id);

ALTER TABLE ONLY public.inventory_claim
    ADD CONSTRAINT inventory_claim_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.inventory_claim
    ADD CONSTRAINT inventory_claim_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_grant
    ADD CONSTRAINT inventory_grant_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_grant
    ADD CONSTRAINT inventory_grant_source_key UNIQUE (character_id, source);

ALTER TABLE ONLY public.life_operation_receipt
    ADD CONSTRAINT life_operation_receipt_pkey PRIMARY KEY (character_id, request_id);

ALTER TABLE ONLY public.life_skill
    ADD CONSTRAINT life_skill_pkey PRIMARY KEY (character_id, skill);

ALTER TABLE ONLY public.life_skill_receipt
    ADD CONSTRAINT life_skill_receipt_pkey PRIMARY KEY (character_id, request_id);

ALTER TABLE ONLY public.market_listing_item
    ADD CONSTRAINT market_listing_item_pkey PRIMARY KEY (listing_id, asset_id);

ALTER TABLE ONLY public.market_listing
    ADD CONSTRAINT market_listing_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.market_listing_skill
    ADD CONSTRAINT market_listing_skill_pkey PRIMARY KEY (listing_id, skill);

ALTER TABLE ONLY public.market_receipt
    ADD CONSTRAINT market_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.market_receipt
    ADD CONSTRAINT market_receipt_request_key UNIQUE (account_id, request_id);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT owned_creature_character_id_party_position_key UNIQUE (character_id, party_position);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT owned_creature_character_id_source_key UNIQUE (character_id, source);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT owned_creature_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.owned_item
    ADD CONSTRAINT owned_item_character_id_source_key UNIQUE (character_id, source);

ALTER TABLE ONLY public.owned_item
    ADD CONSTRAINT owned_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.personal_farm
    ADD CONSTRAINT personal_farm_pkey PRIMARY KEY (character_id);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_account_id_key UNIQUE (account_id);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_name_key UNIQUE (name);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pvp_battle
    ADD CONSTRAINT pvp_battle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pvp_command_receipt
    ADD CONSTRAINT pvp_command_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pvp_protection
    ADD CONSTRAINT pvp_protection_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.pvp_protection_receipt
    ADD CONSTRAINT pvp_protection_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_accept_receipt
    ADD CONSTRAINT quest_accept_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_journal_state
    ADD CONSTRAINT quest_participation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_receipt
    ADD CONSTRAINT quest_receipt_character_id_step_key UNIQUE (character_id, step);

ALTER TABLE ONLY public.quest_receipt
    ADD CONSTRAINT quest_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_runtime_instance
    ADD CONSTRAINT quest_runtime_instance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_runtime_request
    ADD CONSTRAINT quest_runtime_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quest_tracking
    ADD CONSTRAINT quest_tracking_character_id_key UNIQUE (character_id);

ALTER TABLE ONLY public.quest_tracking
    ADD CONSTRAINT quest_tracking_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_receipt
    ADD CONSTRAINT refresh_receipt_pkey PRIMARY KEY (token_hash);

ALTER TABLE ONLY public.research_exchange_receipt
    ADD CONSTRAINT research_exchange_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.research_exchange_receipt
    ADD CONSTRAINT research_exchange_request_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.scene_action_receipt
    ADD CONSTRAINT scene_action_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.skill_learning_receipt
    ADD CONSTRAINT skill_learning_receipt_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.skill_learning_receipt
    ADD CONSTRAINT skill_learning_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.supply_purchase
    ADD CONSTRAINT supply_purchase_character_id_request_id_key UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.supply_purchase
    ADD CONSTRAINT supply_purchase_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.battle_turn
    ADD CONSTRAINT uq_battle_turn_request UNIQUE (battle_id, request_id);

ALTER TABLE ONLY public.dungeon_submission
    ADD CONSTRAINT uq_dungeon_submission_request UNIQUE (battle_id, character_id, request_id);

ALTER TABLE ONLY public.pvp_command_receipt
    ADD CONSTRAINT uq_pvp_command_request UNIQUE (battle_id, character_id, request_id);

ALTER TABLE ONLY public.pvp_protection_receipt
    ADD CONSTRAINT uq_pvp_protection_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.pvp_battle
    ADD CONSTRAINT uq_pvp_start_request UNIQUE (attacker_id, request_id);

ALTER TABLE ONLY public.quest_accept_receipt
    ADD CONSTRAINT uq_quest_accept_character_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.quest_journal_state
    ADD CONSTRAINT uq_quest_participation_character_code UNIQUE (character_id, quest_code);

ALTER TABLE ONLY public.quest_runtime_instance
    ADD CONSTRAINT uq_quest_runtime_attempt UNIQUE (character_id, quest_code, attempt);

ALTER TABLE ONLY public.quest_runtime_request
    ADD CONSTRAINT uq_quest_runtime_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.scene_action_receipt
    ADD CONSTRAINT uq_scene_action_character_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.workshop_course_receipt
    ADD CONSTRAINT uq_workshop_receipt_character_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.workshop_course_session
    ADD CONSTRAINT uq_workshop_session_character_request UNIQUE (character_id, request_id);

ALTER TABLE ONLY public.workshop_course_receipt
    ADD CONSTRAINT workshop_course_receipt_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.workshop_course_session
    ADD CONSTRAINT workshop_course_session_pkey PRIMARY KEY (id);

CREATE INDEX chat_incoming ON public.chat_message USING btree (recipient, id DESC);

CREATE INDEX chat_outgoing ON public.chat_message USING btree (sender, id DESC);

CREATE UNIQUE INDEX chat_player_request ON public.chat_message USING btree (sender, request_id) WHERE (sender IS NOT NULL);

CREATE INDEX chat_public_history ON public.chat_message USING btree (channel, id DESC) WHERE (recipient IS NULL);

CREATE INDEX chat_retention ON public.chat_message USING btree (created_at) WHERE (((channel)::text = 'WORLD'::text) OR (((channel)::text = 'SYSTEM'::text) AND (recipient IS NULL)));

CREATE INDEX chat_sender_rate ON public.chat_message USING btree (sender, created_at DESC) WHERE (sender IS NOT NULL);

CREATE UNIQUE INDEX chat_system_request ON public.chat_message USING btree (request_id) WHERE (sender IS NULL);

CREATE UNIQUE INDEX creature_origin_source ON public.owned_creature USING btree (origin_character_id, source);

CREATE INDEX dungeon_battle_due ON public.dungeon_battle USING btree (deadline, id) WHERE active;

CREATE INDEX dungeon_chest_owner ON public.dungeon_chest USING btree (character_id, claimed);

CREATE INDEX dungeon_invitation_inbox ON public.dungeon_invitation USING btree (recipient_id, id) WHERE ((status)::text = 'PENDING'::text);

CREATE UNIQUE INDEX dungeon_invitation_pending ON public.dungeon_invitation USING btree (lobby_id, recipient_id) WHERE ((status)::text = 'PENDING'::text);

CREATE INDEX dungeon_lobby_deadline ON public.dungeon_lobby USING btree (deadline) WHERE open;

CREATE UNIQUE INDEX dungeon_run_active_battle ON public.dungeon_run USING btree (active_battle_id) WHERE (active_battle_id IS NOT NULL);

CREATE UNIQUE INDEX dungeon_supply_active_character ON public.dungeon_supply_scope USING btree (character_id) WHERE active;

CREATE INDEX friendship_first_page ON public.friendship USING btree (first_character_id, id);

CREATE INDEX friendship_second_page ON public.friendship USING btree (second_character_id, id);

CREATE INDEX idx_quest_runtime_history ON public.quest_runtime_instance USING btree (character_id, quest_code, accepted_at);

CREATE INDEX idx_receipt_expiry ON public.refresh_receipt USING btree (expires_at);

CREATE INDEX idx_session_owner ON public.device_session USING btree (account_id);

CREATE INDEX ix_quest_accept_character_step ON public.quest_accept_receipt USING btree (character_id, step, id);

CREATE UNIQUE INDEX market_listing_asset_in_custody ON public.market_listing USING btree (kind, asset_id) WHERE ((state)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RETURN_PENDING'::character varying])::text[]));

CREATE INDEX market_listing_expiry ON public.market_listing USING btree (state, expires_at);

CREATE INDEX market_listing_item_search ON public.market_listing USING btree (state, item_category, unit_price, id);

CREATE INDEX market_listing_seller_state ON public.market_listing USING btree (seller_account_id, state);

CREATE UNIQUE INDEX one_active_battle ON public.battle USING btree (character_id) WHERE active;

CREATE UNIQUE INDEX one_equipped_slot ON public.owned_item USING btree (character_id, slot) WHERE (equipped = true);

ALTER TABLE ONLY public.battle
    ADD CONSTRAINT battle_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.battle_turn
    ADD CONSTRAINT battle_turn_battle_id_fkey FOREIGN KEY (battle_id) REFERENCES public.battle(id);

ALTER TABLE ONLY public.book_recycling_receipt
    ADD CONSTRAINT book_recycling_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.character_fact
    ADD CONSTRAINT character_fact_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_block
    ADD CONSTRAINT chat_block_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_block
    ADD CONSTRAINT chat_block_peer_fk FOREIGN KEY (peer) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_message
    ADD CONSTRAINT chat_message_recipient_fk FOREIGN KEY (recipient) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_message
    ADD CONSTRAINT chat_message_sender_fk FOREIGN KEY (sender) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_seen
    ADD CONSTRAINT chat_seen_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.chat_seen
    ADD CONSTRAINT chat_seen_message_fk FOREIGN KEY (message_id) REFERENCES public.chat_message(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.chat_settings
    ADD CONSTRAINT chat_settings_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.collection_receipt
    ADD CONSTRAINT collection_receipt_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);

ALTER TABLE ONLY public.combat_vitals
    ADD CONSTRAINT combat_vitals_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.crafting_receipt
    ADD CONSTRAINT crafting_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT creature_captor_character_fkey FOREIGN KEY (captor_character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.creature_evolution_receipt
    ADD CONSTRAINT creature_evolution_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT creature_origin_character_fkey FOREIGN KEY (origin_character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.creature_release_receipt
    ADD CONSTRAINT creature_release_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.creature_rename_receipt
    ADD CONSTRAINT creature_rename_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.cultivation_reward_receipt
    ADD CONSTRAINT cultivation_reward_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.device_session
    ADD CONSTRAINT device_session_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);

ALTER TABLE ONLY public.creature_discovery
    ADD CONSTRAINT discovery_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_battle_member
    ADD CONSTRAINT dungeon_battle_member_battle_id_fkey FOREIGN KEY (battle_id) REFERENCES public.dungeon_battle(id);

ALTER TABLE ONLY public.dungeon_battle_member
    ADD CONSTRAINT dungeon_battle_member_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_chest
    ADD CONSTRAINT dungeon_chest_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_chest
    ADD CONSTRAINT dungeon_chest_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.dungeon_run(id);

ALTER TABLE ONLY public.dungeon_daily_start
    ADD CONSTRAINT dungeon_daily_start_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_invitation
    ADD CONSTRAINT dungeon_invitation_lobby_id_fkey FOREIGN KEY (lobby_id) REFERENCES public.dungeon_lobby(id);

ALTER TABLE ONLY public.dungeon_invitation
    ADD CONSTRAINT dungeon_invitation_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_invitation
    ADD CONSTRAINT dungeon_invitation_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_lobby
    ADD CONSTRAINT dungeon_lobby_leader_id_fkey FOREIGN KEY (leader_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_run
    ADD CONSTRAINT dungeon_run_active_battle_id_fkey FOREIGN KEY (active_battle_id) REFERENCES public.dungeon_battle(id);

ALTER TABLE ONLY public.dungeon_run
    ADD CONSTRAINT dungeon_run_leader_id_fkey FOREIGN KEY (leader_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_submission
    ADD CONSTRAINT dungeon_submission_battle_id_character_id_fkey FOREIGN KEY (battle_id, character_id) REFERENCES public.dungeon_battle_member(battle_id, character_id);

ALTER TABLE ONLY public.dungeon_supply
    ADD CONSTRAINT dungeon_supply_item_id_fkey FOREIGN KEY (item_id) REFERENCES public.owned_item(id);

ALTER TABLE ONLY public.dungeon_supply_scope
    ADD CONSTRAINT dungeon_supply_scope_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.dungeon_supply
    ADD CONSTRAINT dungeon_supply_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.dungeon_supply_scope(id);

ALTER TABLE ONLY public.enhancement_receipt
    ADD CONSTRAINT enhancement_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.equipment_receipt
    ADD CONSTRAINT equipment_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.fishing_session
    ADD CONSTRAINT fishing_session_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.pvp_battle
    ADD CONSTRAINT fk_pvp_attacker FOREIGN KEY (attacker_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.pvp_battle
    ADD CONSTRAINT fk_pvp_defender FOREIGN KEY (defender_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.pvp_protection
    ADD CONSTRAINT fk_pvp_protection_character FOREIGN KEY (id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.pvp_protection_receipt
    ADD CONSTRAINT fk_pvp_protection_receipt_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.pvp_command_receipt
    ADD CONSTRAINT fk_pvp_receipt_battle FOREIGN KEY (battle_id) REFERENCES public.pvp_battle(id);

ALTER TABLE ONLY public.pvp_command_receipt
    ADD CONSTRAINT fk_pvp_receipt_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.quest_accept_receipt
    ADD CONSTRAINT fk_quest_accept_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.quest_journal_state
    ADD CONSTRAINT fk_quest_participation_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.quest_runtime_instance
    ADD CONSTRAINT fk_quest_runtime_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.quest_runtime_request
    ADD CONSTRAINT fk_quest_runtime_request_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.quest_tracking
    ADD CONSTRAINT fk_quest_tracking_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.scene_action_receipt
    ADD CONSTRAINT fk_scene_action_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.workshop_course_receipt
    ADD CONSTRAINT fk_workshop_receipt_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.workshop_course_session
    ADD CONSTRAINT fk_workshop_session_character FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.friendship
    ADD CONSTRAINT friendship_first_character_id_fkey FOREIGN KEY (first_character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.friendship
    ADD CONSTRAINT friendship_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.friendship
    ADD CONSTRAINT friendship_second_character_id_fkey FOREIGN KEY (second_character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.gathering_session
    ADD CONSTRAINT gathering_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.inventory_claim
    ADD CONSTRAINT inventory_claim_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.inventory_claim
    ADD CONSTRAINT inventory_claim_item_id_fkey FOREIGN KEY (item_id) REFERENCES public.owned_item(id);

ALTER TABLE ONLY public.inventory_grant
    ADD CONSTRAINT inventory_grant_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.life_operation_receipt
    ADD CONSTRAINT life_operation_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.life_skill
    ADD CONSTRAINT life_skill_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.life_skill_receipt
    ADD CONSTRAINT life_skill_receipt_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.market_listing_item
    ADD CONSTRAINT market_listing_item_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.market_listing(id);

ALTER TABLE ONLY public.market_listing
    ADD CONSTRAINT market_listing_seller_fkey FOREIGN KEY (seller_account_id) REFERENCES public.account(id);

ALTER TABLE ONLY public.market_listing_skill
    ADD CONSTRAINT market_listing_skill_listing FOREIGN KEY (listing_id) REFERENCES public.market_listing(id);

ALTER TABLE ONLY public.market_receipt
    ADD CONSTRAINT market_receipt_account_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);

ALTER TABLE ONLY public.owned_creature
    ADD CONSTRAINT owned_creature_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.owned_item
    ADD CONSTRAINT owned_item_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.owned_item
    ADD CONSTRAINT owned_item_origin_grant_fkey FOREIGN KEY (origin_grant_id) REFERENCES public.inventory_grant(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.personal_farm
    ADD CONSTRAINT personal_farm_character_fk FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.account(id);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_active_dungeon_id_fkey FOREIGN KEY (active_dungeon_id) REFERENCES public.dungeon_run(id);

ALTER TABLE ONLY public.player_character
    ADD CONSTRAINT player_character_dungeon_lobby_id_fkey FOREIGN KEY (dungeon_lobby_id) REFERENCES public.dungeon_lobby(id);

ALTER TABLE ONLY public.quest_receipt
    ADD CONSTRAINT quest_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.refresh_receipt
    ADD CONSTRAINT refresh_receipt_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.device_session(id);

ALTER TABLE ONLY public.research_exchange_receipt
    ADD CONSTRAINT research_exchange_character_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.skill_learning_receipt
    ADD CONSTRAINT skill_learning_receipt_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);

ALTER TABLE ONLY public.supply_purchase
    ADD CONSTRAINT supply_purchase_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.player_character(id);
