CREATE SCHEMA IF NOT EXISTS integration;
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS player;
CREATE SCHEMA IF NOT EXISTS battle;

CREATE TABLE integration.outbox_event
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    event_id       UUID                                    NOT NULL,
    owner_context  VARCHAR(64)                             NOT NULL,
    aggregate_type VARCHAR(128)                            NOT NULL,
    aggregate_id   VARCHAR(128)                            NOT NULL,
    event_type     VARCHAR(255)                            NOT NULL,
    payload        JSONB                                   NOT NULL,
    headers        JSONB                                   NOT NULL DEFAULT '{}'::jsonb,
    status         VARCHAR(32)                             NOT NULL,
    retry_count    INTEGER                                 NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ                             NOT NULL,
    occurred_at    TIMESTAMPTZ                             NOT NULL,
    published_at   TIMESTAMPTZ NULL,
    last_error     TEXT NULL,
    trace_id       VARCHAR(128) NULL,
    claim_token    UUID NULL,
    claimed_at     TIMESTAMPTZ NULL,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT uk_outbox_event__event_id UNIQUE (event_id),
    CONSTRAINT ck_outbox_event__status CHECK (status IN ('PENDING', 'DISPATCHING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_event__status__available_at
    ON integration.outbox_event (status, available_at);

CREATE INDEX idx_outbox_event__owner_context__aggregate_type__aggregate_id
    ON integration.outbox_event (owner_context, aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_event__status__claimed_at
    ON integration.outbox_event (status, claimed_at);

CREATE TABLE iam.user_account
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    username            VARCHAR(64)                             NOT NULL,
    phone               VARCHAR(32) NULL,
    email               VARCHAR(255) NULL,
    avatar              VARCHAR(512) NULL,
    enabled             BOOLEAN                                 NOT NULL DEFAULT TRUE,
    password_hash       VARCHAR(255) NULL,
    username_normalized VARCHAR(64)                             NOT NULL,
    email_normalized    VARCHAR(255) NULL,
    phone_normalized    VARCHAR(32) NULL,
    password_updated_at TIMESTAMPTZ NULL,
    last_login_at       TIMESTAMPTZ NULL,
    version             BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_account PRIMARY KEY (id),
    CONSTRAINT uk_user_account__username UNIQUE (username),
    CONSTRAINT uk_user_account__phone UNIQUE (phone),
    CONSTRAINT uk_user_account__email UNIQUE (email),
    CONSTRAINT uk_user_account__username_normalized UNIQUE (username_normalized),
    CONSTRAINT uk_user_account__email_normalized UNIQUE (email_normalized),
    CONSTRAINT uk_user_account__phone_normalized UNIQUE (phone_normalized)
);

CREATE TABLE iam.role
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code       VARCHAR(64)                             NOT NULL,
    name       VARCHAR(128)                            NOT NULL,
    enabled    BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version    BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT uk_role__code UNIQUE (code)
);

CREATE TABLE iam.menu
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    parent_id             UUID NULL,
    disabled              BOOLEAN                                 NOT NULL DEFAULT FALSE,
    extra                 TEXT NULL,
    icon                  VARCHAR(128) NULL,
    menu_key              VARCHAR(128)                            NOT NULL,
    title                 VARCHAR(128)                            NOT NULL,
    visible               BOOLEAN                                 NOT NULL DEFAULT TRUE,
    path                  VARCHAR(255) NULL,
    route_name            VARCHAR(128) NULL,
    redirect              VARCHAR(255) NULL,
    component             VARCHAR(255) NULL,
    sorting_order         INTEGER                                 NOT NULL DEFAULT 0,
    pinned                BOOLEAN                                 NOT NULL DEFAULT FALSE,
    show_tab              BOOLEAN                                 NOT NULL DEFAULT TRUE,
    enable_multi_tab      BOOLEAN                                 NOT NULL DEFAULT FALSE,
    menu_type             VARCHAR(32)                             NOT NULL,
    hidden                BOOLEAN                                 NOT NULL DEFAULT FALSE,
    hide_children_in_menu BOOLEAN                                 NOT NULL DEFAULT FALSE,
    flat_menu             BOOLEAN                                 NOT NULL DEFAULT FALSE,
    active_menu           VARCHAR(255) NULL,
    external              BOOLEAN                                 NOT NULL DEFAULT FALSE,
    target                VARCHAR(32) NULL,
    version               BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_menu PRIMARY KEY (id),
    CONSTRAINT uk_menu__menu_key UNIQUE (menu_key),
    CONSTRAINT fk_menu__parent_id FOREIGN KEY (parent_id) REFERENCES iam.menu (id),
    CONSTRAINT ck_menu__menu_type CHECK (menu_type IN ('directory', 'menu', 'button', 'link'))
);

