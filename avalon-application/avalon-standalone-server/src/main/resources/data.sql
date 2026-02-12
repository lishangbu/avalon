-- ----------------------------
-- Records of berry_firmness
-- ----------------------------
BEGIN;
INSERT INTO "berry_firmness" ("id", "internal_name", "name")
VALUES (5, 'super-hard', '非常坚硬'),
       (4, 'very-hard', '很坚硬'),
       (3, 'hard', '坚硬'),
       (2, 'soft', '柔软'),
       (1, 'very-soft', '很柔软') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of berry_flavor
-- ----------------------------
BEGIN;
INSERT INTO "berry_flavor" ("id", "internal_name", "name")
VALUES (1, 'spicy', '辣'),
       (2, 'dry', '涩'),
       (3, 'sweet', '甜'),
       (4, 'bitter', '苦'),
       (5, 'sour', '酸') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of encounter_condition
-- ----------------------------
BEGIN;
INSERT INTO encounter_condition ("id", "internal_name", "name")
VALUES (1, 'swarm', 'Swarm'),
       (2, 'time', 'Time of day'),
       (3, 'radar', 'PokeRadar'),
       (4, 'slot2', 'Gen 3 game in slot 2'),
       (5, 'radio', 'Radio'),
       (6, 'season', 'Season'),
       (7, 'starter', 'Chosen Starter'),
       (8, 'tv-option', 'Chosen dialogue at the news report'),
       (9, 'story-progress', 'Story Progress'),
       (10, 'other', 'Miscellaneous'),
       (11, 'item', 'item'),
       (12, 'weekday', 'weekday'),
       (13, 'first-party-pokemon', 'first-party-pokemon'),
       (14, 'special-encounter', 'special-encounter') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of encounter_condition_value
