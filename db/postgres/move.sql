BEGIN;
insert into public.move (id, accuracy, ailment_chance, crit_rate, drain, effect, effect_chance, flinch_chance, healing,
                         internal_name, max_hits, max_turns, min_hits, min_turns, name, power, pp, priority,
                         short_effect, stat_chance, text, move_ailment_id, move_category_id, move_damage_class_id,
                         move_target_id, type_id)
values (1, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'pound', null, null, null, null, '拍击', 40, 35, 0,
        'Inflicts regular damage with no additional effect.', 0, '使用长长的尾巴或手等
拍打对手进行攻击。', 0, 0, 2, 10, 1),
       (2, 100, 0, 1, 0, 'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.',
        null, 0, 0, 'karate-chop', null, null, null, null, '空手劈', 50, 25, 0,
        'Has an increased chance for a critical hit.', 0, '用锋利的手刀
劈向对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 2),
       (3, 85, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'double-slap', 5, null, 2, null, '连环巴掌', 15, 10, 0, 'Hits 2-5 times in one turn.', 0, '用连环巴掌
拍打对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (4, 85, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'comet-punch', 5, null, 2, null, '连续拳', 18, 15, 0, 'Hits 2-5 times in one turn.', 0, '用拳头怒涛般的
殴打对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (5, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'mega-punch', null, null, null, null, '百万吨重拳', 80,
        20, 0, 'Inflicts regular damage with no additional effect.', 0, '用充满力量的拳头攻击对手。', 0, 0, 2, 10, 1),
       (6, 100, 0, 0, 0,
        'Inflicts regular damage.  After the battle ends, the winner receives five times the user’s level in extra money for each time this move was used.',
        null, 0, 0, 'pay-day', null, null, null, null, '聚宝功', 40, 20, 0,
        'Scatters money on the ground worth five times the user’s level.', 0, '向对手的身体
投掷小金币进行攻击。
战斗后可以拿到钱。', 0, 0, 2, 10, 1),
       (7, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'fire-punch', null, null, null, null, '火焰拳', 75, 15, 0, 'Has a $effect_chance% chance to burn the target.',
        0, '用充满火焰的拳头攻击对手。
有时会让对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (8, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target.', 10, 0, 0,
        'ice-punch', null, null, null, null, '冰冻拳', 75, 15, 0, 'Has a $effect_chance% chance to freeze the target.',
        0, '用充满寒气的拳头攻击对手。
有时会让对手陷入冰冻状态。', 3, 4, 2, 10, 15),
       (9, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 10, 0, 0,
        'thunder-punch', null, null, null, null, '雷电拳', 75, 15, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '用充满电流的拳头攻击对手。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 13),
       (10, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'scratch', null, null, null, null, '抓', 40, 35, 0,
        'Inflicts regular damage with no additional effect.', 0, '用坚硬且无比锋利的爪子
抓对手进行攻击。', 0, 0, 2, 10, 1),
       (11, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'vice-grip', null, null, null, null, '夹住', 55, 30,
        0, 'Inflicts regular damage with no additional effect.', 0, '将对手从两侧夹住，
给予伤害。', 0, 0, 2, 10, 1),
       (12, 30, 0, 0, 0, 'Inflicts damage equal to the target’s max HP.  Ignores accuracy and evasion modifiers.  This move’s accuracy is 30% plus 1% for each level the user is higher than the target.  If the user is a lower level than the target, this move will fail.

Because this move inflicts a specific and finite amount of damage, endure still prevents the target from fainting.

The effects of lock on, mind reader, and no guard still apply, as long as the user is equal or higher level than the target.  However, they will not give this move a chance to break through detect or protect.',
        null, 0, 0, 'guillotine', null, null, null, null, '断头钳', null, 5, 0, 'Causes a one-hit KO.', 0, '用大钳子或剪刀等
夹断对手进行攻击。
只要命中就会一击濒死。', 0, 9, 2, 10, 1),
       (13, 100, 0, 1, 0, 'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.  User charges for one turn before attacking.

This move cannot be selected by sleep talk.', null, 0, 0, 'razor-wind', null, null, null, null, '旋风刀', 80, 10, 0,
        'Requires a turn to charge before attacking.', 0, '制造风之刃，
于第２回合攻击对手。
容易击中要害。', 0, 0, 3, 11, 1),
       (14, null, 0, 0, 0, 'Raises the user’s Attack by two stages.', null, 0, 0, 'swords-dance', null, null, null,
        null, '剑舞', null, 20, 0, 'Raises the user’s Attack by two stages.', 0, '激烈地跳起战舞提高气势。
大幅提高自己的攻击。', 0, 2, 1, 7, 1),
       (15, 95, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'cut', null, null, null, null, '居合斩', 50, 30, 0,
        'Inflicts regular damage with no additional effect.', 0, '用镰刀或爪子等
切斩对手进行攻击。', 0, 0, 2, 10, 1),
       (16, 100, 0, 0, 0, 'Inflicts regular damage.

If the target is under the effect of bounce, fly, or sky drop, this move will hit with double power.', null, 0, 0,
        'gust', null, null, null, null, '起风', 40, 35, 0, 'Inflicts regular damage and can hit Pokémon in the air.', 0, '用翅膀将刮起的狂风
袭向对手进行攻击。', 0, 0, 3, 10, 3),
       (17, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'wing-attack', null, null, null, null, '翅膀攻击', 60,
        35, 0, 'Inflicts regular damage with no additional effect.', 0, '大大地展开美丽的翅膀，
将其撞向对手进行攻击。', 0, 0, 2, 10, 3),
       (18, null, 0, 0, 0, 'Switches the target out for another of its trainer’s Pokémon selected at random.  Wild battles end immediately.

Doesn’t affect Pokémon with suction cups or under the effect of ingrain.', null, 0, 0, 'whirlwind', null, null, null,
        null, '吹飞', null, 20, -6, 'Immediately ends wild battles.  Forces trainers to switch Pokémon.', 0, '吹飞对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', 0, 12, 1, 10, 1),
       (19, 95, 0, 0, 0, 'Inflicts regular damage.  User flies high into the air for one turn, becoming immune to attack, and hits on the second turn.

During the immune turn, gust, hurricane, sky uppercut, smack down, thunder, twister, and whirlwind still hit the user normally.  gust and twister also have double power against the user.

The damage from hail and sandstorm still applies during the immune turn.

The user may be hit during its immune turn if under the effect of lock on, mind reader, or no guard.

This move cannot be used while gravity is in effect.

This move cannot be selected by sleep talk.', null, 0, 0, 'fly', null, null, null, null, '飞翔', 90, 15, 0,
        'User flies high into the air, dodging all attacks, and hits next turn.', 0, '第１回合飞上天空，
第２回合攻击对手。', 0, 0, 2, 10, 3),
       (39, 100, 0, 0, 0, 'Lowers the target’s Defense by one stage.', null, 0, 0, 'tail-whip', null, null, null, null,
        '摇尾巴', null, 30, 0, 'Lowers the target’s Defense by one stage.', 0, '可爱地左右摇晃尾巴，
诱使对手疏忽大意。
会降低对手的防御。', 0, 2, 1, 11, 1),
       (20, 85, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'bind', null, 6, null, 5, '绑紧', 15, 20, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '使用长长的身体或藤蔓等，
在４～５回合内
绑紧对手进行攻击。', 8, 4, 2, 10, 1),
       (21, 75, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'slam', null, null, null, null, '摔打', 80, 20, 0,
        'Inflicts regular damage with no additional effect.', 0, '使用长长的尾巴或藤蔓等
摔打对手进行攻击。', 0, 0, 2, 10, 1),
       (22, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'vine-whip', null, null, null, null, '藤鞭', 45, 25,
        0, 'Inflicts regular damage with no additional effect.', 0, '用如同鞭子般弯曲而细长的藤蔓
摔打对手进行攻击。', 0, 0, 2, 10, 12),
       (23, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.

Power is doubled against Pokémon that have used minimize since entering the field.', 30, 30, 0, 'stomp', null, null,
        null, null, '踩踏', 65, 20, 0, 'Has a $effect_chance% chance to make the target flinch.', 0, '用大脚踩踏对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 1),
       (24, 100, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', null, 0, 0, 'double-kick', 2, null, 2,
        null, '二连踢', 30, 30, 0, 'Hits twice in one turn.', 0, '用２只脚踢飞对手进行攻击。
连续２次给予伤害。', 0, 0, 2, 10, 2),
       (25, 75, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'mega-kick', null, null, null, null, '百万吨重踢', 120,
        5, 0, 'Inflicts regular damage with no additional effect.', 0, '使出力大无穷的重踢
踢飞对手进行攻击。', 0, 0, 2, 10, 1),
       (26, 95, 0, 0, 0, 'Inflicts regular damage. If this move misses, is blocked by protect or detect, or has no effect, the user takes damage equal to half of its max HP rounded down.

This move cannot be used while gravity is in effect.', null, 0, 0, 'jump-kick', null, null, null, null, '飞踢', 100, 10,
        0, 'If the user misses, it takes half the damage it would have inflicted in recoil.', 0, '使出高高的腾空踢攻击对手。
如果踢偏则自己会受到伤害。', 0, 0, 2, 10, 2),
       (27, 85, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30, 0,
        'rolling-kick', null, null, null, null, '回旋踢', 60, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '一边使身体快速旋转，
一边踢飞对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 2),
       (28, 100, 0, 0, 0, 'Lowers the target’s accuracy by one stage.', null, 0, 0, 'sand-attack', null, null, null,
        null, '泼沙', null, 15, 0, 'Lowers the target’s accuracy by one stage.', 0, '向对手脸上泼沙子，
从而降低命中率。', 0, 2, 1, 10, 5),
       (29, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'headbutt', null, null, null, null, '头锤', 70, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '将头伸出，
笔直地扑向对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 1),
       (30, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'horn-attack', null, null, null, null, '角撞', 65, 25,
        0, 'Inflicts regular damage with no additional effect.', 0, '用尖锐的角攻击对手。', 0, 0, 2, 10, 1),
       (31, 85, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'fury-attack', 5, null, 2, null, '乱击', 15, 20, 0, 'Hits 2-5 times in one turn.', 0, '用角或喙
刺向对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (32, 30, 0, 0, 0, 'Inflicts damage equal to the target’s max HP.  Ignores accuracy and evasion modifiers.  This move’s accuracy is 30% plus 1% for each level the user is higher than the target.  If the user is a lower level than the target, this move will fail.

Because this move inflicts a specific and finite amount of damage, endure still prevents the target from fainting.

The effects of lock on, mind reader, and no guard still apply, as long as the user is equal or higher level than the target.  However, they will not give this move a chance to break through detect or protect.',
        null, 0, 0, 'horn-drill', null, null, null, null, '角钻', null, 5, 0, 'Causes a one-hit KO.', 0, '用旋转的角
刺入对手进行攻击。
只要命中就会一击濒死。', 0, 9, 2, 10, 1),
       (33, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'tackle', null, null, null, null, '撞击', 40, 35, 0,
        'Inflicts regular damage with no additional effect.', 0, '用整个身体
撞向对手进行攻击。', 0, 0, 2, 10, 1),
       (34, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'body-slam', null, null, null, null, '泰山压顶', 85, 15, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '用整个身体
压住对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 1),
       (35, 90, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'wrap', null, 4, null, 2, '紧束', 15, 20, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '使用长长的身体或藤蔓等，
在４～５回合内
紧束对手进行攻击。', 8, 4, 2, 10, 1),
       (36, 85, 0, 0, -25, 'Inflicts regular damage.  User takes 1/4 the damage it inflicts in recoil.', null, 0, 0,
        'take-down', null, null, null, null, '猛撞', 90, 20, 0, 'User receives 1/4 the damage it inflicts in recoil.',
        0, '以惊人的气势
撞向对手进行攻击。
自己也会受到少许伤害。', 0, 0, 2, 10, 1),
       (37, 100, 0, 0, 0, 'Inflicts regular damage.  User is forced to attack with this move for 2–3 turns,selected at random.  After the last hit, the user becomes confused.

safeguard does not protect against the confusion from this move.', null, 0, 0, 'thrash', null, null, null, null,
        '大闹一番', 120, 10, 0, 'Hits every turn for 2-3 turns, then confuses the user.', 0, '在２～３回合内，
乱打一气地攻击对手。
大闹一番后自己会陷入混乱。', 0, 0, 2, 8, 1),
       (38, 100, 0, 0, -33, 'Inflicts regular damage.  User takes 1/3 the damage it inflicts in recoil.', null, 0, 0,
        'double-edge', null, null, null, null, '舍身冲撞', 120, 15, 0,
        'User receives 1/3 the damage inflicted in recoil.', 0, '拼命地猛撞向对手进行攻击。
自己也会受到不小的伤害。', 0, 0, 2, 10, 1),
       (40, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 30, 0, 0,
        'poison-sting', null, null, null, null, '毒针', 15, 35, 0, 'Has a $effect_chance% chance to poison the target.',
        0, '将有毒的针
刺入对手进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 2, 10, 4),
       (41, 100, 20, 0, 0,
        'Inflicts regular damage.  Hits twice in the same turn.  Has a $effect_chance% chance to poison the target.',
        20, 0, 0, 'twineedle', 2, null, 2, null, '双针', 25, 20, 0,
        'Hits twice in the same turn.  Has a $effect_chance% chance to poison the target.', 0, '将２根针刺入对手，
连续２次给予伤害。
有时会让对手陷入中毒状态。', 5, 4, 2, 10, 7),
       (42, 95, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'pin-missile', 5, null, 2, null, '飞弹针', 25, 20, 0, 'Hits 2-5 times in one turn.', 0, '向对手发射
锐针进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 7),
       (43, 100, 0, 0, 0, 'Lowers the target’s Defense by one stage.', 100, 0, 0, 'leer', null, null, null, null,
        '瞪眼', null, 30, 0, 'Lowers the target’s Defense by one stage.', 100, '用犀利的眼神使其害怕，
从而降低对手的防御。', 0, 2, 1, 11, 1),
       (44, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'bite', null, null, null, null, '咬住', 60, 25, 0, 'Has a $effect_chance% chance to make the target flinch.',
        0, '用尖锐的牙
咬住对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 17),
       (45, 100, 0, 0, 0, 'Lowers the target’s Attack by one stage.', null, 0, 0, 'growl', null, null, null, null,
        '叫声', null, 40, 0, 'Lowers the target’s Attack by one stage.', 0, '让对手听可爱的叫声，
引开注意力使其疏忽，
从而降低对手的攻击。', 0, 2, 1, 11, 1),
       (46, null, 0, 0, 0, 'Switches the target out for another of its trainer’s Pokémon selected at random.  Wild battles end immediately.

Doesn’t affect Pokémon with suction cups or under the effect of ingrain.', null, 0, 0, 'roar', null, null, null, null,
        '吼叫', null, 20, -6, 'Immediately ends wild battles.  Forces trainers to switch Pokémon.', 0, '放走对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', 0, 12, 1, 10, 1),
       (47, 55, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'sing', null, 4, null, 2, '唱歌', null, 15, 0,
        'Puts the target to sleep.', 0, '让对手听舒适、
美妙的歌声，
从而陷入睡眠状态。', 2, 1, 1, 10, 1),
       (48, 55, 0, 0, 0, 'Confuses the target.', null, 0, 0, 'supersonic', null, 5, null, 2, '超音波', null, 20, 0,
        'Confuses the target.', 0, '从身体发出
特殊的音波，
从而使对手混乱。', 6, 1, 1, 10, 1),
       (49, 90, 0, 0, 0, 'Inflicts exactly 20 damage.', null, 0, 0, 'sonic-boom', null, null, null, null, '音爆', null,
        20, 0, 'Inflicts 20 points of damage.', 0, '将冲击波
撞向对手进行攻击。
必定会给予２０的伤害。', 0, 0, 3, 10, 1),
       (50, 100, 0, 0, 0,
        'Disables the target’s last used move, preventing its use for 4–7 turns, selected at random, or until the target leaves the field.  If the target hasn’t used a move since entering the field, if it tried to use a move this turn and failed,  if its last used move has 0 PP remaining, or if it already has a move disabled, this move will fail.',
        null, 0, 0, 'disable', null, 4, null, 4, '定身法', null, 20, 0,
        'Disables the target’s last used move for 1-8 turns.', 0, '阻碍对手行动，
之前使出的招式
将在４回合内无法使用。', 13, 13, 1, 10, 1),
       (51, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'acid', null, null, null, null, '溶解液', 40, 30, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '将强酸泼向对手进行攻击。
有时会降低对手的特防。', 0, 6, 3, 11, 4),
       (52, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'ember', null, null, null, null, '火花', 40, 25, 0, 'Has a $effect_chance% chance to burn the target.', 0, '向对手发射
小型火焰进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 10),
       (53, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'flamethrower', null, null, null, null, '喷射火焰', 90, 15, 0,
        'Has a $effect_chance% chance to burn the target.', 0, '向对手发射
烈焰进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 10),
       (54, null, 0, 0, 0, 'Pokémon on the user’s side of the field are immune to stat-lowering effects for five turns.

guard swap, heart swap, and power swap may still be used.

defog used by an opponent will end this effect.', null, 0, 0, 'mist', null, null, null, null, '白雾', null, 30, 0,
        'Protects the user’s stats from being changed by enemy moves.', 0, '用白雾覆盖身体。
在５回合内不会让对手
降低自己的能力。', 0, 11, 1, 4, 15),
       (55, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'water-gun', null, null, null, null, '水枪', 40, 25,
        0, 'Inflicts regular damage with no additional effect.', 0, '向对手猛烈地喷射
水流进行攻击。', 0, 0, 3, 10, 11),
       (56, 80, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'hydro-pump', null, null, null, null, '水炮', 110, 5,
        0, 'Inflicts regular damage with no additional effect.', 0, '向对手猛烈地喷射
大量水流进行攻击。', 0, 0, 3, 10, 11),
       (57, 100, 0, 0, 0, 'Inflicts regular damage.

If the target is in the first turn of dive, this move will hit with double power.', null, 0, 0, 'surf', null, null,
        null, null, '冲浪', 90, 15, 0, 'Inflicts regular damage and can hit Dive users.', 0, '利用大浪
攻击自己周围所有的宝可梦。', 0, 0, 3, 9, 11),
       (58, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target.', 10, 0, 0,
        'ice-beam', null, null, null, null, '冰冻光束', 90, 10, 0, 'Has a $effect_chance% chance to freeze the target.',
        0, '向对手发射
冰冻光束进行攻击。
有时会让对手陷入冰冻状态。', 3, 4, 3, 10, 15),
       (59, 70, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target.

During hail, this move has 100% accuracy.  It also has a (100 - accuracy)% chance to break through the protection of protect and detect.',
        10, 0, 0, 'blizzard', null, null, null, null, '暴风雪', 110, 5, 0,
        'Has a $effect_chance% chance to freeze the target.', 0, '将猛烈的暴风雪
刮向对手进行攻击。
有时会让对手陷入冰冻状态。', 3, 4, 3, 11, 15),
       (60, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 10, 0, 0,
        'psybeam', null, 5, null, 2, '幻象光线', 65, 20, 0, 'Has a $effect_chance% chance to confuse the target.', 0, '向对手发射
神奇的光线进行攻击。
有时会使对手混乱。', 6, 4, 3, 10, 14),
       (61, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, 0, 0,
        'bubble-beam', null, null, null, null, '泡沫光线', 65, 20, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, '向对手猛烈地喷射
泡沫进行攻击。
有时会降低对手的速度。', 0, 6, 3, 10, 11),
       (62, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Attack by one stage.', 10, 0, 0,
        'aurora-beam', null, null, null, null, '极光束', 65, 20, 0,
        'Has a $effect_chance% chance to lower the target’s Attack by one stage.', 10, '向对手发射
虹色光束进行攻击。
有时会降低对手的攻击。', 0, 6, 3, 10, 15),
       (63, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'hyper-beam', null, null, null, null, '破坏光线', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '向对手发射
强烈的光线进行攻击。
下一回合自己将无法动弹。', 0, 0, 3, 10, 1),
       (64, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'peck', null, null, null, null, '啄', 35, 35, 0,
        'Inflicts regular damage with no additional effect.', 0, '用尖锐的喙或角
刺向对手进行攻击。', 0, 0, 2, 10, 3),
       (65, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'drill-peck', null, null, null, null, '啄钻', 80, 20,
        0, 'Inflicts regular damage with no additional effect.', 0, '一边旋转，一边将尖喙
刺入对手进行攻击。', 0, 0, 2, 10, 3),
       (66, 80, 0, 0, -25, 'Inflicts regular damage.  User takes 1/4 the damage it inflicts in recoil.', null, 0, 0,
        'submission', null, null, null, null, '地狱翻滚', 80, 20, 0,
        'User receives 1/4 the damage it inflicts in recoil.', 0, '将对手连同自己一起
摔向地面进行攻击。
自己也会受到少许伤害。', 0, 0, 2, 10, 2),
       (67, 100, 0, 0, 0, 'Inflicts regular damage.  Power increases with the target’s weight in kilograms, to a maximum of 120.

Target’s weight | Power
--------------- | ----:
Up to 10kg      |    20
Up to 25kg      |    40
Up to 50kg      |    60
Up to 100kg     |    80
Up to 200kg     |   100
Above 200kg     |   120
', null, 0, 0, 'low-kick', null, null, null, null, '踢倒', null, 20, 0,
        'Inflicts more damage to heavier targets, with a maximum of 120 power.', 0, '用力踢对手的脚，
使其摔倒进行攻击。
对手越重，威力越大。', 0, 0, 2, 10, 2),
       (68, 100, 0, 0, 0, 'Targets the last opposing Pokémon to hit the user with a physical move this turn.  Inflicts twice the damage that move did to the user.  If there is no eligible target, this move will fail.  Type immunity applies, but other type effects are ignored.

This move cannot be copied by mirror move, nor selected by assist or metronome.', null, 0, 0, 'counter', null, null,
        null, null, '双倍奉还', null, 20, -5,
        'Inflicts twice the damage the user received from the last physical hit it took.', 0, '从对手那里受到
物理攻击的伤害将以
２倍返还给同一个对手。', 0, 0, 2, 1, 2),
       (69, 100, 0, 0, 0,
        'Inflicts damage equal to the user’s level.  Type immunity applies, but other type effects are ignored.', null,
        0, 0, 'seismic-toss', null, null, null, null, '地球上投', null, 20, 0,
        'Inflicts damage equal to the user’s level.', 0, '利用引力将对手甩飞出去。
给予对手和自己等级相同的伤害。', 0, 0, 2, 10, 2),
       (70, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'strength', null, null, null, null, '怪力', 80, 15, 0,
        'Inflicts regular damage with no additional effect.', 0, '使出浑身力气
殴打对手进行攻击。', 0, 0, 2, 10, 1),
       (71, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'absorb', null, null, null, null, '吸取', 20, 25, 0, 'Drains half the damage inflicted to heal the user.', 0, '吸取对手的养分进行攻击。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 3, 10, 12),
       (72, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'mega-drain', null, null, null, null, '超级吸取', 40, 15, 0,
        'Drains half the damage inflicted to heal the user.', 0, '吸取对手的养分进行攻击。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 3, 10, 12),
       (73, 90, 0, 0, 0, 'Plants a seed on the target that drains 1/8 of its max HP at the end of every turn and heals the user for the amount taken.  Has no effect on grass Pokémon.  The seed remains until the target leaves the field.

The user takes damage instead of being healed if the target has liquid ooze.

rapid spin will remove this effect.

This effect is passed on by baton pass.', null, 0, 0, 'leech-seed', null, null, null, null, '寄生种子', null, 10, 0,
        'Seeds the target, stealing HP from it every turn.', 0, '植入寄生种子后，将在每回合
一点一点吸取对手的ＨＰ，
从而用来回复自己的ＨＰ。', 18, 1, 1, 10, 12),
       (74, null, 0, 0, 0,
        'Raises the user’s Attack and Special Attack by one stage each.  During sunny day, raises both stats by two stages.',
        null, 0, 0, 'growth', null, null, null, null, '生长', null, 20, 0,
        'Raises the user’s Attack and Special Attack by one stage.', 0, '让身体一下子长大，
从而提高攻击和特攻。', 0, 2, 1, 7, 1),
       (75, 95, 0, 1, 0, 'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.',
        null, 0, 0, 'razor-leaf', null, null, null, null, '飞叶快刀', 55, 25, 0,
        'Has an increased chance for a critical hit.', 0, '飞出叶片，
切斩对手进行攻击。
容易击中要害。', 0, 0, 2, 11, 12),
       (76, 100, 0, 0, 0, 'Inflicts regular damage.  User charges for one turn before attacking.

During sunny day, the charge turn is skipped.

During hail, rain dance, or sandstorm, power is halved.

This move cannot be selected by sleep talk.', null, 0, 0, 'solar-beam', null, null, null, null, '日光束', 120, 10, 0,
        'Requires a turn to charge before attacking.', 0, '第１回合收集满满的日光，
第２回合发射光束进行攻击。', 0, 0, 3, 10, 12),
       (77, 75, 0, 0, 0, 'Poisons the target.', null, 0, 0, 'poison-powder', null, null, null, null, '毒粉', null, 35,
        0, 'Poisons the target.', 0, '撒出毒粉，
从而让对手陷入中毒状态。', 5, 1, 1, 10, 4),
       (78, 75, 0, 0, 0, 'Paralyzes the target.', null, 0, 0, 'stun-spore', null, null, null, null, '麻痹粉', null, 30,
        0, 'Paralyzes the target.', 0, '撒出麻痹粉，
从而让对手陷入麻痹状态。', 1, 1, 1, 10, 12),
       (79, 75, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'sleep-powder', null, 4, null, 2, '催眠粉', null, 15,
        0, 'Puts the target to sleep.', 0, '撒出催眠粉，
从而让对手陷入睡眠状态。', 2, 1, 1, 10, 12),
       (80, 100, 0, 0, 0, 'Inflicts regular damage.  User is forced to attack with this move for 2–3 turns,selected at random.  After the last hit, the user becomes confused.

safeguard does not protect against the confusion from this move.', null, 0, 0, 'petal-dance', null, null, null, null,
        '花瓣舞', 120, 10, 0, 'Hits every turn for 2-3 turns, then confuses the user.', 0, '在２～３回合内，
散落花瓣攻击对手。
之后自己会陷入混乱。', 0, 0, 3, 8, 12),
       (81, 95, 0, 0, 0, 'Lowers the target’s Speed by two stages.', null, 0, 0, 'string-shot', null, null, null, null,
        '吐丝', null, 40, 0, 'Lowers the target’s Speed by two stages.', 0, '用口中吐出的丝缠绕对手，
从而大幅降低对手的速度。', 0, 2, 1, 11, 7),
       (82, 100, 0, 0, 0, 'Inflicts exactly 40 damage.', null, 0, 0, 'dragon-rage', null, null, null, null, '龙之怒',
        null, 10, 0, 'Inflicts 40 points of damage.', 0, '将愤怒的冲击波
撞向对手进行攻击。
必定会给予４０的伤害。', 0, 0, 3, 10, 16),
       (83, 85, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'fire-spin', null, 6, null, 5, '火焰旋涡', 35, 15, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '将对手困在
激烈的火焰旋涡中，
在４～５回合内进行攻击。', 8, 4, 3, 10, 10),
       (84, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 10, 0, 0,
        'thunder-shock', null, null, null, null, '电击', 40, 30, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '发出电流刺激对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (85, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 10, 0, 0,
        'thunderbolt', null, null, null, null, '十万伏特', 90, 15, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '向对手发出
强力电击进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (86, 90, 0, 0, 0, 'Paralyzes the target.', null, 0, 0, 'thunder-wave', null, null, null, null, '电磁波', null,
        20, 0, 'Paralyzes the target.', 0, '向对手发出
微弱的电击，
从而让对手陷入麻痹状态。', 1, 1, 1, 10, 13),
       (87, 70, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.

During rain dance, this move has 100% accuracy.  It also has a (100 - accuracy)% chance to break through the protection of protect and detect.

During sunny day, this move has 50% accuracy.', 30, 0, 0, 'thunder', null, null, null, null, '打雷', 110, 10, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '向对手劈下暴雷进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (88, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'rock-throw', null, null, null, null, '落石', 50, 15,
        0, 'Inflicts regular damage with no additional effect.', 0, '拿起小岩石，
投掷对手进行攻击。', 0, 0, 2, 10, 6),
       (89, 100, 0, 0, 0, 'Inflicts regular damage.

If the target is in the first turn of dig, this move will hit with double power.', null, 0, 0, 'earthquake', null, null,
        null, null, '地震', 100, 10, 0, 'Inflicts regular damage and can hit Dig users.', 0, '利用地震的冲击，
攻击自己周围所有的宝可梦。', 0, 0, 2, 9, 5),
       (90, 30, 0, 0, 0, 'Inflicts damage equal to the target’s max HP.  Ignores accuracy and evasion modifiers.  This move’s accuracy is 30% plus 1% for each level the user is higher than the target.  If the user is a lower level than the target, this move will fail.

Because this move inflicts a specific and finite amount of damage, endure still prevents the target from fainting.

The effects of lock on, mind reader, and no guard still apply, as long as the user is equal or higher level than the target.  However, they will not give this move a chance to break through detect or protect.',
        null, 0, 0, 'fissure', null, null, null, null, '地裂', null, 5, 0, 'Causes a one-hit KO.', 0, '让对手掉落于地裂的
裂缝中进行攻击。
只要命中就会一击濒死。', 0, 9, 2, 10, 5),
       (91, 100, 0, 0, 0, 'Inflicts regular damage.  User digs underground for one turn, becoming immune to attack, and hits on the second turn.

During the immune turn, earthquake, fissure, and magnitude still hit the user normally, and their power is doubled if appropriate.

The user may be hit during its immune turn if under the effect of lock on, mind reader, or no guard.

This move cannot be selected by sleep talk.', null, 0, 0, 'dig', null, null, null, null, '挖洞', 80, 10, 0,
        'User digs underground, dodging all attacks, and hits next turn.', 0, '第１回合钻入，
第２回合攻击对手。', 0, 0, 2, 10, 5),
       (92, 90, 0, 0, 0, 'Badly poisons the target.  Never misses when used by a poison-type Pokémon.', null, 0, 0,
        'toxic', null, 15, null, 15, '剧毒', null, 10, 0,
        'Badly poisons the target, inflicting more damage every turn.', 0, '让对手陷入剧毒状态。
随着回合的推进，
中毒伤害会增加。', 5, 1, 1, 10, 4),
       (93, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 10, 0, 0,
        'confusion', null, 5, null, 2, '念力', 50, 25, 0, 'Has a $effect_chance% chance to confuse the target.', 0, '向对手发送
微弱的念力进行攻击。
有时会使对手混乱。', 6, 4, 3, 10, 14),
       (94, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'psychic', null, null, null, null, '精神强念', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '向对手发送
强大的念力进行攻击。
有时会降低对手的特防。', 0, 6, 3, 10, 14),
       (95, 60, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'hypnosis', null, 4, null, 2, '催眠术', null, 20, 0,
        'Puts the target to sleep.', 0, '施以诱导睡意的暗示，
让对手陷入睡眠状态。', 2, 1, 1, 10, 14),
       (96, null, 0, 0, 0, 'Raises the user’s Attack by one stage.', null, 0, 0, 'meditate', null, null, null, null,
        '瑜伽姿势', null, 40, 0, 'Raises the user’s Attack by one stage.', 0, '唤醒身体深处
沉睡的力量，
从而提高自己的攻击。', 0, 2, 1, 7, 14),
       (97, null, 0, 0, 0, 'Raises the user’s Speed by two stages.', null, 0, 0, 'agility', null, null, null, null,
        '高速移动', null, 30, 0, 'Raises the user’s Speed by two stages.', 0, '让身体放松变得轻盈，
以便高速移动。
大幅提高自己的速度。', 0, 2, 1, 7, 14),
       (98, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'quick-attack', null, null, null, null, '电光一闪',
        40, 30, 1, 'Inflicts regular damage with no additional effect.', 0, '以迅雷不及掩耳之势扑向对手。
必定能够先制攻击。', 0, 0, 2, 10, 1),
       (99, 100, 0, 0, 0,
        'Inflicts regular damage.  Every time the user is hit after it uses this move but before its next action, its Attack raises by one stage.',
        null, 0, 0, 'rage', null, null, null, null, '愤怒', 20, 20, 0,
        'If the user is hit after using this move, its Attack rises by one stage.', 0, '如果在使出招式后
受到攻击的话，
会因愤怒的力量而提高攻击。', 0, 0, 2, 10, 1),
       (100, null, 0, 0, 0, 'Does nothing.  Wild battles end immediately.', null, 0, 0, 'teleport', null, null, null,
        null, '瞬间移动', null, 20, -6, 'Immediately ends wild battles.  No effect otherwise.', 0,
        '停止和野生宝可梦战斗并逃走。', 0, 13, 1, 7, 14),
       (101, 100, 0, 0, 0,
        'Inflicts damage equal to the user’s level.  Type immunity applies, but other type effects are ignored.', null,
        0, 0, 'night-shade', null, null, null, null, '黑夜魔影', null, 15, 0,
        'Inflicts damage equal to the user’s level.', 0, '显示恐怖幻影，
只给予对手
和自己等级相同的伤害。', 0, 0, 3, 10, 8),
       (102, null, 0, 0, 0, 'This move is replaced by the target’s last successfully used move, and its PP changes to 5.  If the target hasn’t used a move since entering the field, if it tried to use a move this turn and failed, or if the user already knows the targeted move, this move will fail.  This effect vanishes when the user leaves the field.

If chatter, metronome, mimic, sketch, or struggle is selected, this move will fail.

This move cannot be copied by mirror move, nor selected by assist or metronome, nor forced by encore.', null, 0, 0,
        'mimic', null, null, null, null, '模仿', null, 10, 0, 'Copies the target’s last used move.', 0, '可以将对手
最后使用的招式，
在战斗内变成自己的招式。', 0, 13, 1, 10, 1),
       (103, 85, 0, 0, 0, 'Lowers the target’s Defense by two stages.', null, 0, 0, 'screech', null, null, null, null,
        '刺耳声', null, 40, 0, 'Lowers the target’s Defense by two stages.', 0, '发出不由自主想要
捂起耳朵的刺耳声，
从而大幅降低对手的防御。', 0, 2, 1, 10, 1),
       (104, null, 0, 0, 0, 'Raises the user’s evasion by one stage.', null, 0, 0, 'double-team', null, null, null,
        null, '影子分身', null, 15, 0, 'Raises the user’s evasion by one stage.', 0, '通过快速移动来制造分身，
扰乱对手，从而提高闪避率。', 0, 2, 1, 7, 1),
       (105, null, 0, 0, 0, 'Heals the user for half its max HP.', null, 0, 50, 'recover', null, null, null, null,
        '自我再生', null, 5, 0, 'Heals the user by half its max HP.', 0, '让细胞再生，
从而回复自己
最大ＨＰ的一半。', 0, 3, 1, 7, 1),
       (106, null, 0, 0, 0, 'Raises the user’s Defense by one stage.', null, 0, 0, 'harden', null, null, null, null,
        '变硬', null, 30, 0, 'Raises the user’s Defense by one stage.', 0, '全身使劲，让身体变硬，
从而提高自己的防御。', 0, 2, 1, 7, 1),
       (107, null, 0, 0, 0, 'Raises the user’s evasion by two stages.

stomp and steamroller have double power against Pokémon that have used this move since entering the field.', null, 0, 0,
        'minimize', null, null, null, null, '变小', null, 10, 0, 'Raises the user’s evasion by two stages.', 0, '蜷缩身体显得很小，
从而大幅提高
自己的闪避率。', 0, 2, 1, 7, 1),
       (108, 100, 0, 0, 0, 'Lowers the target’s accuracy by one stage.', null, 0, 0, 'smokescreen', null, null, null,
        null, '烟幕', null, 20, 0, 'Lowers the target’s accuracy by one stage.', 0, '向对手喷出烟或墨汁等，
从而降低对手的命中率。', 0, 2, 1, 10, 1),
       (109, 100, 0, 0, 0, 'Confuses the target.', null, 0, 0, 'confuse-ray', null, 5, null, 2, '奇异之光', null, 10, 0,
        'Confuses the target.', 0, '显示奇怪的光，
扰乱对手。
使对手混乱。', 6, 1, 1, 10, 8),
       (110, null, 0, 0, 0, 'Raises the user’s Defense by one stage.', null, 0, 0, 'withdraw', null, null, null, null,
        '缩入壳中', null, 40, 0, 'Raises the user’s Defense by one stage.', 0, '缩入壳里保护身体，
从而提高自己的防御。', 0, 2, 1, 7, 11),
       (111, null, 0, 0, 0, 'Raises user’s Defense by one stage.

After this move is used, the power of ice ball and rollout are doubled until the user leaves the field.', null, 0, 0,
        'defense-curl', null, null, null, null, '变圆', null, 40, 0, 'Raises user’s Defense by one stage.', 0, '将身体蜷曲变圆，
从而提高自己的防御。', 0, 2, 1, 7, 1),
       (112, null, 0, 0, 0, 'Raises the user’s Defense by two stages.', null, 0, 0, 'barrier', null, null, null, null,
        '屏障', null, 20, 0, 'Raises the user’s Defense by two stages.', 0, '制造坚固的壁障，
从而大幅提高自己的防御。', 0, 2, 1, 7, 14),
       (113, null, 0, 0, 0, 'Erects a barrier around the user’s side of the field that reduces damage from special attacks by half for five turns.  In double battles, the reduction is 1/3.  Critical hits are not affected by the barrier.

If the user is holding light clay, the barrier lasts for eight turns.

brick break or defog used by an opponent will destroy the barrier.', null, 0, 0, 'light-screen', null, null, null, null,
        '光墙', null, 30, 0, 'Reduces damage from special attacks by 50% for five turns.', 0, '在５回合内使用神奇的墙，
减弱从对手那受到的
特殊攻击的伤害。', 0, 11, 1, 4, 14),
       (114, null, 0, 0, 0, 'Removes stat, accuracy, and evasion modifiers from every Pokémon on the field.

This does not count as a stat reduction for the purposes of clear body or white smoke.', null, 0, 0, 'haze', null, null,
        null, null, '黑雾', null, 30, 0, 'Resets all Pokémon’s stats, accuracy, and evasion.', 0, '升起黑雾，将正在场上战斗的
全体宝可梦的能力变回原点。', 0, 10, 1, 12, 15),
       (115, null, 0, 0, 0, 'Erects a barrier around the user’s side of the field that reduces damage from physical attacks by half for five turns.  In double battles, the reduction is 1/3.  Critical hits are not affected by the barrier.

If the user is holding light clay, the barrier lasts for eight turns.

brick break or defog used by an opponent will destroy the barrier.', null, 0, 0, 'reflect', null, null, null, null,
        '反射壁', null, 20, 0, 'Reduces damage from physical attacks by half.', 0, '在５回合内使用神奇的墙，
减弱从对手那受到的
物理攻击的伤害。', 0, 11, 1, 4, 14),
       (116, null, 0, 0, 0, 'User’s critical hit rate is two levels higher until it leaves the field.  If the user has already used focus energy since entering the field, this move will fail.

This effect is passed on by baton pass.', null, 0, 0, 'focus-energy', null, null, null, null, '聚气', null, 30, 0,
        'Increases the user’s chance to score a critical hit.', 0, '深深地吸口气，集中精神。
自己的攻击
会变得容易击中要害。', 0, 13, 1, 7, 1),
       (117, null, 0, 0, 0, 'User waits for two turns.  On the second turn, the user inflicts twice the damage it accumulated on the last Pokémon to hit it.  Damage inflicted is typeless.

This move cannot be selected by sleep talk.', null, 0, 0, 'bide', null, null, null, null, '忍耐', null, 10, 1,
        'User waits for two turns, then hits back for twice the damage it took.', 0, '在２回合内忍受攻击，
受到的伤害会
２倍返还给对手。', 0, 0, 2, 7, 1),
       (118, null, 0, 0, 0, 'Selects any move at random and uses it.  Moves the user already knows are not eligible.  Assist, meta, protection, and reflection moves are also not eligible; specifically, assist, chatter, copycat, counter, covet, destiny bond, detect, endure, feint, focus punch, follow me, helping hand, me first, metronome, mimic, mirror coat, mirror move, protect, quick guard, sketch, sleep talk, snatch, struggle, switcheroo, thief, trick, and wide guard will not be selected by this move.

This move cannot be copied by mimic or mirror move, nor selected by assist, metronome, or sleep talk.', null, 0, 0,
        'metronome', null, null, null, null, '挥指', null, 10, 0, 'Randomly selects and uses any move in the game.', 0, '挥动手指刺激自己的大脑，
从所有的招式中
任意使出１个。', 0, 13, 1, 7, 1),
       (119, null, 0, 0, 0, 'Uses the last move targeted at the user by a Pokémon still on the field.  A move counts as targeting the user even if it hit multiple Pokémon, as long as the user was one of them; however, moves targeting the field itself do not count.  If the user has not been targeted by an appropriate move since entering the field, or if no Pokémon that targeted the user remains on the field, this move will fail.

Moves that failed, missed, had no effect, or were blocked are still copied.

Assist moves, time-delayed moves, “meta” moves that operate on other moves/Pokémon/abilities, and some other special moves cannot be copied and are ignored; if the last move to hit the user was such a move, the previous move will be used instead.  The full list of ignored moves is: acid armor, acupressure, after you, agility, ally switch, amnesia, aqua ring, aromatherapy, aromatic mist, assist, autotomize, barrier, baton pass, belch, belly drum, bide, bulk up, calm mind, camouflage, celebrate, charge, coil, conversion, conversion 2, copycat, cosmic power, cotton guard, counter, crafty shield, curse, defend order, defense curl, destiny bond, detect, doom desire, double team, dragon dance, electric terrain, endure, final gambit, flower shield, focus energy, focus punch, follow me, future sight, geomancy, grassy terrain, gravity, growth, grudge, guard split, hail, happy hour, harden, haze, heal bell, heal order, heal pulse, healing wish, helping hand, hold hands, hone claws, howl, imprison, ingrain, ion deluge, iron defense, kings shield, light screen, lucky chant, lunar dance, magic coat, magnet rise, magnetic flux, mat block, me first, meditate, metronome, milk drink, mimic, minimize, mirror coat, mirror move, mist, misty terrain, moonlight, morning sun, mud sport, nasty plot, nature power, perish song, power split, power trick, protect, psych up, quick guard, quiver dance, rage powder, rain dance, recover, recycle, reflect, reflect type, refresh, rest, rock polish, role play, roost, rototiller, safeguard, sandstorm, shadow blast, shadow bolt, shadow half, shadow rush, shadow shed, shadow sky, shadow storm, shadow wave, sharpen, shell smash, shift gear, sketch, slack off, sleep talk, snatch, soft boiled, spikes, spiky shield, spit up, splash, stealth rock, sticky web, stockpile, struggle, substitute, sunny day, swallow, swords dance, synthesis, tail glow, tailwind, teleport, toxic spikes, transform, water sport, wide guard, wish, withdraw and work up.

This move cannot be selected by assist, metronome, or sleep talk, nor forced by encore.', null, 0, 0, 'mirror-move',
        null, null, null, null, '鹦鹉学舌', null, 20, 0, 'Uses the target’s last used move.', 0, '模仿对手使用的招式，
自己也使用相同招式。', 0, 13, 1, 10, 3),
       (120, 100, 0, 0, 0, 'User faints, even if the attack fails or misses.  Inflicts regular damage.', null, 0, 0,
        'self-destruct', null, null, null, null, '自爆', 200, 5, 0, 'User faints.', 0, '引发爆炸，
攻击自己周围所有的宝可梦。
使用后陷入濒死。', 0, 0, 2, 9, 1),
       (121, 75, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'egg-bomb', null, null, null, null, '炸蛋', 100, 10,
        0, 'Inflicts regular damage with no additional effect.', 0, '向对手用力投掷
大大的蛋进行攻击。', 0, 0, 2, 10, 1),
       (122, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'lick', null, null, null, null, '舌舔', 30, 30, 0, 'Has a $effect_chance% chance to paralyze the target.', 0, '用长长的舌头，
舔遍对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 8),
       (123, 70, 40, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 40, 0, 0,
        'smog', null, null, null, null, '浊雾', 30, 20, 0, 'Has a $effect_chance% chance to poison the target.', 0, '将肮脏的浓雾
吹向对手进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 3, 10, 4),
       (124, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 30, 0, 0,
        'sludge', null, null, null, null, '污泥攻击', 65, 20, 0, 'Has a $effect_chance% chance to poison the target.',
        0, '用污泥投掷对手进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 3, 10, 4),
       (125, 85, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 10, 10,
        0, 'bone-club', null, null, null, null, '骨棒', 65, 20, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用手中的骨头
殴打对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 5),
       (126, 85, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'fire-blast', null, null, null, null, '大字爆炎', 110, 5, 0, 'Has a $effect_chance% chance to burn the target.',
        0, '用大字形状的火焰烧尽对手。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 10),
       (127, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 20, 20,
        0, 'waterfall', null, null, null, null, '攀瀑', 80, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '以惊人的气势扑向对手。
有时会使对手畏缩。', 0, 0, 2, 10, 11),
       (128, 85, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'clamp', null, 6, null, 5, '贝壳夹击', 35, 15, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '用非常坚固且厚实的贝壳，
在４～５回合内
夹住对手进行攻击。', 8, 4, 2, 10, 11),
       (129, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0, 'swift',
        null, null, null, null, '高速星星', 60, 20, 0, 'Never misses.', 0, '发射星形的光攻击对手。
攻击必定会命中。', 0, 0, 3, 11, 1),
       (130, 100, 100, 0, 0, 'Inflicts regular damage.  Raises the user’s Defense by one stage.  User then charges for one turn before attacking.

This move cannot be selected by sleep talk.', 100, 0, 0, 'skull-bash', null, null, null, null, '火箭头锤', 130, 10, 0,
        'Raises the user’s Defense by one stage.  User charges for one turn before attacking.', 0, '第１回合把头缩进去，
从而提高防御。
第２回合攻击对手。', 0, 0, 2, 10, 1),
       (131, 100, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'spike-cannon', 5, null, 2, null, '尖刺加农炮', 20, 15, 0, 'Hits 2-5 times in one turn.', 0, '向对手发射
锐针进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (132, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, 0, 0,
        'constrict', null, null, null, null, '缠绕', 10, 35, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, '用触手或青藤等缠绕进行攻击。
有时会降低对手的速度。', 0, 6, 2, 10, 1),
       (133, null, 0, 0, 0, 'Raises the user’s Special Defense by two stages.', null, 0, 0, 'amnesia', null, null, null,
        null, '瞬间失忆', null, 20, 0, 'Raises the user’s Special Defense by two stages.', 0, '将头脑清空，
瞬间忘记某事，
从而大幅提高自己的特防。', 0, 2, 1, 7, 14),
       (134, 80, 0, 0, 0, 'Lowers the target’s accuracy by one stage.', null, 0, 0, 'kinesis', null, null, null, null,
        '折弯汤匙', null, 15, 0, 'Lowers the target’s accuracy by one stage.', 0, '折弯汤匙引开注意，
从而降低对手的命中率。', 0, 2, 1, 10, 14),
       (135, null, 0, 0, 0, 'Heals the user for half its max HP.', null, 0, 50, 'soft-boiled', null, null, null, null,
        '生蛋', null, 5, 0, 'Heals the user by half its max HP.', 0, '回复自己最大ＨＰ的一半。', 0, 3, 1, 7, 1),
       (136, 90, 0, 0, 0, 'Inflicts regular damage. If this move misses, is blocked by protect or detect, or has no effect, the user takes damage equal to half of its max HP rounded down.

This move cannot be used while gravity is in effect.', null, 0, 0, 'high-jump-kick', null, null, null, null, '飞膝踢',
        130, 10, 0, 'If the user misses, it takes half the damage it would have inflicted in recoil.', 0, '跳起后用膝盖撞对手进行攻击。
如果撞偏则自己会受到伤害。', 0, 0, 2, 10, 2),
       (137, 100, 0, 0, 0, 'Paralyzes the target.', null, 0, 0, 'glare', null, null, null, null, '大蛇瞪眼', null, 30,
        0, 'Paralyzes the target.', 0, '用腹部的花纹使对手害怕，
从而让其陷入麻痹状态。', 1, 1, 1, 10, 1),
       (138, 100, 0, 0, 50,
        'Fails if not used on a sleeping Pokémon.  Inflicts regular damage.  Drains half the damage inflicted to heal the user.',
        null, 0, 0, 'dream-eater', null, null, null, null, '食梦', 100, 15, 0,
        'Only works on sleeping Pokémon.  Drains half the damage inflicted to heal the user.', 0, '吃掉正在睡觉的对手的梦
进行攻击。回复对手
所受到伤害的一半ＨＰ。', 0, 8, 3, 10, 14),
       (139, 90, 0, 0, 0, 'Poisons the target.', null, 0, 0, 'poison-gas', null, null, null, null, '毒瓦斯', null, 40,
        0, 'Poisons the target.', 0, '将毒瓦斯吹到对手的脸上，
从而让对手陷入中毒状态。', 5, 1, 1, 11, 4),
       (140, 85, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'barrage', 5, null, 2, null, '投球', 15, 20, 0, 'Hits 2-5 times in one turn.', 0, '向对手投掷
圆形物体进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (141, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'leech-life', null, null, null, null, '吸血', 80, 10, 0, 'Drains half the damage inflicted to heal the user.',
        0, '吸取血液攻击对手。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 2, 10, 7),
       (142, 75, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'lovely-kiss', null, 4, null, 2, '恶魔之吻', null,
        10, 0, 'Puts the target to sleep.', 0, '用恐怖的脸强吻对手。
让对手陷入睡眠状态。', 2, 1, 1, 10, 1),
       (143, 90, 0, 1, 0, 'Inflicts regular damage.  User charges for one turn before attacking.  Critical hit chance is one level higher than normal.  Has a $effect_chance% chance to make the target flinch.

This move cannot be selected by sleep talk.', 30, 30, 0, 'sky-attack', null, null, null, null, '神鸟猛击', 140, 5, 0,
        'User charges for one turn before attacking.  Has a $effect_chance% chance to make the target flinch.', 0, '第２回合攻击对手。
偶尔使对手畏缩。
也容易击中要害。', 0, 0, 2, 10, 3),
       (144, null, 0, 0, 0, 'User copies the target’s species, weight, type, ability, calculated stats (except HP), and moves.  Copied moves will all have 5 PP remaining.  IVs are copied for the purposes of hidden power, but stats are not recalculated.

choice band, choice scarf, and choice specs stay in effect, and the user must select a new move.

This move cannot be copied by mirror move, nor forced by encore.', null, 0, 0, 'transform', null, null, null, null,
        '变身', null, 10, 0, 'User becomes a copy of the target until it leaves battle.', 0, '变身成对手宝可梦的样子，
能够使用和对手
完全相同的招式。', 0, 13, 1, 10, 1),
       (145, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, 0, 0,
        'bubble', null, null, null, null, '泡沫', 40, 30, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 10, '向对手用力吹起无数泡泡进行攻击。
有时会降低对手的速度。', 0, 6, 3, 11, 11),
       (146, 100, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 20, 0, 0,
        'dizzy-punch', null, 5, null, 2, '迷昏拳', 70, 10, 0, 'Has a $effect_chance% chance to confuse the target.', 0, '有节奏地出拳攻击对手。
有时会使对手混乱。', 6, 4, 2, 10, 1),
       (147, 100, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'spore', null, 4, null, 2, '蘑菇孢子', null, 15, 0,
        'Puts the target to sleep.', 0, '沙沙沙地撒满具有
催眠效果的孢子，
从而让对手陷入睡眠状态。', 2, 1, 1, 10, 12),
       (148, 100, 0, 0, 0, 'Lowers the target’s accuracy by one stage.', null, 0, 0, 'flash', null, null, null, null,
        '闪光', null, 20, 0, 'Lowers the target’s accuracy by one stage.', 0, '使出耀眼光芒，
从而降低对手的命中率。', 0, 2, 1, 10, 1),
       (149, 100, 0, 0, 0,
        'Inflicts typeless damage between 50% and 150% of the user’s level, selected at random in increments of 10%.',
        null, 0, 0, 'psywave', null, null, null, null, '精神波', null, 15, 0,
        'Inflicts damage between 50% and 150% of the user’s level.', 0, '向对手发射
神奇的念波进行攻击。
每次使用，伤害都会改变。', 0, 0, 3, 10, 14),
       (150, null, 0, 0, 0, 'Does nothing.

This move cannot be used while gravity is in effect.', null, 0, 0, 'splash', null, null, null, null, '跃起', null, 40,
        0, 'Does nothing.', 0, '也不攻击只是一蹦一蹦地跳，
什么都不会发生……', 0, 13, 1, 7, 1),
       (151, null, 0, 0, 0, 'Raises the user’s Defense by two stages.', null, 0, 0, 'acid-armor', null, null, null,
        null, '溶化', null, 20, 0, 'Raises the user’s Defense by two stages.', 0, '通过细胞的变化进行液化，
从而大幅提高自己的防御。', 0, 2, 1, 7, 4),
       (152, 90, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'crabhammer', null, null, null, null, '蟹钳锤', 100, 10, 0, 'Has an increased chance for a critical hit.', 0, '用大钳子
敲打对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 11),
       (153, 100, 0, 0, 0, 'User faints, even if the attack fails or misses.  Inflicts regular damage.', null, 0, 0,
        'explosion', null, null, null, null, '大爆炸', 250, 5, 0, 'User faints.', 0, '引发大爆炸，
攻击自己周围所有的宝可梦。
使用后自己会陷入濒死。', 0, 0, 2, 9, 1),
       (154, 80, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'fury-swipes', 5, null, 2, null, '乱抓', 18, 15, 0, 'Hits 2-5 times in one turn.', 0, '用爪子或镰刀等
抓对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (155, 90, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', null, 0, 0, 'bonemerang', 2, null, 2,
        null, '骨头回力镖', 50, 10, 0, 'Hits twice in one turn.', 0, '用手中的骨头投掷对手，
来回连续２次给予伤害。', 0, 0, 2, 10, 5),
       (156, null, 0, 0, 0, 'User falls to sleep and immediately regains all its HP.  If the user has another major status effect, sleep will replace it.  The user will always wake up after two turns, or one turn with early bird.

This move fails if the Pokémon cannot fall asleep due to uproar, insomnia, or vital spirit.  It also fails if the Pokémon is at full health or is already asleep.',
        null, 0, 0, 'rest', null, null, null, null, '睡觉', null, 5, 0,
        'User sleeps for two turns, completely healing itself.', 0, '连续睡上２回合。
回复自己的全部ＨＰ
以及治愈所有异常状态。', 0, 13, 1, 7, 14),
       (157, 90, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'rock-slide', null, null, null, null, '岩崩', 75, 10, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '将大岩石
猛烈地撞向对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 11, 6),
       (158, 90, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 10, 10,
        0, 'hyper-fang', null, null, null, null, '必杀门牙', 80, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用锋利的门牙
牢牢地咬住对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 1),
       (159, null, 0, 0, 0, 'Raises the user’s Attack by one stage.', null, 0, 0, 'sharpen', null, null, null, null,
        '棱角化', null, 30, 0, 'Raises the user’s Attack by one stage.', 0, '增加身体的角，
变得棱棱角角，
从而提高自己的攻击。', 0, 2, 1, 7, 1),
       (160, null, 0, 0, 0,
        'User’s type changes to the type of one of its moves, selected at random.  hidden power and weather ball are treated as normal.  Only moves with a different type are eligible, and curse is never eligible.  If the user has no suitable moves, this move will fail.',
        null, 0, 0, 'conversion', null, null, null, null, '纹理', null, 30, 0,
        'User’s type changes to the type of one of its moves at random.', 0, '将自己的属性转换成
和已学会的招式中
第一个招式相同的属性。', 0, 13, 1, 7, 1),
       (787, 100, 0, 0, 0, null, 100, 0, 0, 'apple-acid', null, null, null, null, '苹果酸', 80, 10, 0, null, 100, '使用从酸苹果中提取出来的
酸性液体进行攻击。
降低对手的特防。', 0, 6, 3, 10, 12),
       (161, 100, 20, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to burn, freeze, or paralyze the target.  One of these effects is selected at random; they do not each have independent chances to occur.',
        20, 0, 0, 'tri-attack', null, null, null, null, '三重攻击', 80, 10, 0,
        'Has a $effect_chance% chance to burn, freeze, or paralyze the target.', 0, '用３种光线进行攻击。
有时会让对手陷入
麻痹、灼伤或冰冻的状态。', -1, 4, 3, 10, 1),
       (162, 90, 0, 0, 0, 'Inflicts typeless damage equal to half the target’s remaining HP.', null, 0, 0, 'super-fang',
        null, null, null, null, '愤怒门牙', null, 10, 0, 'Inflicts damage equal to half the target’s HP.', 0, '用锋利的门牙
猛烈地咬住对手进行攻击。
对手的ＨＰ减半。', 0, 0, 2, 10, 1),
       (163, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'slash', null, null, null, null, '劈开', 70, 20, 0, 'Has an increased chance for a critical hit.', 0, '用爪子或镰刀等
劈开对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 1),
       (164, null, 0, 0, 0, 'Transfers 1/4 the user’s max HP into a doll that absorbs damage and causes most negative move effects to fail.  If the user leaves the field, the doll will vanish.  If the user cannot pay the HP cost, this move will fail.

The doll takes damage as normal, using the user’s stats and types, and will break when its HP reaches zero.  Self-inflicted damage from confusion or recoil is not absorbed.  Healing effects from opponents ignore the doll and heal the user as normal.  Moves that work based on the user’s HP still do so; the doll’s HP does not influence any move.

The doll will block major status effects, confusion, and flinching.  The effects of smelling salts and wake up slap do not trigger against a doll, even if the Pokémon behind the doll has the appropriate major status effect.  Multi-turn trapping moves like wrap will hit the doll for their regular damage, but the multi-turn trapping and damage effects will not activate.

Moves blocked or damage absorbed by the doll do not count as hitting the user or inflicting damage for any effects that respond to such, e.g., avalanche, counter, or a rowap berry.  magic coat still works as normal, even against moves the doll would block.  Opposing Pokémon that damage the doll with a leech move like absorb are healed as normal.

It will also block acupressure, block, the curse effect of curse, dream eater, embargo, flatter, gastro acid, grudge, heal block, leech seed, lock on, mean look, mimic, mind reader, nightmare, pain split, psycho shift, spider web, sketch, swagger, switcheroo, trick, worry seed, and yawn.  A Pokémon affected by yawn before summoning the doll will still fall to sleep.

The doll blocks intimidate, but all other abilities act as though the doll did not exist.  If the user has an ability that absorbs moves of a certain type for HP (such as volt absorb absorbing thunder wave), such moves will not be blocked.

life orb and berries that cause confusion still work as normal, but their respective HP loss and confusion are absorbed/blocked by the doll.

The user is still vulnerable to damage inflicted when entering or leaving the field, such as by pursuit or spikes; however, the doll will block the poison effect of toxic spikes.

The doll is passed on by baton pass.  It keeps its existing HP, but uses the replacement Pokémon’s stats and types for damage calculation.

All other effects work as normal.', null, 0, 0, 'substitute', null, null, null, null, '替身', null, 10, 0,
        'Transfers 1/4 of the user’s max HP into a doll, protecting the user from further damage or status changes until it breaks.',
        0, '削减少许自己的ＨＰ，
制造分身。
分身将成为自己的替身。', 0, 13, 1, 7, 1),
       (165, null, 0, 0, 0, 'Inflicts typeless regular damage.  User takes 1/4 its max HP in recoil.  Ignores accuracy and evasion modifiers.

This move is used automatically when a Pokémon cannot use any other move legally, e.g., due to having no PP remaining or being under the effect of both encore and torment at the same time.

This move’s recoil is not treated as recoil for the purposes of anything that affects recoil, such as the ability rock head.  It also is not prevented by magic guard.

This move cannot be copied by mimic, mirror move, or sketch, nor selected by assist or metronome, nor forced by encore.',
        null, 0, -25, 'struggle', null, null, null, null, '挣扎', 50, 1, 0, 'User takes 1/4 its max HP in recoil.', 0, '当自己的ＰＰ耗尽时，
努力挣扎攻击对手。
自己也会受到少许伤害。', 0, 0, 2, 8, 1),
       (166, null, 0, 0, 0, 'Permanently replaces itself with the target’s last used move.  If that move is chatter or struggle, this move will fail.

This move cannot be copied by mimic or mirror move, nor selected by assist or metronome, nor forced by encore.', null,
        0, 0, 'sketch', null, null, null, null, '写生', null, 1, 0, 'Permanently becomes the target’s last used move.',
        0, '将对手使用的招式
变成自己的招式。
使用１次后写生消失。', 0, 13, 1, 10, 1),
       (167, 90, 0, 0, 0, 'Inflicts regular damage.  Hits three times in the same turn.  The second hit has double power, and the third hit has triple power.  Each hit has a separate accuracy check, and this move stops if a hit misses.

skill link does not apply.', null, 0, 0, 'triple-kick', 3, null, 3, null, '三连踢', 10, 10, 0,
        'Hits three times, increasing power by 100% with each successful hit.', 0, '连续３次踢对手进行攻击。
每踢中一次，威力就会提高。', 0, 0, 2, 10, 2),
       (168, 100, 0, 0, 0, 'Inflicts regular damage.  If the target is holding an item and the user is not, the user will permanently take the item.  Damage is still inflicted if an item cannot be taken.

Pokémon with sticky hold or multitype are immune to the item theft effect.

The target cannot recover its item with recycle.

This move cannot be selected by assist or metronome.', null, 0, 0, 'thief', null, null, null, null, '小偷', 60, 25, 0,
        'Takes the target’s item.', 0, '攻击的同时盗取道具。
当自己携带道具时，
不会去盗取。', 0, 0, 2, 10, 17),
       (169, null, 0, 0, 0, 'The target cannot switch out normally.  Ignores accuracy and evasion modifiers.  This effect ends when the user leaves the field.

The target may still escape by using baton pass, u turn, or a shed shell.

Both the user and the target pass on this effect with baton pass.', null, 0, 0, 'spider-web', null, null, null, null,
        '蛛网', null, 10, 0, 'Prevents the target from leaving battle.', 0, '将黏糊糊的细丝
一层一层缠住对手，
使其不能从战斗中逃走。', 0, 13, 1, 10, 7),
       (170, null, 0, 0, 0, 'If the user targets the same target again before the end of the next turn, the move it uses is guaranteed to hit.  This move itself also ignores accuracy and evasion modifiers.

One-hit KO moves are also guaranteed to hit, as long as the user is equal or higher level than the target.  This effect also allows the user to hit Pokémon that are off the field due to moves such as dig or fly.

If the target uses detect or protect while under the effect of this move, the user is not guaranteed to hit, but has a (100 - accuracy)% chance to break through the protection.

This effect is passed on by baton pass.', null, 0, 0, 'mind-reader', null, null, null, null, '心之眼', null, 5, 0,
        'Ensures that the user’s next move will hit the target.', 0, '用心感受对手的行动，
下次攻击必定
会击中对手。', 0, 13, 1, 10, 1),
       (171, 100, 0, 0, 0,
        'Only works on sleeping Pokémon.  Gives the target a nightmare, damaging it for 1/4 its max HP every turn.  If the target wakes up or leaves the field, this effect ends.',
        null, 0, 0, 'nightmare', null, null, null, null, '恶梦', null, 15, 0,
        'Target loses 1/4 its max HP every turn as long as it’s asleep.', 0, '让在睡眠状态下的对手做恶梦，
每回合会缓缓减少ＨＰ。', 9, 1, 1, 10, 8),
       (172, 100, 10, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.  Frozen Pokémon may use this move, in which case they will thaw.',
        10, 0, 0, 'flame-wheel', null, null, null, null, '火焰轮', 60, 25, 0,
        'Has a $effect_chance% chance to burn the target.  Lets frozen Pokémon thaw themselves.', 0, '让火焰覆盖全身，
猛撞向对手进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (173, 100, 0, 0, 0,
        'Only usable if the user is sleeping.  Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.',
        30, 30, 0, 'snore', null, null, null, null, '打鼾', 50, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.  Only works if the user is sleeping.', 0, '在自己睡觉时，
发出噪音进行攻击。
有时会使对手畏缩。', 0, 0, 3, 10, 1),
       (174, null, 0, 0, 0, 'If the user is a ghost: user pays half its max HP to place a curse on the target, damaging it for 1/4 its max HP every turn.
Otherwise: Lowers the user’s Speed by one stage, and raises its Attack and Defense by one stage each.

The curse effect is passed on by baton pass.

This move cannot be copied by mirror move.', null, 0, 0, 'curse', null, null, null, null, '诅咒', null, 10, 0,
        'Ghosts pay half their max HP to hurt the target every turn.  Others decrease Speed but raise Attack and Defense.',
        0, '使用该招式的宝可梦，
其属性是幽灵属性或其他属性时，
效果会不一样。', 0, 13, 1, 1, 8),
       (175, 100, 0, 0, 0, 'Inflicts regular damage.  Power varies inversely with the user’s proportional remaining HP.

64 * current HP / max HP | Power
-----------------------: | ----:
 0– 1                    |  200
 2– 5                    |  150
 6–12                    |  100
13–21                    |   80
22–42                    |   40
43–64                    |   20
', null, 0, 0, 'flail', null, null, null, null, '抓狂', null, 15, 0,
        'Inflicts more damage when the user has less HP remaining, with a maximum of 200 power.', 0, '抓狂般乱打进行攻击。
自己的ＨＰ越少，
招式的威力越大。', 0, 0, 2, 10, 1),
       (176, null, 0, 0, 0,
        'Changes the user’s type to a type either resistant or immune to the last damaging move that hit it.  The new type is selected at random and cannot be a type the user already is.  If there is no eligible new type, this move will fail.',
        null, 0, 0, 'conversion-2', null, null, null, null, '纹理２', null, 30, 0,
        'Changes the user’s type to a random type either resistant or immune to the last move used against it.', 0, '为了可以抵抗对手
最后使用的招式，
从而使自己的属性发生变化。', 0, 13, 1, 10, 1),
       (177, 95, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'aeroblast', null, null, null, null, '气旋攻击', 100, 5, 0, 'Has an increased chance for a critical hit.', 0, '发射空气旋涡
进行攻击。
容易击中要害。', 0, 0, 3, 10, 3),
       (178, 100, 0, 0, 0, 'Lowers the target’s Speed by two stages.', null, 0, 0, 'cotton-spore', null, null, null,
        null, '棉孢子', null, 40, 0, 'Lowers the target’s Speed by two stages.', 0, '将棉花般柔软的孢子
紧贴对手，
从而大幅降低对手的速度。', 0, 2, 1, 11, 12),
       (179, 100, 0, 0, 0, 'Inflicts regular damage.  Power varies inversely with the user’s proportional remaining HP.

64 * current HP / max HP | Power
-----------------------: | ----:
 0– 1                    |  200
 2– 5                    |  150
 6–12                    |  100
13–21                    |   80
22–42                    |   40
43–64                    |   20
', null, 0, 0, 'reversal', null, null, null, null, '起死回生', null, 15, 0,
        'Inflicts more damage when the user has less HP remaining, with a maximum of 200 power.', 0, '竭尽全力进行攻击。
自己的ＨＰ越少，
招式的威力越大。', 0, 0, 2, 10, 2),
       (180, 100, 0, 0, 0,
        'Lowers the PP of the target’s last used move by 4.  If the target hasn’t used a move since entering the field, if it tried to use a move this turn and failed, or if its last used move has 0 PP remaining, this move will fail.',
        null, 0, 0, 'spite', null, null, null, null, '怨恨', null, 10, 0,
        'Lowers the PP of the target’s last used move by 4.', 0, '对对手最后使用的招式
怀有怨恨，
减少４ＰＰ该招式。', 0, 13, 1, 10, 8),
       (181, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target.', 10, 0, 0,
        'powder-snow', null, null, null, null, '细雪', 40, 25, 0, 'Has a $effect_chance% chance to freeze the target.',
        0, '将冰冷的细雪
吹向对手进行攻击。
有时会让对手陷入冰冻状态。', 3, 4, 3, 11, 15),
       (182, null, 0, 0, 0, 'No moves will hit the user for the remainder of this turn.  If the user is last to act this turn, this move will fail.

If the user successfully used detect, endure, protect, quick guard, or wide guard on the last turn, this move has a 50% chance to fail.

lock on, mind reader, and no guard provide a (100 – accuracy)% chance for moves to break through this move.  This does not apply to one-hit KO moves (fissure, guillotine, horn drill, and sheer cold); those are always blocked by this move.

thunder during rain dance and blizzard during hail have a 30% chance to break through this move.

The following effects are not prevented by this move:

* acupressure from an ally
* curse’s curse effect
* Delayed damage from doom desire and future sight; however, these moves will be prevented if they are used this turn
* feint, which will also end this move’s protection after it hits
* imprison
* perish song
* shadow force
* Moves that merely copy the user, such as transform or psych up

This move cannot be selected by assist or metronome.', null, 0, 0, 'protect', null, null, null, null, '守住', null, 10,
        4, 'Prevents any moves from hitting the user this turn.', 0, '完全抵挡
对手的攻击。
连续使出则容易失败。', 0, 13, 1, 7, 1),
       (183, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'mach-punch', null, null, null, null, '音速拳', 40,
        30, 1, 'Inflicts regular damage with no additional effect.', 0, '以迅雷不及掩耳之势出拳。
必定能够先制攻击。', 0, 0, 2, 10, 2),
       (184, 100, 0, 0, 0, 'Lowers the target’s Speed by two stages.', null, 0, 0, 'scary-face', null, null, null, null,
        '鬼面', null, 10, 0, 'Lowers the target’s Speed by two stages.', 0, '用恐怖的脸瞪着对手，
使其害怕，
从而大幅降低对手的速度。', 0, 2, 1, 10, 1),
       (185, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'feint-attack', null, null, null, null, '出奇一击', 60, 20, 0, 'Never misses.', 0, '悄悄地靠近对手，
趁其不备进行殴打。
攻击必定会命中。', 0, 0, 2, 10, 17),
       (186, 75, 0, 0, 0, 'Confuses the target.', null, 0, 0, 'sweet-kiss', null, 5, null, 2, '天使之吻', null, 10, 0,
        'Confuses the target.', 0, '像天使般可爱地亲吻对手，
从而使对手混乱。', 6, 1, 1, 10, 18),
       (187, null, 0, 0, 0,
        'User pays half its max HP to raise its Attack to +6 stages.  If the user cannot pay the HP cost, this move will fail.',
        null, 0, 0, 'belly-drum', null, null, null, null, '腹鼓', null, 10, 0,
        'User pays half its max HP to max out its Attack.', 0, '将自己的ＨＰ减少到
最大ＨＰ的一半，
从而最大限度提高自己的攻击。', 0, 13, 1, 7, 1),
       (188, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 30, 0, 0,
        'sludge-bomb', null, null, null, null, '污泥炸弹', 90, 10, 0,
        'Has a $effect_chance% chance to poison the target.', 0, '用污泥投掷对手进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 3, 10, 4),
       (838, 100, null, null, null, null, null, null, null, 'headlong-rush', null, null, null, null, '突飞猛扑', 120, 5,
        0, null, null,
        'The user smashes into the target in a full-body tackle. This also lowers the user’s defensive stats.', null,
        null, 2, 10, 5),
       (189, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 100, 0,
        0, 'mud-slap', null, null, null, null, '掷泥', 20, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 100, '向对手的脸等
投掷泥块进行攻击。
会降低对手的命中率。', 0, 6, 3, 10, 5),
       (190, 85, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 50, 0, 0,
        'octazooka', null, null, null, null, '章鱼桶炮', 65, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 50, '向对手的脸等
喷出墨汁进行攻击。
有时会降低对手的命中率。', 0, 6, 3, 10, 11),
       (191, null, 0, 0, 0, 'Scatters spikes around the opposing field, which damage opposing Pokémon that enter the field for 1/8 of their max HP.  Pokémon immune to ground moves are immune to this damage, except during gravity.  Up to three layers of spikes may be laid down, adding 1/16 to the damage done: two layers of spikes damage for 3/16 max HP, and three layers damage for 1/4 max HP.

wonder guard does not block damage from this effect.

rapid spin removes this effect from its user’s side of the field.  defog removes this effect from its target’s side of the field.',
        null, 0, 0, 'spikes', null, null, null, null, '撒菱', null, 20, 0,
        'Scatters Spikes, hurting opposing Pokémon that switch in.', 0, '在对手的脚下扔撒菱。
对替换出场的对手的宝可梦
给予伤害。', 0, 11, 1, 6, 5),
       (192, 50, 100, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 100, 0, 0,
        'zap-cannon', null, null, null, null, '电磁炮', 120, 5, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '发射大炮一样的
电流进行攻击。
让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (193, null, 0, 0, 0,
        'Resets the target’s evasion to normal and prevents any further boosting until the target leaves the field.  A ghost under this effect takes normal damage from normal and fighting moves.  This move itself ignores accuracy and evasion modifiers.',
        null, 0, 0, 'foresight', null, null, null, null, '识破', null, 40, 0,
        'Forces the target to have no Evade, and allows it to be hit by Normal and Fighting moves even if it’s a Ghost.',
        0, '对幽灵属性宝可梦没有效果的招式
以及闪避率高的对手，
使用后变得能够打中。', 17, 1, 1, 10, 1),
       (194, null, 0, 0, 0, 'If the user faints before its next move, the Pokémon that fainted it will automatically faint.  End-of-turn damage is ignored.

This move cannot be selected by assist or metronome.', null, 0, 0, 'destiny-bond', null, null, null, null, '同命', null,
        5, 0, 'If the user faints this turn, the target automatically will, too.', 0, '使出招式后，当受到对手攻击
陷入濒死时，对手也会一同濒死。
连续使出则会失败。', 0, 13, 1, 7, 8),
       (195, null, 0, 0, 0, 'Every Pokémon is given a counter that starts at 3 and decreases by 1 at the end of every turn, including this one.  When a Pokémon’s counter reaches zero, that Pokémon faints.  A Pokémon that leaves the field will lose its counter; its replacement does not inherit the effect, and other Pokémon’s counters remain.

This effect is passed on by baton pass.

This move cannot be copied by mirror move.', null, 0, 0, 'perish-song', null, 4, null, 4, '灭亡之歌', null, 5, 0,
        'User and target both faint after three turns.', 0, '倾听歌声的宝可梦
经过３回合陷入濒死。
替换后效果消失。', 20, 1, 1, 14, 1),
       (196, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, 0, 0,
        'icy-wind', null, null, null, null, '冰冻之风', 55, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, '将结冰的冷气
吹向对手进行攻击。
会降低对手的速度。', 0, 6, 3, 11, 15),
       (197, null, 0, 0, 0, 'No moves will hit the user for the remainder of this turn.  If the user is last to act this turn, this move will fail.

If the user successfully used detect, endure, protect, quick guard, or wide guard on the last turn, this move has a 50% chance to fail.

lock on, mind reader, and no guard provide a (100 – accuracy)% chance for moves to break through this move.  This does not apply to one-hit KO moves (fissure, guillotine, horn drill, and sheer cold); those are always blocked by this move.

thunder during rain dance and blizzard during hail have a 30% chance to break through this move.

The following effects are not prevented by this move:

* acupressure from an ally
* curse’s curse effect
* Delayed damage from doom desire and future sight; however, these moves will be prevented if they are used this turn
* feint, which will also end this move’s protection after it hits
* imprison
* perish song
* shadow force
* Moves that merely copy the user, such as transform or psych up

This move cannot be selected by assist or metronome.', null, 0, 0, 'detect', null, null, null, null, '看穿', null, 5, 4,
        'Prevents any moves from hitting the user this turn.', 0, '完全抵挡
对手的攻击。
连续使出则容易失败。', 0, 13, 1, 7, 2),
       (198, 90, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'bone-rush', 5, null, 2, null, '骨棒乱打', 25, 10, 0, 'Hits 2-5 times in one turn.', 0, '用坚硬的骨头
殴打对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 5),
       (199, null, 0, 0, 0, 'If the user targets the same target again before the end of the next turn, the move it uses is guaranteed to hit.  This move itself also ignores accuracy and evasion modifiers.

One-hit KO moves are also guaranteed to hit, as long as the user is equal or higher level than the target.  This effect also allows the user to hit Pokémon that are off the field due to moves such as dig or fly.

If the target uses detect or protect while under the effect of this move, the user is not guaranteed to hit, but has a (100 - accuracy)% chance to break through the protection.

This effect is passed on by baton pass.', null, 0, 0, 'lock-on', null, null, null, null, '锁定', null, 5, 0,
        'Ensures that the user’s next move will hit the target.', 0, '紧紧瞄准对手，
下次攻击必定会打中。', 0, 13, 1, 10, 1),
       (200, 100, 0, 0, 0, 'Inflicts regular damage.  User is forced to attack with this move for 2–3 turns,selected at random.  After the last hit, the user becomes confused.

safeguard does not protect against the confusion from this move.', null, 0, 0, 'outrage', null, null, null, null,
        '逆鳞', 120, 10, 0, 'Hits every turn for 2-3 turns, then confuses the user.', 0, '在２～３回合内，
乱打一气地进行攻击。
大闹一番后自己会陷入混乱。', 0, 0, 2, 8, 16),
       (201, null, 0, 0, 0, 'Changes the weather to a sandstorm for five turns.  Pokémon that are not ground, rock, or steel take 1/16 their max HP at the end of every turn.  Every rock Pokémon’s original Special Defense is raised by 50% for the duration of this effect.

solar beam’s power is halved.

moonlight, morning sun, and synthesis only heal 1/4 the user’s max HP.', null, 0, 0, 'sandstorm', null, null, null,
        null, '沙暴', null, 10, 0, 'Changes the weather to a sandstorm for five turns.', 0, '在５回合内扬起沙暴，除岩石、地面和
钢属性以外的宝可梦，都会受到伤害。
岩石属性的特防还会提高。', 0, 10, 1, 12, 6),
       (202, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'giga-drain', null, null, null, null, '终极吸取', 75, 10, 0,
        'Drains half the damage inflicted to heal the user.', 0, '吸取对手的养分进行攻击。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 3, 10, 12),
       (203, null, 0, 0, 0, 'The user’s HP cannot be lowered below 1 by any means for the remainder of this turn.

If the user successfully used detect, endure, protect, quick guard, or wide guard on the last turn, this move has a 50% chance to fail.

This move cannot be selected by assist or metronome.', null, 0, 0, 'endure', null, null, null, null, '挺住', null, 10,
        4, 'Prevents the user’s HP from lowering below 1 this turn.', 0, '即使受到攻击，
也至少会留下１ＨＰ。
连续使出则容易失败。', 0, 13, 1, 7, 1),
       (204, 100, 0, 0, 0, 'Lowers the target’s Attack by two stages.', null, 0, 0, 'charm', null, null, null, null,
        '撒娇', null, 20, 0, 'Lowers the target’s Attack by two stages.', 0, '可爱地凝视，
诱使对手疏忽大意，
从而大幅降低对手的攻击。', 0, 2, 1, 10, 18),
       (205, 90, 0, 0, 0, 'Inflicts regular damage.  User is forced to use this move for five turns.  Power doubles every time this move is used in succession to a maximum of 16x, and resets to normal after the lock-in ends.  If this move misses or becomes unusable, the lock-in ends.

If the user has used defense curl since entering the field, this move has double power.', null, 0, 0, 'rollout', null,
        null, null, null, '滚动', 30, 20, 0,
        'Power doubles every turn this move is used in succession after the first, resetting after five turns.', 0, '在５回合内连续滚动攻击对手。
招式每次击中，威力就会提高。', 0, 0, 2, 10, 6),
       (206, 100, 0, 0, 0, 'Inflicts regular damage.  Will not reduce the target’s HP below 1.', null, 0, 0,
        'false-swipe', null, null, null, null, '点到为止', 40, 40, 0, 'Cannot lower the target’s HP below 1.', 0, '对手的ＨＰ
至少会留下１ＨＰ，
如此般手下留情地攻击。', 0, 0, 2, 10, 1),
       (207, 85, 0, 0, 0,
        'Raises the target’s Attack by two stages, then confuses it.  If the target’s Attack cannot be raised by two stages, the confusion is not applied.',
        null, 0, 0, 'swagger', null, 5, null, 2, '虚张声势', null, 15, 0,
        'Raises the target’s Attack by two stages and confuses the target.', 0, '激怒对手，使其混乱。
因为愤怒，对手的攻击
会大幅提高。', 6, 5, 1, 10, 1),
       (208, null, 0, 0, 0, 'Heals the user for half its max HP.', null, 0, 50, 'milk-drink', null, null, null, null,
        '喝牛奶', null, 5, 0, 'Heals the user by half its max HP.', 0, '回复自己最大ＨＰ的一半。', 0, 3, 1, 7, 1),
       (209, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'spark', null, null, null, null, '电光', 65, 20, 0, 'Has a $effect_chance% chance to paralyze the target.', 0, '让电流覆盖全身，
猛撞向对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 13),
       (210, 95, 0, 0, 0,
        'Inflicts regular damage.  Power doubles after every time this move is used, whether consecutively or not, maxing out at 16x.  If this move misses or the user leaves the field, power resets.',
        null, 0, 0, 'fury-cutter', null, null, null, null, '连斩', 40, 20, 0,
        'Power doubles every turn this move is used in succession after the first, maxing out after five turns.', 0, '用镰刀或爪子等
切斩对手进行攻击。
连续击中，威力就会提高。', 0, 0, 2, 10, 7),
       (211, 90, 0, 0, 0, 'Inflicts regular damage. Has a 10% chance to raise the user’s Defense one stage.', 10, 0, 0,
        'steel-wing', null, null, null, null, '钢翼', 70, 25, 0,
        'Has a 10% chance to raise the user’s Defense by one stage.', 10, '用坚硬的翅膀敲打
对手进行攻击。
有时会提高自己的防御。', 0, 7, 2, 10, 9),
       (212, null, 0, 0, 0, 'The target cannot switch out normally.  Ignores accuracy and evasion modifiers.  This effect ends when the user leaves the field.

The target may still escape by using baton pass, u turn, or a shed shell.

Both the user and the target pass on this effect with baton pass.', null, 0, 0, 'mean-look', null, null, null, null,
        '黑色目光', null, 5, 0, 'Prevents the target from leaving battle.', 0, '用好似要勾人心魂的黑色目光
一动不动地凝视对手，
使其不能从战斗中逃走。', 0, 13, 1, 10, 1),
       (213, 100, 0, 0, 0,
        'Causes the target to fall in love with the user, giving it a 50% chance to do nothing each turn.  If the user and target are the same gender, or either is genderless, this move will fail.  If either Pokémon leaves the field, this effect ends.',
        null, 0, 0, 'attract', null, null, null, null, '迷人', null, 15, 0,
        'Target falls in love if it has the opposite gender, and has a 50% chance to refuse attacking the user.', 0, '♂诱惑♀或♀诱惑♂，
让对手着迷。
对手将很难使出招式。', 7, 1, 1, 10, 1),
       (214, null, 0, 0, 0, 'Only usable if the user is sleeping.  Randomly selects and uses one of the user’s other three moves.  Use of the selected move requires and costs 0 PP.

This move will not select assist, bide, bounce, chatter, copycat, dig, dive, fly, focus punch, me first, metronome, mirror move, shadow force, skull bash, sky attack, sky drop, sleep talk, solar beam, razor wind, or uproar.

If the selected move requires a recharge turn—i.e., one of blast burn, frenzy plant, giga impact, hydro cannon, hyper beam, roar of time, or rock wrecker—and the user is still sleeping next turn, then it’s forced to use this move again and pay another PP for the recharge turn.

This move cannot be copied by mirror move, nor selected by assist, metronome, or sleep talk.', null, 0, 0, 'sleep-talk',
        null, null, null, null, '梦话', null, 10, 0,
        'Randomly uses one of the user’s other three moves.  Only works if the user is sleeping.', 0, '从自己已学会的招式中
任意使出１个。
只能在自己睡觉时使用。', 0, 13, 1, 7, 1),
       (215, null, 0, 0, 0, 'Removes major status effects and confusion from every Pokémon in the user’s party.', null,
        0, 0, 'heal-bell', null, null, null, null, '治愈铃声', null, 5, 0,
        'Cures the entire party of major status effects.', 0, '让同伴听舒适的铃音，
从而治愈我方全员的异常状态。', 0, 13, 1, 13, 1),
       (216, 100, 0, 0, 0,
        'Inflicts regular damage.  Power increases with happiness, given by `happiness * 2 / 5`, to a maximum of 102.  Power bottoms out at 1.',
        null, 0, 0, 'return', null, null, null, null, '报恩', null, 20, 0,
        'Power increases with happiness, up to a maximum of 102.', 0, '为了训练家而
全力攻击对手。
亲密度越高，威力越大。', 0, 0, 2, 10, 1),
       (217, 90, 0, 0, 0, 'Randomly uses one of the following effects.

Effect                                             | Chance
-------------------------------------------------- | -----:
Inflicts regular damage with 40 power  |    40%
Inflicts regular damage with 80 power  |    30%
Inflicts regular damage with 120 power |    10%
Heals the target for 1/4 its max HP    |    20%

On average, this move inflicts regular damage with 52 power and heals the target for 1/20 its max HP.', null, 0, 0,
        'present', null, null, null, null, '礼物', null, 15, 0,
        'Randomly inflicts damage with power from 40 to 120 or heals the target for 1/4 its max HP.', 0, '递给对手设有圈套的
盒子进行攻击。
也有可能回复对手ＨＰ。', 0, 0, 2, 10, 1),
       (218, 100, 0, 0, 0,
        'Inflicts regular damage.  Power increases inversely with happiness, given by `(255 - happiness) * 2 / 5`, to a maximum of 102.  Power bottoms out at 1.',
        null, 0, 0, 'frustration', null, null, null, null, '迁怒', null, 20, 0,
        'Power increases as happiness decreases, up to a maximum of 102.', 0, '为了发泄不满而
全力攻击对手。
亲密度越低，威力越大。', 0, 0, 2, 10, 1),
       (219, null, 0, 0, 0, 'Protects Pokémon on the user’s side of the field from major status effects and confusion for five turns.  Does not cancel existing ailments.  This effect remains even if the user leaves the field.

If yawn is used while this move is in effect, it will immediately fail.

defog used by an opponent will end this effect.

This effect does not prevent the confusion caused by outrage, petal dance, or thrash.', null, 0, 0, 'safeguard', null,
        null, null, null, '神秘守护', null, 25, 0,
        'Protects the user’s field from major status ailments and confusion for five turns.', 0, '在５回合内
被神奇的力量守护，
从而不会陷入异常状态。', 0, 11, 1, 4, 1),
       (220, null, 0, 0, 0, 'Changes the user’s and target’s remaining HP to the average of their current remaining HP.  Ignores accuracy and evasion modifiers.  This effect does not count as inflicting damage for other moves and effects that respond to damage taken.

This effect fails against a substitute.', null, 0, 0, 'pain-split', null, null, null, null, '分担痛楚', null, 20, 0,
        'Sets the user’s and targets’s HP to the average of their current HP.', 0, '将自己的ＨＰ和
对手的ＨＰ相加，
然后自己和对手友好地平分。', 0, 13, 1, 10, 1),
       (221, 95, 50, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.  Frozen Pokémon may use this move, in which case they will thaw.',
        50, 0, 0, 'sacred-fire', null, null, null, null, '神圣之火', 100, 5, 0,
        'Has a $effect_chance% chance to burn the target.  Lets frozen Pokémon thaw themselves.', 0, '用神秘的火焰
烧尽对手进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (222, 100, 0, 0, 0, 'Inflicts regular damage.  Power is selected at random between 10 and 150, with an average of 71:

Magnitude | Power | Chance
--------: | ----: | -----:
        4 |    10 |     5%
        5 |    30 |    10%
        6 |    50 |    20%
        7 |    70 |    30%
        8 |    90 |    20%
        9 |   110 |    10%
       10 |   150 |     5%

This move has double power against Pokémon currently underground due to dig.', null, 0, 0, 'magnitude', null, null,
        null, null, '震级', null, 30, 0, 'Power varies randomly from 10 to 150.', 0, '晃动地面，攻击自己
周围所有的宝可梦。
招式的威力会有各种变化。', 0, 0, 2, 9, 5),
       (223, 50, 100, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 100, 0, 0,
        'dynamic-punch', null, 5, null, 2, '爆裂拳', 100, 5, 0, 'Has a $effect_chance% chance to confuse the target.',
        0, '使出浑身力气出拳进行攻击。
必定会使对手混乱。', 6, 4, 2, 10, 2),
       (224, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'megahorn', null, null, null, null, '超级角击', 120,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '用坚硬且华丽的角狠狠地
刺入对手进行攻击。', 0, 0, 2, 10, 7),
       (225, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'dragon-breath', null, null, null, null, '龙息', 60, 20, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '将强烈的气息
吹向对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 3, 10, 16),
       (226, null, 0, 0, 0, 'User switches out, and the trainer selects a replacement Pokémon from the party.  Stat changes, confusion, and persistent move effects are passed along to the replacement Pokémon.

The following move effects are passed:

* aqua ring
* both the user’s and target’s effect of block, mean look, and spider web
* the curse effect of curse
* embargo
* focus energy or an activated lansat berry
* gastro acid
* ingrain
* being sapped by leech seed
* being targeted by lock on or mind reader
* magnet rise
* perish song’s counter
* power trick
* substitute; the doll’s HP is unchanged

The replacement Pokémon does not trigger effects that respond to Pokémon switching in.', null, 0, 0, 'baton-pass', null,
        null, null, null, '接棒', null, 40, 0,
        'Allows the trainer to switch out the user and pass effects along to its replacement.', 0, '和后备宝可梦进行替换。
换上的宝可梦能直接继承
其能力的变化。', 0, 13, 1, 7, 1),
       (227, 100, 0, 0, 0, 'The next 4–8 times (selected at random) the target attempts to move, it is forced to repeat its last used move.  If the selected move allows the trainer to select a target, an opponent will be selected at random each turn.  If the target is prevented from using the selected move by some other effect, struggle will be used instead.  This effect ends if the selected move runs out of PP.

If the target hasn’t used a move since entering the field, if it tried to use a move this turn and failed, if it does not know the selected move, or if the selected move has 0 PP remaining, this move will fail.  If the target’s last used move was encore, mimic, mirror move, sketch, struggle, or transform, this move will fail.',
        null, 0, 0, 'encore', null, null, null, null, '再来一次', null, 5, 0,
        'Forces the target to repeat its last used move every turn for 2 to 6 turns.', 0, '让对手接受再来一次，
连续３次使出最后使用的招式。', 0, 13, 1, 10, 1),
       (228, 100, 0, 0, 0, 'Inflicts regular damage.  If the target attempts to switch out this turn before the user acts, this move hits the target before it leaves and has double power.

This effect can still hit a Pokémon that switches out when it has a substitute up or when an ally has used follow me.',
        null, 0, 0, 'pursuit', null, null, null, null, '追打', 40, 20, 0,
        'Has double power against, and can hit, Pokémon attempting to switch out.', 0, '当对手替换宝可梦上场时
使出此招式的话，
能够以２倍的威力进行攻击。', 0, 0, 2, 10, 17),
       (229, 100, 0, 0, 0,
        'Inflicts regular damage.  Removes leech seed from the user, frees the user from bind, clamp, fire spin, magma storm, sand tomb, whirlpool, and wrap, and clears spikes, stealth rock, and toxic spikes from the user’s side of the field.  If this move misses or has no effect, its effect doesn’t activate.',
        100, 0, 0, 'rapid-spin', null, null, null, null, '高速旋转', 50, 40, 0,
        'Frees the user from binding moves, removes Leech Seed, and blows away Spikes.', 100, '通过旋转来攻击对手。
还可以摆脱绑紧、紧束、
寄生种子和撒菱等招式。', 0, 7, 2, 10, 1),
       (230, 100, 0, 0, 0, 'Lowers the target’s evasion by one stage.', null, 0, 0, 'sweet-scent', null, null, null,
        null, '甜甜香气', null, 20, 0, 'Lowers the target’s evasion by one stage.', 0, '用香气大幅降低对手的闪避率。', 0,
        2, 1, 11, 1),
       (231, 75, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 30, 0, 0,
        'iron-tail', null, null, null, null, '铁尾', 100, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 30, '使用坚硬的尾巴
摔打对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 9),
       (232, 95, 0, 0, 0, 'Inflicts regular damage. Has a $effect_chance% chance to raise the user’s Attack one stage.',
        10, 0, 0, 'metal-claw', null, null, null, null, '金属爪', 50, 35, 0,
        'Has a $effect_chance% chance to raise the user’s Attack by one stage.', 10, '用钢铁之爪
劈开对手进行攻击。
有时会提高自己的攻击。', 0, 7, 2, 10, 9),
       (233, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'vital-throw', null, null, null, null, '借力摔', 70, 10, -1, 'Never misses.', 0, '会在对手之后进行攻击。
但是自己的攻击必定会命中。', 0, 0, 2, 10, 2),
       (234, null, 0, 0, 0, 'Heals the user for half its max HP.

During sunny day, the healing is increased to 2/3 max HP.

During hail, rain dance, or sandstorm, the healing is decreased to 1/4 max HP.', null, 0, 50, 'morning-sun', null, null,
        null, null, '晨光', null, 5, 0, 'Heals the user by half its max HP.  Affected by weather.', 0, '回复自己的ＨＰ。
根据天气的不同，
回复量也会有所变化。', 0, 3, 1, 7, 1),
       (235, null, 0, 0, 0, 'Heals the user for half its max HP.

During sunny day, the healing is increased to 2/3 max HP.

During hail, rain dance, or sandstorm, the healing is decreased to 1/4 max HP.', null, 0, 50, 'synthesis', null, null,
        null, null, '光合作用', null, 5, 0, 'Heals the user by half its max HP.  Affected by weather.', 0, '回复自己的ＨＰ。
根据天气的不同，
回复量也会有所变化。', 0, 3, 1, 7, 12),
       (236, null, 0, 0, 0, 'Heals the user for half its max HP.

During sunny day, the healing is increased to 2/3 max HP.

During hail, rain dance, or sandstorm, the healing is decreased to 1/4 max HP.', null, 0, 50, 'moonlight', null, null,
        null, null, '月光', null, 5, 0, 'Heals the user by half its max HP.  Affected by weather.', 0, '回复自己的ＨＰ。
根据天气的不同，
回复量也会有所变化。', 0, 3, 1, 7, 18),
       (237, 100, 0, 0, 0, 'Inflicts regular damage.  Power and type are determined by the user’s IVs.

Power is given by `x * 40 / 63 + 30`.  `x` is obtained by arranging bit 1 from the IV for each of Special Defense, Special Attack, Speed, Defense, Attack, and HP in that order.  (Bit 1 is 1 if the IV is of the form `4n + 2` or `4n + 3`.  `x` is then 64 * Special Defense IV bit 1, plus 32 * Special Attack IV bit 1, etc.)

Power is always between 30 and 70, inclusive.  Average power is 49.5.

Type is given by `y * 15 / 63`, where `y` is similar to `x` above, except constructed from bit 0.  (Bit 0 is 1 if the IV is odd.)  The result is looked up in the following table.

Value | Type
----: | --------
    0 | fighting
    1 | flying
    2 | poison
    3 | ground
    4 | rock
    5 | bug
    6 | ghost
    7 | steel
    8 | fire
    9 | water
   10 | grass
   11 | electric
   12 | psychic
   13 | ice
   14 | dragon
   15 | dark

This move thus cannot be normal.  Most other types have an equal 1/16 chance to be selected, given random IVs.  However, due to the flooring used here, bug, fighting, and grass appear 5/64 of the time, and dark only 1/64 of the time.',
        null, 0, 0, 'hidden-power', null, null, null, null, '觉醒力量', 60, 15, 0,
        'Power and type depend upon user’s IVs.  Power can range from 30 to 70.', 0, '招式的属性会随着
使用此招式的宝可梦而改变。', 0, 0, 3, 10, 1),
       (238, 80, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'cross-chop', null, null, null, null, '十字劈', 100, 5, 0, 'Has an increased chance for a critical hit.', 0, '用两手呈十字
劈打对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 2),
       (239, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make each target flinch.

If the target is under the effect of bounce, fly, or sky drop, this move will hit with double power.', 20, 20, 0,
        'twister', null, null, null, null, '龙卷风', 40, 20, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '兴起龙卷风，
将对手卷入进行攻击。
有时会使对手畏缩。', 0, 0, 3, 11, 16),
       (240, null, 0, 0, 0, 'Changes the weather to rain for five turns, during which water moves inflict 50% extra damage, and fire moves inflict half damage.

If the user is holding damp rock, this effect lasts for eight turns.

thunder has 100% accuracy.  If the target has used detect or protect, thunder has a (100 - accuracy)% chance to break through the protection.

solar beam has half power.

moonlight, morning sun, and synthesis heal only 1/4 of the user’s max HP.

Pokémon with swift swim have doubled original Speed.

Pokémon with forecast become water.

Pokémon with dry skin heal 1/8 max HP, Pokémon with hydration are cured of major status effects, and Pokémon with rain dish heal 1/16 max HP at the end of each turn.',
        null, 0, 0, 'rain-dance', null, null, null, null, '求雨', null, 5, 0,
        'Changes the weather to rain for five turns.', 0, '在５回合内一直降雨，
从而提高水属性的招式威力。
火属性的招式威力则降低。', 0, 10, 1, 12, 11),
       (241, null, 0, 0, 0, 'Changes the weather to sunshine for five turns, during which fire moves inflict 50% extra damage, and water moves inflict half damage.

If the user is holding heat rock, this effect lasts for eight turns.

Pokémon cannot become frozen.

thunder has 50% accuracy.

solar beam skips its charge turn.

moonlight, morning sun, and synthesis heal 2/3 of the user’s max HP.

Pokémon with chlorophyll have doubled original Speed.

Pokémon with forecast become fire.

Pokémon with leaf guard are not affected by major status effects.

Pokémon with flower gift change form; every Pokémon on their side of the field have their original Attack and Special Attack increased by 50%.

Pokémon with dry skin lose 1/8 max HP at the end of each turn.

Pokémon with solar power have their original Special Attack raised by 50% but lose 1/8 their max HP at the end of each turn.',
        null, 0, 0, 'sunny-day', null, null, null, null, '大晴天', null, 5, 0,
        'Changes the weather to sunny for five turns.', 0, '在５回合内阳光变得强烈，
从而提高火属性的招式威力。
水属性的招式威力则降低。', 0, 10, 1, 12, 10),
       (242, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, 0, 0,
        'crunch', null, null, null, null, '咬碎', 80, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, '用利牙咬碎对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 17),
       (243, 100, 0, 0, 0, 'Targets the last opposing Pokémon to hit the user with a special move this turn.  Inflicts twice the damage that move did to the user.  If there is no eligible target, this move will fail.  Type immunity applies, but other type effects are ignored.

This move cannot be copied by mirror move, nor selected by assist or metronome.', null, 0, 0, 'mirror-coat', null, null,
        null, null, '镜面反射', null, 20, -5,
        'Inflicts twice the damage the user received from the last special hit it took.', 0, '从对手那里受到
特殊攻击的伤害将以
２倍返还给同一个对手。', 0, 0, 3, 1, 14),
       (244, null, 0, 0, 0, 'Discards the user’s stat changes and copies the target’s.

This move cannot be copied by mirror move.', null, 0, 0, 'psych-up', null, null, null, null, '自我暗示', null, 10, 0,
        'Discards the user’s stat changes and copies the target’s.', 0, '向自己施以自我暗示，
将能力变化的状态
变得和对手一样。', 0, 13, 1, 10, 1),
       (245, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'extreme-speed', null, null, null, null, '神速', 80,
        5, 2, 'Inflicts regular damage with no additional effect.', 0, '以迅雷不及掩耳之势
猛撞向对手进行攻击。
必定能够先制攻击。', 0, 0, 2, 10, 1),
       (246, 100, 0, 0, 0,
        'Inflicts regular damage. Has a $effect_chance% chance to raise all of the user’s stats one stage.', 10, 0, 0,
        'ancient-power', null, null, null, null, '原始之力', 60, 5, 0,
        'Has a $effect_chance% chance to raise all of the user’s stats by one stage.', 10, '用原始之力进行攻击。
有时会提高
自己所有的能力。', 0, 7, 3, 10, 6),
       (247, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        20, 0, 0, 'shadow-ball', null, null, null, null, '暗影球', 80, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 20, '投掷一团黑影进行攻击。
有时会降低对手的特防。', 0, 6, 3, 10, 8),
       (248, 100, 0, 0, 0, 'Inflicts typeless regular damage at the end of the third turn, starting with this one.  This move cannot score a critical hit.  If the target switches out, its replacement will be hit instead.  Damage is calculated at the time this move is used; stat changes and switching out during the delay won’t change the damage inflicted.  No move with this effect can be used against the same target again until after the end of the third turn.

This effect breaks through wonder guard.

If the target is protected by protect or detect on the turn this move is used, this move will fail.  However, the damage on the third turn will break through protection.

The damage is applied at the end of the turn, so it ignores endure and focus sash.

This move cannot be copied by mirror move.', null, 0, 0, 'future-sight', null, null, null, null, '预知未来', 120, 10, 0,
        'Hits the target two turns later.', 0, '在使用招式２回合后，
向对手发送一团念力进行攻击。', 0, 13, 3, 10, 14),
       (249, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, 0, 0,
        'rock-smash', null, null, null, null, '碎岩', 40, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, '用拳头进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 2),
       (250, 85, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

If the target is in the first turn of dive, this move will hit with double power.', 100, 0, 0, 'whirlpool', null, 6,
        null, 5, '潮旋', 35, 15, 0,
        'Prevents the target from leaving battle and inflicts 1/16 its max HP in damage for 2-5 turns.', 0, '将对手困在激烈的
水流旋涡中，
在４～５回合内进行攻击。', 8, 4, 3, 10, 11),
       (251, 100, 0, 0, 0, 'Inflicts typeless regular damage.  Every Pokémon in the user’s party, excepting those that have fainted or have a major status effect, attacks the target.  Calculated stats are ignored; the base stats for the target and assorted attackers are used instead.  The random factor in the damage formula is not used.  dark Pokémon still get STAB.

This effect breaks through wonder guard.', null, 0, 0, 'beat-up', 6, null, 6, null, '围攻', null, 10, 0,
        'Hits once for every conscious Pokémon the trainer has.', 0, '我方全员进行攻击。
同行的宝可梦越多，
招式的攻击次数越多。', 0, 0, 2, 10, 17),
       (252, 100, 0, 0, 0,
        'Inflicts regular damage.  Causes the target to flinch.  Can only be used on the user’s first turn after entering the field.',
        100, 100, 0, 'fake-out', null, null, null, null, '击掌奇袭', 40, 10, 3,
        'Can only be used as the first move after the user enters battle.  Causes the target to flinch.', 0, '进行先制攻击，使对手畏缩。
要在出场后立刻使出才能成功。', 0, 0, 2, 10, 1),
       (253, 100, 0, 0, 0, 'Inflicts regular damage.  User is forced to use this move for 2–5 turns, selected at random.  All Pokémon on the field wake up, and none can fall to sleep until the lock-in ends.

Pokémon cannot use rest during this effect.

This move cannot be selected by sleep talk.', null, 0, 0, 'uproar', null, 3, null, 3, '吵闹', 90, 10, 0,
        'Forced to use this move for several turns.  Pokémon cannot fall asleep in that time.', 0, '在３回合内
用骚乱攻击对手。
在此期间谁都不能入眠。', 0, 0, 3, 8, 1),
       (254, null, 0, 0, 0, 'Raises the user’s Defense and Special Defense by one stage each.  Stores energy for use with spit up and swallow.  Up to three levels of energy can be stored, and all are lost if the user leaves the field.  Energy is still stored even if the stat boosts cannot be applied.

If the user uses baton pass, the stat boosts are passed as normal, but the stored energy is not.', null, 0, 0,
        'stockpile', null, null, null, null, '蓄力', null, 20, 0,
        'Stores energy up to three times for use with Spit Up and Swallow.', 0, '积蓄力量，
提高自己的防御和特防。
最多积蓄３次。', 0, 13, 1, 7, 1),
       (255, 100, 0, 0, 0, 'Inflicts regular damage.  Power is equal to 100 times the amount of energy stored by stockpile.  Ignores the random factor in the damage formula.  Stored energy is consumed, and the user’s Defense and Special Defense are reset to what they would be if stockpile had not been used.  If the user has no energy stored, this move will fail.

This move cannot be copied by mirror move.', null, 0, 0, 'spit-up', null, null, null, null, '喷出', null, 10, 0,
        'Power is 100 times the amount of energy Stockpiled.', 0, '将积蓄的力量
撞向对手进行攻击。
积蓄得越多，威力越大。', 0, 0, 3, 10, 1),
       (256, null, 0, 0, 0,
        'Heals the user depending on the amount of energy stored by stockpile: 1/4 its max HP after one use, 1/2 its max HP after two uses, or fully after three uses.  Stored energy is consumed, and the user’s Defense and Special Defense are reset to what they would be if stockpile had not been used.  If the user has no energy stored, this move will fail.',
        null, 0, 25, 'swallow', null, null, null, null, '吞下', null, 10, 0,
        'Recovers 1/4 HP after one Stockpile, 1/2 HP after two Stockpiles, or full HP after three Stockpiles.', 0, '将积蓄的力量吞下，
从而回复自己的ＨＰ。
积蓄得越多，回复越大。', 0, 3, 1, 7, 1),
       (257, 90, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'heat-wave', null, null, null, null, '热风', 95, 10, 0, 'Has a $effect_chance% chance to burn the target.', 0, '将炎热的气息
吹向对手进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 11, 10),
       (284, 100, 0, 0, 0,
        'Inflicts regular damage.  Power increases with the user’s remaining HP and is given by `150 * HP / max HP`, to a maximum of 150 when the user has full HP.',
        null, 0, 0, 'eruption', null, null, null, null, '喷火', 150, 5, 0,
        'Inflicts more damage when the user has more HP remaining, with a maximum of 150 power.', 0, '爆发怒火攻击对手。
自己的ＨＰ越少，
招式的威力越小。', 0, 0, 3, 11, 10),
       (258, null, 0, 0, 0, 'Changes the weather to hail for five turns, during which non-ice Pokémon are damaged for 1/16 their max HP at the end of every turn.

If the user is holding icy rock, this effect lasts for eight turns.

blizzard has 100% accuracy.  If the target has used detect or protect, blizzard has a (100 - accuracy)% chance to break through the protection.

moonlight, morning sun, and synthesis heal only 1/4 of the user’s max HP.

Pokémon with snow cloak are exempt from this effect’s damage.', null, 0, 0, 'hail', null, null, null, null, '冰雹',
        null, 10, 0, 'Changes the weather to a hailstorm for five turns.', 0, '在５回合内一直降冰雹，
除冰属性的宝可梦以外，
给予全体宝可梦伤害。', 0, 10, 1, 12, 15),
       (259, 100, 0, 0, 0, 'Prevents the target from attempting to use the same move twice in a row.  When the target leaves the field, this effect ends.

If the target is forced to attempt a repeated move due to choice band, choice scarf, choice specs, disable, encore, taunt, only having PP remaining for one move, or any other effect, the target will use struggle instead.  The target is then free to use the forced move next turn, as it didn’t use that move this turn.',
        null, 0, 0, 'torment', null, null, null, null, '无理取闹', null, 15, 0,
        'Prevents the target from using the same move twice in a row.', 0, '向对手无理取闹，
令其不能连续２次
使出相同招式。', 12, 1, 1, 10, 17),
       (260, 100, 0, 0, 0, 'Raises the target’s Special Attack by one stage, then confuses it.', null, 0, 0, 'flatter',
        null, 5, null, 2, '吹捧', null, 15, 0,
        'Raises the target’s Special Attack by one stage and confuses the target.', 0, '吹捧对手，使其混乱。
同时还会提高对手的特攻。', 6, 5, 1, 10, 17),
       (261, 85, 0, 0, 0, 'Burns the target.', null, 0, 0, 'will-o-wisp', null, null, null, null, '鬼火', null, 15, 0,
        'Burns the target.', 0, '放出怪异的火焰，
从而让对手陷入灼伤状态。', 4, 1, 1, 10, 10),
       (262, 100, 0, 0, 0, 'Lowers the target’s Attack and Special Attack by two stages.  User faints.', null, 0, 0,
        'memento', null, null, null, null, '临别礼物', null, 10, 0,
        'Lowers the target’s Attack and Special Attack by two stages.  User faints.', 0, '虽然会使自己陷入濒死，
但是能够大幅降低
对手的攻击和特攻。', 0, 13, 1, 10, 17),
       (263, 100, 0, 0, 0,
        'Inflicts regular damage.  If the user is burned, paralyzed, or poisoned, this move has double power.', null, 0,
        0, 'facade', null, null, null, null, '硬撑', 70, 20, 0,
        'Power doubles if user is burned, paralyzed, or poisoned.', 0, '当自己处于中毒、麻痹、灼伤状态时，
向对手使出此招式的话，
威力会变成２倍。', 0, 0, 2, 10, 1),
       (264, 100, 0, 0, 0, 'Inflicts regular damage.  If the user takes damage this turn before hitting, this move will fail.

This move cannot be copied by mirror move, nor selected by assist, metronome, or sleep talk.', null, 0, 0,
        'focus-punch', null, null, null, null, '真气拳', 150, 20, -3,
        'If the user takes damage before attacking, the attack is canceled.', 0, '集中精神出拳。
在招式使出前
若受到攻击则会失败。', 0, 0, 2, 10, 2),
       (265, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target is paralyzed, this move has double power, and the target is cured of its paralysis.',
        null, 0, 0, 'smelling-salts', null, null, null, null, '清醒', 70, 10, 0,
        'If the target is paralyzed, inflicts double damage and cures the paralysis.', 0, '对于麻痹状态下的对手，
威力会变成２倍。
但相反对手的麻痹也会被治愈。', 0, 0, 2, 10, 1),
       (266, null, 0, 0, 0, 'Until the end of this turn, any moves that opposing Pokémon target solely at the user’s ally will instead target the user.  If both Pokémon on the same side of the field use this move on the same turn, the Pokémon that uses it last will become the target.

This effect takes priority over lightning rod and storm drain.

If the user’s ally switches out, opposing Pokémon may still hit it with pursuit.

This move cannot be selected by assist or metronome.', null, 0, 0, 'follow-me', null, null, null, null, '看我嘛', null,
        20, 2, 'Redirects the target’s single-target effects to the user for this turn.', 0, '引起对手的注意，
将对手的攻击
全部转移到自己身上。', 0, 13, 1, 7, 1),
       (267, null, 0, 0, 0, 'Uses another move chosen according to the terrain.

Terrain                   | Selected move
------------------------- | ------------------
Building                  | tri attack
Cave                      | rock slide
Deep water                | hydro pump
Desert                    | earthquake
Grass                     | seed bomb
Mountain                  | rock slide
Road                      | earthquake
Shallow water             | hydro pump
Snow                      | blizzard
Tall grass                | seed bomb
electric terrain | thunderbolt
grassy terrain   | energy ball
misty terrain    | moonblast

In Pokémon Battle Revolution:

Terrain        | Selected move
-------------- | ------------------
Courtyard      | tri attack
Crystal        | rock slide
Gateway        | hydro pump
Magma          | rock slide
Main Street    | tri attack
Neon           | tri attack
Stargazer      | rock slide
Sunny Park     | seed bomb
Sunset         | earthquake
Waterfall      | seed bomb

This move cannot be copied by mirror move.', null, 0, 0, 'nature-power', null, null, null, null, '自然之力', null, 20,
        0, 'Uses a move which depends upon the terrain.', 0, '用自然之力进行攻击。
根据所使用场所的不同，
使出的招式也会有所变化。', 0, 13, 1, 10, 1),
       (268, null, 0, 0, 0,
        'Raises the user’s Special Defense by one stage.  If the user uses an electric move next turn, its power will be doubled.',
        null, 0, 0, 'charge', null, null, null, null, '充电', null, 20, 0,
        'Raises the user’s Special Defense by one stage.  User’s Electric moves have doubled power next turn.', 0, '提高下一回合使出的
电属性的招式威力。
自己的特防也会提高。', 0, 2, 1, 7, 13),
       (269, 100, 0, 0, 0, 'Target is forced to only use damaging moves for the next 3–5 turns, selected at random.  Moves that select other moves not known in advance do not count as damaging.

assist, copycat, me first, metronome, mirror move, and sleep talk do not directly inflict damage and thus may not be used.

bide, counter, endeavor, metal burst, and mirror coat are allowed.', null, 0, 0, 'taunt', null, null, null, null,
        '挑衅', null, 20, 0, 'For the next few turns, the target can only use damaging moves.', 0, '使对手愤怒。
在３回合内让对手
只能使出给予伤害的招式。', 0, 13, 1, 10, 17),
       (270, null, 0, 0, 0, 'Boosts the power of the target’s moves by 50% until the end of this turn.

This move cannot be copied by mirror move, nor selected by assist or metronome.', null, 0, 0, 'helping-hand', null,
        null, null, null, '帮助', null, 20, 5, 'Ally’s next move inflicts half more damage.', 0, '帮助伙伴。
被帮助的宝可梦，
其招式威力变得比平时大。', 0, 13, 1, 3, 1),
       (285, null, 0, 0, 0, 'User and target switch abilities.  Ignores accuracy and evasion modifiers.

If either Pokémon has multitype or wonder guard, this move will fail.', null, 0, 0, 'skill-swap', null, null, null,
        null, '特性互换', null, 10, 0, 'User and target swap abilities.', 0, '利用超能力互换
自己和对手的特性。', 0, 13, 1, 10, 14),
       (871, 100, null, null, null, null, null, null, null, 'torch-song', null, null, null, null, '闪焰高歌', 80, 10, 0,
        null, null,
        'The user blows out raging flames as if singing a song, scorching the target. This also boosts the user''s Sp. Atk stat.',
        null, null, 3, 10, 10),
       (271, 100, 0, 0, 0, 'User and target permanently swap held items.  Works even if one of the Pokémon isn’t holding anything.  If either Pokémon is holding mail, this move will fail.

If either Pokémon has multitype or sticky hold, this move will fail.

If this move results in a Pokémon obtaining choice band, choice scarf, or choice specs, and that Pokémon was the latter of the pair to move this turn, then the move it used this turn becomes its chosen forced move.  This applies even if both Pokémon had a choice item before this move was used.  If the first of the two Pokémon gains a choice item, it may select whatever choice move it wishes next turn.

Neither the user nor the target can recover its item with recycle.

This move cannot be selected by assist or metronome.', null, 0, 0, 'trick', null, null, null, null, '戏法', null, 10, 0,
        'User and target swap items.', 0, '抓住对手的空隙，
交换自己和对手的持有物。', 0, 13, 1, 10, 14),
       (272, null, 0, 0, 0, 'User’s ability is replaced with the target’s until the user leaves the field.  Ignores accuracy and evasion modifiers.

If the target has flower gift, forecast, illusion, imposter, multitype, stance change, trace, wonder guard, or zen mode, this move will fail.

This move cannot be copied by mirror move.', null, 0, 0, 'role-play', null, null, null, null, '扮演', null, 10, 0,
        'Copies the target’s ability.', 0, '扮演对手，
让自己的特性
变得和对手相同。', 0, 13, 1, 10, 14),
       (273, null, 0, 0, 0,
        'At the end of the next turn, user will be healed for half its max HP.  If the user is switched out, its replacement will be healed instead for half of the user’s max HP.  If the user faints or is forcefully switched by roar or whirlwind, this effect will not activate.',
        null, 0, 0, 'wish', null, null, null, null, '祈愿', null, 10, 0,
        'User will recover half its max HP at the end of the next turn.', 0, '在下一回合回复自己或是
替换出场的宝可梦最大ＨＰ的一半。', 0, 13, 1, 7, 1),
       (274, null, 0, 0, 0, 'Uses a move from another Pokémon in the user’s party, both selected at random.  Moves from fainted Pokémon can be used.  If there are no eligible Pokémon or moves, this move will fail.

This move will not select assist, chatter, circle throw, copycat, counter, covet, destiny bond, detect, dig, dive, dragon tail, endure, feint, fly focus punch, follow me, helping hand, me first, metronome, mimic, mirror coat, mirror move, phantom force protect, quick guard, roar shadow force, sketch, sleep talk, snatch, struggle, switcheroo, thief, trick, whirlwind, or wide guard.

This move cannot be copied by mirror move, nor selected by metronome or sleep talk.', null, 0, 0, 'assist', null, null,
        null, null, '借助', null, 20, 0, 'Randomly selects and uses one of the trainer’s other Pokémon’s moves.', 0, '向我方紧急求助，
从我方宝可梦已学会的
招式中随机使用１个。', 0, 13, 1, 7, 1),
       (275, null, 0, 0, 0, 'Prevents the user from switching out.  User regains 1/16 of its max HP at the end of every turn.  If the user was immune to ground attacks, it will now take normal damage from them.

roar and whirlwind will not affect the user.  The user cannot use magnet rise.

The user may still use u turn to leave the field.

This effect can be passed with baton pass.', null, 0, 0, 'ingrain', null, null, null, null, '扎根', null, 20, 0,
        'Prevents the user from leaving battle.  User regains 1/16 of its max HP every turn.', 0, '在大地上扎根，
每回合回复自己的ＨＰ。
因为扎根了，所以不能替换宝可梦。', 21, 1, 1, 7, 12),
       (276, 100, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Attack and Defense by one stage each.', 100,
        0, 0, 'superpower', null, null, null, null, '蛮力', 120, 5, 0,
        'Lowers the user’s Attack and Defense by one stage after inflicting damage.', 100, '发挥惊人的力量攻击对手。
自己的攻击和防御会降低。', 0, 7, 2, 10, 2),
       (277, null, 0, 0, 0, 'The first non-damaging move targeting the user this turn that inflicts major status effects, stat changes, or trapping effects will be reflected at its user.

defog, memento, and teeter dance are not reflected.

attract, flatter, gastro acid, leech seed, swagger, worry seed, and yawn are reflected.

This move cannot be copied by mirror move.', null, 0, 0, 'magic-coat', null, null, null, null, '魔法反射', null, 15, 4,
        'Reflects back the first effect move used on the user this turn.', 0, '当对手使出会变成异常状态的
招式或寄生种子等时，
会将对手的招式反射回去。', 0, 13, 1, 7, 14),
       (278, null, 0, 0, 0, 'User recovers the last item consumed by the user or a Pokémon in its position on the field.  The item must be used again before it can be recovered by this move again.  If the user is holding an item, this move fails.

Items taken or given away by covet, knock off, switcheroo, thief, or trick may not be recovered.', null, 0, 0,
        'recycle', null, null, null, null, '回收利用', null, 10, 0, 'User recovers the item it last used up.', 0, '使战斗中已经消耗掉的
自己的持有物再生，
并可以再次使用。', 0, 13, 1, 7, 1),
       (279, 100, 0, 0, 0, 'Inflicts regular damage.  If the target damaged the user this turn and was the last to do so, this move has double power.

pain split does not count as damaging the user.', null, 0, 0, 'revenge', null, null, null, null, '报复', 60, 10, -4,
        'Inflicts double damage if the user takes damage before attacking this turn.', 0, '如果受到对手的招式攻击，
就能给予对手２倍的伤害。', 0, 0, 2, 10, 2),
       (280, 100, 0, 0, 0,
        'Destroys any light screen or reflect on the target’s side of the field, then inflicts regular damage.', null,
        0, 0, 'brick-break', null, null, null, null, '劈瓦', 75, 15, 0, 'Destroys Reflect and Light Screen.', 0, '将手刀猛烈地挥下攻击对手。
还可以破坏光墙和反射壁等。', 0, 0, 2, 10, 2),
       (281, null, 0, 0, 0, 'Puts the target to sleep at the end of the next turn.  Ignores accuracy and evasion modifiers.  If the target leaves the field, this effect is canceled.  If the target has a status effect when this move is used, this move will fail.

If the target is protected by safeguard when this move is used, this move will fail.

insomnia and vital spirit prevent the sleep if the target has either at the end of the next turn, but will not cause this move to fail on use.',
        null, 0, 0, 'yawn', null, 2, null, 2, '哈欠', null, 10, 0, 'Target sleeps at the end of the next turn.', 0, '打个大哈欠引起睡意。
在下一回合让对手陷入睡眠状态。', 14, 1, 1, 10, 1),
       (282, 100, 0, 0, 0, 'Inflicts regular damage.  Target loses its held item.

Neither the user nor the target can recover its item with recycle.

If the target has multitype or sticky hold, it will take damage but not lose its item.', null, 0, 0, 'knock-off', null,
        null, null, null, '拍落', 65, 20, 0, 'Target drops its held item.', 0, '拍落对手的持有物，
直到战斗结束都不能使用。
对手携带道具时会增加伤害。', 0, 0, 2, 10, 17),
       (283, 100, 0, 0, 0,
        'Inflicts exactly enough damage to lower the target’s HP to equal the user’s.  If the target’s HP is not higher than the user’s, this move has no effect.  Type immunity applies, but other type effects are ignored.  This effect counts as damage for moves that respond to damage.',
        null, 0, 0, 'endeavor', null, null, null, null, '蛮干', null, 5, 0,
        'Lowers the target’s HP to equal the user’s.', 0, '给予伤害，
使对手的ＨＰ变得
和自己的ＨＰ一样。', 0, 0, 2, 10, 1),
       (286, null, 0, 0, 0,
        'Prevents any Pokémon on the opposing side of the field from using any move the user knows until the user leaves the field.  This effect is live; if the user obtains new moves while on the field, these moves become restricted.  If no opposing Pokémon knows any of the user’s moves when this move is used, this move will fail.',
        null, 0, 0, 'imprison', null, null, null, null, '封印', null, 10, 0,
        'Prevents the target from using any moves that the user also knows.', 0, '如果对手有和自己相同的招式，
那么只有对手无法使用该招式。', 0, 13, 1, 7, 14),
       (287, null, 0, 0, 0, 'Removes a burn, paralysis, or poison from the user.', null, 0, 0, 'refresh', null, null,
        null, null, '焕然一新', null, 20, 0, 'Cleanses the user of a burn, paralysis, or poison.', 0, '让身体休息，
治愈自己身上所中的
毒、麻痹、灼伤的异常状态。', 0, 13, 1, 7, 1),
       (288, null, 0, 0, 0,
        'If the user faints before it next acts, the move that fainted it will have its PP dropped to 0.  End-of-turn damage does not trigger this effect.',
        null, 0, 0, 'grudge', null, null, null, null, '怨念', null, 5, 0,
        'If the user faints this turn, the PP of the move that fainted it drops to 0.', 0, '因对手的招式而陷入濒死时
给对手施加怨念，
让该招式的ＰＰ变成０。', 0, 13, 1, 7, 8),
       (289, null, 0, 0, 0, 'The next time a Pokémon uses a beneficial move on itself or itself and its ally this turn, the user of this move will steal the move and use it itself.  Moves which may be stolen by this move are identified by the "snatchable" flag.

If two Pokémon use this move on the same turn, the faster Pokémon will steal the first beneficial move, and the slower Pokémon will then steal it again—thus, only the slowest Pokémon using this move ultimately gains a stolen move’s effect.

If the user steals psych up, it will target the Pokémon that used psych up.  If the user was the original target of psych up, and the Pokémon that originally used it’s affected by pressure, it will only lose 1 PP.

This move cannot be copied by mirror move, nor selected by assist or metronome.', null, 0, 0, 'snatch', null, null,
        null, null, '抢夺', null, 10, 4, 'Steals the target’s move, if it’s self-targeted.', 0, '将对手打算使用的回复招式
或能力变化招式夺为己用。', 0, 13, 1, 7, 17),
       (290, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to inflict an effect chosen according to the terrain.

Terrain        | Effect
-------------- | -------------------------------------------------------------
Building       | Paralyzes target
Cave           | Makes target flinch
Deep water     | Lowers target’s Attack by one stage
Desert         | Lowers target’s accuracy by one stage
Grass          | Puts target to sleep
Mountain       | Makes target flinch
Road           | Lowers target’s accuracy by one stage
Shallow water  | Lowers target’s Attack by one stage
Snow           | Freezes target
Tall grass     | Puts target to sleep

In Pokémon Battle Revolution:

Terrain        | Effect
-------------- | -------------------------------------------------------------
Courtyard      | Paralyzes target
Crystal        | Makes target flinch
Gateway        | Lowers target’s Attack by one stage
Magma          | Makes target flinch
Main Street    | Paralyzes target
Neon           | Paralyzes target
Stargazer      | Makes target flinch
Sunny Park     | Puts target to sleep
Sunset         | Lowers target’s accuracy by one stage
Waterfall      | Puts target to sleep
', 30, 0, 0, 'secret-power', null, null, null, null, '秘密之力', 70, 20, 0,
        'Has a $effect_chance% chance to inflict a status effect which depends upon the terrain.', 0, '根据使用场所不同，
该招式的追加效果也会有所变化。', 0, 0, 2, 10, 1),
       (291, 100, 0, 0, 0, 'Inflicts regular damage.  User dives underwater for one turn, becoming immune to attack, and hits on the second turn.

During the immune turn, surf, and whirlpool still hit the user normally, and their power is doubled if appropriate.

The user may be hit during its immune turn if under the effect of lock on, mind reader, or no guard.

This move cannot be selected by sleep talk.', null, 0, 0, 'dive', null, null, null, null, '潜水', 80, 10, 0,
        'User dives underwater, dodging all attacks, and hits next turn.', 0, '第１回合潜入，
第２回合浮上来进行攻击。', 0, 0, 2, 10, 11),
       (292, 100, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'arm-thrust', 5, null, 2, null, '猛推', 15, 20, 0, 'Hits 2-5 times in one turn.', 0, '用张开着的双手
猛推对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 2),
       (293, null, 0, 0, 0, 'User’s type changes according to the terrain.

Terrain        | New type
-------------- | --------------
Building       | normal
Cave           | rock
Desert         | ground
Grass          | grass
Mountain       | rock
Ocean          | water
Pond           | water
Road           | ground
Snow           | ice
Tall grass     | grass

In Pokémon Battle Revolution:

Terrain        | New type
-------------- | --------------
Courtyard      | normal
Crystal        | rock
Gateway        | water
Magma          | rock
Main Street    | normal
Neon           | normal
Stargazer      | rock
Sunny Park     | grass
Sunset         | ground
Waterfall      | grass
', null, 0, 0, 'camouflage', null, null, null, null, '保护色', null, 20, 0, 'User’s type changes to match the terrain.',
        0, '根据所在场所不同，
如水边、草丛和洞窟等，
可以改变自己的属性。', 0, 13, 1, 7, 1),
       (294, null, 0, 0, 0, 'Raises the user’s Special Attack by three stages.', null, 0, 0, 'tail-glow', null, null,
        null, null, '萤火', null, 20, 0, 'Raises the user’s Special Attack by three stages.', 0, '凝视闪烁的光芒，
集中自己的精神，
从而巨幅提高特攻。', 0, 2, 1, 7, 7),
       (295, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        50, 0, 0, 'luster-purge', null, null, null, null, '洁净光芒', 95, 5, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 50, '释放耀眼的光芒进行攻击。
有时会降低对手的特防。', 0, 6, 3, 10, 14),
       (296, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 50,
        0, 0, 'mist-ball', null, null, null, null, '薄雾球', 95, 5, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 50, '用围绕着雾状
羽毛的球进行攻击。
有时会降低对手的特攻。', 0, 6, 3, 10, 14),
       (297, 100, 0, 0, 0, 'Lowers the target’s Attack by two stages.', null, 0, 0, 'feather-dance', null, null, null,
        null, '羽毛舞', null, 15, 0, 'Lowers the target’s Attack by two stages.', 0, '撒出羽毛，
笼罩在对手的周围。
大幅降低对手的攻击。', 0, 2, 1, 10, 3),
       (298, 100, 0, 0, 0, 'Confuses all targets.', null, 0, 0, 'teeter-dance', null, 5, null, 2, '摇晃舞', null, 20, 0,
        'Confuses the target.', 0, '摇摇晃晃地跳起舞蹈，
让自己周围的宝可梦
陷入混乱状态。', 6, 1, 1, 9, 1),
       (339, null, 0, 0, 0, 'Raises the user’s Attack and Defense by one stage each.', null, 0, 0, 'bulk-up', null,
        null, null, null, '健美', null, 20, 0, 'Raises the user’s Attack and Defense by one stage.', 0, '使出全身力气绷紧肌肉，
从而提高自己的攻击和防御。', 0, 2, 1, 7, 2),
       (299, 90, 10, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move. Has a $effect_chance% chance to burn the target.',
        10, 0, 0, 'blaze-kick', null, null, null, null, '火焰踢', 85, 10, 0,
        'Has an increased chance for a critical hit and a $effect_chance% chance to burn the target.', 0, '攻击对手后，
有时会使其陷入灼伤状态。
也容易击中要害。', 4, 4, 2, 10, 10),
       (300, null, 0, 0, 0,
        'electric moves inflict half damage, regardless of target.  If the user leaves the field, this effect ends.',
        null, 0, 0, 'mud-sport', null, null, null, null, '玩泥巴', null, 15, 0, 'Halves all Electric-type damage.', 0, '一旦使用此招式，
周围就会弄得到处是泥。
在５回合内减弱电属性的招式。', 0, 10, 1, 12, 5),
       (301, 90, 0, 0, 0, 'Inflicts regular damage.  User is forced to use this move for five turns.  Power doubles every time this move is used in succession to a maximum of 16x, and resets to normal after the lock-in ends.  If this move misses or becomes unusable, the lock-in ends.

If the user has used defense curl since entering the field, this move has double power.', null, 0, 0, 'ice-ball', null,
        null, null, null, '冰球', 30, 20, 0,
        'Power doubles every turn this move is used in succession after the first, resetting after five turns.', 0, '在５回合内攻击对手。
招式每次击中，威力就会提高。', 0, 0, 2, 10, 15),
       (302, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'needle-arm', null, null, null, null, '尖刺臂', 60, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用带刺的手臂
猛烈地挥舞进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 12),
       (303, null, 0, 0, 0, 'Heals the user for half its max HP.', null, 0, 50, 'slack-off', null, null, null, null,
        '偷懒', null, 5, 0, 'Heals the user by half its max HP.', 0, '偷懒休息。
回复自己最大ＨＰ的一半。
', 0, 3, 1, 7, 1),
       (304, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'hyper-voice', null, null, null, null, '巨声', 90,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '给予对手又吵又响的
巨大震动进行攻击。', 0, 0, 3, 11, 1),
       (305, 100, 50, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to badly poison the target.', 50, 0,
        0, 'poison-fang', null, null, null, null, '剧毒牙', 50, 15, 0,
        'Has a $effect_chance% chance to badly poison the target.', 0, '用有毒的牙齿
咬住对手进行攻击。
有时会使对手中剧毒。', 5, 4, 2, 10, 4),
       (306, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, 0, 0,
        'crush-claw', null, null, null, null, '撕裂爪', 75, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, '用坚硬的锐爪
劈开对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 1),
       (307, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'blast-burn', null, null, null, null, '爆炸烈焰', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '用爆炸的火焰
烧尽对手进行攻击。
下一回合自己将无法动弹。', 0, 0, 3, 10, 10),
       (308, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'hydro-cannon', null, null, null, null, '加农水炮', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '向对手喷射水炮进行攻击。
下一回合自己将无法动弹。', 0, 0, 3, 10, 11),
       (309, 90, 0, 0, 0, 'Inflicts regular damage. Has a $effect_chance% chance to raise the user’s Attack one stage.',
        20, 0, 0, 'meteor-mash', null, null, null, null, '彗星拳', 90, 10, 0,
        'Has a $effect_chance% chance to raise the user’s Attack by one stage.', 20, '使出彗星般的拳头攻击对手。
有时会提高自己的攻击。', 0, 7, 2, 10, 9),
       (310, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'astonish', null, null, null, null, '惊吓', 30, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用尖叫声等
突然惊吓对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 8),
       (311, 100, 0, 0, 0,
        'Inflicts regular damage.  If a weather move is active, this move has double power, and its type becomes the type of the weather move.  shadow sky is typeless for the purposes of this move.',
        null, 0, 0, 'weather-ball', null, null, null, null, '气象球', 50, 10, 0,
        'If there be weather, this move has doubled power and the weather’s type.', 0, '根据使用时的天气，
招式属性和威力会改变。', 0, 0, 3, 10, 1),
       (312, null, 0, 0, 0, 'Removes major status effects and confusion from every Pokémon in the user’s party.', null,
        0, 0, 'aromatherapy', null, null, null, null, '芳香治疗', null, 5, 0,
        'Cures the entire party of major status effects.', 0, '让同伴闻沁人心脾的香气，
从而治愈我方全员的异常状态。', 0, 13, 1, 13, 12),
       (313, 100, 0, 0, 0, 'Lowers the target’s Special Defense by two stages.', null, 0, 0, 'fake-tears', null, null,
        null, null, '假哭', null, 20, 0, 'Lowers the target’s Special Defense by two stages.', 0, '装哭流泪。
使对手不知所措，
从而大幅降低对手的特防。', 0, 2, 1, 10, 17),
       (314, 95, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'air-cutter', null, null, null, null, '空气利刃', 60, 25, 0, 'Has an increased chance for a critical hit.', 0, '用锐利的风
切斩对手进行攻击。
容易击中要害。', 0, 0, 3, 11, 3),
       (315, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Special Attack by two stages.', 100, 0, 0,
        'overheat', null, null, null, null, '过热', 130, 5, 0,
        'Lowers the user’s Special Attack by two stages after inflicting damage.', 100, '使出全部力量攻击对手。
使用之后会因为反作用力，
自己的特攻大幅降低。', 0, 7, 3, 10, 10),
       (316, null, 0, 0, 0,
        'Resets the target’s evasion to normal and prevents any further boosting until the target leaves the field.  A ghost under this effect takes normal damage from normal and fighting moves.  This move itself ignores accuracy and evasion modifiers.',
        null, 0, 0, 'odor-sleuth', null, null, null, null, '气味侦测', null, 40, 0,
        'Forces the target to have no Evade, and allows it to be hit by Normal and Fighting moves even if it’s a Ghost.',
        0, '对幽灵属性宝可梦没有效果的招式
以及闪避率高的对手，
使用后变得能够打中。', 17, 1, 1, 10, 1),
       (317, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, 0, 0,
        'rock-tomb', null, null, null, null, '岩石封锁', 60, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, '投掷岩石进行攻击。
封住对手的行动，
从而降低速度。', 0, 6, 2, 10, 6),
       (338, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'frenzy-plant', null, null, null, null, '疯狂植物', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '用大树摔打对手进行攻击。
下一回合自己将无法动弹。', 0, 0, 3, 10, 12),
       (318, 100, 0, 0, 0,
        'Inflicts regular damage. Has a $effect_chance% chance to raise all of the user’s stats one stage.', 10, 0, 0,
        'silver-wind', null, null, null, null, '银色旋风', 60, 5, 0,
        'Has a $effect_chance% chance to raise all of the user’s stats by one stage.', 10, '在风中掺入鳞粉攻击对手。
有时会提高自己的全部能力。', 0, 7, 3, 10, 7),
       (319, 85, 0, 0, 0, 'Lowers the target’s Special Defense by two stages.', null, 0, 0, 'metal-sound', null, null,
        null, null, '金属音', null, 40, 0, 'Lowers the target’s Special Defense by two stages.', 0, '让对手听摩擦金属般
讨厌的声音。
大幅降低对手的特防。', 0, 2, 1, 10, 9),
       (320, 55, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'grass-whistle', null, 4, null, 2, '草笛', null, 15,
        0, 'Puts the target to sleep.', 0, '让对手听舒适的笛声，
从而陷入睡眠状态。', 2, 1, 1, 10, 12),
       (321, 100, 0, 0, 0, 'Lowers the target’s Attack and Defense by one stage.', null, 0, 0, 'tickle', null, null,
        null, null, '挠痒', null, 20, 0, 'Lowers the target’s Attack and Defense by one stage.', 0, '给对手挠痒，使其发笑，
从而降低对手的攻击和防御。', 0, 2, 1, 10, 1),
       (322, null, 0, 0, 0, 'Raises the user’s Defense and Special Defense by one stage.', null, 0, 0, 'cosmic-power',
        null, null, null, null, '宇宙力量', null, 20, 0, 'Raises the user’s Defense and Special Defense by one stage.',
        0, '汲取宇宙中神秘的力量，
从而提高自己的防御和特防。', 0, 2, 1, 7, 14),
       (323, 100, 0, 0, 0,
        'Inflicts regular damage.  Power increases with the user’s remaining HP and is given by `150 * HP / max HP`, to a maximum of 150 when the user has full HP.',
        null, 0, 0, 'water-spout', null, null, null, null, '喷水', 150, 5, 0,
        'Inflicts more damage when the user has more HP remaining, with a maximum of 150 power.', 0, '掀起潮水进行攻击。
自己的ＨＰ越少，
招式的威力越小。', 0, 0, 3, 11, 11),
       (324, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 10, 0, 0,
        'signal-beam', null, 5, null, 2, '信号光束', 75, 15, 0, 'Has a $effect_chance% chance to confuse the target.',
        0, '发射神奇的光线进行攻击。
有时会使对手混乱。', 6, 4, 3, 10, 7),
       (325, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'shadow-punch', null, null, null, null, '暗影拳', 60, 20, 0, 'Never misses.', 0, '使出混影之拳。
攻击必定会命中。', 0, 0, 2, 10, 8),
       (326, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 10, 10,
        0, 'extrasensory', null, null, null, null, '神通力', 80, 20, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '发出看不见的
神奇力量进行攻击。
有时会使对手畏缩。', 0, 0, 3, 10, 14),
       (327, 90, 0, 0, 0, 'Inflicts regular damage.

This move can hit Pokémon under the effect of bounce, fly, or sky drop.', null, 0, 0, 'sky-uppercut', null, null, null,
        null, '冲天拳', 85, 15, 0, 'Inflicts regular damage and can hit Bounce and Fly users.', 0, '用冲向天空般高高的上勾拳
顶起对手进行攻击。', 0, 0, 2, 10, 2),
       (328, 85, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'sand-tomb', null, 6, null, 5, '流沙地狱', 35, 15, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '将对手困在
铺天盖地的沙暴中，
在４～５回合内进行攻击。', 8, 4, 2, 10, 5),
       (329, 30, 0, 0, 0, 'Inflicts damage equal to the target’s max HP.  Ignores accuracy and evasion modifiers.  This move’s accuracy is 30% plus 1% for each level the user is higher than the target.  If the user is a lower level than the target, this move will fail.

Because this move inflicts a specific and finite amount of damage, endure still prevents the target from fainting.

The effects of lock on, mind reader, and no guard still apply, as long as the user is equal or higher level than the target.  However, they will not give this move a chance to break through detect or protect.',
        null, 0, 0, 'sheer-cold', null, null, null, null, '绝对零度', null, 5, 0, 'Causes a one-hit KO.', 0, '给对手一击濒死。
如果是冰属性以外的宝可梦使用，
就会难以打中。', 0, 9, 3, 10, 15),
       (330, 85, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, 0, 0,
        'muddy-water', null, null, null, null, '浊流', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, '向对手喷射
浑浊的水进行攻击。
有时会降低对手的命中率。', 0, 6, 3, 11, 11),
       (331, 100, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'bullet-seed', 5, null, 2, null, '种子机关枪', 25, 30, 0, 'Hits 2-5 times in one turn.', 0, '向对手猛烈地
发射种子进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 12),
       (332, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'aerial-ace', null, null, null, null, '燕返', 60, 20, 0, 'Never misses.', 0, '以敏捷的动作
戏弄对手后进行切斩。
攻击必定会命中。', 0, 0, 2, 10, 3),
       (333, 100, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'icicle-spear', 5, null, 2, null, '冰锥', 25, 30, 0, 'Hits 2-5 times in one turn.', 0, '向对手发射
锋利的冰柱进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 15),
       (334, null, 0, 0, 0, 'Raises the user’s Defense by two stages.', null, 0, 0, 'iron-defense', null, null, null,
        null, '铁壁', null, 15, 0, 'Raises the user’s Defense by two stages.', 0, '将皮肤变得坚硬如铁，
从而大幅提高自己的防御。', 0, 2, 1, 7, 9),
       (335, null, 0, 0, 0, 'The target cannot switch out normally.  Ignores accuracy and evasion modifiers.  This effect ends when the user leaves the field.

The target may still escape by using baton pass, u turn, or a shed shell.

Both the user and the target pass on this effect with baton pass.', null, 0, 0, 'block', null, null, null, null, '挡路',
        null, 5, 0, 'Prevents the target from leaving battle.', 0, '张开双手进行阻挡，
封住对手的退路，
使其不能逃走。', 0, 13, 1, 10, 1),
       (336, null, 0, 0, 0, 'Raises the user’s Attack by one stage.', null, 0, 0, 'howl', null, null, null, null,
        '长嚎', null, 40, 0, 'Raises the user’s Attack by one stage.', 0, '大声吼叫提高气势，
从而提高自己的攻击。', 0, 2, 1, 13, 1),
       (337, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dragon-claw', null, null, null, null, '龙爪', 80,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '用尖锐的巨爪
劈开对手进行攻击。', 0, 0, 2, 10, 16),
       (340, 85, 30, 0, 0, 'Inflicts regular damage.  User bounces high into the air for one turn, becoming immune to attack, and hits on the second turn.  Has a $effect_chance% chance to paralyze the target.

During the immune turn, gust, hurricane, sky uppercut, smack down, thunder, and twister still hit the user normally.  gust and twister also have double power against the user.

The damage from hail and sandstorm still applies during the immune turn.

The user may be hit during its immune turn if under the effect of lock on, mind reader, or no guard.

This move cannot be used while gravity is in effect.

This move cannot be selected by sleep talk.', 30, 0, 0, 'bounce', null, null, null, null, '弹跳', 85, 5, 0,
        'User bounces high into the air, dodging all attacks, and hits next turn.', 0, '弹跳到高高的空中，
第２回合攻击对手。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 3),
       (341, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, 0, 0,
        'mud-shot', null, null, null, null, '泥巴射击', 55, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, '向对手投掷
泥块进行攻击。
同时降低对手的速度。', 0, 6, 3, 10, 5),
       (342, 100, 10, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move. Has a $effect_chance% chance to poison the target.',
        10, 0, 0, 'poison-tail', null, null, null, null, '毒尾', 50, 25, 0,
        'Has an increased chance for a critical hit and a $effect_chance% chance to poison the target.', 0, '用尾巴拍打。
有时会让对手陷入中毒状态，
也容易击中要害。', 5, 4, 2, 10, 4),
       (343, 100, 0, 0, 0, 'Inflicts regular damage.  If the target is holding an item and the user is not, the user will permanently take the item.  Damage is still inflicted if an item cannot be taken.

Pokémon with sticky hold or multitype are immune to the item theft effect.

The target cannot recover its item with recycle.

This move cannot be selected by assist or metronome.', null, 0, 0, 'covet', null, null, null, null, '渴望', 60, 25, 0,
        'Takes the target’s item.', 0, '一边可爱地撒娇，
一边靠近对手进行攻击，
还能夺取对手携带的道具。', 0, 0, 2, 10, 1),
       (344, 100, 10, 0, -33,
        'Inflicts regular damage.  User takes 1/3 the damage it inflicts in recoil.  Has a $effect_chance% chance to paralyze the target.',
        10, 0, 0, 'volt-tackle', null, null, null, null, '伏特攻击', 120, 15, 0,
        'User takes 1/3 the damage inflicted in recoil.  Has a $effect_chance% chance to paralyze the target.', 0, '让电流覆盖全身猛撞向对手。
自己也会受到不小的伤害。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 13),
       (345, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'magical-leaf', null, null, null, null, '魔法叶', 60, 20, 0, 'Never misses.', 0, '散落可以追踪
对手的神奇叶片。
攻击必定会命中。', 0, 0, 3, 10, 12),
       (346, null, 0, 0, 0,
        'fire moves inflict half damage, regardless of target.  If the user leaves the field, this effect ends.', null,
        0, 0, 'water-sport', null, null, null, null, '玩水', null, 15, 0, 'Halves all Fire-type damage.', 0, '用水湿透周围。
在５回合内
减弱火属性的招式。', 0, 10, 1, 12, 11),
       (347, null, 0, 0, 0, 'Raises the user’s Special Attack and Special Defense by one stage each.', null, 0, 0,
        'calm-mind', null, null, null, null, '冥想', null, 20, 0,
        'Raises the user’s Special Attack and Special Defense by one stage.', 0, '静心凝神，
从而提高自己的特攻和特防。', 0, 2, 1, 7, 14),
       (348, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'leaf-blade', null, null, null, null, '叶刃', 90, 15, 0, 'Has an increased chance for a critical hit.', 0, '像用剑一般操纵叶片
切斩对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 12),
       (349, null, 0, 0, 0, 'Raises the user’s Attack and Speed by one stage each.', null, 0, 0, 'dragon-dance', null,
        null, null, null, '龙之舞', null, 20, 0, 'Raises the user’s Attack and Speed by one stage.', 0, '激烈地跳起神秘
且强有力的舞蹈。
从而提高自己的攻击和速度。', 0, 2, 1, 7, 16),
       (350, 90, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'rock-blast', 5, null, 2, null, '岩石爆击', 25, 10, 0, 'Hits 2-5 times in one turn.', 0, '向对手发射
坚硬的岩石进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 6),
       (351, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'shock-wave', null, null, null, null, '电击波', 60, 20, 0, 'Never misses.', 0, '向对手快速发出电击。
攻击必定会命中。', 0, 0, 3, 10, 13),
       (352, 100, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 20, 0, 0,
        'water-pulse', null, 5, null, 2, '水之波动', 60, 20, 0, 'Has a $effect_chance% chance to confuse the target.',
        0, '用水的震动攻击对手。
有时会使对手混乱。', 6, 4, 3, 10, 11),
       (353, 100, 0, 0, 0, 'Inflicts typeless regular damage at the end of the third turn, starting with this one.  This move cannot score a critical hit.  If the target switches out, its replacement will be hit instead.  Damage is calculated at the time this move is used; stat changes and switching out during the delay won’t change the damage inflicted.  No move with this effect can be used against the same target again until after the end of the third turn.

This effect breaks through wonder guard.

If the target is protected by protect or detect on the turn this move is used, this move will fail.  However, the damage on the third turn will break through protection.

The damage is applied at the end of the turn, so it ignores endure and focus sash.

This move cannot be copied by mirror move.', null, 0, 0, 'doom-desire', null, null, null, null, '破灭之愿', 140, 5, 0,
        'Hits the target two turns later.', 0, '使用招式２回合后，
会用无数道光束攻击对手。', 0, 13, 3, 10, 9),
       (354, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Special Attack by two stages.', 100, 0, 0,
        'psycho-boost', null, null, null, null, '精神突进', 140, 5, 0,
        'Lowers the user’s Special Attack by two stages after inflicting damage.', 100, '使出全部力量攻击对手。
使用之后会因为反作用力，
自己的特攻大幅降低。', 0, 7, 3, 10, 14),
       (355, null, 0, 0, 0,
        'Heals the user for half its max HP.  If the user is flying, its flying type is ignored until the end of this turn.',
        null, 0, 50, 'roost', null, null, null, null, '羽栖', null, 5, 0, 'Heals the user by half its max HP.', 0, '降到地面，使身体休息。
回复自己最大ＨＰ的一半。', 0, 3, 1, 7, 3),
       (370, 100, 0, 0, 0,
        'Inflicts regular damage, then lowers the user’s Defense and Special Defense by one stage each.', 100, 0, 0,
        'close-combat', null, null, null, null, '近身战', 120, 5, 0,
        'Lowers the user’s Defense and Special Defense by one stage after inflicting damage.', 100, '放弃守护，
向对手的怀里突击。
自己的防御和特防会降低。', 0, 7, 2, 10, 2),
       (356, null, 0, 0, 0, 'For five turns (including this one), all immunities to ground moves are disabled.  For the duration of this effect, the evasion of every Pokémon on the field is lowered by two stages.  Cancels the effects of bounce, fly, and sky drop.

Specifically, flying Pokémon and those with levitate or that have used magnet rise are no longer immune to ground attacks, arena trap, spikes, or toxic spikes.

bounce, fly, sky drop, high jump kick, jump kick, and splash cannot be used while this move is in effect.

*Bug*: If this move is used during a double or triple battle while Pokémon are under the effect of sky drop, Sky Drop’s effect is not correctly canceled on its target, and it remains high in the air indefinitely.  As Sky Drop prevents the target from acting, the only way to subsequently remove it from the field is to faint it.',
        null, 0, 0, 'gravity', null, null, null, null, '重力', null, 5, 0,
        'Disables moves and immunities that involve flying or levitating for five turns.', 0, '在５回合内，飘浮特性和飞行属性的
宝可梦会被地面属性的招式击中。
飞向空中的招式也将无法使用。', 0, 10, 1, 12, 14),
       (357, null, 0, 0, 0,
        'Resets the target’s evasion to normal and prevents any further boosting until the target leaves the field.  A dark Pokémon under this effect takes normal damage from psychic moves.  This move itself ignores accuracy and evasion modifiers.',
        null, 0, 0, 'miracle-eye', null, null, null, null, '奇迹之眼', null, 40, 0,
        'Forces the target to have no evasion, and allows it to be hit by Psychic moves even if it’s Dark.', 0, '对恶属性宝可梦没有效果的招式
以及闪避率高的对手，
使用后变得能够打中。', 17, 1, 1, 10, 14),
       (358, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target is sleeping, this move has double power, and the target wakes up.',
        null, 0, 0, 'wake-up-slap', null, null, null, null, '唤醒巴掌', 70, 10, 0,
        'If the target is asleep, has double power and wakes it up.', 0, '给予睡眠状态下的对手较大的伤害。
但相反对手会从睡眠中醒过来。', 0, 0, 2, 10, 2),
       (359, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Speed by one stage.', 100, 0, 0,
        'hammer-arm', null, null, null, null, '臂锤', 100, 10, 0, 'Lowers user’s Speed by one stage.', 100, '挥舞强力而沉重的拳头，
给予对手伤害。
自己的速度会降低。', 0, 7, 2, 10, 2),
       (360, 100, 0, 0, 0,
        'Inflicts regular damage.  Power increases with the target’s current Speed compared to the user, given by `1 + 25 * target Speed / user Speed`, capped at 150.',
        null, 0, 0, 'gyro-ball', null, null, null, null, '陀螺球', null, 5, 0,
        'Power raises when the user has lower Speed, up to a maximum of 150.', 0, '让身体高速旋转并撞击对手。
速度比对手越慢，威力越大。', 0, 0, 2, 10, 9),
       (361, null, 0, 0, 0,
        'User faints.  Its replacement’s HP is fully restored, and any major status effect is removed.  If the replacement Pokémon is immediately fainted by a switch-in effect, the next replacement is healed by this move instead.',
        null, 0, 0, 'healing-wish', null, null, null, null, '治愈之愿', null, 10, 0,
        'User faints.  Its replacement has its HP fully restored and any major status effect removed.', 0, '虽然自己陷入濒死，
但可以治愈后备上场的
宝可梦的异常状态以及回复ＨＰ。', 0, 13, 1, 7, 14),
       (362, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target has less than half its max HP remaining, this move has double power.',
        null, 0, 0, 'brine', null, null, null, null, '盐水', 65, 10, 0,
        'Has double power against Pokémon that have less than half their max HP remaining.', 0, '当对手的ＨＰ
负伤到一半左右时，
招式威力会变成２倍。', 0, 0, 3, 10, 11),
       (363, 100, 0, 0, 0,
        'Inflicts regular damage.  Power and type are determined by the user’s held berry.  The berry is consumed.  If the user is not holding a berry, this move will fail.',
        null, 0, 0, 'natural-gift', null, null, null, null, '自然之恩', null, 15, 0,
        'Power and type depend on the held berry.', 0, '从树果上获得力量进行攻击。
根据携带的树果，
招式属性和威力会改变。', 0, 0, 2, 10, 1),
       (364, 100, 0, 0, 0, 'Inflicts regular damage.  Removes the effects of detect or protect from the target before hitting.

This move cannot be copied by mirror move, nor selected by assist or metronome.', null, 0, 0, 'feint', null, null, null,
        null, '佯攻', 30, 10, 2, 'Hits through Protect and Detect.', 0, '能够攻击正在使用
守住或看穿等招式的对手。
解除其守护效果。', 0, 0, 2, 10, 1),
       (365, 100, 0, 0, 0, 'Inflicts regular damage.  If the target is holding a berry, this move has double power, and the user takes the berry and uses it immediately.

If the target is holding a jaboca berry or rowap berry, the berry is still removed, but has no effect.

If this move is super effective and the target is holding a berry that can reduce this move’s damage, it will do so, and will not be stolen.',
        null, 0, 0, 'pluck', null, null, null, null, '啄食', 60, 20, 0,
        'If target has a berry, inflicts double damage and uses the berry.', 0, '用喙进行攻击。
当对手携带树果时，
可以食用并获得其效果。', 0, 0, 2, 10, 3),
       (366, null, 0, 0, 0,
        'For the next three turns, all Pokémon on the user’s side of the field have their original Speed doubled.  This effect remains if the user leaves the field.',
        null, 0, 0, 'tailwind', null, null, null, null, '顺风', null, 15, 0,
        'For three turns, friendly Pokémon have doubled Speed.', 0, '刮起猛烈的旋风，
在４回合内
提高我方全员的速度。', 0, 11, 1, 4, 3),
       (367, null, 0, 0, 0, 'Raises one of the target’s stats by two stages.  The raised stat is chosen at random from any stats that can be raised by two stages.  If no stat is eligible, this move will fail.

If the target has a substitute, this move will have no effect, even if the user is the target.

This move cannot be copied by mirror move.', null, 0, 0, 'acupressure', null, null, null, null, '点穴', null, 30, 0,
        'Raises one of a friendly Pokémon’s stats at random by two stages.', 0, '通过点穴
让身体舒筋活络。
大幅提高某１项能力。', 0, 13, 1, 5, 1),
       (368, 100, 0, 0, 0,
        'Targets the last opposing Pokémon to hit the user with a damaging move this turn.  Inflicts 1.5× the damage that move did to the user.  If there is no eligible target, this move will fail.  Type immunity applies, but other type effects are ignored.',
        null, 0, 0, 'metal-burst', null, null, null, null, '金属爆炸', null, 10, 0,
        'Strikes back at the last Pokémon to hit the user this turn with 1.5× the damage.', 0, '使出招式前，
将最后受到的招式的伤害
大力返还给对手。', 0, 0, 2, 1, 9),
       (369, 100, 0, 0, 0, 'Inflicts regular damage, then the user immediately switches out, and the trainer selects a replacement Pokémon from the party.  If the target faints from this attack, the user’s trainer selects the new Pokémon to send out first.  If the user is the last Pokémon in its party that can battle, it will not switch out.

The user may be hit by pursuit when it switches out, if it has been targeted and pursuit has not yet been used.

This move may be used even if the user is under the effect of ingrain.  ingrain’s effect will end.', null, 0, 0,
        'u-turn', null, null, null, null, '急速折返', 70, 20, 0, 'User must switch out after attacking.', 0, '在攻击之后急速返回，
和后备宝可梦进行替换。', 0, 0, 2, 10, 7),
       (371, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target uses a move or switches out this turn before this move is used, this move has double power.',
        null, 0, 0, 'payback', null, null, null, null, '以牙还牙', 50, 10, 0,
        'Power is doubled if the target has already moved this turn.', 0, '蓄力攻击。
如果能在对手之后攻击，
招式的威力会变成２倍。', 0, 0, 2, 10, 17),
       (372, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target takes damage this turn for any reason before this move is used, this move has double power.',
        null, 0, 0, 'assurance', null, null, null, null, '恶意追击', 60, 10, 0,
        'Power is doubled if the target has already received damage this turn.', 0, '如果此回合内对手
已经受到伤害的话，
招式威力会变成２倍。', 0, 0, 2, 10, 17),
       (373, 100, 0, 0, 0, 'Target cannot use its held item for five turns.  If the target leaves the field, this effect ends.

If a Pokémon under this effect uses bug bite or pluck on a Pokémon holding a berry, the berry is destroyed but not used.  If a Pokémon under this effect uses fling, it will fail.

This effect is passed by baton pass.', null, 0, 0, 'embargo', null, 5, null, 5, '查封', null, 15, 0,
        'Target cannot use held items.', 0, '让对手在５回合内不能使用
宝可梦携带的道具。训练家也
不能给那只宝可梦使用道具。', 19, 1, 1, 10, 17),
       (374, 100, 0, 0, 0, 'Inflicts regular damage.  Power and type are determined by the user’s held item.  The item is consumed.  If the user is not holding an item, or its item has no set type and power, this move will fail.

This move ignores sticky hold.

If the user is under the effect of embargo, this move will fail.', null, 0, 0, 'fling', null, null, null, null, '投掷',
        null, 10, 0, 'Throws held item at the target; power depends on the item.', 0, '快速投掷携带的道具进行攻击。
根据道具不同，
威力和效果会改变。', 0, 0, 2, 10, 17),
       (375, 100, 0, 0, 0,
        'If the user has a major status effect and the target does not, the user’s status is transferred to the target.',
        null, 0, 0, 'psycho-shift', null, null, null, null, '精神转移', null, 10, 0,
        'Transfers the user’s major status effect to the target.', 0, '利用超能力施以暗示，
从而将自己受到的异常状态
转移给对手。', 0, 13, 1, 10, 14),
       (376, null, 0, 0, 0, 'Inflicts regular damage.  Power is determined by the PP remaining for this move, after its PP cost is deducted.  Ignores accuracy and evasion modifiers.

PP remaining | Power
------------ | ----:
4 or more    |    40
3            |    50
2            |    60
1            |    80
0            |   200

If this move is activated by another move, the activating move’s PP is used to calculate power.', null, 0, 0,
        'trump-card', null, null, null, null, '王牌', null, 5, 0,
        'Power increases when this move has less PP, up to a maximum of 200.', 0, '王牌招式的
剩余ＰＰ越少，
招式的威力越大。', 0, 0, 3, 10, 1),
       (377, 100, 0, 0, 0,
        'For the next five turns, the target may not use any moves that only restore HP, and move effects that heal the target are disabled.  Moves that steal HP may still be used, but will only inflict damage and not heal the target.',
        null, 0, 0, 'heal-block', null, 5, null, 5, '回复封锁', null, 15, 0,
        'Prevents target from restoring its HP for five turns.', 0, '在５回合内
无法通过招式、特性或
携带的道具来回复ＨＰ。', 15, 1, 1, 11, 14),
       (378, 100, 0, 0, 0,
        'Inflicts regular damage.  Power directly relates to the target’s relative remaining HP, given by `1 + 120 * current HP / max HP`, to a maximum of 121.',
        null, 0, 0, 'wring-out', null, null, null, null, '绞紧', null, 5, 0,
        'Power increases against targets with more HP remaining, up to a maximum of 121 power.', 0, '用力勒紧对手进行攻击。
对手的ＨＰ越多，
威力越大。', 0, 0, 3, 10, 1),
       (379, null, 0, 0, 0, 'The user’s original Attack and Defense are swapped.

This effect is passed on by baton pass.', null, 0, 0, 'power-trick', null, null, null, null, '力量戏法', null, 10, 0,
        'User swaps Attack and Defense.', 0, '利用超能力交换
自己的攻击和
防御的力量。', 0, 13, 1, 7, 14),
       (380, 100, 0, 0, 0, 'The target’s ability is disabled as long as it remains on the field.

This effect is passed on by baton pass.', null, 0, 0, 'gastro-acid', null, null, null, null, '胃液', null, 10, 0,
        'Nullifies target’s ability until it leaves battle.', 0, '将胃液吐向对手的身体。
沾上的胃液会消除
对手的特性效果。', 0, 13, 1, 10, 4),
       (381, null, 0, 0, 0, 'For five turns, opposing Pokémon cannot score critical hits.', null, 0, 0, 'lucky-chant',
        null, null, null, null, '幸运咒语', null, 30, 0,
        'Prevents the target from scoring critical hits for five turns.', 0, '向天许愿，
从而在５回合内不会
被对手的攻击打中要害。', 0, 11, 1, 4, 1),
       (382, null, 0, 0, 0, 'If the target has selected a damaging move this turn, the user will copy that move and use it against the target, with a 50% increase in power.

If the target moves before the user, this move will fail.

This move cannot be copied by mirror move, nor selected by assist, metronome, or sleep talk.', null, 0, 0, 'me-first',
        null, null, null, null, '抢先一步', null, 20, 0,
        'Uses the target’s move against it before it attacks, with power increased by half.', 0, '提高威力，抢先使出
对手想要使出的招式。
如果不先使出则会失败。', 0, 0, 1, 2, 1),
       (383, null, 0, 0, 0, 'Uses the last move that was used successfully by any Pokémon, including the user.

This move cannot copy itself, nor roar nor whirlwind.

This move cannot be copied by mirror move, nor selected by assist, metronome, or sleep talk.', null, 0, 0, 'copycat',
        null, null, null, null, '仿效', null, 20, 0, 'Uses the target’s last used move.', 0, '模仿对手刚才使出的招式，
并使出相同招式。
如果对手还没出招则会失败。', 0, 13, 1, 7, 1),
       (384, null, 0, 0, 0, 'User swaps its Attack and Special Attack stat modifiers modifiers with the target.', null,
        0, 0, 'power-swap', null, null, null, null, '力量互换', null, 10, 0,
        'User swaps Attack and Special Attack changes with the target.', 0, '利用超能力互换
自己和对手的攻击
以及特攻的能力变化。', 0, 13, 1, 10, 14),
       (385, null, 0, 0, 0, 'User swaps its Defense and Special Defense modifiers with the target.', null, 0, 0,
        'guard-swap', null, null, null, null, '防守互换', null, 10, 0,
        'User swaps Defense and Special Defense changes with the target.', 0, '利用超能力互换
自己和对手的防御
以及特防的能力变化。', 0, 13, 1, 10, 14),
       (386, 100, 0, 0, 0,
        'Inflicts regular damage.  Power starts at 60 and is increased by 20 for every stage any of the target’s stats has been raised, capping at 200.  Accuracy and evasion modifiers do not increase this move’s power.',
        null, 0, 0, 'punishment', null, null, null, null, '惩罚', null, 5, 0,
        'Power increases against targets with more raised stats, up to a maximum of 200.', 0, '根据能力变化，
对手提高的力量越大，
招式的威力越大。', 0, 0, 2, 10, 17),
       (387, 100, 0, 0, 0,
        'Inflicts regular damage.  This move can only be used if each of the user’s other moves has been used at least once since the user entered the field.  If this is the user’s only move, this move will fail.',
        null, 0, 0, 'last-resort', null, null, null, null, '珍藏', 140, 5, 0,
        'Can only be used after all of the user’s other moves have been used.', 0, '当战斗中已学会的招式
全部使用过后，
才能开始使出珍藏的招式。', 0, 0, 2, 10, 1),
       (388, 100, 0, 0, 0, 'Changes the target’s ability to insomnia.

If the target’s ability is truant or multitype, this move will fail.', null, 0, 0, 'worry-seed', null, null, null, null,
        '烦恼种子', null, 10, 0, 'Changes the target’s ability to Insomnia.', 0, '种植心神不宁的种子。
使对手不能入眠，
并将特性变成不眠。', 0, 13, 1, 10, 12),
       (389, 100, 0, 0, 0, 'Inflicts regular damage.  If the target has not selected a damaging move this turn, or if the target has already acted this turn, this move will fail.

This move is not affected by iron fist.', null, 0, 0, 'sucker-punch', null, null, null, null, '突袭', 70, 5, 1,
        'Only works if the target is about to use a damaging move.', 0, '可以比对手先攻击。
对手使出的招式
如果不是攻击招式则会失败。', 0, 0, 2, 10, 17),
       (390, null, 0, 0, 0, 'Scatters poisoned spikes around the opposing field, which poison opposing Pokémon that enter the field.  A second layer of these spikes may be laid down, in which case Pokémon will be badly poisoned instead.  Pokémon immune to either ground moves or being poisoned are immune to this effect.  Pokémon otherwise immune to ground moves are affected during gravity.

If a poison Pokémon not immune to ground moves enters a field covered with poisoned spikes, the spikes are removed.

rapid spin will remove this effect from its user’s side of the field.  defog will remove this effect from its target’s side of the field.

This move does not trigger synchronize, unless the Pokémon with synchronize was forced to enter the field by another effect such as roar.

Pokémon entering the field due to baton pass are not affected by this effect.', null, 0, 0, 'toxic-spikes', null, null,
        null, null, '毒菱', null, 20, 0, 'Scatters poisoned spikes, poisoning opposing Pokémon that switch in.', 0, '在对手的脚下撒毒菱。
使对手替换出场的宝可梦中毒。', 0, 11, 1, 6, 4),
       (391, null, 0, 0, 0, 'User swaps its stat modifiers with the target.', null, 0, 0, 'heart-swap', null, null,
        null, null, '心灵互换', null, 10, 0, 'User and target swap stat changes.', 0, '利用超能力互换
自己和对手之间的
能力变化。', 0, 13, 1, 10, 14),
       (392, null, 0, 0, 0, 'Restores 1/16 of the user’s max HP at the end of each turn.  If the user leaves the field, this effect ends.

This effect is passed on by baton pass.', null, 0, 0, 'aqua-ring', null, null, null, null, '水流环', null, 20, 0,
        'Restores 1/16 of the user’s max HP each turn.', 0, '在自己身体的周围
覆盖用水制造的幕。
每回合回复ＨＰ。', 0, 13, 1, 7, 11),
       (393, null, 0, 0, 0, 'For five turns, the user is immune to ground moves.

If the user is under the effect of ingrain or has levitate, this move will fail.

This effect is temporarily disabled by and cannot be used during gravity.

This effect is passed on by baton pass.', null, 0, 0, 'magnet-rise', null, null, null, null, '电磁飘浮', null, 10, 0,
        'User is immune to Ground moves and effects for five turns.', 0, '利用电气产生的磁力浮在空中。
在５回合内可以飘浮。', 0, 13, 1, 7, 13),
       (394, 100, 10, 0, -33,
        'Inflicts regular damage.  User takes 1/3 the damage it inflicts in recoil.  Has a $effect_chance% chance to burn the target.  Frozen Pokémon may use this move, in which case they will thaw.',
        10, 0, 0, 'flare-blitz', null, null, null, null, '闪焰冲锋', 120, 15, 0,
        'User takes 1/3 the damage inflicted in recoil.  Has a $effect_chance% chance to burn the target.', 0, '让火焰覆盖全身猛撞向对手。
自己也会受到不小的伤害。
有时会让对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (395, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'force-palm', null, null, null, null, '发劲', 60, 10, 0, 'Has a $effect_chance% chance to paralyze the target.',
        0, '向对手的身体
发出冲击波进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 2),
       (396, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'aura-sphere', null, null, null, null, '波导弹', 80, 20, 0, 'Never misses.', 0, '从体内产生出波导之力，
然后向对手发出。
攻击必定会命中。', 0, 0, 3, 10, 2),
       (397, null, 0, 0, 0, 'Raises the user’s Speed by two stages.', null, 0, 0, 'rock-polish', null, null, null, null,
        '岩石打磨', null, 20, 0, 'Raises the user’s Speed by two stages.', 0, '打磨自己的身体，
减少空气阻力。
可以大幅提高自己的速度。', 0, 2, 1, 7, 6),
       (398, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 30, 0, 0,
        'poison-jab', null, null, null, null, '毒击', 80, 20, 0, 'Has a $effect_chance% chance to poison the target.',
        0, '用带毒的触手或手臂刺入对手。
有时会让对手陷入中毒状态。', 5, 4, 2, 10, 4),
       (399, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 20, 20,
        0, 'dark-pulse', null, null, null, null, '恶之波动', 80, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '从体内发出
充满恶意的恐怖气场。
有时会使对手畏缩。', 0, 0, 3, 10, 17),
       (400, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'night-slash', null, null, null, null, '暗袭要害', 70, 15, 0, 'Has an increased chance for a critical hit.', 0, '抓住瞬间的空隙
切斩对手。
容易击中要害。', 0, 0, 2, 10, 17),
       (401, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'aqua-tail', null, null, null, null, '水流尾', 90, 10,
        0, 'Inflicts regular damage with no additional effect.', 0, '如惊涛骇浪般挥动
大尾巴攻击对手。', 0, 0, 2, 10, 11),
       (402, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'seed-bomb', null, null, null, null, '种子炸弹', 80,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '将外壳坚硬的大种子，
从上方砸下攻击对手。', 0, 0, 2, 10, 12),
       (403, 95, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'air-slash', null, null, null, null, '空气斩', 75, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用连天空也能劈开的
空气之刃进行攻击。
有时会使对手畏缩。', 0, 0, 3, 10, 3),
       (404, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'x-scissor', null, null, null, null, '十字剪', 80,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '将镰刀或爪子像剪刀般地交叉，
顺势劈开对手。', 0, 0, 2, 10, 7),
       (405, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'bug-buzz', null, null, null, null, '虫鸣', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '利用振动发出音波进行攻击。
有时会降低对手的特防。', 0, 6, 3, 10, 7),
       (406, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dragon-pulse', null, null, null, null, '龙之波动',
        85, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '从大大的口中
掀起冲击波攻击对手。', 0, 0, 3, 10, 16),
       (872, 100, null, null, null, null, null, null, null, 'aqua-step', null, null, null, null, '流水旋舞', 80, 10, 0,
        null, null,
        'The user toys with the target and attacks it using light and fluid dance steps. This also boosts the user''s Speed stat.',
        null, null, 2, 10, 11),
       (407, 75, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 20, 20,
        0, 'dragon-rush', null, null, null, null, '龙之俯冲', 100, 10, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '释放出骇人的杀气，
一边威慑一边撞击对手。
有时会使对手畏缩。', 0, 0, 2, 10, 16),
       (408, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'power-gem', null, null, null, null, '力量宝石', 80,
        20, 0, 'Inflicts regular damage with no additional effect.', 0, '发射如宝石般
闪耀的光芒攻击对手。', 0, 0, 3, 10, 6),
       (409, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'drain-punch', null, null, null, null, '吸取拳', 75, 10, 0,
        'Drains half the damage inflicted to heal the user.', 0, '用拳头吸取对手的力量。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 2, 10, 2),
       (410, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'vacuum-wave', null, null, null, null, '真空波', 40,
        30, 1, 'Inflicts regular damage with no additional effect.', 0, '挥动拳头，
掀起真空波。
必定能够先制攻击。', 0, 0, 3, 10, 2),
       (411, 70, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'focus-blast', null, null, null, null, '真气弹', 120, 5, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '提高气势，
释放出全部力量。
有时会降低对手的特防。', 0, 6, 3, 10, 2),
       (412, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'energy-ball', null, null, null, null, '能量球', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '发射从自然收集的生命力量。
有时会降低对手的特防。', 0, 6, 3, 10, 12),
       (413, 100, 0, 0, -33, 'Inflicts regular damage.  User takes 1/3 the damage it inflicts in recoil.', null, 0, 0,
        'brave-bird', null, null, null, null, '勇鸟猛攻', 120, 15, 0,
        'User receives 1/3 the damage inflicted in recoil.', 0, '收拢翅膀，
通过低空飞行突击对手。
自己也会受到不小的伤害。', 0, 0, 2, 10, 3),
       (414, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'earth-power', null, null, null, null, '大地之力', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '向对手脚下
释放出大地之力。
有时会降低对手的特防。', 0, 6, 3, 10, 5),
       (415, 100, 0, 0, 0, 'User and target permanently swap held items.  Works even if one of the Pokémon isn’t holding anything.  If either Pokémon is holding mail, this move will fail.

If either Pokémon has multitype or sticky hold, this move will fail.

If this move results in a Pokémon obtaining choice band, choice scarf, or choice specs, and that Pokémon was the latter of the pair to move this turn, then the move it used this turn becomes its chosen forced move.  This applies even if both Pokémon had a choice item before this move was used.  If the first of the two Pokémon gains a choice item, it may select whatever choice move it wishes next turn.

Neither the user nor the target can recover its item with recycle.

This move cannot be selected by assist or metronome.', null, 0, 0, 'switcheroo', null, null, null, null, '掉包', null,
        10, 0, 'User and target swap items.', 0, '用一闪而过的速度
交换自己和对手的持有物。', 0, 13, 1, 10, 17),
       (416, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'giga-impact', null, null, null, null, '终极冲击', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '使出自己浑身力量突击对手。
下一回合自己将无法动弹。', 0, 0, 2, 10, 1),
       (417, null, 0, 0, 0, 'Raises the user’s Special Attack by two stages.', null, 0, 0, 'nasty-plot', null, null,
        null, null, '诡计', null, 20, 0, 'Raises the user’s Special Attack by two stages.', 0, '谋划诡计，激活头脑。
大幅提高自己的特攻。', 0, 2, 1, 7, 17),
       (418, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'bullet-punch', null, null, null, null, '子弹拳', 40,
        30, 1, 'Inflicts regular damage with no additional effect.', 0, '向对手使出如子弹般
快速而坚硬的拳头。
必定能够先制攻击。', 0, 0, 2, 10, 9),
       (419, 100, 0, 0, 0, 'Inflicts regular damage.  If the target damaged the user this turn and was the last to do so, this move has double power.

pain split does not count as damaging the user.', null, 0, 0, 'avalanche', null, null, null, null, '雪崩', 60, 10, -4,
        'Inflicts double damage if the user takes damage before attacking this turn.', 0, '如果受到对手的招式攻击，
就能给予该对手
２倍威力的攻击。', 0, 0, 2, 10, 15),
       (420, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'ice-shard', null, null, null, null, '冰砾', 40, 30,
        1, 'Inflicts regular damage with no additional effect.', 0, '瞬间制作冰块，
快速地扔向对手。
必定能够先制攻击。', 0, 0, 2, 10, 15),
       (421, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'shadow-claw', null, null, null, null, '暗影爪', 70, 15, 0, 'Has an increased chance for a critical hit.', 0, '以影子做成的锐爪，
劈开对手。
容易击中要害。', 0, 0, 2, 10, 8),
       (422, 95, 10, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target and a separate $effect_chance% chance to make the target flinch.',
        10, 10, 0, 'thunder-fang', null, null, null, null, '雷电牙', 65, 15, 0,
        'Has a $effect_chance% chance to paralyze the target and a $effect_chance% chance to make the target flinch.',
        0, '用蓄满电流的牙齿咬住对手。
有时会使对手畏缩
或陷入麻痹状态。', 1, 4, 2, 10, 13),
       (423, 95, 10, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target and a separate $effect_chance% chance to make the target flinch.',
        10, 10, 0, 'ice-fang', null, null, null, null, '冰冻牙', 65, 15, 0,
        'Has a $effect_chance% chance to freeze the target and a $effect_chance% chance to make the target flinch.', 0, '用藏有冷气的牙齿咬住对手。
有时会使对手畏缩
或陷入冰冻状态。', 3, 4, 2, 10, 15),
       (424, 95, 10, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to burn the target and a separate $effect_chance% chance to make the target flinch.',
        10, 10, 0, 'fire-fang', null, null, null, null, '火焰牙', 65, 15, 0,
        'Has a $effect_chance% chance to burn the target and a $effect_chance% chance to make the target flinch.', 0, '用覆盖着火焰的牙齿咬住对手。
有时会使对手畏缩
或陷入灼伤状态。', 4, 4, 2, 10, 10),
       (425, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'shadow-sneak', null, null, null, null, '影子偷袭',
        40, 30, 1, 'Inflicts regular damage with no additional effect.', 0, '伸长影子，
从对手的背后进行攻击。
必定能够先制攻击。', 0, 0, 2, 10, 8),
       (426, 85, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, 0, 0,
        'mud-bomb', null, null, null, null, '泥巴炸弹', 65, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, '向对手发射
坚硬的泥弹进行攻击。
有时会降低对手的命中率。', 0, 6, 3, 10, 5),
       (427, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'psycho-cut', null, null, null, null, '精神利刃', 70, 20, 0, 'Has an increased chance for a critical hit.', 0, '用实体化的
心之利刃劈开对手。
容易击中要害。', 0, 0, 2, 10, 14),
       (428, 90, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 20, 20,
        0, 'zen-headbutt', null, null, null, null, '意念头锤', 80, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '将思念的力量集中在
前额进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 14),
       (429, 85, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, 0, 0,
        'mirror-shot', null, null, null, null, '镜光射击', 65, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 30, '抛光自己的身体，
向对手释放出闪光之力。
有时会降低对手的命中率。', 0, 6, 3, 10, 9),
       (430, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by one stage.',
        10, 0, 0, 'flash-cannon', null, null, null, null, '加农光炮', 80, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by one stage.', 10, '将身体的光芒
聚集在一点释放出去。
有时会降低对手的特防。', 0, 6, 3, 10, 9),
       (431, 85, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 20, 0, 0,
        'rock-climb', null, 5, null, 2, '攀岩', 90, 20, 0, 'Has a $effect_chance% chance to confuse the target.', 0, '用尽全力扑向对手进行攻击。
有时会使对手混乱。', 6, 4, 2, 10, 1),
       (432, null, 0, 0, 0, 'Lowers the target’s evasion by one stage.  Clears away fog.  Removes the effects of mist, light screen, reflect, safeguard, spikes, stealth rock, and toxic spikes from the target’s side of the field.

If the target is protected by mist, it will prevent the evasion change, then be removed by this move.', null, 0, 0,
        'defog', null, null, null, null, '清除浓雾', null, 15, 0,
        'Lowers the target’s evasion by one stage.  Removes field effects from the enemy field.', 0, '用强风吹开对手的
反射壁或光墙等。
也会降低对手的闪避率。', 0, 13, 1, 10, 3),
       (433, null, 0, 0, 0, 'For five turns (including this one), slower Pokémon will act before faster Pokémon.  Move priority is not affected.  Using this move when its effect is already active will end the effect.

Pokémon holding full incense, lagging tail, or quick claw and Pokémon with stall ignore this effect.', null, 0, 0,
        'trick-room', null, null, null, null, '戏法空间', null, 5, -7,
        'For five turns, slower Pokémon will act before faster Pokémon.', 0, '制造出离奇的空间。
在５回合内
速度慢的宝可梦可以先行动。', 0, 10, 1, 12, 14),
       (434, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Special Attack by two stages.', 100, 0, 0,
        'draco-meteor', null, null, null, null, '流星群', 130, 5, 0,
        'Lowers the user’s Special Attack by two stages after inflicting damage.', 100, '从天空中向对手落下陨石。
使用之后因为反作用力，
自己的特攻会大幅降低。', 0, 7, 3, 10, 16),
       (435, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 30, 0, 0,
        'discharge', null, null, null, null, '放电', 80, 15, 0, 'Has a $effect_chance% chance to paralyze the target.',
        0, '用耀眼的电击
攻击自己周围所有的宝可梦。
有时会陷入麻痹状态。', 1, 4, 3, 9, 13),
       (436, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 30, 0, 0,
        'lava-plume', null, null, null, null, '喷烟', 80, 15, 0, 'Has a $effect_chance% chance to burn the target.', 0, '用熊熊烈火
攻击自己周围所有的宝可梦。
有时会陷入灼伤状态。', 4, 4, 3, 9, 10),
       (437, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Special Attack by two stages.', 100, 0, 0,
        'leaf-storm', null, null, null, null, '飞叶风暴', 130, 5, 0,
        'Lowers the user’s Special Attack by two stages after inflicting damage.', 100, '用尖尖的叶片向对手卷起风暴。
使用之后因为反作用力
自己的特攻会大幅降低。', 0, 7, 3, 10, 12),
       (438, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'power-whip', null, null, null, null, '强力鞭打', 120,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '激烈地挥舞青藤或触手
摔打对手进行攻击。', 0, 0, 2, 10, 12),
       (439, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'rock-wrecker', null, null, null, null, '岩石炮', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '向对手发射
巨大的岩石进行攻击。
下一回合自己将无法动弹。', 0, 0, 2, 10, 6),
       (440, 100, 10, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move. Has a $effect_chance% chance to poison the target.',
        10, 0, 0, 'cross-poison', null, null, null, null, '十字毒刃', 70, 20, 0,
        'Has an increased chance for a critical hit and a $effect_chance% chance to poison the target.', 0, '用毒刃劈开对手。
有时会让对手陷入中毒状态，
也容易击中要害。', 5, 4, 2, 10, 4),
       (441, 80, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 30, 0, 0,
        'gunk-shot', null, null, null, null, '垃圾射击', 120, 5, 0,
        'Has a $effect_chance% chance to poison the target.', 0, '用肮脏的垃圾
撞向对手进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 2, 10, 4),
       (442, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'iron-head', null, null, null, null, '铁头', 80, 15, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用钢铁般
坚硬的头部进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 9),
       (443, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'magnet-bomb', null, null, null, null, '磁铁炸弹', 60, 20, 0, 'Never misses.', 0, '发射吸住对手的
钢铁炸弹。
攻击必定会命中。', 0, 0, 2, 10, 9),
       (444, 80, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'stone-edge', null, null, null, null, '尖石攻击', 100, 5, 0, 'Has an increased chance for a critical hit.', 0, '用尖尖的岩石
刺入对手进行攻击。
容易击中要害。', 0, 0, 2, 10, 6),
       (513, null, 0, 0, 0, 'User’s type changes to match the target’s.', null, 0, 0, 'reflect-type', null, null, null,
        null, '镜面属性', null, 15, 0, 'User becomes the target’s type.', 0, '反射对手的属性，
让自己也变成一样的属性。', 0, 13, 1, 10, 1),
       (445, 100, 0, 0, 0,
        'Lowers the target’s Special Attack by two stages.  If the user and target are the same gender, or either is genderless, this move will fail.',
        null, 0, 0, 'captivate', null, null, null, null, '诱惑', null, 20, 0,
        'Lowers the target’s Special Attack by two stages if it’s the opposite gender.', 0, '♂诱惑♀或♀诱惑♂，
从而大幅降低对手的特攻。', 0, 2, 1, 11, 1),
       (446, null, 0, 0, 0, 'Spreads sharp rocks around the opposing field, damaging any Pokémon that enters the field for 1/8 its max HP.  This damage is affected by the entering Pokémon’s susceptibility to rock moves.

rapid spin removes this effect from its user’s side of the field.', null, 0, 0, 'stealth-rock', null, null, null, null,
        '隐形岩', null, 20, 0, 'Causes damage when opposing Pokémon switch in.', 0, '将无数岩石悬浮在对手的周围，
从而对替换出场的对手的
宝可梦给予伤害。', 0, 11, 1, 6, 6),
       (447, 100, 0, 0, 0, 'Inflicts regular damage.  Power increases with the target’s weight in kilograms, to a maximum of 120.

Target’s weight | Power
--------------- | ----:
Up to 10kg      |    20
Up to 25kg      |    40
Up to 50kg      |    60
Up to 100kg     |    80
Up to 200kg     |   100
Above 200kg     |   120
', null, 0, 0, 'grass-knot', null, null, null, null, '打草结', null, 20, 0,
        'Inflicts more damage to heavier targets, with a maximum of 120 power.', 0, '用草缠住并绊倒对手。
对手越重，威力越大。', 0, 0, 3, 10, 12),
       (448, 100, 100, 0, 0, 'Inflicts regular damage.  Has either a 1%, 11%, or 31% chance to confuse the target, based on the volume of the recording made for this move; louder recordings increase the chance of confusion.  If the user is not a chatot, this move will not cause confusion.

This move cannot be copied by mimic, mirror move, or sketch, nor selected by assist, metronome, or sleep talk.', 100, 0,
        0, 'chatter', null, 5, null, 2, '喋喋不休', 65, 20, 0,
        'Has a higher chance to confuse the target when the recorded sound is louder.', 0, '用非常烦人的，
喋喋不休的音波攻击对手。
使对手混乱。', 6, 4, 3, 10, 3),
       (449, 100, 0, 0, 0, 'Inflicts regular damage.  If the user is holding a plate or a drive, this move’s type is the type corresponding to that item.

Note: This effect is technically shared by both techno blast and judgment; however, Techno Blast is only affected by drives, and Judgment is only affected by plates.',
        null, 0, 0, 'judgment', null, null, null, null, '制裁光砾', 100, 10, 0,
        'If the user is holding a appropriate plate or drive, the damage inflicted will match it.', 0, '向对手放出无数的光弹。
属性会根据自己
携带的石板不同而改变。', 0, 0, 3, 10, 1),
       (450, 100, 0, 0, 0, 'Inflicts regular damage.  If the target is holding a berry, this move has double power, and the user takes the berry and uses it immediately.

If the target is holding a jaboca berry or rowap berry, the berry is still removed, but has no effect.

If this move is super effective and the target is holding a berry that can reduce this move’s damage, it will do so, and will not be stolen.',
        null, 0, 0, 'bug-bite', null, null, null, null, '虫咬', 60, 20, 0,
        'If target has a berry, inflicts double damage and uses the berry.', 0, '咬住进行攻击。
当对手携带树果时，
可以食用并获得其效果。', 0, 0, 2, 10, 7),
       (451, 90, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to raise the user’s Special Attack by one stage.', 70,
        0, 0, 'charge-beam', null, null, null, null, '充电光束', 50, 10, 0,
        'Has a $effect_chance% chance to raise the user’s Special Attack by one stage.', 70, '向对手发射电击光束。
由于蓄满电流，
有时会提高自己的特攻。', 0, 7, 3, 10, 13),
       (452, 100, 0, 0, -33, 'Inflicts regular damage.  User takes 1/3 the damage it inflicts in recoil.', null, 0, 0,
        'wood-hammer', null, null, null, null, '木槌', 120, 15, 0, 'User receives 1/3 the damage inflicted in recoil.',
        0, '用坚硬的躯体
撞击对手进行攻击。
自己也会受到不小的伤害。', 0, 0, 2, 10, 12),
       (453, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'aqua-jet', null, null, null, null, '水流喷射', 40,
        20, 1, 'Inflicts regular damage with no additional effect.', 0, '以迅雷不及掩耳之势
扑向对手。
必定能够先制攻击。', 0, 0, 2, 10, 11),
       (454, 100, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'attack-order', null, null, null, null, '攻击指令', 90, 15, 0, 'Has an increased chance for a critical hit.', 0, '召唤手下，
让其朝对手发起攻击。
容易击中要害。', 0, 0, 2, 10, 7),
       (455, null, 0, 0, 0, 'Raises the user’s Defense and Special Defense by one stage.', null, 0, 0, 'defend-order',
        null, null, null, null, '防御指令', null, 10, 0, 'Raises the user’s Defense and Special Defense by one stage.',
        0, '召唤手下，
让其附在自己的身体上。
可以提高自己的防御和特防。', 0, 2, 1, 7, 7),
       (456, null, 0, 0, 0, 'Heals the user for half its max HP.', null, 0, 50, 'heal-order', null, null, null, null,
        '回复指令', null, 10, 0, 'Heals the user by half its max HP.', 0, '召唤手下疗伤。
回复自己最大ＨＰ的一半。', 0, 3, 1, 7, 7),
       (457, 80, 0, 0, -50, 'Inflicts regular damage.  User takes 1/2 the damage it inflicts in recoil.', null, 0, 0,
        'head-smash', null, null, null, null, '双刃头锤', 150, 5, 0,
        'User receives 1/2 the damage inflicted in recoil.', 0, '拼命使出浑身力气，
向对手进行头锤攻击。
自己也会受到非常大的伤害。', 0, 0, 2, 10, 6),
       (458, 90, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', null, 0, 0, 'double-hit', 2, null, 2,
        null, '二连击', 35, 10, 0, 'Hits twice in one turn.', 0, '使用尾巴等
拍打对手进行攻击。
连续２次给予伤害。', 0, 0, 2, 10, 1),
       (459, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'roar-of-time', null, null, null, null, '时光咆哮', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '释放出扭曲时间般的
强大力量攻击对手。
下一回合自己将无法动弹。', 0, 0, 3, 10, 16),
       (460, 95, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'spacial-rend', null, null, null, null, '亚空裂斩', 100, 5, 0, 'Has an increased chance for a critical hit.', 0, '将对手连同周围的空间一起
撕裂并给予伤害。
容易击中要害。', 0, 0, 3, 10, 16),
       (461, null, 0, 0, 0,
        'User faints.  Its replacement’s HP and PP are fully restored, and any major status effect is removed.', null,
        0, 0, 'lunar-dance', null, null, null, null, '新月舞', null, 10, 0,
        'User faints, and its replacement is fully healed.', 0, '虽然自己陷入濒死，
但可以治愈后备上场的
宝可梦的全部状态。', 0, 13, 1, 7, 14),
       (462, 100, 0, 0, 0,
        'Inflicts regular damage.  Power directly relates to the target’s relative remaining HP, given by `1 + 120 * current HP / max HP`, to a maximum of 121.',
        null, 0, 0, 'crush-grip', null, null, null, null, '捏碎', null, 5, 0,
        'Power increases against targets with more HP remaining, up to a maximum of 121 power.', 0, '用骇人的力量捏碎对手。
对手的ＨＰ越多，
威力越大。', 0, 0, 2, 10, 1),
       (463, 75, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'magma-storm', null, 6, null, 5, '熔岩风暴', 100, 5, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '将对手困在
熊熊燃烧的火焰中，
在４～５回合内进行攻击。', 8, 4, 3, 10, 10),
       (464, 50, 0, 0, 0, 'Puts the target to sleep.', null, 0, 0, 'dark-void', null, 4, null, 2, '暗黑洞', null, 10, 0,
        'Puts the target to sleep.', 0, '将对手强制拖入黑暗的世界，
从而让对手陷入睡眠状态。', 2, 1, 1, 11, 17),
       (465, 85, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Defense by two stages.',
        40, 0, 0, 'seed-flare', null, null, null, null, '种子闪光', 120, 5, 0,
        'Has a $effect_chance% chance to lower the target’s Special Defense by two stages.', 40, '从身体里产生冲击波。
有时会大幅降低对手的特防。', 0, 6, 3, 10, 12),
       (466, 100, 10, 0, 0,
        'Inflicts regular damage. Has a $effect_chance% chance to raise all of the user’s stats one stage.', 10, 0, 0,
        'ominous-wind', null, null, null, null, '奇异之风', 60, 5, 0,
        'Has a $effect_chance% chance to raise all of the user’s stats by one stage.', 10, '突然刮起毛骨悚然的暴风
攻击对手。有时会提高
自己的全部能力。', 0, 7, 3, 10, 8),
       (467, 100, 0, 0, 0, 'Inflicts regular damage.  User vanishes for one turn, becoming immune to attack, and hits on the second turn.

This move ignores the effects of detect and protect.

This move cannot be selected by sleep talk.', null, 0, 0, 'shadow-force', null, null, null, null, '暗影潜袭', 120, 5, 0,
        'User vanishes, dodging all attacks, and hits next turn.  Hits through Protect and Detect.', 0, '第１回合消失踪影，
第２回合攻击对手。
即使对手正受保护，也能击中。', 0, 0, 2, 10, 8),
       (468, null, 0, 0, 0, 'Raises the user’s Attack and accuracy by one stage.', null, 0, 0, 'hone-claws', null, null,
        null, null, '磨爪', null, 15, 0, 'Raises the user’s Attack and accuracy by one stage.', 0, '将爪子磨得更加锋利。
提高自己的攻击和命中率。', 0, 2, 1, 7, 17),
       (469, null, 0, 0, 0, 'Moves with multiple targets will not hit friendly Pokémon for the remainder of this turn.  If the user is last to act this turn, this move will fail.

This move cannot be selected by assist or metronome.', null, 0, 0, 'wide-guard', null, null, null, null, '广域防守',
        null, 10, 3, 'Prevents any multi-target moves from hitting friendly Pokémon this turn.', 0, '在１回合内防住
击打我方全员的攻击。', 0, 11, 1, 4, 6),
       (470, null, 0, 0, 0,
        'Averages the user’s unmodified Defense with the target’s unmodified Defense; the value becomes the unmodified Defense for both Pokémon. Unmodified Special Defense is averaged the same way.',
        null, 0, 0, 'guard-split', null, null, null, null, '防守平分', null, 10, 0,
        'Averages Defense and Special Defense with the target.', 0, '利用超能力将自己和对手的
防御和特防相加，
再进行平分。', 0, 13, 1, 10, 14),
       (471, null, 0, 0, 0, 'Averages the user’s unmodified Attack with the target’s unmodified Attack; the value becomes the unmodified Attack for both Pokémon. Unmodified Special Attack is averaged the same way.

This effect applies before any other persistent changes to unmodified Attack or Special Attack, such as flower gift during sunny day.',
        null, 0, 0, 'power-split', null, null, null, null, '力量平分', null, 10, 0,
        'Averages Attack and Special Attack with the target.', 0, '利用超能力将自己和对手的
攻击和特攻相加，
再进行平分。', 0, 13, 1, 10, 14),
       (472, null, 0, 0, 0,
        'For five turns (including this one), every Pokémon’s Defense and Special Defense are swapped.', null, 0, 0,
        'wonder-room', null, null, null, null, '奇妙空间', null, 10, 0,
        'All Pokémon’s Defense and Special Defense are swapped for 5 turns.', 0, '制造出离奇的空间。
在５回合内互换
所有宝可梦的防御和特防。', 0, 10, 1, 12, 14),
       (473, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation always uses the target’s Defense, regardless of this move’s damage class.',
        null, 0, 0, 'psyshock', null, null, null, null, '精神冲击', 80, 10, 0,
        'Inflicts damage based on the target’s Defense, not Special Defense.', 0, '将神奇的念波实体化攻击对手。
给予物理伤害。', 0, 0, 3, 10, 14),
       (474, 100, 0, 0, 0, 'Inflicts regular damage.  If the target is poisoned, this move has double power.', null, 0,
        0, 'venoshock', null, null, null, null, '毒液冲击', 65, 10, 0,
        'Inflicts double damage if the target is Poisoned.', 0, '将特殊的毒液泼向对手。
对处于中毒状态的对手，
威力会变成２倍。', 0, 0, 3, 10, 4),
       (475, null, 0, 0, 0,
        'Raises the user’s Speed by two stages.  Halves the user’s weight; this effect does not stack.', null, 0, 0,
        'autotomize', null, null, null, null, '身体轻量化', null, 15, 0,
        'Raises the user’s Speed by two stages and halves the user’s weight.', 0, '削掉身体上没用的部分。
大幅提高自己的速度，
同时体重也会变轻。', 0, 2, 1, 7, 9),
       (476, null, 0, 0, 0, 'Until the end of this turn, any moves that opposing Pokémon target solely at the user’s ally will instead target the user.  If both Pokémon on the same side of the field use this move on the same turn, the Pokémon that uses it last will become the target.

This effect takes priority over lightning rod and storm drain.

If the user’s ally switches out, opposing Pokémon may still hit it with pursuit.

This move cannot be selected by assist or metronome.', null, 0, 0, 'rage-powder', null, null, null, null, '愤怒粉',
        null, 20, 2, 'Redirects the target’s single-target effects to the user for this turn.', 0, '将令人烦躁的粉末撒在自己身上，
用以吸引对手的注意。
使对手的攻击全部指向自己。', 0, 13, 1, 7, 7),
       (477, null, 0, 0, 0, 'For three turns (including this one), moves used against the target have 100% accuracy, but the target is immune to ground damage.  Accuracy of one-hit KO moves is exempt from this effect.

This effect is removed by gravity.  If Gravity is already in effect, this move will fail.', null, 0, 0, 'telekinesis',
        null, 3, null, 3, '意念移物', null, 15, 0, 'Moves have 100% accuracy against the target for three turns.', 0, '利用超能力使对手浮起来。
在３回合内
攻击会变得容易打中对手。', -1, 1, 1, 10, 14),
       (478, null, 0, 0, 0,
        'For five turns (including this one), passive effects of held items are ignored, and Pokémon will not use their held items.',
        null, 0, 0, 'magic-room', null, null, null, null, '魔法空间', null, 10, 0, 'Negates held items for five turns.',
        0, '制造出离奇的空间。
在５回合内所有宝可梦
携带道具的效果都会消失。', 0, 10, 1, 12, 14),
       (514, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon fainted on the previous turn, this move has double power.',
        null, 0, 0, 'retaliate', null, null, null, null, '报仇', 70, 5, 0,
        'Has double power if a friendly Pokémon fainted last turn.', 0, '为倒下的同伴报仇。
如果上一回合有同伴倒下，
威力就会提高。', 0, 0, 2, 10, 1),
       (479, 100, 100, 0, 0, 'Inflicts regular damage.  Removes the target’s immunity to ground-type damage.  This effect removes any existing Ground immunity due to levitate, magnet rise, or telekinesis, and causes the target’s flying type to be ignored when it takes Ground damage.

If the target isn’t immune to Ground damage, this move will fail.

This move can hit Pokémon under the effect of bounce, fly, or sky drop, and ends the effect of Bounce or Fly.', 100, 0,
        0, 'smack-down', null, null, null, null, '击落', 50, 15, 0, 'Removes any immunity to Ground damage.', 0, '扔石头或炮弹，
攻击飞行的对手。
对手会被击落，掉到地面。', -1, 0, 2, 10, 6),
       (480, 100, 0, 6, 0, 'Inflicts regular damage.  Always scores a critical hit.', null, 0, 0, 'storm-throw', null,
        null, null, null, '山岚摔', 60, 10, 0, 'Always scores a critical hit.', 0, '向对手使出强烈的一击。
攻击必定会击中要害。', 0, 0, 2, 10, 2),
       (481, 100, 0, 0, 0,
        'Inflicts regular damage.  If this move successfully hits the target, any Pokémon adjacent to the target are damaged for 1/16 their max HP.',
        null, 0, 0, 'flame-burst', null, null, null, null, '烈焰溅射', 70, 15, 0,
        'Deals splash damage to Pokémon next to the target.', 0, '如果击中，爆裂的火焰会
攻击到对手。爆裂出的火焰
还会飞溅到旁边的对手。', 0, 0, 3, 10, 10),
       (482, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to poison the target.', 10, 0, 0,
        'sludge-wave', null, null, null, null, '污泥波', 95, 10, 0,
        'Has a $effect_chance% chance to poison the target.', 0, '用污泥波攻击
自己周围所有的宝可梦。
有时会陷入中毒状态。', 5, 4, 3, 9, 4),
       (483, null, 0, 0, 0, 'Raises the user’s Special Attack, Special Defense, and Speed by one stage each.', null, 0,
        0, 'quiver-dance', null, null, null, null, '蝶舞', null, 20, 0,
        'Raises the user’s Special Attack, Special Defense, and Speed by one stage each.', 0, '轻巧地跳起神秘而又美丽的舞蹈。
提高自己的特攻、特防和速度。', 0, 2, 1, 7, 7),
       (484, 100, 0, 0, 0, 'Inflicts regular damage.  The greater the user’s weight compared to the target’s, the higher power this move has, to a maximum of 120.

User’s weight                    | Power
-------------------------------- | ----:
Up to 2× the target’s weight     |    40
Up to 3× the target’s weight     |    60
Up to 4× the target’s weight     |    80
Up to 5× the target’s weight     |   100
More than 5× the target’s weight |   120
', null, 0, 0, 'heavy-slam', null, null, null, null, '重磅冲撞', null, 10, 0,
        'Power is higher when the user weighs more than the target, up to a maximum of 120.', 0, '用沉重的身体撞向对手进行攻击。
自己比对手越重，威力越大。', 0, 0, 2, 10, 9),
       (485, 100, 0, 0, 0,
        'Inflicts regular damage.  Only Pokémon that share a type with the user will take damage from this move.', null,
        0, 0, 'synchronoise', null, null, null, null, '同步干扰', 120, 10, 0,
        'Hits any Pokémon that shares a type with the user.', 0, '用神奇电波对
周围所有和自己属性相同的
宝可梦给予伤害。', 0, 0, 3, 9, 14),
       (486, 100, 0, 0, 0, 'Inflicts regular damage.  The greater the user’s Speed compared to the target’s, the higher power this move has, to a maximum of 150.

User’s Speed                     | Power
-------------------------------- | ----:
Up to 2× the target’s Speed      |    60
Up to 3× the target’s Speed      |    80
Up to 4× the target’s Speed      |   120
More than 4× the target’s Speed  |   150
', null, 0, 0, 'electro-ball', null, null, null, null, '电球', null, 10, 0,
        'Power is higher when the user has greater Speed than the target, up to a maximum of 150.', 0, '用电气团撞向对手。
自己比对手速度越快，
威力越大。', 0, 0, 3, 10, 13),
       (487, 100, 0, 0, 0,
        'Changes the target to pure water-type until it leaves the field.  If the target has multitype, this move will fail.',
        null, 0, 0, 'soak', null, null, null, null, '浸水', null, 20, 0, 'Changes the target’s type to Water.', 0, '将大量的水泼向对手，
从而使其变成水属性。', 0, 13, 1, 10, 11),
       (488, 100, 0, 0, 0, 'Inflicts regular damage.  Raises the user’s Speed by one stage.', 100, 0, 0, 'flame-charge',
        null, null, null, null, '蓄能焰袭', 50, 20, 0,
        'Inflicts regular damage.  Raises the user’s Speed by one stage.', 100, '让火焰覆盖全身，攻击对手。
积蓄力量并提高自己的速度。', 0, 7, 2, 10, 10),
       (489, null, 0, 0, 0, 'Raises the user’s Attack, Defense, and accuracy by one stage each.', null, 0, 0, 'coil',
        null, null, null, null, '盘蜷', null, 20, 0,
        'Raises the user’s Attack, Defense, and accuracy by one stage each.', 0, '盘蜷着集中精神。
提高自己的攻击、防御和命中率。', 0, 2, 1, 7, 4),
       (490, 100, 0, 0, 0, 'Lowers the target’s Speed by one stage.', 100, 0, 0, 'low-sweep', null, null, null, null,
        '下盘踢', 65, 20, 0, 'Lowers the target’s Speed by one stage.', 100, '以敏捷的动作瞄准
对手的脚进行攻击。
降低对手的速度。', 0, 6, 2, 10, 2),
       (491, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Special Defense by two stages.', 100, 0, 0,
        'acid-spray', null, null, null, null, '酸液炸弹', 40, 20, 0,
        'Lowers the target’s Special Defense by two stages.', 100, '喷出能溶化对手的液体进行攻击。
大幅降低对手的特防。', 0, 6, 3, 10, 4),
       (492, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage is calculated using the target’s attacking stat rather than the user’s.',
        null, 0, 0, 'foul-play', null, null, null, null, '欺诈', 95, 15, 0,
        'Calculates damage with the target’s attacking stat.', 0, '利用对手的力量进行攻击。
正和自己战斗的对手，
其攻击越高，伤害越大。', 0, 0, 2, 10, 17),
       (493, 100, 0, 0, 0, 'Changes the target’s ability to simple.', null, 0, 0, 'simple-beam', null, null, null, null,
        '单纯光束', null, 15, 0, 'Changes the target’s ability to Simple.', 0, '向对手发送谜之念波。
接收到念波的对手，
其特性会变为单纯。', 0, 13, 1, 10, 1),
       (494, 100, 0, 0, 0,
        'Changes the target’s ability to match the user’s.  This effect ends when the target leaves battle.', null, 0,
        0, 'entrainment', null, null, null, null, '找伙伴', null, 15, 0, 'Copies the user’s ability onto the target.',
        0, '用神奇的节奏跳舞。
使对手模仿自己的动作，
从而将特性变成一样。', 0, 13, 1, 10, 1),
       (495, null, 0, 0, 0, 'The target will act next this turn, regardless of Speed or move priority.
If the target has already acted this turn, this move will fail.', null, 0, 0, 'after-you', null, null, null, null,
        '您先请', null, 15, 0, 'Makes the target act next this turn.', 0, '支援我方或对手的行动，
使其紧接着此招式之后行动。', 0, 13, 1, 10, 1),
       (496, 100, 0, 0, 0,
        'Inflicts regular damage.  If round has already been used this turn, this move’s power is doubled.  After this move is used, any other Pokémon using it this turn will immediately do so (in the order they would otherwise act), regardless of Speed or priority.  Pokémon using other moves will then continue to act as usual.',
        null, 0, 0, 'round', null, null, null, null, '轮唱', 60, 15, 0,
        'Has double power if it’s used more than once per turn.', 0, '用歌声攻击对手。
同伴还可以接着使出轮唱招式，
威力也会提高。', 0, 0, 3, 10, 1),
       (497, 100, 0, 0, 0,
        'Inflicts regular damage.  If any friendly Pokémon used this move earlier this turn or on the previous turn, that use’s power is added to this move’s power, to a maximum of 200.',
        null, 0, 0, 'echoed-voice', null, null, null, null, '回声', 40, 15, 0,
        'Power increases by 100% for each consecutive use by any friendly Pokémon, to a maximum of 200.', 0, '用回声攻击对手。
如果每回合都有宝可梦接着
使用该招式，威力就会提高。', 0, 0, 3, 10, 1),
       (498, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation ignores the target’s stat modifiers, including evasion.', null, 0,
        0, 'chip-away', null, null, null, null, '逐步击破', 70, 20, 0, 'Ignores the target’s stat modifiers.', 0, '看准机会稳步攻击。
无视对手的能力变化，
直接给予伤害。', 0, 0, 2, 10, 1),
       (499, null, 0, 0, 0, 'Inflicts regular damage.  All of the target’s stat modifiers are reset to zero.', null, 0,
        0, 'clear-smog', null, null, null, null, '清除之烟', 50, 15, 0, 'Removes all of the target’s stat modifiers.',
        0, '向对手投掷
特殊的泥块进行攻击。
使其能力变回原点。', 0, 0, 3, 10, 4),
       (500, 100, 0, 0, 0,
        'Inflicts regular damage.  Power is increased by 100% its original value for every stage any of the user’s stats have been raised.  Accuracy, evasion, and lowered stats do not affect this move’s power.  For a Pokémon with all five stats modified to +6, this move’s power is 31×.',
        null, 0, 0, 'stored-power', null, null, null, null, '辅助力量', 20, 10, 0,
        'Power is higher the more the user’s stats have been raised, to a maximum of 31×.', 0, '用蓄积起来的力量攻击对手。
自己的能力提高得越多，
威力就越大。', 0, 0, 3, 10, 14),
       (501, null, 0, 0, 0, 'Moves with priority greater than 0 will not hit friendly Pokémon for the remainder of this turn.  If the user is last to act this turn, this move will fail.

This move cannot be selected by assist or metronome.', null, 0, 0, 'quick-guard', null, null, null, null, '快速防守',
        null, 15, 3, 'Prevents any priority moves from hitting friendly Pokémon this turn.', 0, '守护自己和同伴，
以防对手的先制攻击。', 0, 11, 1, 4, 2),
       (502, null, 0, 0, 0,
        'User switches position on the field with the friendly Pokémon opposite it.  If the user is in the middle position in a triple battle, or there are no other friendly Pokémon, this move will fail.',
        null, 0, 0, 'ally-switch', null, null, null, null, '交换场地', null, 15, 2,
        'User switches places with the friendly Pokémon opposite it.', 0, '用神奇的力量瞬间移动，
互换自己和同伴所在的位置。', 0, 13, 1, 7, 14),
       (503, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 30, 0, 0,
        'scald', null, null, null, null, '热水', 80, 15, 0, 'Has a $effect_chance% chance to burn the target.', 0, '向对手喷射
煮得翻滚的开水进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 11),
       (504, null, 0, 0, 0,
        'Raises the user’s Attack, Special Attack, and Speed by two stages each.  Lowers the user’s Defense and Special Defense by one stage each.',
        null, 0, 0, 'shell-smash', null, null, null, null, '破壳', null, 15, 0,
        'Raises user’s Attack, Special Attack, and Speed by two stages.  Lower user’s Defense and Special Defense by one stage.',
        0, '打破外壳，
降低自己的防御和特防，
但大幅提高攻击、特攻和速度。', 0, 13, 1, 7, 1),
       (505, null, 0, 0, 0, 'Heals the target for half its max HP.', null, 0, 50, 'heal-pulse', null, null, null, null,
        '治愈波动', null, 10, 0, 'Heals the target for half its max HP.', 0, '放出治愈波动，
从而回复对手
最大ＨＰ的一半。', 0, 3, 1, 10, 14),
       (506, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target has a major status ailment, this move has double power.', null, 0, 0,
        'hex', null, null, null, null, '祸不单行', 65, 10, 0,
        'Has double power if the target has a major status ailment.', 0, '接二连三地进行攻击。
对处于异常状态的对手
给予较大的伤害。', 0, 0, 3, 10, 8),
       (507, 100, 0, 0, 0, 'Inflicts regular damage.  User carries the target high into the air for one turn, during which no moves will hit either Pokémon and neither can act.  On the following turn, the user drops the target, inflicting damage and ending the effect.

If the target is flying-type, this move will function as normal but inflict no damage.

gust, hurricane, sky uppercut, smack down, thunder, twister, and whirlwind can hit both the user and the target during this effect.  gust and twister will additionally have double power.

The damage from hail and sandstorm still applies during this effect.

Either Pokémon may be hit during this effect if also under the effect of lock on, mind reader, or no guard.

This move cannot be used while gravity is in effect.

This move cannot be selected by sleep talk.

*Bug*: If gravity is used during a double or triple battle while this move is in effect, this move is not correctly canceled on the target, and it remains high in the air indefinitely.  As this move prevents the target from acting, the only way to subsequently remove it from the field is to faint it.',
        null, 0, 0, 'sky-drop', null, null, null, null, '自由落体', 60, 10, 0,
        'Carries the target high into the air, dodging all attacks against either, and drops it next turn.', 0, '第１回合将对手带到空中，
第２回合将其摔下进行攻击。
被带到空中的对手不能动弹。', 0, 0, 2, 10, 3),
       (508, null, 0, 0, 0, 'Raises the user’s Attack by one stage and its Speed by two stages.', null, 0, 0,
        'shift-gear', null, null, null, null, '换档', null, 10, 0,
        'Raises the user’s Attack by one stage and its Speed by two stages.', 0, '转动齿轮，
不仅提高自己的攻击，
还会大幅提高速度。', 0, 2, 1, 7, 9),
       (509, 90, 0, 0, 0, 'Inflicts regular damage, then switches the target out for another of its trainer’s Pokémon, selected at random.

If the target is under the effect of ingrain or suction cups, or it has a substitute, or its Trainer has no more usable Pokémon, it will not be switched out.  If the target is a wild Pokémon, the battle ends instead.',
        null, 0, 0, 'circle-throw', null, null, null, null, '巴投', 60, 10, -6,
        'Ends wild battles.  Forces trainers to switch Pokémon.', 0, '扔飞对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', 0, 0, 2, 10, 2),
       (510, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target is holding a berry, it’s destroyed and cannot be used in response to this move.',
        null, 0, 0, 'incinerate', null, null, null, null, '烧尽', 60, 15, 0, 'Destroys the target’s held berry.', 0, '用火焰攻击对手。
对手携带树果等时，
会烧掉，使其不能使用。', 0, 0, 3, 11, 10),
       (511, 100, 0, 0, 0,
        'Forces the target to act last this turn, regardless of Speed or move priority.  If the target has already acted this turn, this move will fail.',
        null, 0, 0, 'quash', null, null, null, null, '延后', null, 15, 0, 'Makes the target act last this turn.', 0, '压制对手，
从而将其行动顺序放到最后。', 0, 13, 1, 10, 17),
       (512, 100, 0, 0, 0, 'Inflicts regular damage.  If the user has no held item, this move has double power.', null,
        0, 0, 'acrobatics', null, null, null, null, '杂技', 55, 15, 0, 'Has double power if the user has no held item.',
        0, '轻巧地攻击对手。
自己没有携带道具时，
会给予较大的伤害。', 0, 0, 2, 10, 3),
       (515, 100, 0, 0, 0, 'Inflicts damage equal to the user’s remaining HP.  User faints.', null, 0, 0,
        'final-gambit', null, null, null, null, '搏命', null, 5, 0,
        'Inflicts damage equal to the user’s remaining HP.  User faints.', 0, '拼命攻击对手。
虽然自己陷入濒死，但会给予对手
和自己目前ＨＰ等量的伤害。', 0, 0, 3, 10, 2),
       (516, null, 0, 0, 0,
        'Transfers the user’s held item to the target.  If the user has no held item, or the target already has a held item, this move will fail.',
        null, 0, 0, 'bestow', null, null, null, null, '传递礼物', null, 15, 0,
        'Gives the user’s held item to the target.', 0, '当对手未携带道具时，
能够将自己携带的道具交给对手。', 0, 13, 1, 10, 1),
       (517, 50, 100, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 100, 0, 0,
        'inferno', null, null, null, null, '炼狱', 100, 5, 0, 'Has a $effect_chance% chance to burn the target.', 0, '用烈焰包裹住对手进行攻击。
让对手陷入灼伤状态。', 4, 4, 3, 10, 10),
       (518, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon used grass pledge earlier this turn, all opposing Pokémon have halved Speed for four turns (including this one).',
        null, 0, 0, 'water-pledge', null, null, null, null, '水之誓约', 80, 10, 0,
        'With Grass Pledge, halves opposing Pokémon’s Speed for four turns.', 0, '用水柱进行攻击。
如果和火组合，威力就会提高，
天空中会挂上彩虹。', 0, 0, 3, 10, 11),
       (519, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon used water pledge earlier this turn, moves used by any friendly Pokémon have doubled effect chance for four turns (including this one).',
        null, 0, 0, 'fire-pledge', null, null, null, null, '火之誓约', 80, 10, 0,
        'With Water Pledge, doubles the effect chance of friendly Pokémon’s moves for four turns.', 0, '用火柱进行攻击。
如果和草组合，威力就会提高，
周围会变成火海。', 0, 0, 3, 10, 10),
       (520, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon used fire pledge earlier this turn, all opposing Pokémon will take 1/8 their max HP in damage at the end of every turn for four turns (including this one).',
        null, 0, 0, 'grass-pledge', null, null, null, null, '草之誓约', 80, 10, 0,
        'With Fire Pledge, damages opposing Pokémon for 1/8 their max HP every turn for four turns.', 0, '用草柱进行攻击。
如果和水组合，威力就会提高，
周围会变成湿地。', 0, 0, 3, 10, 12),
       (521, 100, 0, 0, 0, 'Inflicts regular damage, then the user immediately switches out, and the trainer selects a replacement Pokémon from the party.  If the target faints from this attack, the user’s trainer selects the new Pokémon to send out first.  If the user is the last Pokémon in its party that can battle, it will not switch out.

The user may be hit by pursuit when it switches out, if it has been targeted and pursuit has not yet been used.

This move may be used even if the user is under the effect of ingrain.  ingrain’s effect will end.', null, 0, 0,
        'volt-switch', null, null, null, null, '伏特替换', 70, 20, 0, 'User must switch out after attacking.', 0, '在攻击之后急速返回，
和后备宝可梦进行替换。', 0, 0, 3, 10, 13),
       (522, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.',
        100, 0, 0, 'struggle-bug', null, null, null, null, '虫之抵抗', 50, 20, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 100, '抵抗并攻击对手。
降低对手的特攻。', 0, 6, 3, 11, 7),
       (523, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, 0, 0,
        'bulldoze', null, null, null, null, '重踏', 60, 20, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, '用力踩踏地面并攻击
自己周围所有的宝可梦。
降低对方的速度。', 0, 6, 2, 9, 5),
       (524, 90, 100, 6, 0, 'Inflicts regular damage.  Always scores a critical hit.', 100, 0, 0, 'frost-breath', null,
        null, null, null, '冰息', 60, 10, 0, 'Always scores a critical hit.', 0, '将冰冷的气息
吹向对手进行攻击。
必定会击中要害。', 0, 0, 3, 10, 15),
       (525, 90, 0, 0, 0, 'Inflicts regular damage, then switches the target out for another of its trainer’s Pokémon, selected at random.

If the target is under the effect of ingrain or suction cups, or it has a substitute, or its Trainer has no more usable Pokémon, it will not be switched out.  If the target is a wild Pokémon, the battle ends instead.',
        null, 0, 0, 'dragon-tail', null, null, null, null, '龙尾', 60, 10, -6,
        'Ends wild battles.  Forces trainers to switch Pokémon.', 0, '弹飞对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', 0, 0, 2, 10, 16),
       (526, null, 0, 0, 0, 'Raises the user’s Attack and Special Attack by one stage each.', null, 0, 0, 'work-up',
        null, null, null, null, '自我激励', null, 30, 0,
        'Raises the user’s Attack and Special Attack by one stage each.', 0, '激励自己，
从而提高攻击和特攻。', 0, 2, 1, 7, 1),
       (527, 95, 0, 0, 0, 'Lowers the target’s Speed by one stage.', 100, 0, 0, 'electroweb', null, null, null, null,
        '电网', 55, 15, 0, 'Lowers the target’s Speed by one stage.', 100, '用电网捉住对手进行攻击。
降低对手的速度。', 0, 6, 3, 11, 13),
       (528, 100, 0, 0, -25, 'Inflicts regular damage.  User takes 1/4 the damage it inflicts in recoil.', null, 0, 0,
        'wild-charge', null, null, null, null, '疯狂伏特', 90, 15, 0,
        'User receives 1/4 the damage it inflicts in recoil.', 0, '让电流覆盖全身
撞向对手进行攻击。
自己也会受到少许伤害。', 0, 0, 2, 10, 13),
       (529, 95, 0, 1, 0,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, 0, 0,
        'drill-run', null, null, null, null, '直冲钻', 80, 10, 0, 'Has an increased chance for a critical hit.', 0, '像钢钻一样，一边旋转身体
一边撞击对手。
容易击中要害。', 0, 0, 2, 10, 5),
       (530, 90, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', null, 0, 0, 'dual-chop', 2, null, 2,
        null, '二连劈', 40, 15, 0, 'Hits twice in one turn.', 0, '用身体坚硬的部分
拍打对手进行攻击。
连续２次给予伤害。', 0, 0, 2, 10, 16),
       (531, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'heart-stamp', null, null, null, null, '爱心印章', 60, 25, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '以可爱的动作使对手疏忽，
乘机给出强烈的一击。
有时会使对手畏缩。', 0, 0, 2, 10, 14),
       (532, 100, 0, 0, 50, 'Inflicts regular damage.  Drains half the damage inflicted to heal the user.', null, 0, 0,
        'horn-leech', null, null, null, null, '木角', 75, 10, 0, 'Drains half the damage inflicted to heal the user.',
        0, '将角刺入，吸取对手的养分。
可以回复给予对手
伤害的一半ＨＰ。', 0, 8, 2, 10, 12),
       (533, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation ignores the target’s stat modifiers, including evasion.', null, 0,
        0, 'sacred-sword', null, null, null, null, '圣剑', 90, 15, 0, 'Ignores the target’s stat modifiers.', 0, '用长角切斩对手进行攻击。
无视对手的能力变化，
直接给予伤害。', 0, 0, 2, 10, 2),
       (534, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, 0, 0,
        'razor-shell', null, null, null, null, '贝壳刃', 75, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 50, '用锋利的贝壳切斩
对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 11),
       (535, 100, 0, 0, 0, 'Inflicts regular damage.  The greater the user’s weight compared to the target’s, the higher power this move has, to a maximum of 120.

User’s weight                    | Power
-------------------------------- | ----:
Up to 2× the target’s weight     |    40
Up to 3× the target’s weight     |    60
Up to 4× the target’s weight     |    80
Up to 5× the target’s weight     |   100
More than 5× the target’s weight |   120
', null, 0, 0, 'heat-crash', null, null, null, null, '高温重压', null, 10, 0,
        'Power is higher when the user weighs more than the target, up to a maximum of 120.', 0, '用燃烧的身体撞向对手进行攻击。
自己比对手越重，威力越大。', 0, 0, 2, 10, 10),
       (536, 90, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 50, 0, 0,
        'leaf-tornado', null, null, null, null, '青草搅拌器', 65, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 50, '用锋利的叶片包裹住
对手进行攻击。
有时会降低对手的命中率。', 0, 6, 3, 10, 12),
       (537, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.

Power is doubled against Pokémon that have used minimize since entering the field.', 30, 30, 0, 'steamroller', null,
        null, null, null, '疯狂滚压', 65, 20, 0, 'Has a $effect_chance% chance to make the target flinch.', 0, '旋转揉成团的身体
压扁对手。
有时会使对手畏缩。', 0, 0, 2, 10, 7),
       (538, null, 0, 0, 0, 'Raises the user’s Defense by three stages.', null, 0, 0, 'cotton-guard', null, null, null,
        null, '棉花防守', null, 10, 0, 'Raises the user’s Defense by three stages.', 0, '用软绵绵的绒毛包裹住
自己的身体进行守护。
巨幅提高自己的防御。', 0, 2, 1, 7, 12),
       (539, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 40, 0, 0,
        'night-daze', null, null, null, null, '暗黑爆破', 85, 10, 0,
        'Has a $effect_chance% chance to lower the target’s accuracy by one stage.', 40, '放出黑暗的冲击波攻击对手。
有时会降低对手的命中率。', 0, 6, 3, 10, 17),
       (540, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation always uses the target’s Defense, regardless of this move’s damage class.',
        null, 0, 0, 'psystrike', null, null, null, null, '精神击破', 100, 10, 0,
        'Inflicts damage based on the target’s Defense, not Special Defense.', 0, '将神奇的念波实体化攻击对手。
给予物理伤害。', 0, 0, 3, 10, 14),
       (541, 85, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times in one turn.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.',
        null, 0, 0, 'tail-slap', 5, null, 2, null, '扫尾拍打', 25, 10, 0, 'Hits 2-5 times in one turn.', 0, '用坚硬的尾巴
拍打对手进行攻击。
连续攻击２～５次。', 0, 0, 2, 10, 1),
       (542, 70, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.

This move can hit Pokémon under the effect of bounce, fly, or sky drop.

During rain dance, this move has 100% accuracy.  During sunny day, this move has 50% accuracy.', 30, 0, 0, 'hurricane',
        null, 5, null, 2, '暴风', 110, 10, 0, 'Has a $effect_chance% chance to confuse the target.', 0, '用强烈的风席卷
对手进行攻击。
有时会使对手混乱。', 6, 4, 3, 10, 3),
       (543, 100, 0, 0, -25, 'Inflicts regular damage.  User takes 1/4 the damage it inflicts in recoil.', null, 0, 0,
        'head-charge', null, null, null, null, '爆炸头突击', 120, 15, 0,
        'User receives 1/4 the damage it inflicts in recoil.', 0, '用厉害的爆炸头
猛撞向对手进行攻击。
自己也会受到少许伤害。', 0, 0, 2, 10, 1),
       (544, 85, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', null, 0, 0, 'gear-grind', 2, null, 2,
        null, '齿轮飞盘', 50, 15, 0, 'Hits twice in one turn.', 0, '向对手投掷
钢铁齿轮进行攻击。
连续２次给予伤害。', 0, 0, 2, 10, 9),
       (545, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 30, 0, 0,
        'searing-shot', null, null, null, null, '火焰弹', 100, 5, 0, 'Has a $effect_chance% chance to burn the target.',
        0, '用熊熊烈火
攻击自己周围所有的宝可梦。
有时会陷入灼伤状态。', 4, 4, 3, 9, 10),
       (546, 100, 0, 0, 0, 'Inflicts regular damage.  If the user is holding a plate or a drive, this move’s type is the type corresponding to that item.

Note: This effect is technically shared by both techno blast and judgment; however, Techno Blast is only affected by drives, and Judgment is only affected by plates.',
        null, 0, 0, 'techno-blast', null, null, null, null, '高科技光炮', 120, 5, 0,
        'If the user is holding a appropriate plate or drive, the damage inflicted will match it.', 0, '向对手放出光弹。
属性会根据自己
携带的卡带不同而改变。', 0, 0, 3, 10, 1),
       (547, 100, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to put the target to sleep.
If the user is a meloetta, it will toggle between Aria and Pirouette Forme.', 10, 0, 0, 'relic-song', null, 4, null, 2,
        '古老之歌', 75, 10, 0, 'Has a $effect_chance% chance to put the target to sleep.', 0, '让对手听古老之歌，
打动对手的内心进行攻击。
有时会让对手陷入睡眠状态。', 2, 4, 3, 11, 1),
       (548, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation always uses the target’s Defense, regardless of this move’s damage class.',
        null, 0, 0, 'secret-sword', null, null, null, null, '神秘之剑', 85, 10, 0,
        'Inflicts damage based on the target’s Defense, not Special Defense.', 0, '用长角切斩对手进行攻击。
角上拥有的神奇力量
将给予物理伤害。', 0, 0, 3, 10, 2),
       (549, 95, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Speed by one stage.', 100, 0, 0, 'glaciate',
        null, null, null, null, '冰封世界', 65, 10, 0, 'Lowers the target’s Speed by one stage.', 100, '将冰冻的冷气
吹向对手进行攻击。
会降低对手的速度。', 0, 6, 3, 11, 15),
       (550, 85, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 20, 0, 0,
        'bolt-strike', null, null, null, null, '雷击', 130, 5, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '让强大的电流覆盖全身，
猛撞向对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 13),
       (551, 85, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 20, 0, 0,
        'blue-flare', null, null, null, null, '青焰', 130, 5, 0, 'Has a $effect_chance% chance to burn the target.', 0, '用美丽而激烈的青焰
包裹住对手进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 10),
       (552, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to raise the user’s Special Attack by one stage.', 50,
        0, 0, 'fiery-dance', null, null, null, null, '火之舞', 80, 10, 0,
        'Has a $effect_chance% chance to raise the user’s Special Attack by one stage.', 50, '让火焰覆盖全身，
振翅攻击对手。
有时会提高自己的特攻。', 0, 7, 3, 10, 10),
       (553, 90, 30, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.  User charges for one turn before attacking.',
        30, 0, 0, 'freeze-shock', null, null, null, null, '冰冻伏特', 140, 5, 0,
        'Requires a turn to charge before attacking.  Has a $effect_chance% chance to paralyze the target.', 0, '用覆盖着电流的冰块，
在第２回合撞向对手。
有时会让对手陷入麻痹状态。', 1, 4, 2, 10, 15),
       (554, 90, 30, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.  User charges for one turn before attacking.',
        30, 0, 0, 'ice-burn', null, null, null, null, '极寒冷焰', 140, 5, 0,
        'Requires a turn to charge before attacking.  Has a $effect_chance% chance to burn the target.', 0, '用能够冻结一切的强烈冷气，
在第２回合包裹住对手。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 15),
       (555, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.',
        100, 0, 0, 'snarl', null, null, null, null, '大声咆哮', 55, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 100, '没完没了地大声斥责，
从而降低对手的特攻。', 0, 6, 3, 11, 17),
       (556, 90, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'icicle-crash', null, null, null, null, '冰柱坠击', 85, 10, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '用大冰柱激烈地
撞向对手进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 15),
       (557, 95, 0, 0, 0,
        'Inflicts regular damage.  Lowers the user’s Defense, Special Defense, and Speed by one stage each.', 100, 0, 0,
        'v-create', null, null, null, null, 'Ｖ热焰', 180, 5, 0,
        'Lowers the user’s Defense, Special Defense, and Speed by one stage each.', 100, '从前额产生灼热的火焰，
舍身撞击对手。
防御、特防和速度会降低。', 0, 7, 2, 10, 10),
       (558, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon used fusion bolt earlier this turn, this move has double power.',
        null, 0, 0, 'fusion-flare', null, null, null, null, '交错火焰', 100, 5, 0,
        'With Fusion Bolt, inflicts double damage.', 0, '释放出巨大的火焰。
受到巨大的闪电影响时，
招式威力会提高。', 0, 0, 3, 10, 10),
       (559, 100, 0, 0, 0,
        'Inflicts regular damage.  If a friendly Pokémon used fusion flare earlier this turn, this move has double power.',
        null, 0, 0, 'fusion-bolt', null, null, null, null, '交错闪电', 100, 5, 0,
        'With Fusion Flare, inflicts double damage.', 0, '释放出巨大的闪电。
受到巨大的火焰影响时，
招式威力会提高。', 0, 0, 2, 10, 13),
       (560, 95, 0, 0, 0, 'Inflicts regular damage.  For the purposes of type effectiveness, this move is both fighting- and flying-type: its final effectiveness is determined by multiplying the effectiveness of each type against each of the target’s types.

For all other purposes, this move is pure Fighting-type.  If this move’s type is changed, its Fighting typing is overwritten, and its secondary type remains Flying.

If the target has used minimize since entering battle, this move has double power and will never miss.', null, 0, 0,
        'flying-press', null, null, null, null, '飞身重压', 100, 10, 0, 'Deals both fighting and flying-type damage.',
        0, '从空中俯冲向对手。
此招式同时带有
格斗属性和飞行属性。', 0, 0, 2, 10, 2),
       (561, null, 0, 0, 0,
        'Protects all friendly Pokémon from damaging moves.  Only works on the first turn after the user is sent out.',
        null, 0, 0, 'mat-block', null, null, null, null, '掀榻榻米', null, 10, 0,
        'Protects all friendly Pokémon from damaging moves.  Only works on the first turn after the user is sent out.',
        0, '将掀起来的榻榻米当作盾牌，
防住自己和同伴免受招式伤害。
变化招式无法防住。', 0, 11, 1, 4, 2),
       (562, 90, 0, 0, 0, 'Inflicts regular damage.  Can only be used if the user has eaten a berry since the beginning of the battle.

After the user eats a berry, it may use this move any number of times until the end of the battle, even if it switches out.  Eating a held berry, eating a berry via bug bite or pluck, or being the target of a Flung berry will enable this move.  Feeding a Pokémon a berry from the bag or using natural gift will not.

If the trainer chooses this move when it cannot be used, the choice is rejected outright and the trainer must choose another move.',
        null, 0, 0, 'belch', null, null, null, null, '打嗝', 120, 10, 0,
        'Can only be used after the user has eaten a berry.', 0, '朝着对手打嗝，
并给予伤害。
如果不吃树果则无法使出。', 0, 0, 3, 10, 4),
       (563, null, 0, 0, 0, 'Raises the Attack and Special Attack of all grass Pokémon in battle.', 100, 0, 0,
        'rototiller', null, null, null, null, '耕地', null, 10, 0,
        'Raises the Attack and Special Attack of all grass Pokémon in battle.', 100, '翻耕土地，
使草木更容易成长。
会提高草属性宝可梦的攻击和特攻。', 0, 2, 1, 14, 5),
       (564, null, 0, 0, 0, 'Shoots a web over the opponents’ side of the field, which lowers the Speed of any opposing Pokémon that enters the field by one stage.

Pokémon in the air, such as flying-types and those with levitate, are unaffected.  rapid spin removes Sticky Web from the user’s side of the field; defog removes it from both sides.',
        null, 0, 0, 'sticky-web', null, null, null, null, '黏黏网', null, 20, 0,
        'Covers the opposing field, lowering opponents’ Speed by one stage upon switching in.', 0, '在对手周围围上黏黏的网，
降低替换出场的对手的速度。', 0, 11, 1, 6, 7),
       (565, 100, 0, 0, 0, 'Inflicts regular damage.  Raises the user’s Attack by two stages if it KOs the target.',
        null, 0, 0, 'fell-stinger', null, null, null, null, '致命针刺', 50, 25, 0,
        'Raises the user’s Attack by two stages if it KOs the target.', 0, '如果使用此招式打倒对手，
攻击会巨幅提高。', 0, 0, 2, 10, 7),
       (566, 100, 0, 0, 0, 'Inflicts regular damage.  User vanishes for one turn, becoming immune to attack, and hits on the second turn.

This move ignores the effects of detect and protect.

This move cannot be selected by sleep talk.', null, 0, 0, 'phantom-force', null, null, null, null, '潜灵奇袭', 90, 10,
        0, 'User vanishes, dodging all attacks, and hits next turn.  Hits through Protect and Detect.', 0, '第１回合消失在某处，
第２回合攻击对手。
可以无视守护进行攻击。', 0, 0, 2, 10, 8),
       (567, 100, 0, 0, 0, 'Adds ghost to the target’s types.', null, 0, 0, 'trick-or-treat', null, null, null, null,
        '万圣夜', null, 20, 0, 'Adds ghost to the target’s types.', 0, '邀请对手参加万圣夜。
使对手被追加幽灵属性。', 0, 13, 1, 10, 8),
       (568, 100, 0, 0, 0, 'Lowers the target’s Attack and Special Attack by one stage.', 100, 0, 0, 'noble-roar', null,
        null, null, null, '战吼', null, 30, 0, 'Lowers the target’s Attack and Special Attack by one stage.', 100, '发出战吼威吓对手，
从而降低对手的攻击和特攻。', 0, 2, 1, 10, 1),
       (569, null, 0, 0, 0, 'Changes all Pokémon’s normal moves to electric moves for the rest of the turn.', null, 0,
        0, 'ion-deluge', null, null, null, null, '等离子浴', null, 25, 1,
        'Changes all normal moves to electric moves for the rest of the turn.', 0, '将带电粒子扩散开来，
使一般属性的招式变成电属性。', 0, 10, 1, 12, 13),
       (570, 100, 0, 0, 50, 'Heals the user for half the total damage dealt to all targets.', null, 0, 0,
        'parabolic-charge', null, null, null, null, '抛物面充电', 65, 20, 0,
        'Heals the user for half the total damage dealt to all targets.', 0, '给周围全体宝可梦造成伤害。
可以回复给予伤害的一半ＨＰ。', 0, 8, 3, 9, 13),
       (571, 100, 0, 0, 0, 'Adds grass to the target’s types.', null, 0, 0, 'forests-curse', null, null, null, null,
        '森林诅咒', null, 20, 0, 'Adds grass to the target’s types.', 0, '向对手施加森林诅咒。
中了诅咒的对手
会被追加草属性。', 0, 13, 1, 10, 12),
       (572, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'petal-blizzard', null, null, null, null, '落英缤纷',
        90, 15, 0, 'Inflicts regular damage.', 0, '猛烈地刮起飞雪般的落花，
攻击周围所有的宝可梦，
并给予伤害。', 0, 0, 2, 9, 12),
       (573, 100, 10, 0, 0, 'Inflicts regular damage.  This move is super-effective against the water type.

The target’s other type will affect damage as usual.  If this move’s type is changed, it remains super-effective against Water regardless of its type.',
        10, 0, 0, 'freeze-dry', null, null, null, null, '冷冻干燥', 70, 20, 0, 'Super-effective against water.', 0, '急剧冷冻对手，
有时会让对手陷入冰冻状态。
对于水属性宝可梦也是效果绝佳。', 3, 4, 3, 10, 15),
       (574, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion.', null, 0, 0, 'disarming-voice',
        null, null, null, null, '魅惑之声', 40, 15, 0, 'Never misses.', 0, '发出魅惑的叫声，
给予对手精神上的伤害。
攻击必定会命中。', 0, 0, 3, 11, 18),
       (575, 100, 0, 0, 0, 'Lowers all targets’ Attack and Special Attack by one stage.  Makes the user switch out.',
        100, 0, 0, 'parting-shot', null, null, null, null, '抛下狠话', null, 20, 0,
        'Lowers all targets’ Attack and Special Attack by one stage.  Makes the user switch out.', 100, '抛下狠话威吓对手，
降低攻击和特攻后，
和后备宝可梦进行替换。', 0, 2, 1, 10, 17),
       (576, null, 0, 0, 0, 'Inverts the target’s stat modifiers.', null, 0, 0, 'topsy-turvy', null, null, null, null,
        '颠倒', null, 20, 0, 'Inverts the target’s stat modifiers.', 0, '颠倒对手身上的
所有能力变化，
变成和原来相反的状态。', 0, 13, 1, 10, 17),
       (577, 100, 0, 0, 75, 'Deals regular damage.  Drains 75% of the damage inflicted to heal the user.', null, 0, 0,
        'draining-kiss', null, null, null, null, '吸取之吻', 50, 10, 0,
        'Drains 75% of the damage inflicted to heal the user.', 0, '用一个吻吸取对手的ＨＰ。
回复给予对手
伤害的一半以上的ＨＰ。', 0, 8, 3, 10, 18),
       (578, null, 0, 0, 0, 'Protects all friendly Pokémon from non-damaging moves for the rest of the turn.

Unlike other blocking moves, this move may be used consecutively without its chance of success falling.', null, 0, 0,
        'crafty-shield', null, null, null, null, '戏法防守', null, 10, 3,
        'Protects all friendly Pokémon from non-damaging moves.', 0, '使用神奇的力量
防住攻击我方的变化招式。
但无法防住伤害招式的攻击。', 0, 11, 1, 4, 18),
       (579, null, 0, 0, 0, 'Raises the Defense of all grass Pokémon in battle.', 100, 0, 0, 'flower-shield', null,
        null, null, null, '鲜花防守', null, 10, 0, 'Raises the Defense of all grass Pokémon in battle.', 100, '使用神奇的力量
提高在场的所有
草属性宝可梦的防御。', 0, 13, 1, 14, 18),
       (580, null, 0, 0, 0, 'For five turns, heals all Pokémon on the ground for 1/16 their max HP each turn and strengthens their grass moves to 1.5× their power.

Changes nature power to energy ball.', null, 0, 0, 'grassy-terrain', null, null, null, null, '青草场地', null, 10, 0,
        'For five turns, heals all Pokémon on the ground for 1/16 max HP each turn and strengthens their grass moves to 1.5× their power.',
        0, '在５回合内变成青草场地。
地面上的宝可梦每回合都能回复。
草属性的招式威力还会提高。', 0, 10, 1, 12, 12),
       (581, null, 0, 0, 0, 'For five turns, protects all Pokémon on the ground from major status ailments and confusion and weakens dragon moves used against them to 0.5× their power.

Changes nature power to moonblast.', null, 0, 0, 'misty-terrain', null, null, null, null, '薄雾场地', null, 10, 0,
        'For five turns, protects all Pokémon on the ground from major status ailments and confusion, and halves the power of incoming dragon moves.',
        0, '在５回合内，
地面上的宝可梦不会陷入异常状态。
龙属性招式的伤害也会减半。', 0, 10, 1, 12, 18),
       (582, null, 0, 0, 0, 'Changes the target’s move’s type to electric if it hasn’t moved yet this turn.', null, 0,
        0, 'electrify', null, null, null, null, '输电', null, 20, 0,
        'Changes the target’s move’s type to electric if it hasn’t moved yet this turn.', 0, '对手使出招式前，
如果输电，则该回合
对手的招式变成电属性。', 0, 13, 1, 10, 13),
       (583, 90, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Attack by one stage.', 10, 0, 0,
        'play-rough', null, null, null, null, '嬉闹', 90, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Attack by one stage.', 10, '与对手嬉闹并攻击。
有时会降低对手的攻击。', 0, 6, 2, 10, 18),
       (584, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'fairy-wind', null, null, null, null, '妖精之风', 40,
        30, 0, 'Inflicts regular damage with no additional effect.', 0, '刮起妖精之风，
吹向对手进行攻击。', 0, 0, 3, 10, 18),
       (585, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 30,
        0, 0, 'moonblast', null, null, null, null, '月亮之力', 95, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 30, '借用月亮的力量攻击对手。
有时会降低对手的特攻。', 0, 6, 3, 10, 18),
       (586, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'boomburst', null, null, null, null, '爆音波', 140,
        10, 0, 'Inflicts regular damage.', 0, '通过震耳欲聋的爆炸声
产生的破坏力，
攻击自己周围所有的宝可梦。', 0, 0, 3, 9, 1),
       (587, null, 0, 0, 0, 'Prevents all Pokémon from fleeing or switching out during the next turn.', null, 0, 0,
        'fairy-lock', null, null, null, null, '妖精之锁', null, 10, 0,
        'Prevents all Pokémon from fleeing or switching out during the next turn.', 0, '通过封锁，
下一回合所有的
宝可梦都无法逃走。', 0, 10, 1, 12, 18),
       (588, null, 0, 0, 0,
        'Blocks damaging attacks and lowers attacking Pokémon’s Attack by two stages on contact.  Switches Aegislash to Shield Forme.',
        null, 0, 0, 'kings-shield', null, null, null, null, '王者盾牌', null, 10, 4,
        'Blocks damaging attacks and lowers attacking Pokémon’s Attack by two stages on contact.  Switches Aegislash to Shield Forme.',
        0, '防住对手攻击的同时，
自己变为防御姿态。
大幅降低所接触到的对手的攻击。', 0, 13, 1, 7, 9),
       (589, null, 0, 0, 0, 'Lowers the target’s Attack by one stage.', 100, 0, 0, 'play-nice', null, null, null, null,
        '和睦相处', null, 20, 0, 'Lowers the target’s Attack by one stage.', 100, '和对手和睦相处，
使其失去战斗的气力，
从而降低对手的攻击。', 0, 2, 1, 10, 1),
       (590, null, 0, 0, 0, 'Lowers the target’s Special Attack by one stage.', 100, 0, 0, 'confide', null, null, null,
        null, '密语', null, 20, 0, 'Lowers the target’s Special Attack by one stage.', 100, '和对手进行密语，
使其失去集中力，
从而降低对手的特攻。', 0, 2, 1, 10, 1),
       (591, 95, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to raise the user’s Defense by two stages for each target hit.',
        50, 0, 0, 'diamond-storm', null, null, null, null, '钻石风暴', 100, 5, 0,
        'Has a $effect_chance% chance to raise the user’s Defense by two stages for each target hit.', 50, '掀起钻石风暴给予伤害。
有时会大幅提高自己的防御。', 0, 7, 2, 11, 6),
       (592, 95, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 30, 0, 0,
        'steam-eruption', null, null, null, null, '蒸汽爆炸', 110, 5, 0,
        'Has a $effect_chance% chance to burn the target.', 0, '将滚烫的蒸汽喷向对手。
有时会让对手灼伤。', 4, 4, 3, 10, 11),
       (593, null, 0, 0, 0, 'Inflicts regular damage.  Bypasses and removes any protection effect on the target.', null,
        0, 0, 'hyperspace-hole', null, null, null, null, '异次元洞', 80, 5, 0,
        'Ignores and destroys protection effects.', 0, '通过异次元洞，
突然出现在对手的侧面进行攻击。
还可以无视守住和看穿等招式。', 0, 0, 3, 10, 14),
       (594, 100, 0, 0, 0, 'Inflicts regular damage.  Hits 2–5 times.', null, 0, 0, 'water-shuriken', 5, null, 2, null,
        '飞水手里剑', 15, 20, 1, 'Hits 2–5 times.', 0, '用粘液制成的手里剑，
连续攻击２～５次。
必定能够先制攻击。', 0, 0, 3, 10, 11),
       (595, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.',
        100, 0, 0, 'mystical-fire', null, null, null, null, '魔法火焰', 75, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 100, '从口中喷出特别灼热的
火焰进行攻击。
降低对手的特攻。', 0, 6, 3, 10, 10),
       (596, null, 0, 0, 0, 'Blocks damaging attacks and damages attacking Pokémon for 1/8 their max HP.', null, 0, 0,
        'spiky-shield', null, null, null, null, '尖刺防守', null, 10, 4,
        'Blocks damaging attacks and damages attacking Pokémon for 1/8 their max HP.', 0, '防住对手攻击的同时，
削减接触到自己的对手的体力。', 0, 13, 1, 7, 12),
       (597, null, 0, 0, 0, 'Raises a selected ally’s Special Defense by one stage.', null, 0, 0, 'aromatic-mist', null,
        null, null, null, '芳香薄雾', null, 20, 0, 'Raises a selected ally’s Special Defense by one stage.', 0, '通过神奇的芳香，
提高我方宝可梦的特防。', 0, 2, 1, 3, 18),
       (598, 100, 0, 0, 0, 'Lowers the target’s Special Attack by two stages.', null, 0, 0, 'eerie-impulse', null, null,
        null, null, '怪异电波', null, 15, 0, 'Lowers the target’s Special Attack by two stages.', 0, '从身体放射出怪异电波，
让对手沐浴其中，
从而大幅降低其特攻。', 0, 2, 1, 10, 13),
       (599, 100, 0, 0, 0, 'Lowers the target’s Attack, Special Attack, and Speed by one stage if it is poisoned.', 100,
        0, 0, 'venom-drench', null, null, null, null, '毒液陷阱', null, 20, 0,
        'Lowers the target’s Attack, Special Attack, and Speed by one stage if it is poisoned.', 100, '将特殊的毒液泼向对手。
对处于中毒状态的对手，
其攻击、特攻和速度都会降低。', 0, 2, 1, 11, 4),
       (600, 100, 0, 0, 0,
        'Explodes if the target uses a fire move this turn, damaging it for 1/4 its max HP and preventing the move.',
        null, 0, 0, 'powder', null, null, null, null, '粉尘', null, 20, 1,
        'Explodes if the target uses a fire move this turn, damaging it for 1/4 its max HP and preventing the move.', 0, '如果被撒到粉尘的对手
使用火招式，
则会爆炸并给予伤害。', 0, 13, 1, 10, 7),
       (601, null, 0, 0, 0,
        'Takes one turn to charge, then raises the user’s Special Attack, Special Defense, and Speed by two stages.',
        null, 0, 0, 'geomancy', null, null, null, null, '大地掌控', null, 10, 0,
        'Takes one turn to charge, then raises the user’s Special Attack, Special Defense, and Speed by two stages.', 0, '第１回合吸收能量，
第２回合大幅提高
特攻、特防和速度。', 0, 2, 1, 7, 18),
       (602, null, 0, 0, 0,
        'Raises the Defense and Special Defense of all friendly Pokémon with plus or minus by one stage.', null, 0, 0,
        'magnetic-flux', null, null, null, null, '磁场操控', null, 20, 0,
        'Raises the Defense and Special Defense of all friendly Pokémon with plus or minus by one stage.', 0, '通过操控磁场，
会提高特性为正电和负电的
宝可梦的防御和特防。', 0, 2, 1, 13, 13),
       (603, null, 0, 0, 0, 'Doubles prize money.

Stacks with a held item.  Only works once per battle.', null, 0, 0, 'happy-hour', null, null, null, null, '欢乐时光',
        null, 30, 0, 'Doubles prize money.', 0, '如果使用欢乐时光，
战斗后得到的钱会翻倍。', 0, 13, 1, 4, 1),
       (604, null, 0, 0, 0, 'For five turns, prevents all Pokémon on the ground from sleeping and strengthens their electric moves to 1.5× their power.

Changes nature power to thunderbolt.', null, 0, 0, 'electric-terrain', null, null, null, null, '电气场地', null, 10, 0,
        'For five turns, prevents all Pokémon on the ground from sleeping and strengthens their electric moves to 1.5× their power.',
        0, '在５回合内变成电气场地。
地面上的宝可梦将无法入眠。
电属性的招式威力还会提高。', 0, 10, 1, 12, 13),
       (605, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dazzling-gleam', null, null, null, null, '魔法闪耀',
        80, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '向对手发射强光，
并给予伤害。', 0, 0, 3, 11, 18),
       (606, null, 0, 0, 0, 'Does nothing.', null, 0, 0, 'celebrate', null, null, null, null, '庆祝', null, 40, 0,
        'Does nothing.', 0, '宝可梦为十分
开心的你庆祝。', 0, 13, 1, 7, 1),
       (607, null, 0, 0, 0, 'Does nothing.', null, 0, 0, 'hold-hands', null, null, null, null, '牵手', null, 40, 0,
        'Does nothing.', 0, '我方宝可梦之间牵手。
能带来非常幸福的心情。', 0, 13, 1, 3, 1),
       (608, 100, 0, 0, 0, 'Lowers the target’s Attack by one stage.', null, 0, 0, 'baby-doll-eyes', null, null, null,
        null, '圆瞳', null, 30, 1, 'Lowers the target’s Attack by one stage.', 0, '用圆瞳凝视对手，
从而降低其攻击。
必定能够先制攻击。', 0, 2, 1, 10, 18),
       (609, 100, 100, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 100, 0,
        0, 'nuzzle', null, null, null, null, '蹭蹭脸颊', 20, 20, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '将带电的脸颊
蹭蹭对手进行攻击。
让对手陷入麻痹状态。', 1, 4, 2, 10, 13),
       (610, 100, 0, 0, 0, 'Inflicts regular damage.  Will not reduce the target’s HP below 1.', null, 0, 0,
        'hold-back', null, null, null, null, '手下留情', 40, 40, 0, 'Cannot lower the target’s HP below 1.', 0, '在攻击的时候手下留情，
从而使对手的ＨＰ
至少会留下１ＨＰ。', 0, 0, 2, 10, 1),
       (611, 100, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'infestation', null, 5, null, 4, '死缠烂打', 20, 20, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '在４～５回合内
死缠烂打地进行攻击。
在此期间对手将无法逃走。', 8, 4, 3, 10, 7),
       (612, 100, 0, 0, 0, 'Inflicts regular damage.  Raises the user’s Attack by one stage.', 100, 0, 0,
        'power-up-punch', null, null, null, null, '增强拳', 40, 20, 0,
        'Raises the user’s Attack by one stage after inflicting damage.', 100, '通过反复击打对手，
使自己的拳头慢慢变硬。
打中对手攻击就会提高。', 0, 7, 2, 10, 2),
       (613, 100, 0, 0, 75, 'Deals regular damage.  Drains 75% of the damage inflicted to heal the user.', null, 0, 0,
        'oblivion-wing', null, null, null, null, '死亡之翼', 80, 10, 0,
        'Drains 75% of the damage inflicted to heal the user.', 0, '从锁定的对手身上吸取ＨＰ。
回复给予对手
伤害的一半以上的ＨＰ。', 0, 8, 3, 10, 3),
       (614, 100, 100, 0, 0,
        'Inflicts regular damage.  Grounds the target until it leaves battle.  Ignores levitation effects and the immunity of flying-type Pokémon.',
        100, 0, 0, 'thousand-arrows', null, null, null, null, '千箭齐发', 90, 10, 0,
        'Grounds the target, and hits even Flying-type or levitating Pokémon.', 0, '可以击中浮在空中的宝可梦。
空中的对手被击落后，
会掉到地面。', -1, 0, 2, 11, 5),
       (615, 100, 0, 0, 0, 'Inflicts regular damage.  Traps the target.', null, 0, 0, 'thousand-waves', null, null,
        null, null, '千波激荡', 90, 10, 0, 'Prevents the target from leaving battle.', 0, '从地面掀起波浪进行攻击。
被掀入波浪中的对手，
将无法从战斗中逃走。', 0, 0, 2, 11, 5),
       (616, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'lands-wrath', null, null, null, null, '大地神力',
        90, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '聚集大地的力量，
将此力量集中攻击对手，
并给予伤害。', 0, 0, 2, 11, 5),
       (617, 90, 0, 0, -50, 'Inflicts regular damage.  User takes 1/2 the damage it inflicts in recoil.', null, 0, 0,
        'light-of-ruin', null, null, null, null, '破灭之光', 140, 5, 0,
        'User receives 1/2 the damage inflicted in recoil.', 0, '借用永恒之花的力量，
发射出强力光线。
自己也会受到不小的伤害。', 0, 0, 3, 10, 18),
       (618, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'origin-pulse', null, null, null, null, '根源波动',
        110, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '用无数青白色
且闪耀的光线攻击对手。', 0, 0, 3, 11, 11),
       (619, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'precipice-blades', null, null, null, null,
        '断崖之剑', 120, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '将大地的力量变化为利刃
攻击对手。', 0, 0, 2, 11, 5),
       (620, 100, 0, 0, 0,
        'Inflicts regular damage, then lowers the user’s Defense and Special Defense by one stage each.', 100, 0, 0,
        'dragon-ascent', null, null, null, null, '画龙点睛', 120, 5, 0,
        'Lowers the user’s Defense and Special Defense by one stage after inflicting damage.', 100, '从天空中急速下降攻击对手。
自己的防御和特防会降低。', 0, 7, 2, 10, 3),
       (621, null, 0, 0, 0, 'Inflicts regular damage.  Bypasses and removes any protection effect on the target.', 100,
        0, 0, 'hyperspace-fury', null, null, null, null, '异次元猛攻', 100, 5, 0,
        'Ignores and destroys protection effects.', 100, '用许多手臂，无视对手的
守住或看穿等招式进行连续攻击，
自己的防御会降低。', 0, 7, 2, 10, 17),
       (622, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'breakneck-blitz--physical', null, null, null, null,
        '究极无敌大冲撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量气势猛烈地
全力撞上对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 1),
       (623, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'breakneck-blitz--special', null, null, null, null,
        '究极无敌大冲撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 1),
       (624, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'all-out-pummeling--physical', null, null, null,
        null, '全力无双激烈拳', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量制造出能量弹，
全力撞向对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 2),
       (625, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'all-out-pummeling--special', null, null, null,
        null, '全力无双激烈拳', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 2),
       (626, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'supersonic-skystrike--physical', null, null, null,
        null, '极速俯冲轰烈撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量猛烈地飞向天空，
朝对手全力落下。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 3),
       (627, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'supersonic-skystrike--special', null, null, null,
        null, '极速俯冲轰烈撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 3),
       (628, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'acid-downpour--physical', null, null, null, null,
        '强酸剧毒灭绝雨', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量使毒沼涌起，
全力让对手沉下去。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 4),
       (629, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'acid-downpour--special', null, null, null, null,
        '强酸剧毒灭绝雨', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 4),
       (630, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'tectonic-rage--physical', null, null, null, null,
        '地隆啸天大终结', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量潜入地里最深处，
全力撞上对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 5),
       (631, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'tectonic-rage--special', null, null, null, null,
        '地隆啸天大终结', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 5),
       (632, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'continental-crush--physical', null, null, null,
        null, '毁天灭地巨岩坠', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量召唤大大的岩山，
全力撞向对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 6),
       (633, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'continental-crush--special', null, null, null,
        null, '毁天灭地巨岩坠', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 6),
       (634, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'savage-spin-out--physical', null, null, null, null,
        '绝对捕食回旋斩', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量将吐出的丝线
全力束缚对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 7),
       (635, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'savage-spin-out--special', null, null, null, null,
        '绝对捕食回旋斩', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 7),
       (636, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'never-ending-nightmare--physical', null, null,
        null, null, '无尽暗夜之诱惑', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量召唤强烈的怨念，
全力降临到对手身上。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 8),
       (637, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'never-ending-nightmare--special', null, null, null,
        null, '无尽暗夜之诱惑', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 8),
       (638, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'corkscrew-crash--physical', null, null, null, null,
        '超绝螺旋连击', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量进行高速旋转，
全力撞上对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 9),
       (639, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'corkscrew-crash--special', null, null, null, null,
        '超绝螺旋连击', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 9),
       (640, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'inferno-overdrive--physical', null, null, null,
        null, '超强极限爆焰弹', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量喷出熊熊烈火，
全力撞向对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 10),
       (641, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'inferno-overdrive--special', null, null, null,
        null, '超强极限爆焰弹', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 10),
       (642, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'hydro-vortex--physical', null, null, null, null,
        '超级水流大漩涡', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量制造大大的潮旋，
全力吞没对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 11),
       (643, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'hydro-vortex--special', null, null, null, null,
        '超级水流大漩涡', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 11),
       (644, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'bloom-doom--physical', null, null, null, null,
        '绚烂缤纷花怒放', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量借助花草的能量，
全力攻击对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 12),
       (645, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'bloom-doom--special', null, null, null, null,
        '绚烂缤纷花怒放', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 12),
       (646, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'gigavolt-havoc--physical', null, null, null, null,
        '终极伏特狂雷闪', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量将蓄积的强大电流
全力撞向对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 13),
       (647, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'gigavolt-havoc--special', null, null, null, null,
        '终极伏特狂雷闪', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 13),
       (648, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'shattered-psyche--physical', null, null, null,
        null, '至高精神破坏波', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量操纵对手，
全力使其感受到痛苦。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 14),
       (649, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'shattered-psyche--special', null, null, null, null,
        '至高精神破坏波', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 14),
       (650, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'subzero-slammer--physical', null, null, null, null,
        '激狂大地万里冰', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量急剧降低气温，
全力冰冻对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 15),
       (651, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'subzero-slammer--special', null, null, null, null,
        '激狂大地万里冰', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 15),
       (652, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'devastating-drake--physical', null, null, null,
        null, '究极巨龙震天地', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量将气场实体化，
向对手全力发动袭击。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 16),
       (653, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'devastating-drake--special', null, null, null,
        null, '究极巨龙震天地', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 16),
       (654, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'black-hole-eclipse--physical', null, null, null,
        null, '黑洞吞噬万物灭', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量收集恶能量，
全力将对手吸入。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 17),
       (655, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'black-hole-eclipse--special', null, null, null,
        null, '黑洞吞噬万物灭', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0,
        0, 3, 10, 17),
       (656, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'twinkle-tackle--physical', null, null, null, null,
        '可爱星星飞天撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量制造魅惑空间，
全力捉弄对手。
威力会根据原来的招式而改变。', 0, 0, 2, 10, 18),
       (657, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'twinkle-tackle--special', null, null, null, null,
        '可爱星星飞天撞', null, 1, 0, 'Inflicts regular damage with no additional effect.', 0, 'ダミーデータ', 0, 0, 3,
        10, 18),
       (658, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'catastropika', null, null, null, null,
        '皮卡皮卡必杀击', 210, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量，
皮卡丘全身覆盖最强电力，
全力猛扑对手。', 0, 0, 2, 10, 13),
       (659, null, 0, 0, 0, 'Heals the user for ½ its max HP.  During a sandstorm, the healing is increased to ⅔.',
        null, 0, 50, 'shore-up', null, null, null, null, '集沙', null, 5, 0,
        'Heals the user for ½ its max HP, or ⅔ during a sandstorm.', 0, '回复自己最大ＨＰ的一半。
在沙暴中回复得更多。', 0, 3, 1, 7, 5),
       (660, 100, 0, 0, 0,
        'Inflicts regular damage. Can only be used on the user’s first turn after entering the field.', null, 0, 0,
        'first-impression', null, null, null, null, '迎头一击', 90, 10, 2,
        'Can only be used as the first move after the user enters battle.', 0, '威力很高的招式，
但只有在出场战斗时，
立刻使出才能成功。', 0, 0, 2, 10, 7),
       (661, null, 0, 0, 0,
        'Grants the user protection for the rest of the turn.  If a Pokémon attempts to use a move that makes contact with the user, that Pokémon will be poisoned.  This move’s chance of success halves every time it’s used consecutively with any other protection move.',
        null, 0, 0, 'baneful-bunker', null, null, null, null, '碉堡', null, 10, 4,
        'Grants the user protection for the rest of the turn and poisons any Pokémon that tries to use a contact move on it.',
        0, '防住对手攻击的同时，
让接触到自己的对手中毒。', 0, 13, 1, 7, 4),
       (662, 100, 0, 0, 0, 'Inflicts regular damage.  Traps the target.', null, 0, 0, 'spirit-shackle', null, null,
        null, null, '缝影', 80, 10, 0, 'Traps the target.', 0, '攻击的同时，
缝住对手的影子，
使其无法逃走。', 0, 0, 2, 10, 8),
       (663, 100, 0, 0, 0,
        'Inflicts regular damage.  Damage calculation ignores the target’s stat modifiers, including evasion.', null, 0,
        0, 'darkest-lariat', null, null, null, null, 'ＤＤ金勾臂', 85, 10, 0, 'Ignores the target’s stat modifiers.', 0, '旋转双臂打向对手。
无视对手的能力变化，
直接给予伤害。', 0, 0, 2, 10, 17),
       (664, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target has a burn and takes damage from this move, its burn is healed.', null,
        0, 0, 'sparkling-aria', null, null, null, null, '泡影的咏叹调', 90, 10, 0, 'Cures the target of burns.', 0, '随着唱歌会放出很多气球。
受到此招式攻击时，
灼伤会被治愈。', 0, 0, 3, 9, 11),
       (665, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Speed by one stage.', 100, 0, 0,
        'ice-hammer', null, null, null, null, '冰锤', 100, 10, 0, 'Lowers user’s Speed by one stage.', 100, '挥舞强力而沉重的拳头，
给予对手伤害。
自己的速度会降低。', 0, 7, 2, 10, 15),
       (666, null, 0, 0, 0, 'Heals the target for ½ its max HP.  If grassy terrain is in effect, heals for ⅔ instead.',
        null, 0, 50, 'floral-healing', null, null, null, null, '花疗', null, 10, 0,
        'Heals the target for ½ its max HP, or ⅔ on Grassy Terrain.', 0, '回复对手最大ＨＰ的一半。
在青草场地时，效果会提高。', 0, 3, 1, 10, 18),
       (667, 95, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'high-horsepower', null, null, null, null, '十万马力',
        95, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '使出全身力量，
猛攻对手。', 0, 0, 2, 10, 5),
       (668, 100, 0, 0, 0, 'Lowers the target’s Attack by one stage.  Heals the user by the target’s current Attack, including modifiers, but not including this move’s Attack-lowering effect.

If the target’s Attack is already at -6, this move will fail.  In any other situation that would prevent a stat modification, the healing will still succeed.',
        100, 0, 0, 'strength-sap', null, null, null, null, '吸取力量', null, 10, 0,
        'Heals the user by the target’s current Attack stat and lowers the target’s Attack by one stage.', 100, '给自己回复和对手攻击力
相同数值的ＨＰ，
然后降低对手的攻击。', 0, 13, 1, 10, 12),
       (669, 100, 0, 0, 0, 'Inflicts regular damage.  User charges for one turn before attacking.

During sunny day, the charge turn is skipped.

During hail, rain dance, or sandstorm, power is halved.

This move cannot be selected by sleep talk.', null, 0, 0, 'solar-blade', null, null, null, null, '日光刃', 125, 10, 0,
        'Requires a turn to charge before attacking.', 0, '第１回合收集满满的日光，
第２回合将此力量
集中在剑上进行攻击。', 0, 0, 2, 10, 12),
       (670, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'leafage', null, null, null, null, '树叶', 40, 40, 0,
        'Inflicts regular damage with no additional effect.', 0, '将叶片打向对手，
进行攻击。', 0, 0, 2, 10, 12),
       (671, null, 0, 0, 0,
        'For the duration of the turn, if the target is also a valid target for a move used by one of its opponents, that move will target it instead.',
        null, 0, 0, 'spotlight', null, null, null, null, '聚光灯', null, 15, 3,
        'Forces the target’s opponents to aim at the target for the rest of the turn.', 0, '给宝可梦打上聚光灯，
该回合只能瞄准该宝可梦。', 0, 13, 1, 10, 1),
       (672, 100, 100, 0, 0, 'Poisons the target and lowers its Speed by one stage.', 100, 0, 0, 'toxic-thread', null,
        null, null, null, '毒丝', null, 20, 0, 'Poisons the target and lowers its Speed by one stage.', 100, '将混有毒的丝吐向对手。
使其中毒，
从而降低对手的速度。', 5, 5, 1, 10, 4),
       (673, null, 0, 0, 0, 'The user’s next move will result in a critical hit.', null, 0, 0, 'laser-focus', null,
        null, null, null, '磨砺', null, 30, 0, 'Guarantees a critical hit with the user’s next move.', 0, '集中精神，
下次攻击必定会击中要害。', 0, 13, 1, 7, 1),
       (674, null, 0, 0, 0,
        'Raises the Attack and Special Attack of all friendly Pokémon with plus or minus by one stage.', null, 0, 0,
        'gear-up', null, null, null, null, '辅助齿轮', null, 20, 0,
        'Raises the Attack and Special Attack of all friendly Pokémon with plus or minus by one stage.', 0, '启动齿轮，
提高特性为正电和负电的
宝可梦的攻击和特攻。', 0, 2, 1, 13, 9),
       (675, 100, 100, 0, 0,
        'Inflicts regular damage.  Silences the target for two turns, preventing it from using any sound-based moves.',
        100, 0, 0, 'throat-chop', null, 2, null, 2, '地狱突刺', 80, 15, 0,
        'Prevents the target from using sound-based moves for two turns.', 0, '受到此招式攻击的对手，
会因为地狱般的痛苦，在２回合内，
变得无法使出声音类招式。', 24, 4, 2, 10, 17),
       (676, 100, 0, 0, 0,
        'If the target is an opponent, inflicts regular damage.  If the target is an ally, heals the target for 50% of its max HP.',
        null, 0, 0, 'pollen-puff', null, null, null, null, '花粉团', 90, 15, 0,
        'Damages opponents, but heals allies for 50% of their max HP.', 0, '对敌人使用是会爆炸的团子。
对我方使用则是给予回复的团子。', 0, 0, 3, 10, 7),
       (677, 100, 0, 0, 0, 'Inflicts regular damage.  Traps the target.', null, 0, 0, 'anchor-shot', null, null, null,
        null, '掷锚', 80, 20, 0, 'Traps the target.', 0, '将锚缠住对手进行攻击。
使对手无法逃走。', 0, 0, 2, 10, 9),
       (678, null, 0, 0, 0, 'Changes the terrain to Psychic Terrain for 5 turns.  Overrides electric terrain, grassy terrain, and misty terrain.

All Pokémon on the ground are immune to moves with priority greater than 0.  (Moves that target the field rather than individual Pokémon, such as spikes, are not affected.)  Additionally, when a Pokémon on the ground uses a psychic-type move, that move’s power is increased to 1.5×.

If a Pokémon is holding a Terrain Extender when creating Psychic Terrain (by any means), the effect lasts for 8 turns instead of 5.',
        null, 0, 0, 'psychic-terrain', null, null, null, null, '精神场地', null, 10, 0,
        'Protects Pokémon on the ground from priority moves and increases the power of their  Psychic moves by 50%.', 0, '在５回合内，地面上的宝可梦
不会受到先制招式的攻击。
超能力属性的招式威力会提高。', 0, 10, 1, 12, 14),
       (679, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Attack by one stage.', 100, 0, 0, 'lunge',
        null, null, null, null, '猛扑', 80, 15, 0, 'Lowers the target’s Attack by one stage after inflicting damage.',
        100, '全力猛扑对手进行攻击。
从而降低对手的攻击。', 0, 6, 2, 10, 7),
       (680, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Defense by one stage.', 100, 0, 0,
        'fire-lash', null, null, null, null, '火焰鞭', 80, 15, 0,
        'Lowers the target’s Defense by one stage after inflicting damage.', 100, '用燃烧的鞭子抽打对手。
受到攻击的对手防御会降低。', 0, 6, 2, 10, 10),
       (681, 100, 0, 0, 0,
        'Inflicts regular damage.  Power is increased by 100% its original value for every stage any of the user’s stats have been raised.  Accuracy, evasion, and lowered stats do not affect this move’s power.  For a Pokémon with all five stats modified to +6, this move’s power is 31×.',
        null, 0, 0, 'power-trip', null, null, null, null, '嚣张', 20, 10, 0,
        'Power is higher the more the user’s stats have been raised, to a maximum of 31×.', 0, '耀武扬威地攻击对手，
自己的能力提高得越多，
威力就越大。', 0, 0, 2, 10, 17),
       (682, 100, 0, 0, 0,
        'Inflicts regular damage.  Removes the user’s fire type after damage calculation.  If the user is not fire-type, this move will fail.',
        null, 0, 0, 'burn-up', null, null, null, null, '燃尽', 130, 5, 0,
        'Removes the user’s fire type after inflicting damage.', 0, '将自己全身燃烧起火焰来，
给予对手大大的伤害。
自己的火属性将会消失。', 0, 0, 3, 10, 10),
       (683, null, 0, 0, 0, 'Exchanges the original Speed stats of the user and target.', null, 0, 0, 'speed-swap',
        null, null, null, null, '速度互换', null, 10, 0, 'Exchanges the user’s Speed with the target’s.', 0, '将对手和自己的速度
进行互换。', 0, 13, 1, 10, 14),
       (684, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'smart-strike', null, null, null, null, '修长之角', 70, 10, 0, 'Never misses.', 0, '用尖尖的角刺入对手进行攻击。
攻击必定会命中。', 0, 0, 2, 10, 9),
       (685, null, 0, 0, 0,
        'Cures the target of its major status ailment and heals the user for 50% of its max HP.  If the target has no major status ailment, this move will fail.',
        null, 0, 50, 'purify', null, null, null, null, '净化', null, 20, 0,
        'Cures the target of a major status ailment and heals the user for 50% of its max HP.', 0, '治愈对手的异常状态。
治愈后可以回复自己的ＨＰ。', 0, 13, 1, 10, 4),
       (686, 100, 0, 0, 0,
        'Inflicts regular damage.  This move’s type matches the user’s first type, if any; otherwise, it’s typeless.',
        null, 0, 0, 'revelation-dance', null, null, null, null, '觉醒之舞', 90, 15, 0, 'Has the same type as the user.',
        0, '全力跳舞进行攻击。
此招式的属性将
变得和自己的属性相同。', 0, 0, 3, 10, 1),
       (687, 100, 0, 0, 0,
        'Inflicts regular damage.  If the target has already moved this turn, its ability is nullified.', null, 0, 0,
        'core-enforcer', null, null, null, null, '核心惩罚者', 100, 10, 0,
        'Nullifies the target’s ability if it moves earlier.', 0, '如果给予过伤害的对手
已经结束行动，
其特性就会被消除。', 0, 0, 3, 11, 16),
       (688, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Attack by one stage.', 100, 0, 0, 'trop-kick',
        null, null, null, null, '热带踢', 70, 15, 0, 'Lowers the target’s Attack by one stage after inflicting damage.',
        100, '向对手使出来自南国的火热脚踢。
从而降低对手的攻击。', 0, 6, 2, 10, 12),
       (689, null, 0, 0, 0,
        'The target immediately uses its most recently-used move.  This is independent of the target’s normal action for the turn (i.e., it may end up moving twice), but otherwise functions as usual, including deduction of PP.  This effect works for disabled moves and ignores torment.',
        null, 0, 0, 'instruct', null, null, null, null, '号令', null, 15, 0,
        'Forces the target to repeat its last used move.', 0, '向对手下达指示，
让其再次使出刚才的招式。', 0, 13, 1, 10, 14),
       (690, 100, 0, 0, 0,
        'Begins charging at the start of the turn, then attacks as normal.  Any Pokémon that makes contact with the user while charging is burned.  The charging is not affected by accuracy, sleep, paralysis, or any other effect that would interfere with a move.',
        null, 0, 0, 'beak-blast', null, null, null, null, '鸟嘴加农炮', 100, 15, -3,
        'Inflicts a burn on any Pokémon that makes contact before the attack.', 0, '先加热鸟嘴后再进行攻击。
鸟嘴在加热时对手触碰的话，
就会使其灼伤。', 0, 0, 2, 10, 3),
       (691, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the user’s Defense by one stage.', 100, 0, 0,
        'clanging-scales', null, null, null, null, '鳞片噪音', 110, 5, 0,
        'Lowers the user’s Defense by one stage after inflicting damage.', 100, '摩擦全身鳞片，
发出响亮的声音进行攻击。
攻击后自己的防御会降低。', 0, 7, 3, 11, 16),
       (692, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dragon-hammer', null, null, null, null, '龙锤', 90,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '将身体当作锤子，
向对手发动袭击，
给予伤害。', 0, 0, 2, 10, 16),
       (693, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'brutal-swing', null, null, null, null, '狂舞挥打',
        60, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '用自己的身体狂舞挥打，
给予对手伤害。', 0, 0, 2, 9, 17),
       (694, null, 0, 0, 0, 'Places the Aurora Veil effect on the user’s side of the field for the next 5 turns.  If the weather is not hail, or the weather is disabled by the effects of cloud nine or air lock, this move will fail.

Any regular damage dealt to an affected Pokémon is reduced by ½.  (If there are multiple Pokémon on the affected field, the reduction is ⅓.)',
        null, 0, 0, 'aurora-veil', null, null, null, null, '极光幕', null, 20, 0,
        'Reduces damage five turns, but must be used during hail.', 0, '在５回合内减弱
物理和特殊的伤害。
只有冰雹时才能使出。', 0, 11, 1, 4, 15),
       (695, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'sinister-arrow-raid', null, null, null, null,
        '遮天蔽日暗影箭', 180, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量制造出
无数箭的狙射树枭
将全力射穿对手进行攻击。', 0, 0, 2, 10, 8),
       (696, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'malicious-moonsault', null, null, null, null,
        '极恶飞跃粉碎击', 180, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量得到
强壮肉体的炽焰咆哮虎
将全力撞向对手进行攻击。', 0, 0, 2, 10, 17),
       (697, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'oceanic-operetta', null, null, null, null,
        '海神庄严交响乐', 195, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量召唤
大量水的西狮海壬
将全力攻击对手。', 0, 0, 3, 10, 11),
       (698, null, 0, 0, 0, 'Inflicts direct damage equal to ¾ of the target’s remaining HP.', null, 0, 0,
        'guardian-of-alola', null, null, null, null, '巨人卫士・阿罗拉', null, 1, 0,
        'Damages the target for 75% of its remaining HP.', 0, '通过Ｚ力量得到阿罗拉之力的
土地神宝可梦将全力进行攻击。
对手的剩余ＨＰ会减少很多。', 0, 0, 3, 10, 18),
       (699, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'soul-stealing-7-star-strike', null, null, null,
        null, '七星夺魂腿', 195, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '得到Ｚ力量的玛夏多
将全力使出拳头和脚踢的
连续招式叩打对手。', 0, 0, 2, 10, 8),
       (700, null, 100, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.', 100, 0,
        0, 'stoked-sparksurfer', null, null, null, null, '驾雷驭电戏冲浪', 175, 1, 0,
        'Has a $effect_chance% chance to paralyze the target.', 0, '得到Ｚ力量的阿罗拉地区的
雷丘将全力进行攻击。
从而让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (701, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'pulverizing-pancake', null, null, null, null,
        '认真起来大爆击', 210, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '通过Ｚ力量使得认真起来的
卡比兽跃动巨大身躯，
全力向对手发动袭击。', 0, 0, 2, 10, 1),
       (702, null, 0, 0, 0,
        'Raises the user’s Attack, Defense, Special Attack, Special Defense, and Speed by two stages each.', 100, 0, 0,
        'extreme-evoboost', null, null, null, null, '九彩升华齐聚顶', null, 1, 0,
        'Raises all of the user’s stats by two stages.', 100, '得到Ｚ力量的伊布
将借助进化后伙伴们的力量，
大幅提高能力。', 0, 2, 1, 7, 1),
       (703, null, 0, 0, 0, 'Inflicts regular damage, then (if successful) changes the terrain to psychic terrain.',
        null, 0, 0, 'genesis-supernova', null, null, null, null, '起源超新星大爆炸', 185, 1, 0,
        'Changes the terrain to Psychic Terrain after inflicting damage.', 0, '得到Ｚ力量的梦幻
将全力攻击对手。
脚下会变成精神场地。', 0, 0, 3, 10, 14),
       (704, 100, 0, 0, 0,
        'Inflicts regular damage.  If the user was not yet hit by an opponent’s physical move this turn, this move will fail.',
        null, 0, 0, 'shell-trap', null, null, null, null, '陷阱甲壳', 150, 5, -3,
        'Only inflicts damage if the user was hit by a physical move this turn.', 0, '设下甲壳陷阱。
如果对手使出物理招式，
陷阱就会爆炸并给予对手伤害。', 0, 0, 3, 11, 10),
       (705, 90, 0, 0, 0, 'Inflicts regular damage, then lowers the user’s Special Attack by two stages.', 100, 0, 0,
        'fleur-cannon', null, null, null, null, '花朵加农炮', 130, 5, 0,
        'Lowers the user’s Special Attack by two stages after inflicting damage.', 100, '放出强力光束后，
自己的特攻会大幅降低。', 0, 7, 3, 10, 18),
       (706, 100, 0, 0, 0,
        'Destroys any light screen or reflect on the target’s side of the field, then inflicts regular damage.', null,
        0, 0, 'psychic-fangs', null, null, null, null, '精神之牙', 85, 10, 0, 'Destroys Reflect and Light Screen.', 0, '利用精神力量咬住对手进行攻击。
还可以破坏光墙和反射壁等。', 0, 0, 2, 10, 14),
       (707, 100, 0, 0, 0,
        'Inflicts regular damage.  Power is doubled if the user’s last move failed for any reason (i.e., produced the message "But it failed!") or was ineffective due to types.',
        null, 0, 0, 'stomping-tantrum', null, null, null, null, '跺脚', 75, 10, 0,
        'Has double power if the user’s last move failed.', 0, '化悔恨为力量进行攻击。
如果上一回合招式没有打中，
威力就会翻倍。', 0, 0, 2, 10, 5),
       (708, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, 0, 0,
        'shadow-bone', null, null, null, null, '暗影之骨', 85, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, '用附有灵魂的骨头
殴打对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 8),
       (709, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'accelerock', null, null, null, null, '冲岩', 40, 20,
        1, 'Inflicts regular damage with no additional effect.', 0, '迅速撞向对手进行攻击。
必定能够先制攻击。', 0, 0, 2, 10, 6),
       (710, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, 0, 0,
        'liquidation', null, null, null, null, '水流裂破', 85, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Defense by one stage.', 20, '用水之力量撞向对手进行攻击。
有时会降低对手的防御。', 0, 6, 2, 10, 11),
       (711, 100, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'prismatic-laser', null, null, null, null, '棱镜镭射', 160, 10, 0,
        'User foregoes its next turn to recharge.', 0, '用棱镜的力量发射强烈光线。
下一回合自己将无法动弹。', 0, 0, 3, 10, 14),
       (712, 100, 0, 0, 0,
        'Steals the target’s stat increases, then inflicts regular damage.  Will not steal stat increases that would put any of the user’s stats at more than +6; any excess is left on the target.  Stolen increases are affected by abilities as normal.',
        null, 0, 0, 'spectral-thief', null, null, null, null, '暗影偷盗', 90, 10, 0,
        'Steals the target’s stat increases, then inflicts damage.', 0, '潜入对手的影子进行攻击。
会夺取对手的能力提升。', 0, 0, 2, 10, 8),
       (713, 100, 0, 0, 0,
        'Inflicts regular damage.  Other Pokémon’s abilities cannot activate in response to this move.  In particular, it hits through disguise',
        null, 0, 0, 'sunsteel-strike', null, null, null, null, '流星闪冲', 100, 5, 0,
        'Cannot be disrupted by abilities.', 0, '以流星般的气势猛撞对手。
可以无视对手的特性进行攻击。', 0, 0, 2, 10, 9),
       (714, 100, 0, 0, 0,
        'Inflicts regular damage.  Other Pokémon’s abilities cannot activate in response to this move.  In particular, it hits through disguise',
        null, 0, 0, 'moongeist-beam', null, null, null, null, '暗影之光', 100, 5, 0,
        'Cannot be disrupted by abilities.', 0, '放出奇怪的光线攻击对手。
可以无视对手的特性进行攻击。', 0, 0, 3, 10, 8),
       (715, null, 0, 0, 0, 'Lowers the target’s Attack and Special Attack by one stage each.', 100, 0, 0,
        'tearful-look', null, null, null, null, '泪眼汪汪', null, 20, 0,
        'Lowers the target’s Attack and Special Attack by one stage.', 100, '变得泪眼汪汪，
让对手丧失斗志。
从而降低对手的攻击和特攻。', 0, 2, 1, 10, 1),
       (716, 100, 0, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to make the target flinch.', 30, 30,
        0, 'zing-zap', null, null, null, null, '麻麻刺刺', 80, 10, 0,
        'Has a $effect_chance% chance to make the target flinch.', 0, '撞向对手，并发出强电，
使其感到麻麻刺刺的。
有时会使对手畏缩。', 0, 0, 2, 10, 13),
       (717, 90, 0, 0, 0, 'Inflicts typeless damage equal to half the target’s remaining HP.', null, 0, 0,
        'natures-madness', null, null, null, null, '自然之怒', null, 10, 0,
        'Inflicts damage equal to half the target’s HP.', 0, '向对手释放自然之怒。
对手的ＨＰ会减半。', 0, 0, 3, 10, 18),
       (737, 95, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'baddy-bad', null, null, null, null, '坏坏领域', 80,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '恶行恶相地进行攻击。
制造一道能减弱对手
物理攻击的神奇墙壁。', 0, 0, 3, 10, 17),
       (909, 100, null, null, null, null, null, null, null, 'thunderclap', null, null, null, null, '迅雷', 70, 5, 1,
        null, null,
        'This move enables the user to attack first with a jolt of electricity. This move fails if the target is not readying an attack.',
        null, null, 3, 10, 13),
       (718, 100, 0, 0, 0, 'Inflicts regular damage.  If the user is holding a plate or a drive, this move’s type is the type corresponding to that item.

Note: This effect is technically shared by both techno blast and judgment; however, Techno Blast is only affected by drives, and Judgment is only affected by plates.',
        null, 0, 0, 'multi-attack', null, null, null, null, '多属性攻击', 120, 10, 0,
        'If the user is holding a appropriate plate or drive, the damage inflicted will match it.', 0, '一边覆盖高能量，
一边撞向对手进行攻击。
根据存储碟不同，属性会改变。', 0, 0, 2, 10, 1),
       (719, null, 0, 2, 0, 'Inflicts regular damage.', null, 0, 0, '10-000-000-volt-thunderbolt', null, null, null,
        null, '千万伏特', 195, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '戴着帽子的皮卡丘将通过Ｚ力量
增强的电击全力释放给对手。
容易击中要害。', 0, 0, 3, 10, 13),
       (720, 100, 0, 0, 0,
        'Inflicts damage, and the user takes damage equal to half of its max HP, rounded up. The user still takes damage if the move is blocked by Protect or Substitute, misses, or if the target has Flash Fire.',
        null, 0, 0, 'mind-blown', null, null, null, null, '惊爆大头', 150, 5, 0,
        'Inflicts damage, and the user takes damage equal to half of its max HP, rounded up.', 0, '让自己的头爆炸，
来攻击周围的一切。
自己也会受到伤害。', 0, 0, 3, 9, 10),
       (721, 100, 0, 0, 0,
        'After inflicting damage, all Normal-type moves become Electric-type for the remainder of the turn, including status moves. This effect is applied after move type-changing abilities, such as Pixilate and Normalize.',
        null, 0, 0, 'plasma-fists', null, null, null, null, '等离子闪电拳', 100, 15, 0,
        'After inflicting damage, causes all Normal-type moves to become Electric-type for the remainder of the turn.',
        0, '用覆盖着电流的拳头进行攻击。
使一般属性的招式变成电属性。', 0, 0, 2, 10, 13),
       (722, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'photon-geyser', null, null, null, null, '光子喷涌',
        100, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '用光柱来进行攻击。
比较自己的攻击和特攻，
用数值相对较高的一项给予对方伤害。', 0, 0, 3, 10, 14),
       (723, null, 0, 0, 0,
        'Inflicts damage using either Attack or Special Attack stat, whichever is higher. Determining which stat is used takes into account stat changes but not held items or abilities.',
        null, 0, 0, 'light-that-burns-the-sky', null, null, null, null, '焚天灭世炽光爆', 200, 1, 0,
        'Inflicts damage using either Attack or Special Attack stat, whichever is higher.', 0, '奈克洛兹玛会无视对手的特性效果，
在攻击和特攻之间，
用数值相对较高的一项给予对方伤害。', 0, 0, 3, 10, 14),
       (724, null, 0, 0, 0,
        'Inflicts regular damage.  Other Pokémon’s abilities cannot activate in response to this move.  In particular, it hits through disguise',
        null, 0, 0, 'searing-sunraze-smash', null, null, null, null, '日光回旋下苍穹', 200, 1, 0,
        'Cannot be disrupted by abilities.', 0, '得到Ｚ力量的索尔迦雷欧
将全力进行攻击。
可以无视对手的特性效果。', 0, 0, 2, 10, 9),
       (725, null, 0, 0, 0,
        'Inflicts regular damage.  Other Pokémon’s abilities cannot activate in response to this move.  In particular, it hits through disguise',
        null, 0, 0, 'menacing-moonraze-maelstrom', null, null, null, null, '月华飞溅落灵霄', 200, 1, 0,
        'Cannot be disrupted by abilities.', 0, '得到Ｚ力量的露奈雅拉
将全力进行攻击。
可以无视对手的特性效果。', 0, 0, 3, 10, 8),
       (726, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'lets-snuggle-forever', null, null, null, null,
        '亲密无间大乱揍', 190, 1, 0, 'Inflicts regular damage with no additional effect.', 0, '得到Ｚ力量的谜拟Ｑ
将全力进行乱揍攻击。', 0, 0, 2, 10, 18),
       (727, null, 0, 0, 0, 'Inflicts damage and removes any terrain present on the battlefield.', null, 0, 0,
        'splintered-stormshards', null, null, null, null, '狼啸石牙飓风暴', 190, 1, 0,
        'Inflicts damage and removes any terrain present on the battlefield.', 0, '得到Ｚ力量的鬃岩狼人
将全力进行攻击。
而且会消除场地状态。', 0, 0, 2, 10, 6),
       (728, null, 0, 0, 0,
        'Inflicts sound-based damage to all opposing Pokémon and increases the user’s Attack, Defense, Special Attack, Special Defense, and Speed by one stage each. The user’s stats are not raised if the move fails to damage any opposing Pokémon.',
        100, 0, 0, 'clangorous-soulblaze', null, null, null, null, '炽魂热舞烈音爆', 185, 1, 0,
        'Inflicts damage to all opposing Pokémon and increases the user’s Attack, Defense, Special Attack, Special Defense, and Speed by one stage each.',
        100, '得到Ｚ力量的杖尾鳞甲龙
将全力攻击对手。
并且自己的能力会提高。', 0, 7, 3, 11, 16),
       (729, 100, 0, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'zippy-zap', null, null, null, null, '电电加速', 80,
        10, 2, 'Inflicts regular damage with no additional effect.', 100, '迅猛无比的电击。
必定能够先制攻击，
击中对方的要害。', 0, 7, 2, 10, 13),
       (730, 100, 30, 0, 0, 'Inflicts regular damage.', 30, 0, 0, 'splishy-splash', null, null, null, null, '滔滔冲浪',
        90, 15, 0, 'Inflicts regular damage with no additional effect.', 0, '往巨浪中注入电能后
冲撞对手进行攻击。
有时会让对手陷入麻痹状态。', 1, 4, 3, 11, 11),
       (731, 95, 0, 0, 0, 'Inflicts regular damage.', 30, 30, 0, 'floaty-fall', null, null, null, null, '飘飘坠落', 90,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '轻飘飘地浮起来后，
再猛地俯冲下去进行攻击。
有时会使对手畏缩。', 0, 0, 2, 10, 3),
       (732, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'pika-papow', null, null, null, null, '闪闪雷光',
        null, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '皮卡丘越喜欢训练家，
电击的威力就越强。
攻击必定会命中。', 0, 0, 3, 10, 13),
       (733, 100, 0, 0, 100, 'Inflicts regular damage.', null, 0, 0, 'bouncy-bubble', null, null, null, null,
        '活活气泡', 60, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '投掷水球进行攻击。
吸水后能回复等同于
造成的伤害一半的HP。', 0, 8, 3, 10, 11),
       (734, 100, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'buzzy-buzz', null, null, null, null, '麻麻电击',
        60, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '放出电击攻击对手。
让对手陷入麻痹状态。', 1, 4, 3, 10, 13),
       (735, 100, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'sizzly-slide', null, null, null, null, '熊熊火爆',
        60, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '用燃起大火的身体
猛烈地冲撞对手。
让对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (736, 95, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'glitzy-glow', null, null, null, null, '哗哗气场', 80,
        15, 0, 'Inflicts regular damage with no additional effect.', 0, '利用念力强攻，粉碎对方信心。
制造一道能减弱对手
特殊攻击的神奇墙壁。', 0, 0, 3, 10, 14),
       (786, 100, 0, 0, 0, null, null, 0, 0, 'overdrive', null, null, null, null, '破音', 80, 10, 0, null, 0, '奏响吉他和贝斯，释放出
发出巨响的剧烈震动
攻击对手。', 0, 0, 3, 11, 13),
       (738, 90, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'sappy-seed', null, null, null, null, '茁茁轰炸',
        100, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '长出巨大的藤蔓，
播撒种子进行攻击。
种子每回合都会吸取对手的HP。', 18, 4, 2, 10, 12),
       (739, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'freezy-frost', null, null, null, null, '冰冰霜冻',
        100, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '利用冰冷的黑雾结晶进行攻击。
使全体宝可梦的能力变回原点。', 0, 0, 3, 10, 15),
       (740, 85, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'sparkly-swirl', null, null, null, null, '亮亮风暴',
        120, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '利用芬芳刺鼻的龙卷风吞噬对方。
能治愈我方宝可梦的异常状态。', 0, 0, 3, 10, 18),
       (741, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'veevee-volley', null, null, null, null, '砰砰击破',
        null, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '伊布越喜欢训练家，
冲撞的威力就越强。
攻击必定会命中。', 0, 0, 2, 10, 1),
       (742, 100, 0, 0, 0, 'Inflicts regular damage.  Hits twice in one turn.', 30, 30, 0, 'double-iron-bash', 2, null,
        2, null, '钢拳双击', 60, 5, 0, 'Hits twice in one turn.', 0, '以胸口的螺帽为中心旋转，
并连续２次挥动手臂打击对手。
有时会使对手畏缩。', 0, 0, 2, 10, 9),
       (743, null, 0, 0, 0, 'No moves will hit the user for the remainder of this turn.  If the user is last to act this turn, this move will fail.

If the user successfully used detect, endure, protect, quick guard, or wide guard on the last turn, this move has a 50% chance to fail.

lock on, mind reader, and no guard provide a (100 – accuracy)% chance for moves to break through this move.  This does not apply to one-hit KO moves (fissure, guillotine, horn drill, and sheer cold); those are always blocked by this move.

thunder during rain dance and blizzard during hail have a 30% chance to break through this move.

The following effects are not prevented by this move:

* acupressure from an ally
* curse’s curse effect
* Delayed damage from doom desire and future sight; however, these moves will be prevented if they are used this turn
* feint, which will also end this move’s protection after it hits
* imprison
* perish song
* shadow force
* Moves that merely copy the user, such as transform or psych up

This move cannot be selected by assist or metronome.', null, 0, 0, 'max-guard', null, null, null, null, '极巨防壁',
        null, 10, 4, 'Prevents any moves from hitting the user this turn.', 0, '完全抵挡
对手的攻击。
连续使出则容易失败。', 0, 13, 1, 7, 1),
       (744, 100, 0, 0, 0,
        'Inflicts regular damage, then (if successful) sets light screen on the user’s side of the field.', null, 0, 0,
        'dynamax-cannon', null, null, null, null, '极巨炮', 100, 5, 0,
        'Sets Light Screen on the user’s side of the field after inflicting damage.', 0, '从核心放出光束进行攻击。
如果对手正处于极巨化状态，
则造成的伤害会变为２倍。', 0, 0, 3, 10, 16),
       (745, 100, 0, 1, 0,
        'Inflicts regular damage, then (if successful) sets reflect on the user’s side of the field.', null, 0, 0,
        'snipe-shot', null, null, null, null, '狙击', 80, 15, 0,
        'Sets Reflect on the user’s side of the field after inflicting damage.', 0, '能无视具有吸引对手招式效果的
特性或招式的影响。
可以向选定的对手进行攻击。', 0, 0, 3, 10, 11),
       (746, 100, 0, 0, 0, 'Inflicts regular damage, then (if successful) sets leech seed on the target.', null, 0, 0,
        'jaw-lock', null, null, null, null, '紧咬不放', 80, 10, 0, 'Seeds the target after inflicting damage.', 0, '使双方直到一方濒死为止
无法替换宝可梦。
其中一方退场则可以解除效果。', 0, 0, 2, 10, 17),
       (747, null, 0, 0, 0,
        'Inflicts regular damage, then (if successful) removes major status effects from every Pokémon in the user’s party.',
        100, 0, 0, 'stuff-cheeks', null, null, null, null, '大快朵颐', null, 10, 0,
        'Cures the entire party of major status effects after inflicting damage.', 100, '吃掉携带的树果，
大幅提高防御。', 0, 2, 1, 7, 1),
       (748, null, 0, 0, 0,
        'Inflicts regular damage.  Hits twice in one turn, with a $effect_chance% chance to make the target flinch.',
        100, 0, 0, 'no-retreat', null, null, null, null, '背水一战', null, 5, 0,
        'Hits twice in one turn, with a $effect_chance% chance to make the target flinch.', 100, '提高自己的所有能力，
但无法替换或逃走。', 0, 2, 1, 7, 2),
       (749, 100, 100, 0, 0, null, 100, 0, 0, 'tar-shot', null, null, null, null, '沥青射击', null, 15, 0, null, 100, '泼洒黏糊糊的沥青，
降低对手的速度，
并且使对手的弱点变为火。', 42, 5, 1, 10, 6),
       (750, 100, 0, 0, 0, null, null, 0, 0, 'magic-powder', null, null, null, null, '魔法粉', null, 20, 0, null, 0, '向对手喷洒魔法粉，
使对手变为超能力属性。', 0, 13, 1, 10, 14),
       (751, 100, 0, 0, 0, null, null, 0, 0, 'dragon-darts', 2, null, 2, null, '龙箭', 50, 10, 0, null, 0, '让多龙梅西亚进行２次攻击。
如果对手有２只宝可梦，
则对它们各进行１次攻击。', 0, 0, 2, 10, 16),
       (752, null, 0, 0, 0, null, null, 0, 0, 'teatime', null, null, null, null, '茶会', null, 10, 0, null, 0, '举办一场茶会，
场上的所有宝可梦都会
吃掉自己携带的树果。', 0, 13, 1, 14, 1),
       (753, 100, 0, 0, 0, null, null, 0, 0, 'octolock', null, null, null, null, '蛸固', null, 15, 0, null, 0, '让对手无法逃走。
对手被固定后，
每回合都会降低防御和特防。', 0, 13, 1, 10, 2),
       (754, 100, 0, 0, 0, null, null, 0, 0, 'bolt-beak', null, null, null, null, '电喙', 85, 10, 0, null, 0, '用带电的喙啄刺对手。
如果比对手先出手攻击，
招式的威力会变成２倍。', 0, 0, 2, 10, 13),
       (755, 100, 0, 0, 0, null, null, 0, 0, 'fishious-rend', null, null, null, null, '鳃咬', 85, 10, 0, null, 0, '用坚硬的腮咬住对手。
如果比对手先出手攻击，
招式的威力会变成２倍。', 0, 0, 2, 10, 11),
       (756, 100, 0, 0, 0, null, null, 0, 0, 'court-change', null, null, null, null, '换场', null, 10, 0, null, 0, '用神奇的力量
交换双方的场地效果。', 0, 13, 1, 12, 1),
       (757, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-flare', null, null, null, null, '极巨火爆',
        100, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的火属性攻击。
可在５回合内让日照变得强烈。', 0, 0, 2, 2, 10),
       (758, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-flutterby', null, null, null, null, '极巨虫蛊',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的虫属性攻击。
会降低对手的特攻。', 0, 0, 2, 2, 7),
       (759, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-lightning', null, null, null, null, '极巨闪电',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的电属性攻击。
可在５回合内将脚下变成电气场地。', 0, 0, 2, 2, 13),
       (760, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-strike', null, null, null, null, '极巨攻击',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的一般属性攻击。
会降低对手的速度。', 0, 0, 2, 2, 1),
       (761, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-knuckle', null, null, null, null, '极巨拳斗',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的格斗属性攻击。
会提高我方的攻击。', 0, 0, 2, 2, 2),
       (762, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-phantasm', null, null, null, null, '极巨幽魂',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的幽灵属性攻击。
会降低对手的防御。', 0, 0, 2, 2, 8),
       (763, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-hailstorm', null, null, null, null, '极巨寒冰',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦
才能使出的冰属性攻击。
在５回合内会降下冰雹。', 0, 0, 2, 2, 15),
       (764, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-ooze', null, null, null, null, '极巨酸毒', 10,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的毒属性攻击。
会提高我方的特攻。', 0, 0, 2, 2, 4),
       (765, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-geyser', null, null, null, null, '极巨水流',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的水属性攻击。
可在５回合内降下大雨。', 0, 0, 2, 2, 11),
       (766, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-airstream', null, null, null, null, '极巨飞冲',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的飞行属性攻击。
会提高我方的速度。', 0, 0, 2, 2, 3),
       (767, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-starfall', null, null, null, null, '极巨妖精',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的妖精属性攻击。
可在５回合内将脚下变成薄雾场地。', 0, 0, 2, 2, 18),
       (768, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-wyrmwind', null, null, null, null, '极巨龙骑',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的龙属性攻击。
会降低对手的攻击。', 0, 0, 2, 2, 16),
       (769, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-mindstorm', null, null, null, null, '极巨超能',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的超能力属性攻击。
可在５回合内将脚下变成精神场地。', 0, 0, 2, 2, 14),
       (770, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-rockfall', null, null, null, null, '极巨岩石',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的岩石属性攻击。
可在５回合内卷起沙暴。', 0, 0, 2, 2, 6),
       (771, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-quake', null, null, null, null, '极巨大地', 10,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的地面属性攻击。
会提高我方的特防。', 0, 0, 2, 2, 5),
       (772, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-darkness', null, null, null, null, '极巨恶霸',
        10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的恶属性攻击。
会降低对手的特防。', 0, 0, 2, 2, 17),
       (773, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-overgrowth', null, null, null, null,
        '极巨草原', 10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的草属性攻击。
可在５回合内将脚下变成青草场地。', 0, 0, 2, 2, 12),
       (774, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'max-steelspike', null, null, null, null,
        '极巨钢铁', 10, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '极巨化宝可梦使出的钢属性攻击。
会提高我方的防御。', 0, 0, 2, 2, 9),
       (775, 100, 0, 0, 0, null, 100, 0, -33, 'clangorous-soul', null, null, null, null, '魂舞烈音爆', null, 5, 0, null,
        100, '削减少许自己的ＨＰ，
使所有能力都提高。', 0, 2, 1, 7, 16),
       (776, 100, 0, 0, 0, null, null, 0, 0, 'body-press', null, null, null, null, '扑击', 80, 10, 0, null, 0, '用身体撞向对手进行攻击。
防御越高，
给予的伤害就越高。', 0, 0, 2, 10, 2),
       (777, null, 0, 0, 0, null, 100, 0, 0, 'decorate', null, null, null, null, '装饰', null, 15, 0, null, 100, '通过装饰，
大幅提高对方的
攻击和特攻。', 0, 2, 1, 10, 18),
       (778, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, 0, 0,
        'drum-beating', null, null, null, null, '鼓击', 80, 10, 0,
        'Has a $effect_chance% chance to lower the target’s Speed by one stage.', 100, '用鼓点来控制
鼓的根部进行攻击，
从而降低对手的速度。', 0, 6, 2, 10, 12),
       (779, 100, 100, 0, 0, 'Inflicts regular damage.  For the next 2–5 turns, the target cannot leave the field and is damaged for 1/16 its max HP at the end of each turn.  The user continues to use other moves during this time.  If the user leaves the field, this effect ends.

Has a 3/8 chance each to hit 2 or 3 times, and a 1/8 chance each to hit 4 or 5 times.  Averages to 3 hits per use.

rapid spin cancels this effect.', 100, 0, 0, 'snap-trap', null, 6, null, 5, '捕兽夹', 35, 15, 0,
        'Prevents the target from fleeing and inflicts damage for 2-5 turns.', 0, '使用捕兽夹，
在４～５回合内，
夹住对手进行攻击。', 8, 4, 2, 10, 12),
       (780, 90, 10, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10, 0, 0,
        'pyro-ball', null, null, null, null, '火焰球', 120, 5, 0, 'Has a $effect_chance% chance to burn the target.', 0, '点燃小石子，形成火球攻击对手。
有时会使对手陷入灼伤状态。', 4, 4, 2, 10, 10),
       (781, 100, 0, 0, 0, null, null, 0, 0, 'behemoth-blade', null, null, null, null, '巨兽斩', 100, 5, 0, null, 0, '变身为巨大的剑，挥斩对手。
如果对手正处于极巨化状态，
则造成的伤害会变为２倍。', 0, 0, 2, 10, 9),
       (782, 100, 0, 0, 0, null, null, 0, 0, 'behemoth-bash', null, null, null, null, '巨兽弹', 100, 5, 0, null, 0, '变身为巨大的盾，撞击对手。
如果对手正处于极巨化状态，
则造成的伤害会变为２倍。', 0, 0, 2, 10, 9),
       (783, 100, 0, 0, 0, null, 100, 0, 0, 'aura-wheel', null, null, null, null, '气场轮', 110, 10, 0, null, 100, '用储存在颊囊里的能量
进行攻击，并提高自己的速度。
其属性会随着莫鲁贝可的样子而改变。', 0, 7, 2, 10, 13),
       (784, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Attack by one stage.', 100, 0, 0,
        'breaking-swipe', null, null, null, null, '广域破坏', 60, 15, 0,
        'Lowers the target’s Attack by one stage after inflicting damage.', 100, '用坚韧的尾巴
猛扫对手进行攻击，
从而降低对手的攻击。', 0, 6, 2, 11, 16),
       (785, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'branch-poke', null, null, null, null, '木枝突刺',
        40, 40, 0, 'Inflicts regular damage with no additional effect.', 0, '使用尖锐的树枝
刺向对手进行攻击。', 0, 0, 2, 10, 12),
       (788, 100, 0, 0, 0, 'Inflicts regular damage.  Lowers the target’s Defense by one stage.', 100, 0, 0,
        'grav-apple', null, null, null, null, '万有引力', 80, 10, 0,
        'Lowers the target’s Defense by one stage after inflicting damage.', 100, '从高处落下苹果，
给予对手伤害。
可降低对手的防御。', 0, 6, 2, 10, 12),
       (789, 100, 0, 0, 0,
        'Inflicts regular damage.  Has a $effect_chance% chance to lower the target’s Special Attack by one stage.',
        100, 0, 0, 'spirit-break', null, null, null, null, '灵魂冲击', 75, 15, 0,
        'Has a $effect_chance% chance to lower the target’s Special Attack by one stage.', 100, '用足以让对手一蹶不振的
气势进行攻击。
会降低对手的特攻。', 0, 6, 2, 10, 18),
       (790, 95, 20, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to confuse the target.', 20, 0, 0,
        'strange-steam', null, 5, null, 2, '神奇蒸汽', 90, 10, 0, 'Has a $effect_chance% chance to confuse the target.',
        0, '喷出烟雾攻击对手。
有时会使对手混乱。', 6, 4, 3, 10, 18),
       (791, null, 0, 0, 0, null, null, 0, 25, 'life-dew', null, null, null, null, '生命水滴', null, 10, 0, null, 0, '喷洒出神奇的水，
回复自己和场上同伴的ＨＰ。', 0, 3, 1, 13, 11),
       (792, 100, 0, 0, 0, null, null, 0, 0, 'obstruct', null, null, null, null, '拦堵', null, 10, 4, null, 0, '完全抵挡对手的攻击。
连续使出则容易失败。
一旦触碰，防御就会大幅降低。', 0, 13, 1, 7, 17),
       (793, null, 0, 0, 0, 'Inflicts regular damage.  Ignores accuracy and evasion modifiers.', null, 0, 0,
        'false-surrender', null, null, null, null, '假跪真撞', 80, 10, 0, 'Never misses.', 0, '装作低头认错的样子，
用凌乱的头发进行突刺。
攻击必定会命中。', 0, 0, 2, 10, 17),
       (794, 100, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'meteor-assault', null, null, null, null, '流星突击', 150, 5, 0,
        'User foregoes its next turn to recharge.', 0, '大力挥舞粗壮的茎进行攻击。
但同时自己也会被晃晕，
下一回合自己将无法动弹。', 0, 0, 2, 10, 2),
       (795, 90, 0, 0, 0,
        'Inflicts regular damage.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, 0, 0, 'eternabeam', null, null, null, null, '无极光束', 160, 5, 0,
        'User foregoes its next turn to recharge.', 0, '无极汰那变回原来的样子后，
发动的最强攻击。
下一回合自己将无法动弹。', 0, 0, 3, 10, 16),
       (796, 95, 0, 0, 0,
        'Inflicts damage, and the user takes damage equal to half of its max HP, rounded up. The user still takes damage if the move is blocked by Protect or Substitute, misses, or if the target has Flash Fire.',
        null, 0, 0, 'steel-beam', null, null, null, null, '铁蹄光线', 140, 5, 0,
        'Inflicts damage, and the user takes damage equal to half of its max HP, rounded up.', 0, '将从全身聚集的钢铁
化为光束，激烈地发射出去。
自己也会受到伤害。', 0, 0, 3, 10, 9),
       (797, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'expanding-force', null, null, null, null,
        '广域战力', 80, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '利用精神力量攻击对手。
在精神场地上威力会有所提高，
能对所有对手造成伤害。', 0, 0, 3, 10, 14),
       (798, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'steel-roller', null, null, null, null, '铁滚轮',
        130, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '在破坏场地的同时攻击对手。
如果脚下没有任何场地状态存在，
使出此招式时便会失败。', 0, 0, 2, 10, 9),
       (799, 90, 0, 0, 0,
        'Inflicts regular damage two to five times in a row, raising the user’s Speed and lowering the user’s Defense by one stage each upon last hit.',
        null, 0, 0, 'scale-shot', 5, null, 2, null, '鳞射', 25, 20, 0,
        'Boosts the user’s Speed and lowers their Defense by one stage after inflicting damage two to five times in a row.',
        0, '发射鳞片进行攻击。
连续攻击２～５次。
速度会提高但防御会降低。', 0, 0, 2, 10, 16),
       (800, 90, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'meteor-beam', null, null, null, null, '流星光束',
        120, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '第１回合聚集宇宙之力提高特攻，
第２回合攻击对手。', 0, 0, 3, 10, 6),
       (801, 100, 20, 0, 0, 'Inflicts regular damage.', 20, 0, 0, 'shell-side-arm', null, null, null, null, '臂贝武器',
        90, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '从物理攻击和特殊攻击中选择
可造成较多伤害的方式进行攻击。
有时会让对手陷入中毒状态。', 5, 4, 3, 10, 4),
       (802, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'misty-explosion', null, null, null, null,
        '薄雾炸裂', 100, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '对自己周围的所有宝可梦进行攻击，
但使出后，自己会陷入濒死。
在薄雾场地上，招式威力会提高。', 0, 0, 3, 9, 18),
       (803, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'grassy-glide', null, null, null, null, '青草滑梯',
        55, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '仿佛在地面上滑行般地攻击对手。
在青草场地上，
必定能够先制攻击。', 0, 0, 2, 10, 12),
       (804, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'rising-voltage', null, null, null, null, '电力上升',
        70, 20, 0, 'Inflicts regular damage with no additional effect.', 0, '用从地面升腾而起的电击进行攻击。
当对手处于电气场地上时，
招式威力会变成２倍。', 0, 0, 3, 10, 13),
       (805, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'terrain-pulse', null, null, null, null, '大地波动',
        50, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '借助场地的力量进行攻击。
视使出招式时场地状态不同，
招式的属性和威力会有所变化。', 0, 0, 3, 10, 1),
       (806, 90, 0, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'skitter-smack', null, null, null, null, '爬击', 70,
        10, 0, 'Inflicts regular damage with no additional effect.', 100, '从对手背后爬近后进行攻击。
会降低对手的特攻。', 0, 6, 2, 10, 7),
       (807, 100, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'burning-jealousy', null, null, null, null, '妒火',
        70, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '用嫉妒的能量攻击对手。
会让在该回合内能力有所提高的
宝可梦陷入灼伤状态。', 4, 4, 3, 11, 10),
       (808, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'lash-out', null, null, null, null, '泄愤', 75, 5, 0,
        'Inflicts regular damage with no additional effect.', 0, '攻击对手以发泄对其感到的恼怒情绪。
如果在该回合内自身能力遭到降低，
招式的威力会变成２倍。', 0, 0, 2, 10, 17),
       (809, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'poltergeist', null, null, null, null, '灵骚', 110, 5,
        0, 'Inflicts regular damage with no additional effect.', 0, '操纵对手的持有物进行攻击。
当对手没有携带道具时，
使出此招式时便会失败。', 0, 0, 2, 10, 8),
       (810, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'corrosive-gas', null, null, null, null, '腐蚀气体',
        null, 40, 0, 'Inflicts regular damage with no additional effect.', 0, '用具有强酸性的气体
包裹住自己周围所有的宝可梦，
并融化其所携带的道具。', 0, 13, 1, 9, 4),
       (811, null, 0, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'coaching', null, null, null, null, '指导', null, 10,
        0, 'Inflicts regular damage with no additional effect.', 100, '通过进行正确合理的指导，
提高我方全员的攻击和防御。', 0, 2, 1, 13, 2),
       (812, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'flip-turn', null, null, null, null, '快速折返', 60,
        20, 0, 'Inflicts regular damage with no additional effect.', 0, '在攻击之后急速返回，
和后备宝可梦进行替换。', 0, 0, 2, 10, 11),
       (813, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'triple-axel', 3, null, 3, null, '三旋击', 20, 10, 0,
        'Inflicts regular damage with no additional effect.', 0, '连续３次踢对手进行攻击。
每踢中一次，威力就会提高。', 0, 0, 2, 10, 15),
       (814, 90, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dual-wingbeat', 2, null, 2, null, '双翼', 40, 10, 0,
        'Inflicts regular damage with no additional effect.', 0, '将翅膀撞向对手进行攻击。
连续２次给予伤害。', 0, 0, 2, 10, 3),
       (815, 100, 30, 0, 0, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 30, 0, 0,
        'scorching-sands', null, null, null, null, '热沙大地', 70, 10, 0,
        'Has a $effect_chance% chance to burn the target.', 0, '将滚烫的沙子砸向对手进行攻击。
有时会让对手陷入灼伤状态。', 4, 4, 3, 10, 5),
       (816, null, 0, 0, 0, 'Inflicts regular damage.', null, 0, 25, 'jungle-healing', null, null, null, null,
        '丛林治疗', null, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '与丛林融为一体，
回复自己和场上同伴的ＨＰ和状态。', 0, 13, 1, 13, 12),
       (817, 100, 0, 6, 0, 'Inflicts regular damage.', null, 0, 0, 'wicked-blow', null, null, null, null, '暗冥强击',
        75, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '将恶之流派修炼至大成的
猛烈一击。
必定会击中要害。', 0, 0, 2, 10, 17),
       (818, 100, 0, 6, 0, 'Inflicts regular damage.', null, 0, 0, 'surging-strikes', 3, null, 3, null, '水流连打', 25,
        5, 0, 'Inflicts regular damage with no additional effect.', 0, '将水之流派修炼至大成的
仿若行云流水般的３次连击。
必定会击中要害。', 0, 0, 2, 10, 11),
       (819, 90, 100, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'thunder-cage', null, 6, null, 5, '雷电囚笼', 80, 15,
        0, 'Inflicts regular damage with no additional effect.', 0, '将对手困在
电流四溅的囚笼中，
在４～５回合内进行攻击。', 8, 4, 3, 10, 13),
       (820, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'dragon-energy', null, null, null, null, '巨龙威能',
        150, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '把生命力转换为力量攻击对手。
自己的ＨＰ越少，招式的威力越小。', 0, 0, 3, 11, 16),
       (821, 100, 10, 0, 0, 'Inflicts regular damage.', 10, 0, 0, 'freezing-glare', null, null, null, null, '冰冷视线',
        90, 10, 0, 'Inflicts regular damage with no additional effect.', 0, '从双眼发射精神力量进行攻击。
有时会让对手陷入冰冻状态。', 3, 4, 3, 10, 14),
       (822, 100, 0, 0, 0, 'Inflicts regular damage.', 20, 20, 0, 'fiery-wrath', null, null, null, null, '怒火中烧', 90,
        10, 0, 'Inflicts regular damage with no additional effect.', 0, '将愤怒转化为火焰般的气场进行攻击。
有时会使对手畏缩。', 0, 0, 3, 11, 17),
       (823, 100, 0, 0, 0, 'Inflicts regular damage.', 100, 0, 0, 'thunderous-kick', null, null, null, null, '雷鸣蹴击',
        90, 10, 0, 'Inflicts regular damage with no additional effect.', 100, '以雷电般的动作
戏耍对手的同时使出脚踢。
可降低对手的防御。', 0, 6, 2, 10, 2),
       (824, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'glacial-lance', null, null, null, null, '雪矛', 120,
        5, 0, 'Inflicts regular damage with no additional effect.', 0, '向对手投掷
掀起暴风雪的冰矛进行攻击。', 0, 0, 2, 11, 15),
       (825, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'astral-barrage', null, null, null, null, '星碎',
        120, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '用大量的小灵体向
对手发起攻击。', 0, 0, 3, 11, 8),
       (826, 100, 0, 0, 0, 'Inflicts regular damage.', null, 0, 0, 'eerie-spell', null, null, null, null, '诡异咒语',
        80, 5, 0, 'Inflicts regular damage with no additional effect.', 0, '用强大的精神力量攻击。
让对手最后使用的招式
减少３ＰＰ。', 0, 0, 3, 10, 14),
       (827, 100, null, null, null, null, null, null, null, 'dire-claw', null, null, null, null, '克命爪', 80, 15, 0,
        null, null,
        'The user lashes out at the target with ruinous claws, aiming to land a critical hit. This may also leave the target poisoned, paralyzed, or drowsy.',
        null, null, 2, 10, 4),
       (828, 90, null, null, null, null, null, null, null, 'psyshield-bash', null, null, null, null, '屏障猛攻', 70, 10,
        0, null, null,
        'Cloaking itself in psychic energy, the user slams into the target. This may also raise the user’s defensive stats.',
        null, null, 2, 10, 14),
       (829, null, null, null, null, null, null, null, null, 'power-shift', null, null, null, null, '力量转换', 0, 10,
        0, null, null, null, null, null, 1, 7, 1),
       (830, 90, null, null, null, null, null, null, null, 'stone-axe', null, null, null, null, '岩斧', 65, 15, 0, null,
        null,
        'The user swings its stone axes at the target, aiming to land a critical hit. Stone splinters left behind by this attack continue to damage the target for several turns.',
        null, null, 2, 10, 6),
       (831, 80, null, null, null, null, null, null, null, 'springtide-storm', null, null, null, null, '阳春风暴', 100,
        5, 0, null, null,
        'The user attacks by wrapping the target in fierce winds brimming with love and hate. This move’s additional effects depend on the user’s form.',
        null, null, 3, 11, 18),
       (832, 90, null, null, null, null, null, null, null, 'mystical-power', null, null, null, null, '神秘之力', 70, 10,
        0, null, null,
        'The user strengthens itself with a mysterious power. If it excels in offense, its offensive stats are raised. If it excels in defense, its defensive stats are raised.',
        null, null, 3, 10, 14),
       (833, 100, null, null, null, null, null, null, null, 'raging-fury', null, null, null, null, '大愤慨', 120, 10, 0,
        null, null,
        'The user rampages and spews vicious flames to inflict damage on the target, then becomes fixated on using this move.',
        null, null, 2, 8, 10),
       (834, 100, null, null, null, null, null, null, null, 'wave-crash', null, null, null, null, '波动冲', 120, 10, 0,
        null, null,
        'The user shrouds itself in water and slams into the target with its whole body to inflict damage. This also damages the user and raises the user’s action speed.',
        null, null, 2, 10, 11),
       (835, 95, null, null, null, null, null, null, null, 'chloroblast', null, null, null, null, '叶绿爆震', 150, 5, 0,
        null, null,
        'The user launches its amassed chlorophyll to inflict damage on the target. This also damages the user and lowers the user''s action speed.',
        null, null, 3, 10, 12),
       (836, 85, null, null, null, null, null, null, null, 'mountain-gale', null, null, null, null, '冰山风', 100, 10,
        0, null, null, 'The user hurls giant chunks of ice at the target to inflict damage.', null, null, 2, 10, 15),
       (837, null, null, null, null, null, null, null, null, 'victory-dance', null, null, null, null, '胜利之舞', 0, 10,
        0, null, null,
        'The user performs a dance to usher in victory. This raises the user’s offensive and defensive stats and increases the damage dealt by the user’s moves by 50 percent.',
        null, null, 1, 7, 2),
       (839, 100, null, null, null, null, null, null, null, 'barb-barrage', null, null, null, null, '毒千针', 60, 10, 0,
        null, null,
        'The user launches countless toxic barbs to inflict damage. This may also poison the target. This move’s power is doubled if the target has a status condition.',
        null, null, 2, 10, 4),
       (840, 100, null, null, null, null, null, null, null, 'esper-wing', null, null, null, null, '气场之翼', 80, 10, 0,
        null, null,
        'The user slashes the target with aura-enriched wings. This also raises the user''s action speed. This move has a heightened chance of landing a critical hit.',
        null, null, 3, 10, 14),
       (841, 100, null, null, null, null, null, null, null, 'bitter-malice', null, null, null, null, '冤冤相报', 75, 10,
        0, null, null,
        'The user attacks its target with spine-chilling resentment. This may also leave the target with frostbite. This move''s power is doubled if the target has a status condition.',
        null, null, 3, 10, 8),
       (842, null, null, null, null, null, null, null, null, 'shelter', null, null, null, null, '闭关', 0, 10, 0, null,
        null,
        'The user makes its skin as hard as an iron shield, raising its defensive stats. Incoming moves also become more likely to miss.',
        null, null, 1, 7, 9),
       (843, 100, null, null, null, null, null, null, null, 'triple-arrows', null, null, null, null, '三连箭', 90, 10,
        0, null, null,
        'The user delivers an axe kick, then fires three arrows. This raises the chance of its future attacks landing critical hits and also lowers the target''s defensive stats.',
        null, null, 2, 10, 2),
       (844, 100, null, null, null, null, null, null, null, 'infernal-parade', null, null, null, null, '群魔乱舞', 60,
        15, 0, null, null,
        'The user attacks with myriad fireballs. This may also leave the target with a burn. This move''s power is doubled if the target has a status condition.',
        null, null, 3, 10, 8),
       (845, 90, null, null, null, null, null, null, null, 'ceaseless-edge', null, null, null, null, '秘剑・千重涛', 65,
        15, 0, null, null,
        'The user slashes its shell blade at the target, aiming to land a critical hit. Shell splinters left behind by this attack will continue to damage the target for several turns.',
        null, null, 2, 10, 17),
       (846, 80, null, null, null, null, null, null, null, 'bleakwind-storm', null, null, null, null, '枯叶风暴', 100,
        10, 0, null, null,
        'The user attacks with savagely cold winds that cause both body and spirit to tremble. This may also leave the target with frostbite.',
        null, null, 3, 11, 3),
       (847, 80, null, null, null, null, null, null, null, 'wildbolt-storm', null, null, null, null, '鸣雷风暴', 100,
        10, 0, null, null,
        'The user summons a thunderous tempest and savagely attacks with lightning and wind. This may also leave the target with paralysis.',
        null, null, 3, 11, 13),
       (848, 80, null, null, null, null, null, null, null, 'sandsear-storm', null, null, null, null, '热沙风暴', 100,
        10, 0, null, null,
        'The user attacks by wrapping the target in fierce winds and searingly hot sand. This also leaves the target with a burn.',
        null, null, 3, 11, 5),
       (849, null, null, null, null, null, null, null, null, 'lunar-blessing', null, null, null, null, '新月祈祷', 0, 5,
        0, null, null,
        'The user heals its own status conditions and restores its HP. Incoming moves also become more likely to miss.',
        null, null, 1, 15, 14),
       (850, null, null, null, null, null, null, null, null, 'take-heart', null, null, null, null, '勇气填充', 0, 10, 0,
        null, null,
        'The user lifts its spirits, healing its own status conditions and raising its offensive and defensive stats.',
        null, null, 1, 15, 14),
       (851, 100, null, null, null, null, null, null, null, 'tera-blast', null, null, null, null, '太晶爆发', 80, 10, 0,
        null, null,
        'If the user has Terastallized, it unleashes energy of its Tera Type. This move inflicts damage using the Attack or Sp. Atk stat—whichever is higher for the user.',
        null, null, 3, 10, 1),
       (852, null, null, null, null, null, null, null, null, 'silk-trap', null, null, null, null, '线阱', 0, 10, 4,
        null, null,
        'The user spins a silken trap, protecting itself from damage while lowering the Speed stat of any attacker that makes direct contact.',
        null, null, 1, 7, 7),
       (853, 90, null, null, null, null, null, null, null, 'axe-kick', null, null, null, null, '下压踢', 120, 10, 0,
        null, null,
        'The user attacks by kicking up into the air and slamming its heel down upon the target. This may also confuse the target. If it misses, the user takes damage instead.',
        null, null, 2, 10, 2),
       (854, 100, null, null, null, null, null, null, null, 'last-respects', null, null, null, null, '扫墓', 50, 10, 0,
        null, null,
        'The user attacks to avenge its allies. The more defeated allies there are in the user''s party, the greater the move''s power.',
        null, null, 2, 10, 8),
       (855, 100, null, null, null, null, null, null, null, 'lumina-crash', null, null, null, null, '琉光冲激', 80, 10,
        0, null, null,
        'The user attacks by unleashing a peculiar light that even affects the mind. This also harshly lowers the target''s Sp. Def stat.',
        null, null, 3, 10, 14),
       (856, 100, null, null, null, null, null, null, null, 'order-up', null, null, null, null, '上菜', 80, 10, 0, null,
        null,
        'The user attacks with elegant poise. If the user has a Tatsugiri in its mouth, this move boosts one of the user''s stats based on the Tatsugiri''s form.',
        null, null, 2, 10, 16),
       (857, 100, null, null, null, null, null, null, null, 'jet-punch', null, null, null, null, '喷射拳', 60, 15, 1,
        null, null,
        'The user summons a torrent around its fist and punches at blinding speed. This move always goes first.', null,
        null, 2, 10, 11),
       (858, null, null, null, null, null, null, null, null, 'spicy-extract', null, null, null, null, '辣椒精华', 0, 15,
        0, null, null,
        'The user emits an incredibly spicy extract, sharply boosting the target''s Attack stat and harshly lowering the target''s Defense stat.',
        null, null, 1, 10, 12),
       (859, 100, null, null, null, null, null, null, null, 'spin-out', null, null, null, null, '疾速转轮', 100, 5, 0,
        null, null,
        'The user spins furiously by straining its legs, inflicting damage on the target. This also harshly lowers the user''s Speed stat.',
        null, null, 2, 10, 9),
       (860, 90, null, null, null, null, null, null, null, 'population-bomb', null, null, null, null, '鼠数儿', 20, 10,
        0, null, null,
        'The user’s fellows gather in droves to perform a combo attack that hits the target one to ten times in a row.',
        null, null, 2, 10, 1),
       (861, 100, null, null, null, null, null, null, null, 'ice-spinner', null, null, null, null, '冰旋', 80, 15, 0,
        null, null,
        'The user covers its feet in thin ice and twirls around, slamming into the target. This move''s spinning motion also destroys the terrain.',
        null, null, 2, 10, 15),
       (862, 100, null, null, null, null, null, null, null, 'glaive-rush', null, null, null, null, '巨剑突击', 120, 5,
        0, null, null,
        'The user throws its entire body into a reckless charge. After this move is used, attacks on the user cannot miss and will inflict double damage until the user’s next turn.',
        null, null, 2, 10, 16),
       (863, null, null, null, null, null, null, null, null, 'revival-blessing', null, null, null, null, '复生祈祷', 0,
        1, 0, null, null,
        'The user bestows a loving blessing, reviving a party Pokémon that has fainted and restoring half that Pokémon''s max HP.',
        null, null, 1, 16, 1),
       (864, 100, null, null, null, null, null, null, null, 'salt-cure', null, null, null, null, '盐腌', 40, 15, 0,
        null, null,
        'The user salt cures the target, inflicting damage every turn. Steel and Water types are more strongly affected by this move.',
        null, null, 2, 10, 6),
       (865, 95, null, null, null, null, null, null, null, 'triple-dive', null, null, null, null, '三连钻', 30, 10, 0,
        null, null,
        'The user performs a perfectly timed triple dive, hitting the target with splashes of water three times in a row.',
        null, null, 2, 10, 11),
       (866, 100, null, null, null, null, null, null, null, 'mortal-spin', null, null, null, null, '晶光转转', 30, 15,
        0, null, null,
        'The user performs a spin attack that can also eliminate the effects of such moves as Bind, Wrap, and Leech Seed. This also poisons opposing Pokémon.',
        null, null, 2, 11, 4),
       (867, 100, null, null, null, null, null, null, null, 'doodle', null, null, null, null, '描绘', 0, 10, 0, null,
        null,
        'The user captures the very essence of the target in a sketch. This changes the Abilities of the user and its ally Pokémon to that of the target.',
        null, null, 1, 10, 1),
       (868, null, null, null, null, null, null, null, null, 'fillet-away', null, null, null, null, '甩肉', 0, 10, 0,
        null, null, 'The user sharply boosts its Attack, Sp. Atk, and Speed stats by using its own HP.', null, null, 1,
        7, 1),
       (869, null, null, null, null, null, null, null, null, 'kowtow-cleave', null, null, null, null, '仆刀', 85, 10, 0,
        null, null,
        'The user slashes at the target after kowtowing to make the target let down its guard. This attack never misses.',
        null, null, 2, 10, 17),
       (870, null, null, null, null, null, null, null, null, 'flower-trick', null, null, null, null, '千变万花', 70, 10,
        0, null, null,
        'The user throws a rigged bouquet of flowers at the target. This attack never misses and always lands a critical hit.',
        null, null, 2, 10, 12),
       (873, 100, null, null, null, null, null, null, null, 'raging-bull', null, null, null, null, '怒牛', 90, 10, 0,
        null, null,
        'The user performs a tackle like a raging bull. This move''s type depends on the user''s form. It can also break barriers, such as Light Screen and Reflect.',
        null, null, 2, 10, 1),
       (874, 100, null, null, null, null, null, null, null, 'make-it-rain', null, null, null, null, '淘金潮', 120, 5, 0,
        null, null,
        'The user attacks by throwing out a mass of coins. This also lowers the user''s Sp. Atk stat. Money is earned after the battle.',
        null, null, 3, 11, 9),
       (875, 100, null, null, null, null, null, null, null, 'psyblade', null, null, null, null, '精神剑', 80, 15, 0,
        null, null,
        'The user rends the target with an ethereal blade. This move''s power is boosted by 50 percent if the user is on Electric Terrain.',
        null, null, 2, 10, 14),
       (876, 100, null, null, null, null, null, null, null, 'hydro-steam', null, null, null, null, '水蒸气', 80, 15, 0,
        null, null,
        'The user blasts the target with boiling-hot water. This move''s power is not lowered in harsh sunlight but rather boosted by 50 percent.',
        null, null, 3, 10, 11),
       (877, 90, null, null, null, null, null, null, null, 'ruination', null, null, null, null, '大灾难', 1, 10, 0,
        null, null, 'The user summons a ruinous disaster. This cuts the target’s HP in half.', null, null, 3, 10, 17),
       (878, 100, null, null, null, null, null, null, null, 'collision-course', null, null, null, null, '全开猛撞', 100,
        5, 0, null, null,
        'The user transforms and crashes to the ground, causing a massive prehistoric explosion. This move''s power is boosted more than usual if it’s a supereffective hit.',
        null, null, 2, 10, 2),
       (879, 100, null, null, null, null, null, null, null, 'electro-drift', null, null, null, null, '闪电猛冲', 100, 5,
        0, null, null,
        'The user races forward at ultrafast speeds, piercing its target with futuristic electricity. This move''s power is boosted more than usual if it''s a supereffective hit.',
        null, null, 3, 10, 13),
       (880, null, null, null, null, null, null, null, null, 'shed-tail', null, null, null, null, '断尾', 0, 10, 0,
        null, null,
        'The user creates a substitute for itself using its own HP before switching places with a party Pokémon in waiting.',
        null, null, 1, 7, 1),
       (881, null, null, null, null, null, null, null, null, 'chilly-reception', null, null, null, null, '冷笑话', 0,
        10, 0, null, null, null, null, null, 1, 12, 15),
       (882, null, null, null, null, null, null, null, null, 'tidy-up', null, null, null, null, '大扫除', 0, 10, 0,
        null, null,
        'The user tidies up and removes the effects of Spikes, Stealth Rock, Sticky Web, Toxic Spikes, and Substitute. This also boosts the user’s Attack and Speed stats.',
        null, null, 1, 7, 1),
       (883, null, null, null, null, null, null, null, null, 'snowscape', null, null, null, null, '雪景', 0, 10, 0,
        null, null, 'The user summons a snowstorm lasting five turns. This boosts the Defense stats of Ice types.',
        null, null, 1, 12, 15),
       (884, 100, null, null, null, null, null, null, null, 'pounce', null, null, null, null, '虫扑', 50, 20, 0, null,
        null, 'The user attacks by pouncing on the target. This also lowers the target''s Speed stat.', null, null, 2,
        10, 7),
       (885, 100, null, null, null, null, null, null, null, 'trailblaze', null, null, null, null, '起草', 50, 20, 0,
        null, null,
        'The user attacks suddenly as if leaping out from tall grass. The user''s nimble footwork boosts its Speed stat.',
        null, null, 2, 10, 12),
       (886, 100, null, null, null, null, null, null, null, 'chilling-water', null, null, null, null, '泼冷水', 50, 20,
        0, null, null,
        'The user attacks the target by showering it with water that''s so cold it saps the target''s power. This also lowers the target''s Attack stat.',
        null, null, 3, 10, 11),
       (887, 100, null, null, null, null, null, null, null, 'hyper-drill', null, null, null, null, '强力钻', 100, 5, 0,
        null, null,
        'The user spins the pointed part of its body at high speed to pierce the target. This attack can hit a target using a move such as Protect or Detect.',
        null, null, 2, 10, 1),
       (888, 100, null, null, null, null, null, null, null, 'twin-beam', null, null, null, null, '双光束', 40, 10, 0,
        null, null, 'The user shoots mystical beams from its eyes to inflict damage. The target is hit twice in a row.',
        null, null, 3, 10, 14),
       (889, 100, null, null, null, null, null, null, null, 'rage-fist', null, null, null, null, '愤怒之拳', 50, 10, 0,
        null, null,
        'The user converts its rage into energy to attack. The more times the user has been hit by attacks, the greater the move''s power.',
        null, null, 2, 10, 8),
       (890, 100, null, null, null, null, null, null, null, 'armor-cannon', null, null, null, null, '铠农炮', 120, 5, 0,
        null, null,
        'The user shoots its own armor out as blazing projectiles. This also lowers the user’s Defense and Sp. Def stats.',
        null, null, 3, 10, 10),
       (891, 100, null, null, null, null, null, null, null, 'bitter-blade', null, null, null, null, '悔念剑', 90, 10, 0,
        null, null,
        'The user focuses its bitter feelings toward the world of the living into a slashing attack. The user''s HP is restored by up to half the damage taken by the target.',
        null, null, 2, 10, 10),
       (892, 100, null, null, null, null, null, null, null, 'double-shock', null, null, null, null, '电光双击', 120, 5,
        0, null, null,
        'The user discharges all the electricity from its body to perform a high-damage attack. After using this move, the user will no longer be Electric type.',
        null, null, 2, 10, 13),
       (893, 100, null, null, null, null, null, null, null, 'gigaton-hammer', null, null, null, null, '巨力锤', 160, 5,
        0, null, null,
        'The user swings its whole body around to attack with its huge hammer. This move can''t be used twice in a row.',
        null, null, 2, 10, 9),
       (894, 100, null, null, null, null, null, null, null, 'comeuppance', null, null, null, null, '复仇', 1, 10, 0,
        null, null,
        'The user retaliates with much greater force against the opponent that last inflicted damage on it.', null,
        null, 2, 1, 17),
       (895, 100, null, null, null, null, null, null, null, 'aqua-cutter', null, null, null, null, '水波刀', 70, 20, 0,
        null, null,
        'The user expels pressurized water to cut at the target like a blade. This move has a heightened chance of landing a critical hit.',
        null, null, 2, 10, 11),
       (896, 100, null, null, null, null, null, null, null, 'blazing-torque', null, null, null, null, '灼热暴冲', 80,
        10, 0, null, null, null, null, null, 2, 10, 10),
       (897, 100, null, null, null, null, null, null, null, 'wicked-torque', null, null, null, null, '黑暗暴冲', 80, 10,
        0, null, null, null, null, null, 2, 10, 17),
       (898, 100, null, null, null, null, null, null, null, 'noxious-torque', null, null, null, null, '剧毒暴冲', 100,
        10, 0, null, null, null, null, null, 2, 10, 4),
       (899, 100, null, null, null, null, null, null, null, 'combat-torque', null, null, null, null, '格斗暴冲', 100,
        10, 0, null, null, null, null, null, 2, 10, 2),
       (900, 100, null, null, null, null, null, null, null, 'magical-torque', null, null, null, null, '魔法暴冲', 100,
        10, 0, null, null, null, null, null, 2, 10, 18),
       (901, 100, null, null, null, null, null, null, null, 'blood-moon', null, null, null, null, 'Blood Moon', 140, 5,
        0, null, null,
        'The user unleashes the full brunt of its spirit from a full moon that shines as red as blood. This move can''t be used twice in a row.',
        null, null, 3, 10, 1),
       (902, 90, null, null, null, null, null, null, null, 'matcha-gotcha', null, null, null, null, 'Matcha Gotcha', 80,
        15, 0, null, null,
        'The user fires a blast of tea that it mixed. The user''s HP is restored by up to half the damage taken by the target. This may also leave the target with a burn.',
        null, null, 3, 10, 12),
       (903, 85, 0, 0, 0, null, null, 0, 0, 'syrup-bomb', null, 3, null, 3, 'Syrup Bomb', 60, 10, 0, null, 100,
        'The user sets off an explosion of sticky candy syrup, which coats the target and causes the target''s Speed stat to drop each turn for three turns.',
        0, 13, 3, 10, 12),
       (904, 100, null, null, null, null, null, null, null, 'ivy-cudgel', null, null, null, null, 'Ivy Cudgel', 100, 10,
        0, null, null,
        'The user strikes with an ivy-wrapped cudgel. This move''s type changes depending on the mask worn by the user, and it has a heightened chance of landing a critical hit.',
        null, null, 2, 10, 12),
       (905, 100, null, null, null, null, null, null, null, 'electro-shot', null, null, null, null, '电光束', 130, 10,
        0, null, null,
        'The user gathers electricity on the first turn, boosting its Sp. Atk stat, then fires a high-voltage shot on the next turn. The shot will be fired immediately in rain.',
        null, null, 3, 10, 13),
       (906, 100, null, null, null, null, null, null, null, 'tera-starstorm', null, null, null, null, '晶光星群', 120,
        5, 0, null, null,
        'With the power of its crystals, the user bombards and eliminates the target. When used by Terapagos in its Stellar Form, this move damages all opposing Pokémon.',
        null, null, 3, 11, 1),
       (907, 100, null, null, null, null, null, null, null, 'fickle-beam', null, null, null, null, '随机光', 80, 5, 0,
        null, null,
        'The user shoots a beam of light to inflict damage. Sometimes all the user''s heads shoot beams in unison, doubling the move''s power.',
        null, null, 3, 10, 16),
       (908, 0, null, null, null, null, null, null, null, 'burning-bulwark', null, null, null, null, '火焰守护', 0, 10,
        4, null, null,
        'The user''s intensely hot fur protects it from attacks and also burns any attacker that makes direct contact with it.',
        null, null, 1, 7, 10),
       (910, 100, null, null, null, null, null, null, null, 'mighty-cleave', null, null, null, null, '强刃攻击', 95, 5,
        0, null, null,
        'The user wields the light that has accumulated atop its head to cleave the target. This move hits even if the target protects itself.',
        null, null, 2, 10, 6),
       (911, 0, null, null, null, null, null, null, null, 'tachyon-cutter', null, null, null, null, '迅子利刃', 50, 10,
        0, null, null,
        'The user attacks by launching particle blades at the target twice in a row. This attack never misses.', null,
        null, 3, 10, 9),
       (912, 100, null, null, null, null, null, null, null, 'hard-press', null, null, null, null, '硬压', 0, 10, 0,
        null, null,
        'The target is crushed with an arm, a claw, or the like to inflict damage. The more HP the target has left, the greater the move''s power.',
        null, null, 2, 10, 9),
       (913, 0, null, null, null, null, null, null, null, 'dragon-cheer', null, null, null, null, '龙声鼓舞', 0, 15, 0,
        null, null,
        'The user raises its allies’ morale with a draconic cry so that their future attacks have a heightened chance of landing critical hits. This rouses Dragon types more.',
        null, null, 1, 15, 16),
       (914, 100, null, null, null, null, null, null, null, 'alluring-voice', null, null, null, null, '魅诱之声', 80,
        10, 0, null, null,
        'The user attacks the target using its angelic voice. This also confuses the target if its stats have been boosted during the turn.',
        null, null, 3, 10, 18),
       (915, 100, null, null, null, null, null, null, null, 'temper-flare', null, null, null, null, '豁出去', 75, 10, 0,
        null, null,
        'Spurred by desperation, the user attacks the target. This move''s power is doubled if the user''s previous move failed.',
        null, null, 2, 10, 10),
       (916, 95, null, null, null, null, null, null, null, 'supercell-slam', null, null, null, null, '闪电强袭', 100,
        15, 0, null, null,
        'The user electrifies its body and drops onto the target to inflict damage. If this move misses, the user takes damage instead.',
        null, null, 2, 10, 13),
       (917, 100, null, null, null, null, null, null, null, 'psychic-noise', null, null, null, null, '精神噪音', 75, 10,
        0, null, null,
        'The user attacks the target with unpleasant sound waves. For two turns, the target is prevented from recovering HP through moves, Abilities, or held items.',
        null, null, 3, 10, 14),
       (918, 100, null, null, null, null, null, null, null, 'upper-hand', null, null, null, null, '快手还击', 65, 15, 3,
        null, null,
        'The user reacts to the target''s movement and strikes with the heel of its palm, making the target flinch. This move fails if the target is not readying a priority move.',
        null, null, 2, 10, 2),
       (919, 100, null, null, null, null, null, null, null, 'malignant-chain', null, null, null, null, '邪毒锁链', 100,
        5, 0, null, null,
        'The user pours toxins into the target by wrapping them in a toxic, corrosive chain. This may also leave the target badly poisoned.',
        null, null, 3, 10, 4),
       (10001, 100, null, null, null,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move while in hyper mode.',
        null, null, null, 'shadow-rush', null, null, null, null, 'Shadow Rush', 55, null, 0,
        'Has an increased chance for a critical hit in Hyper Mode.', null, null, null, null, 2, 10, 10002),
       (10002, 100, null, null, null,
        'Inflicts regular damage.  User’s critical hit rate is one level higher when using this move.', null, null,
        null, 'shadow-blast', null, null, null, null, 'Shadow Blast', 80, null, 0,
        'Has an increased chance for a critical hit.', null, null, null, null, 2, 10, 10002),
       (10003, 100, null, null, null, 'Inflicts regular damage.', null, null, null, 'shadow-blitz', null, null, null,
        null, 'Shadow Blitz', 40, null, 0, 'Inflicts regular damage with no additional effect.', null, null, null, null,
        2, 10, 10002),
       (10004, 100, null, null, null, 'Inflicts regular damage.  Has a $effect_chance% chance to paralyze the target.',
        10, null, null, 'shadow-bolt', null, null, null, null, 'Shadow Bolt', 75, null, 0,
        'Has a $effect_chance% chance to paralyze the target.', null, null, null, null, 3, 10, 10002),
       (10005, 100, null, null, null, 'Inflicts regular damage.', null, null, null, 'shadow-break', null, null, null,
        null, 'Shadow Break', 75, null, 0, 'Inflicts regular damage with no additional effect.', null, null, null, null,
        2, 10, 10002),
       (10006, 100, null, null, null, 'Inflicts regular damage.  Has a $effect_chance% chance to freeze the target.',
        10, null, null, 'shadow-chill', null, null, null, null, 'Shadow Chill', 75, null, 0,
        'Has a $effect_chance% chance to freeze the target.', null, null, null, null, 3, 10, 10002),
       (10007, 60, null, null, null, 'Inflicts regular damage.  User takes 1/2 of its current HP in recoil.', null,
        null, null, 'shadow-end', null, null, null, null, 'Shadow End', 120, null, 0,
        'User receives 1/2 its HP in recoil.', null, null, null, null, 2, 10, 10002),
       (10008, 100, null, null, null, 'Inflicts regular damage.  Has a $effect_chance% chance to burn the target.', 10,
        null, null, 'shadow-fire', null, null, null, null, 'Shadow Fire', 75, null, 0,
        'Has a $effect_chance% chance to burn the target.', null, null, null, null, 3, 10, 10002),
       (10009, 100, null, null, null, 'Inflicts regular damage.', null, null, null, 'shadow-rave', null, null, null,
        null, 'Shadow Rave', 70, null, 0, 'Inflicts regular damage with no additional effect.', null, null, null, null,
        3, 6, 10002),
       (10010, 100, null, null, null, 'Inflicts regular damage.', null, null, null, 'shadow-storm', null, null, null,
        null, 'Shadow Storm', 95, null, 0, 'Inflicts regular damage with no additional effect.', null, null, null, null,
        3, 6, 10002),
       (10011, 100, null, null, null, 'Inflicts regular damage.', null, null, null, 'shadow-wave', null, null, null,
        null, 'Shadow Wave', 50, null, 0, 'Inflicts regular damage with no additional effect.', null, null, null, null,
        3, 6, 10002),
       (10012, 100, null, null, null, 'Lowers the target’s Defense by two stages.', null, null, null, 'shadow-down',
        null, null, null, null, 'Shadow Down', null, null, 0, 'Lowers the target’s Defense by two stages.', null, null,
        null, null, 1, 6, 10002),
       (10013, 100, null, null, null,
        'Halves HP of all Pokémon on the field.  User loses its next turn to "recharge", and cannot attack or switch out during that turn.',
        null, null, null, 'shadow-half', null, null, null, null, 'Shadow Half', null, null, 0,
        'Halves HP of all Pokémon on the field.  Must recharge', null, null, null, null, 3, 12, 10002),
       (10014, null, null, null, null, 'The target cannot switch out normally.  Ignores accuracy and evasion modifiers.  This effect ends when the user leaves the field.

The target may still escape by using baton pass, u turn, or a shed shell.

Both the user and the target pass on this effect with baton pass.', null, null, null, 'shadow-hold', null, null, null,
        null, 'Shadow Hold', null, null, 0, 'Prevents the target from leaving battle.', null, null, null, null, 1, 6,
        10002),
       (10015, 100, null, null, null, 'Lowers the target’s evasion by two stages.', null, null, null, 'shadow-mist',
        null, null, null, null, 'Shadow Mist', null, null, 0, 'Lowers the target’s evasion by two stages.', null, null,
        null, null, 1, 6, 10002),
       (10016, 90, null, null, null, 'Confuses the target.', null, null, null, 'shadow-panic', null, null, null, null,
        'Shadow Panic', null, null, 0, 'Confuses the target.', null, null, null, null, 1, 6, 10002),
       (10017, null, null, null, null, 'Removes the effects of light screen, reflect, and safeguard.', null, null, null,
        'shadow-shed', null, null, null, null, 'Shadow Shed', null, null, 0,
        'Removes Light Screen, Reflect, and Safeguard.', null, null, null, null, 1, 12, 10002),
       (10018, null, null, null, null,
        'Changes the weather to Shadow Sky for five turns.  Pokémon other than shadow Pokémon take 1/16 their max HP at the end of every turn.  This move is typeless for the purposes of weather ball.',
        null, null, null, 'shadow-sky', null, null, null, null, 'Shadow Sky', null, null, 0,
        'Changes the weather to Shadow Sky for five turns.', null, null, null, null, 1, 12, 10002);
COMMIT;