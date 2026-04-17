CREATE TABLE catalog.berry_definition
(
    id                   UUID         NOT NULL DEFAULT uuidv7(),
    code                 VARCHAR(64)  NOT NULL,
    name                 VARCHAR(128) NOT NULL,
    description          TEXT NULL,
    icon                 VARCHAR(255) NULL,
    color_code           VARCHAR(32) NULL,
    firmness_code        VARCHAR(32) NULL,
    size_cm              NUMERIC(6, 2) NULL,
    smoothness           INTEGER NULL,
    spicy                INTEGER      NOT NULL DEFAULT 0,
    dry                  INTEGER      NOT NULL DEFAULT 0,
    sweet                INTEGER      NOT NULL DEFAULT 0,
    bitter               INTEGER      NOT NULL DEFAULT 0,
    sour                 INTEGER      NOT NULL DEFAULT 0,
    natural_gift_type_id UUID NULL,
    natural_gift_power   INTEGER NULL,
    sorting_order        INTEGER      NOT NULL DEFAULT 0,
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_definition PRIMARY KEY (id),
    CONSTRAINT uk_berry_definition__code UNIQUE (code),
    CONSTRAINT fk_berry_definition__natural_gift_type_id FOREIGN KEY (natural_gift_type_id)
        REFERENCES catalog.type_definition (id),
    CONSTRAINT ck_berry_definition__color_code CHECK (
        color_code IS NULL OR color_code IN (
            'RED', 'BLUE', 'YELLOW', 'GREEN', 'PINK', 'PURPLE', 'BROWN', 'BLACK', 'WHITE'
        )
        ),
    CONSTRAINT ck_berry_definition__firmness_code CHECK (
        firmness_code IS NULL OR firmness_code IN (
            'VERY_SOFT', 'SOFT', 'HARD', 'VERY_HARD', 'SUPER_HARD'
        )
        ),
    CONSTRAINT ck_berry_definition__size_cm CHECK (size_cm IS NULL OR size_cm > 0),
    CONSTRAINT ck_berry_definition__smoothness CHECK (smoothness IS NULL OR smoothness >= 0),
    CONSTRAINT ck_berry_definition__flavor_range CHECK (
        spicy >= 0 AND dry >= 0 AND sweet >= 0 AND bitter >= 0 AND sour >= 0
        ),
    CONSTRAINT ck_berry_definition__natural_gift_pair CHECK (
        (natural_gift_type_id IS NULL AND natural_gift_power IS NULL) OR
        (natural_gift_type_id IS NOT NULL AND natural_gift_power IS NOT NULL)
        ),
    CONSTRAINT ck_berry_definition__natural_gift_power CHECK (
        natural_gift_power IS NULL OR natural_gift_power > 0
        )
);

CREATE TABLE catalog.berry_battle_effect
(
    berry_id                   UUID   NOT NULL,
    hold_effect_summary        TEXT NULL,
    direct_use_effect_summary  TEXT NULL,
    fling_power                INTEGER NULL,
    fling_effect_summary       TEXT NULL,
    pluck_effect_summary       TEXT NULL,
    bug_bite_effect_summary    TEXT NULL,
    version                    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_battle_effect PRIMARY KEY (berry_id),
    CONSTRAINT fk_berry_battle_effect__berry_id FOREIGN KEY (berry_id)
        REFERENCES catalog.berry_definition (id) ON DELETE CASCADE,
    CONSTRAINT ck_berry_battle_effect__fling_power CHECK (fling_power IS NULL OR fling_power > 0)
);

CREATE TABLE catalog.berry_cultivation_profile
(
    berry_id             UUID   NOT NULL,
    growth_hours_min     INTEGER NULL,
    growth_hours_max     INTEGER NULL,
    yield_min            INTEGER NULL,
    yield_max            INTEGER NULL,
    cultivation_summary  TEXT NULL,
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_cultivation_profile PRIMARY KEY (berry_id),
    CONSTRAINT fk_berry_cultivation_profile__berry_id FOREIGN KEY (berry_id)
        REFERENCES catalog.berry_definition (id) ON DELETE CASCADE,
    CONSTRAINT ck_berry_cultivation_profile__growth_range CHECK (
        (growth_hours_min IS NULL AND growth_hours_max IS NULL) OR
        (growth_hours_min IS NOT NULL AND growth_hours_max IS NOT NULL AND growth_hours_min > 0 AND growth_hours_max >= growth_hours_min)
        ),
    CONSTRAINT ck_berry_cultivation_profile__yield_range CHECK (
        (yield_min IS NULL AND yield_max IS NULL) OR
        (yield_min IS NOT NULL AND yield_max IS NOT NULL AND yield_min >= 0 AND yield_max >= yield_min)
        )
);