CREATE TABLE iam.permission
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    menu_id       UUID                                  NOT NULL,
    code          VARCHAR(128)                            NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_permission PRIMARY KEY (id),
    CONSTRAINT uk_permission__code UNIQUE (code),
    CONSTRAINT fk_permission__menu_id FOREIGN KEY (menu_id) REFERENCES iam.menu (id)
);

CREATE TABLE iam.user_role
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role__user_id FOREIGN KEY (user_id) REFERENCES iam.user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role__role_id FOREIGN KEY (role_id) REFERENCES iam.role (id) ON DELETE CASCADE
);

CREATE TABLE iam.role_permission
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission__role_id FOREIGN KEY (role_id) REFERENCES iam.role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission__permission_id FOREIGN KEY (permission_id) REFERENCES iam.permission (id) ON DELETE CASCADE
);

CREATE TABLE iam.role_menu
(
    role_id UUID NOT NULL,
    menu_id UUID NOT NULL,
    CONSTRAINT pk_role_menu PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu__role_id FOREIGN KEY (role_id) REFERENCES iam.role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu__menu_id FOREIGN KEY (menu_id) REFERENCES iam.menu (id) ON DELETE CASCADE
);

CREATE TABLE iam.authentication_log
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    client_id     VARCHAR(100) NULL,
    error_message VARCHAR(2000) NULL,
    grant_type    VARCHAR(50) NULL,
    ip            VARCHAR(512) NULL,
    occurred_at   TIMESTAMPTZ                             NOT NULL,
    success       BOOLEAN                                 NOT NULL,
    user_agent    VARCHAR(512) NULL,
    username      VARCHAR(64) NULL,
    user_id       UUID NULL,
    session_id    VARCHAR(128) NULL,
    identity_type VARCHAR(32) NULL,
    principal     VARCHAR(255) NULL,
    result_code   VARCHAR(64) NULL,
    CONSTRAINT pk_authentication_log PRIMARY KEY (id)
);