-- ----------------------------
BEGIN;
INSERT INTO encounter_condition_value ("id", "internal_name", "name", "encounter_condition_id")
VALUES (1, 'swarm-yes', 'During a swarm', 1),
       (2, 'swarm-no', 'Not during a swarm', 1),
       (3, 'time-morning', 'In the morning', 2),
       (4, 'time-day', 'During the day', 2),
       (5, 'time-night', 'At night', 2),
       (6, 'radar-on', 'Using PokéRadar', 3),
       (7, 'radar-off', 'Not using PokéRadar', 3),
       (8, 'slot2-none', 'No game in slot 2', 4),
       (9, 'slot2-ruby', 'Ruby in slot 2', 4),
       (10, 'slot2-sapphire', 'Sapphire in slot 2', 4),
       (11, 'slot2-emerald', 'Emerald in slot 2', 4),
       (12, 'slot2-firered', 'FireRed in slot 2', 4),
       (13, 'slot2-leafgreen', 'LeafGreen in slot 2', 4),
       (14, 'radio-off', 'Radio off', 5),
       (15, 'radio-hoenn', 'Hoenn radio', 5),
       (16, 'radio-sinnoh', 'Sinnoh radio', 5),
       (17, 'season-spring', 'During Spring', 6),
       (18, 'season-summer', 'During Summer', 6),
       (19, 'season-autumn', 'During Autumn', 6),
       (20, 'season-winter', 'During Winter', 6),
       (21, 'starter-bulbasaur', 'Bulbasaur as starter', 7),
       (22, 'starter-squirtle', 'Squirtle as starter', 7),
       (23, 'starter-charmander', 'Charmander as starter', 7),
       (24, 'starter-chespin', 'Chespin as starter', 7),
       (25, 'starter-fennekin', 'Fennekin as starter', 7),
       (26, 'starter-froakie', 'Froakie as starter', 7),
       (27, 'tv-option-blue', 'Chose ‘Blue’ on the TV news report', 8),
       (28, 'tv-option-red', 'Chose ‘Red’ on the TV news report', 8),
       (29, 'story-progress-awakened-beasts', 'Awakened the legendary beasts at Burned Tower', 9),
       (30, 'story-progress-beat-galactic-coronet', 'Visited Lake Verity after defeating Team Galactic at Mt. Coronet',
        9),
       (31, 'story-progress-oak-eterna-city', 'Talked to Professor Oak at Eterna City', 9),
       (32, 'story-progress-vermilion-copycat', 'Visited the Pokémon Fan Club with Copycat’s doll', 9),
       (33, 'story-progress-met-tornadus-thundurus', 'Met Tornadus or Thundurus in a cutscene', 9),
       (34, 'story-progress-beat-elite-four-round-two', 'Beat the Elite 4 for the second time', 9),
       (35, 'story-progress-hall-of-fame', 'Enter the Hall of Fame', 9),
       (36, 'story-progress-none', 'None', 9),
       (37, 'story-progress-national-dex', 'Acquired National Pokédex', 9),
       (38, 'other-none', 'None', 10),
       (39, 'other-snorlax-11-beat-league', 'Beat the Pokémon league after knocking out Snorlax at Route 11', 10),
       (40, 'other-virtual-console', 'Playing on the Virtual Console Release', 10),
       (41, 'story-progress-cure-eldritch-nightmares', 'Cure the nightmares of Eldritch’s Son', 9),
       (42, 'other-talk-to-cynthias-grandmother', 'Talk to Cynthia’s grandmother', 10),
       (43, 'item-none', 'No item requirement', 11),
       (44, 'item-adamant-orb', 'Have Adamant Orb in bag', 11),
       (45, 'item-lustrous-orb', 'Have Lustrous Orb in bag', 11),
       (46, 'item-helix-fossil', 'Have Helix Fossil in bag', 11),
       (47, 'item-dome-fossil', 'Have Dome Fossil in bag', 11),
       (48, 'item-old-amber', 'Have Old Amber in bag', 11),
       (49, 'item-root-fossil', 'Have Root Fossil in bag', 11),
       (50, 'item-claw-fossil', 'Have Claw Fossil in bag', 11),
       (51, 'story-progress-defeat-jupiter', 'Defeat Jupiter', 9),
       (52, 'story-progress-beat-team-galactic-iron-island', 'Defeat Team Galactic at Iron Island', 9),
       (53, 'other-correct-password', 'Input correct password', 10),
       (54, 'story-progress-zephyr-badge', 'Obtained Zephyr badge', 9),
       (55, 'story-progress-beat-red', 'Defeat Red', 9),
       (56, 'other-received-kanto-starter', 'Received a Kanto Starter', 10),
       (57, 'story-progress-receive-tm-from-claire', 'Received TM59 From Claire', 9),
       (58, 'other-regirock-regice-registeel-in-party', 'Have Regirock, Regice and Registeel in the party', 10),
       (59, 'weekday-sunday', 'Sunday', 12),
       (60, 'weekday-monday', 'Monday', 12),
       (61, 'weekday-tuesday', 'Tuesday', 12),
       (62, 'weekday-wednesday', 'Wednesday', 12),
       (63, 'weekday-thursday', 'Thursday', 12),
       (64, 'weekday-friday', 'Friday', 12),
       (65, 'weekday-saturday', 'Saturday', 12),
       (66, 'first-party-pokemon-high-friendship', 'The first Pokémon in the player’s party has a high friendship stat',
        13),
       (67, 'story-progress-defeat-mars', 'Beat Mars for the first time', 9),
       (68, 'item-odd-keystone', 'Have Odd Keystone in bag', 11),
       (69, 'other-talked-to-32-people-underground', 'Has talked to at least 32 people in the underground', 10),
       (70, 'story-progress-returned-machine-part', 'Returned Machine Part to Power Plant', 9),
       (71, 'other-event-arceus-in-party', 'Have an Event Arceus in the party', 10),
       (72, 'special-encounter-couldnt-capture-before',
        'This special Pokémon couldn’t be captured in previous encounters (either fainted or ran from battle)', 14),
       (73, 'item-ice-key', 'Have Ice Key in bag', 11),
       (74, 'item-iron-key', 'Have Iron Key in bag', 11),
       (75, 'story-progress-juniper-cave-of-being', 'Spoke to Professor Juniper in the Cave of Being', 9),
       (76, 'item-lunar-wing', 'Have Lunar Wing in bag', 11),
       (77, 'story-progress-quake-badge', 'Obtained Quake Badge', 9),
       (78, 'item-light-stone', 'Have Light Stone in bag', 11),
       (79, 'item-dark-stone', 'Have Dark Stone in bag', 11),
       (80, 'other-captured-reshiram-or-zekrom', 'Captured Reshiram or Zekrom previously', 10),
       (81, 'defeated-ghetsis', 'Defeated Ghetsis', 9),
       (82, 'other-found-11-times-roaming', 'Has been unsuccesfully battled 11 times before', 10),
       (83, 'time-minute-00-to-19', 'The current minute is between 00 and 19', 2),
       (84, 'time-minute-20-to-39', 'The current minute is between 20 and 39', 2),
       (85, 'time-minute-40-to-59', 'The current minute is between 40 and 59', 2),
       (86, 'time-04-00-to-19-59', 'The current time is between 04:00 and 19:59', 2),
       (87, 'time-20-00-to-21-59', 'The current time is between 20:00 and 21:59', 2),
       (88, 'time-21-00-to-03-59', 'The current time is between 22:00 and 03:59', 2),
       (89, 'item-tidal-bell', 'Have Tidal Bell in bag', 11),
       (90, 'item-clear-bell', 'Have Clear Bell in bag', 11),
       (91, 'story-progress-defeated-groudon-or-kyogre', 'Defeated or captured Groudon or Kyogre', 9),
       (92, 'other-uxie-mesprit-azelf-in-party', 'Have Uxie, Mesprit, and Azelf in the party', 10),
       (93, 'other-nicknamed-cold-item-regice-regirock-registeel3',
        'After capturing the three regis in the game, have a nicknamed Regice holding a cold-based item, plus Regirock and Registeel in the party',
        10),
       (94, 'other-dialga-or-palkia-in-party', 'Have Dialga or Palkia in the party', 10),
       (95, 'other-castform-in-party', 'Have Castform in party', 10),
       (96, 'other-level-100-pokemon-in-party', 'Have a Level 100 Pokémon in party', 10),
       (97, 'other-tornadus-thundurus-in-party', 'Have Tornadus and Thundurus in party', 10),
       (98, 'other-reshiram-zekrom-in-party', 'Have Reshiram and Zekrom in party', 10),
       (99, 'other-captured-all-ultra-beasts', 'Captured all Ultra Beasts', 10),
       (100, 'story-progress-finished-looker-sidequest', 'Finished Looker’s Sidequest', 9),
       (101, 'story-progress-beat-olivias-trial', 'After finishing Olivia’s trial', 9),
       (102, 'other-raikou-entei-in-party', 'Have Raikou and Entei in party', 10),
       (103, 'other-groudon-kyogre-in-party', 'Have Groudon and Kyogre in party', 10),
       (104, 'other-dialga-palkia-in-party', 'Have Dialga and Palkia in party', 10),
       (105, 'other-scan-qr-code', 'Scan a specific QR Code', 10),
       (106, 'other-caught-articuno', 'other-caught-articuno', 10),
       (107, 'other-caught-zapdos', 'other-caught-zapdos', 10),
       (108, 'other-caught-moltres', 'other-caught-moltres', 10)ON CONFLICT ("id") DO NOTHING;
COMMIT;


-- ----------------------------
-- Records of evolution_trigger
-- ----------------------------
BEGIN;
INSERT INTO evolution_trigger ("id", "internal_name", "name")
VALUES (1, 'level-up', 'Level up'),
       (2, 'trade', 'Trade or Linking Cord'),
       (3, 'use-item', 'Use item'),
       (4, 'shed', 'Shed'),
       (5, 'spin', 'Spin'),
       (6, 'tower-of-darkness', 'Train in the Tower of Darkness'),
       (7, 'tower-of-waters', 'Train in the Tower of Waters'),
       (8, 'three-critical-hits', 'Land three critical hits in a battle'),
       (9, 'take-damage', 'Go somewhere after taking damage'),
       (10, 'other', 'Other'),
       (11, 'agile-style-move', 'agile-style-move'),
       (12, 'strong-style-move', 'strong-style-move'),
       (13, 'recoil-damage', 'recoil-damage'),
       (14, 'use-move', 'Use move'),
       (15, 'three-defeated-bisharp', 'Defeat three Bisharp that hold a Leader''s Crest'),
       (16, 'gimmmighoul-coins', 'Collect 999 Gimmighoul Coins') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of gender