CREATE TABLE catalog.berry_acquisition
(
    id              UUID         NOT NULL DEFAULT uuidv7(),
    berry_id        UUID         NOT NULL,
    source_type     VARCHAR(32)  NOT NULL,
    condition_note  TEXT         NOT NULL,
    sorting_order   INTEGER      NOT NULL DEFAULT 0,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_acquisition PRIMARY KEY (id),
    CONSTRAINT fk_berry_acquisition__berry_id FOREIGN KEY (berry_id)
        REFERENCES catalog.berry_definition (id) ON DELETE CASCADE,
    CONSTRAINT ck_berry_acquisition__source_type CHECK (
        source_type IN (
            'BERRY_TREE', 'BERRY_MASTER', 'NATURAL_OBJECT', 'FIELD_PICKUP', 'PICKUP',
            'WILD_HELD_ITEM', 'TRAINER_HELD_ITEM', 'NPC_GIFT', 'EVENT_REWARD',
            'EXCHANGE', 'OTHER'
        )
        )
);

CREATE TABLE catalog.berry_move_relation
(
    id             UUID         NOT NULL DEFAULT uuidv7(),
    berry_id       UUID         NOT NULL,
    move_code      VARCHAR(64)  NOT NULL,
    move_name      VARCHAR(128) NOT NULL,
    relation_kind  VARCHAR(32)  NOT NULL,
    note           TEXT NULL,
    sorting_order  INTEGER      NOT NULL DEFAULT 0,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_move_relation PRIMARY KEY (id),
    CONSTRAINT uk_berry_move_relation__berry_id__move_code__relation_kind UNIQUE (berry_id, move_code, relation_kind),
    CONSTRAINT fk_berry_move_relation__berry_id FOREIGN KEY (berry_id)
        REFERENCES catalog.berry_definition (id) ON DELETE CASCADE,
    CONSTRAINT ck_berry_move_relation__relation_kind CHECK (
        relation_kind IN ('FLING', 'NATURAL_GIFT', 'PLUCK', 'BUG_BITE', 'CUSTOM')
        )
);

CREATE TABLE catalog.berry_ability_relation
(
    id              UUID         NOT NULL DEFAULT uuidv7(),
    berry_id        UUID         NOT NULL,
    ability_code    VARCHAR(64)  NOT NULL,
    ability_name    VARCHAR(128) NOT NULL,
    relation_kind   VARCHAR(32)  NOT NULL,
    note            TEXT NULL,
    sorting_order   INTEGER      NOT NULL DEFAULT 0,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_berry_ability_relation PRIMARY KEY (id),
    CONSTRAINT uk_berry_ability_relation__berry_id__ability_code__relation_kind UNIQUE (berry_id, ability_code, relation_kind),
    CONSTRAINT fk_berry_ability_relation__berry_id FOREIGN KEY (berry_id)
        REFERENCES catalog.berry_definition (id) ON DELETE CASCADE,
    CONSTRAINT ck_berry_ability_relation__relation_kind CHECK (
        relation_kind IN ('BATTLE_INTERACTION', 'CULTIVATION_INTERACTION', 'OTHER')
        )
);

CREATE INDEX idx_berry_definition__sorting_order__id
    ON catalog.berry_definition (sorting_order, id);

CREATE INDEX idx_berry_acquisition__berry_id__sorting_order__id
    ON catalog.berry_acquisition (berry_id, sorting_order, id);

CREATE INDEX idx_berry_move_relation__berry_id__sorting_order__id
    ON catalog.berry_move_relation (berry_id, sorting_order, id);

CREATE INDEX idx_berry_ability_relation__berry_id__sorting_order__id
    ON catalog.berry_ability_relation (berry_id, sorting_order, id);