CREATE TABLE iam.user_session
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    session_key           VARCHAR(128)                            NOT NULL,
    user_id               UUID                                  NOT NULL,
    client_type           VARCHAR(32)                             NOT NULL,
    device_name           VARCHAR(128) NULL,
    device_fingerprint    VARCHAR(128) NULL,
    user_agent            VARCHAR(512) NULL,
    ip                    VARCHAR(128) NULL,
    status                VARCHAR(32)                             NOT NULL,
    revoked_reason        VARCHAR(64) NULL,
    last_authenticated_at TIMESTAMPTZ                             NOT NULL,
    last_refreshed_at     TIMESTAMPTZ                             NOT NULL,
    expires_at            TIMESTAMPTZ                             NOT NULL,
    version               BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_session PRIMARY KEY (id),
    CONSTRAINT uk_user_session__session_key UNIQUE (session_key),
    CONSTRAINT fk_user_session__user_id FOREIGN KEY (user_id) REFERENCES iam.user_account (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_session__client_type CHECK (client_type IN ('ADMIN', 'WEB', 'APP')),
    CONSTRAINT ck_user_session__status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE TABLE iam.refresh_token
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    session_id      UUID                                  NOT NULL,
    token_hash      VARCHAR(128)                            NOT NULL,
    status          VARCHAR(32)                             NOT NULL,
    expires_at      TIMESTAMPTZ                             NOT NULL,
    rotated_from_id UUID NULL,
    used_at         TIMESTAMPTZ NULL,
    revoked_at      TIMESTAMPTZ NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token__token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token__session_id FOREIGN KEY (session_id) REFERENCES iam.user_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token__rotated_from_id FOREIGN KEY (rotated_from_id) REFERENCES iam.refresh_token (id),
    CONSTRAINT ck_refresh_token__status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_menu__parent_id__sorting_order
    ON iam.menu (parent_id, sorting_order, id);

CREATE INDEX idx_permission__menu_id__sorting_order
    ON iam.permission (menu_id, sorting_order, id);

CREATE INDEX idx_user_role__role_id
    ON iam.user_role (role_id);

CREATE INDEX idx_role_permission__permission_id
    ON iam.role_permission (permission_id);

CREATE INDEX idx_role_menu__menu_id
    ON iam.role_menu (menu_id);

CREATE INDEX idx_authentication_log__username__occurred_at
    ON iam.authentication_log (username, occurred_at DESC);

CREATE INDEX idx_authentication_log__user_id__occurred_at
    ON iam.authentication_log (user_id, occurred_at DESC);

CREATE INDEX idx_authentication_log__principal__occurred_at
    ON iam.authentication_log (principal, occurred_at DESC);

CREATE INDEX idx_user_session__user_id__client_type__status__id
    ON iam.user_session (user_id, client_type, status, id);

CREATE INDEX idx_user_session__expires_at
    ON iam.user_session (expires_at);

CREATE INDEX idx_refresh_token__session_id__status__id
    ON iam.refresh_token (session_id, status, id DESC);

CREATE INDEX idx_refresh_token__expires_at
    ON iam.refresh_token (expires_at);

CREATE TABLE catalog.type_definition
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    icon          VARCHAR(255) NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_type_definition PRIMARY KEY (id),
    CONSTRAINT uk_type_definition__code UNIQUE (code)
);

CREATE TABLE catalog.type_effectiveness
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    attacking_type_id UUID                                  NOT NULL,
    defending_type_id UUID                                  NOT NULL,
    multiplier        NUMERIC(4, 2)                           NOT NULL,
    version           BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_type_effectiveness PRIMARY KEY (id),
    CONSTRAINT uk_type_effectiveness__attacking_type_id__defending_type_id UNIQUE (attacking_type_id, defending_type_id),
    CONSTRAINT fk_type_effectiveness__attacking_type_id FOREIGN KEY (attacking_type_id) REFERENCES catalog.type_definition (id),
    CONSTRAINT fk_type_effectiveness__defending_type_id FOREIGN KEY (defending_type_id) REFERENCES catalog.type_definition (id),
    CONSTRAINT ck_type_effectiveness__multiplier CHECK (multiplier >= 0.00 AND multiplier <= 4.00)
);

CREATE TABLE catalog.nature
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code                VARCHAR(64)                             NOT NULL,
    name                VARCHAR(128)                            NOT NULL,
    description         TEXT NULL,
    increased_stat_code VARCHAR(32) NULL,
    decreased_stat_code VARCHAR(32) NULL,
    sorting_order       INTEGER                                 NOT NULL DEFAULT 0,
    enabled             BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version             BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_nature PRIMARY KEY (id),
    CONSTRAINT uk_nature__code UNIQUE (code),
    CONSTRAINT ck_nature__modifier_pair CHECK (
        (increased_stat_code IS NULL AND decreased_stat_code IS NULL) OR
        (increased_stat_code IS NOT NULL AND decreased_stat_code IS NOT NULL)
        ),
    CONSTRAINT ck_nature__distinct_modifier_stat CHECK (
        increased_stat_code IS NULL OR increased_stat_code <> decreased_stat_code
        ),
    CONSTRAINT ck_nature__increased_stat_code CHECK (
        increased_stat_code IS NULL OR increased_stat_code IN (
                                                               'ATTACK',
                                                               'DEFENSE',
                                                               'SPECIAL_ATTACK',
                                                               'SPECIAL_DEFENSE',
                                                               'SPEED'
            )
        ),
    CONSTRAINT ck_nature__decreased_stat_code CHECK (
        decreased_stat_code IS NULL OR decreased_stat_code IN (
                                                               'ATTACK',
                                                               'DEFENSE',
                                                               'SPECIAL_ATTACK',
                                                               'SPECIAL_DEFENSE',
                                                               'SPEED'
            )
        )
);