-- ----------------------------
BEGIN;
INSERT INTO "gender" (id, internal_name)
VALUES (1, 'female'),
       (2, 'male'),
       (3, 'genderless') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of growth_rate
-- ----------------------------
BEGIN;
INSERT INTO "growth_rate" ("id", "description", "internal_name", "name")
VALUES (1, 'slow', 'slow', '慢'),
       (2, 'medium', 'medium', '较快'),
       (3, 'fast', 'fast', '快'),
       (4, 'medium slow', 'medium-slow', '较慢'),
       (5, 'erratic', 'slow-then-very-fast', '最快'),
       (6, 'fluctuating', 'fast-then-very-slow', '最慢') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of item_attribute
-- ----------------------------
BEGIN;
INSERT INTO "item_attribute"
VALUES (1, 'Has a count in the bag', 'countable', 'Countable'),
       (2, 'Consumed when used', 'consumable', 'Consumable'),
       (3, 'Usable outside battle', 'usable-overworld', 'Usable_overworld'),
       (4, 'Usable in battle', 'usable-in-battle', 'Usable_in_battle'),
       (5, 'Can be held by a Pokémon', 'holdable', 'Holdable'),
       (6, 'Works passively when held', 'holdable-passive', 'Holdable_passive'),
       (7, 'Usable by a Pokémon when held', 'holdable-active', 'Holdable_active'),
       (8, 'Appears in Sinnoh Underground', 'underground', 'Underground') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of move_ailment
-- ----------------------------
BEGIN;
INSERT INTO "move_ailment" ("id", "internal_name", "name")
VALUES (-1, 'unknown', 'unknown'),
       (0, 'none', 'none'),
       (1, 'paralysis', 'paralysis'),
       (2, 'sleep', 'sleep'),
       (3, 'freeze', 'freeze'),
       (4, 'burn', 'burn'),
       (5, 'poison', 'poison'),
       (6, 'confusion', 'confusion'),
       (7, 'infatuation', 'infatuation'),
       (8, 'trap', 'trap'),
       (9, 'nightmare', 'nightmare'),
       (12, 'torment', 'torment'),
       (13, 'disable', 'disable'),
       (14, 'yawn', 'yawn'),
       (15, 'heal-block', 'heal-block'),
       (17, 'no-type-immunity', 'no-type-immunity'),
       (18, 'leech-seed', 'leech-seed'),
       (19, 'embargo', 'embargo'),
       (20, 'perish-song', 'perish-song'),
       (21, 'ingrain', 'ingrain'),
       (24, 'silence', 'silence'),
       (42, 'tar-shot', 'tar-shot') ON CONFLICT ("id") DO NOTHING;
COMMIT;


-- ----------------------------
-- Records of move_category
-- ----------------------------
BEGIN;
INSERT INTO "move_category" ("id", "description", "internal_name", "name")
VALUES (0, 'Inflicts damage', 'damage', 'damage'),
       (1, 'No damage; inflicts status ailment', 'ailment', 'ailment'),
       (2, 'No damage; lowers target’s stats or raises user’s stats', 'net-good-stats', 'net-good-stats'),
       (3, 'No damage; heals the user', 'heal', 'heal'),
       (4, 'Inflicts damage; inflicts status ailment', 'damage+ailment', 'damage+ailment'),
       (5, 'No damage; inflicts status ailment; raises target’s stats', 'swagger', 'swagger'),
       (6, 'Inflicts damage; lowers target’s stats', 'damage+lower', 'damage+lower'),
       (7, 'Inflicts damage; raises user’s stats', 'damage+raise', 'damage+raise'),
       (8, 'Inflicts damage; absorbs damage done to heal the user', 'damage+heal', 'damage+heal'),
       (9, 'One-hit KO', 'ohko', 'ohko'),
       (10, 'Effect on the whole field', 'whole-field-effect', 'whole-field-effect'),
       (11, 'Effect on one side of the field', 'field-effect', 'field-effect'),
       (12, 'Forces target to switch out', 'force-switch', 'force-switch'),
       (13, 'Unique effect', 'unique', 'unique') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of move_damage_class
-- ----------------------------
BEGIN;
INSERT INTO "move_damage_class" (id, description, internal_name, name)
VALUES (1, '没有伤害', 'status', '变化'),
       (2, '物理伤害，受攻击和防御影响', 'physical', '物理'),
       (3, '特殊伤害，受特攻和特防影响', 'special', '特殊') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of move_learn_method
-- ----------------------------
BEGIN;
INSERT INTO "move_learn_method" (id, description, internal_name, name)
VALUES (1, 'Learned when a Pokémon reaches a certain level.', 'level-up', 'Level up'),
       (2, 'Appears on a newly-hatched Pokémon, if the father had the same move.', 'egg', 'Egg'),
       (3, 'Can be taught at any time by an NPC.', 'tutor', 'Tutor'),
       (4, 'Can be taught at any time by using a TM or HM.', 'machine', 'Machine'),
       (5,
        'Learned when a non-rental Pikachu helps beat Prime Cup Master Ball R-2.  It must participate in every battle, and you must win with no continues.',
        'stadium-surfing-pikachu', 'Stadium: Surfing Pikachu'),
       (6, 'Appears on a Pichu whose mother was holding a Light Ball.  The father cannot be Ditto.', 'light-ball-egg',
        'Volt Tackle Pichu'),
       (7, 'Appears on a Shadow Pokémon as it becomes increasingly purified.', 'colosseum-purification',
        'Colosseum: Purification'),
       (8, 'Appears on a Snatched Shadow Pokémon.', 'xd-shadow', 'XD: Shadow'),
       (9, 'Appears on a Shadow Pokémon as it becomes increasingly purified.', 'xd-purification', 'XD: Purification'),
       (10,
        'Appears when Rotom or Cosplay Pikachu changes form.  Disappears if the Pokémon becomes another form and this move can only be learned by form change.',
        'form-change', 'Form Change'),
       (11,
        'Can be taught using the Zygarde Cube.  Must find the corresponding Zygarde Core first in Sun/Moon.  All moves are available immediately in Ultra Sun/Ultra Moon.',
        'zygarde-cube', 'Zygarde Cube') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of move_target
