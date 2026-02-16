BEGIN;
insert into public.item (id, cost, effect, fling_power, internal_name, name, short_effect, text, item_fling_effect_id)
values (1, 0, 'Used in battle
:   Catches a wild Pokémon without fail.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'master-ball', '大师球',
        'Catches a wild Pokémon every time.', '必定能捉到野生宝可梦的，
性能最好的球。', null),
       (2, 800, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 2×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'ultra-ball', '高级球',
        'Tries to catch a wild Pokémon.  Success rate is 2×.', '比起超级球来
更容易捉到宝可梦的，
性能非常不错的球。', null),
       (3, 600, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1.5×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'great-ball', '超级球',
        'Tries to catch a wild Pokémon.  Success rate is 1.5×.', '比起精灵球来
更容易捉到宝可梦的，
性能还算不错的球。', null),
       (4, 200, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'poke-ball', '精灵球',
        'Tries to catch a wild Pokémon.', '用于投向野生宝可梦
并将其捕捉的球。
它是胶囊样式的。', null),
       (5, 0, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1.5×.

This item can only be used in the great marsh or kanto safari zone.', null, 'safari-ball', '狩猎球',
        'Tries to catch a wild Pokémon in the Great Marsh or Safari Zone.  Success rate is 1.5×.', '仅能在大湿地中使用的特殊的球。
上面有迷彩花纹。', null),
       (6, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  If the wild Pokémon is water- or bug-type, this ball has a catch rate of 3×.  Otherwise, it has a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'net-ball', '捕网球',
        'Tries to catch a wild Pokémon.  Success rate is 3× for water and bug Pokémon.', '有点与众不同的球。
能很容易地捕捉
水属性和虫属性的宝可梦。', null),
       (7, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  If the wild Pokémon was encountered by surfing or fishing, this ball has a catch rate of 3.5×.  Otherwise, it has a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'dive-ball', '潜水球',
        'Tries to catch a wild Pokémon. Success rate is 3.5× when underwater, fishing, or surfing.', '有点与众不同的球。
能很容易地捕捉
生活在水世界里的宝可梦。', null),
       (8, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  Has a catch rate of given by `(40 - level) / 10`, where `level` is the wild Pokémon’s level, to a maximum of 3.9× for level 1 Pokémon.  If the wild Pokémon’s level is higher than 30, this ball has a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'nest-ball', '巢穴球',
        'Tries to catch a wild Pokémon.  Success rate is 3.9× for level 1 Pokémon, and drops steadily to 1× at level 30.', '有点与众不同的球。
捕捉的野生宝可梦越弱，
就会越容易捕捉。', null),
       (9, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  If the wild Pokémon’s species is marked as caught in the trainer’s Pokédex, this ball has a catch rate of 3×.  Otherwise, it has a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'repeat-ball', '重复球',
        'Tries to catch a wild Pokémon.  Success rate is 3× for previously-caught Pokémon.', '有点与众不同的球。
能很容易地捕捉
以前曾捉到过的宝可梦。', null),
       (10, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  Has a catch rate of 1.1× on the first turn of the battle and increases by 0.1× every turn, to a maximum of 4× on turn 30.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'timer-ball', '计时球',
        'Tries to catch a wild Pokémon. Success rate increases by 0.1× (Gen V: 0.3×) every turn, to a max of 4×.', '有点与众不同的球。
回合数花费得越多，
宝可梦就会越容易捕捉。', null),
       (11, 3000, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1×.  Whenever the caught Pokémon’s happiness increases, it increases by one extra point.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'luxury-ball', '豪华球',
        'Tries to catch a wild Pokémon.  Caught Pokémon start with 200 happiness.', '住着十分惬意的球。
捉到的野生宝可梦会
变得容易和训练家亲密。', null),
       (12, 20, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'premier-ball', '纪念球',
        'Tries to catch a wild Pokémon.', '有点珍贵的球。
特制出来的某种纪念品。', null),
       (13, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon.  If it’s currently nighttime or the wild Pokémon was encountered while walking in a cave, this ball has a catch rate of 3.5×.  Otherwise, it has a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'dusk-ball', '黑暗球',
        'Tries to catch a wild Pokémon.  Success rate is 3.5× at night and in caves.', '有点与众不同的球。
能很容易地在夜晚或洞窟等
阴暗的地方捕捉宝可梦。', null),
       (14, 300, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1×.  The caught Pokémon’s HP is immediately restored, PP for all its moves is restored, and any status ailment is cured.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'heal-ball', '治愈球',
        'Tries to catch a wild Pokémon.  Caught Pokémon are immediately healed.', '有点温柔的球。
能回复捉到的宝可梦的
ＨＰ并治愈异常状态。', null),
       (15, 1000, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 4× on the first turn of a battle, but 1× any other time.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'quick-ball', '先机球',
        'Tries to catch a wild Pokémon. Success rate is 4× (Gen V: 5×), but only on the first turn.', '有点与众不同的球。
如果战斗开始后立刻使用，
就能很容易地捉到宝可梦。', null),
       (16, 0, 'Used in battle
:   Attempts to catch a wild Pokémon, using a catch rate of 1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'cherish-ball', '贵重球',
        'Tries to catch a wild Pokémon.', '相当珍贵的球。
特制出来的某种纪念品。', null),
       (17, 200, 'Used on a friendly Pokémon
:   Restores 20 HP.', 30, 'potion', '伤药', 'Restores 20 HP.', '喷雾式伤药。
能让１只宝可梦
回复２０ＨＰ。', null),
       (18, 200, 'Used on a party Pokémon
:   Cures poison.', 30, 'antidote', '解毒药', 'Cures poison.', '喷雾式药水。
能治愈１只宝可梦的
中毒状态。', null),
       (19, 200, 'Used on a party Pokémon
:   Cures a burn.', 30, 'burn-heal', '灼伤药', 'Cures a burn.', '喷雾式药水。
能治愈１只宝可梦的
灼伤状态。', null),
       (20, 200, 'Used on a party Pokémon
:   Cures freezing.', 30, 'ice-heal', '解冻药', 'Cures freezing.', '喷雾式药水。
能治愈１只宝可梦的
冰冻状态。', null),
       (21, 200, 'Used on a party Pokémon
:   Cures sleep.', 30, 'awakening', '解眠药', 'Cures sleep.', '喷雾式药水。
能治愈１只宝可梦的
睡眠状态。', null),
       (2218, 0, null, null, 'tm229', 'TM229', null, null, null),
       (22, 200, 'Used on a party Pokémon
:   Cures paralysis.', 30, 'paralyze-heal', '解麻药', 'Cures paralysis.', '喷雾式药水。
能治愈１只宝可梦的
麻痹状态。', null),
       (23, 3000, 'Used on a party Pokémon
:   Restores HP to full and cures any status ailment and confusion.', 30, 'full-restore', '全复药',
        'Restores HP to full and cures any status ailment and confusion.', '能回复１只宝可梦的
所有ＨＰ并治愈所有异常状态。', null),
       (24, 2500, 'Used on a party Pokémon
:   Restores HP to full.', 30, 'max-potion', '全满药', 'Restores HP to full.', '喷雾式伤药。
能让１只宝可梦
回复所有ＨＰ。', null),
       (25, 1500, 'Used on a party Pokémon
:   Restores 200 HP.', 30, 'hyper-potion', '厉害伤药', 'Restores 200 HP.', '喷雾式伤药。
能让１只宝可梦
回复１２０ＨＰ。', null),
       (26, 700, 'Used on a party Pokémon
:   Restores 50 HP.', 30, 'super-potion', '好伤药', 'Restores 50 HP.', '喷雾式伤药。
能让１只宝可梦
回复６０ＨＰ。', null),
       (27, 400, 'Used on a party Pokémon
:   Cures any status ailment and confusion.', 30, 'full-heal', '万灵药', 'Cures any status ailment and confusion.', '喷雾式药水。
能治愈１只宝可梦的
所有异常状态。', null),
       (28, 2000, 'Used on a party Pokémon
:   Revives the Pokémon and restores half its HP.', 30, 'revive', '活力碎片', 'Revives with half HP.', '能让１只陷入濒死的
宝可梦重获生机，
并回复一半ＨＰ。', null),
       (29, 4000, 'Used on a party Pokémon
:   Revives the Pokémon and restores its HP to full.', 30, 'max-revive', '活力块', 'Revives with full HP.', '能让１只陷入濒死的
宝可梦重获生机，
并回复所有ＨＰ。', null),
       (30, 200, 'Used on a party Pokémon
:   Restores 50 HP.', 30, 'fresh-water', '美味之水', 'Restores 50 HP.', '富含矿物质的水。
能让１只宝可梦
回复３０ＨＰ。', null),
       (31, 300, 'Used on a party Pokémon
:   Restores 60 HP.', 30, 'soda-pop', '劲爽汽水', 'Restores 60 HP.', '翻腾着气泡的汽水。
能让１只宝可梦
回复５０ＨＰ。', null),
       (32, 400, 'Used on a party Pokémon
:   Restores 80 HP.', 30, 'lemonade', '果汁牛奶', 'Restores 80 HP.', '非常香甜的牛奶。
能让１只宝可梦
回复７０ＨＰ。', null),
       (33, 600, 'Used on a party Pokémon
:   Restores 100 HP.', 30, 'moomoo-milk', '哞哞鲜奶', 'Restores 100 HP.', '营养百分百的牛奶。
能让１只宝可梦
回复１００ＨＰ。', null),
       (34, 500, 'Used on a party Pokémon
:   Restores 50 HP.  Decreases happiness by 5/5/10.', 30, 'energy-powder', '元气粉',
        'Restores 50 HP, but lowers happiness.', '非常苦的药粉。
能让１只宝可梦
回复６０ＨＰ。', null),
       (35, 1200, 'Used on a party Pokémon
:   Restores 200 HP.  Decreases happiness by 10/10/15.', 30, 'energy-root', '元气根',
        'Restores 200 HP, but lowers happiness.', '非常苦的根。
能让１只宝可梦
回复１２０ＨＰ。', null),
       (36, 300, 'Used on a party Pokémon
:   Cures any status ailment.  Decreases happiness by 5/5/10.', 30, 'heal-powder', '万能粉',
        'Cures any status ailment, but lowers happiness.', '非常苦的药粉。
能治愈１只宝可梦的
所有异常状态。', null),
       (37, 2800, 'Used on a party Pokémon
:   Revives a fainted Pokémon and restores its HP to full.  Decreases happiness by 10/10/15.', 30, 'revival-herb',
        '复活草', 'Revives with full HP, but lowers happiness.', '非常苦的药草。
能让１只濒死的宝可梦
回复所有ＨＰ。', null),
       (38, 1200, 'Used on a party Pokémon
:   Restores 10 PP for a selected move.', 30, 'ether', 'ＰＰ单项小补剂', 'Restores 10 PP for one move.', '能让宝可梦学会的
其中１个招式
回复１０ＰＰ。', null),
       (39, 2000, 'Used on a party Pokémon
:   Restores PP to full for a selected move.', 30, 'max-ether', 'ＰＰ单项全补剂', 'Restores PP to full for one move.', '能让宝可梦学会的
其中１个招式
回复所有ＰＰ。', null),
       (40, 3000, 'Used on a party Pokémon
:   Restores 10 PP for each move.', 30, 'elixir', 'ＰＰ多项小补剂', 'Restores 10 PP for each move.', '能让宝可梦学会的
４个招式各
回复１０ＰＰ。', null),
       (41, 4500, 'Used on a party Pokémon
:   Restores PP to full for each move.', 30, 'max-elixir', 'ＰＰ多项全补剂', 'Restores PP to full for each move.', '能让宝可梦学会的
４个招式
回复所有ＰＰ。', null),
       (42, 350, 'Used on a party Pokémon
:   Cures any status ailment and confusion.', 30, 'lava-cookie', '釜炎仙贝', 'Cures any status ailment and confusion.', '釜炎特产的仙贝。
能治愈１只宝可梦的
所有异常状态。', null),
       (43, 200, 'Used on a party Pokémon
:   Restores 20 HP.', 30, 'berry-juice', '树果汁', 'Restores 20 HP.', '１００％树果果汁。
能让１只宝可梦
回复２０ＨＰ。', null),
       (44, 50000, 'Used
:   Revives all fainted Pokémon in the party and restores their HP to full.', 30, 'sacred-ash', '圣灰',
        'Revives all fainted Pokémon with full HP.', '能让陷入濒死的
全部宝可梦
回复所有ＨＰ。', null),
       (45, 10000, 'Used on a party Pokémon
:   Increases HP effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30, 'hp-up',
        'ＨＰ增强剂', 'Raises HP effort and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
ＨＰ的基础点数。', null),
       (46, 10000, 'Used on a party Pokémon
:   Increases Attack effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30, 'protein',
        '攻击增强剂', 'Raises Attack effort and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
攻击的基础点数。', null),
       (47, 10000, 'Used on a party Pokémon
:   Increases Defense effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30, 'iron',
        '防御增强剂', 'Raises Defense effort and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
防御的基础点数。', null),
       (48, 10000, 'Used on a party Pokémon
:   Increases Speed effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30, 'carbos',
        '速度增强剂', 'Raises Speed effort and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
速度的基础点数。', null),
       (49, 10000, 'Used on a party Pokémon
:   Increases Special Attack effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30,
        'calcium', '特攻增强剂', 'Raises Special Attack effort and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
特攻的基础点数。', null),
       (50, 10000, 'Used on a party Pokémon
:   Increases level by 1.  Increases happiness by 5/3/2.', 30, 'rare-candy', '神奇糖果',
        'Causes a level-up and raises happiness.', '充满能量的糖果。
将它交给宝可梦后，
１只宝可梦的等级仅会提高１。', null),
       (51, 10000, 'Used on a party Pokémon
:   Increases a selected move’s max PP by 20% its original max PP, to a maximum of 1.6×.  Increases happiness by 5/3/2.',
        30, 'pp-up', 'ＰＰ提升剂', 'Raises a move’s max PP by 20%.', '能让宝可梦学会的
其中１个招式的
ＰＰ最大值少量提高。', null),
       (333, 1000, 'Teaches Psychic to a compatible Pokémon.', null, 'tm29', '招式学习器２９',
        'Teaches Psychic to a compatible Pokémon.', '向对手发送
强大的念力进行攻击。
有时会降低对手的特防。', null),
       (52, 10000, 'Used on a party Pokémon
:   Increases Special Defense effort by 10, but won’t increase it beyond 100.  Increases happiness by 5/3/2.', 30,
        'zinc', '特防增强剂', 'Raises Special Defense and happiness.', '宝可梦的营养饮料。
能提高１只宝可梦的
特防的基础点数。', null),
       (53, 10000, 'Used on a party Pokémon
:   Increases a selected move’s max PP to 1.6× its original max PP.  Increases happiness by 5/3/2.', 30, 'pp-max',
        'ＰＰ极限提升剂', 'Raises a move’s max PP by 60%.', '能将宝可梦学会的
其中１个招式的
ＰＰ最大值提至最高。', null),
       (54, 350, 'Used on a party Pokémon
:   Cures any status ailment and confusion.', 30, 'old-gateau', '森之羊羹', 'Cures any status ailment and confusion.', '百代不为人知的特产。
能治愈１只宝可梦的
所有异常状态。', null),
       (55, 1500, 'Used on a party Pokémon in battle
:   Protects the target’s stats from being lowered for the next five turns.  Increases happiness by 1/1/0.', 30,
        'guard-spec', '能力防守', 'Prevents stat changes in battle for five turns in battle.  Raises happiness.', '在战斗中，
５回合内不让我方
能力降低的道具。', null),
       (56, 1000, 'Used on a party Pokémon in battle
:   Increases the target’s critical hit chance by one stage until it leaves the field.  Increases happiness by 1/1/0.',
        30, 'dire-hit', '要害攻击', 'Increases the chance of a critical hit in battle.  Raises happiness.', '击中要害的几率会大幅提高。
只能使用１次。
离场后，效果便会消失。', null),
       (57, 1000, 'Used on a party Pokémon in battle
:   Raises the target’s Attack by one stage.  Increases happiness by 1/1/0.', 30, 'x-attack', '力量强化',
        'Raises Attack by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦攻击的道具。
离场后，效果便会消失。', null),
       (58, 2000, 'Used on a party Pokémon in battle
:   Raises the target’s Defense by one stage.  Increases happiness by 1/1/0.', 30, 'x-defense', '防御强化',
        'Raises Defense by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦防御的道具。
离场后，效果便会消失。', null),
       (59, 1000, 'Used on a party Pokémon in battle
:   Raises the target’s Speed by one stage.  Increases happiness by 1/1/0.', 30, 'x-speed', '速度强化',
        'Raises Speed by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦速度的道具。
离场后，效果便会消失。', null),
       (60, 1000, 'Used on a party Pokémon in battle
:   Raises the target’s accuracy by one stage.  Increases happiness by 1/1/0.', 30, 'x-accuracy', '命中强化',
        'Raises accuracy by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦命中的道具。
离场后，效果便会消失。', null),
       (61, 1000, 'Used on a party Pokémon in battle
:   Raises the target’s Special Attack by one stage.  Increases happiness by 1/1/0.', 30, 'x-sp-atk', '特攻强化',
        'Raises Special Attack by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦特攻的道具。
离场后，效果便会消失。', null),
       (62, 2000, 'Used on a party Pokémon in battle
:   Raises the target’s Special Defense by one stage.  Increases happiness by 1/1/0.', 30, 'x-sp-def', '特防强化',
        'Raises Special Defense by one stage in battle.  Raises happiness.', '大幅提高战斗中
宝可梦特防的道具。
离场后，效果便会消失。', null),
       (63, 300, 'Used in battle
:   Ends a wild battle.  Cannot be used in trainer battles.', 30, 'poke-doll', '皮皮玩偶', 'Ends a wild battle.', '能吸引宝可梦注意的道具。
在和野生宝可梦的
战斗中绝对可以逃走。', null),
       (64, 100, 'Used in battle
:   Ends a wild battle.  Cannot be used in trainer battles.', 30, 'fluffy-tail', '向尾喵的尾巴', 'Ends a wild battle.', '能吸引宝可梦注意的道具。
在和野生宝可梦的
战斗中绝对可以逃走。', null),
       (65, 20, 'Used on a party Pokémon
:   Cures sleep.', 30, 'blue-flute', '蓝色玻璃哨', 'Cures sleep.', '以蓝色玻璃制成的哨子。
可以治愈睡眠状态。', null),
       (66, 20, 'Used on a party Pokémon in battle
:   Cures confusion.', 30, 'yellow-flute', '黄色玻璃哨', 'Cures confusion.', '以黄色玻璃制成的哨子。
可以治愈混乱状态。', null),
       (67, 20, 'Used on a party Pokémon in battle
:   Cures attraction.', 30, 'red-flute', '红色玻璃哨', 'Cures attraction.', '以红色玻璃制成的哨子。
可以治愈着迷状态。', null),
       (68, 20, 'Used outside of battle
:   Decreases the wild Pokémon encounter rate by 50%.', 30, 'black-flute', '黑色玻璃哨',
        'Halves the wild Pokémon encounter rate.', '以黑色玻璃制成的哨子。
在使用的地方更容易
遇到强大的宝可梦。', null),
       (69, 20, 'Used outside of battle
:   Doubles the wild Pokémon encounter rate.', 30, 'white-flute', '白色玻璃哨',
        'Doubles the wild Pokémon encounter rate.', '以白色玻璃制成的哨子。
在使用的地方更容易
遇到弱小的宝可梦。', null),
       (70, 20, 'No effect.', 30, 'shoal-salt', '浅滩海盐',
        'No effect. Gen III: Trade four and four Shoal Shells for a Shell Bell.', '在浅滩洞穴这地方
找到的海盐。', null),
       (71, 20, 'No effect.', 30, 'shoal-shell', '浅滩贝壳',
        'No effect. Gen III: Trade four and four Shoal Salts for a Shell Bell.', '在浅滩洞穴这地方
找到的贝壳。', null),
       (72, 1000, 'No effect.

In Diamond and Pearl, trade ten for a sunny day TM in the house midway along the southern section of sinnoh route 212.

In Platinum, trade to move tutors on sinnoh route 212, in snowpoint city, and in the survival area.  Eight shards total are required per tutelage, but the particular combination of colors varies by move.

In HeartGold and SoulSilver, trade one for a cheri berry, a leppa berry, and a pecha berry with the Juggler near the Pokémon Center in violet city.

In HeartGold and SoulSilver, trade one for a persim berry, a pomeg berry, and a razz berry with the Juggler near the pal park entrance in fuchsia city.',
        30, 'red-shard', '红色碎片', 'No effect. Can be traded for items or moves.', '红色的小碎片。
好像是以前制作的
某道具的一部分。', null),
       (73, 1000, 'No effect.

In Diamond and Pearl, trade ten for a rain dance TM in the house midway along the southern section of sinnoh route 212.

In Platinum, trade to move tutors on sinnoh route 212, in snowpoint city, and in the survival area.  Eight shards total are required per tutelage, but the particular combination of colors varies by move.

In HeartGold and SoulSilver, trade one for a chesto berry, an oran berry, and a wiki berry with the Juggler near the Pokémon Center in violet city.

In HeartGold and SoulSilver, trade one for a bluk berry, a cornn berry, and a kelpsy berry with the Juggler near the pal park entrance in fuchsia city.',
        30, 'blue-shard', '蓝色碎片', 'No effect. Can be traded for items or moves.', '蓝色的小碎片。
好像是以前制作的
某道具的一部分。', null),
       (93, 100,
        'Trade one to the Move Relearner near the shore in pastoria city or with the Move Deleter in blackthorn city to teach one party Pokémon a prior level-up move.',
        30, 'heart-scale', '心之鳞片', 'No effect. Can be traded for prior Level-up moves.', '有着美丽心形外形
且非常珍稀的鳞片。
有些人收到会很高兴。', null),
       (426, 0, 'Contains up to 100 Poffins.', null, 'poffin-case', '宝芬盒', 'Holds Poffins.', '用来保存烹饪好的
宝芬的容器。', null),
       (74, 1000, 'No effect.

In Diamond and Pearl, trade ten for a sandstorm TM in the house midway along the southern section of sinnoh route 212.

In Platinum, trade to move tutors on sinnoh route 212, in snowpoint city, and in the survival area.  Eight shards total are required per tutelage, but the particular combination of colors varies by move.

In HeartGold and SoulSilver, trade one for an aspear berry, a iapapa berry, and a sitrus berry with the Juggler near the Pokémon Center in violet city.

In HeartGold and SoulSilver, trade one for a grepa berry, a nomel berry, and a pinap berry with the Juggler near the pal park entrance in fuchsia city.',
        30, 'yellow-shard', '黄色碎片', 'No effect. Can be traded for items or moves.', '黄色的小碎片。
好像是以前制作的
某道具的一部分。', null),
       (75, 1000, 'No effect.

In Diamond and Pearl, trade ten for a hail TM in the house midway along the southern section of sinnoh route 212.

In Platinum, trade to move tutors on sinnoh route 212, in snowpoint city, and in the survival area.  Eight shards total are required per tutelage, but the particular combination of colors varies by move.

In HeartGold and SoulSilver, trade one for an aguav berry, a lum berry, and a rawst berry with the Juggler near the Pokémon Center in violet city.

In HeartGold and SoulSilver, trade one for a durin berry, a hondew berry, and a wepear berry with the Juggler near the pal park entrance in fuchsia city.',
        30, 'green-shard', '绿色碎片', 'No effect. Can be traded for items or moves.', '绿色的小碎片。
好像是以前制作的
某道具的一部分。', null),
       (76, 700, 'Used outside of battle
:   Trainer will skip encounters with wild Pokémon of a lower level than the lead party Pokémon.  This effect wears off after the trainer takes 200 steps.',
        30, 'super-repel', '白银喷雾',
        'For 200 steps, prevents wild encounters of level lower than your party’s lead Pokémon.', '弱小的野生宝可梦
将完全不会出现。
效果比除虫喷雾更持久。', null),
       (77, 900, 'Used outside of battle
:   Trainer will skip encounters with wild Pokémon of a lower level than the lead party Pokémon.  This effect wears off after the trainer takes 250 steps.',
        30, 'max-repel', '黄金喷雾',
        'For 250 steps, prevents wild encounters of level lower than your party’s lead Pokémon.', '弱小的野生宝可梦
将完全不会出现。
效果比白银喷雾更持久。', null),
       (78, 300, 'Used outside of battle
:   Transports the trainer to the last-entered dungeon entrance.  Cannot be used outside, in buildings, or in distortion world, sinnoh hall of origin 1, spear pillar, or turnback cave.',
        30, 'escape-rope', '离洞绳', 'Transports user to the outside entrance of a cave.', '结实的长绳。
可以从洞窟或迷宫中脱身。', null),
       (79, 400, 'Used outside of battle
:   Trainer will skip encounters with wild Pokémon of a lower level than the lead party Pokémon.  This effect wears off after the trainer takes 100 steps.',
        30, 'repel', '除虫喷雾',
        'For 100 steps, prevents wild encounters of level lower than your party’s lead Pokémon.', '使用后，在较短的一段时间内，
弱小的野生宝可梦将完全不会出现。', null),
       (80, 3000, 'Used on a party Pokémon
:   Evolves a cottonee into whimsicott, a gloom into bellossom, a petilil into lilligant, or a sunkern into sunflora.',
        30, 'sun-stone', '日之石',
        'Evolves a Cottonee into Whimsicott, a Gloom into Bellossom, a Petilil into Lilligant, or a Sunkern into Sunflora.', '能让某些特定宝可梦
进化的神奇石头。
像太阳一样赤红。', null),
       (81, 3000, 'Used on a party Pokémon
:   Evolves a clefairy into clefable, a jigglypuff into wigglytuff, a munna into musharna, a nidorina into nidoqueen, a nidorino into nidoking, or a skitty into delcatty.',
        30, 'moon-stone', '月之石',
        'Evolves a Clefairy into Clefable, a Jigglypuff into Wigglytuff, a Munna into Musharna, a Nidorina into Nidoqueen, a Nidorino into Nidoking, or a Skitty into Delcatty.', '能让某些特定宝可梦
进化的神奇石头。
像夜空一样乌黑。', null),
       (82, 3000, 'Used on a party Pokémon
:   Evolves an eevee into flareon, a growlithe into arcanine, a pansear into simisear, or a vulpix into ninetales.', 30,
        'fire-stone', '火之石',
        'Evolves an Eevee into Flareon, a Growlithe into Arcanine, a Pansear into Simisear, or a Vulpix into Ninetales.', '能让某些特定宝可梦
进化的神奇石头。
看上去是橙黄色的。', null),
       (83, 3000, 'Used on a party Pokémon
:   Evolves an eelektrik into eelektross, an eevee into jolteon, or a pikachu into raichu.', 30, 'thunder-stone',
        '雷之石', 'Evolves an Eelektrik into Eelektross, an Eevee into Jolteon, or a Pikachu into Raichu.', '能让某些特定宝可梦
进化的神奇石头。
有着闪电般的花纹。', null),
       (84, 3000, 'Used on a party Pokémon
:   Evolves an eevee into vaporeon, a lombre into ludicolo, a panpour into simipour, a poliwhirl into poliwrath, a shellder into cloyster, or a staryu into starmie.',
        30, 'water-stone', '水之石',
        'Evolves an Eevee into Vaporeon, a Lombre into Ludicolo, a Panpour into Simipour, a Poliwhirl into Poliwrath, a Shellder into Cloyster, or a Staryu into Starmie.', '能让某些特定宝可梦
进化的神奇石头。
看上去是澄蓝色的。', null),
       (85, 3000, 'Used on a party Pokémon
:   Evolves an exeggcute into exeggutor, a gloom into vileplume, a nuzleaf into shiftry, a pansage into simisage, or a weepinbell into victreebel.',
        30, 'leaf-stone', '叶之石',
        'Evolves an Exeggcute into Exeggutor, a Gloom into Vileplume, a Nuzleaf into Shiftry, a Pansage into Simisage, or a Weepinbell into Victreebel.', '能让某些特定宝可梦
进化的神奇石头。
有着叶子般的花纹。', null),
       (86, 500, 'Vendor trash.', 30, 'tiny-mushroom', '小蘑菇',
        'Fire Red and Leaf Green: Trade two for prior Level-up moves. Sell for 250 Pokédollars, or to Hungry Maid for 500 Pokédollars.', '珍稀的小蘑菇。
在一些爱好者中
有着相当高的人气。', null),
       (87, 5000, 'Vendor trash.', 30, 'big-mushroom', '大蘑菇',
        'Fire Red and Leaf Green: Trade for prior Level-up moves. Sell for 2500 Pokédollars, or to Hungry Maid for 5000 Pokédollars.', '珍稀的大蘑菇。
在一些爱好者中
有着非常高的人气。', null),
       (88, 2000, 'Vendor trash.', 30, 'pearl', '珍珠',
        'Sell for 700 Pokédollars, or to Ore Collector for 1400 Pokédollars.', '散发着美丽银辉
且有点小的珍珠。
可以在商店低价出售。', null),
       (89, 8000, 'Vendor trash.', 30, 'big-pearl', '大珍珠',
        'Sell for 3750 Pokédollars, or to Ore Collector for 7500 Pokédollars.', '散发着美丽银辉
且相当大颗的珍珠。
可以在商店高价出售。', null),
       (90, 3000, 'Vendor trash.', 30, 'stardust', '星星沙子',
        'Sell for 1000 Pokédollars, or to Ore Collector for 2000 Pokédollars.', '手感细腻且十分
美丽的红色沙子。
可以在商店低价出售。', null),
       (91, 12000, 'Vendor trash.', 30, 'star-piece', '星星碎片',
        'Platinum: Trade for one of each color Shard. Black and White: Trade for PP Up. Sell for 4900 Pokédollars, or to Ore Collector for 9800 Pokédollars.', '闪着红光且十分
美丽的宝石碎片。
可以在商店高价出售。', null),
       (92, 10000, 'Vendor trash.', 30, 'nugget', '金珠',
        'Sell for 5000 Pokédollars, or to Ore Collector for 10000 Pokédollars.', '闪着金光，
以纯金制成的珠子。
可以在商店高价出售。', null),
       (1030, 1000, null, 30, 'silver-leaf', '银色叶子', null, '神奇的银色叶子。
至今仍未发现能长出
这种叶子的树木。', null),
       (94, 900, 'Used outside of battle
:   Immediately triggers a wild Pokémon battle, as long as the trainer is somewhere with wild Pokémon—i.e., in tall grass, in a cave, or surfing.

Can be smeared on sweet-smelling trees to attract tree-dwelling Pokémon after six hours.', 30, 'honey', '甜甜蜜', '', '在草丛或洞窟等地方使用后，
被甜甜香气吸引的
野生宝可梦就会出现。', null),
       (95, 200, 'Used on a patch of soil
:   Plant’s growth stages will each last 25% less time.  Dries soil out more quickly.', 30, 'growth-mulch', '速速肥',
        'Growing time of berries is reduced, but the soil dries out faster.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (96, 200, 'Used on a patch of soil
:   Plant’s growth stages will each last 25% more time.  Dries soil out more slowly.', 30, 'damp-mulch', '湿湿肥',
        'Growing time of berries is increased, but the soil dries out slower.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (97, 200, 'Used on a patch of soil
:   Fully-grown plant will last 25% longer before dying and possibly regrowing.', 30, 'stable-mulch', '久久肥',
        'Berries stay on the plant for longer than their usual time.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (98, 200, 'Used on a path of soil
:   Plant will regrow after dying 25% more times.', 30, 'gooey-mulch', '粘粘肥',
        'Berries regrow from dead plants an increased number of times.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (99, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive a lileep.',
        100, 'root-fossil', '根状化石', 'Can be revived into a Lileep.', '很久以前栖息在海里的
古代宝可梦的化石。
好像是根的一部分。', null),
       (100, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive an anorith.',
        100, 'claw-fossil', '爪子化石', 'Can be revived into an Anorith.', '很久以前栖息在海里的
古代宝可梦的化石。
好像是爪子的一部分。', null),
       (101, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive an omanyte.',
        100, 'helix-fossil', '贝壳化石', 'Can be revived into an Omanyte.', '很久以前栖息在海里的
古代宝可梦的化石。
好像是贝壳的一部分。', null),
       (102, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive a kabuto.',
        100, 'dome-fossil', '甲壳化石', 'Can be revived into a Kabuto.', '很久以前栖息在海里的
古代宝可梦的化石。
好像是甲壳的一部分。', null),
       (103, 10000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive an aerodactyl.',
        100, 'old-amber', '秘密琥珀', 'Can be revived into an Aerodactyl.', '封存着古代宝可梦
遗传基因的琥珀，
透着点红色。', null),
       (104, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive a shieldon.',
        100, 'armor-fossil', '盾甲化石', 'Can be revived into a Shieldon.', '很久以前生活在地上的
古代宝可梦的化石。
好像是领饰的一部分。', null),
       (105, 7000,
        'Give to a scientist in the mining museum in oreburgh city or the Museum of Science in pewter city to receive a cranidos.',
        100, 'skull-fossil', '头盖化石', 'Can be revived into a Cranidos.', '很久以前生活在地上的
古代宝可梦的化石。
好像是头部的一部分。', null),
       (106, 5000, 'Vendor trash.', 100, 'rare-bone', '贵重骨头',
        'Sell for 5000 Pokédollars, or to Bone Man for 10000 Pokédollars.', '在宝可梦考古学上
非常贵重的骨头。
可以在商店高价出售。', null),
       (107, 3000, 'Used on a party Pokémon
:   Evolves a minccino into cinccino, a roselia into roserade, or a togetic into togekiss.', 80, 'shiny-stone',
        '光之石', 'Evolves a Minccino into Cinccino, a Roselia into Roserade, or a Togetic into Togekiss.', '能让某些特定宝可梦
进化的神奇石头。
像光芒一样耀眼。', null),
       (108, 3000, 'Used on a party Pokémon
:   Evolves a lampent into chandelure, a misdreavus into mismagius, or a murkrow into honchkrow.', 80, 'dusk-stone',
        '暗之石', 'Evolves a Lampent into Chandelure, a Misdreavus into Mismagius, or a Murkrow into Honchkrow.', '能让某些特定宝可梦
进化的神奇石头。
像黑夜一般漆黑。', null),
       (109, 3000, 'Used on a party Pokémon
:   Evolves a male kirlia into gallade or a female snorunt into froslass.', 80, 'dawn-stone', '觉醒之石',
        'Evolves a male Kirlia into Gallade or a female Snorunt into Froslass.', '能让某些特定宝可梦
进化的神奇石头。
像眼眸一般光彩动人。', null),
       (110, 2000, 'Held by happiny
:   Holder evolves into chansey when it levels up during the daytime.', 80, 'oval-stone', '浑圆之石',
        'Level-up during Day on a Happiny: Holder evolves into Chansey.', '能让某些特定宝可梦
进化的神奇石头。
像珠子一般圆润。', null),
       (111, 2100,
        'Place in the tower on sinnoh route 209.  Check the stone to encounter a spiritomb, as long as the trainer’s Underground status card counts at least 32 greetings.',
        80, 'odd-keystone', '楔石',
        'Use on the tower on Route 209 to encounter Spiritomb if you have at least 32 Underground greetings.', '没有它，石之塔就会
崩塌的重要石头。
有时能从石头里听到声音。', null),
       (112, 0, 'Held by dialga
:   Holder’s dragon- and steel-type moves have 1.2× their usual power.', 60, 'adamant-orb', '金刚宝珠',
        'Boosts the damage from Dialga’s Dragon-type and Steel-type moves by 20%.', '让帝牙卢卡携带的话，
龙和钢属性的招式威力就会提高。
散发着光辉的宝珠。', null),
       (113, 0, 'Held by palkia
:   Holder’s dragon- and water-type moves have 1.2× their usual power.', 60, 'lustrous-orb', '白玉宝珠',
        'Boosts the damage from Palkia’s Dragon-type and Water-type moves by 20%.', '让帕路奇亚携带的话，
龙和水属性的招式威力就会提高。
散发着美丽光辉的宝珠。', null),
       (114, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'grass-mail', 'Grass Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a
refreshingly green field.
Let a Pokémon hold it for delivery.', null),
       (115, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'flame-mail', 'Flame Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of flames
in blazing red.
Let a Pokémon hold it for delivery.', null),
       (116, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bubble-mail', 'Bubble Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a blue
world underwater.
Let a Pokémon hold it for delivery.', null),
       (117, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bloom-mail', 'Bloom Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of pretty
floral patterns.
Let a Pokémon hold it for delivery.', null),
       (118, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'tunnel-mail', 'Tunnel Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a dimly
lit coal mine.
Let a Pokémon hold it for delivery.', null),
       (119, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'steel-mail', 'Steel Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of cool
mechanical designs.
Let a Pokémon hold it for delivery.', null),
       (120, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'heart-mail', 'Heart Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of giant
heart patterns.
Let a Pokémon hold it for delivery.', null),
       (121, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'snow-mail', 'Snow Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a
chilly, snow-covered world.
Let a Pokémon hold it for delivery.', null),
       (122, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'space-mail', 'Space Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print depicting
the huge expanse of space.
Let a Pokémon hold it for delivery.', null),
       (123, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'air-mail', 'Air Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of
colorful letter sets.
Let a Pokémon hold it for delivery.', null),
       (124, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'mosaic-mail', 'Mosaic Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a vivid
rainbow pattern.
Let a Pokémon hold it for delivery.', null),
       (125, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'brick-mail', 'Brick Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'Stationery featuring a print of a
tough-looking brick pattern.
Let a Pokémon hold it for delivery.', null),
       (126, 80, 'Held in battle
:   When the holder is paralyzed, it consumes this item to cure the paralysis.

Used on a party Pokémon
:   Cures paralysis.', 10, 'cheri-berry', '樱子果', 'Held: Consumed when paralyzed to cure paralysis.', '让宝可梦携带后，
可以治愈麻痹。', 3),
       (127, 80, 'Held in battle
:   When the holder is asleep, it consumes this item to wake up.

Used on a party Pokémon
:   Cures sleep.', 10, 'chesto-berry', '零余果', 'Held: Consumed when asleep to cure sleep.', '让宝可梦携带后，
可以治愈睡眠。', 3),
       (128, 80, 'Held in battle
:   When the holder is poisoned, it consumes this item to cure the poison.

Used on a party Pokémon
:   Cures poison.', 10, 'pecha-berry', '桃桃果', 'Held: Consumed when poisoned to cure poison.', '让宝可梦携带后，
可以治愈中毒。', 3),
       (129, 80, 'Held in battle
:   When the holder is burned, it consumes this item to cure the burn.

Used on a party Pokémon
:   Cures a burn.', 10, 'rawst-berry', '莓莓果', 'Held: Consumed when burned to cure a burn.', '让宝可梦携带后，
可以治愈灼伤。', 3),
       (130, 80, 'Held in battle
:   When the holder is frozen, it consumes this item to thaw itself.

Used on a party Pokémon
:   Cures freezing.', 10, 'aspear-berry', '利木果', 'Held: Consumed when frozen to cure frozen.', '让宝可梦携带后，
可以治愈冰冻。', 3),
       (331, 1000, 'Teaches Return to a compatible Pokémon.', null, 'tm27', '招式学习器２７',
        'Teaches Return to a compatible Pokémon. (Gen I: Fissure)', '为了训练家而
全力攻击对手。
亲密度越高，威力越大。', null),
       (131, 80, 'Held in battle
:   When the holder is out of PP for one of its moves, it consumes this item to restore 10 of that move’s PP.

Used on a party Pokémon
:   Restores 10 PP for a selected move.', 10, 'leppa-berry', '苹野果',
        'Held: Consumed when a move runs out of PP to restore its PP by 10.', '让宝可梦携带后，
可以回复１０ＰＰ。', 3),
       (132, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 10 HP.

Used on a party Pokémon
:   Restores 10 HP.', 10, 'oran-berry', '橙橙果', 'Held: Consumed at 1/2 max HP to recover 10 HP.', '让宝可梦携带后，
可以回复１０ＨＰ。', 3),
       (133, 80, 'Held in battle
:   When the holder is confused, it consumes this item to cure the confusion.

Used on a party Pokémon
:   Cures confusion.', 10, 'persim-berry', '柿仔果', 'Held: Consumed when confused to cure confusion.', '让宝可梦携带后，
可以治愈混乱。', 3),
       (134, 80, 'Held in battle
:   When the holder is afflicted with a major status ailment, it consumes this item to cure the ailment.

Used on a party Pokémon
:   Cures any major status ailment.', 10, 'lum-berry', '木子果',
        'Held: Consumed to cure any status condition or confusion.', '让宝可梦携带后，
可以治愈所有异常状态。', 3),
       (135, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/4 its max HP.

Used on a party Pokémon
:   Restores 1/4 the Pokémon’s max HP.', 10, 'sitrus-berry', '文柚果',
        'Held: Consumed at 1/2 max HP to recover 1/4 max HP.', '让宝可梦携带后，
可以回复少量ＨＰ。', 3),
       (136, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/8 its max HP.  If the holder dislikes spicy flavors (i.e., has a nature that lowers Attack), it will also become confused.',
        10, 'figy-berry', '勿花果',
        'Held: Consumed at 1/2 max HP to restore 1/8 max HP. Confuses Pokémon that dislike spicy flavor.', '让宝可梦携带后，
危机时可以回复ＨＰ。
如果讨厌这味道就会混乱。', 3),
       (137, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/8 its max HP.  If the holder dislikes dry flavors (i.e., has a nature that lowers Special Attack), it will also become confused.',
        10, 'wiki-berry', '异奇果',
        'Held: Consumed at 1/2 max HP to restore 1/8 max HP. Confuses Pokémon that dislike dry flavor.', '让宝可梦携带后，
危机时可以回复ＨＰ。
如果讨厌这味道就会混乱。', 3),
       (138, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/8 its max HP.  If the holder dislikes sweet flavors (i.e., has a nature that lowers Speed), it will also become confused.',
        10, 'mago-berry', '芒芒果',
        'Held: Consumed at 1/2 max HP to restore 1/8 max HP. Confuses Pokémon that dislike sweet flavor.', '让宝可梦携带后，
危机时可以回复ＨＰ。
如果讨厌这味道就会混乱。', 3),
       (139, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/8 its max HP.  If the holder dislikes bitter flavors (i.e., has a nature that lowers Special Defense), it will also become confused.',
        10, 'aguav-berry', '乐芭果',
        'Held: Consumed at 1/2 max HP to restore 1/8 max HP. Confuses Pokémon that dislike bitter flavor.', '让宝可梦携带后，
危机时可以回复ＨＰ。
如果讨厌这味道就会混乱。', 3),
       (140, 80, 'Held in battle
:   When the holder has 1/2 its max HP remaining or less, it consumes this item to restore 1/8 its max HP.  If the holder dislikes sour flavors (i.e., has a nature that lowers Defense), it will also become confused.',
        10, 'iapapa-berry', '芭亚果',
        'Held: Consumed at 1/2 max HP to restore 1/8 max HP. Confuses Pokémon that dislike sour flavor.', '让宝可梦携带后，
危机时可以回复ＨＰ。
如果讨厌这味道就会混乱。', 3),
       (141, 200, 'No effect; only useful for planting and cooking.', 10, 'razz-berry', '蔓莓果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨帅气。
红色的果实尝起来是辣的。', null),
       (142, 20, 'No effect; only useful for planting and cooking.', 10, 'bluk-berry', '墨莓果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨美丽。
蓝色的果实尝起来是涩的。', null),
       (143, 200, 'No effect; only useful for planting and cooking.', 10, 'nanab-berry', '蕉香果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨可爱。
粉红色的果实尝起来是甜的。', null),
       (144, 20, 'No effect; only useful for planting and cooking.', 10, 'wepear-berry', '西梨果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨聪明。
绿色的果实尝起来是苦的。', null),
       (145, 200, 'No effect; only useful for planting and cooking.', 10, 'pinap-berry', '凰梨果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨强壮。
黄色的果实尝起来是酸的。', null),
       (146, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers HP effort by 10.', 10, 'pomeg-berry', '榴石果',
        'Drops HP Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但ＨＰ的基础点数会降低。', null),
       (147, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers Attack effort by 10.', 10, 'kelpsy-berry', '藻根果',
        'Drops Attack Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但攻击的基础点数会降低。', null),
       (148, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers Defense effort by 10.', 10, 'qualot-berry', '比巴果',
        'Drops Defense Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但防御的基础点数会降低。', null),
       (149, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers Special Attack effort by 10.', 10, 'hondew-berry', '哈密果',
        'Drops Special Attack Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但特攻的基础点数会降低。', null),
       (150, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers Special Defense effort by 10.', 10, 'grepa-berry', '萄葡果',
        'Drops Special Defense Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但特防的基础点数会降低。', null),
       (151, 80, 'Used on a party Pokémon
:   Increases happiness by 10/5/2.  Lowers Speed effort by 10.', 10, 'tamato-berry', '茄番果',
        'Drops Speed Effort Values by 10 and raises happiness.', '如果把它交给宝可梦，
宝可梦就会变得非常容易和训练家亲密，
但速度的基础点数会降低。', null),
       (332, 100000, 'Teaches Dig to a compatible Pokémon.', null, 'tm28', '招式学习器２８',
        'Teaches Dig to a compatible Pokémon.', '吸取血液攻击对手。
可以回复给予对手
伤害的一半ＨＰ。', null),
       (152, 20, 'No effect; only useful for planting and cooking.', 10, 'cornn-berry', '玉黍果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨美丽。
在其他地区很少见的树果。', null),
       (153, 20, 'No effect; only useful for planting and cooking.', 10, 'magost-berry', '岳竹果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨可爱。
在其他地区很少见的树果。', null),
       (154, 20, 'No effect; only useful for planting and cooking.', 10, 'rabuta-berry', '茸丹果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨聪明。
在其他地区很少见的树果。', null),
       (155, 20, 'No effect; only useful for planting and cooking.', 10, 'nomel-berry', '檬柠果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨强壮。
在其他地区很少见的树果。', null),
       (156, 20, 'No effect; only useful for planting and cooking.', 10, 'spelon-berry', '刺角果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨可爱。
在其他地区很少见的树果。', null),
       (157, 20, 'No effect; only useful for planting and cooking.', 10, 'pamtre-berry', '椰木果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨美丽。
在其他地区很少见的树果。', null),
       (158, 20, 'No effect; only useful for planting and cooking.', 10, 'watmel-berry', '瓜西果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨聪明。
在其他地区很少见的树果。', null),
       (159, 20, 'No effect; only useful for planting and cooking.', 10, 'durin-berry', '金枕果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨聪明。
在其他地区很少见的树果。', null),
       (160, 20, 'No effect; only useful for planting and cooking.', 10, 'belue-berry', '靛莓果',
        'Used for creating PokéBlocks and Poffins.', '用于制作宝可方块，
制作出来的宝可方块可用来打磨美丽。
在其他地区很少见的树果。', null),
       (161, 80, 'Held in battle
:   When the holder would take super-effective fire-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'occa-berry', '巧可果',
        'Held: Consumed when struck by a super-effective Fire-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的火属性招式
攻击时，能令其威力减弱。', null),
       (162, 80, 'Held in battle
:   When the holder would take super-effective water-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'passho-berry', '千香果',
        'Held: Consumed when struck by a super-effective Water-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的水属性招式
攻击时，能令其威力减弱。', null),
       (163, 80, 'Held in battle
:   When the holder would take super-effective electric-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'wacan-berry', '烛木果',
        'Held: Consumed when struck by a super-effective Electric-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的电属性招式
攻击时，能令其威力减弱。', null),
       (164, 80, 'Held in battle
:   When the holder would take super-effective grass-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'rindo-berry', '罗子果',
        'Held: Consumed when struck by a super-effective Grass-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的草属性招式
攻击时，能令其威力减弱。', null),
       (165, 80, 'Held in battle
:   When the holder would take super-effective ice-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'yache-berry', '番荔果',
        'Held: Consumed when struck by a super-effective Ice-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的冰属性招式
攻击时，能令其威力减弱。', null),
       (166, 80, 'Held in battle
:   When the holder would take super-effective fighting-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'chople-berry', '莲蒲果',
        'Held: Consumed when struck by a super-effective Fighting-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的格斗属性招式
攻击时，能令其威力减弱。', null),
       (167, 80, 'Held in battle
:   When the holder would take super-effective poison-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'kebia-berry', '通通果',
        'Held: Consumed when struck by a super-effective Poison-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的毒属性招式
攻击时，能令其威力减弱。', null),
       (168, 80, 'Held in battle
:   When the holder would take super-effective ground-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'shuca-berry', '腰木果',
        'Held: Consumed when struck by a super-effective Ground-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的地面属性招式
攻击时，能令其威力减弱。', null),
       (169, 80, 'Held in battle
:   When the holder would take super-effective flying-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'coba-berry', '棱瓜果',
        'Held: Consumed when struck by a super-effective Flying-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的飞行属性招式
攻击时，能令其威力减弱。', null),
       (170, 80, 'Held in battle
:   When the holder would take super-effective psychic-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'payapa-berry', '福禄果',
        'Held: Consumed when struck by a super-effective Psychic-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的超能力属性招式
攻击时，能令其威力减弱。', null),
       (171, 80, 'Held in battle
:   When the holder would take super-effective bug-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'tanga-berry', '扁樱果',
        'Held: Consumed when struck by a super-effective Bug-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的虫属性招式
攻击时，能令其威力减弱。', null),
       (172, 80, 'Held in battle
:   When the holder would take super-effective rock-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'charti-berry', '草蚕果',
        'Held: Consumed when struck by a super-effective Rock-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的岩石属性招式
攻击时，能令其威力减弱。', null),
       (173, 80, 'Held in battle
:   When the holder would take super-effective ghost-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'kasib-berry', '佛柑果',
        'Held: Consumed when struck by a super-effective Ghost-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的幽灵属性招式
攻击时，能令其威力减弱。', null),
       (1697, 20000, null, null, 'ability-shield', '特性护具', null, null, null),
       (1698, 30000, null, null, 'clear-amulet', '清净坠饰', null, null, null),
       (174, 80, 'Held in battle
:   When the holder would take super-effective dragon-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'haban-berry', '莓榴果',
        'Held: Consumed when struck by a super-effective Dragon-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的龙属性招式
攻击时，能令其威力减弱。', null),
       (175, 80, 'Held in battle
:   When the holder would take super-effective dark-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'colbur-berry', '刺耳果',
        'Held: Consumed when struck by a super-effective Dark-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的恶属性招式
攻击时，能令其威力减弱。', null),
       (176, 80, 'Held in battle
:   When the holder would take super-effective steel-type damage, it consumes this item to halve the amount of damage taken.',
        10, 'babiri-berry', '霹霹果',
        'Held: Consumed when struck by a super-effective Steel-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的钢属性招式
攻击时，能令其威力减弱。', null),
       (177, 80, 'Held in battle
:   When the holder would take normal-type damage, it consumes this item to halve the amount of damage taken.', 10,
        'chilan-berry', '灯浆果', 'Held: Consumed when struck by a Normal-type attack to halve the damage.', '让宝可梦携带后，
在受到一般属性招式攻击时，
能令其威力减弱。', null),
       (178, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its Attack by one stage.', 10,
        'liechi-berry', '枝荔果', 'Held: Consumed at 1/4 max HP to boost Attack.', '让宝可梦携带后，危机时，
自己的攻击就会提高。', 3),
       (179, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its Defense by one stage.', 10,
        'ganlon-berry', '龙睛果', 'Held: Consumed at 1/4 max HP to boost Defense.', '让宝可梦携带后，危机时，
自己的防御就会提高。', 3),
       (180, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its Speed by one stage.', 10,
        'salac-berry', '沙鳞果', 'Held: Consumed at 1/4 max HP to boost Speed.', '让宝可梦携带后，危机时，
自己的速度就会提高。', 3),
       (181, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its Special Attack by one stage.',
        10, 'petaya-berry', '龙火果', 'Held: Consumed at 1/4 max HP to boost Special Attack.', '让宝可梦携带后，危机时，
自己的特攻就会提高。', 3),
       (182, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its Special Defense by one stage.',
        10, 'apicot-berry', '杏仔果', 'Held: Consumed at 1/4 max HP to boost Special Defense.', '让宝可梦携带后，危机时，
自己的特防就会提高。', 3),
       (183, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise its critical hit chance by one stage.',
        10, 'lansat-berry', '兰萨果', 'Held: Consumed at 1/4 max HP to boost critical hit ratio by two stages.', '让宝可梦携带后，危机时，
攻击会变得容易击中要害。', 3),
       (184, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item to raise a random stat by two stages.',
        10, 'starf-berry', '星桃果', 'Held: Consumed at 1/4 max HP to boost a random stat by two stages.', '让宝可梦携带后，危机时，
某一项能力就会大幅提高。', 3),
       (185, 80, 'Held in battle
:   When the holder takes super-effective damage, it consumes this item to restore 1/4 its max HP.', 10, 'enigma-berry',
        '谜芝果', 'Held: Consumed when struck by a super-effective attack to restore 1/4 max HP.', '让宝可梦携带后，
在受到效果绝佳的招式攻击时，
可以回复ＨＰ。', null),
       (186, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item, and its next used move has 1.2× its normal accuracy.',
        10, 'micle-berry', '奇秘果',
        'Held: Consumed at 1/4 max HP to boost accuracy of next move by 20%. (Gen IV: Perfect accuracy)', '让宝可梦携带后，危机时，
招式的命中率仅会提高１次。', 3),
       (187, 80, 'Held in battle
:   When the holder has 1/4 its max HP remaining or less, it consumes this item.  On the following turn, the holder will act first among moves with the same priority, regardless of Speed.',
        10, 'custap-berry', '释陀果', 'Held: Consumed at 1/4 max HP when using a move to go first.', '让宝可梦携带后，危机时，
行动仅会变快１次。', null),
       (188, 80, 'Held in battle
:   When the holder takes physical damage, it consumes this item to damage the attacking Pokémon for 1/8 its max HP.',
        10, 'jaboca-berry', '嘉珍果',
        'Held: Consumed to deal 1/8 attacker’s max HP when holder is struck by a physical attack.', '让宝可梦携带后，
在受到物理招式攻击时，
能给予对手伤害。', null),
       (189, 80, 'Held in battle
:   When the holder takes special damage, it consumes this item to damage the attacking Pokémon for 1/8 its max HP.',
        10, 'rowap-berry', '雾莲果',
        'Held: Consumed to deal 1/8 attacker’s max HP when holder is struck by a special attack.', '让宝可梦携带后，
在受到特殊招式攻击时，
能给予对手伤害。', null),
       (190, 4000, 'Held in battle
:   Moves targeting the holder have 0.9× chance to hit.', 10, 'bright-powder', '光粉',
        'Held: Increases the holder’s evasion by 1/9 (11 1/9%).', '闪闪发光的粉末。
携带后，光芒会迷惑对手，
从而使其招式变得不容易命中。', null),
       (191, 4000, 'Held in battle
:   At the end of each turn, if any of the holder’s stats have a negative stat modifier, the holder consumes this item to remove the modifiers from those stats.',
        10, 'white-herb', '白色香草', 'Held: Resets all lowered stats to normal at end of turn. Consumed after use.', '当携带它的宝可梦能力降低时，
仅能回到之前的状态１次。', 4),
       (192, 3000, 'Held
:   When the holder would gain effort due to battle, it gains double that effort instead.

Held in battle
:   Holder has half its Speed.', 60, 'macho-brace', '强制锻炼器',
        'Held: Holder gains double effort values from battles, but has halved Speed in battle.', '又硬又重的锻炼器。
虽然携带后速度会降低，
但会比平时更容易茁壮成长。', null),
       (193, 0, 'Held
:   Experience is split across two groups: Pokémon who participated in battle, and Pokémon holding this item.  Each Pokémon earns experience as though it had battled alone, divided by the number of Pokémon in its group, then divided by the number of groups. Pokémon holding this item who also participated in battle effectively earn experience twice.

    Fainted Pokémon never earn experience, and empty groups are
ignored; thus, if a single Pokémon is holding this item and the only Pokémon who battled faints from explosion, the holder will gain full experience.',
        null, 'exp-share', '学习装置',
        'Held: Half the experience from a battle is split between Pokémon holding this item.', '打开开关后，
能让同行的所有宝可梦
获得经验值的装置。', null),
       (556, 0, 'Provides access to Navel Rock, ho oh, and lugia.', null, 'mysticticket', 'MysticTicket',
        'Allows access to Navel Rock and Lugia or Ho-oh.', 'A ticket required
to board the ship
to NAVEL ROCK.', null),
       (194, 4000, 'Held in battle
:   Whenever the holder attempts to use a move, it has a 3/16 chance to act first among moves with the same priority.  If multiple Pokémon have this effect at the same time, Speed is the tie-breaker as normal, but the effect of trick room is ignored.',
        80, 'quick-claw', '先制之爪', 'Held: Holder has a 3/16 (18.75%) chance to move first.', '又轻又尖锐的爪子。
携带后，有时能比对手
先一步行动。', null),
       (195, 4000, 'Held
:   When the holder would earn happiness for any reason, it earns twice that amount instead.', 10, 'soothe-bell',
        '安抚之铃', 'Held: Doubles the happiness earned by the holder.', '音色悦耳的铃铛。
携带它的宝可梦会受到安抚，
变得容易和训练家亲密。', null),
       (196, 4000, 'Held in battle
:   When the holder is attracted, it consumes this item to cure the attraction.', 10, 'mental-herb', '心灵香草',
        'Held: Consumed to cure infatuation. Gen V: Also removes Taunt, Encore, Torment, Disable, and Cursed Body.', '当携带它的宝可梦
无法自由使出招式时，
仅会回复１次。', 4),
       (197, 4000, 'Held in battle
:   Holder has 1.5× its Attack.  When the holder attempts to use a move, all its other moves are disabled until it leaves battle or loses this item.

    The restriction ends even if this item is swapped for another Choice item
via trick or switcheroo.', 10, 'choice-band', '讲究头带',
        'Held: Increases Attack by 50%, but restricts the holder to only one move.', '有点讲究的头带。
虽然携带后攻击会提高，
但只能使出相同的招式。', null),
       (198, 5000, 'Held in battle
:   Holder’s damaging moves have a 10% chance to make their target flinch.  This chance applies independently to each hit of a multi-hit move.

    This item’s chance is rolled independently of any other move effects;
e.g., a move with a 30% chance to flinch normally will have a 37% total chance to flinch when used with this item, because 3% of the time, both effects activate.

Held by poliwhirl or slowbro
:   Holder evolves into politoed or slowking, respectively, when traded.', 30, 'kings-rock', '王者之证',
        'Held: Damaging moves gain a 10% chance to make their target flinch. Traded on a Poliwhirl: Holder evolves into Politoed. Traded on a Slowpoke: Holder evolves into Slowking.', '携带后进行攻击，
在造成伤害时，
有时会让对手畏缩。', 7),
       (199, 1000, 'Held in battle
:   Holder’s bug-type moves have 1.2× their power.
', 10, 'silver-powder', '银粉', 'Held: Bug-Type moves from holder do 20% more damage.', '散发着银色光辉的粉末。
携带后，虫属性的
招式威力就会提高。', null),
       (200, 10000, 'Held
:   If the holder participated in a trainer battle, the trainer earns twice the usual prize money.  This effect applies even if the holder fainted.

    This effect does not stack with any other similar effect.', 30, 'amulet-coin', '护符金币',
        'Held: Doubles the money earned from a battle. Does not stack with Luck Incense.', '只要携带它的宝可梦
在战斗时出场一次，
就能获得２倍金钱。', null),
       (201, 5000, 'Held by lead Pokémon: Prevents wild battles with Pokémon that are lower level than the holder.', 30,
        'cleanse-tag', '洁净之符', 'Prevents wild encounters of level lower than your party’s lead Pokémon.', '让排在最前面的宝可梦携带后，
野生宝可梦就会不容易出现。', null),
       (202, 0, 'Held by Latias or Latios: Increases the holder’s Special Attack and Special Defense by 50%.', 30,
        'soul-dew', '心之水滴', 'Raises Latias and Latios’s Special Attack and Special Defense by 50%.', '让拉帝欧斯或拉帝亚斯携带后，
超能力和龙属性的招式威力
就会提高的神奇珠子。', null),
       (203, 2000,
        'Held by Clamperl: Doubles the holder’s Special Attack.  Evolves the holder into Huntail when traded.', 90,
        'deep-sea-tooth', '深海之牙',
        'Doubles Clamperl’s Special Attack. Traded on a Clamperl: Holder evolves into Huntail.', '让珍珠贝携带后，
特攻就会提高的牙齿。
散发着闪亮的银光。', null),
       (204, 2000,
        'Held by Clamperl: Doubles the holder’s Special Defense.  Evolves the holder into Gorebyss when traded.', 30,
        'deep-sea-scale', '深海鳞片',
        'Doubles Clamperl’s Special Defense. Traded on a Clamperl: Holder evolves into Gorebyss.', '让珍珠贝携带后，
特防就会提高的鳞片。
散发着淡淡的粉红色光芒。', null),
       (205, 4000, 'Held: In wild battles, attempts to run away on the holder’s turn will always succeed.', 30,
        'smoke-ball', '烟雾球', 'Held: Allows the Holder to escape from any wild battle.', '携带它的宝可梦在和
野生宝可梦的战斗中
绝对可以逃走。', null),
       (206, 3000,
        'Held: Prevents the holder from evolving naturally.  Evolution initiated by the trainer (Stones, etc) will still work.',
        30, 'everstone', '不变之石', 'Held: Prevents level-based evolution from occuring.', '携带后，宝可梦在此期间
不会进化的神奇石头。', null),
       (207, 4000,
        'Held: If the holder is attacked for regular damage that would faint it, this item has a 10% chance to prevent the holder’s HP from lowering below 1.',
        10, 'focus-band', '气势头带',
        'Held: Holder has 10% chance to survive attacks or self-inflicted damage at 1 HP.', '携带后，即便受到
可能会导致濒死的招式，
有时也能仅以１ＨＰ撑过去。', null),
       (208, 10000, 'Held: Increases any Exp the holder gains by 50%.', 30, 'lucky-egg', '幸运蛋',
        'Held: Increases EXP earned in battle by 50%.', '满载着幸福的蛋。
携带它的宝可梦获得的
经验值会少量增加。', null),
       (209, 4000, 'Held: Raises the holder’s critical hit counter by 1.', 30, 'scope-lens', '焦点镜',
        'Held: Raises the holder’s critical hit ratio by one stage.', '能看见弱点的镜片。
携带它的宝可梦的招式
会变得容易击中要害。', null),
       (210, 2000, 'Held: Increases the power of the holder’s Steel moves by 20%.
Held by Onix or Scyther: Evolves the holder into Steelix or Scizor when traded, respectively.', 30, 'metal-coat',
        '金属膜', 'Held: Steel-Type moves from holder do 20% more damage.', '特殊的金属膜。
携带后，钢属性的
招式威力就会提高。', null),
       (211, 4000, 'Held: Heals the holder by 1/16 its max HP at the end of each turn.', 10, 'leftovers', '吃剩的东西',
        'Held: Restores 1/16 (6.25%) holder’s max HP at the end of each turn.', '携带后，宝可梦的ＨＰ
会在战斗期间缓缓回复。', null),
       (212, 2000, 'Held by Seadra: Evolves the holder into Kingdra when traded.', 30, 'dragon-scale', '龙之鳞片',
        'Traded on a Seadra: Holder evolves into Kingdra.', '又硬又坚固的鳞片。
龙属性宝可梦有时会携带它。', null),
       (213, 1000, 'Held by Pikachu: Doubles the holder’s initial Attack and Special Attack.', 30, 'light-ball',
        '电气球',
        'Doubles Pikachu’s Attack and Special Attack. Breed on Pikachu or Raichu: Pichu Egg will have Volt Tackle.', '让皮卡丘携带后，
攻击和特攻的威力
就会提高的神奇之球。', 5),
       (214, 1000, 'Held: Increases the power of the holder’s Ground moves by 20%.', 10, 'soft-sand', '柔软沙子',
        'Held: Ground-Type moves from holder do 20% more damage.', '手感细腻的沙子。
携带后，地面属性的
招式威力就会提高。', null),
       (215, 1000, 'Held: Increases the power of the holder’s Rock moves by 20%.', 100, 'hard-stone', '硬石头',
        'Held: Rock-Type moves from holder do 20% more damage.', '绝对不会裂开的石头。
携带后，岩石属性的
招式威力就会提高。', null),
       (216, 1000, 'Held: Increases the power of the holder’s Grass moves by 20%.', 30, 'miracle-seed', '奇迹种子',
        'Held: Grass-Type moves from holder do 20% more damage.', '孕育生命的种子。
携带后，草属性的
招式威力就会提高。', null),
       (217, 1000, 'Held: Increases the power of the holder’s Dark moves by 20%.', 30, 'black-glasses', '黑色眼镜',
        'Held: Dark-Type moves from holder do 20% more damage.', '看上去很奇怪的眼镜。
携带后，恶属性的
招式威力就会提高。', null),
       (218, 1000, 'Held: Increases the power of the holder’s Fighting moves by 20%.', 30, 'black-belt', '黑带',
        'Held: Fighting-Type moves from holder do 20% more damage.', '能振作精神的带子。
携带后，格斗属性的
招式威力就会提高。', null),
       (219, 1000, 'Held: Increases the power of the holder’s Electric moves by 20%.', 30, 'magnet', '磁铁',
        'Held: Electric-Type moves from holder do 20% more damage.', '强力的磁铁。
携带后，电属性的
招式威力就会提高。', null),
       (220, 1000, 'Held: Increases the power of the holder’s Water moves by 20%.', 30, 'mystic-water', '神秘水滴',
        'Held: Water-Type moves from holder do 20% more damage.', '水滴形状的宝石。
携带后，水属性的
招式威力就会提高。', null),
       (221, 1000, 'Held: Increases the power of the holder’s Flying moves by 20%.', 50, 'sharp-beak', '锐利鸟嘴',
        'Held: Flying-Type moves from holder do 20% more damage.', '又长又尖的鸟嘴。
携带后，飞行属性的
招式威力就会提高。', null),
       (222, 1000, 'Held: Increases the power of the holder’s Poison moves by 20%.', 70, 'poison-barb', '毒针',
        'Held: Poison-Type moves from holder do 20% more damage.', '有毒的小针。
携带后，毒属性的
招式威力就会提高。', 6),
       (223, 1000, 'Held: Increases the power of the holder’s Ice moves by 20%.', 30, 'never-melt-ice', '不融冰',
        'Held: Ice-Type moves from holder do 20% more damage.', '能隔绝热量的冰。
携带后，冰属性的
招式威力就会提高。', null),
       (224, 1000, 'Held: Increases the power of the holder’s Ghost moves by 20%.', 30, 'spell-tag', '诅咒之符',
        'Held: Ghost-Type moves from holder do 20% more damage.', '古怪可怕的咒符。
携带后，幽灵属性的
招式威力就会提高。', null),
       (225, 1000, 'Held: Increases the power of the holder’s Psychic moves by 20%.', 30, 'twisted-spoon', '弯曲的汤匙',
        'Held: Psychic-Type moves from holder do 20% more damage.', '注入了念力的汤匙。
携带后，超能力属性的
招式威力就会提高。', null),
       (226, 1000, 'Held: Increases the power of the holder’s Fire moves by 20%.', 30, 'charcoal', '木炭',
        'Held: Fire-Type moves from holder do 20% more damage.', '焚烧用的燃料。
携带后，火属性的
招式威力就会提高。', null),
       (227, 1000, 'Held: Increases the power of the holder’s Dragon moves by 20%.', 70, 'dragon-fang', '龙之牙',
        'Held: Dragon-Type moves from holder do 20% more damage.', '坚硬锐利的牙齿。
携带后，龙属性的
招式威力就会提高。', null),
       (228, 1000, 'Held: Increases the power of the holder’s Normal moves by 20%.', 10, 'silk-scarf', '丝绸围巾',
        'Held: Normal-Type moves from holder do 20% more damage.', '手感不错的围巾。
携带后，一般属性的
招式威力就会提高。', null),
       (229, 2000, 'Held by Porygon: Evolves the holder into Porygon2 when traded.', 30, 'up-grade', '升级数据',
        'Traded on a Porygon: Holder evolves into Porygon2.', '内部储存了各种信息的透明机器。
西尔佛公司制造。', null),
       (230, 4000, 'Held: Heals the holder by 1/8 of any damage it inflicts.', 30, 'shell-bell', '贝壳之铃',
        'Held: Holder receives 1/8 of the damage it deals when attacking.', '当携带它的宝可梦
攻击对手并造成伤害时，
能回复少量ＨＰ。', null),
       (231, 2000, 'Held: Increases the power of the holder’s Water moves by 20%.', 10, 'sea-incense', '海潮薰香',
        'Held: Water-Type moves from holder do 20% more damage. Breeding: Marill or Azumarill beget an Azurill Egg.', '有着神奇香气的薰香。
携带后，水属性的
招式威力就会提高。', null),
       (232, 5000, 'Held: Increases the holder’s Evasion by 5%.', 10, 'lax-incense', '悠闲薰香',
        'Held: Holder’s evasion is increased by 5%. Breeding: Wobbuffet begets a Wynaut Egg.', '携带后，薰香的
神奇香气会迷惑对手，
其招式会变得不容易命中。', null),
       (233, 1000, 'Held by Chansey: Raises the holder’s critical hit counter by 2.', 40, 'lucky-punch', '吉利拳',
        'Raises Chansey’s critical hit ratio by two stages.', '能带来幸运的拳套。
让吉利蛋携带后，
招式会变得容易击中要害。', null),
       (234, 1000, 'Held by Ditto: Increases the holder’s initial Defense and Special Defense by 50%.', 10,
        'metal-powder', '金属粉',
        'Raises Ditto’s Defense and Special Defense by 50%. The boost is lost after transforming.', '让百变怪携带后，
防御就会提高的神奇粉末。
非常细腻坚硬。', null),
       (235, 1000, 'Held by Cubone or Marowak: Doubles the holder’s Attack.', 90, 'thick-club', '粗骨头',
        'Doubles Cubone or Marowak’s Attack.', '某种坚硬的骨头。
让卡拉卡拉或嘎啦嘎啦携带后，
攻击就会提高。', null),
       (236, 1000, 'Held by Farfetch’d: Raises the holder’s critical hit counter by 2.', 60, 'stick', '大葱',
        'Raises Farfetch’d’s critical hit ratio by two stages.', '非常长且坚硬的茎。
让大葱鸭携带后，
招式会变得容易击中要害。', null),
       (237, 100, 'Held: Increases the holder’s Coolness during a Super Contest’s Visual Competition.', 10, 'red-scarf',
        '红色头巾', 'Raises the holder’s Coolness while in a contest.', '携带它去参加华丽大赛的
宝可梦会比平时
看上去更加帅气。', null),
       (238, 100, 'Held: Increases the holder’s Beauty during a Super Contest’s Visual Competition.', 10, 'blue-scarf',
        '蓝色头巾', 'Raises the holder’s Beauty while in a contest.', '携带它去参加华丽大赛的
宝可梦会比平时
看上去更加美丽。', null),
       (239, 100, 'Held: Increases the holder’s Cuteness during a Super Contest’s Visual Competition.', 10,
        'pink-scarf', '粉红头巾', 'Raises the holder’s Cuteness while in a contest.', '携带它去参加华丽大赛的
宝可梦会比平时
看上去更加可爱。', null),
       (240, 100, 'Held: Increases the holder’s Smartness during a Super Contest’s Visual Competition.', 10,
        'green-scarf', '绿色头巾', 'Raises the holder’s Smartness while in a contest.', '携带它去参加华丽大赛的
宝可梦会比平时
看上去更加聪明。', null),
       (241, 100, 'Held: Increases the holder’s Toughness during a Super Contest’s Visual Competition.', 10,
        'yellow-scarf', '黄色头巾', 'Raises the holder’s Toughness while in a contest.', '携带它去参加华丽大赛的
宝可梦会比平时
看上去更加强壮。', null),
       (242, 4000,
        'Held: Increases the accuracy of any move the holder uses by 10% (multiplied; i.e. 70% accuracy is increased to 77%).',
        10, 'wide-lens', '广角镜', 'Held: Provides a 1/10 (10%) boost in accuracy to the holder.', '能放大观看物体的镜片。
携带后，招式的命中率
就会少量提高。', null),
       (243, 4000, 'Held: Increases the power of the holder’s physical moves by 10%.', 10, 'muscle-band', '力量头带',
        'Held: Boosts the damage of physical moves used by the holder by 10%.', '力如泉涌的头带。
携带后，物理招式的
威力就会少量提高。', null),
       (244, 4000, 'Held: Increases the power of the holder’s special moves by 10%.', 10, 'wise-glasses', '博识眼镜',
        'Held: Boosts the damage of special moves used by the holder by 1/10 (10%).', '装着很厚镜片的眼镜。
携带后，特殊招式的
威力就会少量提高。', null),
       (245, 4000, 'Held: When the holder hits with a super-effective move, its power is raised by 20%.', 10,
        'expert-belt', '达人带', 'Held: Holder’s Super Effective moves do 20% extra damage.', '用惯了的黑色带子。
携带后，效果绝佳时的
招式威力就会少量提高。', null),
       (246, 4000,
        'Held: The holder’s Reflect and Light Screen will create effects lasting for eight turns rather than five.  As this item affects the move rather than the barrier itself, the effect is not lost if the holder leaves battle or drops this item.',
        30, 'light-clay', '光之黏土', 'Held: Light Screen and Reflect used by the holder last 8 rounds instead of 5.', '当携带它的宝可梦
使出光墙或反射壁时，
效果会比平时持续得更长。', null),
       (247, 4000,
        'Held: Damage from the holder’s moves is increased by 30%.  On each turn the holder uses a damage-inflicting move, it takes 10% its max HP in damage.',
        30, 'life-orb', '生命宝珠', 'Held: Holder’s moves inflict 30% extra damage, but cost 10% max HP.', '携带后，虽然每次攻击时
ＨＰ少量减少，
但招式的威力会提高。', null),
       (248, 4000,
        'Held: Whenever the holder uses a move that requires a turn to charge first (Bounce, Dig, Dive, Fly, Razor Wind, Skull Bash, Sky Attack, or Solarbeam), this item is consumed and the charge is skipped.  Skull Bash still provides a Defense boost.',
        10, 'power-herb', '强力香草', 'Held: Both turns of a two-turn charge move happen at once. Consumed upon use.', '携带它的宝可梦仅有１次机会
可以在第１回合使出
需要蓄力的招式。', null),
       (249, 4000, 'Held: Badly poisons the holder at the end of each turn.', 30, 'toxic-orb', '剧毒宝珠',
        'Held: Inflicts Toxic on the holder at the end of the turn. Activates after Poison damage would occur.', '触碰后会放出毒的神奇宝珠。
携带后，在战斗时会变成剧毒状态。', 1),
       (250, 4000, 'Held: Burns the holder at the end of each turn.', 30, 'flame-orb', '火焰宝珠',
        'Held: Inflicts Burn on the holder at the end of the turn. Activates after Burn damage would occur.', '触碰后会放出热量的神奇宝珠。
携带后，在战斗时会变成灼伤状态。', 2),
       (251, 1000, 'Held by Ditto: Doubles the holder’s initial Speed.', 10, 'quick-powder', '速度粉',
        'Doubles Ditto’s Speed when held. The boost is lost after transforming.', '让百变怪携带后，
速度就会提高的神奇粉末。
非常细腻坚硬。', null),
       (252, 4000,
        'Held: If the holder has full HP and is attacked for regular damage that would faint it, this item is consumed and prevents the holder’s HP from lowering below 1.  This effect works against multi-hit attacks, but does not work against the effects of Doom Desire or Future Sight.',
        10, 'focus-sash', '气势披带',
        'Held: Holder survives any single-hit attack at 1 HP if at max HP, then the item is consumed.', '携带后，在ＨＰ全满时，
即便受到可能会导致濒死的招式，
也能仅以１ＨＰ撑过去１次。', null),
       (253, 4000, 'Held: Raises the holder’s Accuracy by 20% when it goes last.
Ingame description is incorrect.', 10, 'zoom-lens', '对焦镜',
        'Held: Provides a 1/5 (20%) boost in accuracy if the holder moves after the target.', '当携带它的宝可梦比
对手行动迟缓时，
招式会变得容易命中。', null),
       (254, 4000,
        'Held: Each time the holder uses the same move consecutively, its power is increased by another 10% of its original, to a maximum of 100%.',
        30, 'metronome', '节拍器',
        'Held: Consectutive uses of the same attack have a cumulative damage boost of 10%. Maximum 100% boost.', '携带后，连续使出相同招式时，
威力就会提高。不再使出
相同招式时，威力就会复原。', null),
       (255, 4000,
        'Held: Decreases the holder’s Speed by 50%.  If the holder is Flying or has Levitate, it takes regular damage from Ground attacks and is suspectible to Spikes and Toxic Spikes.',
        130, 'iron-ball', '黑色铁球',
        'Held: Holder’s Speed is halved. Negates all Ground-type immunities, and makes Flying-types take neutral damage from Ground-type moves. Arena Trap. Spikes, and Toxic Spikes affect the holder.', '携带后，速度会降低。
飞行属性以及飘浮宝可梦
会被地面招式击中。', null),
       (256, 4000,
        'Held: The holder will go last within its move’s priority bracket, regardless of Speed.  If multiple Pokémon within the same priority bracket are subject to this effect, the slower Pokémon will go first.  The holder will move after Pokémon with Stall.  If the holder has Stall, Stall is ignored.  This item ignores Trick Room.',
        10, 'lagging-tail', '后攻之尾', 'Held: Holder moves last in its priority bracket.', '非常沉重的某种尾巴。
携带后，行动会比平时更加迟缓。', null),
       (257, 4000, 'Held: When the holder becomes Attracted, the Pokémon it is Attracted to becomes Attracted back.',
        10, 'destiny-knot', '红线', 'Held: Infatuates opposing Pokémon when holder is inflicted with infatuation.', '长长的鲜红色细线。
携带后，在自己着迷时
能让对手也着迷。', null),
       (258, 4000,
        'Held: If the holder is Poison-type, restores 1/16 max HP at the end of each turn.  Otherwise, damages the holder by 1/16 its max HP at the end of each turn.',
        30, 'black-sludge', '黑色污泥',
        'Held: Poison-type holder recovers 1/16 (6.25%) max HP each turn. Non-Poison-Types take 1/8 (12.5%) max HP damage.', '携带后，毒属性的宝可梦
会缓缓回复ＨＰ。
其他属性的话，ＨＰ则会减少。', null),
       (259, 4000,
        'Held: The holder’s Hail will create a hailstorm lasting for eight turns rather than five.  As this item affects the move rather than the weather itself, the effect is not lost if the holder leaves battle or drops this item.',
        40, 'icy-rock', '冰冷岩石', 'Held: Hail by the holder lasts 8 rounds instead of 5.', '携带它的宝可梦使出冰雹的话，
冰雹的时间就会比平时更长。', null),
       (260, 4000,
        'Held: The holder’s Sandstorm will create a sandstorm lasting for eight turns rather than five.  As this item affects the move rather than the weather itself, the effect is not lost if the holder leaves battle or drops this item.',
        10, 'smooth-rock', '沙沙岩石', 'Held: Sandstorm by the holder lasts 8 rounds instead of 5.', '携带它的宝可梦使出沙暴的话，
沙暴的时间就会比平时更长。', null),
       (261, 4000,
        'Held: The holder’s Sunny Day will create sunshine lasting for eight turns rather than five.  As this item affects the move rather than the weather itself, the effect is not lost if the holder leaves battle or drops this item.',
        60, 'heat-rock', '炽热岩石', 'Held: Sunny Day by the holder lasts 8 rounds instead of 5.', '携带它的宝可梦使出大晴天的话，
晴天的时间就会比平时更长。', null),
       (262, 4000,
        'Held: The holder’s Rain Dance will create rain lasting for eight turns rather than five.  As this item affects the move rather than the weather itself, the effect is not lost if the holder leaves battle or drops this item.',
        60, 'damp-rock', '潮湿岩石', 'Held: Rain Dance by the holder lasts 8 rounds instead of 5.', '携带它的宝可梦使出求雨的话，
下雨的时间就会比平时更长。', null),
       (1031, 1200, null, 30, 'polished-mud-ball', '光滑泥球', null, '用泥土制作的丸子。
花了大把时间打磨，
表面油光水滑。', null),
       (263, 4000, 'Held: Increases the duration of the holder’s multiturn (2-5 turn) moves by three turns.', 90,
        'grip-claw', '紧缠钩爪', 'Held: Holder’s multi-turn trapping moves last 5 turns.', '携带后，绑紧以及紧束等
会持续造成伤害的招式的
回合数会增加。', null),
       (264, 4000, 'Held: Increases the holder’s Speed by 50%, but restricts it to the first move it uses until it leaves battle or loses this item.  If this item is swapped for another Choice item via Trick or Switcheroo, the holder’s restriction is still lifted, but it will again be restricted to the next move it uses.
(Quirk: If the holder is switched in by U-Turn and it also knows U-Turn, U-Turn becomes its restricted move.)', 10,
        'choice-scarf', '讲究围巾', 'Held: Increases Speed by 50%, but restricts the holder to only one move.', '有点讲究的围巾。
虽然携带后速度会提高，
但只能使出相同的招式。', null),
       (265, 4000,
        'Held: Damaged the holder for 1/8 its max HP.  When the holder is struck by a contact move, damages the attacker for 1/8 its max HP; if the attacker is not holding an item, it will take this item.',
        80, 'sticky-barb', '附着针',
        'Held: Holder takes 1/8 (12.5%) its max HP at the end of each turn. When the holder is hit by a contact move, the attacking Pokémon takes 1/8 its max HP in damage and receive the item if not holding one.', '携带后，每回合都会受到伤害。
有时也会附着到
碰到自己的对手身上。', null),
       (266, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains Attack effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-bracer', '力量护腕', 'Held: Holder gains 4 Attack effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的攻击
会比平时成长得更高。', null),
       (267, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains Defense effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-belt', '力量腰带', 'Held: Holder gains 4 Defense effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的防御
会比平时成长得更高。', null),
       (268, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains Special Attack effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-lens', '力量镜',
        'Held: Holder gains 4 Special Attack effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的特攻
会比平时成长得更高。', null),
       (269, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains Special Defense effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-band', '力量束带',
        'Held: Holder gains 4 Special Defense effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的特防
会比平时成长得更高。', null),
       (270, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains Speed effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-anklet', '力量护踝', 'Held: Holder gains 4 Speed effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的速度
会比平时成长得更高。', null),
       (271, 3000,
        'Held: Decreases the holder’s Speed by 50%.  Whenever the holder gains HP effort from battle, increases that effort by 4; this applies before the PokéRUS doubling effect.',
        70, 'power-weight', '力量负重', 'Held: Holder gains 4 HP effort values, but has halved Speed in battle.', '虽然携带后速度会降低，
但宝可梦的ＨＰ
会比平时成长得更高。', null),
       (272, 4000,
        'Held: The holder is unaffected by any moves or abilities that would prevent it from actively leaving battle.',
        10, 'shed-shell', '美丽空壳',
        'Held: Holder can bypass all trapping effects and switch out. Multi-turn moves still cannot be switched out of.', '结实坚硬的空壳。
携带它的宝可梦必定可以
和后备的宝可梦进行替换。', null),
       (273, 4000,
        'Held: HP restored from Absorb, Aqua Ring, Drain Punch, Dream Eater, Giga Drain, Ingrain, Leech Life, Leech Seed, and Mega Drain is increased by 30%.  Damage inflicted is not affected.',
        10, 'big-root', '大根茎',
        'Held: Increases HP recovered from draining moves, Ingrain, and Aqua Ring by 3/10 (30%).', '携带后，吸取ＨＰ的招式
可以比平时更多地回复自己的ＨＰ。', null),
       (274, 4000, 'Held: Increases the holder’s Special Attack by 50%, but restricts it to the first move it uses until it leaves battle or loses this item.  If this item is swapped for another Choice item via Trick or Switcheroo, the holder’s restriction is still lifted, but it will again be restricted to the next move it uses.
(Quirk: If the holder is switched in by U-Turn and it also knows U-Turn, U-Turn becomes its restricted move.)', 10,
        'choice-specs', '讲究眼镜', 'Held: Increases Special Attack by 50%, but restricts the holder to only one move.', '有点讲究的眼镜。
虽然携带后特攻会提高，
但只能使出相同的招式。', null),
       (275, 1000, 'Held: Increases the power of the holder’s Fire moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Fire.', 90, 'flame-plate', '火球石板',
        'Held: Fire-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Fire.', '火属性的石板。
携带后，火属性的
招式威力就会增强。', null),
       (276, 1000, 'Held: Increases the power of the holder’s Water moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Water.', 90, 'splash-plate', '水滴石板',
        'Held: Water-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Water.', '水属性的石板。
携带后，水属性的
招式威力就会增强。', null),
       (277, 1000, 'Held: Increases the power of the holder’s Electric moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Electric.', 90, 'zap-plate', '雷电石板',
        'Held: Electric-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Electric.', '电属性的石板。
携带后，电属性的
招式威力就会增强。', null),
       (278, 1000, 'Held: Increases the power of the holder’s Grass moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Grass.', 90, 'meadow-plate', '碧绿石板',
        'Held: Grass-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Grass.', '草属性的石板。
携带后，草属性的
招式威力就会增强。', null),
       (279, 1000, 'Held: Increases the power of the holder’s Ice moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Ice.', 90, 'icicle-plate', '冰柱石板',
        'Held: Ice-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Ice.', '冰属性的石板。
携带后，冰属性的
招式威力就会增强。', null),
       (280, 1000, 'Held: Increases the power of the holder’s Fighting moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Fighting.', 90, 'fist-plate', '拳头石板',
        'Held: Fighting-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Fighting.', '格斗属性的石板。
携带后，格斗属性的
招式威力就会增强。', null),
       (1032, 2000, null, 30, 'tropical-shell', '南国贝壳', null, '美丽的白色贝壳。
或许是从温暖的海域那里
漂流到这里的。', null),
       (281, 1000, 'Held: Increases the power of the holder’s Poison moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Poison.', 90, 'toxic-plate', '剧毒石板',
        'Held: Poison-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Poison.', '毒属性的石板。
携带后，毒属性的
招式威力就会增强。', null),
       (282, 1000, 'Held: Increases the power of the holder’s Ground moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Ground.', 90, 'earth-plate', '大地石板',
        'Held: Ground-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Ground.', '地面属性的石板。
携带后，地面属性的
招式威力就会增强。', null),
       (283, 1000, 'Held: Increases the power of the holder’s Flying moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Flying.', 90, 'sky-plate', '蓝天石板',
        'Held: Flying-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Flying.', '飞行属性的石板。
携带后，飞行属性的
招式威力就会增强。', null),
       (284, 1000, 'Held: Increases the power of the holder’s Psychic moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Psychic.', 90, 'mind-plate', '神奇石板',
        'Held: Psychic-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Psychic.', '超能力属性的石板。
携带后，超能力属性的
招式威力就会增强。', null),
       (285, 1000, 'Held: Increases the power of the holder’s Bug moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Bug.', 90, 'insect-plate', '玉虫石板',
        'Held: Bug-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Bug.', '虫属性的石板。
携带后，虫属性的
招式威力就会增强。', null),
       (286, 1000, 'Held: Increases the power of the holder’s Rock moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Rock.', 90, 'stone-plate', '岩石石板',
        'Held: Rock-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Rock.', '岩石属性的石板。
携带后，岩石属性的
招式威力就会增强。', null),
       (287, 1000, 'Held: Increases the power of the holder’s Ghost moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Ghost.', 90, 'spooky-plate', '妖怪石板',
        'Held: Ghost-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Ghost.', '幽灵属性的石板。
携带后，幽灵属性的
招式威力就会增强。', null),
       (288, 1000, 'Held: Increases the power of the holder’s Dragon moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Dragon.', 90, 'draco-plate', '龙之石板',
        'Held: Dragon-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Dragon.', '龙属性的石板。
携带后，龙属性的
招式威力就会增强。', null),
       (289, 1000, 'Held: Increases the power of the holder’s Dark moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Dark.', 90, 'dread-plate', '恶颜石板',
        'Held: Dark-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Dark.', '恶属性的石板。
携带后，恶属性的
招式威力就会增强。', null),
       (290, 1000, 'Held: Increases the power of the holder’s Steel moves by 20%.
Held by a Multitype Pokémon: Holder’s type becomes Steel.', 90, 'iron-plate', '钢铁石板',
        'Held: Steel-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Steel.', '钢属性的石板。
携带后，钢属性的
招式威力就会增强。', null),
       (291, 2000, 'Held: Increases the power of the holder’s Psychic moves by 20%.', 10, 'odd-incense', '奇异薰香',
        'Held: Psychic-Type moves from holder do 20% more damage. Breeding: Mr. Mime begets a Mime Jr. Egg.', '有着神奇香气的薰香。
携带后，超能力属性的
招式威力就会提高。', null),
       (292, 2000, 'Held: Increases the power of the holder’s Rock moves by 20%.', 10, 'rock-incense', '岩石薰香',
        'Held: Rock-Type moves from holder do 20% more damage. Breeding: Sudowoodo begets a Bonsly Egg.', '有着神奇香气的薰香。
携带后，岩石属性的
招式威力就会提高。', null),
       (293, 5000,
        'Held: The holder will go last within its move’s priority bracket, regardless of Speed.  If multiple Pokémon within the same priority bracket are subject to this effect, the slower Pokémon will go first.  The holder will move after Pokémon with Stall.  If the holder has Stall, Stall is ignored.  This item ignores Trick Room.',
        10, 'full-incense', '饱腹薰香',
        'Held: Holder moves last in its priority bracket. Breeding: Snorlax begets a Munchlax Egg.', '有着神奇香气的薰香。
携带后，宝可梦的行动
会比平时更加迟缓。', null),
       (294, 2000, 'Held: Increases the power of the holder’s Water moves by 20%.', 10, 'wave-incense', '涟漪薰香',
        'Held: Water-Type moves from holder do 20% more damage. Breeding: Mantine begets a Mantyke Egg.', '有着神奇香气的薰香。
携带后，水属性的
招式威力就会提高。', null),
       (295, 2000, 'Held: Increases the power of the holder’s Grass moves by 20%.', 10, 'rose-incense', '花朵薰香',
        'Held: Grass-Type moves from holder do 20% more damage. Breeding: Roselia or Roserade beget a Budew Egg.', '有着神奇香气的薰香。
携带后，草属性的
招式威力就会提高。', null),
       (296, 11000,
        'Held: Doubles the money the trainer receives after an in-game trainer battle.  This effect cannot apply more than once to the same battle.',
        10, 'luck-incense', '幸运薰香',
        'Held: Doubles the money earned from a battle. Does not stack with Amulet Coin. Breeding: Chansey and Blissey beget a Happiny Egg.', '只要携带它的宝可梦
在战斗时出场一次，
就能获得２倍金钱。', null),
       (297, 6000, 'Held by lead Pokémon: Prevents wild battles with Pokémon that are lower level than the holder.', 10,
        'pure-incense', '洁净薰香',
        'Prevents wild encounters of level lower than your party’s lead Pokémon. Breeding: Chimecho begets a Chingling Egg.', '让排在最前面的宝可梦携带后，
野生宝可梦就会不容易出现。', null),
       (298, 2000, 'Held by Rhydon: Evolves the holder into Rhyperior when traded.', 80, 'protector', '护具',
        'Traded on a Rhydon: Holder evolves into Rhyperior.', '某种护具。
非常坚硬而且沉重。
某种宝可梦很喜欢它。', null),
       (299, 2000, 'Held by Electabuzz: Evolves the holder into Electivire when traded.', 80, 'electirizer',
        '电力增幅器', 'Traded on an Electabuzz: Holder evolves into Electivire.', '积蓄着庞大电气能量的箱子。
某种宝可梦很喜欢它。', null),
       (300, 2000, 'Held by Magmar: Evolves the holder into Magmortar when traded.', 80, 'magmarizer', '熔岩增幅器',
        'Traded on a Magmar: Holder evolves into Magmortar.', '积蓄着庞大熔岩能量的箱子。
某种宝可梦很喜欢它。', null),
       (301, 2000, 'Held by Porygon2: Evolves the holder into Porygon-Z when traded.', 50, 'dubious-disc', '可疑补丁',
        'Traded on a Porygon2: Holder evolves into Porygon-Z.', '内部储存了奇怪信息的透明机器。
制造者不明。', null),
       (302, 2000, 'Held by Dusclops: Evolves the holder into Dusknoir when traded.', 10, 'reaper-cloth', '灵界之布',
        'Traded on a Dusclops: Holder evolves into Dusknoir.', '蕴含着惊人强大灵力的布。
某种宝可梦很喜欢它。', null),
       (1084, 20, null, null, 'abra-candy', '凯西的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (303, 5000, 'Held: Raises the holder’s critical hit counter by 1.
Held by Sneasel: Evolves the holder into Weavile when it levels up during the night.', 80, 'razor-claw', '锐利之爪',
        'Held: Raises the holder’s critical hit ratio by one stage. Held by a Sneasel while levelling up at night: Holder evolves into Weavile.', '尖锐的爪子。
携带后，招式会
变得容易击中要害。', null),
       (304, 5000, 'Held: When the holder attacks with most damaging moves, provides an extra 11.7% (30/256) chance for the target to flinch.
Held by Gligar: Evolves the holder into Gliscor when it levels up.', 30, 'razor-fang', '锐利之牙',
        'Held: Damaging moves gain a 10% chance to make their target flinch. Held by a Gligar while levelling up: Holder evolves into Gliscor.', '尖锐的牙齿。
携带后，在给予伤害时，
有时会让对手畏缩。', 7),
       (305, 40000, 'Teaches Focus Punch to a compatible Pokémon.', null, 'tm01', '招式学习器０１',
        'Teaches Hone Claws to a compatible Pokémon. (Gen IV & III: Focus Punch Gen II: DynamicPunch Gen I: Mega Punch)', '将头伸出，
笔直地扑向对手进行攻击。
有时会使对手畏缩。', null),
       (306, 1000, 'Teaches Dragon Claw to a compatible Pokémon.', null, 'tm02', '招式学习器０２',
        'Teaches Dragon Claw to a compatible Pokémon. (Gen II: Headbutt Gen I: Razor Wind)', '用尖锐的巨爪
劈开对手进行攻击。', null),
       (307, 50000, 'Teaches Water Pulse to a compatible Pokémon.', null, 'tm03', '招式学习器０３',
        'Teaches Psyshock to a compatible Pokémon. (Gen IV & III: Water Pulse Gen II: Curse Gen I: Swords Dance)', '用充满火焰的拳头攻击对手。
有时会让对手陷入灼伤状态。', null),
       (308, 50000, 'Teaches Calm Mind to a compatible Pokémon.', null, 'tm04', '招式学习器０４',
        'Teaches Calm Mind to a compatible Pokémon. (Gen II: Rollout Gen I: Whirlwind)', '静心凝神，
从而提高自己的特攻和特防。', null),
       (309, 50000, 'Teaches Roar to a compatible Pokémon.', null, 'tm05', '招式学习器０５',
        'Teaches Roar to a compatible Pokémon. (Gen I: Mega Kick)', '放走对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', null),
       (310, 1000, 'Teaches Toxic to a compatible Pokémon.', null, 'tm06', '招式学习器０６',
        'Teaches Toxic to a compatible Pokémon.', '让对手陷入剧毒状态。
随着回合的推进，
中毒伤害会增加。', null),
       (311, 1000, 'Teaches Hail to a compatible Pokémon.', null, 'tm07', '招式学习器０７',
        'Teaches Hail to a compatible Pokémon. (Gen II: Zap Cannon Gen I: Horn Drill)', '在５回合内一直降冰雹，
除冰属性的宝可梦以外，
给予其他宝可梦伤害。', null),
       (312, 50000, 'Teaches Bulk Up to a compatible Pokémon.', null, 'tm08', '招式学习器０８',
        'Teaches Bulk Up to a compatible Pokémon. (Gen II: Rock Smash Gen I: Body Slam)', '使出全身力气绷紧肌肉，
从而提高自己的攻击和防御。', null),
       (313, 50000, 'Teaches Bullet Seed to a compatible Pokémon.', null, 'tm09', '招式学习器０９',
        'Teaches Venoshock to a compatible Pokémon. (Gen IV & III: Bullet Seed Gen II: Psych Up Gen I: Take Down)', '将特殊的毒液泼向对手。
对处于中毒状态的对手，
威力会变成２倍。', null),
       (314, 1000, 'Teaches Hidden Power to a compatible Pokémon.', null, 'tm10', '招式学习器１０',
        'Teaches Hidden Power to a compatible Pokémon. (Gen I: Double-Edge)', '根据所使用招式的宝可梦，
招式属性也会改变。', null),
       (315, 1000, 'Teaches Sunny Day to a compatible Pokémon.', null, 'tm11', '招式学习器１１',
        'Teaches Sunny Day to a compatible Pokémon. (Gen I: BubbleBeam)', '在５回合内阳光变得强烈，
从而提高火属性的招式威力。
水属性的招式威力则降低。', null),
       (316, 50000, 'Teaches Taunt to a compatible Pokémon.', null, 'tm12', '招式学习器１２',
        'Teaches Taunt to a compatible Pokémon. (Gen II: Sweet Scent Gen I: Water Gun)', '使对手愤怒。
在３回合内让对手
只能使出给予伤害的招式。', null),
       (317, 10000, 'Teaches Ice Beam to a compatible Pokémon.', null, 'tm13', '招式学习器１３',
        'Teaches Ice Beam to a compatible Pokémon. (Gen II: Snore)', '向对手发射
冰冻光束进行攻击。
有时会让对手陷入冰冻状态。', null),
       (318, 1000, 'Teaches Blizzard to a compatible Pokémon.', null, 'tm14', '招式学习器１４',
        'Teaches Blizzard to a compatible Pokémon.', '将猛烈的暴风雪
刮向对手进行攻击。
有时会让对手陷入冰冻状态。', null),
       (319, 1000, 'Teaches Hyper Beam to a compatible Pokémon.', null, 'tm15', '招式学习器１５',
        'Teaches Hyper Beam to a compatible Pokémon.', '向对手发射
强烈的光线进行攻击。
下一回合自己将无法动弹。', null),
       (320, 1000, 'Teaches Light Screen to a compatible Pokémon.', null, 'tm16', '招式学习器１６',
        'Teaches Light Screen to a compatible Pokémon. (Gen II: Icy Wind Gen I: Pay Day)', '在５回合内使用神奇的墙，
减弱从对手那受到的
特殊攻击的伤害。', null),
       (321, 10000, 'Teaches Protect to a compatible Pokémon.', null, 'tm17', '招式学习器１７',
        'Teaches Protect to a compatible Pokémon. (Gen I: Submission)', '完全抵挡
对手的攻击。
连续使出则容易失败。', null),
       (322, 10000, 'Teaches Rain Dance to a compatible Pokémon.', null, 'tm18', '招式学习器１８',
        'Teaches Rain Dance to a compatible Pokémon. (Gen I: Counter)', '在５回合内一直降雨，
从而提高水属性的招式威力。
火属性的招式威力则降低。', null),
       (323, 10000, 'Teaches Giga Drain to a compatible Pokémon.', null, 'tm19', '招式学习器１９',
        'Teaches Telekinesis to a compatible Pokémon. (Gen IV & III & II: Giga Drain Gen I: Seismic Toss)', '降到地面，使身体休息。
回复自己最大ＨＰ的一半。', null),
       (324, 100000, 'Teaches Safeguard to a compatible Pokémon.', null, 'tm20', '招式学习器２０',
        'Teaches Safeguard to a compatible Pokémon. (Gen II: Endure Gen I: Rage)', '在５回合内
被神奇的力量守护，
从而不会陷入异常状态。', null),
       (325, 1000, 'Teaches Frustration to a compatible Pokémon.', null, 'tm21', '招式学习器２１',
        'Teaches Frustration to a compatible Pokémon. (Gen I: Mega Drain)', '为了发泄不满而
全力进行攻击。
亲密度越低，威力越大。', null),
       (326, 1000, 'Teaches SolarBeam to a compatible Pokémon.', null, 'tm22', '招式学习器２２',
        'Teaches SolarBeam to a compatible Pokémon.', '第１回合收集满满的日光，
第２回合发射光束进行攻击。', null),
       (327, 10000, 'Teaches Iron Tail to a compatible Pokémon.', null, 'tm23', '招式学习器２３',
        'Teaches Smack Down to a compatible Pokémon. (Gen IV & III & II: Iron Tail Gen I: Dragon Rage)', '扔石头或炮弹，
攻击飞行的对手。
对手会被击落，掉到地面。', null),
       (328, 1000, 'Teaches Thunderbolt to a compatible Pokémon.', null, 'tm24', '招式学习器２４',
        'Teaches Thunderbolt to a compatible Pokémon. (Gen II: DragonBreath)', '向对手发出
强力电击进行攻击。
有时会让对手陷入麻痹状态。', null),
       (329, 10000, 'Teaches Thunder to a compatible Pokémon.', null, 'tm25', '招式学习器２５',
        'Teaches Thunder to a compatible Pokémon.', '向对手劈下暴雷进行攻击。
有时会让对手陷入麻痹状态。', null),
       (330, 1000, 'Teaches Earthquake to a compatible Pokémon.', null, 'tm26', '招式学习器２６',
        'Teaches Earthquake to a compatible Pokémon.', '利用地震的冲击，
攻击自己周围所有的宝可梦。', null),
       (1682, 500, null, null, 'grass-tera-shard', '草太晶碎块', null, null, null),
       (334, 1000, 'Teaches Shadow Ball to a compatible Pokémon.', null, 'tm30', '招式学习器３０',
        'Teaches Shadow Ball to a compatible Pokémon. (Gen I: Teleport)', '投掷一团黑影进行攻击。
有时会降低对手的特防。', null),
       (335, 1000, 'Teaches Brick Break to a compatible Pokémon.', null, 'tm31', '招式学习器３１',
        'Teaches Brick Break to a compatible Pokémon. (Gen II: Mud-Slap Gen I: Mimic)', '将手刀猛烈地挥下攻击对手。
还可以破坏光墙和反射壁等。', null),
       (336, 10000, 'Teaches Double Team to a compatible Pokémon.', null, 'tm32', '招式学习器３２',
        'Teaches Double Team to a compatible Pokémon.', '通过快速移动来制造分身，
扰乱对手，从而提高闪避率。', null),
       (337, 10000, 'Teaches Reflect to a compatible Pokémon.', null, 'tm33', '招式学习器３３',
        'Teaches Reflect to a compatible Pokémon. (Gen II: Ice Punch)', '在５回合内使用神奇的墙，
减弱从对手那受到的
物理攻击的伤害。', null),
       (338, 10000, 'Teaches Shock Wave to a compatible Pokémon.', null, 'tm34', '招式学习器３４',
        'Teaches Sludge Wave to a compatible Pokémon. (Gen IV & III: Shock Wave Gen II: Swagger Gen I: Bide)', '用污泥波攻击
自己周围所有的宝可梦。
有时会陷入中毒状态。', null),
       (339, 10000, 'Teaches Flamethrower to a compatible Pokémon.', null, 'tm35', '招式学习器３５',
        'Teaches Flamethrower to a compatible Pokémon. (Gen II: Sleep Talk Gen I: Metronome)', '向对手发射
烈焰进行攻击。
有时会让对手陷入灼伤状态。', null),
       (340, 1000, 'Teaches Sludge Bomb to a compatible Pokémon.', null, 'tm36', '招式学习器３６',
        'Teaches Sludge Bomb to a compatible Pokémon. (Gen I: Selfdestruct)', '用污泥投掷对手进行攻击。
有时会让对手陷入中毒状态。', null),
       (341, 1000, 'Teaches Sandstorm to a compatible Pokémon.', null, 'tm37', '招式学习器３７',
        'Teaches Sandstorm to a compatible Pokémon. (Gen I: Egg Bomb)', '向对手发射
烈焰进行攻击。
有时会让对手陷入灼伤状态。', null),
       (342, 1000, 'Teaches Fire Blast to a compatible Pokémon.', null, 'tm38', '招式学习器３８',
        'Teaches Fire Blast to a compatible Pokémon.', '用大字的火焰烧尽对手。
有时会让对手陷入灼伤状态。', null),
       (343, 1000, 'Teaches Rock Tomb to a compatible Pokémon.', null, 'tm39', '招式学习器３９',
        'Teaches Rock Tomb to a compatible Pokémon. (Gen II & I: Swift)', '投掷岩石进行攻击。
封住对手的行动，
从而降低速度。', null),
       (344, 1000, 'Teaches Aerial Ace to a compatible Pokémon.', null, 'tm40', '招式学习器４０',
        'Teaches Aerial Ace to a compatible Pokémon. (Gen II: Defense Curl Gen I: Skull Bash)', '以敏捷的动作
戏弄对手后进行切斩。
攻击必定会命中。', null),
       (345, 10000, 'Teaches Torment to a compatible Pokémon.', null, 'tm41', '招式学习器４１',
        'Teaches Torment to a compatible Pokémon. (Gen II: ThunderPunch Gen I: Softboiled)', '向对手无理取闹，
令其不能连续２次
使出相同招式。', null),
       (346, 1000, 'Teaches Facade to a compatible Pokémon.', null, 'tm42', '招式学习器４２',
        'Teaches Facade to a compatible Pokémon. (Gen II & I: Dream Eater)', '当自己处于中毒、麻痹、灼伤状态时，
向对手使出此招式的话，
威力会变成２倍。', null),
       (347, 1000, 'Teaches Secret Power to a compatible Pokémon.', null, 'tm43', '招式学习器４３',
        'Teaches Flame Charge to a compatible Pokémon. (Gen IV & III: Secret Power Gen II: Detect Gen I: Sky Attack)', '将火焰围绕身体攻击对手。
积蓄力量并提高自己的速度。', null),
       (348, 100000, 'Teaches Rest to a compatible Pokémon.', null, 'tm44', '招式学习器４４',
        'Teaches Rest to a compatible Pokémon.', '连续睡上２回合。
回复自己的全部ＨＰ
以及治愈所有异常状态。', null),
       (349, 1000, 'Teaches Attract to a compatible Pokémon.', null, 'tm45', '招式学习器４５',
        'Teaches Attract to a compatible Pokémon. (Gen I: Thunder Wave)', '♂诱惑♀或♀诱惑♂，
让对手着迷。
对手将很难使出招式。', null),
       (350, 30000, 'Teaches Thief to a compatible Pokémon.', null, 'tm46', '招式学习器４６',
        'Teaches Thief to a compatible Pokémon. (Gen I: Psywave)', '攻击的同时盗取道具。
当自己携带道具时，
不会去盗取。', null),
       (351, 1000, 'Teaches Steel Wing to a compatible Pokémon.', null, 'tm47', '招式学习器４７',
        'Teaches Low Sweep to a compatible Pokémon. (Gen IV & III & II: Steel Wing Gen I: Explosion)', '以敏捷的动作瞄准
对手的脚进行攻击。
降低对手的速度。', null),
       (352, 1000, 'Teaches Skill Swap to a compatible Pokémon.', null, 'tm48', '招式学习器４８',
        'Teaches Round to a compatible Pokémon. (Gen IV & III: Skill Swap Gen II: Fire Punch Gen I: Rock Slide)', '用歌声攻击对手。
同伴还可以接着使出轮唱招式，
威力也会提高。', null),
       (353, 1000, 'Teaches Snatch to a compatible Pokémon.', null, 'tm49', '招式学习器４９',
        'Teaches Echoed Voice to a compatible Pokémon. (Gen IV & III: Snatch Gen II: Fury Cutter Gen I: Tri Attack)', '用回声攻击对手。
如果每回合都有宝可梦接着
使用该招式，威力就会提高。', null),
       (354, 10000, 'Teaches Overheat to a compatible Pokémon.', null, 'tm50', '招式学习器５０',
        'Teaches Overheat to a compatible Pokémon. (Gen II: Nightmare Gen I: Substitute)', '使出全部力量攻击对手。
使用之后会因为反作用力，
自己的特攻大幅降低。', null),
       (355, 1000, 'Teaches Roost to a compatible Pokémon.', null, 'tm51', '招式学习器５１',
        'Teaches Ally Switch to a compatible Pokémon. (Gen IV: Roost)', '用坚硬的翅膀敲打
对手进行攻击。
有时会提高自己的防御。', null),
       (356, 100000, 'Teaches Focus Blast to a compatible Pokémon.', null, 'tm52', '招式学习器５２',
        'Teaches Focus Blast to a compatible Pokémon.', '提高气势，
释放出全部力量。
有时会降低对手的特防。', null),
       (357, 1000, 'Teaches Energy Ball to a compatible Pokémon.', null, 'tm53', '招式学习器５３',
        'Teaches Energy Ball to a compatible Pokémon.', '发射从自然收集的生命力量。
有时会降低对手的特防。', null),
       (358, 1000, 'Teaches False Swipe to a compatible Pokémon.', null, 'tm54', '招式学习器５４',
        'Teaches False Swipe to a compatible Pokémon.', '对手的ＨＰ
至少会留下１ＨＰ，
如此般手下留情地攻击。', null),
       (359, 10000, 'Teaches Brine to a compatible Pokémon.', null, 'tm55', '招式学习器５５',
        'Teaches Scald to a compatible Pokémon. (Gen IV: Brine)', '向对手喷射
煮得翻滚的开水进行攻击。
有时会让对手陷入灼伤状态。', null),
       (360, 1000, 'Teaches Fling to a compatible Pokémon.', null, 'tm56', '招式学习器５６',
        'Teaches Fling to a compatible Pokémon.', '快速投掷携带的道具进行攻击。
根据道具不同，
威力和效果会改变。', null),
       (361, 1000, 'Teaches Charge Beam to a compatible Pokémon.', null, 'tm57', '招式学习器５７',
        'Teaches Charge Beam to a compatible Pokémon.', '向对手发射电击光束。
由于蓄满电流，
有时会提高自己的特攻。', null),
       (362, 1000, 'Teaches Endure to a compatible Pokémon.', null, 'tm58', '招式学习器５８',
        'Teaches Sky Drop to a compatible Pokémon. (Gen IV: Endure)', '将带走的对手在第２回合
从空中摔下进行攻击。
被带到空中的对手不能动弹。', null),
       (557, 0, 'Provides access to Birth Island and deoxys.', null, 'auroraticket', 'AuroraTicket',
        'Allows access to Birth Island and Deoxys.', 'A ticket required
to board the ship
to BIRTH ISLAND.', null),
       (363, 100000, 'Teaches Dragon Pulse to a compatible Pokémon.', null, 'tm59', '招式学习器５９',
        'Teaches Incinerate to a compatible Pokémon. (Gen IV: Dragon Pulse)', '用自己的身体狂舞挥打，
给予对手伤害。', null),
       (364, 30000, 'Teaches Drain Punch to a compatible Pokémon.', null, 'tm60', '招式学习器６０',
        'Teaches Quash to a compatible Pokémon. (Gen IV: Drain Punch)', '压制对手，
从而将其行动顺序放到最后。', null),
       (365, 30000, 'Teaches Will-O-Wisp to a compatible Pokémon.', null, 'tm61', '招式学习器６１',
        'Teaches Will-O-Wisp to a compatible Pokémon.', '放出怪异的火焰，
从而让对手陷入灼伤状态。', null),
       (366, 30000, 'Teaches Silver Wind to a compatible Pokémon.', null, 'tm62', '招式学习器６２',
        'Teaches Acrobatics to a compatible Pokémon. (Gen IV: Silver Wind)', '轻巧地攻击对手。
自己没有携带道具时，
会给予较大的伤害。', null),
       (367, 50000, 'Teaches Embargo to a compatible Pokémon.', null, 'tm63', '招式学习器６３',
        'Teaches Embargo to a compatible Pokémon.', '让对手在５回合内不能使用
宝可梦携带的道具。训练家也
不能给那只宝可梦使用道具。', null),
       (368, 1000, 'Teaches Explosion to a compatible Pokémon.', null, 'tm64', '招式学习器６４',
        'Teaches Explosion to a compatible Pokémon.', '引发大爆炸，
攻击自己周围所有的宝可梦。
使用后自己会陷入濒死。', null),
       (369, 1000, 'Teaches Shadow Claw to a compatible Pokémon.', null, 'tm65', '招式学习器６５',
        'Teaches Shadow Claw to a compatible Pokémon.', '以影子做成的锐爪，
劈开对手。
容易击中要害。', null),
       (370, 30000, 'Teaches Payback to a compatible Pokémon.', null, 'tm66', '招式学习器６６',
        'Teaches Payback to a compatible Pokémon.', '蓄力攻击。
如果能在对手之后攻击，
招式的威力会变成２倍。', null),
       (371, 30000, 'Teaches Recycle to a compatible Pokémon.', null, 'tm67', '招式学习器６７',
        'Teaches Retaliate to a compatible Pokémon. (Gen IV: Recycle)', '用尖尖的角刺入对手进行攻击。
攻击必定会命中。', null),
       (372, 30000, 'Teaches Giga Impact to a compatible Pokémon.', null, 'tm68', '招式学习器６８',
        'Teaches Giga Impact to a compatible Pokémon.', '使出自己浑身力量突击对手。
下一回合自己将无法动弹。', null),
       (373, 1000, 'Teaches Rock Polish to a compatible Pokémon.', null, 'tm69', '招式学习器６９',
        'Teaches Rock Polish to a compatible Pokémon.', '打磨自己的身体，
减少空气阻力。
可以大幅提高自己的速度。', null),
       (374, 50000, 'Teaches Flash to a compatible Pokémon.', null, 'tm70', '招式学习器７０',
        'Teaches Flash to a compatible Pokémon.', '在５回合内减弱
物理和特殊的伤害。
只有冰雹时才能使出。', null),
       (375, 50000, 'Teaches Stone Edge to a compatible Pokémon.', null, 'tm71', '招式学习器７１',
        'Teaches Stone Edge to a compatible Pokémon.', '用尖尖的岩石
刺入对手进行攻击。
容易击中要害。', null),
       (376, 50000, 'Teaches Avalanche to a compatible Pokémon.', null, 'tm72', '招式学习器７２',
        'Teaches Volt Switch to a compatible Pokémon. (Gen IV: Avalanche)', '在攻击之后急速返回，
和后备宝可梦进行替换。', null),
       (377, 1000, 'Teaches Thunder Wave to a compatible Pokémon.', null, 'tm73', '招式学习器７３',
        'Teaches Thunder Wave to a compatible Pokémon.', '向对手发出
微弱的电击，
从而让对手陷入麻痹状态。', null),
       (378, 1000, 'Teaches Gyro Ball to a compatible Pokémon.', null, 'tm74', '招式学习器７４',
        'Teaches Gyro Ball to a compatible Pokémon.', '让身体高速旋转并撞击对手。
速度比对手越慢，威力越大。', null),
       (379, 1000, 'Teaches Swords Dance to a compatible Pokémon.', null, 'tm75', '招式学习器７５',
        'Teaches Swords Dance to a compatible Pokémon.', '激烈地跳起战舞提高气势。
大幅提高自己的攻击。', null),
       (380, 10000, 'Teaches Stealth Rock to a compatible Pokémon.', null, 'tm76', '招式学习器７６',
        'Teaches Struggle Bug to a compatible Pokémon. (Gen IV: Stealth Rock)', '第１回合飞上天空，
第２回合攻击对手。', null),
       (381, 1000, 'Teaches Psych Up to a compatible Pokémon.', null, 'tm77', '招式学习器７７',
        'Teaches Psych Up to a compatible Pokémon.', '向自己施以自我暗示，
将能力变化的状态
变得和对手一样。', null),
       (382, 1000, 'Teaches Captivate to a compatible Pokémon.', null, 'tm78', '招式学习器７８',
        'Teaches Bulldoze to a compatible Pokémon. (Gen IV: Captivate)', '用力踩踏地面并攻击
自己周围所有的宝可梦。
降低对方的速度。', null),
       (383, 1000, 'Teaches Dark Pulse to a compatible Pokémon.', null, 'tm79', '招式学习器７９',
        'Teaches Frost Breath to a compatible Pokémon. (Gen IV: Dark Pulse)', '将冰冷的气息
吹向对手进行攻击。
必定会击中要害。', null),
       (384, 1000, 'Teaches Rock Slide to a compatible Pokémon.', null, 'tm80', '招式学习器８０',
        'Teaches Rock Slide to a compatible Pokémon.', '将大岩石
猛烈地撞向对手进行攻击。
有时会使对手畏缩。', null),
       (385, 1000, 'Teaches X-Scissor to a compatible Pokémon.', null, 'tm81', '招式学习器８１',
        'Teaches X-Scissor to a compatible Pokémon.', '将镰刀或爪子像剪刀般地交叉，
顺势劈开对手。', null),
       (386, 1000, 'Teaches Sleep Talk to a compatible Pokémon.', null, 'tm82', '招式学习器８２',
        'Teaches Dragon Tail to a compatible Pokémon. (Gen IV: Sleep Talk)', '弹飞对手，强制拉后备宝可梦上场。
如果对手为野生宝可梦，
战斗将直接结束。', null),
       (387, 100000, 'Teaches Natural Gift to a compatible Pokémon.', null, 'tm83', '招式学习器８３',
        'Teaches Work Up to a compatible Pokémon. (Gen IV: Natural Gift)', '在４～５回合内
死缠烂打地进行攻击。
在此期间对手将无法逃走。', null),
       (388, 1000, 'Teaches Poison Jab to a compatible Pokémon.', null, 'tm84', '招式学习器８４',
        'Teaches Poison Jab to a compatible Pokémon.', '用带毒的触手或手臂刺入对手。
有时会让对手陷入中毒状态。', null),
       (389, 1000, 'Teaches Dream Eater to a compatible Pokémon.', null, 'tm85', '招式学习器８５',
        'Teaches Dream Eater to a compatible Pokémon.', '吃掉正在睡觉的对手的梦
进行攻击。回复对手
所受到伤害的一半ＨＰ。', null),
       (390, 1000, 'Teaches Grass Knot to a compatible Pokémon.', null, 'tm86', '招式学习器８６',
        'Teaches Grass Knot to a compatible Pokémon.', '用草缠住并绊倒对手。
对手越重，威力越大。', null),
       (391, 1000, 'Teaches Swagger to a compatible Pokémon.', null, 'tm87', '招式学习器８７',
        'Teaches Swagger to a compatible Pokémon.', '激怒对手，使其混乱。
因为愤怒，对手的攻击
会大幅提高。', null),
       (392, 20000, 'Teaches Pluck to a compatible Pokémon.', null, 'tm88', '招式学习器８８',
        'Teaches Pluck to a compatible Pokémon.', '从自己已学会的招式中
任意使出１个。
只能在自己睡觉时使用。', null),
       (393, 20000, 'Teaches U-Turn to a compatible Pokémon.', null, 'tm89', '招式学习器８９',
        'Teaches U-turn to a compatible Pokémon.', '在攻击之后急速返回，
和后备宝可梦进行替换。', null),
       (394, 20000, 'Teaches Substitute to a compatible Pokémon.', null, 'tm90', '招式学习器９０',
        'Teaches Substitute to a compatible Pokémon.', '削减少许自己的ＨＰ，
制造分身。
分身将成为自己的替身。', null),
       (1683, 500, null, null, 'ice-tera-shard', '冰太晶碎块', null, null, null),
       (395, 20000, 'Teaches Flash Cannon to a compatible Pokémon.', null, 'tm91', '招式学习器９１',
        'Teaches Flash Cannon to a compatible Pokémon.', '将身体的光芒
聚集在一点释放出去。
有时会降低对手的特防。', null),
       (396, 100000, 'Teaches Trick Room to a compatible Pokémon.', null, 'tm92', '招式学习器９２',
        'Teaches Trick Room to a compatible Pokémon.', '制造出离奇的空间。
在５回合内
速度慢的宝可梦可以先行动。', null),
       (397, 0, 'Teaches Cut to a compatible Pokémon.', null, 'hm01', '秘传学习器０１',
        'Teaches Cut to a compatible Pokémon.', '用镰刀或爪子等
切斩对手进行攻击。
还可以切断细的树木。', null),
       (398, 0, 'Teaches Fly to a compatible Pokémon.', null, 'hm02', '秘传学习器０２',
        'Teaches Fly to a compatible Pokémon.', '第１回合飞上天空，
第２回合攻击对手。
还可以飞到去过的城镇。', null),
       (399, 0, 'Teaches Surf to a compatible Pokémon.', null, 'hm03', '秘传学习器０３',
        'Teaches Surf to a compatible Pokémon.', '利用大浪
攻击自己周围所有的宝可梦。', null),
       (400, 0, 'Teaches Strength to a compatible Pokémon.', null, 'hm04', '秘传学习器０４',
        'Teaches Strength to a compatible Pokémon.', '使出浑身力气
殴打对手进行攻击。
还可以推动沉重的岩石。', null),
       (401, 0, 'Teaches Defog to a compatible Pokémon.', null, 'hm05', '秘传学习器０５',
        'Teaches Waterfall to a compatible Pokémon. (HS: Whirlpool DPP: Defog Gen III & II & I: Flash)', '以惊人的气势扑向对手。
有时会使对手畏缩。
还可以游泳登上瀑布。', null),
       (402, 0, 'Teaches Rock Smash to a compatible Pokémon.', null, 'hm06', '秘传学习器０６',
        'Teaches Dive to a compatible Pokémon. (Gen IV & III: Rock Smash Gen II: Whirlpool)', '用拳头进行攻击。
有时会降低对手的防御。
还可以击碎岩石。', null),
       (403, 0, 'Teaches Waterfall to a compatible Pokémon.', null, 'hm07', '秘传学习器０７',
        'Teaches a move to a compatible Pokémon. (Gen IV & III & II: Waterfall)', '第１回合潜入，
第２回合浮上来进行攻击。', null),
       (404, 0, 'Teaches Rock Climb to a compatible Pokémon.', null, 'hm08', 'HM08',
        'Teaches a move to a compatible Pokémon. (Gen IV: Rock Climb Gen III: Dive)', 'Dives underwater
the 1st turn, then
attacks next turn.', null),
       (405, 0, 'Sends the trainer to the Underground.  Only usable outside.', null, 'explorer-kit', '探险套装',
        'Allows visiting the Underground.', '装着有助于探险且
方便使用的道具的袋子。
有了它就可以进入地下通道。', null),
       (406, 0, 'Unused.', null, 'loot-sack', '宝物袋', 'Carries coal mine loot.', '结实的大袋子。
可以将在煤矿里
获得的宝物放进去。', null),
       (407, 0, 'Unused.', null, 'rule-book', '规则书', 'List of battle types and their rules.', '写着对战规则。
在进行连接对战时，
可以选择规则。', null),
       (408, 0,
        'Designates several nearby patches of grass as containing Pokémon, some of which may be special radar-only Pokémon.  Successive uses in a certain way create chains of encounters with the same species; longer chains increase the chance that a shiny Pokémon of that species will appear.',
        null, 'poke-radar', '宝可追踪', 'Use to track down rare or shiny Pokémon. 50 steps to recharge.', '能够将藏在草丛里的
宝可梦找出来的道具。
走路就能给电池充电。', null),
       (409, 0, 'Tracks Battle Points.', null, 'point-card', '点数卡', 'Keeps count of Battle Points earned.', '可以查看赢得的
对战点数的卡片。', null),
       (410, 0, 'Records some of the trainer’s activities for the day.', null, 'journal', '冒险笔记',
        'Records prior significant activities the player took.', '记录着到现在为止的
冒险经历的笔记。', null),
       (411, 0, 'Contains Seals used for decorating Pokéballs.', null, 'seal-case', '贴纸盒',
        'Stores Seals that can be applied to Poké Ball capsules.', '放有贴在球壳上的
贴纸的容器。', null),
       (412, 0, 'Contains Pokémon Accessories.', null, 'fashion-case', '饰品盒',
        'Holds Pokémon Accessories for use in Contests.', '漂亮精美的盒子。
可以存放宝可梦出演音乐剧时
用来装扮自己的多种小物件。', null),
       (413, 0, 'Unused.', null, 'seal-bag', '贴纸袋', 'Holds ten Seals for Poké Balls.', '可以放入１０张
贴纸的小袋子。', null),
       (414, 0,
        'Contains friend codes for up to 32 other players, as well as their sprite, gender, and basic statistics for those that have been seen on WFC.',
        null, 'pal-pad', '朋友手册', 'Use to record Friend Codes and check your own.', '使用方便的手册。
可以添加朋友或
记录游戏的内容。', null),
       (415, 0, 'Opens the front door of the Valley Windworks.  Reusable.', null, 'works-key', '发电厂钥匙',
        'Grants access to Valley Windworks.', '用来打开或关闭
山谷发电厂大门的大钥匙。
不知为何，它落入了银河队的手中。', null),
       (416, 0, 'Given to Cynthia’s grandmother to get the Surf HM.', null, 'old-charm', '古代护符',
        'Trade to Cynthia’s grandmother in Celestic Town for HM04 (Surf).', '要交给神和镇上长老的远古护符。
由宝可梦的骨头制成。', null),
       (417, 0, 'Grants access to Galactic HQ in Veilstone City.', null, 'galactic-key', '银河队钥匙',
        'Grants access to Galactic HQ in Veilstone City.', '用于解除银河队本部
安全防范系统的钥匙卡。
弄丢了的话，好像会受到惩罚。', null),
       (418, 0, 'Unused.', null, 'red-chain', '红色锁链', 'Used to bind Palkia and Dialga.', '神话道具。
据说连接着孕育出
神奥地区的传说的宝可梦。', null),
       (419, 0,
        'Displays a map of the region including the trainer’s position, location names, visited towns, gym locations, and where the trainer has been walking recently.',
        null, 'town-map', '城镇地图', 'Use to see the overworld map.', '可以随时轻松
查看的便利地图。
也能清楚自己的位置。', null),
       (420, 0,
        'Reveals trainers who want a rematch, by showing !! over their heads.  Each use drains the battery; requires 100 steps to charge.',
        null, 'vs-seeker', '对战搜寻器', 'Allows rebattling of on-screen trainers. 100 steps to recharge.', '会告诉你想对战的
训练家在哪里的机器。
走路就能给电池充电。', null),
       (421, 0, 'Contains the Coins used by the Game Corner, to a maximum of 50,000.', null, 'coin-case', '代币盒',
        'Holds coins for the Game Corner.', '可以存放代币的盒子。
最多能放入５００００枚
在游戏城获得的代币。', null),
       (422, 0, 'Used to find Pokémon on the Old Rod list for an area, which are generally Magikarp or similar.', null,
        'old-rod', '破旧钓竿', 'Used to catch Pokémon in bodies of water.', '又破又旧的钓竿。
在有水的地方使用的话，
可以钓到宝可梦。', null),
       (423, 0, 'Used to find Pokémon on the Good Rod list for an area, which are generally mediocre.', null,
        'good-rod', '好钓竿', 'Used to catch Pokémon in bodies of water.', '不错的新钓竿。
在有水的地方使用的话，
可以钓到宝可梦。', null),
       (424, 0, 'Used to find Pokémon on the Super Rod list for an area, which are generally the best available there.',
        null, 'super-rod', '厉害钓竿', 'Used to catch Pokémon in bodies of water.', '最新的厉害钓竿。
在有水的地方使用的话，
可以钓到宝可梦。', null),
       (425, 0, 'Waters Berry plants.', null, 'sprayduck', '可达鸭喷壶', 'Used to water berries.', '浇水的道具。
能让埋在松软土壤里的
树果快快长大。', null),
       (427, 0,
        'Increases movement speed outside or in caves.  In high gear, allows the trainer to hop over some rocks and ascend muddy slopes.',
        null, 'bicycle', 'Bicycle', 'Use for fast transit.', 'A folding bicycle
that is faster than
the RUNNING SHOES.', null),
       (428, 0, 'Opens the locked building in the lakeside resort.', null, 'suite-key', '房间钥匙',
        'Opens a locked building in the Lakeside Resort.', '湖畔高级宾馆的房间钥匙。
不知为何，它经常会丢失。', null),
       (429, 0, 'Grants access to Flower Paradise and Shaymin.', null, 'oaks-letter', '大木的信',
        'Allows access to Seabreak path, Flower Paradise, and Shaymin.', '大木博士的来信。
上面写着请来２２４号道路。', null),
       (430, 0, 'Cures the sailor’s son of his nightmares; no reward, only a side effect of seeing Cresselia.', null,
        'lunar-wing', '新月之羽', 'Cures sailor’s son of nightmares in Canalave City.', '散发着月辉般光芒的羽毛。
据说隐藏着可以驱散恶梦的力量。', null),
       (431, 0, 'Provides access to Newmoon Island and Darkrai.', null, 'member-card', '会员卡',
        'Allows access to Newmoon Island and Darkrai.', '可以进入水脉市旅馆的卡片。
不知为何，上面刻有
５０年前左右的日期。', null),
       (432, 0, '', null, 'azure-flute', '天界之笛', 'Allows entry into the Hall of Origin. Unreleased.', '能发出响彻云霄的天籁之音的笛子。
不知道是谁在什么时候制造的它。', null),
       (433, 0, 'Allows passage on a ferry.

The same item is used for different ferries between different games.', null, 'ss-ticket', '船票',
        'Ticket for a ship. (RSE: S.S. Tidal LF: S.S. Anne HG: S.S. Aqua)', '乘坐高速船水流号时所需的船票。
上面绘有船的图案。', null),
       (434, 0, 'Allows the trainer to enter Contests.', null, 'contest-pass', '华丽大赛参加证',
        'Allows participation in Pokémon Contests.', '拿着它就可以参加宝可梦华丽大赛。
上面印有纪念奖章。', null),
       (435, 0, 'Causes Heatran to appear at Reversal Mountain.

Unused prior to Black and White 2.', null, 'magma-stone', '火山镇石', 'Magma is sealed inside.', '被灼热熔岩熔化的岩石
凝固后形成的产物。
里面还留有熔岩。', null),
       (436, 0,
        'Given to the trainer’s rival in Jubilife City.  Contains two Town Maps, one of which is given to the trainer upon delivery.',
        null, 'parcel', '包裹', 'Given to the trainer’s rival in Jubilife City. Contains Town Maps.', '这是别人托付给你的包裹。
需要将它交给从双叶镇启程
踏上旅途的青梅竹马。', null),
       (437, 0, 'One of three coupons needed to receive a Pokétch.', null, 'coupon-1', '兑换券１',
        'The first of three tickets used to obtain a Pokétch.', '获取宝可梦手表，简称宝可表时
所需的兑换券。需要３张。', null),
       (438, 0, 'One of three coupons needed to receive a Pokétch.', null, 'coupon-2', '兑换券２',
        'The second of three tickets used to obtain a Pokétch.', '获取宝可梦手表，简称宝可表时
所需的兑换券。需要３张。', null),
       (439, 0, 'One of three coupons needed to receive a Pokétch.', null, 'coupon-3', '兑换券３',
        'The last of three tickets used to obtain a Pokétch.', '获取宝可梦手表，简称宝可表时
所需的兑换券。需要３张。', null),
       (440, 0, 'Grants access to the Team Galactic warehouse in Veilstone City.', null, 'storage-key', '仓库钥匙',
        'Grants access to the Team Galactic warehouse in Veilstone City.', '用于进入银河队在帷幕市市郊的
可疑仓库的钥匙。', null),
       (441, 0, 'Required to cure the Psyducks blocking Route 210 of their chronic headaches.', null, 'secret-potion',
        '秘传之药', 'Used to heal the Ampharos at the top of Olivine Lighthouse.', '在湛蓝市的药店里得到的，
能让任何宝可梦立刻变得
精力充沛的高效药。', null),
       (442, 0, 'Held by giratina
:   Holder’s dragon and ghost moves have 1.2× their base power.

    Holder is in Origin Forme.

This item cannot be held by any Pokémon but Giratina.  When you enter the Union Room or connect to Wi-Fi, this item returns to your bag.',
        60, 'griseous-orb', '白金宝珠',
        'Boosts the damage from Giratina’s Dragon-type and Ghost-type moves by 20%, and transforms it into Origin Forme.', '让骑拉帝纳携带的话，
龙和幽灵属性的招式威力就会提高。
散发着光辉的宝珠。', null),
       (443, 0, 'Optionally records wireless, Wi-Fi, and Battle Frontier battles.

Tracks Battle Points earned in the Battle Frontier, and stores commemorative prints.', null, 'vs-recorder',
        '对战记录器', 'Records wireless, Wi-Fi, or Battle Frontier battles, and stores points.', '很酷的机器。
可以记录和朋友或
在特殊设施里的对战过程。', null),
       (444, 0, 'Used by trainer on a shaymin
:   Changes the target Shaymin from Land Forme to Sky Forme.

    This item cannot be used on a frozen Shaymin or at night.  Sky Forme Shaymin will revert to Land Forme overnight, when frozen, and upon entering a link battle.  This item must be used again to change it back.',
        null, 'gracidea', '葛拉西蒂亚花', 'Changes an unfrozen Shaymin to Sky Forme in the day.', '在生日或纪念日等日子里，
为了表达感激之情，
有时会将其扎成花束送出。', null),
       (445, 0, 'Used by trainer in the Galactic Eterna Building, on the ground floor, to the left of the TV
:   Unlocks the secret rotom room, in which there are five appliances which can change Rotom’s form.', null,
        'secret-key', '秘密钥匙', 'Gen IV: The key to Rotom’s appliance room. Gen III: The key to Cinnabar Gym.', '在特定的地方使用，
就会发出特殊电信号
来开门的高科技钥匙。', null),
       (446, 0, 'Stores Apricorns.', null, 'apricorn-box', '球果盒', 'Holds Apricorns.', '使用方便，可以保存
９９个球果的容器。', null),
       (447, 0, 'Contains four portable pots of soil suitable for growing berries.', null, 'berry-pots', '树果种植盆',
        'Allows portable berry growing.', '可以随时轻松培育树果的
便携式栽培容器。', null),
       (448, 0, 'Required to water berries within the berry pots.

Required to battle the sudowoodo on johto route 36.

This item cannot be directly used from the bag.', null, 'squirt-bottle', '杰尼龟喷壶',
        'Use on Sudowoodo blocking the path on Route 36. Also waters berries.', '浇水的道具。
能让树果种植盆里的
树果快快长大。', null),
       (449, 0, 'Used by trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If the wild Pokémon was encountered by fishing, the wild Pokémon’s catch rate is 3× normal.', null, 'lure-ball',
        '诱饵球', '3× effectiveness while fishing. Made from Blu Apricorn.', '有点与众不同的球。
能很容易地捕捉
用钓竿钓上来的宝可梦。', null),
       (450, 0, 'Used by trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If the trainer’s Pokémon’s level is higher than:

    * four times the wild Pokémon’s, the wild Pokémon’s catch rate is 8× normal.
    * than twice the wild Pokémon’s, the wild Pokémon’s catch rate is 4× normal.
    * the wild Pokémon’s, the wild Pokémon’s catch rate is 2× normal.', null, 'level-ball', '等级球',
        'Success rate based off of fraction target Pokémon is of user’s Pokémon. Made from Red Apricorn.', '有点与众不同的球。
要捕捉的宝可梦比自己宝可梦的
等级越低，就会越容易捕捉。', null),
       (1684, 500, null, null, 'fighting-tera-shard', '格斗太晶碎块', null, null, null),
       (2213, 0, null, null, 'tm224', 'TM224', null, null, null),
       (451, 0, 'Used by trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If the wild Pokémon is a clefairy, nidoran m, nidoran f, jigglypuff, skitty, or any evolution thereof, the wild Pokémon has 4× its catch rate.',
        null, 'moon-ball', '月亮球',
        '4× effectiveness on familes of Pokémon with a Moon Stone evolution. Made from Ylw Apricorn.', '有点与众不同的球。
能很容易地捕捉
使用月之石进化的宝可梦。', null),
       (452, 0, 'Used by a trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If the wild Pokémon weighs:

    * 409.6 kg (903.0 lb) or more, its catch rate is 40 more than normal.
    * 307.2 kg (677.3 lb) or more, its catch rate is 30 more than normal.
    * 204.8 kg (451.5 lb) or more, its catch rate is 20 more than normal.
    * less than 204.8 kg (451.5 lb), its catch rate is 20 less than normal.', null, 'heavy-ball', '沉重球',
        'Has flat bonus or penalty to catch rate depending on weight class of target. Made from Blk Apricorn.', '有点与众不同的球。
能很容易地捕捉
身体沉重的宝可梦。', null),
       (453, 0, 'Used by a trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

:   If the wild Pokémon’s base speed is 100 or more, its catch rate is 4× normal.', null, 'fast-ball', '速度球',
        '4× effectiveness on Pokémon with 100 or greater base speed. (Gen II: Roaming or Fleeing Pokémon). Made from Wht Apricorn.', '有点与众不同的球。
能很容易地捕捉
逃跑速度很快的宝可梦。', null),
       (454, 0, 'Used by a trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If caught, the wild Pokémon’s happiness starts at 200.', null, 'friend-ball', '友友球',
        'Caught Pokémon start with 200 happiness. Made from Grn Apricorn.', '有点与众不同的球。
捉到的野生宝可梦会立刻
变得和训练家亲密起来。', null),
       (455, 0, 'Used by a trainer in battle
:   Attempts to catch a wild Pokémon.  If used in a trainer battle, nothing happens and the ball is lost.

    If the trainer’s Pokémon and wild Pokémon are of the same species but opposite genders, the wild Pokémon’s catch rate is 8× normal.',
        null, 'love-ball', '甜蜜球',
        '8× effectiveness on opposite sex, same species targets of the Active Pokémon. Made from Pnk Apricorn.', '有点与众不同的球。
能很容易地捕捉
和自己宝可梦性别不同的宝可梦。', null),
       (456, 0, 'Used by a trainer in battle
:   Catches a wild Pokémon.

This item can only be used in pal park.', null, 'park-ball', '公园球', 'Catches Pokémon in the Pal Park every time.', '在伙伴公园里使用的
特殊的球。', null),
       (457, 300, 'Used by a trainer in battle
:   Attempts to catch a wild Pokémon.

    The wild Pokémon’s catch rate is 1.5× normal.', null, 'sport-ball', '竞赛球',
        'Tries to catch a Pokémon in the Bug-Catching contest in National Park. (Gen II: Park Ball)', '在捕虫大赛上使用的
特殊的球。', null),
       (458, 200, 'May be given to Kurt in azalea town to produce a level ball.', 10, 'red-apricorn', '红球果',
        'Used to make a Level Ball.', '红色的球果。
有种刺鼻的气味。', null),
       (459, 200, 'May be given to Kurt in azalea town to produce a lure ball.', 10, 'blue-apricorn', '蓝球果',
        'Used to make a Lure Ball.', '蓝色的球果。
略有一股青草的香味。', null),
       (460, 200, 'May be given to Kurt in azalea town to produce a moon ball.', 10, 'yellow-apricorn', '黄球果',
        'Used to make a Moon Ball.', '黄色的球果。
有种清爽的香味。', null),
       (461, 200, 'May be given to Kurt in azalea town to produce a friend ball.', 10, 'green-apricorn', '绿球果',
        'Used to make a Friend Ball.', '绿色的球果。
有种焦香的香味，非常神奇。', null),
       (462, 200, 'May be given to Kurt in azalea town to produce a love ball.', 10, 'pink-apricorn', '粉球果',
        'Used to make a Love Ball.', '粉红色的球果。
有种甜甜的，好闻的香味。', null),
       (463, 200, 'May be given to Kurt in azalea town to produce a fast ball.', 10, 'white-apricorn', '白球果',
        'Used to make a Fast Ball.', '白色的球果。
没有任何气味。', null),
       (464, 200, 'May be given to Kurt in azalea town to produce a heavy ball.', 10, 'black-apricorn', '黑球果',
        'Used to make a Heavy Ball.', '黑色的球果。
有种无法形容的气味。', null),
       (465, 0, 'Used by trainer outside of battle
:   Searches for hidden items.', null, 'dowsing-machine', '探宝器',
        'Use to find hidden items on the field. AKA Itemfinder.', '会对看不见的道具起反应，
并将它的位置告诉你的最尖端机器。
戴在头上使用。', null),
       (466, 350, 'May be traded for a tm64 in the vertical Underground Path.', 30, 'rage-candy-bar', '愤怒馒头',
        'HS: Traded for TM64. Gen II & Gen V: Acts as a Potion.', '卡吉镇特产的馒头。
能治愈１只宝可梦的
所有异常状态。', null),
       (467, 0, 'Causes groudon to appear in the embedded tower.', null, 'red-orb', '朱红色宝珠',
        'Summons Groudon to the Embedded Tower.', '散发着红色光辉的宝珠。
据说和丰缘地区的传说渊源颇深。', null),
       (468, 0, 'Causes kyogre to appear in the embedded tower.', null, 'blue-orb', '靛蓝色宝珠',
        'Summons Kyogre to the Embedded Tower.', '散发着蓝色光辉的宝珠。
据说和丰缘地区的传说渊源颇深。', null),
       (469, 0, 'Causes rayquaza to appear in the embedded tower.', null, 'jade-orb', '草绿色宝珠',
        'Summons Rayquaza to the Embedded Tower.', '散发着绿色光辉的宝珠。
据说和丰缘地区的传说渊源颇深。', null),
       (470, 0, 'When taken to the pewter city museum, causes latias or latios to attack the trainer.

The Pokémon to appear will be whicher can’t be encountered roaming in the wild.', null, 'enigma-stone', '谜之水晶',
        'S: Summons Latias H: Summons Latios.', '从地下挖出的水晶球。
虽然表面覆盖着岩石和尘土，
但非常漂亮。', null),
       (471, 0, 'Lists which unown forms the trainer has caught.', null, 'unown-report', '未知图腾笔记',
        'Keeps track of Unown types caught.', '记录着已找到的
未知图腾样子的笔记本。', null),
       (472, 0,
        'Allows the trainer to answer the daily question on Buena’s radio show.  Records the points earned for correct answers.',
        null, 'blue-card', '蓝卡', 'Keeps track of points from Buena’s show.', '可以将《葵妍的密语》
这节目的点数积攒起来的卡片。', null),
       (473, 0, 'Does nothing.', null, 'slowpoke-tail', '美味尾巴', 'A tasty tail that sells for a high price.', '非常美味的某种尾巴。
可以在商店高价出售。', null),
       (474, 0, 'May be given to the Kimono Girls to summon ho oh to the top of the bell tower.', null, 'clear-bell',
        '透明铃铛', 'HS: Allows Kimono-girls to summon Ho-oh. C: Summons Suicune to the Tin Tower.', '能发出静心宁神音色的，
非常古旧的铃铛。', null),
       (475, 0, 'Used by trainer outside of battle
:   Opens doors in the goldenrod city Radio Tower.', null, 'card-key', '钥匙卡',
        'HS: Opens doors in the Radio Tower. Gen III: Unlocks Silph Co Doors.', '用来打开电台卷帘门的
卡片式钥匙。', null),
       (476, 0, 'Used by trainer outside of battle
:   Opens the door to the basement tunnel under goldenrod city.', null, 'basement-key', '地下钥匙',
        'HS: Key to the tunnel under Goldenrod City. Gen III: Key to New Mauville.', '用来打开满金地道
大门的钥匙。', null),
       (477, 0, 'May be traded to Mr. Pokémon for an exp share.', null, 'red-scale', '红色鳞片',
        'Trade to Mr. Pokémon for an Exp. Share.', '在愤怒之湖里出现的
红色暴鲤龙的鳞片。
散发着像火一样的红色光芒。', null),
       (478, 0, 'May be traded to the Copycat for a pass.', null, 'lost-item', '遗失物',
        'A Poké Doll lost by the Copycat who lives in Saffron City. Trade for a Pass.', '模仿少女丢失的
魔尼尼人偶。', null),
       (479, 0, 'Allows the trainer to ride the Magnet Train between goldenrod city and saffron city.', null, 'pass',
        '磁浮列车自由票', 'Grants access to ride the Magnet Train between Goldenrod City and Saffron City.', '乘坐磁浮列车时所需的车票。
可以随时自由乘坐。', null),
       (480, 0, 'Must be replaced in the power plant to power the Magnet Train.', null, 'machine-part', '机械零件',
        'Must be replaced in the Power Plant to power the Magnet Train.', '发电厂里被盗的，
用于发电机的重要零件。', null),
       (481, 0, 'Causes lugia to appear in the whirl islands.', null, 'silver-wing', '银色之羽',
        'Summons Lugia to the Whirl Islands.', '散发着银色光辉的
神奇羽毛。', null),
       (482, 0, 'Causes ho oh to appear at the top of bell tower.', null, 'rainbow-wing', '虹色之羽',
        'Summons Ho-Oh at the top of the Bell Tower.', '散发着虹色光辉的
神奇羽毛。', null),
       (483, 0,
        'Must be obtained to trigger the break-in at Professor Elm’s lab, the first rival battle, and access to johto route 31.',
        null, 'mystery-egg', '神奇蛋', 'Deliver to Professor Elm.', '从宝可梦爷爷那里得到的
有着神奇花纹的蛋。
不知道是什么的蛋。', null),
       (484, 0, 'Used by trainer outside of battle
:   Changes the background music to the equivalent 8-bit music.', null, 'gb-sounds', 'ＧＢ播放器',
        'Use to listen to GameBoy era audio.', '可以听到怀旧歌曲的音乐播放器。
可用一个开关切换歌曲。', null),
       (485, 0, 'May be given to the Kimono Girls to summon lugia to the top of the bell tower.', null, 'tidal-bell',
        '海声铃铛', 'Allows Kimono-girls to summon Lugia.', '能发出静心宁神音色的，
非常古旧的铃铛。', null),
       (486, 0, 'Records the number of times the trainer has come in first place overall in the Pokéathlon.', null,
        'data-card-01', '数据卡０１',
        'Records the number of times the trainer has come in first place overall in the Pokéathlon.', 'ー
ー
ー', null),
       (487, 0, 'Records the number of times the trainer has come in last place overall in the Pokéathlon.', null,
        'data-card-02', '数据卡０２',
        'Records the number of times the trainer has come in last place overall in the Pokéathlon.', 'ー
ー
ー', null),
       (488, 0, 'Records the number of times the trainer’s Pokémon have dashed in the Pokéathlon.', null,
        'data-card-03', '数据卡０３', 'Records the number of times the trainer’s Pokémon have dashed in the Pokéathlon.', 'ー
ー
ー', null),
       (489, 0, 'Records the number of times the trainer’s Pokémon have jumped in the Pokéathlon.', null,
        'data-card-04', '数据卡０４', 'Records the number of times the trainer’s Pokémon have jumped in the Pokéathlon.', 'ー
ー
ー', null),
       (490, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Hurdle Dash.', null,
        'data-card-05', '数据卡０５',
        'Records the number of times the trainer has come in first in the Pokéathlon Hurdle Dash.', 'ー
ー
ー', null),
       (491, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Relay Run.', null,
        'data-card-06', '数据卡０６',
        'Records the number of times the trainer has come in first in the Pokéathlon Relay Run.', 'ー
ー
ー', null),
       (492, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Pennant Capture.', null,
        'data-card-07', '数据卡０７',
        'Records the number of times the trainer has come in first in the Pokéathlon Pennant Capture.', 'ー
ー
ー', null),
       (493, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Block Smash.', null,
        'data-card-08', '数据卡０８',
        'Records the number of times the trainer has come in first in the Pokéathlon Block Smash.', 'ー
ー
ー', null),
       (494, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Disc Catch.', null,
        'data-card-09', '数据卡０９',
        'Records the number of times the trainer has come in first in the Pokéathlon Disc Catch.', 'ー
ー
ー', null),
       (495, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Snow Throw.', null,
        'data-card-10', '数据卡１０',
        'Records the number of times the trainer has come in first in the Pokéathlon Snow Throw.', 'ー
ー
ー', null),
       (496, 0, 'Records the number of points the trainer has earned in the Pokéathlon.', null, 'data-card-11',
        '数据卡１１', 'Records the number of points the trainer has earned in the Pokéathlon.', 'ー
ー
ー', null),
       (497, 0, 'Records the number of times the trainer’s Pokémon have messed up in the Pokéathlon.', null,
        'data-card-12', '数据卡１２',
        'Records the number of times the trainer’s Pokémon have messed up in the Pokéathlon.', 'ー
ー
ー', null),
       (498, 0, 'Records the number of times the trainer’s Pokémon have defeated themselves in the Pokéathlon.', null,
        'data-card-13', '数据卡１３',
        'Records the number of times the trainer’s Pokémon have defeated themselves in the Pokéathlon.', 'ー
ー
ー', null),
       (499, 0, 'Records the number of times the trainer’s Pokémon have tackled in the Pokéathlon.', null,
        'data-card-14', '数据卡１４', 'Records the number of times the trainer’s Pokémon have tackled in the Pokéathlon.', 'ー
ー
ー', null),
       (500, 0, 'Records the number of times the trainer’s Pokémon have fallen in the Pokéathlon.', null,
        'data-card-15', '数据卡１５', 'Records the number of times the trainer’s Pokémon have fallen in the Pokéathlon.', 'ー
ー
ー', null),
       (501, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Ring Drop.', null,
        'data-card-16', '数据卡１６',
        'Records the number of times the trainer has come in first in the Pokéathlon Ring Drop.', 'ー
ー
ー', null),
       (502, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Lamp Jump.', null,
        'data-card-17', '数据卡１７',
        'Records the number of times the trainer has come in first in the Pokéathlon Lamp Jump.', 'ー
ー
ー', null),
       (503, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Circle Push.', null,
        'data-card-18', '数据卡１８',
        'Records the number of times the trainer has come in first in the Pokéathlon Circle Push.', 'ー
ー
ー', null),
       (504, 0,
        'Records the number of times the trainer has come in first place overall in the Pokéathlon over wirelss.', null,
        'data-card-19', '数据卡１９',
        'Records the number of times the trainer has come in first place overall in the Pokéathlon over wirelss.', 'ー
ー
ー', null),
       (505, 0,
        'Records the number of times the trainer has come in last place overall in the Pokéathlon over wireless.', null,
        'data-card-20', '数据卡２０',
        'Records the number of times the trainer has come in last place overall in the Pokéathlon over wireless.', 'ー
ー
ー', null),
       (506, 0, 'Records the number of times the trainer has come in first across all Pokéathlon events.', null,
        'data-card-21', '数据卡２１',
        'Records the number of times the trainer has come in first across all Pokéathlon events.', 'ー
ー
ー', null),
       (1685, 500, null, null, 'poison-tera-shard', '毒太晶碎块', null, null, null),
       (507, 0, 'Records the number of times the trainer has come in last across all Pokéathlon events.', null,
        'data-card-22', '数据卡２２',
        'Records the number of times the trainer has come in last across all Pokéathlon events.', 'ー
ー
ー', null),
       (508, 0, 'Records the number of times the trainer has switched Pokémon in the Pokéathlon.', null, 'data-card-23',
        '数据卡２３', 'Records the number of times the trainer has switched Pokémon in the Pokéathlon.', 'ー
ー
ー', null),
       (509, 0, 'Records the number of times the trainer has come in first in the Pokéathlon Goal Roll.', null,
        'data-card-24', '数据卡２４',
        'Records the number of times the trainer has come in first in the Pokéathlon Goal Roll.', 'ー
ー
ー', null),
       (510, 0, 'Records the number of times the trainer’s Pokémon received prizes in the Pokéathlon.', null,
        'data-card-25', '数据卡２５',
        'Records the number of times the trainer’s Pokémon received prizes in the Pokéathlon.', 'ー
ー
ー', null),
       (511, 0, 'Records the number of times the trainer has instructed Pokémon in the Pokéathlon.', null,
        'data-card-26', '数据卡２６', 'Records the number of times the trainer has instructed Pokémon in the Pokéathlon.', 'ー
ー
ー', null),
       (512, 0, 'Records the total time spent in the Pokéathlon.', null, 'data-card-27', '数据卡２７',
        'Records the total time spent in the Pokéathlon.', 'ー
ー
ー', null),
       (513, 0, 'Does nothing.', null, 'lock-capsule', '上锁的容器', 'Contains TM95 (Snarl).', '需用特殊钥匙打开的
坚固的容器。', null),
       (514, 0, 'Does nothing.', null, 'photo-album', '相册', 'Stores photos from your adventure.', '用来摆放在冒险中拍摄的
纪念相片的相册。', null),
       (515, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'orange-mail', 'Orange Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A ZIGZAGOON-print
MAIL to be held by
a POKéMON.', null),
       (516, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'harbor-mail', 'Harbor Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A WINGULL-print
MAIL to be held by
a POKéMON.', null),
       (517, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'glitter-mail', 'Glitter Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A PIKACHU-print
MAIL to be held by
a POKéMON.', null),
       (518, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'mech-mail', 'Mech Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A MAGNEMITE-print
MAIL to be held by
a POKéMON.', null),
       (519, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'wood-mail', 'Wood Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A SLAKOTH-print
MAIL to be held by
a POKéMON.', null),
       (520, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'wave-mail', 'Wave Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A WAILMER-print
MAIL to be held by
a POKéMON.', null),
       (521, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bead-mail', 'Bead Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'MAIL featuring a
sketch of the
holding POKéMON.', null),
       (522, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'shadow-mail', 'Shadow Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A DUSKULL-print
MAIL to be held by
a POKéMON.', null),
       (523, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'tropic-mail', 'Tropic Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A BELLOSSOM-print
MAIL to be held by
a POKéMON.', null),
       (524, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'dream-mail', 'Dream Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'MAIL featuring a
sketch of the
holding POKéMON.', null),
       (558, 0, 'Holds Berry Powder from Berry Crushing.', null, 'powder-jar', 'Powder Jar',
        'Stores Berry Powder made using a Berry Crusher.', 'Stores BERRY
POWDER made using
a BERRY CRUSHER.', null),
       (676, 0, '', null, 'plasma-card', '等离子卡', 'Required to progress in the Plasma Frigate.', '在等离子驱逐舰的
船内输入密码时，
需要用到的钥匙卡。', null),
       (525, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'fab-mail', 'Fab Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'A gorgeous-print
MAIL to be held
by a POKéMON.', null),
       (526, 0, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'retro-mail', 'Retro Mail', 'Lets a Trainer write a message and send it via Pokémon trade.', 'MAIL featuring the
drawings of three
POKéMON.', null),
       (527, 0,
        'Increases movement speed outside or in caves.  Faster than the acro bike.  Allows the trainer to ascend muddy slopes.',
        null, 'mach-bike', '音速自行车',
        'Faster than the Acro Bike, and can ride up sandy slopes and across cracked floors.', '能以２倍以上的速度移动的
折叠式自行车。', null),
       (528, 0,
        'Increases movement speed outside or in caves.  Slower than the mach bike.  Can perform various tricks, allowing the trainer to reach certain special areas.',
        null, 'acro-bike', '越野自行车', 'More maneuverable than the Mach Bike, and allows hopping along rails.', '能做出跳跃或抬前轮动作的
折叠式自行车。', null),
       (529, 0, 'Waters Berry plants.', null, 'wailmer-pail', '吼吼鲸喷壶', 'Used to water berries.', '浇水的道具。
能让埋在土壤里的
树果快快长大。', null),
       (530, 0, 'Contains a machine part to be delivered to Captain Stern.', null, 'devon-goods', 'Devon Goods',
        'Contains mechanical parts to be delivered to Captain Stern.', 'A package that
contains DEVON’s
machine parts.', null),
       (531, 0, 'Collects soot when walking through tall grass on hoenn route 113.', null, 'soot-sack', '集灰袋',
        'Stores volcanic ash from Route 113.', '将堆积起来的火山灰
收集起来的袋子。', null),
       (532, 0, 'Stores Pokéblocks.', null, 'pokeblock-case', 'Pokéblock Case', 'Holds Pokéblocks.', 'A case for holding
POKéBLOCKS made with
a BERRY BLENDER.', null),
       (533, 0, 'Contains a letter to be delivered to Steven.', null, 'letter', '给大吾的信',
        'A letter to Steven from the Devon Corp president.', '从得文社长
那里得到的信。', null),
       (534, 0,
        'Provides access to southern island and either latias or latios, whichever is not available roaming around Hoenn.',
        null, 'eon-ticket', '无限船票', 'Provides access to Southern Island and Latias or Latios.', '前往南方孤岛的船票。
橙华道馆的馆主千里
知道其中的秘密！？', null),
       (535, 0, 'May be traded to Captain Stern for a deep sea tooth or a deep sea scale.', null, 'scanner', '探测器',
        'Trade to Captain Stern for a DeepSeaTooth or DeepSeaScale.', '在海紫堇中
找到的道具。', null),
       (536, 0, 'Allows the trainer to enter the desert on hoenn route 111.', null, 'go-goggles', 'ＧＯＧＯ护目镜',
        'Allows passage through windy deserts.', '能在沙漠的沙暴中
保护眼睛的出色护目镜。', null),
       (537, 0, 'RSE: May be traded to Professor Cozmo for tm27.

FRLG: A meteorite to be delivered to Lostelle’s father.', null, 'meteorite', '陨石',
        'FL: Deliver to Lostelle’s father. RSE: Trade to Professor Cozmo for TM27 (Return).', '原本是落入流星瀑布里的陨石。
是在烟囱山获得的。', null),
       (538, 0, 'Unlocks room 1 on the abandoned ship.', null, 'rm-1-key', 'Rm. 1 Key',
        'Unlocks room 1 on the Abandoned Ship.', 'A key that opens a
door inside the
ABANDONED SHIP.', null),
       (539, 0, 'Unlocks room 2 on the abandoned ship.', null, 'rm-2-key', 'Rm. 2 Key',
        'Unlocks room 2 on the Abandoned Ship.', 'A key that opens a
door inside the
ABANDONED SHIP.', null),
       (540, 0, 'Unlocks room 4 on the abandoned ship.', null, 'rm-4-key', 'Rm. 4 Key',
        'Unlocks room 4 on the Abandoned Ship.', 'A key that opens a
door inside the
ABANDONED SHIP.', null),
       (541, 0, 'Unlocks room 6 on the abandoned ship.', null, 'rm-6-key', 'Rm. 6 Key',
        'Unlocks room 6 on the Abandoned Ship.', 'A key that opens a
door inside the
ABANDONED SHIP.', null),
       (542, 0, 'Reveals invisble kecleon on the overworld.', null, 'devon-scope', '得文侦测镜',
        'Allows spotting of invisible Kecleon.', '会对看不见的宝可梦起反应，
并发出声音的得文特制产品。', null),
       (543, 0, 'A parcel to be delivered to Professor Oak for a Pokédex.', null, 'oaks-parcel', 'Oak’s Parcel',
        'Trade to Prof. Oak for a Pokédex.', 'A parcel for PROF.
OAK from a POKéMON
MART’s clerk.', null),
       (544, 0, 'Wakes up sleeping Pokémon.  Required to wake up sleeping snorlax on the overworld.', null,
        'poke-flute', '宝可梦之笛', 'Use to awaken sleeping Pokémon, including Snorlax on roads.', '能吹出让睡着的宝可梦
都会情不自禁醒来的
美妙音色的笛子。', null),
       (545, 0, 'May be traded for a bicycle.', null, 'bike-voucher', 'Bike Voucher',
        'Trade in Cerulean bike shop for a Bicycle.', 'A voucher for
obtaining a bicycle
from the BIKE SHOP.', null),
       (546, 0, 'The Safari Zone warden’s teeth, to be returned to him for hm04.', null, 'gold-teeth', '金假牙',
        'The Safari Zone Warden’s dentures. Trade for HM04 (Strength).', '狩猎地带的园长遗失的金假牙。
装上它后，笑起来十分耀眼。', null),
       (547, 0, 'Operates the elevator in the Celadon Rocket Hideout.', null, 'lift-key', '电梯钥匙',
        'Operates the elevator in Team Rocket’s Celadon Hideout.', '带有火箭队标志的钥匙。
能启动位于火箭队基地的电梯。', null),
       (548, 0, 'Identifies ghosts in pokemon tower.', null, 'silph-scope', '西尔佛检视镜',
        'Used to identify the true forms of ghosts in Pokémon Tower.', '西尔佛公司制造的透视镜。
可以看见人眼无法看见的东西。', null),
       (549, 0, 'Records information on various famous people.', null, 'fame-checker', 'Fame Checker',
        'Records information about NPCs.', 'Stores information
on famous people
for instant recall.', null),
       (550, 0, 'Stores TMs and HMs.', null, 'tm-case', '招式学习器盒', 'Holds TMs.', 'ー
ー
ー', null),
       (551, 0, 'Stores Berries.', null, 'berry-pouch', 'Berry Pouch', 'Holds berries.', 'A convenient
container that
holds BERRIES.', null),
       (552, 0, 'Teaches beginning trainers basic information.', null, 'teachy-tv', 'Teachy TV',
        'Teachers basic trainer advice.', 'A TV set tuned to
an advice program
for TRAINERS.', null),
       (553, 0, 'Provides access to the first three Sevii Islands.', null, 'tri-pass', 'Tri-Pass',
        'Grants access to the first three Sevii Islands.', 'A pass for ferries
between ONE, TWO,
and THREE ISLAND.', null),
       (554, 0, 'Provides access to the Sevii Islands.', null, 'rainbow-pass', 'Rainbow Pass',
        'Grants access to all of the Sevii Islands.', 'For ferries serving
VERMILION and the
SEVII ISLANDS.', null),
       (555, 0, 'Used to bribe the saffron city guards for entry to the city.', null, 'tea', '茶',
        'Grants access to Saffron City.', '有一点点苦，
却又芬芳宜人的香茶。
抿一口，清咽润喉。', null),
       (1026, 300, null, 30, 'marble', '弹珠', null, '非常圆润的玻璃球。
能在透明的球体中
看见彩色玻璃的花纹。', null),
       (559, 0, 'Deliver to Celio for use in the Network Machine.', null, 'ruby', 'Ruby',
        'Give to Celio with Sapphire to activate Network Machine and get Rainbow Pass.', 'An exquisite, red-
glowing gem that
symbolizes passion.', null),
       (560, 0, 'Deliver to Celio for use in the Network Machine.', null, 'sapphire', 'Sapphire',
        'Give to Celio with Ruby to activate Network Machine and get Rainbow Pass.', 'A brilliant blue gem
that symbolizes
honesty.', null),
       (561, 0, 'Provides access to the magma hideout in the jagged pass.', null, 'magma-emblem', 'Magma Emblem',
        'Provides access to the Team Magma Hideout in the Jagged Pass.', 'A medal-like item in
the same shape as
TEAM MAGMA’s mark.', null),
       (562, 0, 'Provides access to Faraway Island and mew.', null, 'old-sea-map', 'Old Sea Map',
        'Allows access to Faraway Island and Mew. Unreleased outside of Japan.', 'A faded sea chart
that shows the way
to a certain island.', null),
       (563, 0, 'Held by genesect
:   Holder’s buster is blue, and its techno blast is water-type.', 70, 'douse-drive', '水流卡带',
        'Grants Genesect a blue, Water-type Techno Blast.', '这是让盖诺赛克特携带的卡带。
携带后，高科技光炮这招式
就会变为水属性。', null),
       (564, 0, 'Held by genesect
:   Holder’s buster is yellow, and its techno blast is electric-type.', 70, 'shock-drive', '闪电卡带',
        'Grants Genesect a yellow, Electric-type Techno Blast.', '这是让盖诺赛克特携带的卡带。
携带后，高科技光炮这招式
就会变为电属性。', null),
       (565, 0, 'Held by genesect
:   Holder’s buster is red, and its techno blast is fire-type.', 70, 'burn-drive', '火焰卡带',
        'Grants Genesect a red, Fire-type Techno Blast.', '这是让盖诺赛克特携带的卡带。
携带后，高科技光炮这招式
就会变为火属性。', null),
       (566, 0, 'Held by genesect
:   Holder’s buster is white, and its techno blast becomes ice-type.', 70, 'chill-drive', '冰冻卡带',
        'Grants Genesect a white, Ice-type Techno Blast.', '这是让盖诺赛克特携带的卡带。
携带后，高科技光炮这招式
就会变为冰属性。', null),
       (567, 3000, 'Used on a friendly Pokémon
:   Restores 20 HP.', 30, 'sweet-heart', '心形甜点', 'Restores 20 HP.', '非常甜腻的巧克力。
能让１只宝可梦
回复２０ＨＰ。', null),
       (568, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'greet-mail', '初次邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写初次见面这类寒暄语的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (569, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'favored-mail', '喜爱邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写表达自己喜好这类邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (570, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'rsvp-mail', '邀请邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写邀请对方这类邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (571, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'thanks-mail', '感谢邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写表达感谢这类邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (572, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'inquiry-mail', '询问邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写询问对方这类邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (573, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'like-mail', '推荐邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写推荐内容这类邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (574, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'reply-mail', '回复邮件', 'Lets a Trainer write a message and send it via Pokémon trade.', '方便写回复邮件的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (575, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bridge-mail-s', '桥梁邮件Ｓ', 'Lets a Trainer write a message and send it via Pokémon trade.', '印有通天桥图案的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (594, 200, 'Held
:   When the holder uses a damaging grass-type move, the move has 1.5× power and this item is consumed.', null,
        'grass-gem', '草之宝石',
        'Held: When the holder uses a damaging grass-type move, the move has 1.5× power and this item is consumed.', '草属性的宝石。
携带后，草属性的
招式威力仅会增强１次。', null),
       (1686, 500, null, null, 'ground-tera-shard', '地面太晶碎块', null, null, null),
       (576, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bridge-mail-d', '桥梁邮件Ｈ', 'Lets a Trainer write a message and send it via Pokémon trade.', '印有鲜红色活动桥图案的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (577, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bridge-mail-t', '桥梁邮件Ｃ', 'Lets a Trainer write a message and send it via Pokémon trade.', '印有钢铁悬索桥图案的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (578, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bridge-mail-v', '桥梁邮件Ｖ', 'Lets a Trainer write a message and send it via Pokémon trade.', '印有砖桥图案的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (579, 50, 'Used to send short messages to other players via Pokémon trading.  Trainer may compose a message from a finite list of words when giving this item to a Pokémon.  Once taken and read, a message may be erased and this item can be reused, or the message may be stored in the trainer’s PC.

Held
:   Holder cannot be placed in the PC.  Any move attempting to remove this item from the holder will fail.', null,
        'bridge-mail-m', '桥梁邮件Ｗ', 'Lets a Trainer write a message and send it via Pokémon trade.', '印有拱桥图案的信纸。
使用该道具时，
需要让宝可梦携带着来用。', null),
       (580, 2000, 'Held by feebas
:   Holder evolves into milotic when traded.', 30, 'prism-scale', '美丽鳞片',
        'Traded on a Feebas: Holder evolves into Milotic.', '能让某些特定宝可梦
进化的神奇鳞片。
散发着虹色光辉。', null),
       (581, 4000, 'Held by a Pokémon that is not fully evolved
:   Holder has 1.5× Defense and Special Defense.', 40, 'eviolite', '进化奇石',
        'Held: Holder has 1.5× Defense and Special Defense, as long as it’s not fully evolved.', '进化的神奇石块。
携带后，还能进化的宝可梦的
防御和特防就会提高。', null),
       (582, 4000, 'Held
:   Holder has 0.5× weight.', 30, 'float-stone', '轻石', 'Held: Holder has 0.5× weight.', '非常轻的石头。
携带后，宝可梦的体重会变轻。', null),
       (583, 4000, 'Held
:   When the holder is hit by a contact move, the attacking Pokémon takes 1/6 its max HP in damage.', 60,
        'rocky-helmet', '凸凸头盔',
        'Held: When the holder is hit by a contact move, the attacking Pokémon takes 1/6 its max HP in damage.', '让宝可梦携带后，
在受到打击招式攻击时，
能给予对手伤害。', null),
       (584, 4000, 'Held
:   Holder is immune to ground-type moves, spikes, toxic spikes, and arena trap.

    This effect does not apply during gravity or ingrain.

    When the holder takes damage from a move, this item is consumed.', 10, 'air-balloon', '气球',
        'Held: Grants immunity to Ground-type moves, Spikes, and Toxic Spikes. Consumed when the holder takes damage from a move.', '让宝可梦携带后，
宝可梦会浮在空中。
受到攻击就会破裂。', null),
       (585, 4000, 'Held
:   When the holder takes damage directly from a move and does not faint, it switches out for another random, non-fainted Pokémon in its party.
This effect does not activate if another effect prevents the holder from switching out.', 10, 'red-card', '红牌',
        'Held: When the holder takes damage from a move, the opponent switches out for another random party Pokémon. Consumed after use.', '有着神奇力量的卡片。
携带后，能让使用了
招式的对手退场。', null),
       (586, 4000, 'Held
:   When one of the user’s types would render it immune to damage, that type is ignored for damage calculation.', 10,
        'ring-target', '标靶', 'Held: Negates the holder’s type immunities. Ability immunities are not removed.', '原本因宝可梦的属性相克关系
而无效的招式现在会变得
能够击中自己。', null),
       (587, 4000, 'Held
:   Moves used by the holder that trap and damage a target for multiple turns (e.g. bind, fire spin) inflict twice their usual per-turn damage.',
        30, 'binding-band', '紧绑束带', 'Held: Doubles the per-turn damage of multi-turn trapping moves.', '能增强绑紧招式的束带。
携带后，绑紧招式的威力会变强。', null),
       (588, 4000, 'Held
:   When the holder takes water-type damage from a move, its Special Attack rises by one stage and this item is consumed.',
        30, 'absorb-bulb', '球根',
        'Held: Raises the holder’s Special Attack by one stage when it takes Water-type damage.', '一次性使用的球根。
携带它的宝可梦如果受到水属性
招式的攻击，特攻就会提高。', null),
       (589, 4000, 'Held
:   When the holder takes electric-type damage from a move, its Attack rises by one stage and this item is consumed.',
        30, 'cell-battery', '充电电池',
        'Held: Raises the holder’s Attack by one stage when it takes Electric-type damage.', '一次性使用的充电电池。
携带它的宝可梦如果受到电属性
招式的攻击，攻击就会提高。', null),
       (590, 4000, 'Held
:   When the holder takes damage directly from a move and does not faint, it switches out for another non-fainted Pokémon in its party, as chosen by the Trainer.
This effect does not activate if another effect prevents the holder from switching out.', 30, 'eject-button',
        '逃脱按键',
        'Held: When the holder takes damage from a move, it switches out for a party Pokémon of the Trainer’s choice.', '携带它的宝可梦如果受到招式攻击，
就能逃脱战斗，并和同行的
其他宝可梦进行替换。', null),
       (591, 200, 'Held
:   When the holder uses a damaging fire-type move, the move has 1.5× power and this item is consumed.', null,
        'fire-gem', '火之宝石',
        'Held: When the holder uses a damaging fire-type move, the move has 1.5× power and this item is consumed.', '火属性的宝石。
携带后，火属性的
招式威力仅会增强１次。', null),
       (592, 200, 'Held
:   When the holder uses a damaging water-type move, the move has 1.5× power and this item is consumed.', null,
        'water-gem', '水之宝石',
        'Held: When the holder uses a damaging water-type move, the move has 1.5× power and this item is consumed.', '水属性的宝石。
携带后，水属性的
招式威力仅会增强１次。', null),
       (593, 200, 'Held
:   When the holder uses a damaging electric-type move, the move has 1.5× power and this item is consumed.', null,
        'electric-gem', '电之宝石',
        'Held: When the holder uses a damaging electric-type move, the move has 1.5× power and this item is consumed.', '电属性的宝石。
携带后，电属性的
招式威力仅会增强１次。', null),
       (2214, 0, null, null, 'tm225', 'TM225', null, null, null),
       (595, 200, 'Held
:   When the holder uses a damaging ice-type move, the move has 1.5× power and this item is consumed.', null, 'ice-gem',
        '冰之宝石',
        'Held: When the holder uses a damaging ice-type move, the move has 1.5× power and this item is consumed.', '冰属性的宝石。
携带后，冰属性的
招式威力仅会增强１次。', null),
       (596, 200, 'Held
:   When the holder uses a damaging fighting-type move, the move has 1.5× power and this item is consumed.', null,
        'fighting-gem', '格斗宝石',
        'Held: When the holder uses a damaging fighting-type move, the move has 1.5× power and this item is consumed.', '格斗属性的宝石。
携带后，格斗属性的
招式威力仅会增强１次。', null),
       (597, 200, 'Held
:   When the holder uses a damaging poison-type move, the move has 1.5× power and this item is consumed.', null,
        'poison-gem', '毒之宝石',
        'Held: When the holder uses a damaging poison-type move, the move has 1.5× power and this item is consumed.', '毒属性的宝石。
携带后，毒属性的
招式威力仅会增强１次。', null),
       (598, 200, 'Held
:   When the holder uses a damaging ground-type move, the move has 1.5× power and this item is consumed.', null,
        'ground-gem', '地面宝石',
        'Held: When the holder uses a damaging ground-type move, the move has 1.5× power and this item is consumed.', '地面属性的宝石。
携带后，地面属性的
招式威力仅会增强１次。', null),
       (599, 200, 'Held
:   When the holder uses a damaging flying-type move, the move has 1.5× power and this item is consumed.', null,
        'flying-gem', '飞行宝石',
        'Held: When the holder uses a damaging flying-type move, the move has 1.5× power and this item is consumed.', '飞行属性的宝石。
携带后，飞行属性的
招式威力仅会增强１次。', null),
       (600, 200, 'Held
:   When the holder uses a damaging psychic-type move, the move has 1.5× power and this item is consumed.', null,
        'psychic-gem', '超能力宝石',
        'Held: When the holder uses a damaging psychic-type move, the move has 1.5× power and this item is consumed.', '超能力属性的宝石。
携带后，超能力属性的
招式威力仅会增强１次。', null),
       (601, 200, 'Held
:   When the holder uses a damaging bug-type move, the move has 1.5× power and this item is consumed.', null, 'bug-gem',
        '虫之宝石',
        'Held: When the holder uses a damaging bug-type move, the move has 1.5× power and this item is consumed.', '虫属性的宝石。
携带后，虫属性的
招式威力仅会增强１次。', null),
       (602, 200, 'Held
:   When the holder uses a damaging rock-type move, the move has 1.5× power and this item is consumed.', null,
        'rock-gem', '岩石宝石',
        'Held: When the holder uses a damaging rock-type move, the move has 1.5× power and this item is consumed.', '岩石属性的宝石。
携带后，岩石属性的
招式威力仅会增强１次。', null),
       (603, 200, 'Held
:   When the holder uses a damaging ghost-type move, the move has 1.5× power and this item is consumed.', null,
        'ghost-gem', '幽灵宝石',
        'Held: When the holder uses a damaging ghost-type move, the move has 1.5× power and this item is consumed.', '幽灵属性的宝石。
携带后，幽灵属性的
招式威力仅会增强１次。', null),
       (604, 200, 'Held
:   When the holder uses a damaging dark-type move, the move has 1.5× power and this item is consumed.', null,
        'dark-gem', '恶之宝石',
        'Held: When the holder uses a damaging dark-type move, the move has 1.5× power and this item is consumed.', '恶属性的宝石。
携带后，恶属性的
招式威力仅会增强１次。', null),
       (605, 200, 'Held
:   When the holder uses a damaging steel-type move, the move has 1.5× power and this item is consumed.', null,
        'steel-gem', '钢之宝石',
        'Held: When the holder uses a damaging steel-type move, the move has 1.5× power and this item is consumed.', '钢属性的宝石。
携带后，钢属性的
招式威力仅会增强１次。', null),
       (606, 300, 'Used on a party Pokémon
:   Increases the target’s HP effort by 1.', 20, 'health-wing', '体力之羽', 'Increases HP effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
ＨＰ的基础点数。', null),
       (607, 300, 'Used on a party Pokémon
:   Increases the target’s Attack effort by 1.', 20, 'muscle-wing', '肌力之羽', 'Increases Attack effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
攻击的基础点数。', null),
       (608, 300, 'Used on a party Pokémon
:   Increases the target’s Defense effort by 1.', 20, 'resist-wing', '抵抗之羽', 'Increases Defense effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
防御的基础点数。', null),
       (609, 300, 'Used on a party Pokémon
:   Increases the target’s Special Attack effort by 1.', 20, 'genius-wing', '智力之羽',
        'Increases Special Attack effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
特攻的基础点数。', null),
       (610, 300, 'Used on a party Pokémon
:   Increases the target’s Special Defense effort by 1.', 20, 'clever-wing', '精神之羽',
        'Increases Special Defense effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
特防的基础点数。', null),
       (611, 300, 'Used on a party Pokémon
:   Increases the target’s Speed effort by 1.', 20, 'swift-wing', '瞬发之羽', 'Increases Speed effort by 1.', '用于宝可梦的道具。
能稍微提高１只宝可梦的
速度的基础点数。', null),
       (612, 1000, 'Vendor trash.', 20, 'pretty-wing', '美丽之羽', 'Sell for 100 Pokédollars.', '仅仅只是漂亮，
没有任何效果，
极其普通的羽毛。', null),
       (613, 7000, 'Give to a scientist in a museum to receive a tirtouga.', 100, 'cover-fossil', '背盖化石',
        'Can be revived into a tirtouga.', '很久以前栖息在海里的
古代宝可梦的化石。
好像是后背的一部分。', null),
       (614, 7000, 'Give to a scientist in a museum to receive an archen.', 100, 'plume-fossil', '羽毛化石',
        'Can be revived into an archen.', '据说是鸟宝可梦的祖先，
古代宝可梦的化石。
好像是翅膀的一部分。', null),
       (615, 0, 'Allows passage on the castelia city ship, which leads to liberty garden and victini.', null,
        'liberty-pass', '自由船票', 'Allows access to Liberty Garden and Victini.', '用于前往自由庭园岛的特殊船票。
可以从飞云市上船。', null),
       (616, 200, 'Acts as currency to activate Pass Powers in the Entralink.', 30, 'pass-orb', '释出之玉',
        'Activates Pass Powers.', '在这块神奇的玉石里，
封存着用于产生
释出之力的合众之力。', null),
       (617, 0, 'Can only be used in Entree Forest, to catch Pokémon encountered in the Dream World.

Used in battle
:   Catches a wild Pokémon without fail.', null, 'dream-ball', '梦境球', 'Catches Pokémon found in the Dream World.', '在连入之森中，不知何时
出现在包包里的梦中球。
能捉到任何宝可梦。', null),
       (618, 100, 'Used in battle
:   Ends a wild battle.  Cannot be used in trainer battles.', 30, 'poke-toy', '宝可尾草', 'Ends a wild battle.', '能吸引宝可梦注意的道具。
在和野生宝可梦的
战斗中绝对可以逃走。', null),
       (619, 0, 'Stores props for the Pokémon Musical.', null, 'prop-case', '物品箱',
        'Stores props for the Pokémon Musical.', '漂亮精美的箱子。
可以存放宝可梦出演音乐剧时
用来装扮自己的多种小物件。', null),
       (675, 0, '', null, 'shiny-charm', '闪耀护符', 'Raises the chance of finding a shiny Pokémon.', '拥有它之后，据说会
更容易遇见发光宝可梦的
神奇闪光护符。', null),
       (620, 0, 'Only used as a plot point; this item is given to the player and taken away in the same cutscene.',
        null, 'dragon-skull', '龙之骨', 'Return to the museum in Nacrene City.', '据说是能在狂风暴雨的
大海上随意翱翔的宝可梦的头骨。', null),
       (621, 15000, 'Cult vendor trash.', 30, 'balm-mushroom', '芳香蘑菇', 'Sell to Hungry Maid for 25000 Pokédollars.', '能让附近一带
芳香四溢的珍稀蘑菇。
可以在商店高价出售。', null),
       (622, 40000, 'Cult vendor trash.', 130, 'big-nugget', '巨大金珠', 'Sell to Ore Collector for 30000 Pokédollars.', '以纯金制成，
闪着金光的大珠子。
可以在商店高价出售。', null),
       (623, 20000, 'Cult vendor trash.', 30, 'pearl-string', '丸子珍珠',
        'Sell to Ore Collector for 25000 Pokédollars.', '散发着美丽银辉
且非常大颗的珍珠。
可以在商店高价出售。', null),
       (624, 25000, 'Cult vendor trash.', 30, 'comet-shard', '彗星碎片', 'Sell to Ore Collector for 60000 Pokédollars.', '彗星临近时，
掉落到地表上的碎片。
可以在商店高价出售。', null),
       (625, 0, 'Cult vendor trash.', 30, 'relic-copper', '古代铜币', 'Sell to Villa Owner for 1000 Pokédollars.', '约３０００年前的
文明使用的铜币。', null),
       (626, 0, 'Cult vendor trash.', 30, 'relic-silver', '古代银币', 'Sell to Villa Owner 5000 Pokédollars.', '约３０００年前的
文明使用的银币。', null),
       (627, 60000, 'Cult vendor trash.', 30, 'relic-gold', '古代金币', 'Sell to Villa Owner 10000 Pokédollars.', '约３０００年前的
文明使用的金币。', null),
       (628, 0, 'Cult vendor trash.', 30, 'relic-vase', '古代之壶', 'Sell to Villa Owner 50000 Pokédollars.', '约３０００年前的
文明制造的壶。', null),
       (629, 0, 'Cult vendor trash.', 30, 'relic-band', '古代手镯', 'Sell to Villa Owner for 100000 Pokédollars.', '约３０００年前的
文明制造的手镯。', null),
       (630, 0, 'Cult vendor trash.', 30, 'relic-statue', '古代石像', 'Sell to Villa Owner 200000 Pokédollars.', '约３０００年前的
文明制造的石像。', null),
       (631, 0, 'Cult vendor trash.', 30, 'relic-crown', '古代王冠', 'Sell to Villa Owner for 300000 Pokédollars.', '约３０００年前的
文明制造的王冠。', null),
       (632, 350, 'Used on a party Pokémon
:   Cures any status ailment and confusion.', 30, 'casteliacone', '飞云冰淇淋',
        'Cures any status ailment and confusion.', '飞云市特产的冰淇淋。
能治愈１只宝可梦的
所有异常状态。', null),
       (633, 0, 'Used on a party Pokémon in battle
:   Raises the target’s critical hit rate by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'dire-hit-2', '要害攻击２',
        'Raises critical hit rate by two stages in battle.  Wonder Launcher only.', '击中要害的几率会提高。
每次使用效果都会提升。
离场后，效果便会消失。', null),
       (634, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Speed by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-speed-2', '速度强化２',
        'Raises Speed by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的速度。
离场后，效果便会消失。', null),
       (635, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Attack by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-atk-2', '特攻强化２',
        'Raises Special Attack by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的特攻。
离场后，效果便会消失。', null),
       (636, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Defense by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-def-2', '特防强化２',
        'Raises Special Defense by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的特防。
离场后，效果便会消失。', null),
       (637, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Defense by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-defense-2', '防御强化２',
        'Raises Defense by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的防御。
离场后，效果便会消失。', null),
       (638, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Attack by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-attack-2', '力量强化２',
        'Raises Attack by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的攻击。
离场后，效果便会消失。', null),
       (639, 0, 'Used on a party Pokémon in battle
:   Raises the target’s accuracy by two stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-accuracy-2', '命中强化２',
        'Raises accuracy by two stages in battle.  Wonder Launcher only.', '能相对提高战斗中
宝可梦的命中。
离场后，效果便会消失。', null),
       (640, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Speed by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-speed-3', '速度强化３',
        'Raises Speed by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的速度。
离场后，效果便会消失。', null),
       (641, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Attack by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-atk-3', '特攻强化３',
        'Raises Special Attack by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的特攻。
离场后，效果便会消失。', null),
       (642, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Defense by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-def-3', '特防强化３',
        'Raises Special Defense by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的特防。
离场后，效果便会消失。', null),
       (643, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Defense by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-defense-3', '防御强化３',
        'Raises Defense by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的防御。
离场后，效果便会消失。', null),
       (644, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Attack by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-attack-3', '力量强化３',
        'Raises Attack by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的攻击。
离场后，效果便会消失。', null),
       (645, 0, 'Used on a party Pokémon in battle
:   Raises the target’s accuracy by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-accuracy-3', '命中强化３',
        'Raises accuracy by three stages in battle.  Wonder Launcher only.', '能大量提高战斗中
宝可梦的命中。
离场后，效果便会消失。', null),
       (646, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Speed by six stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-speed-6', '速度强化６',
        'Raises Speed by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的速度。
离场后，效果便会消失。', null),
       (647, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Attack by six stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-atk-6', '特攻强化６',
        'Raises Special Attack by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的特攻。
离场后，效果便会消失。', null),
       (648, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Special Defense by six stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-sp-def-6', '特防强化６',
        'Raises Special Defense by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的特防。
离场后，效果便会消失。', null),
       (649, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Defense by six stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'x-defense-6', '防御强化６',
        'Raises Defense by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的防御。
离场后，效果便会消失。', null),
       (650, 0, 'Used on a party Pokémon in battle
:   Raises the target’s Attack by six stages.

This item can only be obtained or used via the Wonder Launcher.', null, 'x-attack-6', '力量强化６',
        'Raises Attack by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的攻击。
离场后，效果便会消失。', null),
       (651, 0, 'Used on a party Pokémon in battle
:   Raises the target’s accuracy by six stages.

This item can only be obtained or used via the Wonder Launcher.', null, 'x-accuracy-6', '命中强化６',
        'Raises accuracy by six stages in battle.  Wonder Launcher only.', '能极大提高战斗中
宝可梦的命中。
离场后，效果便会消失。', null),
       (652, 0, 'Used on a party Pokémon in battle
:   Selects another friendly Pokémon at random.  If that Pokémon’s ability is normally activated by some condition—i.e., is not continuous and passive—its effect is forcibly activated.

This item can only be obtained or used via the Wonder Launcher.', null, 'ability-urge', '？？？',
        'Forcibly activates a friendly Pokémon’s ability.', '使用后，能令我方
宝可梦的特性启动。', null),
       (653, 0, 'Used on a party Pokémon in battle
:   Selects another friendly Pokémon at random.  If that Pokémon is holding an item, that item is removed for the duration of the battle.

This item can only be obtained or used via the Wonder Launcher.', null, 'item-drop', '？？？',
        'Forces a friendly Pokémon to drop its held item.', '使用后，能立刻丢弃
我方宝可梦携带的道具。', null),
       (654, 0, 'Used on a party Pokémon in battle
:   Selects another friendly Pokémon at random.  If that Pokémon is holding an item normally activated by some condition—i.e., not continuous and passive—its effect is forcibly activated.

This item can only be obtained or used via the Wonder Launcher.', null, 'item-urge', '？？？',
        'Forcibly activates a friendly Pokémon’s held item.', '使用后，能立刻使用
我方宝可梦携带的道具。', null),
       (655, 0, 'Used on a party Pokémon in battle
:   Selects another friendly Pokémon at random.  Removes all of that Pokémon’s stat changes.

This item can only be obtained or used via the Wonder Launcher.', null, 'reset-urge', '？？？',
        'Resets a friendly Pokémon’s stat changes.', '使用后，我方宝可梦的
能力变化将消失。', null),
       (656, 0, 'Used on a party Pokémon in battle
:   Raises the target’s critical hit rate by three stages.
This item can only be obtained or used via the Wonder Launcher.', null, 'dire-hit-3', '要害攻击３',
        'Raises critical hit rate by three stages in battle.  Wonder Launcher only.', '击中要害的几率会大幅提高。
每次使用效果都会提升。
离场后，效果便会消失。', null),
       (657, 0, 'Summons reshiram for the final battle against N.', null, 'light-stone', '光明石',
        'Summons Reshiram for the final battle against N.', '莱希拉姆的肉体
毁灭后变成的样子。
据说它在等待着英雄的到来。', null),
       (658, 0, 'Summons zekrom for the final battle against N.', null, 'dark-stone', '黑暗石',
        'Summons Zekrom for the final battle against N.', '捷克罗姆的肉体
毁灭后变成的样子。
据说它在等待着英雄的到来。', null),
       (659, 1000, 'Teaches wild charge to a compatible Pokémon.', null, 'tm93', '招式学习器９３',
        'Teaches Wild Charge to a compatible Pokémon.', '让电流覆盖全身
撞向对手进行攻击。
自己也会受到少许伤害。', null),
       (660, 10000, 'Teaches rock smash to a compatible Pokémon.', null, 'tm94', '招式学习器９４',
        'Teaches Rock Smash to a compatible Pokémon.', '利用大浪
攻击自己周围所有的宝可梦。', null),
       (661, 1000, 'Teaches snarl to a compatible Pokémon.', null, 'tm95', '招式学习器９５',
        'Teaches Snarl to a compatible Pokémon.', '没完没了地大声斥责，
从而降低对手的特攻。', null),
       (662, 0,
        'Makes four-way video calls.  Used for plot advancement in-game, but also works with other players via the C-Gear.',
        null, 'xtransceiver', '即时通讯器', 'Makes four-way video calls.', '带摄像头功能，
最多能让４人进行通话的
最新型对讲机。', null),
       (663, 0, 'Unknown.  Currently unused.', null, 'god-stone', 'god stone', 'Unknown.  Currently unused.',
        'A rare stone.', null),
       (664, 0, 'Give to the wingull on unova route 13, along with gram 2 and gram 3, to receive tm89.', null, 'gram-1',
        '配送物品１', 'Part of a sidequest to obtain tm89.', '长翅鸥投递的
重要信件。', null),
       (665, 0, 'Give to the wingull on unova route 13, along with gram 1 and gram 3, to receive tm89.', null, 'gram-2',
        '配送物品２', 'Part of a sidequest to obtain tm89.', '长翅鸥投递的
重要信件。', null),
       (666, 0, 'Give to the wingull on unova route 13, along with gram 1 and gram 2, to receive tm89.', null, 'gram-3',
        '配送物品３', 'Part of a sidequest to obtain tm89.', '长翅鸥投递的
重要信件。', null),
       (668, 200, 'Held
:   When the holder uses a damaging dragon-type move, the move has 1.5× power and this item is consumed.', null,
        'dragon-gem', '龙之宝石',
        'Held: When the holder uses a damaging dragon-type move, the move has 1.5× power and this item is consumed.', '龙属性的宝石。
携带后，龙属性的
招式威力仅会增强１次。', null),
       (669, 4000, 'Held
:   When the holder uses a damaging normal-type move, the move has 1.5× power and this item is consumed.', null,
        'normal-gem', '一般宝石',
        'Held: When the holder uses a damaging normal-type move, the move has 1.5× power and this item is consumed.', '一般属性的宝石。
携带后，一般属性的
招式威力仅会增强１次。', null),
       (670, 0, '', null, 'medal-box', '奖牌盒', 'Holds medals recieved in the medal rally.', '可以放入获得的奖牌，
并记录奖牌信息的箱形机器。', null),
       (671, 0, '', null, 'dna-splicers', '基因之楔',
        'Fuses Kyurem with Reshiram or Zekrom, or splits them apart again.', '据说是能让原本为一体的
酋雷姆和某宝可梦
合体的一对楔子。', null),
       (673, 0, '', null, 'permit', '许可证', 'Grants access to the Nature Preserve.', '只有极少数人才知道的，
进入自然保护区所需的授权卡。', null),
       (674, 0, '', null, 'oval-charm', '圆形护符',
        'Doubles the chance of two Pokémon producing an egg at the daycare every 255 steps.', '拥有它之后，在寄放屋里
会更容易找到蛋的
神奇浑圆护符。', null),
       (1027, 600, null, 30, 'lone-earring', '一边的耳环', null, '某人遗失的单只耳环。', null),
       (677, 0, '', null, 'grubby-hanky', '脏手帕',
        'Appears in the Café Warehouse on Sunday; return to the customer with a Patrat on Thursday.', '七宝市仓库咖啡馆的
熟客遗失的手帕。
隐约有着宝可梦的香气。', null),
       (678, 0, '', null, 'colress-machine', '阿克罗玛机器', 'Wakes up the Crustle blocking the way in Seaside Cave.', '能强行让宝可梦的能力
觉醒的特殊装置。
但由于是试制品，功能尚未完善。', null),
       (679, 0, '', null, 'dropped-item', '遗忘物', 'Returned to Curtis or Yancy as part of a sidequest.', '在雷文市的游乐园里
捡到的即时通讯器。
失主好像是个男孩子。', null),
       (681, 0, '', null, 'reveal-glass', '现形镜',
        'Switches Tornadus, Thundurus, and Landorus between Incarnate and Therian Forme.', '能够通过照出真实，
让宝可梦变回原来
样子的神奇镜子。', null),
       (682, 1000,
        'Held: When the holder is hit by a super effective move, its Attack and Special Attack raise by two stages.',
        80, 'weakness-policy', '弱点保险',
        'Held: When the holder is hit by a super effective move, its Attack and Special Attack raise by two stages.', '被针对弱点时，
攻击和特攻就会大幅提高。', null),
       (683, 1000, 'Raises the holder’s Special Defense to 1.5×.  Prevents the holder from selecting a status move.',
        80, 'assault-vest', '突击背心',
        'Raises the holder’s Special Defense to 1.5×.  Prevents the holder from selecting a status move.', '会变得富有攻击性的背心。
虽然携带后特防会提高，
但会无法使出变化招式。', null),
       (684, 1000,
        'Held: Fairy-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Fairy.', 90,
        'pixie-plate', '妖精石板',
        'Held: Fairy-Type moves from holder do 20% more damage. Changes Arceus’s and Judgment’s type to Fairy.', '妖精属性的石板。
携带后，妖精属性的
招式威力就会增强。', null),
       (685, 10000, 'Switches a Pokémon between its two possible (non-Hidden) Abilities.', null, 'ability-capsule',
        '特性胶囊', 'Switches a Pokémon between its two possible (non-Hidden) Abilities.', '如果用于有着２种特性的宝可梦，
就能令其现有特性变为另一种的胶囊。', null),
       (686, 2000, 'Traded on a Swirlix: Holder evolves into Slurpuff.', 80, 'whipped-dream', '泡沫奶油',
        'Traded on a Swirlix: Holder evolves into Slurpuff.', '松松软软起着泡的，
稍微有点甜的奶油。
某种宝可梦很喜欢它。', null),
       (687, 2000, 'Traded on a Spritzee: Holder evolves into Aromatisse.', 80, 'sachet', '香袋',
        'Traded on a Spritzee: Holder evolves into Aromatisse.', '装着散发微浓香气的
香料的袋子。
某种宝可梦很喜欢它。', null),
       (688, 4000,
        'Held: If the holder is hit by a damaging Water move, it consumes this item and raises its Special Defense by one stage.',
        30, 'luminous-moss', '光苔',
        'Held: If the holder is hit by a damaging Water move, raises its Special Defense by one stage.', '一次性使用的光苔。
携带它的宝可梦如果受到水属性
招式攻击，特防就会提高。', null),
       (689, 4000, 'Held: If the holder is hit by a damaging Ice move, raises its Attack by one stage.', 30, 'snowball',
        '雪球', 'Held: If the holder is hit by a damaging Ice move, raises its Attack by one stage.', '一次性使用的雪球。
携带它的宝可梦如果受到冰属性
招式攻击，攻击就会提高。', null),
       (690, 4000, 'Held: Prevents damage from powder moves and the damage from Hail and Sandstorm.', 80,
        'safety-goggles', '防尘护目镜',
        'Held: Prevents damage from powder moves and the damage from Hail and Sandstorm.', '不单是天气造成的伤害，
就连粉末类招式的效果
也能防御的护目镜。', null),
       (691, 200, 'Increases the total number of Berries by 2.', 30, 'rich-mulch', '硕果肥',
        'Increases the total number of Berries by 2.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (692, 200, 'Increases the chance of Berry mutation.', 30, 'surprise-mulch', '吃惊肥',
        'Increases the chance of Berry mutation.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (693, 200, 'Causes soil to dry out in 4 hours.', 30, 'boost-mulch', '劲劲肥',
        'Causes soil to dry out in 4 hours.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (694, 200,
        'Increases the total number of Berries by 2, increases the chance of Berry mutation, and causes soil to dry out in 4 hours.',
        30, 'amaze-mulch', '超效肥',
        'Increases the total number of Berries by 2, increases the chance of Berry mutation, and causes soil to dry out in 4 hours.', '培育树果时的肥料。
但完全不适合丰缘地区的土壤，
所以没什么效果。', null),
       (695, 0, 'Held: Allows Gengar to Mega Evolve into Mega Gengar.', 80, 'gengarite', '耿鬼进化石',
        'Held: Allows Gengar to Mega Evolve into Mega Gengar.', '让耿鬼携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (696, 0, 'Held: Allows Gardevoir to Mega Evolve into Mega Gardevoir.', 80, 'gardevoirite', '沙奈朵进化石',
        'Held: Allows Gardevoir to Mega Evolve into Mega Gardevoir.', '让沙奈朵携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (697, 0, 'Held: Allows Ampharos to Mega Evolve into Mega Ampharos.', 80, 'ampharosite', '电龙进化石',
        'Held: Allows Ampharos to Mega Evolve into Mega Ampharos.', '让电龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (698, 0, 'Held: Allows Venusaur to Mega Evolve into Mega Venusaur.', 80, 'venusaurite', '妙蛙花进化石',
        'Held: Allows Venusaur to Mega Evolve into Mega Venusaur.', '让妙蛙花携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (699, 0, 'Held: Allows Charizard to Mega Evolve into Mega Charizard X.', 80, 'charizardite-x', '喷火龙进化石Ｘ',
        'Held: Allows Charizard to Mega Evolve into Mega Charizard X.', '让喷火龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (700, 0, 'Held: Allows Blastoise to Mega Evolve into Mega Blastoise.', 80, 'blastoisinite', '水箭龟进化石',
        'Held: Allows Blastoise to Mega Evolve into Mega Blastoise.', '让水箭龟携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (701, 0, 'Held: Allows Mewtwo to Mega Evolve into Mega Mewtwo X.', 80, 'mewtwonite-x', '超梦进化石Ｘ',
        'Held: Allows Mewtwo to Mega Evolve into Mega Mewtwo X.', '让超梦携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (702, 0, 'Held: Allows Mewtwo to Mega Evolve into Mega Mewtwo Y.', 80, 'mewtwonite-y', '超梦进化石Ｙ',
        'Held: Allows Mewtwo to Mega Evolve into Mega Mewtwo Y.', '让超梦携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (703, 0, 'Held: Allows Blaziken to Mega Evolve into Mega Blaziken.', 80, 'blazikenite', '火焰鸡进化石',
        'Held: Allows Blaziken to Mega Evolve into Mega Blaziken.', '让火焰鸡携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (704, 0, 'Held: Allows Medicham to Mega Evolve into Mega Medicham.', 80, 'medichamite', '恰雷姆进化石',
        'Held: Allows Medicham to Mega Evolve into Mega Medicham.', '让恰雷姆携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (2215, 0, null, null, 'tm226', 'TM226', null, null, null),
       (705, 0, 'Held: Allows Houndoom to Mega Evolve into Mega Houndoom.', 80, 'houndoominite', '黑鲁加进化石',
        'Held: Allows Houndoom to Mega Evolve into Mega Houndoom.', '让黑鲁加携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (706, 0, 'Held: Allows Aggron to Mega Evolve into Mega Aggron.', 80, 'aggronite', '波士可多拉进化石',
        'Held: Allows Aggron to Mega Evolve into Mega Aggron.', '让波士可多拉携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (707, 0, 'Held: Allows Banette to Mega Evolve into Mega Banette.', 80, 'banettite', '诅咒娃娃进化石',
        'Held: Allows Banette to Mega Evolve into Mega Banette.', '让诅咒娃娃携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (708, 0, 'Held: Allows Tyranitar to Mega Evolve into Mega Tyranitar.', 80, 'tyranitarite', '班基拉斯进化石',
        'Held: Allows Tyranitar to Mega Evolve into Mega Tyranitar.', '让班基拉斯携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (709, 0, 'Held: Allows Scizor to Mega Evolve into Mega Scizor.', 80, 'scizorite', '巨钳螳螂进化石',
        'Held: Allows Scizor to Mega Evolve into Mega Scizor.', '让巨钳螳螂携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (710, 0, 'Held: Allows Pinsir to Mega Evolve into Mega Pinsir.', 80, 'pinsirite', '凯罗斯进化石',
        'Held: Allows Pinsir to Mega Evolve into Mega Pinsir.', '让凯罗斯携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (711, 0, 'Held: Allows Aerodactyl to Mega Evolve into Mega Aerodactyl.', 80, 'aerodactylite', '化石翼龙进化石',
        'Held: Allows Aerodactyl to Mega Evolve into Mega Aerodactyl.', '让化石翼龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (712, 0, 'Held: Allows Lucario to Mega Evolve into Mega Lucario.', 80, 'lucarionite', '路卡利欧进化石',
        'Held: Allows Lucario to Mega Evolve into Mega Lucario.', '让路卡利欧携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (713, 0, 'Held: Allows Abomasnow to Mega Evolve into Mega Abomasnow.', 80, 'abomasite', '暴雪王进化石',
        'Held: Allows Abomasnow to Mega Evolve into Mega Abomasnow.', '让暴雪王携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (714, 0, 'Held: Allows Kangaskhan to Mega Evolve into Mega Kangaskhan.', 80, 'kangaskhanite', '袋兽进化石',
        'Held: Allows Kangaskhan to Mega Evolve into Mega Kangaskhan.', '让袋兽携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (715, 0, 'Held: Allows Gyarados to Mega Evolve into Mega Gyarados.', 80, 'gyaradosite', '暴鲤龙进化石',
        'Held: Allows Gyarados to Mega Evolve into Mega Gyarados.', '让暴鲤龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (716, 0, 'Held: Allows Absol to Mega Evolve into Mega Absol.', 80, 'absolite', '阿勃梭鲁进化石',
        'Held: Allows Absol to Mega Evolve into Mega Absol.', '让阿勃梭鲁携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (717, 0, 'Held: Allows Charizard to Mega Evolve into Mega Charizard Y.', 80, 'charizardite-y', '喷火龙进化石Ｙ',
        'Held: Allows Charizard to Mega Evolve into Mega Charizard Y.', '让喷火龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (718, 0, 'Held: Allows Alakazam to Mega Evolve into Mega Alakazam.', 80, 'alakazite', '胡地进化石',
        'Held: Allows Alakazam to Mega Evolve into Mega Alakazam.', '能在战斗时让胡地
进行超级进化的
一种神奇超级石。', null),
       (719, 0, 'Held: Allows Heracross to Mega Evolve into Mega Heracross.', 80, 'heracronite', '赫拉克罗斯进化石',
        'Held: Allows Heracross to Mega Evolve into Mega Heracross.', '让赫拉克罗斯携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (720, 0, 'Held: Allows Mawile to Mega Evolve into Mega Mawile.', 80, 'mawilite', '大嘴娃进化石',
        'Held: Allows Mawile to Mega Evolve into Mega Mawile.', '让大嘴娃携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (721, 0, 'Held: Allows Manectric to Mega Evolve into Mega Manectric.', 80, 'manectite', '雷电兽进化石',
        'Held: Allows Manectric to Mega Evolve into Mega Manectric.', '让雷电兽携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (722, 0, 'Held: Allows Garchomp to Mega Evolve into Mega Garchomp.', 80, 'garchompite', '烈咬陆鲨进化石',
        'Held: Allows Garchomp to Mega Evolve into Mega Garchomp.', '让烈咬陆鲨携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (723, 80, 'Held: Consumed when struck by a super-effective Fairy-type attack to halve the damage.', 10,
        'roseli-berry', '洛玫果',
        'Held: Consumed when struck by a super-effective Fairy-type attack to halve the damage.', '让宝可梦携带后，
在受到效果绝佳的妖精属性招式
攻击时，能令其威力减弱。', null),
       (724, 80, 'Held: When the holder is hit by a physical move, increases its Defense by one stage.', 10,
        'kee-berry', '亚开果', 'Held: When the holder is hit by a physical move, increases its Defense by one stage.', '让宝可梦携带后，
在受到物理招式攻击时，
自己的防御就会提高。', 3),
       (725, 80, 'Held: When the holder is hit by a special move, increases its Special Defense by one stage.', 10,
        'maranga-berry', '香罗果',
        'Held: When the holder is hit by a special move, increases its Special Defense by one stage.', '让宝可梦携带后，
在受到特殊招式攻击时，
自己的特防就会提高。', 3),
       (726, 20, 'Can be used at a boutique for a 50% discount.  Consumed once used.', 10, 'discount-coupon', '折价券',
        'Can be used at a boutique for a 50% discount.  Consumed once used.', '在时装店买道具时，
能以比平时更低的
价格购买的券。', null),
       (727, 3000, 'Vendor trash.', 30, 'strange-souvenir', '神秘摆设', 'Vendor trash.', '在阿罗拉地区，据说是模仿
古时候被称为守护神的
神奇宝可梦而制作的摆设。', null),
       (728, 350, 'Cures all major status ailments and confusion.', 30, 'lumiose-galette', '密阿雷格雷派饼',
        'Cures all major status ailments and confusion.', '在密阿雷市很受欢迎的点心。
能治愈１只宝可梦的
所有异常状态。', null),
       (729, 7000, 'Can be revived into a Tyrunt.', 100, 'jaw-fossil', '颚之化石', 'Can be revived into a Tyrunt.', '很久以前生活在地上的
古代宝可梦的化石。
好像是很大的颚的一部分。', null),
       (730, 7000, 'Can be revived into an Amaura.', 100, 'sail-fossil', '鳍之化石', 'Can be revived into an Amaura.', '很久以前生活在地上的
古代宝可梦的化石。
好像是头鳍的一部分。', null),
       (731, 200,
        'Held: When the holder uses a damaging Fairy move, the move has 1.5× power and this item is consumed.', null,
        'fairy-gem', '妖精宝石',
        'Held: When the holder uses a damaging Fairy move, the move has 1.5× power and this item is consumed.', '妖精属性的宝石。
携带后，妖精属性的
招式威力会仅增强１次。', null),
       (1028, 800, null, 30, 'beach-glass', '海边的玻璃', null, '受到波浪的洗刷从而
变成圆形的彩色玻璃片。
表面摸起来有点粗糙不平。', null),
       (732, 0, 'Contains basic gameplay information.', null, 'adventure-rules', '探险心得',
        'Contains basic gameplay information.', '朋友亲手制作的指南手册。
里面汇总了训练家在旅行时
所需注意的各种事项。', null),
       (733, 0, 'Unlocks the elevator in Lysandre Labs.', null, 'elevator-key', '电梯钥匙',
        'Unlocks the elevator in Lysandre Labs.', '可以启动弗拉达利
研究所里电梯的钥匙卡。
上面有着闪焰队的标志。', null),
       (734, 0, 'Displays cutscene conversations as the plot advances.', null, 'holo-caster', '全息影像通讯器',
        'Displays cutscene conversations as the plot advances.', '可以随时查看接收到的
全息影像数据的装置。', null),
       (735, 0, 'Does nothing, but signifies becoming Champion.', null, 'honor-of-kalos', '卡洛斯勋章',
        'Does nothing, but signifies becoming Champion.', '颁发给为卡洛斯地区
作出杰出贡献的人的宝贵勋章。', null),
       (736, 0, 'Trade for a Sun Stone in X and Y, or Pidgeotite in Omega Ruby and Alpha Sapphire.', null,
        'intriguing-stone', '似珍石',
        'Trade for a Sun Stone in X and Y, or Pidgeotite in Omega Ruby and Alpha Sapphire.', '在有些人看来
可能会觉得它十分贵重。
非常与众不同的石头。', null),
       (737, 0, 'Allows the player to change their eye color.', null, 'lens-case', '隐形眼镜盒',
        'Allows the player to change their eye color.', '可以存放隐形眼镜的，
有点漂亮的盒子。', null),
       (738, 0, 'Advances the Looker postgame plot.', null, 'looker-ticket', '帅哥券',
        'Advances the Looker postgame plot.', '喷有闪闪发光的涂料，
帅哥亲手制作的券。', null),
       (739, 0, 'Allows the player’s Pokémon to Mega Evolve.', null, 'mega-ring', '超级环',
        'Allows the player’s Pokémon to Mega Evolve.', '蕴藏着未知力量的圆环。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (740, 0, 'Permits entry to the Kalos Power Plant.', null, 'power-plant-pass', '发电厂通行证',
        'Permits entry to the Kalos Power Plant.', '该通行证可用作进入
１３号道路上的发电厂时
所需的身份证明。', null),
       (741, 0, 'Traded to the player’s mom for the Town Map and a Potion.', null, 'profs-letter', '博士的信',
        'Traded to the player’s mom for the Town Map and a Potion.', '布拉塔诺博士写给妈妈的信。
上面带有微微的香气。', null),
       (742, 0, 'Allows the player to move quickly and off the grid.', null, 'roller-skates', '轮滑鞋',
        'Allows the player to move quickly and off the grid.', '鞋底下装着滑轮。
能在地面上滑行，
或漂亮地表演特技。', null),
       (743, 0, 'Waters Berry plants.', null, 'sprinklotad', '莲叶童子喷壶', 'Waters Berry plants.', '浇水的道具。
能让埋在松软土壤里的
树果快快长大。', null),
       (744, 0, 'Permits access to Kiloude City.', null, 'tmv-pass', 'ＴＭＶ自由票', 'Permits access to Kiloude City.', '可以随时乘坐连接着
密阿雷市和奇楠市的
超高速列车的车票。', null),
       (745, 1000, 'Teaches a Pokémon TM96.', null, 'tm96', '招式学习器９６', 'Teaches a Pokémon TM96.', '用自然之力进行攻击。
根据所使用场所的不同，
使出的招式也会有所变化。', null),
       (746, 1000, 'Teaches a Pokémon TM97.', null, 'tm97', '招式学习器９７', 'Teaches a Pokémon TM97.', '从体内发出
充满恶意的恐怖气场。
有时会使对手畏缩。', null),
       (747, 1000, 'Teaches a Pokémon TM98.', null, 'tm98', '招式学习器９８', 'Teaches a Pokémon TM98.', '以惊人的气势扑向对手。
有时会使对手畏缩。', null),
       (748, 1000, 'Teaches a Pokémon TM99.', null, 'tm99', '招式学习器９９', 'Teaches a Pokémon TM99.', '向对手发出强光，
并给予伤害。', null),
       (749, 5000, 'Teaches a Pokémon TM100.', null, 'tm100', '招式学习器１００', 'Teaches a Pokémon TM100.', '和对手进行密语，
使其失去集中力，
从而降低对手的特攻。', null),
       (760, 0, 'Held: Allows Latias to Mega Evolve into Mega Latias.', 80, 'latiasite', '拉帝亚斯进化石',
        'Held: Allows Latias to Mega Evolve into Mega Latias.', '让拉帝亚斯携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (761, 0, 'Held: Allows Latios to Mega Evolve into Mega Latios.', 80, 'latiosite', '拉帝欧斯进化石',
        'Held: Allows Latios to Mega Evolve into Mega Latios.', '让拉帝欧斯携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (762, 0, 'Unknown.', null, 'common-stone', '常见石', 'Unknown.', '在有些人看来
可能会觉得它十分贵重。
但其实是块普通的石头。', null),
       (763, 0, 'Allows the player to change their lipstick color.', null, 'makeup-bag', '化妆包',
        'Allows the player to change their lipstick color.', '可以存放唇膏的精美小包包。', null),
       (764, 0, 'Unobtainable, but allows the player to change clothes anywhere.', null, 'travel-trunk', '衣物箱',
        'Unobtainable, but allows the player to change clothes anywhere.', '能塞入任意时尚物品，
收纳能力极佳的轻箱子。', null),
       (765, 350, 'Cures any major status ailment and confusion.', 30, 'shalour-sable', '娑罗沙布蕾',
        'Cures any major status ailment and confusion.', '娑罗市特产的沙布蕾。
能治愈１只宝可梦的
所有异常状态。', null),
       (768, 0,
        'Unused.  This appears as the girlplayer’s Mega Bracelet in Pokémon Contests, but it cannot actually be obtained.',
        null, 'mega-charm', '超级坠饰', 'Unused Key Stone.', '蕴藏着未知力量的坠饰。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (769, 0, 'Unused.  This is Korrina’s Key Stone in X and Y, but it cannot be obtained by the player.', null,
        'mega-glove', '超级手套', 'Unused NPC Key Stone.', '蕴藏着未知力量的手套。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (770, 0, 'Allows Captain Stern to set out on his expedition.', null, 'devon-parts', '得文的物品',
        'Allows Captain Stern to set out on his expedition.', '此物品里面放着的是
得文制造的某种零件。', null),
       (772, 0, 'Creates and stores Pokéblocks.', null, 'pokeblock-kit', '宝可方块套装',
        'Creates and stores Pokéblocks.', '这是树果混合器和宝可方块盒的套装。
树果混合器可利用树果制作宝可方块，
而做好的宝可方块可用宝可方块盒来保存。', null),
       (773, 0, 'Unlocks the door to Room 1 in Sea Mauville.', null, 'key-to-room-1', '１号客房的钥匙',
        'Unlocks the door to Room 1 in Sea Mauville.', '进入海紫堇的房间时
所需的钥匙。', null),
       (774, 0, 'Unlocks the door to Room 2 in Sea Mauville.', null, 'key-to-room-2', '２号客房的钥匙',
        'Unlocks the door to Room 2 in Sea Mauville.', '进入海紫堇的房间时
所需的钥匙。', null),
       (775, 0, 'Unlocks the door to Room 4 in Sea Mauville.', null, 'key-to-room-4', '４号客房的钥匙',
        'Unlocks the door to Room 4 in Sea Mauville.', '进入海紫堇的房间时
所需的钥匙。', null),
       (776, 0, 'Unlocks the door to Room 6 in Sea Mauville.', null, 'key-to-room-6', '６号客房的钥匙',
        'Unlocks the door to Room 6 in Sea Mauville.', '进入海紫堇的房间时
所需的钥匙。', null),
       (779, 0, 'Worn by the player while underwater via Dive in Omega Ruby and Alpha Sapphire.', null,
        'devon-scuba-gear', '得文潜水装备', 'Worn by the player while underwater.', '潜水时装备在身上
用来呼吸氧气的面罩。
得文制造。', null),
       (1687, 500, null, null, 'flying-tera-shard', '飞行太晶碎块', null, null, null),
       (780, 0, 'Worn during Pokémon Contests.', null, 'contest-costume--jacket', '演出礼服',
        'Worn during Pokémon Contests.', '在华丽大赛上演出时穿的
非常帅气的礼服。', null),
       (782, 0, 'Allows the player to ride Groudon in the Cave of Origin.', null, 'magma-suit', '熔岩装',
        'Allows the player to ride Groudon in the Cave of Origin.', '集熔岩队科学技术于一身的服装。
能承受任何冲击。', null),
       (783, 0, 'Allows the player to ride Kyogre in the Cave of Origin.', null, 'aqua-suit', '海洋装',
        'Allows the player to ride Kyogre in the Cave of Origin.', '集海洋队科学技术于一身的服装。
能承受任何冲击。', null),
       (784, 0, 'Allows the player and their mother to see the star show in the Mossdeep Space Center.', null,
        'pair-of-tickets', '双人票',
        'Allows the player and their mother to see the star show in the Mossdeep Space Center.', '在绿岭宇宙中心举办的
天体展览的双人票。', null),
       (785, 0, 'Allows the player’s Pokémon to Mega Evolve.', null, 'mega-bracelet', '超级手镯',
        'Allows the player’s Pokémon to Mega Evolve.', '蕴藏着未知力量的手镯。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (786, 0,
        'Unused.  This is Wally’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-pendant', '超级吊坠', 'Unused NPC Key Stone.', '蕴藏着未知力量的吊坠。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (787, 0,
        'Unused.  This is Maxie’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-glasses', '超级眼镜', 'Unused NPC Key Stone.', '蕴藏着未知力量的眼镜。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (788, 0,
        'Unused.  This is Archie’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-anchor', '超级船锚', 'Unused NPC Key Stone.', '蕴藏着未知力量的船锚。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (789, 0,
        'Unused.  This is Steven’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-stickpin', '超级领针', 'Unused NPC Key Stone.', '蕴藏着未知力量的领针。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (790, 0,
        'Unused.  This is Lisia’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-tiara', '超级头冠', 'Unused NPC Key Stone.', '蕴藏着未知力量的头冠。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (791, 0,
        'Unused.  This is Zinnia’s Key Stone in Omega Ruby and Alpha Sapphire, but it cannot be obtained by the player.',
        null, 'mega-anklet', '超级脚镯', 'Unused NPC Key Stone.', '蕴藏着未知力量的脚镯。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (793, 0, 'Held: Allows Swampert to Mega Evolve into Mega Swampert.', 80, 'swampertite', '巨沼怪进化石',
        'Held: Allows Swampert to Mega Evolve into Mega Swampert.', '让巨沼怪携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (794, 0, 'Held: Allows Sceptile to Mega Evolve into Mega Sceptile.', 80, 'sceptilite', '蜥蜴王进化石',
        'Held: Allows Sceptile to Mega Evolve into Mega Sceptile.', '让蜥蜴王携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (795, 0, 'Held: Allows Sableye to Mega Evolve into Mega Sableye.', 80, 'sablenite', '勾魂眼进化石',
        'Held: Allows Sableye to Mega Evolve into Mega Sableye.', '让勾魂眼携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (796, 0, 'Held: Allows Altaria to Mega Evolve into Mega Altaria.', 80, 'altarianite', '七夕青鸟进化石',
        'Held: Allows Altaria to Mega Evolve into Mega Altaria.', '让七夕青鸟携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (797, 0, 'Held: Allows Gallade to Mega Evolve into Mega Gallade.', 80, 'galladite', '艾路雷朵进化石',
        'Held: Allows Gallade to Mega Evolve into Mega Gallade.', '让艾路雷朵携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (798, 0, 'Held: Allows Audino to Mega Evolve into Mega Audino.', 80, 'audinite', '差不多娃娃进化石',
        'Held: Allows Audino to Mega Evolve into Mega Audino.', '让差不多娃娃携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (799, 0, 'Held: Allows Metagross to Mega Evolve into Mega Metagross.', 80, 'metagrossite', '巨金怪进化石',
        'Held: Allows Metagross to Mega Evolve into Mega Metagross.', '让巨金怪携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (800, 0, 'Held: Allows Sharpedo to Mega Evolve into Mega Sharpedo.', 80, 'sharpedonite', '巨牙鲨进化石',
        'Held: Allows Sharpedo to Mega Evolve into Mega Sharpedo.', '让巨牙鲨携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (801, 0, 'Held: Allows Slowbro to Mega Evolve into Mega Slowbro.', 80, 'slowbronite', '呆壳兽进化石',
        'Held: Allows Slowbro to Mega Evolve into Mega Slowbro.', '让呆壳兽携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (802, 0, 'Held: Allows Steelix to Mega Evolve into Mega Steelix.', 80, 'steelixite', '大钢蛇进化石',
        'Held: Allows Steelix to Mega Evolve into Mega Steelix.', '让大钢蛇携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (803, 0, 'Held: Allows Pidgeot to Mega Evolve into Mega Pidgeot.', 80, 'pidgeotite', '大比鸟进化石',
        'Held: Allows Pidgeot to Mega Evolve into Mega Pidgeot.', '让大比鸟携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (804, 0, 'Held: Allows Glalie to Mega Evolve into Mega Glalie.', 80, 'glalitite', '冰鬼护进化石',
        'Held: Allows Glalie to Mega Evolve into Mega Glalie.', '让冰鬼护携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (805, 0, 'Held: Allows Diancie to Mega Evolve into Mega Diancie.', 80, 'diancite', '蒂安希进化石',
        'Held: Allows Diancie to Mega Evolve into Mega Diancie.', '让蒂安希携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (806, 0, 'Transforms Hoopa into its Unbound form for up to three days.', null, 'prison-bottle', '惩戒之壶',
        'Transforms Hoopa into its Unbound form for up to three days.', '据说在很久以前封印着
某只宝可梦力量的壶。', null),
       (807, 0,
        'Unused.  This appears as the boy player’s Mega Bracelet in Pokémon Contests, but it cannot actually be obtained.',
        null, 'mega-cuff', '超级护腕', 'Unused Key Stone.', '蕴藏着未知力量的护腕。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (808, 0, 'Held: Allows Camerupt to Mega Evolve into Mega Camerupt.', 80, 'cameruptite', '喷火驼进化石',
        'Held: Allows Camerupt to Mega Evolve into Mega Camerupt.', '喷火驼携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (809, 0, 'Held: Allows Lopunny to Mega Evolve into Mega Lopunny.', 80, 'lopunnite', '长耳兔进化石',
        'Held: Allows Lopunny to Mega Evolve into Mega Lopunny.', '长耳兔携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (1688, 500, null, null, 'psychic-tera-shard', '超能力太晶碎块', null, null, null),
       (810, 0, 'Held: Allows Salamence to Mega Evolve into Mega Salamence.', 80, 'salamencite', '暴飞龙进化石',
        'Held: Allows Salamence to Mega Evolve into Mega Salamence.', '暴飞龙携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (811, 0, 'Held: Allows Beedrill to Mega Evolve into Mega Beedrill.', 80, 'beedrillite', '大针蜂进化石',
        'Held: Allows Beedrill to Mega Evolve into Mega Beedrill.', '大针蜂携带后，
在战斗时就能进行超级进化的
一种神奇超级石。', null),
       (814, 0, 'Allows the player’s Pokémon to Mega Evolve.', null, 'key-stone', '钥石',
        'Allows the player’s Pokémon to Mega Evolve.', '蕴藏着未知力量的石头。
能让携带着超级石战斗的
宝可梦进行超级进化。', null),
       (815, 0, 'Causes the Meteorite to transform to its final form, allowing Rayquaza to Mega Evolve.', null,
        'meteorite-shard', '陨石碎片',
        'Causes the Meteorite to transform to its final form, allowing Rayquaza to Mega Evolve.', '掉落在石之洞窟里的
陨石碎片之一。
摸上去有一点点暖暖的。', null),
       (816, 0, 'Summons Latias or Latios for a ride.', null, 'eon-flute', '无限之笛',
        'Summons Latias or Latios for a ride.', '无论身在何处，
都能召唤拉帝欧斯或
拉帝亚斯的笛子。', null),
       (817, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Normal moves.', null, 'normalium-z--held',
        '一般Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Normal moves.', 'Ｚ力量的结晶。
会将一般属性的招式
升级成Ｚ招式。', null),
       (818, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Fire moves.', null, 'firium-z--held',
        '火Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Fire moves.', 'Ｚ力量的结晶。
会将火属性的招式
升级成Ｚ招式。', null),
       (819, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Water moves.', null, 'waterium-z--held',
        '水Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Water moves.', 'Ｚ力量的结晶。
会将水属性的招式
升级成Ｚ招式。', null),
       (820, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Electric moves.', null,
        'electrium-z--held', '电Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Electric moves.', 'Ｚ力量的结晶。
会将电属性的招式
升级成Ｚ招式。', null),
       (821, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Grass moves.', null, 'grassium-z--held',
        '草Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Grass moves.', 'Ｚ力量的结晶。
会将草属性的招式
升级成Ｚ招式。', null),
       (822, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Ice moves.', null, 'icium-z--held', '冰Ｚ',
        'Held: Allows a Pokémon to use the Z-move equivalents of its Ice moves.', 'Ｚ力量的结晶。
会将冰属性的招式
升级成Ｚ招式。', null),
       (823, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Fighting moves.', null,
        'fightinium-z--held', '格斗Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Fighting moves.', 'Ｚ力量的结晶。
会将格斗属性的招式
升级成Ｚ招式。', null),
       (824, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Poison moves.', null, 'poisonium-z--held',
        '毒Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Poison moves.', 'Ｚ力量的结晶。
会将毒属性的招式
升级成Ｚ招式。', null),
       (825, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Ground moves.', null, 'groundium-z--held',
        '地面Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Ground moves.', 'Ｚ力量的结晶。
会将地面属性的招式
升级成Ｚ招式。', null),
       (826, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Flying moves.', null, 'flyinium-z--held',
        '飞行Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Flying moves.', 'Ｚ力量的结晶。
会将飞行属性的招式
升级成Ｚ招式。', null),
       (827, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Psychic moves.', null, 'psychium-z--held',
        '超能力Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Psychic moves.', 'Ｚ力量的结晶。
会将超能力属性的招式
升级成Ｚ招式。', null),
       (828, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Bug moves.', null, 'buginium-z--held',
        '虫Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Bug moves.', 'Ｚ力量的结晶。
会将虫属性的招式
升级成Ｚ招式。', null),
       (829, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Rock moves.', null, 'rockium-z--held',
        '岩石Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Rock moves.', 'Ｚ力量的结晶。
会将岩石属性的招式
升级成Ｚ招式。', null),
       (830, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Ghost moves.', null, 'ghostium-z--held',
        '幽灵Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Ghost moves.', 'Ｚ力量的结晶。
会将幽灵属性的招式
升级成Ｚ招式。', null),
       (831, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Dragon moves.', null, 'dragonium-z--held',
        '龙Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Dragon moves.', 'Ｚ力量的结晶。
会将龙属性的招式
升级成Ｚ招式。', null),
       (832, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Dark moves.', null, 'darkinium-z--held',
        '恶Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Dark moves.', 'Ｚ力量的结晶。
会将恶属性的招式
升级成Ｚ招式。', null),
       (833, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Steel moves.', null, 'steelium-z--held',
        '钢Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Steel moves.', 'Ｚ力量的结晶。
会将钢属性的招式
升级成Ｚ招式。', null),
       (834, 0, 'Held: Allows a Pokémon to use the Z-move equivalents of its Fairy moves.', null, 'fairium-z--held',
        '妖精Ｚ', 'Held: Allows a Pokémon to use the Z-move equivalents of its Fairy moves.', 'Ｚ力量的结晶。
会将妖精属性的招式
升级成Ｚ招式。', null),
       (835, 0, 'Held: Allows pikachu to upgrade volt tackle into catastropika.', null, 'pikanium-z--held', '皮卡丘Ｚ',
        'Held: Allows Pikachu to upgrade Volt Tackle into Catastropika.', 'Ｚ力量的结晶。
会将皮卡丘的伏特攻击
升级成Ｚ招式。', null),
       (836, 5000, 'Trade to Mr. Hyper to set one of a Pokémon’s genes to 31.', 30, 'bottle-cap', '银色王冠',
        'Trade to Mr. Hyper to maximize one of a Pokémon’s genes.', '银色的美丽王冠。
有些人收到它会很高兴。', null),
       (837, 10000, 'Trade to Mr. Hyper to set all of a Pokémon’s genes to 31.', 30, 'gold-bottle-cap', '金色王冠',
        'Trade to Mr. Hyper to maximize all of a Pokémon’s genes.', '金色的美丽王冠。
比银色王冠珍贵。
有些人收到它会很高兴。', null),
       (838, 0, 'Allows the player’s Pokémon to use Z-moves.', null, 'z-ring', 'Ｚ手环',
        'Allows the player’s Pokémon to use Z-moves.', '通过使用训练家的
气力和体力来让宝可梦
释放出Ｚ力量的神奇手环。', null),
       (839, 0, 'Held: Allows decidueye to upgrade spirit shackle into sinister arrow raid.', null, 'decidium-z--held',
        '狙射树枭Ｚ', 'Held: Allows Decidueye to upgrade Spirit Shackle into Sinister Arrow Raid.', 'Ｚ力量的结晶。
会将狙射树枭的缝影
升级成Ｚ招式。', null),
       (840, 0, 'Held: Allows incineroar to upgrade darkest lariat into malicious moonsault.', null, 'incinium-z--held',
        '炽焰咆哮虎Ｚ', 'Held: Allows Incineroar to upgrade Darkest Lariat into Malicious Moonsault.', 'Ｚ力量的结晶。
会将炽焰咆哮虎的ＤＤ金勾臂
升级成Ｚ招式。', null),
       (841, 0, 'Held: Allows primarina to upgrade sparkling aria into oceanic operetta.', null, 'primarium-z--held',
        '西狮海壬Ｚ', 'Held: Allows Primarina to upgrade Sparkling Aria into Oceanic Operetta.', 'Ｚ力量的结晶。
会将西狮海壬的泡影的咏叹调
升级成Ｚ招式。', null),
       (842, 0,
        'Held: Allows tapu koko, tapu lele, tapu bulu, and tapu fini to upgrade natures madness into guardian of alola.',
        null, 'tapunium-z--held', '卡璞Ｚ', 'Held: Allows the Tapus to upgrade Nature’s Madness into Guardian of Alola.', 'Ｚ力量的结晶。
会将卡璞们的自然之怒
升级成Ｚ招式。', null),
       (843, 0, 'Held: Allows marshadow to upgrade spectral thief into soul stealing 7 star strike.', null,
        'marshadium-z--held', '玛夏多Ｚ',
        'Held: Allows Marshadow to upgrade Spectral Thief into Soul-Stealing 7 Star Strike.', 'Ｚ力量的结晶。
会将玛夏多的暗影偷盗
升级成Ｚ招式。', null),
       (844, 0, 'Held: Allows Alola raichu to upgrade thunderbolt into stoked sparksurfer.', null,
        'aloraichium-z--held', '阿罗雷Ｚ', 'Held: Allows Alola Raichu to upgrade Thunderbolt into Stoked Sparksurfer.', 'Ｚ力量的结晶。
会将阿罗拉地区雷丘的
十万伏特升级成Ｚ招式。', null),
       (845, 0, 'Held: Allows snorlax to upgrade giga impact into pulverizing pancake.', null, 'snorlium-z--held',
        '卡比兽Ｚ', 'Held: Allows Snorlax to upgrade Giga Impact into Pulverizing Pancake.', 'Ｚ力量的结晶。
会将卡比兽的终极冲击
升级成Ｚ招式。', null),
       (846, 0, 'Held: Allows eevee to upgrade last resort into extreme evoboost.', null, 'eevium-z--held', '伊布Ｚ',
        'Held: Allows Eevee to upgrade Last Resort into Extreme Evoboost.', 'Ｚ力量的结晶。
会将伊布的珍藏
升级成Ｚ招式。', null),
       (847, 0, 'Held: Allows mew to upgrade psychic into genesis supernova.', null, 'mewnium-z--held', '梦幻Ｚ',
        'Held: Allows Mew to upgrade Psychic into Genesis Supernova.', 'Ｚ力量的结晶。
会将梦幻的精神强念
升级成Ｚ招式。', null),
       (877, 0, 'Held: Allows cap-wearing pikachu to upgrade thunderbolt into 10 000 000 volt thunderbolt.', null,
        'pikashunium-z--held', '智皮卡Ｚ',
        'Held: Allows cap-wearing Pikachu to upgrade Thunderbolt into 10,000,000 Volt Thunderbolt.', 'Ｚ力量的结晶。
会将戴着帽子的皮卡丘的
十万伏特升级成Ｚ招式。', null),
       (878, 0, 'Holds ingredients during Mallow’s trial.', null, 'forage-bag', '材料袋',
        'Holds ingredients during Mallow’s trial.', '将丛林里收集到的材料
装在一起的袋子。
在玛奥的考验中使用。', null),
       (879, 0, 'Allows the player to fish for Pokémon.', null, 'fishing-rod', '钓竿',
        'Allows the player to fish for Pokémon.', '水莲队长制作的钓竿。
如果向着岩石阴影那边使用，
就能钓到宝可梦。', null),
       (880, 0, 'Lost by Professor Kukui.', null, 'professors-mask', '博士的面罩', 'Lost by Professor Kukui.', '皇家蒙面人的面罩。
好像是职业摔角手
自己用缝纫机缝制的。', null),
       (881, 10, 'Hosts a mission in Festival Plaza.', null, 'festival-ticket', '圆庆票',
        'Hosts a mission in Festival Plaza.', '可以在圆庆广场
开启游乐项目。', null),
       (882, 0, 'Required to obtain a Z-Ring.', null, 'sparkling-stone', '光辉石', 'Required to obtain a Z-Ring.', '从被认为是阿罗拉守护神的
宝可梦那里得到的石头。
据说其光辉下有着某种秘密。', null),
       (883, 4000,
        'Makes wild Pokémon more likely to summon allies.  Held: increases the holder’s Speed by one stage when affected by Intimidate.',
        30, 'adrenaline-orb', '胆怯球',
        'Makes wild Pokémon more likely to summon allies.  Held: increases the holder’s Speed by one stage when affected by Intimidate.', '使用后会容易呼唤伙伴，
但使用后会消失。
携带后，在受到威吓时速度会提高。', null),
       (884, 0, 'Contains collected Zygarde cells/cores.  Can teach Zygarde moves.', null, 'zygarde-cube',
        '基格尔德多面体', 'Contains collected Zygarde cells/cores.  Can teach Zygarde moves.', '用于收集宝可梦基格尔德的
核心和细胞的道具。
还可以教基格尔德招式。', null),
       (885, 3000, 'Used on a party Pokémon
:   Evolves an Alola sandshrew into Alola sandslash or an Alola vulpix into Alola ninetales.', 30, 'ice-stone',
        '冰之石', 'Evolves an Alola Sandshrew into Alola Sandslash or an Alola Vulpix into Alola Ninetales.', '能让某些特定宝可梦
进化的神奇石头。
有着雪花般的花纹。', null),
       (886, 0, 'Allows the player to summon a Ride Pokémon.  Unused, as this can be done simply by pressing Y.', null,
        'ride-pager', '骑行装置', 'Allows the player to summon a Ride Pokémon.', '通过输入编号，瞬间召唤
与编号对应的坐骑宝可梦的道具。', null),
       (887, 0, 'Used in battle
:   Attempts to catch a wild Pokémon.  If the wild Pokémon is an Ultra Beast, this ball has a catch rate of 5×.  Otherwise, it has a catch rate of 0.1×.

    If used in a trainer battle, nothing happens and the ball is lost.', null, 'beast-ball', '究极球',
        'Tries to catch a wild Pokémon.  Success rate is 5× for Ultra Beasts and 0.1× for all other Pokémon.', '为捕捉究极异兽而制作的特殊精灵球。
很难捕捉究极异兽之外的宝可梦。', null),
       (888, 350, 'Cures major status ailments and confusion.', 30, 'big-malasada', '大马拉萨达',
        'Cures major status ailments and confusion.', '阿罗拉特产的油炸面包。
能治愈１只宝可梦的
所有异常状态。', null),
       (889, 300, 'Changes Oricorio to Baile Style.  Single-use and cannot be used in battle.', 10, 'red-nectar',
        '朱红色花蜜', 'Changes Oricorio to Baile Style.', '在乌拉乌拉花园里获得的花蜜。
可以改变特定宝可梦的样子。', null),
       (890, 300, 'Changes Oricorio to Pom-Pom Style.  Single-use and cannot be used in battle.', 10, 'yellow-nectar',
        '金黄色花蜜', 'Changes Oricorio to Pom-Pom Style.', '在美乐美乐花园里获得的花蜜。
可以改变特定宝可梦的样子。', null),
       (891, 300, 'Changes Oricorio to Pa’u Style.  Single-use and cannot be used in battle.', 10, 'pink-nectar',
        '桃粉色花蜜', 'Changes Oricorio to Pa’u Style.', '在皇家大道的花园里获得的花蜜。
可以改变特定宝可梦的样子。', null),
       (892, 300, 'Changes Oricorio to Sensu Style.  Single-use and cannot be used in battle.', 10, 'purple-nectar',
        '兰紫色花蜜', 'Changes Oricorio to Sensu Style.', '在波尼花园里获得的花蜜。
可以改变特定宝可梦的样子。', null),
       (893, 0, 'Evolves Nebby into Solgaleo when used at the Altar of the Sunne.', null, 'sun-flute', '太阳之笛',
        'Evolves Nebby into Solgaleo when used at the Altar of the Sunne.', '为了对太阳的
传说的宝可梦表达感激之情，
据说会用它来献上美乐。', null),
       (894, 0, 'Evolves Nebby into Lunala when used at the Altar of the Moone.', null, 'moon-flute', '月亮之笛',
        'Evolves Nebby into Lunala when used at the Altar of the Moone.', '为了对月亮的
传说的宝可梦表达感激之情，
据说会用它来献上美乐。', null),
       (895, 0, 'Unlocks Looker’s motel room on Route 8.', null, 'enigmatic-card', '奇异卡片',
        'Unlocks Looker’s motel room on Route 8.', '上面写有“在阿卡拉岛８号道路的
汽车旅馆客房里等”的谜之卡片。', null),
       (896, 4000,
        'Held: When the holder changes the Terrain (whether by move or ability), it will last  8 turns instead of 5.',
        60, 'terrain-extender', '大地膜', 'Held: Extends the holder’s Terrain effects to 8 turns.', '当携带它的宝可梦
利用招式或特性展开场地时，
场地的持续时间会比平时更长。', null),
       (897, 4000, 'Held: Prevents side effects of contact moves used on the holder.', 30, 'protective-pads',
        '部位护具', 'Held: Prevents side effects of contact moves used on the holder.', '不会受到触碰攻击对手时
本应受到的效果。', null),
       (898, 4000,
        'Held: If the holder enters battle during Electric Terrain, or if Electric Terrain is activated while the holder is in battle, this item is consumed and the holder’s Defense raises by one stage.',
        10, 'electric-seed', '电气种子',
        'Held: Consumed on Electric Terrain and raises the holder’s Defense by one stage.', '让宝可梦携带后，
在电气场地上使用，
防御就会提高。', null),
       (899, 4000,
        'Held: If the holder enters battle during Psychic Terrain, or if Psychic Terrain is activated while the holder is in battle, this item is consumed and the holder’s Special Defense raises by one stage.',
        10, 'psychic-seed', '精神种子',
        'Held: Consumed on Psychic Terrain and raises the holder’s Special Defense by one stage.', '让宝可梦携带后，
在精神场地上使用，
特防就会提高。', null),
       (900, 4000,
        'Held: If the holder enters battle during Misty Terrain, or if Misty Terrain is activated while the holder is in battle, this item is consumed and the holder’s Special Defense raises by one stage.',
        10, 'misty-seed', '薄雾种子',
        'Held: Consumed on Misty Terrain and raises the holder’s Special Defense by one stage.', '让宝可梦携带后，
在薄雾场地上使用，
特防就会提高。', null),
       (901, 4000,
        'Held: If the holder enters battle during Grassy Terrain, or if Grassy Terrain is activated while the holder is in battle, this item is consumed and the holder’s Defense raises by one stage.',
        10, 'grassy-seed', '青草种子', 'Held: Consumed on Grassy Terrain and raises the holder’s Defense by one stage.', '让宝可梦携带后，
在青草场地上使用，
防御就会提高。', null),
       (902, 1000, 'Held: Changes Silvally to its Fighting form.  Changes Multi-Attack’s type to Fighting.', 50,
        'fighting-memory', '战斗存储碟',
        'Held: Changes Silvally to its Fighting form.  Changes Multi-Attack’s type to Fighting.', '装有格斗属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (903, 1000, 'Held: Changes Silvally to its Flying form.  Changes Multi-Attack’s type to Flying.', 50,
        'flying-memory', '飞翔存储碟',
        'Held: Changes Silvally to its Flying form.  Changes Multi-Attack’s type to Flying.', '装有飞行属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (904, 1000, 'Held: Changes Silvally to its Poison form.  Changes Multi-Attack’s type to Poison.', 50,
        'poison-memory', '毒存储碟',
        'Held: Changes Silvally to its Poison form.  Changes Multi-Attack’s type to Poison.', '装有毒属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (905, 1000, 'Held: Changes Silvally to its Ground form.  Changes Multi-Attack’s type to Ground.', 50,
        'ground-memory', '大地存储碟',
        'Held: Changes Silvally to its Ground form.  Changes Multi-Attack’s type to Ground.', '装有地面属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (906, 1000, 'Held: Changes Silvally to its Rock form.  Changes Multi-Attack’s type to Rock.', 50, 'rock-memory',
        '岩石存储碟', 'Held: Changes Silvally to its Rock form.  Changes Multi-Attack’s type to Rock.', '装有岩石属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (907, 1000, 'Held: Changes Silvally to its Bug form.  Changes Multi-Attack’s type to Bug.', 50, 'bug-memory',
        '虫子存储碟', 'Held: Changes Silvally to its Bug form.  Changes Multi-Attack’s type to Bug.', '装有虫属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (908, 1000, 'Held: Changes Silvally to its Ghost form.  Changes Multi-Attack’s type to Ghost.', 50,
        'ghost-memory', '幽灵存储碟',
        'Held: Changes Silvally to its Ghost form.  Changes Multi-Attack’s type to Ghost.', '装有幽灵属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (909, 1000, 'Held: Changes Silvally to its Steel form.  Changes Multi-Attack’s type to Steel.', 50,
        'steel-memory', '钢铁存储碟',
        'Held: Changes Silvally to its Steel form.  Changes Multi-Attack’s type to Steel.', '装有钢属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (910, 1000, 'Held: Changes Silvally to its Fire form.  Changes Multi-Attack’s type to Fire.', 50, 'fire-memory',
        '火焰存储碟', 'Held: Changes Silvally to its Fire form.  Changes Multi-Attack’s type to Fire.', '装有火属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (911, 1000, 'Held: Changes Silvally to its Water form.  Changes Multi-Attack’s type to Water.', 50,
        'water-memory', '清水存储碟',
        'Held: Changes Silvally to its Water form.  Changes Multi-Attack’s type to Water.', '装有水属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (912, 1000, 'Held: Changes Silvally to its Grass form.  Changes Multi-Attack’s type to Grass.', 50,
        'grass-memory', '青草存储碟',
        'Held: Changes Silvally to its Grass form.  Changes Multi-Attack’s type to Grass.', '装有草属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (913, 1000, 'Held: Changes Silvally to its Electric form.  Changes Multi-Attack’s type to Electric.', 50,
        'electric-memory', '电子存储碟',
        'Held: Changes Silvally to its Electric form.  Changes Multi-Attack’s type to Electric.', '装有电属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (914, 1000, 'Held: Changes Silvally to its Psychic form.  Changes Multi-Attack’s type to Psychic.', 50,
        'psychic-memory', '精神存储碟',
        'Held: Changes Silvally to its Psychic form.  Changes Multi-Attack’s type to Psychic.', '装有超能力属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (915, 1000, 'Held: Changes Silvally to its Ice form.  Changes Multi-Attack’s type to Ice.', 50, 'ice-memory',
        '冰雪存储碟', 'Held: Changes Silvally to its Ice form.  Changes Multi-Attack’s type to Ice.', '装有冰属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (916, 1000, 'Held: Changes Silvally to its Dragon form.  Changes Multi-Attack’s type to Dragon.', 50,
        'dragon-memory', '龙存储碟',
        'Held: Changes Silvally to its Dragon form.  Changes Multi-Attack’s type to Dragon.', '装有龙属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (917, 1000, 'Held: Changes Silvally to its Dark form.  Changes Multi-Attack’s type to Dark.', 50, 'dark-memory',
        '黑暗存储碟', 'Held: Changes Silvally to its Dark form.  Changes Multi-Attack’s type to Dark.', '装有恶属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (1029, 1000, null, 30, 'gold-leaf', '金色叶子', null, '神奇的金色叶子。
至今仍未发现能长出
这种叶子的树木。', null),
       (918, 1000, 'Held: Changes Silvally to its Fairy form.  Changes Multi-Attack’s type to Fairy.', 50,
        'fairy-memory', '妖精存储碟',
        'Held: Changes Silvally to its Fairy form.  Changes Multi-Attack’s type to Fairy.', '装有妖精属性数据的存储碟。
特定宝可梦携带后，
属性就会发生变化。', null),
       (919, 0, 'XXX new effect for bike--green', null, 'bike--green', '自行车', 'XXX new effect for bike--green', '能比跑步鞋跑得还快的
折叠式自行车。', null),
       (920, 0, 'XXX new effect for storage-key--galactic-warehouse', null, 'storage-key--galactic-warehouse',
        '仓库钥匙', 'XXX new effect for storage-key--galactic-warehouse', '用于进入银河队在帷幕市市郊的
可疑仓库的钥匙。', null),
       (921, 0, 'XXX new effect for basement-key--goldenrod', null, 'basement-key--goldenrod', '地下钥匙',
        'XXX new effect for basement-key--goldenrod', '用来打开满金地道
大门的钥匙。', null),
       (922, 0, 'XXX new effect for xtranceiver--red', null, 'xtransceiver--red', '即时通讯器',
        'XXX new effect for xtranceiver--red', '带摄像头功能，
最多能让４人进行通话的
最新型对讲机。', null),
       (923, 0, 'XXX new effect for xtranceiver--yellow', null, 'xtransceiver--yellow', '即时通讯器',
        'XXX new effect for xtranceiver--yellow', '带摄像头功能，
最多能让４人进行通话的
最新型对讲机。', null),
       (924, 0, 'XXX new effect for dna-splicers--merge', null, 'dna-splicers--merge', '基因之楔',
        'XXX new effect for dna-splicers--merge', '据说是能让原本为一体的
酋雷姆和某宝可梦
合体的一对楔子。', null),
       (925, 0, 'XXX new effect for dna-splicers--split', null, 'dna-splicers--split', '基因之楔',
        'XXX new effect for dna-splicers--split', '能让合体后的
酋雷姆和某宝可梦
分离成原状的一对楔子。', null),
       (926, 0, 'XXX new effect for dropped-item--red', null, 'dropped-item--red', '遗忘物',
        'XXX new effect for dropped-item--red', '在雷文市的游乐园里
捡到的即时通讯器。
失主好像是个男孩子。', null),
       (927, 0, 'XXX new effect for dropped-item--yellow', null, 'dropped-item--yellow', '遗忘物',
        'XXX new effect for dropped-item--yellow', '在雷文市的游乐园里
捡到的即时通讯器。
失主好像是个女孩子。', null),
       (928, 0, 'XXX new effect for holo-caster--green', null, 'holo-caster--green', '全息影像通讯器',
        'XXX new effect for holo-caster--green', '可以随时查看接收到的
全息影像数据的装置。', null),
       (929, 0, 'XXX new effect for bike--yellow', null, 'bike--yellow', '自行车', 'XXX new effect for bike--yellow', '能比跑步鞋跑得还快的
折叠式自行车。', null),
       (930, 0, 'XXX new effect for holo-caster--red', null, 'holo-caster--red', '全息影像通讯器',
        'XXX new effect for holo-caster--red', '可以随时查看接收到的
全息影像数据的装置。', null),
       (931, 0, 'XXX new effect for basement-key--new-mauville', null, 'basement-key--new-mauville', '地下钥匙',
        'XXX new effect for basement-key--new-mauville', '进入紫堇地下的新紫堇时，
所使用的钥匙。', null),
       (932, 0, 'XXX new effect for storage-key--sea-mauville', null, 'storage-key--sea-mauville', '仓库钥匙',
        'XXX new effect for storage-key--sea-mauville', '进入海紫堇的仓库时
所需的钥匙。', null),
       (933, 0, 'XXX new effect for ss-ticket--hoenn', null, 'ss-ticket--hoenn', '船票',
        'XXX new effect for ss-ticket--hoenn', '乘坐渡船时需要用到。', null),
       (934, 0, 'XXX new effect for contest-costume--dress', null, 'contest-costume--dress', '演出礼裙',
        'XXX new effect for contest-costume--dress', '在华丽大赛上演出时穿的
非常可爱的礼裙。', null),
       (935, 0, 'XXX new effect for meteorite--2', null, 'meteorite--2', '陨石', 'XXX new effect for meteorite--2', '在烟囱山上获得的陨石。
隐约散发着光辉。', null),
       (936, 0, 'XXX new effect for meteorite--3', null, 'meteorite--3', '陨石', 'XXX new effect for meteorite--3', '在烟囱山上获得的陨石。
持续散发着微光。
摸上去隐约有点暖暖的。', null),
       (937, 0, 'XXX new effect for meteorite--4', null, 'meteorite--4', '陨石', 'XXX new effect for meteorite--4', '在烟囱山上获得的陨石。
上面浮现着某些花纹，
并散发着七色光芒。', null),
       (938, 0, 'XXX new effect for normalium-z--bag', null, 'normalium-z--bag', '一般Ｚ',
        'XXX new effect for normalium-z--bag', '可用它来制造用于对战
并能将一般属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (939, 0, 'XXX new effect for firium-z--bag', null, 'firium-z--bag', '火Ｚ', 'XXX new effect for firium-z--bag', '可用它来制造用于对战
并能将火属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (940, 0, 'XXX new effect for waterium-z--bag', null, 'waterium-z--bag', '水Ｚ',
        'XXX new effect for waterium-z--bag', '可用它来制造用于对战
并能将水属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (941, 0, 'XXX new effect for electrium-z--bag', null, 'electrium-z--bag', '电Ｚ',
        'XXX new effect for electrium-z--bag', '可用它来制造用于对战
并能将电属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (942, 0, 'XXX new effect for grassium-z--bag', null, 'grassium-z--bag', '草Ｚ',
        'XXX new effect for grassium-z--bag', '可用它来制造用于对战
并能将草属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (943, 0, 'XXX new effect for icium-z--bag', null, 'icium-z--bag', '冰Ｚ', 'XXX new effect for icium-z--bag', '可用它来制造用于对战
并能将冰属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (944, 0, 'XXX new effect for fightinium-z--bag', null, 'fightinium-z--bag', '格斗Ｚ',
        'XXX new effect for fightinium-z--bag', '可用它来制造用于对战
并能将格斗属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (945, 0, 'XXX new effect for poisonium-z--bag', null, 'poisonium-z--bag', '毒Ｚ',
        'XXX new effect for poisonium-z--bag', '可用它来制造用于对战
并能将毒属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (946, 0, 'XXX new effect for groundium-z--bag', null, 'groundium-z--bag', '地面Ｚ',
        'XXX new effect for groundium-z--bag', '可用它来制造用于对战
并能将地面属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (947, 0, 'XXX new effect for flyinium-z--bag', null, 'flyinium-z--bag', '飞行Ｚ',
        'XXX new effect for flyinium-z--bag', '可用它来制造用于对战
并能将飞行属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (948, 0, 'XXX new effect for psychium-z--bag', null, 'psychium-z--bag', '超能力Ｚ',
        'XXX new effect for psychium-z--bag', '可用它来制造用于对战
并能将超能力属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (949, 0, 'XXX new effect for buginium-z--bag', null, 'buginium-z--bag', '虫Ｚ',
        'XXX new effect for buginium-z--bag', '可用它来制造用于对战
并能将虫属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (950, 0, 'XXX new effect for rockium-z--bag', null, 'rockium-z--bag', '岩石Ｚ',
        'XXX new effect for rockium-z--bag', '可用它来制造用于对战
并能将岩石属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (951, 0, 'XXX new effect for ghostium-z--bag', null, 'ghostium-z--bag', '幽灵Ｚ',
        'XXX new effect for ghostium-z--bag', '可用它来制造用于对战
并能将幽灵属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (2216, 0, null, null, 'tm227', 'TM227', null, null, null),
       (952, 0, 'XXX new effect for dragonium-z--bag', null, 'dragonium-z--bag', '龙Ｚ',
        'XXX new effect for dragonium-z--bag', '可用它来制造用于对战
并能将龙属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (953, 0, 'XXX new effect for darkinium-z--bag', null, 'darkinium-z--bag', '恶Ｚ',
        'XXX new effect for darkinium-z--bag', '可用它来制造用于对战
并能将恶属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (954, 0, 'XXX new effect for steelium-z--bag', null, 'steelium-z--bag', '钢Ｚ',
        'XXX new effect for steelium-z--bag', '可用它来制造用于对战
并能将钢属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (955, 0, 'XXX new effect for fairium-z--bag', null, 'fairium-z--bag', '妖精Ｚ',
        'XXX new effect for fairium-z--bag', '可用它来制造用于对战
并能将妖精属性招式升级成
Ｚ招式的Ｚ力量结晶。', null),
       (956, 0, 'XXX new effect for pikanium-z--bag', null, 'pikanium-z--bag', '皮卡丘Ｚ',
        'XXX new effect for pikanium-z--bag', '可用它来制造用于对战
并能将皮卡丘的伏特攻击
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (957, 0, 'XXX new effect for decidium-z--bag', null, 'decidium-z--bag', '狙射树枭Ｚ',
        'XXX new effect for decidium-z--bag', '可用它来制造用于对战
并能将狙射树枭的缝影
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (958, 0, 'XXX new effect for incinium-z--bag', null, 'incinium-z--bag', '炽焰咆哮虎Ｚ',
        'XXX new effect for incinium-z--bag', '可用它来制造用于对战
并能将炽焰咆哮虎的ＤＤ金勾臂
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (959, 0, 'XXX new effect for primarium-z--bag', null, 'primarium-z--bag', '西狮海壬Ｚ',
        'XXX new effect for primarium-z--bag', '可用它来制造用于对战
并能将西狮海壬的泡影的咏叹调
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (960, 0, 'XXX new effect for tapunium-z--bag', null, 'tapunium-z--bag', '卡璞Ｚ',
        'XXX new effect for tapunium-z--bag', '可用它来制造用于对战
并能将卡璞们的自然之怒
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (961, 0, 'XXX new effect for marshadium-z--bag', null, 'marshadium-z--bag', '玛夏多Ｚ',
        'XXX new effect for marshadium-z--bag', '可用它来制造用于对战
并能将玛夏多的暗影偷盗
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (962, 0, 'XXX new effect for aloraichium-z--bag', null, 'aloraichium-z--bag', '阿罗雷Ｚ',
        'XXX new effect for aloraichium-z--bag', '可用它来制造用于对战
并能将阿罗拉地区雷丘的十万伏特
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (963, 0, 'XXX new effect for snorlium-z--bag', null, 'snorlium-z--bag', '卡比兽Ｚ',
        'XXX new effect for snorlium-z--bag', '可用它来制造用于对战
并能将卡比兽的终极冲击
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (964, 0, 'XXX new effect for eevium-z--bag', null, 'eevium-z--bag', '伊布Ｚ', 'XXX new effect for eevium-z--bag', '可用它来制造用于对战
并能将伊布的珍藏
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (965, 0, 'XXX new effect for mewnium-z--bag', null, 'mewnium-z--bag', '梦幻Ｚ',
        'XXX new effect for mewnium-z--bag', '可用它来制造用于对战
并能将梦幻的精神强念
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (966, 0, 'XXX new effect for pikashunium-z--bag', null, 'pikashunium-z--bag', '智皮卡Ｚ',
        'XXX new effect for pikashunium-z--bag', '可用它来制造用于对战
并能将戴着帽子的皮卡丘的十万伏特
升级成特殊Ｚ招式的Ｚ力量结晶。', null),
       (967, 0, 'XXX new effect for solganium-z--held', null, 'solganium-z--held', '索尔迦雷欧Ｚ',
        'XXX new effect for solganium-z--held', 'Ｚ力量的结晶。
会将索尔迦雷欧的流星闪冲
升级成Ｚ招式。', null),
       (968, 0, 'XXX new effect for lunalium-z--held', null, 'lunalium-z--held', '露奈雅拉Ｚ',
        'XXX new effect for lunalium-z--held', 'Ｚ力量的结晶。
会将露奈雅拉的暗影之光
升级成Ｚ招式。', null),
       (969, 0, 'XXX new effect for ultranecrozium-z--held', null, 'ultranecrozium-z--held', '究极奈克洛Ｚ',
        'XXX new effect for ultranecrozium-z--held', 'Ｚ力量的结晶。
会将奈克洛兹玛的光子喷涌
升级成Ｚ招式。', null),
       (970, 0, 'XXX new effect for mimikium-z--held', null, 'mimikium-z--held', '谜拟ＱＺ',
        'XXX new effect for mimikium-z--held', 'Ｚ力量的结晶。
会将谜拟Ｑ的嬉闹
升级成Ｚ招式。', null),
       (971, 0, 'XXX new effect for lycanium-z--held', null, 'lycanium-z--held', '鬃岩狼人Ｚ',
        'XXX new effect for lycanium-z--held', 'Ｚ力量的结晶。
会将鬃岩狼人的尖石攻击
升级成Ｚ招式。', null),
       (972, 0, 'XXX new effect for kommonium-z--held', null, 'kommonium-z--held', '杖尾鳞甲龙Ｚ',
        'XXX new effect for kommonium-z--held', 'Ｚ力量的结晶。
会将杖尾鳞甲龙的鳞片噪音
升级成Ｚ招式。', null),
       (973, 0, 'XXX new effect for solganium-z--bag', null, 'solganium-z--bag', '索尔迦雷欧Ｚ',
        'XXX new effect for solganium-z--bag', '可用它来制造用于对战
并能将索尔迦雷欧的流星闪冲
升级成Ｚ招式的Ｚ力量结晶。', null),
       (974, 0, 'XXX new effect for lunalium-z--bag', null, 'lunalium-z--bag', '露奈雅拉Ｚ',
        'XXX new effect for lunalium-z--bag', '可用它来制造用于对战
并能将露奈雅拉的暗影之光
升级成Ｚ招式的Ｚ力量结晶。', null),
       (975, 0, 'XXX new effect for ultranecrozium-z--bag', null, 'ultranecrozium-z--bag', '究极奈克洛Ｚ',
        'XXX new effect for ultranecrozium-z--bag', '能让和索尔迦雷欧或
露奈雅拉合体的奈克洛兹玛
变成崭新样子的结晶。', null),
       (976, 0, 'XXX new effect for mimikium-z--bag', null, 'mimikium-z--bag', '谜拟ＱＺ',
        'XXX new effect for mimikium-z--bag', '可用它来制造用于对战
并能将谜拟Ｑ的嬉闹
升级成Ｚ招式的Ｚ力量结晶。', null),
       (977, 0, 'XXX new effect for lycanium-z--bag', null, 'lycanium-z--bag', '鬃岩狼人Ｚ',
        'XXX new effect for lycanium-z--bag', '可用它来制造用于对战
并能将鬃岩狼人的尖石攻击
升级成Ｚ招式的Ｚ力量结晶。', null),
       (978, 0, 'XXX new effect for kommonium-z--bag', null, 'kommonium-z--bag', '杖尾鳞甲龙Ｚ',
        'XXX new effect for kommonium-z--bag', '可用它来制造用于对战
并能将杖尾鳞甲龙的鳞片噪音
升级成Ｚ招式的Ｚ力量结晶。', null),
       (979, 0, 'XXX new effect for z-power-ring', null, 'z-power-ring', 'Ｚ强力手环', 'XXX new effect for z-power-ring', '通过使用训练家的
气力和体力来让宝可梦
释放出Ｚ力量的神奇手环。', null),
       (980, 0, 'XXX new effect for pink-petal', null, 'pink-petal', '粉红花瓣', 'XXX new effect for pink-petal', '在茉莉的考验中，
能从茉莉那里得到的干花花瓣。
目标是集齐７种。', null),
       (981, 0, 'XXX new effect for orange-petal', null, 'orange-petal', '橙色花瓣', 'XXX new effect for orange-petal', '在茉莉的考验中，
能从伊利马那里得到的干花花瓣。
目标是集齐７种。', null),
       (982, 0, 'XXX new effect for blue-petal', null, 'blue-petal', '蓝色花瓣', 'XXX new effect for blue-petal', '在茉莉的考验中，
能从水莲那里得到的干花花瓣。
目标是集齐７种。', null),
       (983, 0, 'XXX new effect for red-petal', null, 'red-petal', '红色花瓣', 'XXX new effect for red-petal', '在茉莉的考验中，
能从卡奇那里得到的干花花瓣。
目标是集齐７种。', null),
       (2217, 0, null, null, 'tm228', 'TM228', null, null, null),
       (984, 0, 'XXX new effect for green-petal', null, 'green-petal', '绿色花瓣', 'XXX new effect for green-petal', '在茉莉的考验中，
能从玛奥那里得到的干花花瓣。
目标是集齐７种。', null),
       (985, 0, 'XXX new effect for yellow-petal', null, 'yellow-petal', '黄色花瓣', 'XXX new effect for yellow-petal', '在茉莉的考验中，
能从马玛内那里得到的干花花瓣。
目标是集齐７种。', null),
       (986, 0, 'XXX new effect for purple-petal', null, 'purple-petal', '紫色花瓣', 'XXX new effect for purple-petal', '在茉莉的考验中，
能从默丹那里得到的干花花瓣。
目标是集齐７种。', null),
       (987, 0, 'XXX new effect for rainbow-flower', null, 'rainbow-flower', '虹色之花',
        'XXX new effect for rainbow-flower', '将得自队长们那里的花瓣
化零为整后形成的东西。
是成长得到认可的证明。', null),
       (988, 0, 'XXX new effect for surge-badge', null, 'surge-badge', '桔色徽章', 'XXX new effect for surge-badge', '能从关都之道馆那里得到的徽章赠品。
好像仿照了某个地区的道馆徽章。', null),
       (989, 0, 'XXX new effect for n-solarizer--merge', null, 'n-solarizer--merge', '奈克洛索尔合体器',
        'XXX new effect for n-solarizer--merge', '用来让需求光的奈克洛兹玛和
索尔迦雷欧合体的机器。', null),
       (990, 0, 'XXX new effect for n-lunarizer--merge', null, 'n-lunarizer--merge', '奈克洛露奈合体器',
        'XXX new effect for n-lunarizer--merge', '用来让需求光的奈克洛兹玛和
露奈雅拉合体的机器。', null),
       (991, 0, 'XXX new effect for n-solarizer--split', null, 'n-solarizer--split', '奈克洛索尔合体器',
        'XXX new effect for n-solarizer--split', '用来让曾需求过光的奈克洛兹玛和
索尔迦雷欧分离的机器。', null),
       (992, 0, 'XXX new effect for n-lunarizer--split', null, 'n-lunarizer--split', '奈克洛露奈合体器',
        'XXX new effect for n-lunarizer--split', '用来让曾需求过光的奈克洛兹玛和
露奈雅拉分离的机器。', null),
       (993, 0, 'XXX new effect for ilimas-normalium-z', null, 'ilimas-normalium-z', '伊利马一般Ｚ',
        'XXX new effect for ilimas-normalium-z', '伊利马交给你保管的一般Ｚ。
受人嘱托要把它放到
葱郁洞窟深处的台座上。', null),
       (994, 0, 'XXX new effect for left-poke-ball', null, 'left-poke-ball', '留下的精灵球',
        'XXX new effect for left-poke-ball', '该精灵球里面装的是没有了
训练家的宝可梦。那位训练家
好像是出生自乌拉乌拉岛。', null),
       (995, 0, 'XXX new effect for roto-hatch', null, 'roto-hatch', '孵蛋碰碰', 'XXX new effect for roto-hatch', '洛托姆之力的一种。
使用后，蛋就会变得
非常容易孵化。', null),
       (996, 0, 'XXX new effect for roto-bargain', null, 'roto-bargain', '优惠碰碰', 'XXX new effect for roto-bargain', '洛托姆之力的一种。
使用后，友好商店的
商品就会变成半价。', null),
       (997, 0, 'XXX new effect for roto-prize-money', null, 'roto-prize-money', '零花钱碰碰',
        'XXX new effect for roto-prize-money', '洛托姆之力的一种。
对战后能得到的
零花钱会变成３倍。', null),
       (998, 0, 'XXX new effect for roto-exp-points', null, 'roto-exp-points', '经验碰碰',
        'XXX new effect for roto-exp-points', '洛托姆之力的一种。
对战后能得到的
经验值会稍微增加。', null),
       (999, 0, 'XXX new effect for roto-friendship', null, 'roto-friendship', '亲密碰碰',
        'XXX new effect for roto-friendship', '洛托姆之力的一种。
带在身边的宝可梦
会变得非常容易亲近。', null),
       (1000, 0, 'XXX new effect for roto-encounter', null, 'roto-encounter', '相遇碰碰',
        'XXX new effect for roto-encounter', '洛托姆之力的一种。
一定时间内，会变得容易
遇到高等级的野生宝可梦。', null),
       (1001, 0, 'XXX new effect for roto-stealth', null, 'roto-stealth', '隐身碰碰', 'XXX new effect for roto-stealth', '洛托姆之力的一种。
一定时间内，会变得完全
不会遇到野生宝可梦。', null),
       (1002, 0, 'XXX new effect for roto-hp-restore', null, 'roto-hp-restore', 'ＨＰ回复碰碰',
        'XXX new effect for roto-hp-restore', '洛托姆之力的一种。
正在战斗的我方宝可梦的
ＨＰ会全部回复。', null),
       (1003, 0, 'XXX new effect for roto-pp-restore', null, 'roto-pp-restore', 'ＰＰ回复碰碰',
        'XXX new effect for roto-pp-restore', '洛托姆之力的一种。
正在战斗的我方宝可梦的
ＰＰ会全部回复。', null),
       (1004, 0, 'XXX new effect for roto-boost', null, 'roto-boost', '加油碰碰', 'XXX new effect for roto-boost', '洛托姆之力的一种。
正在战斗的我方宝可梦的
所有能力都会提高。', null),
       (1005, 0, 'XXX new effect for roto-catch', null, 'roto-catch', '捕捉碰碰', 'XXX new effect for roto-catch', '洛托姆之力的一种。
使用后，眼前的野生宝可梦
会变得非常容易捕捉。', null),
       (1006, 0, null, null, 'autograph', '马志士的签名', null, '枯叶市道馆馆主马志士
给你的彩色签名纸。
上面写着“Good Luck！”。', null),
       (1007, 0, null, null, 'pokemon-box', '宝可梦盒', null, 'ー
ー
ー', null),
       (1008, 0, null, null, 'medicine-pocket', '回复口袋', null, 'ー
ー
ー', null),
       (1009, 0, null, null, 'candy-jar', '糖果罐', null, 'ー
ー
ー', null),
       (1010, 0, null, null, 'power-up-pocket', '强化口袋', null, 'ー
ー
ー', null),
       (1011, 0, null, null, 'clothing-trunk', '换装箱', null, '非常轻的箱子。
能塞入宝可梦或
训练家的替换衣物。', null),
       (1012, 0, null, null, 'catching-pocket', '捕捉口袋', null, 'ー
ー
ー', null),
       (1013, 0, null, null, 'battle-pocket', '对战口袋', null, 'ー
ー
ー', null),
       (1014, 1000, null, 10, 'silver-razz-berry', '银色蔓莓果', null, '把这树果交给宝可梦后，
宝可梦就会变得容易捕捉。', null),
       (1015, 5000, null, 10, 'golden-razz-berry', '金色蔓莓果', null, '把这树果交给宝可梦后，
宝可梦就会变得十分容易捕捉。', null),
       (1016, 1000, null, 10, 'silver-nanab-berry', '银色蕉香果', null, '捕捉宝可梦时，
把这树果交给宝可梦
就能平复对方的情绪。', null),
       (1017, 5000, null, 10, 'golden-nanab-berry', '金色蕉香果', null, '捕捉宝可梦时，
把这树果交给宝可梦
就能有效地平复对方的情绪。', null),
       (1018, 1000, null, 10, 'silver-pinap-berry', '银色凰梨果', null, '如果把这树果交给宝可梦，
那么接下来捉住该宝可梦时，
会更容易获得道具。', null),
       (1019, 5000, null, 10, 'golden-pinap-berry', '金色凰梨果', null, '如果把这树果交给宝可梦，
那么接下来捉住该宝可梦时，
会非常容易获得道具。', null),
       (1020, 0, null, null, 'secret-key--letsgo', '秘密钥匙', null, '能打开红莲岛上那座
宝可梦道馆的钥匙。
周身都以红色装饰。', null),
       (1021, 0, null, null, 'ss-ticket--letsgo', '船票', null, '乘坐圣特安努号时所需的船票。
上面绘有船只的图案。', null),
       (1022, 0, null, null, 'parcel--letsgo', '包裹', null, '这是常青市的商店
托付给你的包裹。
需要将它交给大木博士。', null),
       (1023, 0, null, null, 'card-key--letsgo', '钥匙卡', null, '用来打开位于金黄市的
西尔佛公司总部大厦门锁的
卡片式钥匙。', null),
       (1024, 20, null, 30, 'stretchy-spring', '松掉的弹簧', null, '细小的弹簧。
被拉得太长了，
不知道能派上什么用。', null),
       (1025, 60, null, 30, 'chalky-stone', '粉笔石', null, '路边捡到的
白白的小石头。', null),
       (1033, 0, null, 30, 'leaf-letter--pikachu', '树叶信', null, '以树叶制作的信件。
上面留有皮卡丘的足迹，
好像是要表达些什么。', null),
       (1034, 0, null, 30, 'leaf-letter--eevee', '树叶信', null, '以树叶制作的信件。
上面留有伊布的足迹，
好像是要表达些什么。', null),
       (1035, 0, null, 30, 'small-bouquet', '小小花束', null, '竭尽心力制作的素雅花束。
里面满满都是对训练家的心意。', null),
       (1036, 400, null, 30, 'lure', '引虫香水', null, '使用该香水后的一小段时间内，
稀有宝可梦会更容易出现。', null),
       (1037, 700, null, 30, 'super-lure', '白银香水', null, '使用该香水后，
稀有宝可梦会更容易出现。
效果比引虫香水更持久。', null),
       (1038, 900, null, 30, 'max-lure', '黄金香水', null, '使用该香水后，
稀有宝可梦会更容易出现。
效果比白银香水更持久。', null),
       (1039, 250, null, 30, 'pewter-crunchies', '深灰米果', null, '深灰特产的糕点。
能治愈１只宝可梦的
所有异常状态。', null),
       (1040, 20, null, null, 'health-candy', '元气糖果', null, '充满能量的糖果。
能提高宝可梦的体力。', null),
       (1041, 20, null, null, 'mighty-candy', '力量糖果', null, '充满能量的糖果。
能提高宝可梦的攻击。', null),
       (1042, 20, null, null, 'tough-candy', '守护糖果', null, '充满能量的糖果。
能提高宝可梦的防御。', null),
       (1043, 20, null, null, 'smart-candy', '知识糖果', null, '充满能量的糖果。
能提高宝可梦的特攻。', null),
       (1044, 20, null, null, 'courage-candy', '心灵糖果', null, '充满能量的糖果。
能提高宝可梦的特防。', null),
       (1045, 20, null, null, 'quick-candy', '敏捷糖果', null, '充满能量的糖果。
能提高宝可梦的速度。', null),
       (1046, 20, null, null, 'health-candy-l', '元气糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的体力上升。', null),
       (1047, 20, null, null, 'mighty-candy-l', '力量糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的攻击上升。', null),
       (1048, 20, null, null, 'tough-candy-l', '守护糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的防御上升。', null),
       (1049, 20, null, null, 'smart-candy-l', '知识糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的特攻上升。', null),
       (1050, 20, null, null, 'courage-candy-l', '心灵糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的特防上升。', null),
       (1051, 20, null, null, 'quick-candy-l', '敏捷糖果Ｌ', null, '充满能量的大糖果。
能让等级30以上的
宝可梦的速度上升。', null),
       (1052, 20, null, null, 'health-candy-xl', '元气糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的体力上升。', null),
       (1053, 20, null, null, 'mighty-candy-xl', '力量糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的攻击上升。', null),
       (1054, 20, null, null, 'tough-candy-xl', '守护糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的防御上升。', null),
       (1055, 20, null, null, 'smart-candy-xl', '知识糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的特攻上升。', null),
       (1056, 20, null, null, 'courage-candy-xl', '心灵糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的特防上升。', null),
       (1057, 20, null, null, 'quick-candy-xl', '敏捷糖果ＸＬ', null, '充满能量的超大糖果。
能让等级60以上的
宝可梦的速度上升。', null),
       (1058, 20, null, null, 'bulbasaur-candy', '妙蛙种子的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1059, 20, null, null, 'charmander-candy', '小火龙的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1060, 20, null, null, 'squirtle-candy', '杰尼龟的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1061, 20, null, null, 'caterpie-candy', '绿毛虫的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1062, 20, null, null, 'weedle-candy', '独角虫的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1063, 20, null, null, 'pidgey-candy', '波波的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1064, 20, null, null, 'rattata-candy', '小拉达的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1065, 20, null, null, 'spearow-candy', '烈雀的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1066, 20, null, null, 'ekans-candy', '阿柏蛇的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1067, 20, null, null, 'pikachu-candy', '皮卡丘的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1068, 20, null, null, 'sandshrew-candy', '穿山鼠的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1069, 20, null, null, 'nidoran-f-candy', '尼多兰的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1070, 20, null, null, 'nidoran-m-candy', '尼多朗的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1071, 20, null, null, 'clefairy-candy', '皮皮的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1072, 20, null, null, 'vulpix-candy', '六尾的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1073, 20, null, null, 'jigglypuff-candy', '胖丁的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1074, 20, null, null, 'zubat-candy', '超音蝠的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1075, 20, null, null, 'oddish-candy', '走路草的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1076, 20, null, null, 'paras-candy', '派拉斯的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1077, 20, null, null, 'venonat-candy', '毛球的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1078, 20, null, null, 'diglett-candy', '地鼠的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1079, 20, null, null, 'meowth-candy', '喵喵的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1080, 20, null, null, 'psyduck-candy', '可达鸭的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1081, 20, null, null, 'mankey-candy', '猴怪的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1082, 20, null, null, 'growlithe-candy', '卡蒂狗的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1083, 20, null, null, 'poliwag-candy', '蚊香蝌蚪的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1689, 500, null, null, 'bug-tera-shard', '虫太晶碎块', null, null, null),
       (1085, 20, null, null, 'machop-candy', '腕力的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1086, 20, null, null, 'bellsprout-candy', '喇叭芽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1087, 20, null, null, 'tentacool-candy', '玛瑙水母的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1088, 20, null, null, 'geodude-candy', '小拳石的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1089, 20, null, null, 'ponyta-candy', '小火马的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1090, 20, null, null, 'slowpoke-candy', '呆呆兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1091, 20, null, null, 'magnemite-candy', '小磁怪的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1092, 20, null, null, 'farfetchd-candy', '大葱鸭的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1093, 20, null, null, 'doduo-candy', '嘟嘟的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1094, 20, null, null, 'seel-candy', '小海狮的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1095, 20, null, null, 'grimer-candy', '臭泥的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1096, 20, null, null, 'shellder-candy', '大舌贝的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1097, 20, null, null, 'gastly-candy', '鬼斯的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1098, 20, null, null, 'onix-candy', '大岩蛇的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1099, 20, null, null, 'drowzee-candy', '催眠貘的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1100, 20, null, null, 'krabby-candy', '大钳蟹的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1101, 20, null, null, 'voltorb-candy', '霹雳电球的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1102, 20, null, null, 'exeggcute-candy', '蛋蛋的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1103, 20, null, null, 'cubone-candy', '卡拉卡拉的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1104, 20, null, null, 'hitmonlee-candy', '飞腿郎的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1105, 20, null, null, 'hitmonchan-candy', '快拳郎的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1106, 20, null, null, 'lickitung-candy', '大舌头的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1107, 20, null, null, 'koffing-candy', '瓦斯弹的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1108, 20, null, null, 'rhyhorn-candy', '独角犀牛的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1109, 20, null, null, 'chansey-candy', '吉利蛋的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1110, 20, null, null, 'tangela-candy', '蔓藤怪的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1111, 20, null, null, 'kangaskhan-candy', '袋兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1112, 20, null, null, 'horsea-candy', '墨海马的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1113, 20, null, null, 'goldeen-candy', '角金鱼的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1114, 20, null, null, 'staryu-candy', '海星星的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1115, 20, null, null, 'mr-mime-candy', '魔墙人偶的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1116, 20, null, null, 'scyther-candy', '飞天螳螂的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1117, 20, null, null, 'jynx-candy', '迷唇姐的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1118, 20, null, null, 'electabuzz-candy', '电击兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1119, 20, null, null, 'pinsir-candy', '凯罗斯的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1120, 20, null, null, 'tauros-candy', '肯泰罗的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1121, 20, null, null, 'magikarp-candy', '鲤鱼王的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1122, 20, null, null, 'lapras-candy', '拉普拉斯的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1123, 20, null, null, 'ditto-candy', '百变怪的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1124, 20, null, null, 'eevee-candy', '伊布的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1125, 20, null, null, 'porygon-candy', '多边兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1126, 20, null, null, 'omanyte-candy', '菊石兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1127, 20, null, null, 'kabuto-candy', '化石盔的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1128, 20, null, null, 'aerodactyl-candy', '化石翼龙的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1129, 20, null, null, 'snorlax-candy', '卡比兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1130, 20, null, null, 'articuno-candy', '急冻鸟的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1131, 20, null, null, 'zapdos-candy', '闪电鸟的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1132, 20, null, null, 'moltres-candy', '火焰鸟的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1133, 20, null, null, 'dratini-candy', '迷你龙的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1134, 20, null, null, 'mewtwo-candy', '超梦的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1135, 20, null, null, 'mew-candy', '梦幻的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1136, 20, null, null, 'meltan-candy', '美录坦的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1137, 20, null, null, 'magmar-candy', '鸭嘴火兽的糖果', null, '充满能量的糖果。
能让特定宝可梦的
所有能力上升。', null),
       (1138, 0, null, null, 'endorsement', '推荐函', null, '用于参加道馆挑战
这一大赛的推荐函。', null),
       (1139, 0, null, null, 'pokemon-box-link', '宝可梦盒', null, '可以随时访问
宝可梦中心电脑里的盒子，
来寄放或领回需要的宝可梦。', null),
       (1140, 0, null, null, 'wishing-star', '许愿星', null, '这是一块在伽勒尔地区发现的
蕴含着神奇力量的石头。听说
捡到它的人能实现自己的愿望。', null),
       (1141, 0, null, null, 'dynamax-band', '极巨腕带', null, '内部嵌入了许愿星，
在能量点就会发出光芒，
使宝可梦极巨化。', null),
       (1142, 0, null, null, 'fishing-rod--galar', '钓竿', null, '在有水的地方使用，
就能钓到各种各样的宝可梦。', null),
       (1143, 0, null, null, 'rotom-bike', '洛托姆自行车', null, '宝可梦洛托姆潜入马达后，
这辆自行车变得可以
通过涡轮发动机加速了。', null),
       (1144, 400, null, 10, 'sausages', '粗绞肉香肠', null, '在露营营地制作料理时会使用的
一种食材。白香肠是香肠的一种。
需要先煮过之后再食用。', null),
       (1145, 950, null, 80, 'bobs-food-tin', '饱伯罐头', null, '在露营营地制作料理时会使用的
一种食材。不知是什么原因，
饱伯卖的罐头在伽勒尔特别受欢迎。', null),
       (1146, 950, null, 80, 'bachs-food-tin', '巴哈罐头', null, '在露营营地制作料理时会使用的
一种食材。不知是什么原因，
巴哈卖的罐头在伽勒尔特别受欢迎。', null),
       (1147, 400, null, 80, 'tin-of-beans', '豆子罐头', null, '在露营营地制作料理时会使用的
一种食材。放进料理里一起煮，
豆子的那种香甜就会散发出来。', null),
       (1148, 150, null, 10, 'bread', '吐司面包', null, '在露营营地制作料理时会使用的
一种食材。同时也是用于吸干盘中
最后一滴咖喱的秘密武器。', null),
       (1149, 150, null, 10, 'pasta', '通心粉', null, '在露营营地制作料理时会使用的
一种食材。是由小麦粉揉制的面条。
搭配咖喱出乎意料地美味。', null),
       (1150, 400, null, 10, 'mixed-mushrooms', '袋装蕈菇', null, '在露营营地制作料理时会使用的
一种食材。各种菌菇混在一起的嚼劲
会为口感带来变化。', null),
       (1151, 2200, null, 10, 'smoke-poke-tail', '烟熏尾巴', null, '在露营营地制作料理时会使用的
一种食材。宝可梦呆呆兽的尾巴
就算断掉也会很快长出新的。', null),
       (1152, 2200, null, 20, 'large-leek', '粗枝大葱', null, '在露营营地制作料理时会使用的
一种食材。不知是不是宝可梦
大葱鸭最爱的那种植物的茎。', null),
       (1153, 2200, null, 20, 'fancy-apple', '特选苹果', null, '在露营营地制作料理时会使用的
一种食材。历经千挑万选的苹果
不仅形状饱满，表皮更是亮泽鲜艳。', null),
       (1154, 950, null, 30, 'brittle-bones', '细骨', null, '在露营营地制作料理时会使用的
一种食材。将骨头精心熬煮而得的
一丝浓缩的精华加深了料理的美味。', null),
       (1155, 400, null, 10, 'pack-of-potatoes', '袋装土豆', null, '在露营营地制作料理时会使用的
一种食材。土豆能中和辣味，
使整道料理的滋味变得柔和。', null),
       (1156, 950, null, 10, 'pungent-root', '水边香草', null, '在露营营地制作料理时会使用的
一种食材。将信手摘取的香草
点缀在上面，使料理更加色香味俱全。', null),
       (1157, 400, null, 10, 'salad-mix', '袋装蔬菜', null, '在露营营地制作料理时会使用的
一种食材。将各种蔬菜搭配到一起，
看上去就非常有益健康。', null),
       (1158, 150, null, 10, 'fried-food', '炸物拼盘', null, '在露营营地制作料理时会使用的
一种食材。炸完出锅后
放置了一段时间，所以稍稍有些油腻。', null),
       (1159, 2200, null, 20, 'boiled-egg', '水煮蛋', null, '在露营营地制作料理时会使用的
一种食材。只需一个水煮蛋，
整道料理的水准都能得到升华。', null),
       (1160, 0, null, null, 'camping-gear', '露营组合', null, '可以在旷野地带
或露营营地上搭起帐篷，
在那里制作料理。', null),
       (1161, 0, null, null, 'rusted-sword', '腐朽的剑', null, '据说很久以前，英雄就是
拿着这把剑驱走了灾厄。
而现在早已变得锈迹斑斑。', null),
       (1162, 0, null, null, 'rusted-shield', '腐朽的盾', null, '据说很久以前，英雄就是
拿着这面盾驱走了灾厄。
而现在早已变得锈迹斑斑。', null),
       (1163, 5000, null, 100, 'fossilized-bird', '化石鸟', null, '远古时代的宝可梦化石残片。
它曾翱翔于天空，
本来面目至今仍是未解之谜。', null),
       (1164, 5000, null, 100, 'fossilized-fish', '化石鱼', null, '远古时代的宝可梦化石残片。
它曾栖息于大海，
本来面目至今仍是未解之谜。', null),
       (1165, 5000, null, 100, 'fossilized-drake', '化石龙', null, '远古时代的宝可梦化石残片。
它曾栖息于陆地，
本来面目至今仍是未解之谜。', null),
       (1166, 5000, null, 100, 'fossilized-dino', '化石海兽', null, '远古时代的宝可梦化石残片。
它曾栖息于大海，
本来面目至今仍是未解之谜。', null),
       (1167, 500, null, 10, 'strawberry-sweet', '草莓糖饰', null, '草莓形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1168, 500, null, 10, 'love-sweet', '爱心糖饰', null, '爱心形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1169, 500, null, 10, 'berry-sweet', '野莓糖饰', null, '浆果形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1170, 500, null, 10, 'clover-sweet', '幸运草糖饰', null, '四叶草形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1171, 500, null, 10, 'flower-sweet', '花朵糖饰', null, '花朵形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1172, 500, null, 10, 'star-sweet', '星星糖饰', null, '星星形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1173, 500, null, 10, 'ribbon-sweet', '蝴蝶结糖饰', null, '蝴蝶结形状的工艺糖果。
让小仙奶携带的话，
会高兴地转圈圈。', null),
       (1174, 2200, null, 30, 'sweet-apple', '甜甜苹果', null, '这种神奇的苹果
可以使特定的宝可梦进化。
吃起来非常甜美。', null),
       (1175, 2200, null, 30, 'tart-apple', '酸酸苹果', null, '这种神奇的苹果
可以使特定的宝可梦进化。
吃起来酸酸的。', null),
       (1176, 4000, null, 30, 'throat-spray', '爽喉喷雾', null, '使用声音相关的招式时，
特攻会提高。', null),
       (1177, 4000, null, 50, 'eject-pack', '避难背包', null, '当携带它的宝可梦能力下降时，
同行宝可梦就会自动替换上场。', null),
       (1178, 4000, null, 80, 'heavy-duty-boots', '厚底靴', null, '不受脚下陷阱等的影响。', null),
       (1179, 4000, null, 80, 'blunder-policy', '打空保险', null, '招式因命中率影响而落空时，
速度会大幅提高。', null),
       (1180, 4000, null, 100, 'room-service', '客房服务', null, '让宝可梦携带后，
在戏法空间使用时，
速度会下降。', null),
       (1181, 4000, null, 60, 'utility-umbrella', '万能伞', null, '携带它的宝可梦
在下雨或日照很强时，
不会受到天气的影响。', null),
       (1182, 20, null, 30, 'exp-candy-xs', '经验糖果ＸＳ', null, '充满能量的糖果。
交给１只宝可梦后，
可以为它增加一点点经验值。', null),
       (1183, 240, null, 30, 'exp-candy-s', '经验糖果Ｓ', null, '充满能量的糖果。
交给１只宝可梦后，
可以为它增加少许经验值。', null),
       (1184, 1000, null, 30, 'exp-candy-m', '经验糖果Ｍ', null, '充满能量的糖果。
交给１只宝可梦后，
可以为它增加经验值。', null),
       (1185, 3000, null, 30, 'exp-candy-l', '经验糖果Ｌ', null, '充满能量的糖果。
交给１只宝可梦后，
可以为它增加许多经验值。', null),
       (1186, 10000, null, 30, 'exp-candy-xl', '经验糖果ＸＬ', null, '充满能量的糖果。
交给１只宝可梦后，
可以为它增加大量经验值。', null),
       (1187, 0, null, 30, 'dynamax-candy', '极巨糖果', null, '充满能量的糖果。
给予宝可梦后，极巨化等级能够提高１。
当极巨化等级提高时，ＨＰ会增加。', null),
       (1188, 4000, null, 10, 'tr00', '招式记录００', null, '激烈地跳起战舞提高气势。
大幅提高自己的攻击。', null),
       (1189, 6000, null, 85, 'tr01', '招式记录０１', null, '用整个身体
压住对手进行攻击。
有时会让对手陷入麻痹状态。', null),
       (1190, 10000, null, 90, 'tr02', '招式记录０２', null, '向对手发射烈焰进行攻击。
有时会让对手陷入灼伤状态。', null),
       (1191, 16000, null, 110, 'tr03', '招式记录０３', null, '向对手猛烈地
喷射大量水流进行攻击。', null),
       (1192, 10000, null, 90, 'tr04', '招式记录０４', null, '利用大浪
攻击自己周围所有的宝可梦。', null),
       (1193, 10000, null, 90, 'tr05', '招式记录０５', null, '向对手发射冰冻光束进行攻击。
有时会让对手陷入冰冻状态。', null),
       (1194, 16000, null, 110, 'tr06', '招式记录０６', null, '将猛烈的暴风雪
刮向对手进行攻击。
有时会让对手陷入冰冻状态。', null),
       (1195, 6000, null, 10, 'tr07', '招式记录０７', null, '用力踢对手的脚，
使其摔倒进行攻击。
对手越重，威力越大。', null),
       (1196, 10000, null, 90, 'tr08', '招式记录０８', null, '向对手发出强力电击进行攻击。
有时会让对手陷入麻痹状态。', null),
       (1197, 16000, null, 110, 'tr09', '招式记录０９', null, '向对手劈下暴雷进行攻击。
有时会让对手陷入麻痹状态。', null),
       (1198, 16000, null, 100, 'tr10', '招式记录１０', null, '利用地震的冲击，
攻击自己周围所有的宝可梦。', null),
       (1199, 10000, null, 90, 'tr11', '招式记录１１', null, '向对手发送强大的念力进行攻击。
有时会降低对手的特防。', null),
       (1200, 4000, null, 10, 'tr12', '招式记录１２', null, '让身体放松变得轻盈，
以便高速移动。
大幅提高自己的速度。', null),
       (1201, 2000, null, 10, 'tr13', '招式记录１３', null, '深深地吸口气，集中精神。
自己的攻击会变得
容易击中要害。', null),
       (1202, 2000, null, 10, 'tr14', '招式记录１４', null, '挥动手指刺激自己的大脑，
从所有的招式中
任意使出１个。', null),
       (1203, 16000, null, 110, 'tr15', '招式记录１５', null, '用大字形状的火焰烧尽对手。
有时会让对手陷入灼伤状态。', null),
       (1204, 6000, null, 80, 'tr16', '招式记录１６', null, '以惊人的气势扑向对手。
有时会使对手畏缩。', null),
       (1205, 4000, null, 10, 'tr17', '招式记录１７', null, '将头脑清空，
瞬间忘记某事，
从而大幅提高自己的特防。', null),
       (1206, 6000, null, 80, 'tr18', '招式记录１８', null, '吸取血液攻击对手。
可以回复给予对手伤害的一半ＨＰ。', null),
       (1207, 4000, null, 80, 'tr19', '招式记录１９', null, '用３种光线进行攻击。
有时会让对手陷入
麻痹、灼伤或冰冻的状态。', null),
       (1208, 6000, null, 10, 'tr20', '招式记录２０', null, '削减少许自己的ＨＰ，
制造分身。
分身将成为自己的替身。', null),
       (1209, 4000, null, 10, 'tr21', '招式记录２１', null, '竭尽全力进行攻击。
自己的ＨＰ越少，
招式的威力越大。', null),
       (1210, 10000, null, 90, 'tr22', '招式记录２２', null, '用污泥投掷对手进行攻击。
有时会让对手陷入中毒状态。', null),
       (1211, 4000, null, 10, 'tr23', '招式记录２３', null, '在对手的脚下扔撒菱。
对替换出场的
对手的宝可梦给予伤害。', null),
       (1212, 16000, null, 120, 'tr24', '招式记录２４', null, '在２～３回合内，
乱打一气地进行攻击。
大闹一番后自己会陷入混乱。', null),
       (1213, 6000, null, 80, 'tr25', '招式记录２５', null, '将神奇的念波实体化攻击对手。
给予物理伤害。', null),
       (1214, 2000, null, 10, 'tr26', '招式记录２６', null, '即使受到攻击，
也至少会留下１ＨＰ。
连续使出则容易失败。', null),
       (1215, 4000, null, 10, 'tr27', '招式记录２７', null, '从自己已学会的招式中
任意使出１个。
只能在自己睡觉时使用。', null),
       (1216, 16000, null, 120, 'tr28', '招式记录２８', null, '用坚硬且华丽的角狠狠地
刺入对手进行攻击。', null),
       (1217, 4000, null, 10, 'tr29', '招式记录２９', null, '和后备宝可梦进行替换。
换上的宝可梦
能直接继承其能力的变化。', null),
       (1218, 4000, null, 10, 'tr30', '招式记录３０', null, '让对手接受再来一次，
连续３次使出最后使用的招式。', null),
       (1219, 10000, null, 100, 'tr31', '招式记录３１', null, '使用坚硬的尾巴
摔打对手进行攻击。
有时会降低对手的防御。', null),
       (1220, 6000, null, 80, 'tr32', '招式记录３２', null, '用利牙咬碎对手进行攻击。
有时会降低对手的防御。', null),
       (1221, 6000, null, 80, 'tr33', '招式记录３３', null, '投掷一团黑影进行攻击。
有时会降低对手的特防。', null),
       (1222, 6000, null, 120, 'tr34', '招式记录３４', null, '在使用招式２回合后，
向对手发送一团念力进行攻击。', null),
       (1223, 6000, null, 90, 'tr35', '招式记录３５', null, '在３回合内
用骚乱攻击对手。
在此期间谁都不能入眠。', null),
       (1224, 10000, null, 95, 'tr36', '招式记录３６', null, '将炎热的气息
吹向对手进行攻击。
有时会让对手陷入灼伤状态。', null),
       (1225, 4000, null, 10, 'tr37', '招式记录３７', null, '使对手愤怒。
在３回合内让对手只能使出
给予伤害的招式。', null),
       (1226, 4000, null, 10, 'tr38', '招式记录３８', null, '抓住对手的空隙，
交换自己和对手的持有物。', null),
       (1227, 16000, null, 120, 'tr39', '招式记录３９', null, '发挥惊人的力量攻击对手。
自己的攻击和防御会降低。', null),
       (1228, 2000, null, 10, 'tr40', '招式记录４０', null, '利用超能力互换
自己和对手的特性。', null),
       (1229, 6000, null, 85, 'tr41', '招式记录４１', null, '攻击对手后，
有时会使其陷入灼伤状态。
也容易击中要害。', null),
       (1230, 10000, null, 90, 'tr42', '招式记录４２', null, '给予对手又吵又响的
巨大震动进行攻击。', null),
       (1231, 16000, null, 130, 'tr43', '招式记录４３', null, '使出全部力量攻击对手。
使用之后会因为反作用力，
自己的特攻大幅降低。', null),
       (1232, 4000, null, 10, 'tr44', '招式记录４４', null, '汲取宇宙中神秘的力量，
从而提高自己的防御和特防。', null),
       (1233, 10000, null, 90, 'tr45', '招式记录４５', null, '向对手喷射浑浊的水进行攻击。
有时会降低对手的命中率。', null),
       (1234, 4000, null, 10, 'tr46', '招式记录４６', null, '将皮肤变得坚硬如铁，
从而大幅提高自己的防御。', null),
       (1235, 6000, null, 80, 'tr47', '招式记录４７', null, '用尖锐的巨爪
劈开对手进行攻击。', null),
       (1236, 4000, null, 10, 'tr48', '招式记录４８', null, '使出全身力气绷紧肌肉，
从而提高自己的攻击和防御。', null),
       (1237, 4000, null, 10, 'tr49', '招式记录４９', null, '静心凝神，
从而提高自己的特攻和特防。', null),
       (1238, 10000, null, 90, 'tr50', '招式记录５０', null, '像用剑一般操纵叶片
切斩对手进行攻击。
容易击中要害。', null),
       (1239, 4000, null, 10, 'tr51', '招式记录５１', null, '激烈地跳起神秘且
强有力的舞蹈。
从而提高自己的攻击和速度。', null),
       (1240, 6000, null, 10, 'tr52', '招式记录５２', null, '让身体高速旋转并撞击对手。
速度比对手越慢，威力越大。', null),
       (1241, 16000, null, 120, 'tr53', '招式记录５３', null, '放弃守护，向对手的怀里突击。
自己的防御和特防会降低。', null),
       (1242, 4000, null, 10, 'tr54', '招式记录５４', null, '在对手的脚下撒毒菱。
使对手替换出场的宝可梦中毒。', null),
       (1243, 16000, null, 120, 'tr55', '招式记录５５', null, '让火焰覆盖全身猛撞向对手。
自己也会受到不小的伤害。
有时会让对手陷入灼伤状态。', null),
       (1244, 6000, null, 80, 'tr56', '招式记录５６', null, '从体内产生出波导之力，
然后向对手发出。
攻击必定会命中。', null),
       (1245, 6000, null, 80, 'tr57', '招式记录５７', null, '用带毒的触手或手臂
刺入对手。
有时会让对手陷入中毒状态。', null),
       (1246, 6000, null, 80, 'tr58', '招式记录５８', null, '从体内发出
充满恶意的恐怖气场。
有时会使对手畏缩。', null),
       (1247, 6000, null, 80, 'tr59', '招式记录５９', null, '将外壳坚硬的大种子，
从上方砸下攻击对手。', null),
       (1248, 6000, null, 80, 'tr60', '招式记录６０', null, '将镰刀或爪子像剪刀般地交叉，
顺势劈开对手。', null),
       (1249, 10000, null, 90, 'tr61', '招式记录６１', null, '利用振动发出音波进行攻击。
有时会降低对手的特防。', null),
       (1250, 6000, null, 85, 'tr62', '招式记录６２', null, '从大大的口中
掀起冲击波攻击对手。', null),
       (1251, 6000, null, 80, 'tr63', '招式记录６３', null, '发射如宝石般闪耀的光芒
攻击对手。', null),
       (1252, 16000, null, 120, 'tr64', '招式记录６４', null, '提高气势，
释放出全部力量。
有时会降低对手的特防。', null),
       (1253, 10000, null, 90, 'tr65', '招式记录６５', null, '发射从自然收集的生命力量。
有时会降低对手的特防。', null),
       (1254, 16000, null, 120, 'tr66', '招式记录６６', null, '收拢翅膀，
通过低空飞行突击对手。
自己也会受到不小的伤害。', null),
       (1255, 10000, null, 90, 'tr67', '招式记录６７', null, '向对手脚下释放出大地之力。
有时会降低对手的特防。', null),
       (1256, 4000, null, 10, 'tr68', '招式记录６８', null, '谋划诡计，激活头脑。
大幅提高自己的特攻。', null),
       (1257, 6000, null, 80, 'tr69', '招式记录６９', null, '将思念的力量
集中在前额进行攻击。
有时会使对手畏缩。', null),
       (1258, 10000, null, 80, 'tr70', '招式记录７０', null, '将身体的光芒聚集在一点
释放出去。
有时会降低对手的特防。', null),
       (1259, 16000, null, 130, 'tr71', '招式记录７１', null, '用尖尖的叶片向对手卷起风暴。
使用之后因为反作用力
自己的特攻会大幅降低。', null),
       (1260, 16000, null, 120, 'tr72', '招式记录７２', null, '激烈地挥舞青藤或触手
摔打对手进行攻击。', null),
       (1261, 16000, null, 120, 'tr73', '招式记录７３', null, '用肮脏的垃圾
撞向对手进行攻击。
有时会让对手陷入中毒状态。', null),
       (1262, 10000, null, 80, 'tr74', '招式记录７４', null, '用钢铁般坚硬的头部进行攻击。
有时会使对手畏缩。', null),
       (1263, 16000, null, 100, 'tr75', '招式记录７５', null, '用尖尖的岩石
刺入对手进行攻击。
容易击中要害。', null),
       (1264, 6000, null, 10, 'tr76', '招式记录７６', null, '将无数岩石悬浮在对手的周围，
从而对替换出场的
对手的宝可梦给予伤害。', null),
       (1265, 6000, null, 10, 'tr77', '招式记录７７', null, '用草缠住并绊倒对手。
对手越重，威力越大。', null),
       (1266, 10000, null, 95, 'tr78', '招式记录７８', null, '用污泥波攻击
自己周围所有的宝可梦。
有时会陷入中毒状态。', null),
       (1267, 6000, null, 10, 'tr79', '招式记录７９', null, '用沉重的身体撞向对手进行攻击。
自己比对手越重，威力越大。', null),
       (1268, 6000, null, 10, 'tr80', '招式记录８０', null, '用电气团撞向对手。
自己比对手速度越快，威力越大。', null),
       (1269, 6000, null, 95, 'tr81', '招式记录８１', null, '利用对手的力量进行攻击。
正和自己战斗的对手，
其攻击越高，伤害越大。', null),
       (1270, 4000, null, 20, 'tr82', '招式记录８２', null, '用蓄积起来的力量攻击对手。
自己的能力提高得越多，威力就越大。', null),
       (1271, 4000, null, 10, 'tr83', '招式记录８３', null, '用神奇的力量瞬间移动，
互换自己和同伴所在的位置。', null),
       (1272, 6000, null, 80, 'tr84', '招式记录８４', null, '向对手喷射煮得翻滚的开水进行攻击。
有时会让对手陷入灼伤状态。', null),
       (1273, 2000, null, 10, 'tr85', '招式记录８５', null, '激励自己，
从而提高攻击和特攻。', null),
       (1274, 10000, null, 90, 'tr86', '招式记录８６', null, '让电流覆盖全身，
撞向对手进行攻击。
自己也会受到少许伤害。', null),
       (1275, 6000, null, 80, 'tr87', '招式记录８７', null, '像钢钻一样，
一边旋转身体一边撞击对手。
容易击中要害。', null),
       (1276, 6000, null, 10, 'tr88', '招式记录８８', null, '用燃烧的身体
撞向对手进行攻击。
自己比对手越重，威力越大。', null),
       (1277, 16000, null, 110, 'tr89', '招式记录８９', null, '用强烈的风
席卷对手进行攻击。
有时会使对手混乱。', null),
       (1278, 10000, null, 90, 'tr90', '招式记录９０', null, '与对手嬉闹并攻击。
有时会降低对手的攻击。', null),
       (1279, 4000, null, 10, 'tr91', '招式记录９１', null, '将特殊的毒液泼向对手。
对处于中毒状态的对手，
其攻击、特攻和速度都会降低。', null),
       (1280, 6000, null, 80, 'tr92', '招式记录９２', null, '向对手发射强光，
并给予伤害。', null),
       (1281, 10000, null, 85, 'tr93', '招式记录９３', null, '旋转双臂打向对手。
无视对手的能力变化，
直接给予伤害。', null),
       (1282, 10000, null, 95, 'tr94', '招式记录９４', null, '使出全身力量，
猛攻对手。', null),
       (1283, 6000, null, 80, 'tr95', '招式记录９５', null, '受到此招式攻击的对手，
会因为地狱般的痛苦，在２回合内，
变得无法使出声音类招式。', null),
       (1284, 10000, null, 90, 'tr96', '招式记录９６', null, '对敌人使用是会爆炸的团子。
对我方使用则是给予回复的团子。', null),
       (1285, 10000, null, 85, 'tr97', '招式记录９７', null, '利用精神力量咬住对手进行攻击。
还可以破坏光墙和反射壁等。', null),
       (1286, 6000, null, 85, 'tr98', '招式记录９８', null, '用水之力量
撞向对手进行攻击。
有时会降低对手的防御。', null),
       (1287, 6000, null, 80, 'tr99', '招式记录９９', null, '用身体撞向对手进行攻击。
防御越高，
给予的伤害就越高。', null),
       (1288, 10000, null, null, 'tm00', '招式学习器００', null, '用充满力量的拳头攻击对手。', null),
       (1289, 20, null, 10, 'lonely-mint', '怕寂寞薄荷', null, '宝可梦闻了这种薄荷之后，
攻击会易于提高，
而防御则难以提高。', null),
       (1290, 20, null, 10, 'adamant-mint', '固执薄荷', null, '宝可梦闻了这种薄荷之后，
攻击会易于提高，
而特攻则难以提高。', null),
       (1291, 20, null, 10, 'naughty-mint', '顽皮薄荷', null, '宝可梦闻了这种薄荷之后，
会令攻击容易提高，
而特防则难以提高。', null),
       (1292, 20, null, 10, 'brave-mint', '勇敢薄荷', null, '宝可梦闻了这种薄荷之后，
会令攻击容易提高，
而速度则难以提高。', null),
       (1293, 20, null, 10, 'bold-mint', '大胆薄荷', null, '宝可梦闻了这种薄荷之后，
会令防御容易提高，
而攻击则难以提高。', null),
       (1294, 20, null, 10, 'impish-mint', '淘气薄荷', null, '宝可梦闻了这种薄荷之后，
会令防御容易提高，
而特攻则难以提高。', null),
       (1295, 20, null, 10, 'lax-mint', '乐天薄荷', null, '宝可梦闻了这种薄荷之后，
会令防御容易提高，
而特防则难以提高。', null),
       (1296, 20, null, 10, 'relaxed-mint', '悠闲薄荷', null, '宝可梦闻了这种薄荷之后，
会令防御容易提高，
而速度则难以提高。', null),
       (1297, 20, null, 10, 'modest-mint', '内敛薄荷', null, '宝可梦闻了这种薄荷之后，
会令特攻容易提高，
而攻击则难以提高。', null),
       (1298, 20, null, 10, 'mild-mint', '慢吞吞薄荷', null, '宝可梦闻了这种薄荷之后，
会令特攻容易提高，
而防御则难以提高。', null),
       (1299, 20, null, 10, 'rash-mint', '马虎薄荷', null, '宝可梦闻了这种薄荷之后，
会令特攻容易提高，
而特防则难以提高。', null),
       (1300, 20, null, 10, 'quiet-mint', '冷静薄荷', null, '宝可梦闻了这种薄荷之后，
会令特攻容易提高，
而速度则难以提高。', null),
       (1301, 20, null, 10, 'calm-mint', '温和薄荷', null, '宝可梦闻了这种薄荷之后，
会令特防容易提高，
而攻击则难以提高。', null),
       (1302, 20, null, 10, 'gentle-mint', '温顺薄荷', null, '宝可梦闻了这种薄荷之后，
会令特防容易提高，
而防御则难以提高。', null),
       (1303, 20, null, 10, 'careful-mint', '慎重薄荷', null, '宝可梦闻了这种薄荷之后，
会令特防容易提高，
而特攻则难以提高。', null),
       (1304, 20, null, 10, 'sassy-mint', '自大薄荷', null, '宝可梦闻了这种薄荷之后，
会令特防容易提高，
而速度则难以提高。', null),
       (1305, 20, null, 10, 'timid-mint', '胆小薄荷', null, '宝可梦闻了这种薄荷之后，
会令速度容易提高，
而攻击则难以提高。', null),
       (1306, 20, null, 10, 'hasty-mint', '急躁薄荷', null, '宝可梦闻了这种薄荷之后，
会令速度容易提高，
而防御则难以提高。', null),
       (1307, 20, null, 10, 'jolly-mint', '爽朗薄荷', null, '宝可梦闻了这种薄荷之后，
会令速度容易提高，
而特攻则难以提高。', null),
       (1308, 20, null, 10, 'naive-mint', '天真薄荷', null, '宝可梦闻了这种薄荷之后，
会令速度容易提高，
而特防则难以提高。', null),
       (1309, 20, null, 10, 'serious-mint', '认真薄荷', null, '宝可梦闻了这种薄荷之后，
攻击、防御、速度、
特防、特攻能全方位提高。', null),
       (1310, 20, null, 50, 'wishing-piece', '许愿星块', null, '投掷到宝可梦的巢穴里，
就会有极巨化宝可梦出现。', null),
       (1311, 1600, null, 80, 'cracked-pot', '破裂的茶壶', null, '这个神奇的茶壶
可以使特定的宝可梦进化。
虽然有破损，但泡出来的茶依旧清香。', null),
       (1312, 38000, null, 80, 'chipped-pot', '缺损的茶壶', null, '这个神奇的茶壶
可以使特定的宝可梦进化。
虽然有缺陷，但泡出来的茶依旧清香。', null),
       (1313, 0, null, null, 'hi-tech-earbuds', '厉害耳塞', null, '这个神奇又厉害的耳塞
可以自由调节各种声音的音量。', null),
       (1314, 2200, null, 10, 'fruit-bunch', '袋装果实', null, '在露营营地制作料理时会使用的
一种食材。煮得粘稠浓厚的水果，
能为料理增添一份热带风情。', null),
       (1315, 2200, null, 20, 'moomoo-cheese', '哞哞乳酪', null, '在露营营地制作料理时会使用的
一种食材。融化的芝士
能为咖喱增添一份独特的浓郁。', null),
       (1316, 400, null, 50, 'spice-mix', '香料组合', null, '在露营营地制作料理时会使用的
一种食材。加入了超过５０种
香辛料调配而成，香辣十足。', null),
       (1317, 950, null, 10, 'fresh-cream', '鲜鲜奶油', null, '在露营营地制作料理时会使用的
一种食材。在辣味的咖喱中
加入奶油，使滋味变得香甜可口。', null),
       (1318, 950, null, 10, 'packaged-curry', '即食咖喱', null, '在露营营地制作料理时会使用的
一种食材。使用这种成品速食咖喱，
可以有效预防失误。', null),
       (1319, 950, null, 10, 'coconut-milk', '椰奶', null, '在露营营地制作料理时会使用的
一种食材。椰奶那种与众不同的
清甜滋味非常受欢迎。', null),
       (1320, 150, null, 10, 'instant-noodles', '即食面', null, '在露营营地制作料理时会使用的
一种食材。没想到加入速食食品，
滋味也出乎意料地和谐。', null),
       (1321, 150, null, 10, 'precooked-burger', '即食肉排', null, '在露营营地制作料理时会使用的
一种食材。不知道该加什么的时候，
选择这种汉堡肉准没错。', null),
       (1322, 15000, null, 50, 'gigantamix', '超极粉', null, '在露营营地制作料理时会使用的食材。
只要把这种神奇的香辛料往咖喱上
轻轻一撒，咖喱就会变得巨大无比。', null),
       (1323, 0, null, null, 'wishing-chip', '许愿星碎片', null, '投掷数个到宝可梦的巢穴里，
可能就会有极巨化宝可梦出现。', null),
       (1324, 0, null, null, 'rotom-bike--water-mode', '洛托姆自行车', null, '自行车和宝可梦洛托姆组合后，
不仅可以通过涡轮发动机加速，
而且可以在水面上骑行。', null),
       (1325, 0, null, null, 'catching-charm', '防晃护符', null, '带上这个神奇的
可增强稳定性的护身符，
就能更容易触发会心捕捉。', null),
       (1326, 0, null, null, 'old-letter', '陈旧的信', null, '女孩子投递的书信
收件人是男孩子。
散发出一种怀旧的气息。', null),
       (1327, 0, null, null, 'band-autograph', '乐队的签名', null, '伽勒尔地区的标志性乐队
“马西马赛”
全体成员的亲笔签名。', null),
       (1328, 0, null, null, 'sonias-book', '索妮亚的书', null, '索妮亚博士出版的书。
以风趣的文笔展示了
关于伽勒尔地区传说的最新发现。', null),
       (1329, 0, null, null, 'rotom-catalog', '洛托姆型录', null, '上面记载着洛托姆喜欢的家电。
通过使用这本指南，可以
让洛托姆潜入或脱离家电。', null),
       (1690, 500, null, null, 'rock-tera-shard', '岩石太晶碎块', null, null, null),
       (1330, 20, null, null, 'dynamax-crystal-and458', '★And458', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1331, 20, null, null, 'dynamax-crystal-and15', '★And15', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1332, 20, null, null, 'dynamax-crystal-and337', '★And337', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1333, 20, null, null, 'dynamax-crystal-and603', '★And603', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1334, 20, null, null, 'dynamax-crystal-and390', '★And390', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1335, 20, null, null, 'dynamax-crystal-sgr6879', '★Sgr6879', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1336, 20, null, null, 'dynamax-crystal-sgr6859', '★Sgr6859', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1337, 20, null, null, 'dynamax-crystal-sgr6913', '★Sgr6913', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1338, 20, null, null, 'dynamax-crystal-sgr7348', '★Sgr7348', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1339, 20, null, null, 'dynamax-crystal-sgr7121', '★Sgr7121', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1340, 20, null, null, 'dynamax-crystal-sgr6746', '★Sgr6746', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1341, 20, null, null, 'dynamax-crystal-sgr7194', '★Sgr7194', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1342, 20, null, null, 'dynamax-crystal-sgr7337', '★Sgr7337', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1343, 20, null, null, 'dynamax-crystal-sgr7343', '★Sgr7343', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1344, 20, null, null, 'dynamax-crystal-sgr6812', '★Sgr6812', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1345, 20, null, null, 'dynamax-crystal-sgr7116', '★Sgr7116', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1346, 20, null, null, 'dynamax-crystal-sgr7264', '★Sgr7264', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1347, 20, null, null, 'dynamax-crystal-sgr7597', '★Sgr7597', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1348, 20, null, null, 'dynamax-crystal-del7882', '★Del7882', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1349, 20, null, null, 'dynamax-crystal-del7906', '★Del7906', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1350, 20, null, null, 'dynamax-crystal-del7852', '★Del7852', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1351, 20, null, null, 'dynamax-crystal-psc596', '★Psc596', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1352, 20, null, null, 'dynamax-crystal-psc361', '★Psc361', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1353, 20, null, null, 'dynamax-crystal-psc510', '★Psc510', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1354, 20, null, null, 'dynamax-crystal-psc437', '★Psc437', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1355, 20, null, null, 'dynamax-crystal-psc8773', '★Psc8773', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1356, 20, null, null, 'dynamax-crystal-lep1865', '★Lep1865', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1357, 20, null, null, 'dynamax-crystal-lep1829', '★Lep1829', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1358, 20, null, null, 'dynamax-crystal-boo5340', '★Boo5340', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1359, 20, null, null, 'dynamax-crystal-boo5506', '★Boo5506', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1360, 20, null, null, 'dynamax-crystal-boo5435', '★Boo5435', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1361, 20, null, null, 'dynamax-crystal-boo5602', '★Boo5602', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1362, 20, null, null, 'dynamax-crystal-boo5733', '★Boo5733', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1363, 20, null, null, 'dynamax-crystal-boo5235', '★Boo5235', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1364, 20, null, null, 'dynamax-crystal-boo5351', '★Boo5351', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1365, 20, null, null, 'dynamax-crystal-hya3748', '★Hya3748', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1366, 20, null, null, 'dynamax-crystal-hya3903', '★Hya3903', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1367, 20, null, null, 'dynamax-crystal-hya3418', '★Hya3418', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1368, 20, null, null, 'dynamax-crystal-hya3482', '★Hya3482', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1369, 20, null, null, 'dynamax-crystal-hya3845', '★Hya3845', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1370, 20, null, null, 'dynamax-crystal-eri1084', '★Eri1084', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1371, 20, null, null, 'dynamax-crystal-eri472', '★Eri472', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1372, 20, null, null, 'dynamax-crystal-eri1666', '★Eri1666', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1373, 20, null, null, 'dynamax-crystal-eri897', '★Eri897', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1374, 20, null, null, 'dynamax-crystal-eri1231', '★Eri1231', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1375, 20, null, null, 'dynamax-crystal-eri874', '★Eri874', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1376, 20, null, null, 'dynamax-crystal-eri1298', '★Eri1298', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1377, 20, null, null, 'dynamax-crystal-eri1325', '★Eri1325', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1378, 20, null, null, 'dynamax-crystal-eri984', '★Eri984', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1691, 500, null, null, 'ghost-tera-shard', '幽灵太晶碎块', null, null, null),
       (1379, 20, null, null, 'dynamax-crystal-eri1464', '★Eri1464', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1380, 20, null, null, 'dynamax-crystal-eri1393', '★Eri1393', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1381, 20, null, null, 'dynamax-crystal-eri850', '★Eri850', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1382, 20, null, null, 'dynamax-crystal-tau1409', '★Tau1409', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1383, 20, null, null, 'dynamax-crystal-tau1457', '★Tau1457', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1384, 20, null, null, 'dynamax-crystal-tau1165', '★Tau1165', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1385, 20, null, null, 'dynamax-crystal-tau1791', '★Tau1791', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1386, 20, null, null, 'dynamax-crystal-tau1910', '★Tau1910', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1387, 20, null, null, 'dynamax-crystal-tau1346', '★Tau1346', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1388, 20, null, null, 'dynamax-crystal-tau1373', '★Tau1373', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1389, 20, null, null, 'dynamax-crystal-tau1412', '★Tau1412', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1390, 20, null, null, 'dynamax-crystal-cma2491', '★CMa2491', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1391, 20, null, null, 'dynamax-crystal-cma2693', '★CMa2693', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1392, 20, null, null, 'dynamax-crystal-cma2294', '★CMa2294', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1393, 20, null, null, 'dynamax-crystal-cma2827', '★CMa2827', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1394, 20, null, null, 'dynamax-crystal-cma2282', '★CMa2282', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1395, 20, null, null, 'dynamax-crystal-cma2618', '★CMa2618', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1396, 20, null, null, 'dynamax-crystal-cma2657', '★CMa2657', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1397, 20, null, null, 'dynamax-crystal-cma2646', '★CMa2646', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1398, 20, null, null, 'dynamax-crystal-uma4905', '★UMa4905', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1399, 20, null, null, 'dynamax-crystal-uma4301', '★UMa4301', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1400, 20, null, null, 'dynamax-crystal-uma5191', '★UMa5191', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1401, 20, null, null, 'dynamax-crystal-uma5054', '★UMa5054', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1402, 20, null, null, 'dynamax-crystal-uma4295', '★UMa4295', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1403, 20, null, null, 'dynamax-crystal-uma4660', '★UMa4660', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1404, 20, null, null, 'dynamax-crystal-uma4554', '★UMa4554', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1405, 20, null, null, 'dynamax-crystal-uma4069', '★UMa4069', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1406, 20, null, null, 'dynamax-crystal-uma3569', '★UMa3569', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1407, 20, null, null, 'dynamax-crystal-uma3323', '★UMa3323', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1408, 20, null, null, 'dynamax-crystal-uma4033', '★UMa4033', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1409, 20, null, null, 'dynamax-crystal-uma4377', '★UMa4377', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1410, 20, null, null, 'dynamax-crystal-uma4375', '★UMa4375', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1411, 20, null, null, 'dynamax-crystal-uma4518', '★UMa4518', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1412, 20, null, null, 'dynamax-crystal-uma3594', '★UMa3594', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1413, 20, null, null, 'dynamax-crystal-vir5056', '★Vir5056', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1414, 20, null, null, 'dynamax-crystal-vir4825', '★Vir4825', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1415, 20, null, null, 'dynamax-crystal-vir4932', '★Vir4932', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1416, 20, null, null, 'dynamax-crystal-vir4540', '★Vir4540', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1417, 20, null, null, 'dynamax-crystal-vir4689', '★Vir4689', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1418, 20, null, null, 'dynamax-crystal-vir5338', '★Vir5338', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1419, 20, null, null, 'dynamax-crystal-vir4910', '★Vir4910', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1420, 20, null, null, 'dynamax-crystal-vir5315', '★Vir5315', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1421, 20, null, null, 'dynamax-crystal-vir5359', '★Vir5359', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1422, 20, null, null, 'dynamax-crystal-vir5409', '★Vir5409', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1423, 20, null, null, 'dynamax-crystal-vir5107', '★Vir5107', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1424, 20, null, null, 'dynamax-crystal-ari617', '★Ari617', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1425, 20, null, null, 'dynamax-crystal-ari553', '★Ari553', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1426, 20, null, null, 'dynamax-crystal-ari546', '★Ari546', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1427, 20, null, null, 'dynamax-crystal-ari951', '★Ari951', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1692, 500, null, null, 'dragon-tera-shard', '龙太晶碎块', null, null, null),
       (1428, 20, null, null, 'dynamax-crystal-ori1713', '★Ori1713', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1429, 20, null, null, 'dynamax-crystal-ori2061', '★Ori2061', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1430, 20, null, null, 'dynamax-crystal-ori1790', '★Ori1790', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1431, 20, null, null, 'dynamax-crystal-ori1903', '★Ori1903', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1432, 20, null, null, 'dynamax-crystal-ori1948', '★Ori1948', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1433, 20, null, null, 'dynamax-crystal-ori2004', '★Ori2004', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1434, 20, null, null, 'dynamax-crystal-ori1852', '★Ori1852', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1435, 20, null, null, 'dynamax-crystal-ori1879', '★Ori1879', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1436, 20, null, null, 'dynamax-crystal-ori1899', '★Ori1899', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1437, 20, null, null, 'dynamax-crystal-ori1543', '★Ori1543', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1438, 20, null, null, 'dynamax-crystal-cas21', '★Cas21', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1439, 20, null, null, 'dynamax-crystal-cas168', '★Cas168', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1440, 20, null, null, 'dynamax-crystal-cas403', '★Cas403', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1441, 20, null, null, 'dynamax-crystal-cas153', '★Cas153', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1442, 20, null, null, 'dynamax-crystal-cas542', '★Cas542', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1443, 20, null, null, 'dynamax-crystal-cas219', '★Cas219', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1444, 20, null, null, 'dynamax-crystal-cas265', '★Cas265', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1445, 20, null, null, 'dynamax-crystal-cnc3572', '★Cnc3572', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1446, 20, null, null, 'dynamax-crystal-cnc3208', '★Cnc3208', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1447, 20, null, null, 'dynamax-crystal-cnc3461', '★Cnc3461', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1448, 20, null, null, 'dynamax-crystal-cnc3449', '★Cnc3449', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1449, 20, null, null, 'dynamax-crystal-cnc3429', '★Cnc3429', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1450, 20, null, null, 'dynamax-crystal-cnc3627', '★Cnc3627', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1451, 20, null, null, 'dynamax-crystal-cnc3268', '★Cnc3268', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1452, 20, null, null, 'dynamax-crystal-cnc3249', '★Cnc3249', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1453, 20, null, null, 'dynamax-crystal-com4968', '★Com4968', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1454, 20, null, null, 'dynamax-crystal-crv4757', '★Crv4757', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1455, 20, null, null, 'dynamax-crystal-crv4623', '★Crv4623', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1456, 20, null, null, 'dynamax-crystal-crv4662', '★Crv4662', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1457, 20, null, null, 'dynamax-crystal-crv4786', '★Crv4786', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1458, 20, null, null, 'dynamax-crystal-aur1708', '★Aur1708', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1459, 20, null, null, 'dynamax-crystal-aur2088', '★Aur2088', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1460, 20, null, null, 'dynamax-crystal-aur1605', '★Aur1605', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1461, 20, null, null, 'dynamax-crystal-aur2095', '★Aur2095', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1462, 20, null, null, 'dynamax-crystal-aur1577', '★Aur1577', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1463, 20, null, null, 'dynamax-crystal-aur1641', '★Aur1641', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1464, 20, null, null, 'dynamax-crystal-aur1612', '★Aur1612', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1465, 20, null, null, 'dynamax-crystal-pav7790', '★Pav7790', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1466, 20, null, null, 'dynamax-crystal-cet911', '★Cet911', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1467, 20, null, null, 'dynamax-crystal-cet681', '★Cet681', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1468, 20, null, null, 'dynamax-crystal-cet188', '★Cet188', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1469, 20, null, null, 'dynamax-crystal-cet539', '★Cet539', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1470, 20, null, null, 'dynamax-crystal-cet804', '★Cet804', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1471, 20, null, null, 'dynamax-crystal-cep8974', '★Cep8974', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1472, 20, null, null, 'dynamax-crystal-cep8162', '★Cep8162', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1473, 20, null, null, 'dynamax-crystal-cep8238', '★Cep8238', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1474, 20, null, null, 'dynamax-crystal-cep8417', '★Cep8417', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1475, 20, null, null, 'dynamax-crystal-cen5267', '★Cen5267', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1476, 20, null, null, 'dynamax-crystal-cen5288', '★Cen5288', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1693, 500, null, null, 'dark-tera-shard', '恶太晶碎块', null, null, null),
       (1477, 20, null, null, 'dynamax-crystal-cen551', '★Cen551', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1478, 20, null, null, 'dynamax-crystal-cen5459', '★Cen5459', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1479, 20, null, null, 'dynamax-crystal-cen5460', '★Cen5460', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1480, 20, null, null, 'dynamax-crystal-cmi2943', '★CMi2943', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1481, 20, null, null, 'dynamax-crystal-cmi2845', '★CMi2845', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1482, 20, null, null, 'dynamax-crystal-equ8131', '★Equ8131', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1483, 20, null, null, 'dynamax-crystal-vul7405', '★Vul7405', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1484, 20, null, null, 'dynamax-crystal-umi424', '★UMi424', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1485, 20, null, null, 'dynamax-crystal-umi5563', '★UMi5563', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1486, 20, null, null, 'dynamax-crystal-umi5735', '★UMi5735', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1487, 20, null, null, 'dynamax-crystal-umi6789', '★UMi6789', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1488, 20, null, null, 'dynamax-crystal-crt4287', '★Crt4287', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1489, 20, null, null, 'dynamax-crystal-lyr7001', '★Lyr7001', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1490, 20, null, null, 'dynamax-crystal-lyr7178', '★Lyr7178', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1491, 20, null, null, 'dynamax-crystal-lyr7106', '★Lyr7106', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1492, 20, null, null, 'dynamax-crystal-lyr7298', '★Lyr7298', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1493, 20, null, null, 'dynamax-crystal-ara6585', '★Ara6585', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1494, 20, null, null, 'dynamax-crystal-sco6134', '★Sco6134', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1495, 20, null, null, 'dynamax-crystal-sco6527', '★Sco6527', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1496, 20, null, null, 'dynamax-crystal-sco6553', '★Sco6553', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1497, 20, null, null, 'dynamax-crystal-sco5953', '★Sco5953', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1498, 20, null, null, 'dynamax-crystal-sco5984', '★Sco5984', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1499, 20, null, null, 'dynamax-crystal-sco6508', '★Sco6508', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1500, 20, null, null, 'dynamax-crystal-sco6084', '★Sco6084', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1501, 20, null, null, 'dynamax-crystal-sco5944', '★Sco5944', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1502, 20, null, null, 'dynamax-crystal-sco6630', '★Sco6630', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1503, 20, null, null, 'dynamax-crystal-sco6027', '★Sco6027', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1504, 20, null, null, 'dynamax-crystal-sco6247', '★Sco6247', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1505, 20, null, null, 'dynamax-crystal-sco6252', '★Sco6252', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1506, 20, null, null, 'dynamax-crystal-sco5928', '★Sco5928', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1507, 20, null, null, 'dynamax-crystal-sco6241', '★Sco6241', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1508, 20, null, null, 'dynamax-crystal-sco6165', '★Sco6165', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1509, 20, null, null, 'dynamax-crystal-tri544', '★Tri544', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1510, 20, null, null, 'dynamax-crystal-leo3982', '★Leo3982', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1511, 20, null, null, 'dynamax-crystal-leo4534', '★Leo4534', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1512, 20, null, null, 'dynamax-crystal-leo4357', '★Leo4357', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1513, 20, null, null, 'dynamax-crystal-leo4057', '★Leo4057', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1514, 20, null, null, 'dynamax-crystal-leo4359', '★Leo4359', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1515, 20, null, null, 'dynamax-crystal-leo4031', '★Leo4031', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1516, 20, null, null, 'dynamax-crystal-leo3852', '★Leo3852', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1517, 20, null, null, 'dynamax-crystal-leo3905', '★Leo3905', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1518, 20, null, null, 'dynamax-crystal-leo3773', '★Leo3773', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1519, 20, null, null, 'dynamax-crystal-gru8425', '★Gru8425', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1520, 20, null, null, 'dynamax-crystal-gru8636', '★Gru8636', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1521, 20, null, null, 'dynamax-crystal-gru8353', '★Gru8353', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1522, 20, null, null, 'dynamax-crystal-lib5685', '★Lib5685', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1523, 20, null, null, 'dynamax-crystal-lib5531', '★Lib5531', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1524, 20, null, null, 'dynamax-crystal-lib5787', '★Lib5787', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1525, 20, null, null, 'dynamax-crystal-lib5603', '★Lib5603', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1694, 500, null, null, 'steel-tera-shard', '钢太晶碎块', null, null, null),
       (1526, 20, null, null, 'dynamax-crystal-pup3165', '★Pup3165', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1527, 20, null, null, 'dynamax-crystal-pup3185', '★Pup3185', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1528, 20, null, null, 'dynamax-crystal-pup3045', '★Pup3045', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1529, 20, null, null, 'dynamax-crystal-cyg7924', '★Cyg7924', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1530, 20, null, null, 'dynamax-crystal-cyg7417', '★Cyg7417', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1531, 20, null, null, 'dynamax-crystal-cyg7796', '★Cyg7796', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1532, 20, null, null, 'dynamax-crystal-cyg8301', '★Cyg8301', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1533, 20, null, null, 'dynamax-crystal-cyg7949', '★Cyg7949', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1534, 20, null, null, 'dynamax-crystal-cyg7528', '★Cyg7528', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1535, 20, null, null, 'dynamax-crystal-oct7228', '★Oct7228', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1536, 20, null, null, 'dynamax-crystal-col1956', '★Col1956', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1537, 20, null, null, 'dynamax-crystal-col2040', '★Col2040', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1538, 20, null, null, 'dynamax-crystal-col2177', '★Col2177', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1539, 20, null, null, 'dynamax-crystal-gem2990', '★Gem2990', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1540, 20, null, null, 'dynamax-crystal-gem2891', '★Gem2891', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1541, 20, null, null, 'dynamax-crystal-gem2421', '★Gem2421', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1542, 20, null, null, 'dynamax-crystal-gem2473', '★Gem2473', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1543, 20, null, null, 'dynamax-crystal-gem2216', '★Gem2216', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1544, 20, null, null, 'dynamax-crystal-gem2777', '★Gem2777', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1545, 20, null, null, 'dynamax-crystal-gem2650', '★Gem2650', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1546, 20, null, null, 'dynamax-crystal-gem2286', '★Gem2286', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1547, 20, null, null, 'dynamax-crystal-gem2484', '★Gem2484', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1548, 20, null, null, 'dynamax-crystal-gem2930', '★Gem2930', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1549, 20, null, null, 'dynamax-crystal-peg8775', '★Peg8775', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1550, 20, null, null, 'dynamax-crystal-peg8781', '★Peg8781', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1551, 20, null, null, 'dynamax-crystal-peg39', '★Peg39', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1552, 20, null, null, 'dynamax-crystal-peg8308', '★Peg8308', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1553, 20, null, null, 'dynamax-crystal-peg8650', '★Peg8650', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1554, 20, null, null, 'dynamax-crystal-peg8634', '★Peg8634', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1555, 20, null, null, 'dynamax-crystal-peg8684', '★Peg8684', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1556, 20, null, null, 'dynamax-crystal-peg8450', '★Peg8450', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1557, 20, null, null, 'dynamax-crystal-peg8880', '★Peg8880', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1558, 20, null, null, 'dynamax-crystal-peg8905', '★Peg8905', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1559, 20, null, null, 'dynamax-crystal-oph6556', '★Oph6556', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1560, 20, null, null, 'dynamax-crystal-oph6378', '★Oph6378', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1561, 20, null, null, 'dynamax-crystal-oph6603', '★Oph6603', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1562, 20, null, null, 'dynamax-crystal-oph6149', '★Oph6149', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1563, 20, null, null, 'dynamax-crystal-oph6056', '★Oph6056', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1564, 20, null, null, 'dynamax-crystal-oph6075', '★Oph6075', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1565, 20, null, null, 'dynamax-crystal-ser5854', '★Ser5854', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1566, 20, null, null, 'dynamax-crystal-ser7141', '★Ser7141', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1567, 20, null, null, 'dynamax-crystal-ser5879', '★Ser5879', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1568, 20, null, null, 'dynamax-crystal-her6406', '★Her6406', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1569, 20, null, null, 'dynamax-crystal-her6148', '★Her6148', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1570, 20, null, null, 'dynamax-crystal-her6410', '★Her6410', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1571, 20, null, null, 'dynamax-crystal-her6526', '★Her6526', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1572, 20, null, null, 'dynamax-crystal-her6117', '★Her6117', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1573, 20, null, null, 'dynamax-crystal-her6008', '★Her6008', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1574, 20, null, null, 'dynamax-crystal-per936', '★Per936', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1695, 500, null, null, 'fairy-tera-shard', '妖精太晶碎块', null, null, null),
       (1575, 20, null, null, 'dynamax-crystal-per1017', '★Per1017', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1576, 20, null, null, 'dynamax-crystal-per1131', '★Per1131', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1577, 20, null, null, 'dynamax-crystal-per1228', '★Per1228', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1578, 20, null, null, 'dynamax-crystal-per834', '★Per834', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1579, 20, null, null, 'dynamax-crystal-per941', '★Per941', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1580, 20, null, null, 'dynamax-crystal-phe99', '★Phe99', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1581, 20, null, null, 'dynamax-crystal-phe338', '★Phe338', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1582, 20, null, null, 'dynamax-crystal-vel3634', '★Vel3634', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1583, 20, null, null, 'dynamax-crystal-vel3485', '★Vel3485', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1584, 20, null, null, 'dynamax-crystal-vel3734', '★Vel3734', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1585, 20, null, null, 'dynamax-crystal-aqr8232', '★Aqr8232', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1586, 20, null, null, 'dynamax-crystal-aqr8414', '★Aqr8414', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1587, 20, null, null, 'dynamax-crystal-aqr8709', '★Aqr8709', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1588, 20, null, null, 'dynamax-crystal-aqr8518', '★Aqr8518', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1589, 20, null, null, 'dynamax-crystal-aqr7950', '★Aqr7950', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1590, 20, null, null, 'dynamax-crystal-aqr8499', '★Aqr8499', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1591, 20, null, null, 'dynamax-crystal-aqr8610', '★Aqr8610', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1592, 20, null, null, 'dynamax-crystal-aqr8264', '★Aqr8264', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1593, 20, null, null, 'dynamax-crystal-cru4853', '★Cru4853', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1594, 20, null, null, 'dynamax-crystal-cru4730', '★Cru4730', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1595, 20, null, null, 'dynamax-crystal-cru4763', '★Cru4763', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1596, 20, null, null, 'dynamax-crystal-cru4700', '★Cru4700', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1597, 20, null, null, 'dynamax-crystal-cru4656', '★Cru4656', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1598, 20, null, null, 'dynamax-crystal-psa8728', '★PsA8728', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1599, 20, null, null, 'dynamax-crystal-tra6217', '★TrA6217', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1600, 20, null, null, 'dynamax-crystal-cap7776', '★Cap7776', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1601, 20, null, null, 'dynamax-crystal-cap7754', '★Cap7754', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1602, 20, null, null, 'dynamax-crystal-cap8278', '★Cap8278', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1603, 20, null, null, 'dynamax-crystal-cap8322', '★Cap8322', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1604, 20, null, null, 'dynamax-crystal-cap7773', '★Cap7773', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1605, 20, null, null, 'dynamax-crystal-sge7479', '★Sge7479', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1606, 20, null, null, 'dynamax-crystal-car2326', '★Car2326', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1607, 20, null, null, 'dynamax-crystal-car3685', '★Car3685', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1608, 20, null, null, 'dynamax-crystal-car3307', '★Car3307', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1609, 20, null, null, 'dynamax-crystal-car3699', '★Car3699', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1610, 20, null, null, 'dynamax-crystal-dra5744', '★Dra5744', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1611, 20, null, null, 'dynamax-crystal-dra5291', '★Dra5291', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1612, 20, null, null, 'dynamax-crystal-dra6705', '★Dra6705', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1613, 20, null, null, 'dynamax-crystal-dra6536', '★Dra6536', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1614, 20, null, null, 'dynamax-crystal-dra7310', '★Dra7310', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1615, 20, null, null, 'dynamax-crystal-dra6688', '★Dra6688', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1616, 20, null, null, 'dynamax-crystal-dra4434', '★Dra4434', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1617, 20, null, null, 'dynamax-crystal-dra6370', '★Dra6370', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1618, 20, null, null, 'dynamax-crystal-dra7462', '★Dra7462', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1619, 20, null, null, 'dynamax-crystal-dra6396', '★Dra6396', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1620, 20, null, null, 'dynamax-crystal-dra6132', '★Dra6132', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1621, 20, null, null, 'dynamax-crystal-dra6636', '★Dra6636', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1622, 20, null, null, 'dynamax-crystal-cvn4915', '★CVn4915', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1623, 20, null, null, 'dynamax-crystal-cvn4785', '★CVn4785', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1696, 0, null, null, 'booster-energy', '驱劲能量', null, null, null),
       (1624, 20, null, null, 'dynamax-crystal-cvn4846', '★CVn4846', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1625, 20, null, null, 'dynamax-crystal-aql7595', '★Aql7595', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1626, 20, null, null, 'dynamax-crystal-aql7557', '★Aql7557', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1627, 20, null, null, 'dynamax-crystal-aql7525', '★Aql7525', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1628, 20, null, null, 'dynamax-crystal-aql7602', '★Aql7602', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1629, 20, null, null, 'dynamax-crystal-aql7235', '★Aql7235', null, '使用后的当天，
旷野地带的瞭望塔巨巢中
会出现[VAR (0000)]。', null),
       (1630, 4000, null, 30, 'max-honey', '极巨甜蜜', null, '极巨化的蜂女王制造出的蜜汁。
放进极巨汤里，就能让汤的味道变得很温和。
与活力块具有相同的效果。', null),
       (1631, 6000, null, 30, 'max-mushrooms', '极巨菇菇', null, '拥有某种神奇力量的蘑菇，
能改变宝可梦极巨化的样子。
在战斗中，可以提高宝可梦所有的能力。', null),
       (1632, 20, null, 30, 'galarica-twig', '伽勒豆蔻枝', null, '伽勒尔部分地区生长的
伽勒豆蔻这种植物的枝条。
可以用来制作某种宝可梦的饰品。', null),
       (1633, 3000, null, 30, 'galarica-cuff', '伽勒豆蔻手环', null, '用伽勒豆蔻枝编织的手环。
给伽勒尔地区的呆呆兽戴上，
它就会很开心。', null),
       (1634, 0, null, null, 'style-card', '时尚名人卡', null, '拥有后，在时装店可以买到的物品
和在美发沙龙可以选择的发型
都会有所增加的卡片。', null),
       (1635, 0, null, null, 'armor-pass', '铠甲车票', null, '前往铠岛的车票。
在木杆镇的车站，
把它出示给站务员看吧。', null),
       (1636, 0, null, null, 'rotom-bike--sparkling-white', '洛托姆自行车', null, '能借助洛托姆的力量进行涡轮加速，
且可在水面上骑行的自行车。
洁白耀眼的闪亮白。', null),
       (1637, 0, null, null, 'rotom-bike--glistening-black', '洛托姆自行车', null, '能借助洛托姆的力量进行涡轮加速，
且可在水面上骑行的自行车。
闪烁着红黑色光泽的炫耀黑。', null),
       (1638, 0, null, null, 'exp-charm', '经验护符', null, '训练家拥有这个护符后，
宝可梦获得的经验值就会增加。
里面装有类似于机器的部件。', null),
       (1639, 20, null, 30, 'armorite-ore', '铠甲矿石', null, '能够在铠岛找出的珍稀石头。
把它送给铠岛的收藏家，
就会有好事发生。', null),
       (1640, 0, null, null, 'mark-charm', '证章护符', null, '神奇而富有个性的护符。
拥有该护符后，
会更容易遇到带有证章的宝可梦。', null),
       (1641, 0, null, null, 'reins-of-unity--merge', '牵绊缰绳', null, '人们曾经用来进献给国王的缰绳。
可增强丰饶之力，
并使蕾冠王与爱马合二为一。', null),
       (1642, 0, null, null, 'reins-of-unity--split', '牵绊缰绳', null, '人们曾经用来进献给国王的缰绳。
通过缰绳连接的二者可以相互交流。
可使蕾冠王与爱马一分为二。', null),
       (1643, 3000, null, 30, 'galarica-wreath', '伽勒豆蔻花圈', null, '用伽勒豆蔻枝编织的花环头饰。
给伽勒尔地区的呆呆兽戴上这种头饰，
它就会很开心。', null),
       (1644, 0, null, null, 'legendary-clue-1', '传说笔记１', null, '皮欧尼整理的
有关丰饶之王传说的笔记。
上面贴着很旧的照片。', null),
       (1645, 0, null, null, 'legendary-clue-2', '传说笔记２', null, '皮欧尼整理的
有关巨人传说的笔记。
上面绘有充满个性的画。', null),
       (1646, 0, null, null, 'legendary-clue-3', '传说笔记３', null, '皮欧尼整理的
有关鸟宝可梦传说的笔记。
上面绘有充满个性的画。', null),
       (1647, 0, null, null, 'legendary-clue-question', '传说笔记？', null, '皮欧尼掉落的神秘笔记。
上面贴有极巨巢穴
出现光洞的照片。', null),
       (1648, 0, null, null, 'crown-pass', '王冠车票', null, '前往王冠雪原的车票。
在木杆镇的车站，
把它出示给站务员看吧。', null),
       (1649, 0, null, null, 'wooden-crown', '木雕王冠', null, '用木头雕制而成的神秘物体。
上面有看起来缺了什么的痕迹。
可能是其他物品的一部分。', null),
       (1650, 0, null, null, 'radiant-petal', '光辉花瓣', null, '从丰饶之王那里
得到的一片花瓣。
散发着朦胧的光辉。', null),
       (1651, 0, null, null, 'white-mane-hair', '白色鬃毛', null, '丰饶之王的爱马
雪暴马的鬃毛。
既结实又柔韧。', null),
       (1652, 0, null, null, 'black-mane-hair', '黑色鬃毛', null, '丰饶之王的爱马
灵幽马的鬃毛。
摸起来非常柔顺。', null),
       (1653, 0, null, null, 'iceroot-carrot', '冰萝卜', null, '丰饶之王的爱马
喜欢吃的农作物。
生长于白雪皑皑的田地。', null),
       (1654, 0, null, null, 'shaderoot-carrot', '黑萝卜', null, '丰饶之王的爱马
喜欢吃的农作物。
生长于光线暗淡的田地。', null),
       (1655, 20, null, 30, 'dynite-ore', '极矿石', null, '能够在极巨巢穴找出的神秘石头。
把它送给王冠雪原的收藏家，
就会有好事发生。', null),
       (1656, 20, null, null, 'carrot-seeds', '萝卜种子', null, '种在田地里使用的种子。
出产的萝卜种类会根据
种下的田地而有所不同。', null),
       (1657, 20, null, null, 'ability-patch', '特性膏药', null, '可以改变宝可梦特性的膏药。
给拥有常见特性的宝可梦使用后，
其特性会转变为稀有特性。', null),
       (1658, 0, null, null, 'reins-of-unity', '牵绊缰绳', null, '拿到光下即可生辉的布。
人们曾经将其进献给
丰饶之王以表感谢之意。', null),
       (1659, 0, null, null, 'adamant-crystal', '大金刚宝玉', null, null, null),
       (1660, 0, null, null, 'lustrous-globe', '大白宝玉', null, null, null),
       (1661, 0, null, null, 'griseous-core', '大白金宝玉', null, null, null),
       (1662, 0, null, null, 'blank-plate', '净空石板', null, null, null),
       (1663, 0, null, null, 'strange-ball', '奇异球', null, null, null),
       (1664, 0, null, null, 'legend-plate', '传说石板', null, null, null),
       (1665, 0, null, null, 'rotom-phone', '手机洛托姆', null, null, null),
       (1666, 0, null, null, 'sandwich', '三明治', null, null, null),
       (1667, 0, null, null, 'koraidons-poke-ball', '故勒顿的球', null, null, null),
       (1668, 0, null, null, 'miraidons-poke-ball', '密勒顿的球', null, null, null),
       (1669, 0, null, null, 'tera-orb', '太晶珠', null, null, null),
       (1670, 0, null, null, 'scarlet-book', '朱之书', null, null, null),
       (1671, 0, null, null, 'violet-book', '紫之书', null, null, null),
       (1672, 0, null, null, 'kofu’s-wallet', '海岱的钱包', null, null, null),
       (1673, 375, null, null, 'tiny-bamboo-shoot', '小竹笋', null, null, null),
       (1674, 1500, null, null, 'big-bamboo-shoot', '大竹笋', null, null, null),
       (1675, 0, null, null, 'scroll-of-darkness', '恶之挂轴', null, null, null),
       (1676, 0, null, null, 'scroll-of-waters', '水之挂轴', null, null, null),
       (1677, 750, null, null, 'malicious-armor', '咒术之铠', null, null, null),
       (1678, 500, null, null, 'normal-tera-shard', '一般太晶碎块', null, null, null),
       (1679, 500, null, null, 'fire-tera-shard', '火太晶碎块', null, null, null),
       (1680, 500, null, null, 'water-tera-shard', '水太晶碎块', null, null, null),
       (1681, 500, null, null, 'electric-tera-shard', '电太晶碎块', null, null, null),
       (1699, 30000, null, null, 'mirror-herb', '模仿香草', null, null, null),
       (1700, 15000, null, null, 'punching-glove', '拳击手套', null, null, null),
       (1701, 20000, null, null, 'covert-cloak', '密探斗篷', null, null, null),
       (1702, 20000, null, null, 'loaded-dice', '机变骰子', null, null, null),
       (1703, 0, null, null, 'baguette', '长棍面包', null, null, null),
       (1704, 120, null, null, 'mayonnaise', '蛋黄酱', null, null, null),
       (1705, 110, null, null, 'ketchup', '番茄酱', null, null, null),
       (1706, 130, null, null, 'mustard', '黄芥末酱', null, null, null),
       (1707, 250, null, null, 'butter', '黄油', null, null, null),
       (1708, 300, null, null, 'peanut-butter', '花生酱', null, null, null),
       (1709, 320, null, null, 'chili-sauce', '辣酱', null, null, null),
       (1710, 90, null, null, 'salt', '盐', null, null, null),
       (1711, 100, null, null, 'pepper', '胡椒', null, null, null),
       (1712, 140, null, null, 'yogurt', '酸奶', null, null, null),
       (1713, 200, null, null, 'whipped-cream', '鲜奶油', null, null, null),
       (1714, 280, null, null, 'cream-cheese', '奶油芝士', null, null, null),
       (1715, 120, null, null, 'jam', '莓果酱', null, null, null),
       (1716, 260, null, null, 'marmalade', '橘子酱', null, null, null),
       (1717, 300, null, null, 'olive-oil', '橄榄油', null, null, null),
       (1718, 300, null, null, 'vinegar', '醋', null, null, null),
       (1719, 0, null, null, 'sweet-herba-mystica', '秘传：甜味料', null, null, null),
       (1720, 0, null, null, 'salty-herba-mystica', '秘传：咸味料', null, null, null),
       (1721, 0, null, null, 'sour-herba-mystica', '秘传：酸味料', null, null, null),
       (1722, 0, null, null, 'bitter-herba-mystica', '秘传：苦味料', null, null, null),
       (1723, 0, null, null, 'spicy-herba-mystica', '秘传：辣味料', null, null, null),
       (1724, 90, null, null, 'lettuce', '生菜', null, null, null),
       (1725, 100, null, null, 'tomato', '西红柿片', null, null, null),
       (1726, 120, null, null, 'cherry-tomatoes', '小西红柿块', null, null, null),
       (1727, 130, null, null, 'cucumber', '小黄瓜片', null, null, null),
       (1728, 90, null, null, 'pickle', '酸黄瓜片', null, null, null),
       (1729, 130, null, null, 'onion', '洋葱片', null, null, null),
       (1730, 230, null, null, 'red-onion', '红洋葱', null, null, null),
       (1731, 230, null, null, 'green-bell-pepper', '青椒片', null, null, null),
       (1732, 240, null, null, 'red-bell-pepper', '红椒片', null, null, null),
       (1733, 240, null, null, 'yellow-bell-pepper', '黄椒片', null, null, null),
       (1734, 180, null, null, 'avocado', '牛油果', null, null, null),
       (1735, 150, null, null, 'bacon', '煎培根', null, null, null),
       (1736, 170, null, null, 'ham', '火腿片', null, null, null),
       (1737, 200, null, null, 'prosciutto', '生火腿', null, null, null),
       (1738, 150, null, null, 'chorizo', '煎辣香肠', null, null, null),
       (1739, 400, null, null, 'herbed-sausage', '香草香肠', null, null, null),
       (1740, 380, null, null, 'hamburger', '汉堡排', null, null, null),
       (1741, 500, null, null, 'klawf-stick', '毛崖蟹棒', null, null, null),
       (1742, 330, null, null, 'smoked-fillet', '烟熏鱼片', null, null, null),
       (1743, 360, null, null, 'fried-fillet', '炸鱼片', null, null, null),
       (1744, 80, null, null, 'egg', '水煮蛋片', null, null, null),
       (1745, 250, null, null, 'potato-tortilla', '烘蛋', null, null, null),
       (1746, 260, null, null, 'tofu', '豆腐', null, null, null),
       (1747, 280, null, null, 'rice', '米饭', null, null, null),
       (1748, 280, null, null, 'noodles', '面条', null, null, null),
       (1749, 110, null, null, 'potato-salad', '土豆色拉', null, null, null),
       (1750, 120, null, null, 'cheese', '芝士片', null, null, null),
       (1751, 80, null, null, 'banana', '香蕉片', null, null, null),
       (1752, 140, null, null, 'strawberry', '草莓片', null, null, null),
       (1753, 130, null, null, 'apple', '苹果片', null, null, null),
       (1754, 180, null, null, 'kiwi', '奇异果片', null, null, null),
       (1755, 250, null, null, 'pineapple', '凤梨片', null, null, null),
       (1756, 220, null, null, 'jalapeño', '小辣椒', null, null, null),
       (1757, 410, null, null, 'horseradish', '辣根', null, null, null),
       (1758, 450, null, null, 'curry-powder', '咖喱粉', null, null, null),
       (1759, 330, null, null, 'wasabi', '芥末酱', null, null, null),
       (1760, 270, null, null, 'watercress', '豆瓣菜', null, null, null),
       (1761, 280, null, null, 'basil', '罗勒', null, null, null),
       (1762, 80, null, null, 'venonat-fang', '毛球的牙', null, null, null),
       (1763, 80, null, null, 'diglett-dirt', '地鼠的土', null, null, null),
       (1764, 80, null, null, 'meowth-fur', '喵喵的毛', null, null, null),
       (1765, 60, null, null, 'psyduck-down', '可达鸭的羽绒', null, null, null),
       (1766, 80, null, null, 'mankey-fur', '猴怪的毛', null, null, null),
       (1767, 80, null, null, 'growlithe-fur', '卡蒂狗的毛', null, null, null),
       (1768, 80, null, null, 'slowpoke-claw', '呆呆兽的爪子', null, null, null),
       (1769, 80, null, null, 'magnemite-screw', '小磁怪的螺丝', null, null, null),
       (1770, 80, null, null, 'grimer-toxin', '臭泥的毒素', null, null, null),
       (1771, 140, null, null, 'shellder-pearl', '大舌贝的珍珠', null, null, null),
       (1772, 80, null, null, 'gastly-gas', '鬼斯的气体', null, null, null),
       (1773, 60, null, null, 'drowzee-fur', '催眠貘的毛', null, null, null),
       (1774, 80, null, null, 'voltorb-sparks', '霹雳电球的电力', null, null, null),
       (1775, 100, null, null, 'scyther-claw', '飞天螳螂的爪子', null, null, null),
       (1776, 100, null, null, 'tauros-hair', '肯泰罗的毛', null, null, null),
       (1777, 40, null, null, 'magikarp-scales', '鲤鱼王的鳞片', null, null, null),
       (1778, 300, null, null, 'ditto-goo', '百变怪的黏糊', null, null, null),
       (1779, 200, null, null, 'eevee-fur', '伊布的毛', null, null, null),
       (1780, 200, null, null, 'dratini-scales', '迷你龙的鳞片', null, null, null),
       (1781, 100, null, null, 'pichu-fur', '皮丘的毛', null, null, null),
       (1782, 60, null, null, 'igglybuff-fluff', '宝宝丁的毛', null, null, null),
       (1783, 60, null, null, 'mareep-wool', '咩利羊的毛球', null, null, null),
       (1784, 60, null, null, 'hoppip-leaf', '毽子草的叶片', null, null, null),
       (1785, 60, null, null, 'sunkern-leaf', '向日种子的叶片', null, null, null),
       (1786, 100, null, null, 'murkrow-bauble', '黑暗鸦的宝物', null, null, null),
       (1787, 80, null, null, 'misdreavus-tears', '梦妖的眼泪', null, null, null),
       (1788, 100, null, null, 'girafarig-fur', '麒麟奇的毛', null, null, null),
       (1789, 80, null, null, 'pineco-husk', '榛果球的壳', null, null, null),
       (1790, 120, null, null, 'dunsparce-scales', '土龙弟弟的鳞片', null, null, null),
       (1791, 80, null, null, 'qwilfish-spines', '千针鱼的刺', null, null, null),
       (1792, 100, null, null, 'heracross-claw', '赫拉克罗斯的爪子', null, null, null),
       (1793, 80, null, null, 'sneasel-claw', '狃拉的爪子', null, null, null),
       (1794, 80, null, null, 'teddiursa-claw', '熊宝宝的爪子', null, null, null),
       (1795, 240, null, null, 'delibird-parcel', '信使鸟的行李', null, null, null),
       (1796, 80, null, null, 'houndour-fang', '戴鲁比的牙', null, null, null),
       (1797, 80, null, null, 'phanpy-nail', '小小象的爪子', null, null, null),
       (1798, 120, null, null, 'stantler-hair', '惊角鹿的毛', null, null, null),
       (1799, 200, null, null, 'larvitar-claw', '幼基拉斯的爪子', null, null, null),
       (1800, 60, null, null, 'wingull-feather', '长翅鸥的羽毛', null, null, null),
       (1801, 80, null, null, 'ralts-dust', '拉鲁拉丝的掉落物', null, null, null),
       (1802, 60, null, null, 'surskit-syrup', '溜溜糖球的蜜', null, null, null),
       (1803, 80, null, null, 'shroomish-spores', '蘑蘑菇的孢子', null, null, null),
       (1804, 60, null, null, 'slakoth-fur', '懒人獭的毛', null, null, null),
       (1805, 80, null, null, 'makuhita-sweat', '幕下力士的汗', null, null, null),
       (1806, 60, null, null, 'azurill-fur', '露力丽的毛', null, null, null),
       (1807, 200, null, null, 'sableye-gem', '勾魂眼的宝石', null, null, null),
       (1808, 80, null, null, 'meditite-sweat', '玛沙那的汗', null, null, null),
       (1809, 80, null, null, 'gulpin-mucus', '溶食兽的黏液', null, null, null),
       (1810, 80, null, null, 'numel-lava', '呆火驼的熔岩', null, null, null),
       (1811, 140, null, null, 'torkoal-coal', '煤炭龟的煤炭', null, null, null),
       (1812, 140, null, null, 'spoink-pearl', '跳跳猪的珍珠', null, null, null),
       (1813, 80, null, null, 'cacnea-needle', '刺球仙人掌的刺', null, null, null),
       (1814, 100, null, null, 'swablu-fluff', '青绵鸟的羽毛', null, null, null),
       (1815, 120, null, null, 'zangoose-claw', '猫鼬斩的爪子', null, null, null),
       (1816, 120, null, null, 'seviper-fang', '饭匙蛇的牙', null, null, null),
       (1817, 60, null, null, 'barboach-slime', '泥泥鳅的黏液', null, null, null),
       (1818, 80, null, null, 'shuppet-scrap', '怨影娃娃的残片', null, null, null),
       (1819, 140, null, null, 'tropius-leaf', '热带龙的叶片', null, null, null),
       (1820, 80, null, null, 'snorunt-fur', '雪童子的毛', null, null, null),
       (1821, 160, null, null, 'luvdisc-scales', '爱心鱼的鳞片', null, null, null),
       (1822, 200, null, null, 'bagon-scales', '宝贝龙的鳞片', null, null, null),
       (1823, 60, null, null, 'starly-feather', '姆克儿的羽毛', null, null, null),
       (1824, 40, null, null, 'kricketot-shell', '圆法师的蜕壳', null, null, null),
       (1825, 60, null, null, 'shinx-fang', '小猫怪的牙', null, null, null),
       (1826, 100, null, null, 'combee-honey', '三蜜蜂的蜜', null, null, null),
       (1827, 100, null, null, 'pachirisu-fur', '帕奇利兹的毛', null, null, null),
       (1828, 80, null, null, 'buizel-fur', '泳圈鼬的毛', null, null, null),
       (1829, 80, null, null, 'shellos-mucus', '无壳海兔的黏液', null, null, null),
       (1830, 80, null, null, 'drifloon-gas', '飘飘球的气体', null, null, null),
       (1831, 80, null, null, 'stunky-fur', '臭鼬噗的毛', null, null, null),
       (1832, 80, null, null, 'bronzor-fragment', '铜镜怪的碎片', null, null, null),
       (1833, 80, null, null, 'bonsly-tears', '盆才怪的眼泪', null, null, null),
       (1834, 300, null, null, 'happiny-dust', '小福蛋的掉落物', null, null, null),
       (1835, 240, null, null, 'spiritomb-fragment', '花岩怪的碎片', null, null, null),
       (1836, 200, null, null, 'gible-scales', '圆陆鲨的鳞片', null, null, null),
       (1837, 160, null, null, 'riolu-fur', '利欧路的毛', null, null, null),
       (1838, 80, null, null, 'hippopotas-sand', '沙河马的沙', null, null, null),
       (1839, 80, null, null, 'croagunk-poison', '不良蛙的毒素', null, null, null),
       (1840, 80, null, null, 'finneon-scales', '荧光鱼的鳞片', null, null, null),
       (1841, 60, null, null, 'snover-berries', '雪笠怪的果实', null, null, null),
       (1842, 200, null, null, 'rotom-sparks', '洛托姆的电力', null, null, null),
       (1843, 60, null, null, 'petilil-leaf', '百合根娃娃的叶片', null, null, null),
       (1844, 80, null, null, 'basculin-fang', '野蛮鲈鱼的牙', null, null, null),
       (1845, 80, null, null, 'sandile-claw', '黑眼鳄的爪子', null, null, null),
       (1846, 140, null, null, 'zorua-fur', '索罗亚的毛', null, null, null),
       (1847, 80, null, null, 'gothita-eyelash', '哥德宝宝的睫毛', null, null, null),
       (1848, 60, null, null, 'deerling-hair', '四季鹿的毛', null, null, null),
       (1849, 80, null, null, 'foongus-spores', '哎呀球菇的孢子', null, null, null),
       (1850, 120, null, null, 'alomomola-mucus', '保姆曼波的黏液', null, null, null),
       (1851, 80, null, null, 'tynamo-slime', '麻麻小鱼的黏液', null, null, null),
       (1852, 200, null, null, 'axew-scales', '牙牙的鳞片', null, null, null),
       (1853, 80, null, null, 'cubchoo-fur', '喷嚏熊的毛', null, null, null),
       (1854, 100, null, null, 'cryogonal-ice', '几何雪花的冰', null, null, null),
       (1855, 80, null, null, 'pawniard-blade', '驹刀小兵的刀', null, null, null),
       (1856, 80, null, null, 'rufflet-feather', '毛头小鹰的羽毛', null, null, null),
       (1857, 200, null, null, 'deino-scales', '单首龙的鳞片', null, null, null),
       (1858, 200, null, null, 'larvesta-fuzz', '燃烧虫的毛', null, null, null),
       (1859, 60, null, null, 'fletchling-feather', '小箭雀的羽毛', null, null, null),
       (1860, 40, null, null, 'scatterbug-powder', '粉蝶虫的粉', null, null, null),
       (1861, 80, null, null, 'litleo-tuft', '小狮狮的鬃毛', null, null, null),
       (1862, 80, null, null, 'flabebe-pollen', '花蓓蓓的花粉', null, null, null),
       (1863, 80, null, null, 'skiddo-leaf', '坐骑小羊的叶片', null, null, null),
       (1864, 100, null, null, 'skrelp-kelp', '垃垃藻的碎藻', null, null, null),
       (1865, 100, null, null, 'clauncher-claw', '铁臂枪虾的钳子', null, null, null),
       (1866, 100, null, null, 'hawlucha-down', '摔角鹰人的羽绒', null, null, null),
       (1867, 100, null, null, 'dedenne-fur', '咚咚鼠的毛', null, null, null),
       (1868, 140, null, null, 'goomy-goo', '黏黏宝的黏液', null, null, null),
       (1869, 100, null, null, 'klefki-key', '钥圈儿的钥匙', null, null, null),
       (1870, 80, null, null, 'bergmite-ice', '冰宝的冰', null, null, null),
       (1871, 80, null, null, 'noibat-fur', '嗡蝠的毛', null, null, null),
       (1872, 60, null, null, 'yungoos-fur', '猫鼬少的毛', null, null, null),
       (1873, 100, null, null, 'crabrawler-shell', '好胜蟹的壳', null, null, null),
       (1874, 100, null, null, 'oricorio-feather', '花舞鸟的羽毛', null, null, null),
       (1875, 80, null, null, 'rockruff-rock', '岩狗狗的岩石', null, null, null),
       (1876, 80, null, null, 'mareanie-spike', '好坏星的刺', null, null, null),
       (1877, 80, null, null, 'mudbray-mud', '泥驴仔的泥巴', null, null, null),
       (1878, 80, null, null, 'fomantis-leaf', '伪螳草的叶片', null, null, null),
       (1879, 80, null, null, 'salandit-gas', '夜盗火蜥的气体', null, null, null),
       (1880, 80, null, null, 'bounsweet-sweat', '甜竹竹的汗', null, null, null),
       (1881, 120, null, null, 'oranguru-fur', '智挥猩的毛', null, null, null),
       (1882, 120, null, null, 'passimian-fur', '投掷猴的毛', null, null, null),
       (1883, 80, null, null, 'sandygast-sand', '沙丘娃的沙', null, null, null),
       (1884, 100, null, null, 'komala-claw', '树枕尾熊的爪子', null, null, null),
       (1885, 40, null, null, 'mimikyu-scrap', '谜拟丘的残片', null, null, null),
       (1886, 100, null, null, 'bruxish-tooth', '磨牙彩皮鱼的牙齿', null, null, null),
       (1887, 80, null, null, 'chewtle-claw', '咬咬龟的爪子', null, null, null),
       (1888, 60, null, null, 'skwovet-fur', '贪心栗鼠的毛', null, null, null),
       (1889, 80, null, null, 'arrokuda-scales', '刺梭鱼的鳞片', null, null, null),
       (1890, 80, null, null, 'rookidee-feather', '稚山雀的羽毛', null, null, null),
       (1891, 80, null, null, 'toxel-sparks', '电音婴的电力', null, null, null),
       (1892, 120, null, null, 'falinks-sweat', '列阵兵的汗', null, null, null),
       (1893, 80, null, null, 'cufant-tarnish', '铜象的锈', null, null, null),
       (1894, 80, null, null, 'rolycoly-coal', '小炭仔的煤炭', null, null, null),
       (1895, 80, null, null, 'silicobra-sand', '沙包蛇的沙', null, null, null),
       (1896, 120, null, null, 'indeedee-fur', '爱管侍的毛', null, null, null),
       (1897, 100, null, null, 'pincurchin-spines', '啪嚓海胆的针', null, null, null),
       (1898, 80, null, null, 'snom-thread', '雪吞虫的丝', null, null, null),
       (1899, 80, null, null, 'impidimp-hair', '捣蛋小妖的毛', null, null, null),
       (1900, 100, null, null, 'applin-juice', '啃果虫的果汁', null, null, null),
       (1901, 80, null, null, 'sinistea-chip', '来悲茶的碎片', null, null, null),
       (1902, 80, null, null, 'hatenna-dust', '迷布莉姆的掉落物', null, null, null),
       (1903, 120, null, null, 'stonjourner-stone', '巨石丁的岩石', null, null, null),
       (1904, 120, null, null, 'eiscue-down', '冰砌鹅的羽绒', null, null, null),
       (1905, 200, null, null, 'dreepy-powder', '多龙梅西亚的粉', null, null, null),
       (1906, 60, null, null, 'lechonk-hair', '爱吃豚的毛', null, null, null),
       (1907, 60, null, null, 'tarountula-thread', '团珠蛛的丝', null, null, null),
       (1908, 60, null, null, 'nymble-claw', '豆蟋蟀的爪子', null, null, null),
       (1909, 80, null, null, 'rellor-mud', '虫滚泥的泥巴', null, null, null),
       (1910, 80, null, null, 'greavard-wax', '墓仔狗的蜡', null, null, null),
       (1911, 80, null, null, 'flittle-down', '飘飘雏的羽绒', null, null, null),
       (1912, 80, null, null, 'wiglett-sand', '海地鼠的沙', null, null, null),
       (1913, 160, null, null, 'dondozo-whisker', '吃吼霸的胡须', null, null, null),
       (1914, 140, null, null, 'veluza-fillet', '轻身鳕的鱼片', null, null, null),
       (1915, 100, null, null, 'finizen-mucus', '波普海豚的黏液', null, null, null),
       (1916, 80, null, null, 'smoliv-oil', '迷你芙的油', null, null, null),
       (1917, 80, null, null, 'capsakid-seed', '热辣娃的种子', null, null, null),
       (1918, 80, null, null, 'tadbulb-mucus', '光蚪仔的黏液', null, null, null),
       (1919, 80, null, null, 'varoom-fume', '噗隆隆的气体', null, null, null),
       (1920, 200, null, null, 'orthworm-tarnish', '拖拖蚓的锈', null, null, null),
       (1921, 80, null, null, 'tandemaus-fur', '一对鼠的毛', null, null, null),
       (1922, 80, null, null, 'cetoddle-grease', '走鲸的油脂', null, null, null),
       (1923, 200, null, null, 'frigibax-scales', '凉脊龙的鳞片', null, null, null),
       (1924, 200, null, null, 'tatsugiri-scales', '米立龙的鳞片', null, null, null),
       (1925, 120, null, null, 'cyclizar-scales', '摩托蜥的鳞片', null, null, null),
       (1926, 100, null, null, 'pawmi-fur', '布拨的毛', null, null, null),
       (1927, 120, null, null, 'wattrel-feather', '电海燕的羽毛', null, null, null),
       (1928, 200, null, null, 'bombirdier-feather', '下石鸟的羽毛', null, null, null),
       (1929, 80, null, null, 'squawkabilly-feather', '怒鹦哥的羽毛', null, null, null),
       (1930, 80, null, null, 'flamigo-down', '缠红鹤的羽绒', null, null, null),
       (1931, 200, null, null, 'klawf-claw', '毛崖蟹的钳子', null, null, null),
       (1932, 80, null, null, 'nacli-salt', '盐石宝的盐', null, null, null),
       (1933, 120, null, null, 'glimmet-crystal', '晶光芽的结晶', null, null, null),
       (1934, 80, null, null, 'shroodle-ink', '滋汁鼹的墨汁', null, null, null),
       (1935, 80, null, null, 'fidough-fur', '狗仔包的毛', null, null, null),
       (1936, 80, null, null, 'maschiff-fang', '偶叫獒的牙', null, null, null),
       (1937, 80, null, null, 'bramblin-twig', '纳噬草的枝条', null, null, null),
       (1938, 800, null, null, 'gimmighoul-coin', '索财灵的硬币', null, null, null),
       (1939, 80, null, null, 'tinkatink-hair', '小锻匠的毛', null, null, null),
       (1940, 160, null, null, 'charcadet-soot', '炭小侍的煤灰', null, null, null),
       (1941, 100, null, null, 'toedscool-flaps', '原野水母的薄片', null, null, null),
       (1942, 60, null, null, 'wooper-slime', '乌波的黏液', null, null, null),
       (1943, 0, null, null, 'tm100', '招式学习器１００', null, null, null),
       (1944, 0, null, null, 'tm101', '招式学习器１０１', null, null, null),
       (1945, 0, null, null, 'tm102', '招式学习器１０２', null, null, null),
       (1946, 0, null, null, 'tm103', '招式学习器１０３', null, null, null),
       (1947, 0, null, null, 'tm104', '招式学习器１０４', null, null, null),
       (1948, 0, null, null, 'tm105', '招式学习器１０５', null, null, null),
       (1949, 0, null, null, 'tm106', '招式学习器１０６', null, null, null),
       (1950, 0, null, null, 'tm107', '招式学习器１０７', null, null, null),
       (1951, 0, null, null, 'tm108', '招式学习器１０８', null, null, null),
       (1952, 0, null, null, 'tm109', '招式学习器１０９', null, null, null),
       (1953, 0, null, null, 'tm110', '招式学习器１１０', null, null, null),
       (1954, 0, null, null, 'tm111', '招式学习器１１１', null, null, null),
       (1955, 0, null, null, 'tm112', '招式学习器１１２', null, null, null),
       (1956, 0, null, null, 'tm113', '招式学习器１１３', null, null, null),
       (1957, 0, null, null, 'tm114', '招式学习器１１４', null, null, null),
       (1958, 0, null, null, 'tm115', '招式学习器１１５', null, null, null),
       (1959, 0, null, null, 'tm116', '招式学习器１１６', null, null, null),
       (1960, 0, null, null, 'tm117', '招式学习器１１７', null, null, null),
       (1961, 0, null, null, 'tm118', '招式学习器１１８', null, null, null),
       (1962, 0, null, null, 'tm119', '招式学习器１１９', null, null, null),
       (1963, 0, null, null, 'tm120', '招式学习器１２０', null, null, null),
       (1964, 0, null, null, 'tm121', '招式学习器１２１', null, null, null),
       (1965, 0, null, null, 'tm122', '招式学习器１２２', null, null, null),
       (1966, 0, null, null, 'tm123', '招式学习器１２３', null, null, null),
       (1967, 0, null, null, 'tm124', '招式学习器１２４', null, null, null),
       (1968, 0, null, null, 'tm125', '招式学习器１２５', null, null, null),
       (1969, 0, null, null, 'tm126', '招式学习器１２６', null, null, null),
       (1970, 0, null, null, 'tm127', '招式学习器１２７', null, null, null),
       (1971, 0, null, null, 'tm128', '招式学习器１２８', null, null, null),
       (1972, 0, null, null, 'tm129', '招式学习器１２９', null, null, null),
       (1973, 0, null, null, 'tm130', '招式学习器１３０', null, null, null),
       (1974, 0, null, null, 'tm131', '招式学习器１３１', null, null, null),
       (1975, 0, null, null, 'tm132', '招式学习器１３２', null, null, null),
       (1976, 0, null, null, 'tm133', '招式学习器１３３', null, null, null),
       (1977, 0, null, null, 'tm134', '招式学习器１３４', null, null, null),
       (1978, 0, null, null, 'tm135', '招式学习器１３５', null, null, null),
       (1979, 0, null, null, 'tm136', '招式学习器１３６', null, null, null),
       (1980, 0, null, null, 'tm137', '招式学习器１３７', null, null, null),
       (1981, 0, null, null, 'tm138', '招式学习器１３８', null, null, null),
       (1982, 0, null, null, 'tm139', '招式学习器１３９', null, null, null),
       (1983, 0, null, null, 'tm140', '招式学习器１４０', null, null, null),
       (1984, 0, null, null, 'tm141', '招式学习器１４１', null, null, null),
       (1985, 0, null, null, 'tm142', '招式学习器１４２', null, null, null),
       (1986, 0, null, null, 'tm143', '招式学习器１４３', null, null, null),
       (1987, 0, null, null, 'tm144', '招式学习器１４４', null, null, null),
       (1988, 0, null, null, 'tm145', '招式学习器１４５', null, null, null),
       (1989, 0, null, null, 'tm146', '招式学习器１４６', null, null, null),
       (1990, 0, null, null, 'tm147', '招式学习器１４７', null, null, null),
       (1991, 0, null, null, 'tm148', '招式学习器１４８', null, null, null),
       (1992, 0, null, null, 'tm149', '招式学习器１４９', null, null, null),
       (1993, 0, null, null, 'tm150', '招式学习器１５０', null, null, null),
       (1994, 0, null, null, 'tm151', '招式学习器１５１', null, null, null),
       (1995, 0, null, null, 'tm152', '招式学习器１５２', null, null, null),
       (1996, 0, null, null, 'tm153', '招式学习器１５３', null, null, null),
       (1997, 0, null, null, 'tm154', '招式学习器１５４', null, null, null),
       (1998, 0, null, null, 'tm155', '招式学习器１５５', null, null, null),
       (1999, 0, null, null, 'tm156', '招式学习器１５６', null, null, null),
       (2000, 0, null, null, 'tm157', '招式学习器１５７', null, null, null),
       (2001, 0, null, null, 'tm158', '招式学习器１５８', null, null, null),
       (2002, 0, null, null, 'tm159', '招式学习器１５９', null, null, null),
       (2003, 0, null, null, 'tm160', '招式学习器１６０', null, null, null),
       (2004, 0, null, null, 'tm161', '招式学习器１６１', null, null, null),
       (2005, 0, null, null, 'tm162', '招式学习器１６２', null, null, null),
       (2006, 0, null, null, 'tm163', '招式学习器１６３', null, null, null),
       (2007, 0, null, null, 'tm164', '招式学习器１６４', null, null, null),
       (2008, 0, null, null, 'tm165', '招式学习器１６５', null, null, null),
       (2009, 0, null, null, 'tm166', '招式学习器１６６', null, null, null),
       (2010, 0, null, null, 'tm167', '招式学习器１６７', null, null, null),
       (2011, 0, null, null, 'tm168', '招式学习器１６８', null, null, null),
       (2012, 0, null, null, 'tm169', '招式学习器１６９', null, null, null),
       (2013, 0, null, null, 'tm170', '招式学习器１７０', null, null, null),
       (2014, 0, null, null, 'tm171', '招式学习器１７１', null, null, null),
       (2015, 0, null, null, 'picnic-set', '野餐组合', null, null, null),
       (2016, 0, null, null, 'academy-bottle', '校园水壶', null, null, null),
       (2018, 1000, null, null, 'polka-dot-bottle', '圆点水壶', null, null, null),
       (2019, 1000, null, null, 'striped-bottle', '条纹水壶', null, null, null),
       (2020, 1000, null, null, 'diamond-bottle', '菱纹水壶', null, null, null),
       (2021, 0, null, null, 'academy-cup', '校园水杯', null, null, null),
       (2023, 800, null, null, 'striped-cup', '直条纹水杯', null, null, null),
       (2024, 800, null, null, 'polka-dot-cup', '圆点水杯', null, null, null),
       (2025, 800, null, null, 'flower-pattern-cup', '花朵水杯', null, null, null),
       (2026, 0, null, null, 'academy-tablecloth', '校园桌巾', null, null, null),
       (2028, 2000, null, null, 'whimsical-tablecloth', '幻彩桌巾', null, null, null),
       (2029, 2000, null, null, 'leafy-tablecloth', '大自然桌巾', null, null, null),
       (2030, 4000, null, null, 'spooky-tablecloth', '鬼祟桌巾', null, null, null),
       (2031, 0, null, null, 'academy-ball', '校园球', null, null, null),
       (2033, 2000, null, null, 'marill-ball', '玛力露球', null, null, null),
       (2034, 1000, null, null, 'yarn-ball', '毛线球', null, null, null),
       (2035, 2000, null, null, 'cyber-ball', '电竞球', null, null, null),
       (2036, 400, null, null, 'gold-pick', '金色三明治签', null, null, null),
       (2037, 40, null, null, 'silver-pick', '银色三明治签', null, null, null),
       (2038, 120, null, null, 'red-flag-pick', '红旗子三明治签', null, null, null),
       (2039, 120, null, null, 'blue-flag-pick', '蓝旗子三明治签', null, null, null),
       (2040, 480, null, null, 'pika-pika-pick', '皮卡丘三明治签', null, null, null),
       (2041, 1200, null, null, 'winking-pika-pick', '眨眼皮卡丘三明治签', null, null, null),
       (2042, 480, null, null, 'vee-vee-pick', '伊布三明治签', null, null, null),
       (2043, 1200, null, null, 'smiling-vee-pick', '笑脸伊布三明治签', null, null, null),
       (2044, 200, null, null, 'blue-poke-ball-pick', '蓝珠珠三明治签', null, null, null),
       (2045, 3000, null, null, 'auspicious-armor', '庆祝之铠', null, null, null),
       (2046, 3000, null, null, 'leader’s-crest', '头领凭证', null, null, null),
       (2047, 2000, null, null, 'pink-bottle', '粉红水壶', null, null, null),
       (2048, 2000, null, null, 'blue-bottle', '蓝色水壶', null, null, null),
       (2049, 2000, null, null, 'yellow-bottle', '黄色水壶', null, null, null),
       (2050, 1500, null, null, 'steel-bottle-(r)', '红纹不锈钢水壶', null, null, null),
       (2051, 1500, null, null, 'steel-bottle-(y)', '黄纹不锈钢水壶', null, null, null),
       (2052, 1500, null, null, 'steel-bottle-(b)', '蓝纹不锈钢水壶', null, null, null),
       (2053, 20000, null, null, 'silver-bottle', '银钛水壶', null, null, null),
       (2054, 800, null, null, 'barred-cup', '横条纹水杯', null, null, null),
       (2055, 800, null, null, 'diamond-pattern-cup', '菱纹水杯', null, null, null),
       (2056, 800, null, null, 'fire-pattern-cup', '火焰水杯', null, null, null),
       (2057, 1500, null, null, 'pink-cup', '粉红水杯', null, null, null),
       (2058, 1500, null, null, 'blue-cup', '蓝色水杯', null, null, null),
       (2059, 1500, null, null, 'yellow-cup', '黄色水杯', null, null, null),
       (2060, 2000, null, null, 'pikachu-cup', '皮卡丘水杯', null, null, null),
       (2061, 2000, null, null, 'eevee-cup', '伊布水杯', null, null, null),
       (2062, 2000, null, null, 'slowpoke-cup', '呆呆兽水杯', null, null, null),
       (2063, 20000, null, null, 'silver-cup', '银钛水杯', null, null, null),
       (2064, 2000, null, null, 'exercise-ball', '瑜珈球', null, null, null),
       (2065, 3000, null, null, 'plaid-tablecloth-(y)', '黄色格子桌巾', null, null, null),
       (2066, 3000, null, null, 'plaid-tablecloth-(b)', '蓝色格子桌巾', null, null, null),
       (2067, 3000, null, null, 'plaid-tablecloth-(r)', '红色格子桌巾', null, null, null),
       (2068, 0, null, null, 'b&w-grass-tablecloth', '黑白草丛桌巾', null, null, null),
       (2069, 4000, null, null, 'battle-tablecloth', '对决桌巾', null, null, null),
       (2070, 2000, null, null, 'monstrous-tablecloth', '怪兽桌巾', null, null, null),
       (2071, 1500, null, null, 'striped-tablecloth', '条纹桌巾', null, null, null),
       (2072, 1500, null, null, 'diamond-tablecloth', '菱纹桌巾', null, null, null),
       (2073, 1500, null, null, 'polka-dot-tablecloth', '圆点桌巾', null, null, null),
       (2074, 1000, null, null, 'lilac-tablecloth', '紫色桌巾', null, null, null),
       (2075, 1000, null, null, 'mint-tablecloth', '浅绿桌巾', null, null, null),
       (2076, 1000, null, null, 'peach-tablecloth', '米色桌巾', null, null, null),
       (2077, 5000, null, null, 'yellow-tablecloth', '黄色桌巾', null, null, null),
       (2078, 5000, null, null, 'blue-tablecloth', '蓝色桌巾', null, null, null),
       (2079, 5000, null, null, 'pink-tablecloth', '粉红桌巾', null, null, null),
       (2080, 30000, null, null, 'gold-bottle', '金钛水壶', null, null, null),
       (2081, 10000, null, null, 'bronze-bottle', '铜钛水壶', null, null, null),
       (2082, 15000, null, null, 'gold-cup', '金钛水杯', null, null, null),
       (2083, 5000, null, null, 'bronze-cup', '铜钛水杯', null, null, null),
       (2084, 200, null, null, 'green-poke-ball-pick', '绿珠珠三明治签', null, null, null),
       (2085, 200, null, null, 'red-poke-ball-pick', '红珠珠三明治签', null, null, null),
       (2086, 1600, null, null, 'party-sparkler-pick', '烟花祝庆三明治签', null, null, null),
       (2087, 2000, null, null, 'heroic-sword-pick', '勇者之剑三明治签', null, null, null),
       (2088, 600, null, null, 'magical-star-pick', '魔法星星三明治签', null, null, null),
       (2089, 600, null, null, 'magical-heart-pick', '魔法甜心三明治签', null, null, null),
       (2090, 800, null, null, 'parasol-pick', '阳伞三明治签', null, null, null),
       (2091, 1000, null, null, 'blue-sky-flower-pick', '晴空花三明治签', null, null, null),
       (2092, 1000, null, null, 'sunset-flower-pick', '夕阳花三明治签', null, null, null),
       (2093, 1000, null, null, 'sunrise-flower-pick', '朝霞花三明治签', null, null, null),
       (2094, 0, null, null, 'blue-dish', '蓝色盘子', null, null, null),
       (2095, 0, null, null, 'green-dish', '绿色盘子', null, null, null),
       (2096, 0, null, null, 'orange-dish', '橘色盘子', null, null, null),
       (2097, 0, null, null, 'red-dish', '红色盘子', null, null, null),
       (2098, 0, null, null, 'white-dish', '白色盘子', null, null, null),
       (2099, 0, null, null, 'yellow-dish', '黄色盘子', null, null, null),
       (2100, 0, null, null, 'roto-stick', 'Roto Stick', null, null, null),
       (2101, 0, null, null, 'teal-style-card', 'Teal Style Card', null, null, null),
       (2102, 0, null, null, 'teal-mask', 'Teal Mask', null, null, null),
       (2103, 0, null, null, 'glimmering-charm', 'Glimmering Charm', null, null, null),
       (2104, 0, null, null, 'crystal-cluster', 'Crystal Cluster', null, null, null),
       (2105, 750, null, null, 'fairy-feather', 'Fairy Feather', null, null, null),
       (2106, 0, null, null, 'wellspring-mask', 'Wellspring Mask', null, null, null),
       (2107, 0, null, null, 'hearthflame-mask', 'Hearthflame Mask', null, null, null),
       (2108, 0, null, null, 'cornerstone-mask', 'Cornerstone Mask', null, null, null),
       (2109, 500, null, null, 'syrupy-apple', 'Syrupy Apple', null, null, null),
       (2110, 400, null, null, 'unremarkable-teacup', 'Unremarkable Teacup', null, null, null),
       (2111, 9500, null, null, 'masterpiece-teacup', 'Masterpiece Teacup', null, null, null),
       (2112, 125, null, null, 'health-mochi', 'Health Mochi', null, null, null),
       (2113, 125, null, null, 'muscle-mochi', 'Muscle Mochi', null, null, null),
       (2114, 125, null, null, 'resist-mochi', 'Resist Mochi', null, null, null),
       (2115, 125, null, null, 'genius-mochi', 'Genius Mochi', null, null, null),
       (2116, 125, null, null, 'clever-mochi', 'Clever Mochi', null, null, null),
       (2117, 125, null, null, 'swift-mochi', 'Swift Mochi', null, null, null),
       (2118, 75, null, null, 'fresh-start-mochi', 'Fresh Start Mochi', null, null, null),
       (2119, 15, null, null, 'ekans-fang', 'Ekans Fang', null, null, null),
       (2120, 10, null, null, 'sandshrew-claw', 'Sandshrew Claw', null, null, null),
       (2121, 25, null, null, 'cleffa-fur', 'Cleffa Fur', null, null, null),
       (2122, 35, null, null, 'vulpix-fur', 'Vulpix Fur', null, null, null),
       (2123, 35, null, null, 'poliwag-slime', 'Poliwag Slime', null, null, null),
       (2124, 25, null, null, 'bellsprout-vine', 'Bellsprout Vine', null, null, null),
       (2125, 30, null, null, 'geodude-fragment', 'Geodude Fragment', null, null, null),
       (2126, 25, null, null, 'koffing-gas', 'Koffing Gas', null, null, null),
       (2127, 40, null, null, 'munchlax-fang', 'Munchlax Fang', null, null, null),
       (2128, 10, null, null, 'sentret-fur', 'Sentret Fur', null, null, null),
       (2129, 15, null, null, 'hoothoot-feather', 'Hoothoot Feather', null, null, null),
       (2130, 10, null, null, 'spinarak-thread', 'Spinarak Thread', null, null, null),
       (2131, 30, null, null, 'aipom-hair', 'Aipom Hair', null, null, null),
       (2132, 35, null, null, 'yanma-spike', 'Yanma Spike', null, null, null),
       (2133, 40, null, null, 'gligar-fang', 'Gligar Fang', null, null, null),
       (2134, 20, null, null, 'slugma-lava', 'Slugma Lava', null, null, null),
       (2135, 35, null, null, 'swinub-hair', 'Swinub Hair', null, null, null),
       (2136, 15, null, null, 'poochyena-fang', 'Poochyena Fang', null, null, null),
       (2137, 20, null, null, 'lotad-leaf', 'Lotad Leaf', null, null, null),
       (2138, 20, null, null, 'seedot-stem', 'Seedot Stem', null, null, null),
       (2139, 40, null, null, 'nosepass-fragment', 'Nosepass Fragment', null, null, null),
       (2140, 10, null, null, 'volbeat-fluid', 'Volbeat Fluid', null, null, null),
       (2141, 10, null, null, 'illumise-fluid', 'Illumise Fluid', null, null, null),
       (2142, 10, null, null, 'corphish-shell', 'Corphish Shell', null, null, null),
       (2143, 35, null, null, 'feebas-scales', 'Feebas Scales', null, null, null),
       (2144, 40, null, null, 'duskull-fragment', 'Duskull Fragment', null, null, null),
       (2145, 20, null, null, 'chingling-fragment', 'Chingling Fragment', null, null, null),
       (2146, 30, null, null, 'timburr-sweat', 'Timburr Sweat', null, null, null),
       (2147, 20, null, null, 'sewaddle-leaf', 'Sewaddle Leaf', null, null, null),
       (2148, 15, null, null, 'ducklett-feather', 'Ducklett Feather', null, null, null),
       (2149, 30, null, null, 'litwick-soot', 'Litwick Soot', null, null, null),
       (2150, 40, null, null, 'mienfoo-claw', 'Mienfoo Claw', null, null, null),
       (2151, 35, null, null, 'vullaby-feather', 'Vullaby Feather', null, null, null),
       (2152, 50, null, null, 'carbink-jewel', 'Carbink Jewel', null, null, null),
       (2153, 20, null, null, 'phantump-twig', 'Phantump Twig', null, null, null),
       (2154, 25, null, null, 'grubbin-thread', 'Grubbin Thread', null, null, null),
       (2155, 25, null, null, 'cutiefly-powder', 'Cutiefly Powder', null, null, null),
       (2156, 50, null, null, 'jangmo-o-scales', 'Jangmo-o Scales', null, null, null),
       (2157, 30, null, null, 'cramorant-down', 'Cramorant Down', null, null, null),
       (2158, 15, null, null, 'morpeko-snack', 'Morpeko Snack', null, null, null),
       (2159, 30, null, null, 'poltchageist-powder', 'Poltchageist Powder', null, null, null),
       (2160, 8000,
        ' The Linking Cord triggers the evolution of certain Pokémon (all of which can alternatively evolve when traded) upon use. This consumes the Linking Cord.',
        null, 'linking-cord', 'Linking Cord',
        'Allows a Pokemon whose evolution is usually triggered by trading to evolve', null, null),
       (2161, 0, null, null, 'tm172', 'TM172', null, null, null),
       (2162, 0, null, null, 'tm173', 'TM173', null, null, null),
       (2163, 0, null, null, 'tm174', 'TM174', null, null, null),
       (2164, 0, null, null, 'tm175', 'TM175', null, null, null),
       (2165, 0, null, null, 'tm176', 'TM176', null, null, null),
       (2166, 0, null, null, 'tm177', 'TM177', null, null, null),
       (2167, 0, null, null, 'tm178', 'TM178', null, null, null),
       (2168, 0, null, null, 'tm179', 'TM179', null, null, null),
       (2169, 0, null, null, 'tm180', 'TM180', null, null, null),
       (2170, 0, null, null, 'tm181', 'TM181', null, null, null),
       (2171, 0, null, null, 'tm182', 'TM182', null, null, null),
       (2172, 0, null, null, 'tm183', 'TM183', null, null, null),
       (2173, 0, null, null, 'tm184', 'TM184', null, null, null),
       (2174, 0, null, null, 'tm185', 'TM185', null, null, null),
       (2175, 0, null, null, 'tm186', 'TM186', null, null, null),
       (2176, 0, null, null, 'tm187', 'TM187', null, null, null),
       (2177, 0, null, null, 'tm188', 'TM188', null, null, null),
       (2178, 0, null, null, 'tm189', 'TM189', null, null, null),
       (2179, 0, null, null, 'tm190', 'TM190', null, null, null),
       (2180, 0, null, null, 'tm191', 'TM191', null, null, null),
       (2181, 0, null, null, 'tm192', 'TM192', null, null, null),
       (2182, 0, null, null, 'tm193', 'TM193', null, null, null),
       (2183, 0, null, null, 'tm194', 'TM194', null, null, null),
       (2184, 0, null, null, 'tm195', 'TM195', null, null, null),
       (2185, 0, null, null, 'tm196', 'TM196', null, null, null),
       (2186, 0, null, null, 'tm197', 'TM197', null, null, null),
       (2187, 0, null, null, 'tm198', 'TM198', null, null, null),
       (2188, 0, null, null, 'tm199', 'TM199', null, null, null),
       (2189, 0, null, null, 'tm200', 'TM200', null, null, null),
       (2190, 0, null, null, 'tm201', 'TM201', null, null, null),
       (2191, 0, null, null, 'tm202', 'TM202', null, null, null),
       (2192, 0, null, null, 'tm203', 'TM203', null, null, null),
       (2193, 0, null, null, 'tm204', 'TM204', null, null, null),
       (2194, 0, null, null, 'tm205', 'TM205', null, null, null),
       (2195, 0, null, null, 'tm206', 'TM206', null, null, null),
       (2196, 0, null, null, 'tm207', 'TM207', null, null, null),
       (2197, 0, null, null, 'tm208', 'TM208', null, null, null),
       (2198, 0, null, null, 'tm209', 'TM209', null, null, null),
       (2199, 0, null, null, 'tm210', 'TM210', null, null, null),
       (2200, 0, null, null, 'tm211', 'TM211', null, null, null),
       (2201, 0, null, null, 'tm212', 'TM212', null, null, null),
       (2202, 0, null, null, 'tm213', 'TM213', null, null, null),
       (2203, 0, null, null, 'tm214', 'TM214', null, null, null),
       (2204, 0, null, null, 'tm215', 'TM215', null, null, null),
       (2205, 0, null, null, 'tm216', 'TM216', null, null, null),
       (2206, 0, null, null, 'tm217', 'TM217', null, null, null),
       (2207, 0, null, null, 'tm218', 'TM218', null, null, null),
       (2208, 0, null, null, 'tm219', 'TM219', null, null, null),
       (2209, 0, null, null, 'tm220', 'TM220', null, null, null),
       (2210, 0, null, null, 'tm221', 'TM221', null, null, null),
       (2211, 0, null, null, 'tm222', 'TM222', null, null, null),
       (2212, 0, null, null, 'tm223', 'TM223', null, null, null),
       (2219, 0, null, null, 'lastrange-ball', 'Strange Ball', null, null, null),
       (2220, 0, null, null, 'lapoke-ball', 'Poké Ball', null, null, null),
       (2221, 0, null, null, 'lagreat-ball', 'Great Ball', null, null, null),
       (2222, 0, null, null, 'laultra-ball', 'Ultra Ball', null, null, null),
       (2223, 0, null, null, 'laheavy-ball', 'Heavy Ball', null, 'Doesn''t fly far, but is more effective if the Pokémon
hasn''t noticed the player.', null),
       (2224, 0, null, null, 'laleaden-ball', 'Leaden Ball', null, 'Upgraded version of the Heavy Ball. Doesn''t fly far,
but is more effective if the Pokémon hasn''t noticed the player.', null),
       (2225, 0, null, null, 'lagigaton-ball', 'Gigaton Ball', null, 'Upgraded version of the Leaden Ball. Doesn''t fly far,
but is more effective if the Pokémon hasn''t noticed the player.', null),
       (2226, 0, null, null, 'lafeather-ball', 'Feather Ball', null, 'Can be thrown further than a regular Poké Ball.
Is more effective for catching Pokémon that fly high in the air.', null),
       (2227, 0, null, null, 'lawing-ball', 'Wing Ball', null, 'Can be thrown further than a Feather Ball.
Is more effective for catching Pokémon that fly high in the air.', null),
       (2228, 0, null, null, 'lajet-ball', 'Jet Ball', null, 'Can be thrown further than a Wing Ball.
Is more effective for catching Pokémon that fly high in the air.', null),
       (2229, 0, null, null, 'laorigin-ball', 'Origin Ball', null, null, null),
       (10001, 0, null, null, 'black-augurite', 'black-augurite', null, null, null),
       (10002, 0, null, null, 'peat-block', 'peat-block', null, null, null);
COMMIT;