CREATE TABLE catalog.item
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code           VARCHAR(64)                             NOT NULL,
    name           VARCHAR(128)                            NOT NULL,
    category_code  VARCHAR(64)                             NOT NULL,
    description    TEXT NULL,
    icon           VARCHAR(255) NULL,
    max_stack_size INTEGER                                 NOT NULL DEFAULT 1,
    consumable     BOOLEAN                                 NOT NULL DEFAULT FALSE,
    sorting_order  INTEGER                                 NOT NULL DEFAULT 0,
    enabled        BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version        BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT uk_item__code UNIQUE (code),
    CONSTRAINT ck_item__max_stack_size CHECK (max_stack_size > 0)
);

CREATE TABLE catalog.growth_rate
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    formula_code  VARCHAR(32)                             NOT NULL,
    description   TEXT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_growth_rate PRIMARY KEY (id),
    CONSTRAINT uk_growth_rate__code UNIQUE (code),
    CONSTRAINT ck_growth_rate__formula_code CHECK (
        formula_code IN (
                         'FAST',
                         'MEDIUM_FAST',
                         'MEDIUM_SLOW',
                         'SLOW',
                         'ERRATIC',
                         'FLUCTUATING'
            )
        )
);

CREATE TABLE catalog.creature_species
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code                 VARCHAR(64)                             NOT NULL,
    dex_number           INTEGER                                 NOT NULL,
    name                 VARCHAR(128)                            NOT NULL,
    description          TEXT NULL,
    primary_type_id      UUID                                  NOT NULL,
    secondary_type_id    UUID NULL,
    growth_rate_id       UUID NULL,
    base_hp              INTEGER                                 NOT NULL,
    base_attack          INTEGER                                 NOT NULL,
    base_defense         INTEGER                                 NOT NULL,
    base_special_attack  INTEGER                                 NOT NULL,
    base_special_defense INTEGER                                 NOT NULL,
    base_speed           INTEGER                                 NOT NULL,
    sorting_order        INTEGER                                 NOT NULL DEFAULT 0,
    enabled              BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version              BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_creature_species PRIMARY KEY (id),
    CONSTRAINT uk_creature_species__code UNIQUE (code),
    CONSTRAINT uk_creature_species__dex_number UNIQUE (dex_number),
    CONSTRAINT fk_creature_species__primary_type_id FOREIGN KEY (primary_type_id) REFERENCES catalog.type_definition (id),
    CONSTRAINT fk_creature_species__secondary_type_id FOREIGN KEY (secondary_type_id) REFERENCES catalog.type_definition (id),
    CONSTRAINT fk_creature_species__growth_rate_id FOREIGN KEY (growth_rate_id) REFERENCES catalog.growth_rate (id),
    CONSTRAINT ck_creature_species__distinct_type CHECK (
        secondary_type_id IS NULL OR secondary_type_id <> primary_type_id
        ),
    CONSTRAINT ck_creature_species__base_hp CHECK (base_hp >= 1 AND base_hp <= 255),
    CONSTRAINT ck_creature_species__base_attack CHECK (base_attack >= 1 AND base_attack <= 255),
    CONSTRAINT ck_creature_species__base_defense CHECK (base_defense >= 1 AND base_defense <= 255),
    CONSTRAINT ck_creature_species__base_special_attack CHECK (base_special_attack >= 1 AND base_special_attack <= 255),
    CONSTRAINT ck_creature_species__base_special_defense CHECK (base_special_defense >= 1 AND base_special_defense <= 255),
    CONSTRAINT ck_creature_species__base_speed CHECK (base_speed >= 1 AND base_speed <= 255)
);

CREATE TABLE catalog.move_target
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_move_target PRIMARY KEY (id),
    CONSTRAINT uk_move_target__code UNIQUE (code)
);

CREATE TABLE catalog.move_category
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_move_category PRIMARY KEY (id),
    CONSTRAINT uk_move_category__code UNIQUE (code)
);