-- ----------------------------
BEGIN;
INSERT INTO "move_target" ("id", "description", "internal_name", "name")
VALUES (1, 'One specific move.  How this move is chosen depends upon on the move being used.', 'specific-move',
        'specific-move'),
       (2, 'One other Pokémon on the field, selected by the trainer.  Stolen moves reuse the same target.',
        'selected-pokemon-me-first', 'selected-pokemon-me-first'),
       (3, 'The user''s ally (if any).', 'ally', 'ally'),
       (4, 'The user''s side of the field.  Affects the user and its ally (if any).', 'users-field', 'users-field'),
       (5, 'Either the user or its ally, selected by the trainer.', 'user-or-ally', 'user-or-ally'),
       (6, 'The opposing side of the field.  Affects opposing Pokémon.', 'opponents-field', 'opponents-field'),
       (7, 'The user.', 'user', 'user'),
       (8, 'One opposing Pokémon, selected at random.', 'random-opponent', 'random-opponent'),
       (9, 'Every other Pokémon on the field.', 'all-other-pokemon', 'all-other-pokemon'),
       (10, 'One other Pokémon on the field, selected by the trainer.', 'selected-pokemon', 'selected-pokemon'),
       (11, 'All opposing Pokémon.', 'all-opponents', 'all-opponents'),
       (12, 'The entire field.  Affects all Pokémon.', 'entire-field', 'entire-field'),
       (13, 'The user and its allies.', 'user-and-allies', 'user-and-allies'),
       (14, 'Every Pokémon on the field.', 'all-pokemon', 'all-pokemon'),
       (15, 'All of the user''s allies.', 'all-allies', 'all-allies'),
       (16, NULL, 'fainting-pokemon', 'fainting-pokemon') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of menu
-- ----------------------------
BEGIN;
INSERT INTO "menu" ("id", "parent_id", "disabled", "extra", "icon", "key", "label", "show", "path", "name", "redirect",
                    "component", "order", "pinned", "show_tab", "enable_multi_tab")
VALUES (1, NULL, false, null, 'icon-[mage--dashboard-chart]', 'dashboard', '仪表板', true, 'dashboard', 'dashboard', '',
        'dashboard/index', 0, true, true, false),
       (2, NULL, false, null, 'icon-[ic--outline-dataset]', 'dataset', '数据集', true, 'dataset', 'dataset', '',
        'dataset',
        0, false, true, false),
       (3, 2, false, null, 'icon-[game-icons--barbed-star]', 'type', '属性管理', true, 'type', 'type', '',
        'dataset/type/index', 0, false, true, false),
       (4, 2, false, null, 'icon-[game-icons--beveled-star]', 'type-damage-relation', '属性克制管理', true,
        'type-damage-relation', 'type-damage-relation', '', 'dataset/type-damage-relation/index', 0, false, true,
        false),
       (5, 2, false, null, 'icon-[game-icons--diamond-hard]', 'berry-firmness', '树果硬度管理', true, 'berry-firmness',
        'berry-firmness', '', 'dataset/berry-firmness/index', 0, false, true, false),
       (6, 2, false, null, 'icon-[game-icons--opened-food-can]', 'berry-flavor', '树果风味管理', true, 'berry-flavor',
        'berry-flavor', '', 'dataset/berry-flavor/index', 0, false, true, false),
       (7, 2, false, null, 'icon-[game-icons--elderberry]', 'berry', '树果管理', true, 'berry', 'berry', '',
        'dataset/berry/index', 0, false, true, false) ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of oauth_registered_client
-- ----------------------------
BEGIN;
INSERT INTO "oauth_registered_client" ("id", "client_id", "client_id_issued_at", "client_secret",
                                       "client_secret_expires_at", "client_name", "client_authentication_methods",
                                       "authorization_grant_types", "redirect_uris", "post_logout_redirect_uris",
                                       "scopes", "require_proof_key", "require_authorization_consent", "jwk_set_url",
                                       "token_endpoint_authentication_signing_algorithm", "x509_certificate_subject_dn",
                                       "authorization_code_time_to_live", "access_token_time_to_live",
                                       "access_token_format", "device_code_time_to_live", "reuse_refresh_tokens",
                                       "refresh_token_time_to_live", "id_token_signature_algorithm",
                                       "x509_certificate_bound_access_tokens")
VALUES ('1', 'client', '2025-08-12 16:11:22+00', '{noop}client', '5202-08-12 16:11:22+00', '测试客户端的客户端',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'refresh_token,client_credentials,password', '',
        'http://localhost:8080', 'openid,profile', false, false, '', 'RS256', '', '2h', '2h', 'self-contained', '1h',
        true, '30d', 'RS256', false),
       ('2', 'test', '2025-08-12 16:11:22+00', '{noop}test', '5202-08-12 16:11:22+00', '测试REFERENCE模式的客户端',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'refresh_token,client_credentials,password', '',
        'http://localhost:8080', 'openid,profile', false, false, '', 'RS256', '', '2h', '2h', 'reference', '1h', true,
        '30d', 'RS256', false) ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of stat
-- ----------------------------
BEGIN;
INSERT INTO "stat" ("id", "game_index", "internal_name", "is_battle_only", "name", "move_damage_class_id")
VALUES (1, 1, 'hp', false, 'HP', NULL),
       (2, 2, 'attack', false, '攻击', 2),
       (3, 3, 'defense', false, '防御', 2),
       (4, 5, 'special-attack', false, '特攻', 3),
       (5, 6, 'special-defense', false, '特防', 3),
       (6, 4, 'speed', false, '速度', NULL),
       (7, 0, 'accuracy', true, '命中', NULL),
       (8, 0, 'evasion', true, '闪避', NULL) ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of nature
-- ----------------------------
BEGIN;
INSERT INTO "nature" ("id", "internal_name", "name", "decreased_stat_id", "hates_berry_flavor_id", "increased_stat_id",
                      "likes_berry_flavor_id")