CREATE TABLE catalog.move_ailment
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_move_ailment PRIMARY KEY (id),
    CONSTRAINT uk_move_ailment__code UNIQUE (code)
);

CREATE TABLE catalog.move
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code               VARCHAR(64)                             NOT NULL,
    name               VARCHAR(128)                            NOT NULL,
    type_definition_id UUID                                  NOT NULL,
    category_code      VARCHAR(32)                             NOT NULL,
    move_category_id   UUID NULL,
    move_ailment_id    UUID NULL,
    move_target_id     UUID NULL,
    description        TEXT NULL,
    effect_chance      INTEGER NULL,
    power              INTEGER NULL,
    accuracy           INTEGER NULL,
    power_points       INTEGER                                 NOT NULL,
    priority           INTEGER                                 NOT NULL DEFAULT 0,
    text               TEXT NULL,
    short_effect       TEXT NULL,
    effect             TEXT NULL,
    sorting_order      INTEGER                                 NOT NULL DEFAULT 0,
    enabled            BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version            BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_move PRIMARY KEY (id),
    CONSTRAINT uk_move__code UNIQUE (code),
    CONSTRAINT fk_move__type_definition_id FOREIGN KEY (type_definition_id) REFERENCES catalog.type_definition (id),
    CONSTRAINT fk_move__move_category_id FOREIGN KEY (move_category_id) REFERENCES catalog.move_category (id),
    CONSTRAINT fk_move__move_ailment_id FOREIGN KEY (move_ailment_id) REFERENCES catalog.move_ailment (id),
    CONSTRAINT fk_move__move_target_id FOREIGN KEY (move_target_id) REFERENCES catalog.move_target (id),
    CONSTRAINT ck_move__category_code CHECK (category_code IN ('PHYSICAL', 'SPECIAL', 'STATUS')),
    CONSTRAINT ck_move__power CHECK (power IS NULL OR power >= 0),
    CONSTRAINT ck_move__accuracy CHECK (accuracy IS NULL OR (accuracy >= 1 AND accuracy <= 100)),
    CONSTRAINT ck_move__power_points CHECK (power_points > 0)
);

CREATE TABLE catalog.species_evolution
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    from_species_id UUID                                  NOT NULL,
    to_species_id   UUID                                  NOT NULL,
    trigger_code    VARCHAR(32)                             NOT NULL,
    min_level       INTEGER NULL,
    description     TEXT NULL,
    sorting_order   INTEGER                                 NOT NULL DEFAULT 0,
    enabled         BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version         BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_species_evolution PRIMARY KEY (id),
    CONSTRAINT uk_species_evolution__from_to_trigger_code
        UNIQUE (from_species_id, to_species_id, trigger_code),
    CONSTRAINT fk_species_evolution__from_species_id
        FOREIGN KEY (from_species_id) REFERENCES catalog.creature_species (id),
    CONSTRAINT fk_species_evolution__to_species_id
        FOREIGN KEY (to_species_id) REFERENCES catalog.creature_species (id),
    CONSTRAINT ck_species_evolution__distinct_species CHECK (from_species_id <> to_species_id),
    CONSTRAINT ck_species_evolution__trigger_code CHECK (
        trigger_code IN ('LEVEL', 'ITEM', 'TRADE', 'FRIENDSHIP', 'OTHER')
        ),
    CONSTRAINT ck_species_evolution__min_level CHECK (min_level IS NULL OR min_level > 0)
);

CREATE TABLE catalog.ability
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    icon          VARCHAR(255) NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_ability PRIMARY KEY (id),
    CONSTRAINT uk_ability__code UNIQUE (code)
);

CREATE TABLE catalog.species_ability
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    species_id    UUID                                  NOT NULL,
    ability_id    UUID                                  NOT NULL,
    slot_code     VARCHAR(32)                             NOT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_species_ability PRIMARY KEY (id),
    CONSTRAINT uk_species_ability__species_id__slot_code UNIQUE (species_id, slot_code),
    CONSTRAINT uk_species_ability__species_id__ability_id UNIQUE (species_id, ability_id),
    CONSTRAINT fk_species_ability__species_id FOREIGN KEY (species_id) REFERENCES catalog.creature_species (id),
    CONSTRAINT fk_species_ability__ability_id FOREIGN KEY (ability_id) REFERENCES catalog.ability (id),
    CONSTRAINT ck_species_ability__slot_code CHECK (slot_code IN ('PRIMARY', 'SECONDARY', 'HIDDEN'))
);

CREATE TABLE catalog.move_learn_method
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(64)                             NOT NULL,
    name          VARCHAR(128)                            NOT NULL,
    description   TEXT NULL,
    sorting_order INTEGER                                 NOT NULL DEFAULT 0,
    enabled       BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version       BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_move_learn_method PRIMARY KEY (id),
    CONSTRAINT uk_move_learn_method__code UNIQUE (code)
);

CREATE TABLE catalog.species_move_learnset
(
    id             UUID                                    NOT NULL DEFAULT uuidv7(),
    species_id      UUID                                  NOT NULL,
    move_id         UUID                                  NOT NULL,
    learn_method_id UUID                                  NOT NULL,
    level           INTEGER NULL,
    sorting_order   INTEGER                                 NOT NULL DEFAULT 0,
    enabled         BOOLEAN                                 NOT NULL DEFAULT TRUE,
    version         BIGINT                                  NOT NULL DEFAULT 0,
    CONSTRAINT pk_species_move_learnset PRIMARY KEY (id),
    CONSTRAINT uk_species_move_learnset__species_id__move_id__learn_method_id
        UNIQUE (species_id, move_id, learn_method_id),
    CONSTRAINT fk_species_move_learnset__species_id
        FOREIGN KEY (species_id) REFERENCES catalog.creature_species (id),
    CONSTRAINT fk_species_move_learnset__move_id
        FOREIGN KEY (move_id) REFERENCES catalog.move (id),
    CONSTRAINT fk_species_move_learnset__learn_method_id
        FOREIGN KEY (learn_method_id) REFERENCES catalog.move_learn_method (id),
    CONSTRAINT ck_species_move_learnset__level CHECK (level IS NULL OR level > 0)
);

CREATE INDEX idx_type_definition__sorting_order__id
    ON catalog.type_definition (sorting_order, id);

CREATE INDEX idx_nature__sorting_order__id
    ON catalog.nature (sorting_order, id);

CREATE INDEX idx_item__sorting_order__id
    ON catalog.item (sorting_order, id);

CREATE INDEX idx_growth_rate__sorting_order__id
    ON catalog.growth_rate (sorting_order, id);

CREATE INDEX idx_creature_species__sorting_order__id
    ON catalog.creature_species (sorting_order, id);

CREATE INDEX idx_creature_species__growth_rate_id
    ON catalog.creature_species (growth_rate_id);

CREATE INDEX idx_move_target__sorting_order__id
    ON catalog.move_target (sorting_order, id);

CREATE INDEX idx_move_category__sorting_order__id
    ON catalog.move_category (sorting_order, id);

CREATE INDEX idx_move_ailment__sorting_order__id
    ON catalog.move_ailment (sorting_order, id);

CREATE INDEX idx_move__sorting_order__id
    ON catalog.move (sorting_order, id);

CREATE INDEX idx_move__move_category_id
    ON catalog.move (move_category_id);

CREATE INDEX idx_move__move_ailment_id
    ON catalog.move (move_ailment_id);

CREATE INDEX idx_move__move_target_id
    ON catalog.move (move_target_id);

CREATE INDEX idx_species_evolution__from_species_id__sorting_order__id
    ON catalog.species_evolution (from_species_id, sorting_order, id);

CREATE INDEX idx_ability__sorting_order__id
    ON catalog.ability (sorting_order, id);

CREATE INDEX idx_species_ability__species_id__sorting_order__id
    ON catalog.species_ability (species_id, sorting_order, id);

CREATE INDEX idx_move_learn_method__sorting_order__id
    ON catalog.move_learn_method (sorting_order, id);

CREATE INDEX idx_species_move_learnset__species_id__sorting_order__id
    ON catalog.species_move_learnset (species_id, sorting_order, id);