VALUES (1, 'hardy', '勤奋', NULL, NULL, NULL, NULL),
       (2, 'bold', '大胆', 2, 1, 3, 5),
       (3, 'modest', '内敛', 2, 1, 4, 2),
       (4, 'calm', '温和', 2, 1, 5, 4),
       (5, 'timid', '胆小', 2, 1, 6, 3),
       (6, 'lonely', '怕寂寞', 3, 5, 2, 1),
       (7, 'docile', '坦率', NULL, NULL, NULL, NULL),
       (8, 'mild', '慢吞吞', 3, 5, 4, 2),
       (9, 'gentle', '温顺', 3, 5, 5, 4),
       (10, 'hasty', '急躁', 3, 5, 6, 3),
       (11, 'adamant', '固执', 4, 2, 2, 1),
       (12, 'impish', '淘气', 4, 2, 3, 5),
       (13, 'bashful', '害羞', NULL, NULL, NULL, NULL),
       (14, 'careful', '慎重', 4, 2, 5, 4),
       (15, 'rash', '马虎', 5, 4, 4, 2),
       (16, 'jolly', '爽朗', 4, 2, 6, 3),
       (17, 'naughty', '顽皮', 5, 4, 2, 1),
       (18, 'lax', '乐天', 5, 4, 3, 5),
       (19, 'quirky', '浮躁', NULL, NULL, NULL, NULL),
       (20, 'naive', '天真', 5, 4, 6, 3),
       (21, 'brave', '勇敢', 6, 3, 2, 1),
       (22, 'relaxed', '悠闲', 6, 3, 3, 5),
       (23, 'quiet', '冷静', 6, 3, 4, 2),
       (24, 'sassy', '自大', 6, 3, 5, 4),
       (25, 'serious', '认真', NULL, NULL, NULL, NULL) ON CONFLICT ("id") DO NOTHING;
COMMIT;
-- ----------------------------
-- Records of type
-- ----------------------------
BEGIN;
INSERT INTO "type" ("id", "internal_name", "name")
VALUES (1, 'normal', '一般'),
       (2, 'fighting', '格斗'),
       (3, 'flying', '飞行'),
       (4, 'poison', '毒'),
       (5, 'ground', '地面'),
       (6, 'rock', '岩石'),
       (7, 'bug', '虫'),
       (8, 'ghost', '幽灵'),
       (9, 'steel', '钢'),
       (10, 'fire', '火'),
       (11, 'water', '水'),
       (12, 'grass', '草'),
       (13, 'electric', '电'),
       (14, 'psychic', '超能力'),
       (15, 'ice', '冰'),
       (16, 'dragon', '龙'),
       (17, 'dark', '恶'),
       (18, 'fairy', '妖精'),
       (19, 'stellar', '星晶'),
       (10001, 'unknown', '???'),
       (10002, 'shadow', '暗') ON CONFLICT ("id") DO NOTHING;
COMMIT;



-- ----------------------------
-- Records of type_damage_relation
-- ----------------------------
BEGIN;
INSERT INTO "type_damage_relation" ("attacking_type_id", "defending_type_id", "multiplier")
VALUES (1, 1, 1),
       (1, 2, 1),
       (1, 3, 1),
       (1, 4, 1),
       (1, 5, 1),
       (1, 6, 0.5),
       (1, 7, 1),
       (1, 8, 0),
       (1, 9, 0.5),
       (1, 10, 1),
       (1, 11, 1),
       (1, 12, 1),
       (1, 13, 1),
       (1, 14, 1),
       (1, 15, 1),
       (1, 16, 1),
       (1, 17, 1),
       (1, 18, 1),
       (2, 1, 2),
       (2, 2, 1),
       (2, 3, 0.5),
       (2, 4, 0.5),
       (2, 5, 1),
       (2, 6, 2),
       (2, 7, 0.5),
       (2, 8, 0),
       (2, 9, 2),
       (2, 10, 1),
       (2, 11, 1),
       (2, 12, 1),
       (2, 13, 1),
       (2, 14, 0.5),
       (2, 15, 2),
       (2, 16, 1),
       (2, 17, 2),
       (2, 18, 0.5),
       (3, 1, 1),
       (3, 2, 2),
       (3, 3, 1),
       (3, 4, 1),
       (3, 5, 1),
       (3, 6, 0.5),
       (3, 7, 2),
       (3, 8, 1),
       (3, 9, 0.5),
       (3, 10, 1),
       (3, 11, 1),
       (3, 12, 2),
       (3, 13, 0.5),
       (3, 14, 1),
       (3, 15, 1),
       (3, 16, 1),
       (3, 17, 1),
       (3, 18, 1),
       (4, 1, 1),
       (4, 2, 1),
       (4, 3, 1),
       (4, 4, 0.5),
       (4, 5, 0.5),
       (4, 6, 0.5),
       (4, 7, 1),
       (4, 8, 0.5),
       (4, 9, 0),
       (4, 10, 1),
       (4, 11, 1),
       (4, 12, 2),
       (4, 13, 1),
       (4, 14, 1),
       (4, 15, 1),
       (4, 16, 1),
       (4, 17, 1),
       (4, 18, 2),
       (5, 1, 1),
       (5, 2, 1),
       (5, 3, 0),
       (5, 4, 2),
       (5, 5, 1),
       (5, 6, 2),
       (5, 7, 0.5),
       (5, 8, 1),
       (5, 9, 2),
       (5, 10, 2),
       (5, 11, 1),
       (5, 12, 0.5),
       (5, 13, 2),
       (5, 14, 1),
       (5, 15, 1),
       (5, 16, 1),
       (5, 17, 1),
       (5, 18, 1),
       (6, 1, 1),
       (6, 2, 0.5),
       (6, 3, 2),
       (6, 4, 1),
       (6, 5, 0.5),
       (6, 6, 1),
       (6, 7, 2),
       (6, 8, 1),
       (6, 9, 0.5),
       (6, 10, 2),
       (6, 11, 1),
       (6, 12, 1),
       (6, 13, 1),
       (6, 14, 1),
       (6, 15, 2),
       (6, 16, 1),
       (6, 17, 1),
       (6, 18, 1),
       (7, 1, 1),
       (7, 2, 0.5),
       (7, 3, 0.5),
       (7, 4, 0.5),
       (7, 5, 1),
       (7, 6, 1),
       (7, 7, 1),
       (7, 8, 0.5),
       (7, 9, 0.5),
       (7, 10, 0.5),
       (7, 11, 1),
       (7, 12, 2),
       (7, 13, 1),
       (7, 14, 2),
       (7, 15, 1),
       (7, 16, 1),
       (7, 17, 2),
       (7, 18, 0.5),
       (8, 1, 0),
       (8, 2, 1),
       (8, 3, 1),
       (8, 4, 1),
       (8, 5, 1),
       (8, 6, 0.5),
       (8, 7, 1),
       (8, 8, 2),
       (8, 9, 1),
       (8, 10, 1),
       (8, 11, 1),
       (8, 12, 1),
       (8, 13, 1),
       (8, 14, 2),
       (8, 15, 1),
       (8, 16, 1),
       (8, 17, 0.5),
       (8, 18, 1),
       (9, 1, 1),
       (9, 2, 1),
       (9, 3, 1),
       (9, 4, 1),
       (9, 5, 1),
       (9, 6, 2),
       (9, 7, 1),
       (9, 8, 1),
       (9, 9, 0.5),
       (9, 10, 0.5),
       (9, 11, 0.5),
       (9, 12, 1),
       (9, 13, 0.5),
       (9, 14, 1),
       (9, 15, 2),
       (9, 16, 1),
       (9, 17, 1),
       (9, 18, 2),
       (10, 1, 1),
       (10, 2, 1),
       (10, 3, 1),
       (10, 4, 1),
       (10, 5, 1),
       (10, 6, 0.5),
       (10, 7, 2),
       (10, 8, 1),
       (10, 9, 2),
       (10, 10, 0.5),
       (10, 11, 0.5),
       (10, 12, 2),
       (10, 13, 1),
       (10, 14, 1),
       (10, 15, 2),
       (10, 16, 0.5),
       (10, 17, 1),
       (10, 18, 1),
       (11, 1, 1),
       (11, 2, 1),
       (11, 3, 1),
       (11, 4, 1),
       (11, 5, 2),
       (11, 6, 2),
       (11, 7, 1),
       (11, 8, 1),
       (11, 9, 1),
       (11, 10, 2),
       (11, 11, 0.5),
       (11, 12, 0.5),
       (11, 13, 1),
       (11, 14, 1),
       (11, 15, 1),
       (11, 16, 0.5),
       (11, 17, 1),
       (11, 18, 1),
       (12, 1, 1),
       (12, 2, 1),
       (12, 3, 0.5),
       (12, 4, 0.5),
       (12, 5, 2),
       (12, 6, 2),
       (12, 7, 0.5),
       (12, 8, 1),
       (12, 9, 0.5),
       (12, 10, 0.5),
       (12, 11, 2),
       (12, 12, 0.5),
       (12, 13, 1),
       (12, 14, 1),
       (12, 15, 1),
       (12, 16, 0.5),
       (12, 17, 1),
       (12, 18, 1),
       (13, 1, 1),
       (13, 2, 1),
       (13, 3, 2),
       (13, 4, 1),
       (13, 5, 0),
       (13, 6, 1),
       (13, 7, 1),
       (13, 8, 1),
       (13, 9, 1),
       (13, 10, 1),
       (13, 11, 2),
       (13, 12, 0.5),
       (13, 13, 0.5),
       (13, 14, 1),
       (13, 15, 1),
       (13, 16, 0.5),
       (13, 17, 1),
       (13, 18, 1),
       (14, 1, 1),
       (14, 2, 2),
       (14, 3, 1),
       (14, 4, 2),
       (14, 5, 1),
       (14, 6, 1),
       (14, 7, 1),
       (14, 8, 1),
       (14, 9, 0.5),
       (14, 10, 1),
       (14, 11, 1),
       (14, 12, 1),
       (14, 13, 1),
       (14, 14, 0.5),
       (14, 15, 1),
       (14, 16, 1),
       (14, 17, 0),
       (14, 18, 1),
       (15, 1, 1),
       (15, 2, 1),
       (15, 3, 2),
       (15, 4, 1),
       (15, 5, 2),
       (15, 6, 1),
       (15, 7, 1),
       (15, 8, 1),
       (15, 9, 0.5),
       (15, 10, 0.5),
       (15, 11, 0.5),
       (15, 12, 2),
       (15, 13, 1),
       (15, 14, 1),
       (15, 15, 0.5),
       (15, 16, 2),
       (15, 17, 1),
       (15, 18, 1),
       (16, 1, 1),
       (16, 2, 1),
       (16, 3, 1),
       (16, 4, 1),
       (16, 5, 1),
       (16, 6, 1),
       (16, 7, 1),
       (16, 8, 1),
       (16, 9, 0.5),
       (16, 10, 1),
       (16, 11, 1),
       (16, 12, 1),
       (16, 13, 1),
       (16, 14, 1),
       (16, 15, 1),
       (16, 16, 2),
       (16, 17, 1),
       (16, 18, 0),
       (17, 1, 1),
       (17, 2, 0.5),
       (17, 3, 1),
       (17, 4, 1),
       (17, 5, 1),
       (17, 6, 1),
       (17, 7, 1),
       (17, 8, 2),
       (17, 9, 1),
       (17, 10, 1),
       (17, 11, 1),
       (17, 12, 1),
       (17, 13, 1),
       (17, 14, 2),
       (17, 15, 1),
       (17, 16, 1),
       (17, 17, 0.5),
       (17, 18, 0.5),
       (18, 1, 1),
       (18, 2, 2),
       (18, 3, 1),
       (18, 4, 0.5),
       (18, 5, 1),
       (18, 6, 1),
       (18, 7, 1),
       (18, 8, 1),
       (18, 9, 0.5),
       (18, 10, 0.5),
       (18, 11, 1),
       (18, 12, 1),
       (18, 13, 1),
       (18, 14, 1),
       (18, 15, 1),
       (18, 16, 2),
       (18, 17, 2),
       (18, 18, 1) ON CONFLICT ("attacking_type_id", "defending_type_id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of berry
-- ----------------------------
BEGIN;
INSERT INTO "berry" ("id", "bulk", "growth_time", "internal_name", "max_harvest", "name", "natural_gift_power",
                     "smoothness", "soil_dryness", "berry_firmness_id", "natural_gift_type_id")
VALUES (1, 20, 3, 'cheri', 5, '樱子果', 60, 25, 15, 2, 10),
       (2, 80, 3, 'chesto', 5, '零余果', 60, 25, 15, 5, 11),
       (3, 40, 3, 'pecha', 5, '桃桃果', 60, 25, 15, 1, 13),
       (4, 32, 3, 'rawst', 5, '莓莓果', 60, 25, 15, 3, 12),
       (5, 50, 3, 'aspear', 5, '利木果', 60, 25, 15, 5, 15),
       (6, 28, 4, 'leppa', 5, '苹野果', 60, 20, 15, 4, 2),
       (7, 35, 4, 'oran', 5, '橙橙果', 60, 20, 15, 5, 4),
       (8, 47, 4, 'persim', 5, '柿仔果', 60, 20, 15, 3, 5),
       (9, 34, 12, 'lum', 5, '木子果', 60, 20, 8, 5, 3),
       (10, 95, 8, 'sitrus', 5, '文柚果', 60, 20, 7, 4, 14),
       (11, 100, 5, 'figy', 5, '勿花果', 60, 25, 10, 2, 7),
       (12, 115, 5, 'wiki', 5, '异奇果', 60, 25, 10, 3, 6),
       (13, 126, 5, 'mago', 5, '芒芒果', 60, 25, 10, 3, 8),
       (14, 64, 5, 'aguav', 5, '乐芭果', 60, 25, 10, 5, 16),
       (15, 223, 5, 'iapapa', 5, '芭亚果', 60, 25, 10, 2, 17),
       (16, 120, 2, 'razz', 10, '蔓莓果', 60, 20, 35, 4, 9),
       (17, 108, 2, 'bluk', 10, '墨莓果', 70, 20, 35, 2, 10),
       (18, 77, 2, 'nanab', 10, '蕉香果', 70, 20, 35, 4, 11),
       (19, 74, 2, 'wepear', 10, '西梨果', 70, 20, 35, 5, 13),
       (20, 80, 2, 'pinap', 10, '凰梨果', 70, 20, 35, 3, 12),
       (21, 135, 8, 'pomeg', 5, '榴石果', 70, 20, 8, 4, 15),
       (22, 150, 8, 'kelpsy', 5, '藻根果', 70, 20, 8, 3, 2),
       (23, 110, 8, 'qualot', 5, '比巴果', 70, 20, 8, 3, 4),
       (24, 162, 8, 'hondew', 5, '哈密果', 70, 20, 8, 3, 5),
       (25, 149, 8, 'grepa', 5, '萄葡果', 70, 20, 8, 2, 3),
       (26, 200, 8, 'tamato', 5, '茄番果', 70, 30, 8, 2, 14),
       (27, 75, 6, 'cornn', 10, '玉黍果', 70, 30, 10, 3, 7),
       (28, 140, 6, 'magost', 10, '岳竹果', 70, 30, 10, 3, 6),
       (29, 226, 6, 'rabuta', 10, '茸丹果', 70, 30, 10, 2, 8),
       (30, 285, 6, 'nomel', 10, '檬柠果', 70, 30, 10, 5, 16),
       (31, 133, 15, 'spelon', 15, '刺角果', 70, 35, 8, 2, 17),
       (32, 244, 15, 'pamtre', 15, '椰木果', 70, 35, 8, 1, 9),
       (33, 250, 15, 'watmel', 15, '瓜西果', 80, 35, 8, 2, 10),
       (34, 280, 15, 'durin', 15, '金枕果', 80, 35, 8, 3, 11),
       (35, 300, 15, 'belue', 15, '靛莓果', 80, 35, 8, 1, 13),
       (36, 90, 18, 'occa', 5, '巧可果', 60, 30, 6, 5, 10),
       (37, 33, 18, 'passho', 5, '千香果', 60, 30, 6, 2, 11),
       (38, 250, 18, 'wacan', 5, '烛木果', 60, 30, 6, 1, 13),
       (39, 156, 18, 'rindo', 5, '罗子果', 60, 30, 6, 2, 12),
       (40, 135, 18, 'yache', 5, '番荔果', 60, 30, 6, 4, 15),
       (41, 77, 18, 'chople', 5, '莲蒲果', 60, 30, 6, 2, 2),
       (42, 90, 18, 'kebia', 5, '通通果', 60, 30, 6, 3, 4),
       (43, 42, 18, 'shuca', 5, '腰木果', 60, 30, 6, 2, 5),
       (44, 278, 18, 'coba', 5, '棱瓜果', 60, 30, 6, 4, 3),
       (45, 252, 18, 'payapa', 5, '福禄果', 60, 30, 6, 2, 14),
       (46, 42, 18, 'tanga', 5, '扁樱果', 60, 35, 6, 1, 7),
       (47, 28, 18, 'charti', 5, '草蚕果', 60, 35, 6, 1, 6),
       (48, 144, 18, 'kasib', 5, '佛柑果', 60, 35, 6, 3, 8),
       (49, 23, 18, 'haban', 5, '莓榴果', 60, 35, 6, 2, 16),
       (50, 39, 18, 'colbur', 5, '刺耳果', 60, 35, 6, 5, 17),
       (51, 265, 18, 'babiri', 5, '霹霹果', 60, 35, 6, 5, 9),
       (52, 34, 18, 'chilan', 5, '灯浆果', 60, 35, 6, 1, 1),
       (53, 111, 24, 'liechi', 5, '枝荔果', 80, 40, 4, 4, 12),
       (54, 33, 24, 'ganlon', 5, '龙睛果', 80, 40, 4, 4, 15),
       (55, 95, 24, 'salac', 5, '沙鳞果', 80, 40, 4, 4, 2),
       (56, 237, 24, 'petaya', 5, '龙火果', 80, 40, 4, 4, 4),
       (57, 75, 24, 'apicot', 5, '杏仔果', 80, 40, 4, 3, 5),
       (58, 97, 24, 'lansat', 5, '兰萨果', 80, 50, 4, 2, 3),
       (59, 153, 24, 'starf', 5, '星桃果', 80, 50, 4, 5, 14),
       (60, 155, 24, 'enigma', 5, '谜芝果', 80, 60, 7, 3, 7),
       (61, 41, 24, 'micle', 5, '奇秘果', 80, 60, 7, 2, 6),
       (62, 267, 24, 'custap', 5, '释陀果', 80, 60, 7, 5, 8),
       (63, 33, 24, 'jaboca', 5, '嘉珍果', 80, 60, 7, 2, 16),
       (64, 52, 24, 'rowap', 5, '雾莲果', 80, 60, 7, 1, 17) ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of pokemon_color
-- ----------------------------
BEGIN;
INSERT INTO "pokemon_color" ("id", "internal_name", "name")
VALUES (1, 'black', '黑色'),
       (2, 'blue', '蓝色'),
       (3, 'brown', '褐色'),
       (4, 'gray', '灰色'),
       (5, 'green', '绿色'),
       (6, 'pink', '粉红色'),
       (7, 'purple', '紫色'),
       (8, 'red', '红色'),
       (9, 'white', '白色'),
       (10, 'yellow', '黄色') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of pokemon_habitat
-- ----------------------------
BEGIN;
INSERT INTO "pokemon_habitat" ("id", "internal_name", "name")
VALUES (1, 'cave', 'cave'),
       (2, 'forest', 'forest'),
       (3, 'grassland', 'grassland'),
       (4, 'mountain', 'mountain'),
       (5, 'rare', 'rare'),
       (6, 'rough-terrain', 'rough terrain'),
       (7, 'sea', 'sea'),
       (8, 'urban', 'urban'),
       (9, 'waters-edge', 'water''s edge') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of pokemon_shape
-- ----------------------------
BEGIN;
INSERT INTO "pokemon_shape" ("id", "internal_name", "name")
VALUES (1, 'ball', 'Ball'),
       (2, 'squiggle', 'Squiggle'),
       (3, 'fish', 'Fish'),
       (4, 'arms', 'Arms'),
       (5, 'blob', 'Blob'),
       (6, 'upright', 'Upright'),
       (7, 'legs', 'Legs'),
       (8, 'quadruped', 'Quadruped'),
       (9, 'wings', 'Wings'),
       (10, 'tentacles', 'Tentacles'),
       (11, 'heads', 'Heads'),
       (12, 'humanoid', 'Humanoid'),
       (13, 'bug-wings', 'Bug wings'),
       (14, 'armor', 'Armor') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of egg_group
-- ----------------------------
BEGIN;
INSERT INTO "egg_group" ("id", "characteristics", "internal_name", "name", "text")
VALUES (1, '这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。', 'monster', '怪兽',
        '像是怪兽一样，或者比较野性。'),
       (2, '这个蛋群的宝可梦大多原型基于两栖动物和水边生活的多栖动物。', 'water1', '水中1', '可以两栖或多栖。'),
       (3, '这个蛋群的宝可梦大多原型基于昆虫和节肢动物。', 'bug', '虫', '外表长得像虫子。'),
       (4, '这个蛋群的宝可梦原型大多基于鸟类、蝙蝠、会飞的爬行动物甚至是神话中会飞的小妖精。', 'flying', '飞行',
        '外表长得像鸟、蝙蝠等会飞行的生物。'),
       (5, '这个蛋群的宝可梦大多原型基于哺乳动物和爬行动物，以及翅膀退化的鸟类。', 'ground', '陆上',
        '最大的蛋群，住在陆地上的宝可梦基本都属于这个群。'),
       (6, '这个蛋群的宝可梦大多原型基于可爱的小型动物和神话中的妖精。', 'fairy', '妖精',
        '外表可爱或具有传说灵异性质的生物。'),
       (7, '这个蛋群的宝可梦大多原型基于植物和真菌，以及身上长有植物或真菌的动物。', 'plant', '植物', '外表长得像植物。'),
       (8, '这个蛋群的宝可梦都是直立行走的人型生物。', 'humanshape', '人型', '两足行走。'),
       (9, '这个蛋群的宝可梦大多原型基于非鱼类的深海水生动物。', 'water3', '水中３', '水中无脊椎动物。'),
       (10, '这个蛋群的宝可梦大多原型基于无机物和身上带有无机物的生物。', 'mineral', '矿物', '结晶或硅基生物。'),
       (11, '这个蛋群的宝可梦大多原型基于软体动物、灵体，以及身体柔软的生物或非生物。', 'indeterminate', '不定形',
        '没有固定外表。'),
       (12, '这个蛋群的宝可梦大多原型基于鱼类，乌贼以及章鱼。', 'water2', '水中2', '像是鱼一类的脊椎动物。'),
       (13,
        '这个蛋群只有百变怪。处于这个蛋群的宝可梦可以与除未发现群和百变怪群外的任何蛋群的宝可梦生蛋，蛋的种类必然是另一方。',
        'ditto', '百变怪',
        '顾名思义，百变怪是这个群中唯一的宝可梦，可以和除了未发现群及百变怪群以外的所有宝可梦生蛋（无视性别）。'),
       (14, '这个蛋群的宝可梦大多原型基于传说中的龙以及与龙有关的动物（蜥蜴、海马等）。', 'dragon', '龙',
        '外表长得像龙或者具有龙的特质的宝可梦。'),
       (15, '属于此蛋群的宝可梦都无法生蛋。', 'no-eggs', '未发现',
        '不能和任何宝可梦生蛋。') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of role
-- ----------------------------
BEGIN;
INSERT INTO "role" ("id", "code", "name", "enabled")
VALUES (1, 'ROLE_SUPER_ADMIN', '超级管理员', true),
       (2, 'ROLE_TEST', '测试员', true) ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of role_menu_relation
-- ----------------------------
BEGIN;
INSERT INTO "role_menu_relation" ("role_id", "menu_id")
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7) ON CONFLICT ("role_id", "menu_id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO "user" ("id", "username", "password")
VALUES (1, 'admin',
        '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G') ON CONFLICT ("id") DO NOTHING;
COMMIT;

-- ----------------------------
-- Records of user_role_relation
-- ----------------------------
BEGIN;
INSERT INTO "user_role_relation" ("user_id", "role_id")
VALUES (1, 1),
       (1, 2) ON CONFLICT ("user_id", "role_id") DO NOTHING;
COMMIT;
