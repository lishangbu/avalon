BEGIN;
insert into public.ability (id, effect, internal_name, introduction, name)
values (1, 'This Pokémon''s damaging moves have a 10% chance to make the target flinch with each hit if they do not already cause flinching as a secondary effect.

This ability does not stack with a held item.

Overworld: The wild encounter rate is halved while this Pokémon is first in the party.', 'stench', '通过释放臭臭的气味，
在攻击的时候，
有时会使对手畏缩。', '恶臭'),
       (2, 'The weather changes to rain when this Pokémon enters battle and does not end unless replaced by another weather condition.

If multiple Pokémon with this ability, drought, sand stream, or snow warning are sent out at the same time, the abilities will activate in order of Speed, respecting trick room.  Each ability''s weather will cancel the previous weather, and only the weather summoned by the slowest of the Pokémon will stay.',
        'drizzle', '出场时，
会将天气变为下雨。', '降雨'),
       (3, 'This Pokémon''s Speed rises one stage after each turn.', 'speed-boost', '每一回合速度会变快。', '加速'),
       (4, 'Moves cannot score critical hits against this Pokémon.

This ability functions identically to shell armor.', 'battle-armor', '被坚硬的甲壳守护着，
不会被对手的攻击击中要害。', '战斗盔甲'),
       (5, 'When this Pokémon is at full HP, any hit that would knock it out will instead leave it with 1 HP.  Regardless of its current HP, it is also immune to the one-hit KO moves: fissure, guillotine, horn drill, and sheer cold.

If this Pokémon is holding a focus sash, this ability takes precedence and the item will not be consumed.', 'sturdy', '即使受到对手的招式攻击，
也不会被一击打倒。
一击必杀的招式也没有效果。', '结实'),
       (6, 'While this Pokémon is in battle, self destruct and explosion will fail and aftermath will not take effect.',
        'damp', '通过把周围都弄湿，
使谁都无法使用自爆等爆炸类的招式。', '湿气'),
       (7, 'This Pokémon cannot be paralyzed.

If a Pokémon is paralyzed and acquires this ability, its paralysis is healed; this includes when regaining a lost ability upon leaving battle.',
        'limber', '因为身体柔软，
不会变为麻痹状态。', '柔软'),
       (8, 'During a sandstorm, this Pokémon has 1.25× its evasion, and it does not take sandstorm damage regardless of type.

The evasion bonus does not count as a stat modifier.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is halved in a sandstorm.', 'sand-veil', '在沙暴的时候，
闪避率会提高。', '沙隐'),
       (9, 'Whenever a move makes contact with this Pokémon, the move''s user has a 30% chance of being paralyzed.

Pokémon that are immune to electric-type moves can still be paralyzed by this ability.

Overworld: If the lead Pokémon has this ability, there is a 50% chance that encounters will be with an electric Pokémon, if applicable.',
        'static', '身上带有静电，
有时会让接触到的对手麻痹。', '静电'),
       (10, 'Whenever an electric-type move hits this Pokémon, it heals for 1/4 of its maximum HP, negating any other effect on it.

This ability will not take effect if this Pokémon is ground-type and thus immune to Electric moves.  Electric moves will ignore this Pokémon''s substitute.

This effect includes non-damaging moves, i.e. thunder wave.', 'volt-absorb', '受到电属性的招式攻击时，
不会受到伤害，而是会回复。', '蓄电'),
       (11, 'Whenever a water-type move hits this Pokémon, it heals for 1/4 of its maximum HP, negating any other effect on it.

Water moves will ignore this Pokémon''s substitute.', 'water-absorb', '受到水属性的招式攻击时，
不会受到伤害，而是会回复。', '储水'),
       (12, 'This Pokémon cannot be infatuated and is immune to captivate.

If a Pokémon is infatuated and acquires this ability, its infatuation is cleared.', 'oblivious', '因为感觉迟钝，
不会变为着迷和被挑衅状态。', '迟钝'),
       (13, 'While this Pokémon is in battle, weather can still be in play, but will not have any of its effects.

This ability functions identically to air lock.', 'cloud-nine', '任何天气的影响都会消失。', '无关天气'),
       (14, 'This Pokémon''s moves have 1.3× their accuracy.

This ability has no effect on the one-hit KO moves (fissure, guillotine, horn drill, and sheer cold).

Overworld: If the first Pokémon in the party has this ability, the chance of a wild Pokémon holding a particular item is raised from 50%, 5%, or 1% to 60%, 20%, or 5%, respectively.',
        'compound-eyes', '因为拥有复眼，
招式的命中率会提高。', '复眼'),
       (15, 'This Pokémon cannot be asleep.

This causes rest to fail altogether.  If a Pokémon is asleep and acquires this ability, it will immediately wake up; this includes when regaining a lost ability upon leaving battle.

This ability functions identically to vital spirit in battle.', 'insomnia', '因为是不会睡觉的体质，
所以不会变为睡眠状态。', '不眠'),
       (16, 'Whenever this Pokémon takes damage from a move, the Pokémon''s type changes to match the move.

If the Pokémon has two types, both are overridden.  The Pokémon must directly take damage; for example, moves blocked by a substitute will not trigger this ability, nor will moves that deal damage indirectly, such as spikes.

This ability takes effect on only the last hit of a multiple-hit attack.

In Pokémon Colosseum and XD: Gale of Darkness, this ability does not take effect on Shadow-type moves.', 'color-change', '自己的属性会变为
从对手处所受招式的属性。', '变色'),
       (17, 'This Pokémon cannot be poisoned.  This includes bad poison.

If a Pokémon is poisoned and acquires this ability, its poison is healed; this includes when regaining a lost ability upon leaving battle.',
        'immunity', '因为体内拥有免疫能力，
不会变为中毒状态。', '免疫'),
       (18, 'This Pokémon is immune to fire-type moves.  Once this Pokémon has been hit by a Fire move, its own Fire moves will inflict 1.5× as much damage until it leaves battle.

This ability has no effect while the Pokémon is frozen.  The Fire damage bonus is retained even if the Pokémon is frozen and thawed or the ability is lost or disabled.  Fire moves will ignore this Pokémon''s substitute.  This ability takes effect even on non-damaging moves, i.e. will o wisp.',
        'flash-fire', '受到火属性的招式攻击时，
吸收火焰，自己使出的
火属性招式会变强。', '引火'),
       (19, 'This Pokémon is immune to the extra effects of moves used against it.

An extra effect is a move''s chance, listed as an "effect chance", to inflict a status ailment, cause a stat change, or make the target flinch in addition to the move''s main effect.  For example, thunder shock''s paralysis is an extra effect, but thunder wave''s is not, nor are knock off''s item removal and air cutter''s increased critical hit rate.',
        'shield-dust', '被鳞粉守护着，
不会受到招式的追加效果影响。', '鳞粉'),
       (20, 'This Pokémon cannot be confused.

If a Pokémon is confused and acquires this ability, its confusion will immediately be healed.', 'own-tempo', '因为我行我素，
不会变为混乱状态。', '我行我素'),
       (21, 'This Pokémon cannot be forced out of battle by moves such as whirlwind.

dragon tail and circle throw still inflict damage against this Pokémon.

Overworld: If the lead Pokémon has this ability, the success rate while fishing is increased.', 'suction-cups', '用吸盘牢牢贴在地面上，
让替换宝可梦的招式和道具无效。', '吸盘'),
       (22, 'When this Pokémon enters battle, the opponent''s Attack is lowered by one stage.  In a double battle, both opponents are affected.

This ability also takes effect when acquired during a battle, but will not take effect again if lost and reobtained without leaving battle.

This ability has no effect on an opponent that has a substitute.

Overworld: If the first Pokémon in the party has this ability, any random encounter with a Pokémon five or more levels lower than it has a 50% chance of being skipped.',
        'intimidate', '出场时威吓对手，
让其退缩，
降低对手的攻击。', '威吓'),
       (23, 'While this Pokémon is in battle, opposing Pokémon cannot flee or switch out.

Other Pokémon with this ability are unaffected.  Pokémon with run away can still flee.  Pokémon can still switch out with the use of a move or item.',
        'shadow-tag', '踩住对手的影子
使其无法逃走或替换。', '踩影'),
       (24, 'Whenever a move makes contact with this Pokémon, the move''s user takes 1/8 of its maximum HP in damage.

This ability functions identically to iron barbs.', 'rough-skin', '受到攻击时，
用粗糙的皮肤弄伤
接触到自己的对手。', '粗糙皮肤'),
       (25, 'This Pokémon is immune to damaging moves that are not super effective against it.

Moves that inflict fixed damage, such as night shade or seismic toss, are considered super effective if their types are.  Damage not directly dealt by moves, such as damage from weather, a status ailment, or spikes, is not prevented.

This ability cannot be copied with role play or traded away with skill swap, but it can be copied with trace, disabled with gastro acid, or changed with worry seed.  This Pokémon can still use Role Play itself to lose this ability, but not Skill Swap.

If this Pokémon has a substitute, this ability will block moves as usual and any moves not blocked will react to the Substitute as usual.',
        'wonder-guard', '不可思议的力量，
只有效果绝佳的招式才能击中。', '神奇守护'),
       (26, 'This Pokémon is immune to ground-type moves, spikes, toxic spikes, and arena trap.

This ability is disabled during gravity or ingrain, or while holding an iron ball.  This ability is not disabled during roost.',
        'levitate', '从地面浮起，
从而不会受到地面属性招式的攻击。', '飘浮'),
       (27, 'Whenever a move makes contact with this Pokémon, the move''s user has a 30% chance of being paralyzed, poisoned, or put to sleep, chosen at random.

Nothing is done to compensate if the move''s user is immune to one of these ailments; there is simply a lower chance that the move''s user will be affected.',
        'effect-spore', '受到攻击时，
有时会把接触到自己的对手
变为中毒、麻痹或睡眠状态。', '孢子'),
       (28, 'Whenever this Pokémon is burned, paralyzed, or poisoned, the Pokémon who gave this Pokémon that ailment is also given the ailment.

This ability passes back bad poison when this Pokémon is badly poisoned.  This ability cannot pass on a status ailment that the Pokémon did not directly receive from another Pokémon, such as the poison from toxic spikes or the burn from a flame orb.

Overworld: If the lead Pokémon has this ability, wild Pokémon have a 50% chance of having the lead Pokémon''s nature, and a 50% chance of being given a random nature as usual, including the lead Pokémon''s nature.  This does not work on Pokémon received outside of battle or roaming legendaries.',
        'synchronize', '将自己的中毒、麻痹
或灼伤状态传染给对手。', '同步'),
       (29, 'This Pokémon cannot have its stats lowered by other Pokémon.

This ability does not prevent any stat losses other than stat modifiers, such as the Speed cut from paralysis.  This Pokémon can still be passed negative stat modifiers through guard swap, heart swap, or power swap.

This ability functions identically to white smoke in battle.', 'clear-body', '不会因为对手的招式或特性
而被降低能力。', '恒净之躯'),
       (30, 'This Pokémon is cured of any major status ailment when it is switched out for another Pokémon.

If this ability is acquired during battle, the Pokémon is cured upon leaving battle before losing the temporary ability.',
        'natural-cure', '回到同行队伍后，
异常状态就会被治愈。', '自然回复'),
       (31, 'All other Pokémon''s single-target electric-type moves are redirected to this Pokémon if it is an eligible target.  Other Pokémon''s Electric moves raise this Pokémon''s Special Attack one stage, negating any other effect on it, and cannot miss it.

If the move''s intended target also has this ability, the move is not redirected.  When multiple Pokémon with this ability are possible targets for redirection, the move is redirected to the one with the highest Speed stat, or, in the case of a tie, to a random tied Pokémon.  follow me takes precedence over this ability.

If the Pokémon is a ground-type and thus immune to Electric moves, its immunity prevents the Special Attack boost.',
        'lightning-rod', '将电属性的招式吸引到自己身上，
不会受到伤害，而是会提高特攻。', '避雷针'),
       (32, 'This Pokémon''s moves have twice their usual effect chance.

An effect chance is a move''s chance to inflict a status ailment, cause a stat change, or make the target flinch in addition to the move''s main effect.  For example, flamethrower''s chance of burning the target is doubled, but protect''s chance of success and air cutter''s increased critical hit rate are unaffected.

secret power is unaffected.', 'serene-grace', '托天恩的福，
招式的追加效果容易出现。', '天恩'),
       (33, 'This Pokémon''s Speed is doubled during rain.

This bonus does not count as a stat modifier.', 'swift-swim', '下雨天气时，
速度会提高。', '悠游自如'),
       (34, 'This Pokémon''s Speed is doubled during strong sunlight.

This bonus does not count as a stat modifier.', 'chlorophyll', '晴朗天气时，
速度会提高。', '叶绿素'),
       (35, 'Overworld: If the lead Pokémon has this ability, the wild encounter rate is doubled.

This ability has no effect in battle.', 'illuminate', '通过让周围变亮，
变得容易遇到野生的宝可梦。', '发光'),
       (36, 'When this Pokémon enters battle, it copies a random opponent''s ability.

This ability cannot copy flower gift, forecast, illusion, imposter, multitype, trace, wonder guard, or zen mode.',
        'trace', '出场时，复制对手的特性，
变为与之相同的特性。', '复制'),
       (37, 'This Pokémon''s Attack is doubled while in battle.

This bonus does not count as a stat modifier.

This ability functions identically to pure power.', 'huge-power', '物理攻击的威力会变为２倍。', '大力士'),
       (38, 'Whenever a move makes contact with this Pokémon, the move''s user has a 30% chance of being poisoned.',
        'poison-point', '有时会让接触到自己的
对手变为中毒状态。', '毒刺'),
       (39, 'This Pokémon cannot flinch.', 'inner-focus', '通过锻炼精神，
不会因对手的攻击而畏缩。', '精神力'),
       (40, 'This Pokémon cannot be frozen.

If a Pokémon is frozen and acquires this ability, it will immediately thaw out; this includes when regaining a lost ability upon leaving battle.

Overworld: If any Pokémon in the party has this ability, each egg in the party has its hatch counter decreased by 2 (rather than 1) each step cycle, making eggs hatch roughly twice as quickly.  This effect does not stack if multiple Pokémon have this ability or flame body.',
        'magma-armor', '将炽热的熔岩覆盖在身上，
不会变为冰冻状态。', '熔岩铠甲'),
       (41, 'This Pokémon cannot be burned.

If a Pokémon is burned and acquires this ability, its burn is healed; this includes when regaining a lost ability upon leaving battle.',
        'water-veil', '将水幕裹在身上，
不会变为灼伤状态。', '水幕'),
       (42, 'While this Pokémon is in battle, opposing steel-type Pokémon cannot flee or switch out.

Pokémon with run away can still flee.  Pokémon can still switch out with the use of a move or item.

Overworld: If the lead Pokémon has this ability, Steel-type Pokémon have a higher encounter rate.', 'magnet-pull', '用磁力吸住钢属性的宝可梦，
使其无法逃走。', '磁力'),
       (43, 'This Pokémon is immune to moves flagged as being sound-based.

heal bell is unaffected.  uproar still prevents this Pokémon from sleeping.  This Pokémon can still receive a Perish Song counter through baton pass, and will retain a Perish Song counter if it acquires this ability after Perish Song is used.

howl, roar of time, sonic boom, and yawn are not flagged as sound-based.', 'soundproof', '通过屏蔽声音，
不受到声音招式的攻击。', '隔音'),
       (44, 'This Pokémon heals for 1/16 of its maximum HP after each turn during rain.', 'rain-dish', '下雨天气时，
会缓缓回复ＨＰ。', '雨盘'),
       (45, 'The weather changes to a sandstorm when this Pokémon enters battle and does not end unless cancelled by another weather condition.

If multiple Pokémon with this ability, drizzle, drought, or snow warning are sent out at the same time, the abilities will activate in order of Speed, respecting trick room.  Each ability''s weather will cancel the previous weather, and only the weather summoned by the slowest of the Pokémon will stay.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is halved in a sandstorm.', 'sand-stream', '出场时，
会把天气变为沙暴。', '扬沙'),
       (46, 'Moves targetting this Pokémon use one extra PP.

This ability stacks if multiple targets have it.  This ability still affects moves that fail or miss.  This ability does not affect ally moves that target either the entire field or just its side, nor this Pokémon''s self-targetted moves; it does, however, affect single-targetted ally moves aimed at this Pokémon, ally moves that target all other Pokémon, and opponents'' moves that target the entire field.  If this ability raises a move''s PP cost above its remaining PP, it will use all remaining PP.

When this Pokémon enters battle, all participating trainers are notified that it has this ability.

Overworld: If the lead Pokémon has this ability, higher-levelled Pokémon have their encounter rate increased.',
        'pressure', '给予对手压迫感，
大量减少其使用招式的ＰＰ。', '压迫感'),
       (47, 'This Pokémon takes half as much damage from fire- and ice-type moves.', 'thick-fat', '因为被厚厚的脂肪保护着，
会让火属性和冰属性的招式伤害减半。', '厚脂肪'),
       (48, 'This Pokémon''s remaining sleep turn count falls by 2 rather than 1.

If this Pokémon''s sleep counter is at 1, it will fall to 0 and then the Pokémon will wake up.', 'early-bird', '即使变为睡眠状态，
也能以２倍的速度提早醒来。', '早起'),
       (49, 'Whenever a move makes contact with this Pokémon, the move''s user has a 30% chance of being burned.

Overworld: If any Pokémon in the party has this ability, each egg in the party has its hatch counter decreased by 2 (rather than 1) each step cycle, making eggs hatch roughly twice as quickly.  This effect does not stack if multiple Pokémon have this ability or magma armor.',
        'flame-body', '有时会让接触到自己的
对手变为灼伤状态。', '火焰之躯'),
       (50, 'This Pokémon is always successful fleeing from wild battles, even if trapped by a move or ability.',
        'run-away', '一定能从野生宝可梦
那儿逃走。', '逃跑'),
       (51, 'This Pokémon cannot have its accuracy lowered.

This ability does not prevent any accuracy losses other than stat modifiers, such as the accuracy cut from fog; nor does it prevent other Pokémon''s evasion from making this Pokémon''s moves less accurate.  This Pokémon can still be passed negative accuracy modifiers through heart swap.

Overworld: If the first Pokémon in the party has this ability, any random encounter with a Pokémon five or more levels lower than it has a 50% chance of being skipped.',
        'keen-eye', '多亏了锐利的目光，
命中率不会被降低。', '锐利目光'),
       (52, 'This Pokémon''s Attack cannot be lowered by other Pokémon.

This ability does not prevent any Attack losses other than stat modifiers, such as the Attack cut from a burn.  This Pokémon can still be passed negative Attack modifiers through heart swap or power swap.',
        'hyper-cutter', '因为拥有以力量自豪的钳子，
不会被对手降低攻击。', '怪力钳'),
       (53, 'At the end of each turn, if another Pokémon consumed or Flung a held item that turn, this Pokémon picks up the item if it is not already holding one.  After each battle, this Pokémon has a 10% chance of picking up an item if it is not already holding one.

The air balloon and eject button cannot be picked up.

The items that may be found vary by game, and, since Pokémon Emerald, by the Pokémon''s level.  This ability is checked after the battle ends, at which point any temporary ability changes have worn off.',
        'pickup', '有时会捡来对手用过的道具，
冒险过程中也会捡到。', '捡拾'),
       (54, 'Every second turn on which this Pokémon should attempt to use a move, it will instead do nothing ("loaf around").

Loafing around interrupts moves that take multiple turns the same way paralysis, flinching, etc do.  Most such moves, for example bide or rollout, are simply cut off upon loafing around.  Attacks with a recharge turn, such as hyper beam, do not have to recharge; attacks with a preparation turn, such as fly, do not end up being used.  Moves that are forced over multiple turns and keep going through failure, such as outrage, uproar, or any move forced by encore, keep going as usual.

If this Pokémon is confused, its confusion is not checked when loafing around; the Pokémon cannot hurt itself, and its confusion does not end or come closer to ending.

If this Pokémon attempts to move but fails, e.g. because of paralysis or gravity, it still counts as having moved and will loaf around the next turn.  If it does not attempt to move, e.g. because it is asleep or frozen, whatever ',
        'truant', '如果使出招式，
下一回合就会休息。', '懒惰'),
       (55, 'This Pokémon''s physical moves do 1.5× as much regular damage, but have 0.8× their usual accuracy.

Special moves are unaffected.  Moves that do set damage, such as seismic toss, have their accuracy affected, but not their damage.

Overworld: If the lead Pokémon has this ability, higher-levelled Pokémon have their encounter rate increased.',
        'hustle', '自己的攻击变高，
但命中率会降低。', '活力'),
       (56, 'Whenever a move makes contact with this Pokémon, the move''s user has a 30% chance of being infatuated.

Overworld: If the first Pokémon in the party has this ability, any wild Pokémon whose species can be either gender has a 2/3 chance of being set to the opposite gender, and a 1/3 chance of having a random gender as usual.',
        'cute-charm', '有时会让接触到自己的对手着迷。', '迷人之躯'),
       (116, 'This Pokémon takes 0.75× as much damage from moves that are super effective against it.

This ability functions identically to filter.', 'solid-rock', '受到效果绝佳的攻击时，
可以减弱其威力。', '坚硬岩石'),
       (57, 'This Pokémon has 1.5× its Special Attack if any friendly Pokémon has plus or minus.

This bonus does not count as a stat modifier.  If either ability is disabled by gastro acid, both lose their effect.',
        'plus', '出场的伙伴之间
如果有正电或负电特性的宝可梦，
自己的特攻会提高。', '正电'),
       (58, 'This Pokémon has 1.5× its Special Attack if any friendly Pokémon has plus or minus.

This bonus does not count as a stat modifier.  If either ability is disabled by gastro acid, both lose their effect.',
        'minus', '出场的伙伴之间
如果有正电或负电特性的宝可梦，
自己的特攻会提高。', '负电'),
       (59, 'During rain, strong sunlight, or hail, this Pokémon''s type changes to water, fire, or ice, respectively, and its form changes to match.

This ability has no effect for any Pokémon other than castform.

If the weather ends or becomes anything that does not trigger this ability, or a Pokémon with air lock or cloud nine enters battle, this Pokémon''s type and form revert to their default.  If this ability is lost or disabled, this Pokémon cannot change its current type and form until it regains its ability.',
        'forecast', '受天气的影响，
会变为水属性、火属性
或冰属性中的某一个。', '阴晴不定'),
       (60, 'This Pokémon''s hold item cannot be removed by other Pokémon.

Damaging moves that would remove this Pokémon''s item can still inflict damage against this Pokémon, e.g. knock off or pluck.  This Pokémon can still use moves that involve the loss of its own item, e.g. fling or trick.

Overworld: If the lead Pokémon has this ability, the encounter rate while fishing is increased.', 'sticky-hold', '因为道具是粘在黏性身体上的，
所以不会被对手夺走。', '黏着'),
       (61, 'After each turn, this Pokémon has a 33% of being cured of any major status ailment.', 'shed-skin', '通过蜕去身上的皮，
有时会治愈异常状态。', '蜕皮'),
       (62, 'Whenever this Pokémon is asleep, burned, paralyzed, or poisoned, it has 1.5× its Attack.  This Pokémon is not affected by the usual Attack cut from a burn.

This bonus does not count as a stat modifier.', 'guts', '如果变为异常状态，
会拿出毅力，
攻击会提高。', '毅力'),
       (63, 'Whenever this Pokémon has a major status ailment, it has 1.5× its Defense.

This bonus does not count as a stat modifier.', 'marvel-scale', '如果变为异常状态，
神奇鳞片会发生反应，
防御会提高。', '神奇鳞片'),
       (64, 'Whenever a Pokémon would heal after hitting this Pokémon with a leeching move like absorb, it instead loses as many HP as it would usually gain.

dream eater is unaffected.', 'liquid-ooze', '吸收了污泥浆的对手
会因强烈的恶臭
而受到伤害，减少ＨＰ。', '污泥浆'),
       (65,
        'When this Pokémon has 1/3 or less of its HP remaining, its grass-type moves inflict 1.5× as much regular damage.',
        'overgrow', 'ＨＰ减少的时候，
草属性的招式威力会提高。', '茂盛'),
       (66,
        'When this Pokémon has 1/3 or less of its HP remaining, its fire-type moves inflict 1.5× as much regular damage.',
        'blaze', 'ＨＰ减少的时候，
火属性的招式威力会提高。', '猛火'),
       (67,
        'When this Pokémon has 1/3 or less of its HP remaining, its water-type moves inflict 1.5× as much regular damage.',
        'torrent', 'ＨＰ减少的时候，
水属性的招式威力会提高。', '激流'),
       (68, 'When this Pokémon has 1/3 or less of its HP remaining, its bug-type moves inflict 1.5× as much regular damage.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is increased.', 'swarm', 'ＨＰ减少的时候，
虫属性的招式威力会提高。', '虫之预感'),
       (69, 'This Pokémon does not receive recoil damage from its recoil moves.

struggle''s recoil is unaffected.  This ability does not prevent crash damage from missing with jump kick or high jump kick.',
        'rock-head', '即使使出会受反作用力伤害的招式，
ＨＰ也不会减少。', '坚硬脑袋'),
       (70, 'The weather changes to strong sunlight when this Pokémon enters battle and does not end unless cancelled by another weather condition.

If multiple Pokémon with this ability, drizzle, sand stream, or snow warning are sent out at the same time, the abilities will activate in order of Speed, respecting trick room.  Each ability''s weather will cancel the previous weather, and only the weather summoned by the slowest of the Pokémon will stay.',
        'drought', '出场时，
会将天气变为晴朗。', '日照'),
       (71, 'While this Pokémon is in battle, opposing Pokémon cannot flee or switch out.  flying-type Pokémon and Pokémon in the air, e.g. due to levitate or magnet rise, are unaffected.

Pokémon with run away can still flee.  Pokémon can still switch out with the use of a move or item.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is doubled.', 'arena-trap',
        '在战斗中让对手无法逃走。', '沙穴'),
       (72, 'This Pokémon cannot be asleep.

This causes rest to fail altogether.  If a Pokémon is asleep and acquires this ability, it will immediately wake up; this includes when regaining a lost ability upon leaving battle.

This ability functions identically to insomnia in battle.

Overworld: If the lead Pokémon has this ability, higher-levelled Pokémon have their encounter rate increased.',
        'vital-spirit', '通过激发出干劲，
不会变为睡眠状态。', '干劲'),
       (73, 'This Pokémon cannot have its stats lowered by other Pokémon.

This ability does not prevent any stat losses other than stat modifiers, such as the Speed cut from paralysis; nor self-inflicted stat drops, such as the Special Attack drop from overheat; nor opponent-triggered stat boosts, such as the Attack boost from swagger.  This Pokémon can still be passed negative stat modifiers through guard swap, heart swap, or power swap.

This ability functions identically to clear body in battle.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is halved.', 'white-smoke', '被白色烟雾保护着，
不会被对手降低能力。', '白色烟雾'),
       (74, 'This Pokémon''s Attack is doubled in battle.

This bonus does not count as a stat modifier.

This ability functions identically to huge power.', 'pure-power', '因瑜伽的力量，
物理攻击的威力会变为２倍。', '瑜伽之力'),
       (75, 'Moves cannot score critical hits against this Pokémon.

This ability functions identically to battle armor.', 'shell-armor', '被坚硬的壳保护着，
对手的攻击不会击中要害。', '硬壳盔甲'),
       (76, 'While this Pokémon is in battle, weather can still be in play, but will not have any of its effects.

This ability functions identically to cloud nine.', 'air-lock', '所有天气的影响都会消失。', '气闸'),
       (77, 'When this Pokémon is confused, it has twice its evasion.', 'tangled-feet', '在混乱状态时，
闪避率会提高。', '蹒跚'),
       (78, 'Whenever an electric-type move hits this Pokémon, its Speed rises one stage, negating any other effect on it.

This ability will not take effect if this Pokémon is immune to Electric moves.  Electric moves will ignore this Pokémon''s substitute.

This effect includes non-damaging moves, i.e. thunder wave.', 'motor-drive', '受到电属性的招式攻击时，
不会受到伤害，而是速度会提高。', '电气引擎'),
       (263, 'Increases the power of Dragon-type moves used by this Pokémon by 50%.', 'dragons-maw',
        '龙属性的招式威力会提高。', '龙颚'),
       (79, 'This Pokémon inflicts 1.25× as much regular damage against Pokémon of the same gender and 0.75× as much regular damage against Pokémon of the opposite gender.

If either Pokémon is genderless, damage is unaffected.', 'rivalry', '面对性别相同的对手，
会燃起斗争心，变得更强。
而面对性别不同的，则会变弱。', '斗争心'),
       (80, 'Whenever this Pokémon flinches, its Speed rises one stage.', 'steadfast', '每次畏缩时，
不屈之心就会燃起，
速度也会提高。', '不屈之心'),
       (81, 'During hail, this Pokémon has 1.25× its evasion, and it does not take hail damage regardless of type.

The evasion bonus does not count as a stat modifier.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is halved in snow.', 'snow-cloak', '冰雹天气时，
闪避率会提高。', '雪隐'),
       (82,
        'This Pokémon eats any held Berry triggered by low HP when it falls below 50% of its HP, regardless of the Berry''s usual threshold.',
        'gluttony', '原本ＨＰ变得很少时才会吃树果，
在ＨＰ还有一半时就会把它吃掉。', '贪吃鬼'),
       (83, 'Whenever this Pokémon receives a critical hit, its Attack rises to the maximum of 6 stages.

This ability will still take effect if the critical hit is received by a substitute.', 'anger-point', '要害被击中时，
会大发雷霆，
攻击力变为最大。', '愤怒穴位'),
       (84, 'When this Pokémon uses or loses its held item, its Speed is doubled.  If it gains another item or leaves battle, this bonus is lost.

This includes when the Pokémon drops its item because of knock off.  This bonus does not count as a stat modifier.  There is no notification when this ability takes effect.',
        'unburden', '失去所持有的道具时，
速度会提高。', '轻装'),
       (85, 'This Pokémon takes half as much damage from fire-type moves and burns.', 'heatproof', '耐热的体质会
让火属性的招式威力减半。', '耐热'),
       (86, 'Each stage of this Pokémon''s stat modifiers acts as two stages.  These doubled stages are still limited to a minimum of -6 and a maximum of 6.

This Pokémon can still accumulate less than -3 or more than 3 stages of stat modifiers, even though the extra ones have no effect after doubling.',
        'simple', '能力变化会变为平时的２倍。', '单纯'),
       (87,
        'This Pokémon takes 1/8 of its maximum HP in damage after each turn during strong sunlight, but it heals for 1/8 of its HP each turn during rain.  This Pokémon takes 1.25× as much damage from fire-type moves, but whenever a water move hits it, it heals for 1/4 its maximum HP instead.',
        'dry-skin', '下雨天气时和受到水属性的招式时，
ＨＰ会回复。晴朗天气时和受到火属性的
招式时，ＨＰ会减少。', '干燥皮肤'),
       (88, 'When this Pokémon enters battle, its Attack or Special Attack, whichever corresponds to its opponents'' weaker total defensive stat, rises one stage.  In the event of a tie, Special Attack is raised.

This ability also takes effect when acquired during a battle.', 'download', '比较对手的防御和特防，
根据较低的那项能力
相应地提高自己的攻击或特攻。', '下载'),
       (89, 'Moves flagged as being punch-based have 1.2× their base power for this Pokémon.

sucker punch is not flagged as punch-based; its original, Japanese name only means "surprise attack".', 'iron-fist',
        '使用拳类招式的威力会提高。', '铁拳'),
       (90,
        'If this Pokémon is poisoned, it will heal for 1/8 of its maximum HP after each turn rather than taking damage.  This includes bad poison.',
        'poison-heal', '变为中毒状态时，
ＨＰ不会减少，反而会增加起来。', '毒疗'),
       (91,
        'This Pokémon inflicts twice as much damage with moves whose types match its own, rather than the usual same-type attack bonus of 1.5×.',
        'adaptability', '与自身同属性的招式
威力会提高。', '适应力'),
       (92,
        'This Pokémon always hits five times with two-to-five-hit moves, such as icicle spear.  It also bypasses the accuracy checks on triple kick''s second and third hits.',
        'skill-link', '如果使用连续招式，
总是能使出最高次数。', '连续攻击'),
       (93, 'This Pokémon is cured of any major status ailment after each turn during rain.', 'hydration', '下雨天气时，
异常状态会治愈。', '湿润之躯'),
       (94,
        'During strong sunlight, this Pokémon has 1.5× its Special Attack but takes 1/8 of its maximum HP in damage after each turn.',
        'solar-power', '晴朗天气时，
特攻会提高，
而每回合ＨＰ会减少。', '太阳之力'),
       (95, 'Whenever this Pokémon has a major status ailment, it has 1.5× its Speed.  This Pokémon is not affected by the usual Speed cut from paralysis.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is halved.', 'quick-feet', '变为异常状态时，
速度会提高。', '飞毛腿'),
       (96, 'This Pokémon''s moves all act as if they were normal-type.

Moves that inflict typeless damage do so as usual.  Moves of variable type, such as hidden power, are affected.  They otherwise work as usual, however; weather ball, for example, is always forced to be Normal, but it still has doubled power and looks different during weather.

As thunder wave is prevented by immunities, unlike most non-damaging moves, it does not affect ghost-type Pokémon under the effect of this ability.',
        'normalize', '无论是什么属性的招式，
全部会变为一般属性。
威力会少量提高。', '一般皮肤'),
       (97, 'This Pokémon inflicts triple damage with critical hits, rather than the usual double damage.', 'sniper', '击中要害时，
威力会变得更强。', '狙击手'),
       (98, 'This Pokémon is immune to damage not directly caused by a move.

For example, this Pokémon takes no damage from from weather, recoil, status ailments, or spikes, but it still suffers from the Attack cut when burned, and a life orb will still power up this Pokémon''s moves without damaging it.  Anything that directly depends on such damage will also not happen; for example, leech seed will neither hurt this Pokémon nor heal the opponent, and Pokémon with a jaboca berry or rowap berry will not consume the berry when hit by this Pokémon.

The following are unaffected: struggle, pain split (whether used by or against this Pokémon), belly drum, substitute, curse, moves that knock the user out, and damage from confusion.

This Pokémon will neither lose nor regain HP if it drains HP from a Pokémon with liquid ooze.

If this Pokémon is badly poisoned, the poison counter is still increased each turn; if the Pokémon loses this ability, it will begin taking as much damage as it would be if it had ',
        'magic-guard', '不会受到攻击以外的伤害。', '魔法防守'),
       (99, 'Moves used by or against this Pokémon never miss.

One-hit KO moves are unaffected.  Moves affected by this ability can hit Pokémon during the preparation turn of moves like dig or fly.

Overworld: If the lead Pokémon has this ability, the wild encounter rate is doubled.', 'no-guard', '由于无防守战术，
双方使出的招式都必定会击中。', '无防守'),
       (141, 'After each turn, one of this Pokémon''s stats at random rises two stages, and another falls one stage.

If a stat is already at 6 or -6 stages, it will not be chosen to be increased or decreased, respectively.', 'moody', '每一回合，能力中的某项
会大幅提高，而某项会降低。', '心情不定'),
       (100, 'This Pokémon moves last within its priority bracket.

Multiple Pokémon with this ability move in order of Speed amongst themselves.

The full incense and lagging tail take precedence over this ability; that is, Pokémon with these items move after Pokémon with this ability.  Pokémon with both this ability and one of these items are delayed as much as if they had only the item.

This ability works as usual during trick room: Pokémon with this ability will move in reverse order of Speed after Pokémon without it.',
        'stall', '使出招式的顺序
必定会变为最后。', '慢出'),
       (101, 'This Pokémon''s moves have 1.5× their power if their base power is 60 or less.

This includes moves of variable power, such as hidden power and magnitude, when their power is 60 or less.  helping hand''s power boost is taken into account for any move, as is defense curl''s power boost for rollout.',
        'technician', '攻击时可以将
低威力招式的威力提高。', '技术高手'),
       (102, 'This Pokémon cannot be given a major status ailment during strong sunlight.

This ability does not heal prior status ailments.  rest will fail altogether with this ability in effect.  yawn will immediately fail if used on this Pokémon during strong sunlight, and an already-used Yawn will fail if the weather turns to strong sunlight in the meantime.',
        'leaf-guard', '晴朗天气时，
不会变为异常状态。', '叶子防守'),
       (103, 'In battle, this Pokémon cannot use its held item, nor will the item have any passive effect on the battle, positive or negative.  This Pokémon also cannot use fling.

The Speed cut from the iron ball and the effort items (the macho brace, power weight, power bracer, power belt, power lens, power band, and power anklet) is unaffected.  Items that do not directly affect the battle, such as the exp share, the amulet coin, or the soothe bell, work as usual.  All held items work as usual out of battle.

Other moves that use the held item, such as natural gift and switcheroo, work as usual.', 'klutz',
        '无法使用持有的道具。', '笨拙'),
       (104, 'This Pokémon''s moves completely ignore abilities that could hinder or prevent their effect on the target.

For example, this Pokémon''s moves ignore abilities that would fully negate them, such as water absorb; abilities that would prevent any of their effects, such as clear body, shell armor, or sticky hold; and abilities that grant any general protective benefit, such as simple, snow cloak, or thick fat.  If an ability could either hinder or help this Pokémon''s moves, e.g. dry skin or unaware, the ability is ignored either way.

Abilities that do not fit this description, even if they could hinder moves in some other way, are not affected.  For example, cursed body only affects potential future uses of the move, while liquid ooze and shadow tag can only hinder a move''s effect on the user.  This ablity cannot ignore type or form changes granted by abilities, for example color change or forecast; nor effects that were caused by abilities but are no longer tied to an ability, such as the',
        'mold-breaker', '可以不受对手特性的干扰，
向对手使出招式。', '破格'),
       (105, 'This Pokémon''s moves have critical hit rates one stage higher than normal.', 'super-luck', '因为拥有超幸运，
攻击容易击中对手的要害。', '超幸运'),
       (106,
        'When this Pokémon is knocked out by a move that makes contact, the move''s user takes 1/4 its maximum HP in damage.',
        'aftermath', '变为濒死时，
会对接触到自己的对手造成伤害。', '引爆'),
       (107, 'When this Pokémon enters battle, if one of its opponents has a move that is super effective against it, self destruct, explosion, or a one-hit knockout move, all participating trainers are notified.

The move itself is not revealed; only that there is such a move.  Moves that inflict typeless damage, such as future sight, and moves of variable type, such as hidden power, count as their listed types.  counter, metal burst, mirror coat, and one-hit KO moves to which this Pokémon is immune do not trigger this ability.',
        'anticipation', '可以察觉到
对手拥有的危险招式。', '危险预知'),
       (108, 'When this Pokémon enters battle, it reveals the move with the highest base power known by any opposing Pokémon to all participating trainers.

In the event of a tie, one is chosen at random.

Moves without a listed base power are assigned one as follows:

Power | Moves
  160 | One-hit KO moves: fissure, guillotine, horn drill, and sheer cold
  120 | Counter moves: counter, metal burst, and mirror coat
   80 | Variable power or set damage: crush grip, dragon rage, electro ball, endeavor, final gambit, flail, frustration, grass knot, gyro ball, heat crash, heavy slam, hidden power, low kick, natural gift, night shade, psywave, return, reversal, seismic toss, sonic boom, trump card, and wring out
    0 | Any such move not listed
', 'forewarn', '出场时，
只读取１个对手拥有的招式。', '预知梦'),
       (109, 'This Pokémon ignores other Pokémon''s stat modifiers for the purposes of damage and accuracy calculation.

Effectively, this affects modifiers of every stat except Speed.

The power of punishment and stored power is calculated as usual.  When this Pokémon hurts itself in confusion, its stat modifiers affect damage as usual.',
        'unaware', '可以无视对手能力的变化，
进行攻击。', '纯朴'),
       (110, 'This Pokémon deals twice as much damage with moves that are not very effective against the target.',
        'tinted-lens', '可以将效果不好的招式
以通常的威力使出。', '有色眼镜'),
       (111, 'This Pokémon takes 0.75× as much damage from moves that are super effective against it.

This ability functions identically to solid rock.', 'filter', '受到效果绝佳的攻击时，
可以减弱其威力。', '过滤'),
       (112, 'This Pokémon''s Attack and Speed are halved for five turns upon entering battle.

This ability also takes effect when acquired during battle.  If this Pokémon loses its ability before the five turns are up, its Attack and Speed return to normal; if it then regains this ability without leaving battle, its Attack and Speed are halved again, but the counter keeps counting from where it was.',
        'slow-start', '在５回合内，
攻击和速度减半。', '慢启动'),
       (113, 'This Pokémon ignores ghost-type Pokémon''s immunity to normal- and fighting-type moves.

Ghost Pokémon''s other types affect damage as usual.', 'scrappy', '一般属性和格斗属性的招式
可以击中幽灵属性的宝可梦。', '胆量'),
       (114, 'All other Pokémon''s single-target water-type moves are redirected to this Pokémon, if it is an eligible target.  Other Pokémon''s Water moves raise this Pokémon''s Special Attack one stage, negating any other effect on it, and cannot miss it.

If the move''s intended target also has this ability, the move is not redirected.  When multiple Pokémon with this ability are possible targets for redirection, the move is redirected to the one with the highest Speed stat, or, in the case of a tie, to a random tied Pokémon.  follow me takes precedence over this ability.',
        'storm-drain', '将水属性的招式引到自己身上，
不会受到伤害，而是会提高特攻。', '引水'),
       (115,
        'This Pokémon heals for 1/16 of its maximum HP after each turn during hail, and it does not take hail damage regardless of type.',
        'ice-body', '冰雹天气时，
会缓缓回复ＨＰ。', '冰冻之躯'),
       (142, 'This Pokémon does not take damage from weather.', 'overcoat', '不会受到沙暴或冰雹等的伤害。
不会受到粉末类招式的攻击。', '防尘'),
       (117, 'The weather changes to hail when this Pokémon enters battle and does not end unless cancelled by another weather condition.

If multiple Pokémon with this ability, drizzle, drought, or sand stream are sent out at the same time, the abilities will activate in order of Speed, respecting trick room.  Each ability''s weather will cancel the previous weather, and only the weather summoned by the slowest of the Pokémon will stay.',
        'snow-warning', '出场时，
会将天气变为冰雹。', '降雪'),
       (118, 'This Pokémon has a chance of picking up honey after each battle.  This chance starts at 5% and rises another 5% after every tenth level: 5% from level 1–10, 10% from 11–20, and so on, up to 50% from 91–100.

This ability is checked after the battle ends, at which point any temporary ability changes have worn off.',
        'honey-gather', '战斗结束时，
有时候会捡来甜甜蜜。', '采蜜'),
       (119, 'When this Pokémon enters battle, it reveals an opposing Pokémon''s held item to all participating trainers.

In a double battle, if one opponent has an item, this Pokémon will Frisk that Pokémon; if both have an item, it will Frisk one at random.',
        'frisk', '出场时，
可以察觉对手的持有物。', '察觉'),
       (120, 'This Pokémon''s recoil moves and crash moves have 1.2× their base power.

struggle is unaffected.

The "crash moves" are the moves that damage the user upon missing: jump kick and high jump kick.', 'reckless', '自己会因反作用力受伤的招式，
其威力会提高。', '舍身'),
       (121, 'If this Pokémon is holding an elemental Plate, its type and form change to match the Plate.

This Pokémon''s held item, whether or not it is a Plate, cannot be taken by covet or thief, nor removed by knock off, nor traded by switcheroo or trick.  Covet, Thief, and Knock Off still inflict damage against this Pokémon.  Unlike with sticky hold, this Pokémon cannot use fling, Switcheroo, or Trick to lose its item itself, nor gain an item through Switcheroo or Trick if it does not have one.

This ability has no effect for any Pokémon other than arceus.  This ability cannot be traded with skill swap, nor copied with role play or trace, nor disabled with gastro acid, nor changed with worry seed.  This Pokémon cannot use Skill Swap or Role Play to lose its ability itself.  mold breaker cannot ignore this ability.

If a Pokémon Transforms into an Arceus with this ability, it will Transform into Arceus''s default, normal-type form.  If the Transforming Pokémon is holding a Plate, this ability will',
        'multitype', '自己的属性会根据持有的石板
或Ｚ纯晶的属性而改变。', '多属性'),
       (122, 'Friendly Pokémon have 1.5× their Attack and Special Defense during strong sunlight if any friendly Pokémon has this ability.

Unlike forecast, multitype, and zen mode, this ability is not tied to its Pokémon''s form change; cherrim will switch between its forms even if it loses this ability.  As such, this ability also works if obtained by a Pokémon other than Cherrim.',
        'flower-gift', '晴朗天气时，
自己与同伴的攻击和
特防能力会提高。', '花之礼'),
       (123, 'Opposing Pokémon take 1/8 of their maximum HP in damage after each turn while they are asleep.',
        'bad-dreams', '给予睡眠状态的对手伤害。', '梦魇'),
       (124, 'Whenever a move makes contact with this Pokémon, if it does not have a held item, it steals the attacker''s held item.

This Pokémon cannot steal upon being knocked out.  It can steal if the attacker has a substitute, but cannot steal when its own Substitute is hit.  If a move hits multiple times, only the last hit triggers this ability.  If this Pokémon is wild, it cannot steal from a trained Pokémon.',
        'pickpocket', '盗取接触到自己的
对手的道具。', '顺手牵羊'),
       (125, 'This Pokémon''s moves with extra effects have 1.3× their power, but lose their extra effects.

An effect chance is a move''s chance to inflict a status ailment, cause a stat change, or make the target flinch in addition to the move''s main effect. For example, thunder shock''s paralysis is an extra effect, but thunder wave''s is not, nor are knock off''s item removal and air cutter''s increased critical hit rate.

Moves that lower the user''s stats are unaffected.', 'sheer-force', '招式的追加效果消失，
但因此能以更高的威力使出招式。', '强行'),
       (126,
        'Whenever this Pokémon''s stats would be raised, they are instead lowered by the same amount, and vice versa.',
        'contrary', '能力的变化发生逆转，
原本提高时会降低，
而原本降低时会提高。', '唱反调'),
       (127, 'Opposing Pokémon cannot eat held Berries while this Pokémon is in battle.

Affected Pokémon can still use bug bite or pluck to eat a target''s Berry.', 'unnerve', '让对手紧张，
使其无法食用树果。', '紧张感'),
       (128, 'When any of this Pokémon''s stats are lowered, its Attack rises by two stages.

If multiple stats are lowered at once, this ability takes effect with each stat lowered.', 'defiant', '能力被降低时，
攻击会大幅提高。', '不服输'),
       (129, 'This Pokémon''s Attack and Special Attack are halved when it has half its HP or less.', 'defeatist', 'ＨＰ减半时，
会变得软弱，
攻击和特攻会减半。', '软弱'),
       (130, 'Moves that hit this Pokémon have a 30% chance of being Disabled afterward.', 'cursed-body', '受到攻击时，
有时会把对手的招式
变为定身法状态。', '诅咒之躯'),
       (131,
        'Friendly Pokémon next to this Pokémon in double and triple battles each have a 30% chance of being cured of any major status ailment after each turn.',
        'healer', '有时会治愈异常状态的同伴。', '治愈之心'),
       (132, 'All friendly Pokémon take 0.75× as much direct damage from moves while this Pokémon is in battle.

This effect stacks if multiple allied Pokémon have it.', 'friend-guard', '可以减少我方的伤害。', '友情防守'),
       (133, 'Whenever a physical move hits this Pokémon, its Speed rises one stage and its Defense falls one stage.

This ability triggers on every hit of a multiple-hit move.', 'weak-armor', '如果因物理招式受到伤害，
防御会降低，
速度会大幅提高。', '碎裂铠甲'),
       (134, 'This Pokémon has double the usual weight for its species.', 'heavy-metal', '自身的重量会变为２倍。',
        '重金属'),
       (135, 'This Pokémon has half the usual weight for its species.', 'light-metal', '自身的重量会减半。', '轻金属'),
       (136, 'This Pokémon takes half as much damage when it is hit having full HP.', 'multiscale', 'ＨＰ全满时，
受到的伤害会变少。', '多重鳞片'),
       (137, 'This Pokémon has 1.5× its Attack when poisoned.', 'toxic-boost', '变为中毒状态时，
物理招式的威力会提高。', '中毒激升'),
       (138, 'This Pokémon has 1.5× its Special Attack when burned.', 'flare-boost', '变为灼伤状态时，
特殊招式的威力会提高。', '受热激升'),
       (139,
        'After each turn, if the last item this Pokémon consumed was a Berry and it is not currently holding an item, it has a 50% chance of regaining that Berry, or a 100% chance during strong sunlight.',
        'harvest', '可以多次制作出
已被使用掉的树果。', '收获'),
       (140,
        'This Pokémon does not take damage from friendly Pokémon''s moves, including single-target moves aimed at it.',
        'telepathy', '读取我方的攻击，
并闪避其招式伤害。', '心灵感应'),
       (143, 'This Pokémon''s contact moves have a 30% chance of poisoning the target with each hit.

This counts as an extra effect for the purposes of shield dust.  This ability takes effect before mummy.',
        'poison-touch', '只通过接触就有可能
让对手变为中毒状态。', '毒手'),
       (144, 'This Pokémon regains 1/3 of its maximum HP when it is switched out for another Pokémon under any circumstances other than having fainted.

This ability does not take effect when a battle ends.', 'regenerator', '退回同行队伍后，
ＨＰ会少量回复。', '再生力'),
       (145, 'This Pokémon''s Defense cannot be lowered by other Pokémon.

This Pokémon can still be passed negative Defense modifiers through heart swap or guard swap.', 'big-pecks',
        '不会受到防御降低的效果。', '健壮胸肌'),
       (146,
        'This Pokémon''s Speed is doubled during a sandstorm, and it does not take sandstorm damage, regardless of type.',
        'sand-rush', '沙暴天气时，
速度会提高。', '拨沙'),
       (147, 'Non-damaging moves have exactly 50% base accuracy against this Pokémon.', 'wonder-skin', '成为不易受到变化招式
攻击的身体。', '奇迹皮肤'),
       (148, 'This Pokémon''s moves have 1.3× their power when it moves last in a turn.

future sight and doom desire are unaffected.', 'analytic', '如果在最后使出招式，
招式的威力会提高。', '分析'),
       (149, 'This Pokémon, upon being sent out, appears to have the species, nickname, and Poké Ball of the last Pokémon in the party that is able to battle.  This illusion breaks upon being hit by a damaging move.

Other damage, e.g. from weather or spikes, does not break the illusion, nor does damage done to a substitute.

If the party order becomes temporarily shuffled around as Pokémon are switched out in battle, this ability chooses the last Pokémon according to that shuffled order.',
        'illusion', '假扮成同行队伍中的
最后一个宝可梦出场，
迷惑对手。', '幻觉'),
       (150,
        'This Pokémon transforms into a random opponent upon entering battle.  This effect is identical to the move transform.',
        'imposter', '变身为当前面对的宝可梦。', '变身者'),
       (151, 'This Pokémon''s moves ignore light screen, reflect, and safeguard.', 'infiltrator', '可以穿透对手的壁障
或替身进行攻击。', '穿透'),
       (152, 'Whenever a contact move hits this Pokémon, the attacking Pokémon''s ability changes to Mummy.

multitype is unaffected.  If a Pokémon with moxie knocks this Pokémon out, the former''s ability will change without taking effect.',
        'mummy', '被对手接触到后，
会将对手变为木乃伊。', '木乃伊'),
       (153, 'This Pokémon''s Attack rises one stage upon knocking out another Pokémon, even a friendly Pokémon.

This ability does not take effect when the Pokémon indirectly causes another Pokémon to faint, e.g. through poison or spikes.

If this Pokémon knocks out a Pokémon with mummy, the former''s ability will change without taking effect.', 'moxie', '如果打倒对手，
就会充满自信，攻击会提高。', '自信过度'),
       (154, 'Whenever a dark-type move hits this Pokémon, its Attack rises one stage.

The move is not negated in any way.', 'justified', '受到恶属性的招式攻击时，
因为正义感，攻击会提高。', '正义之心'),
       (155, 'This Pokémon''s Speed rises one stage with each hit from a damaging dark-, ghost-, or bug-type move.',
        'rattled', '受到恶属性、幽灵属性
和虫属性的招式攻击时，
会因胆怯而速度提高。', '胆怯'),
       (156, 'When this Pokémon is targeted by a move flagged as being reflectable, the move is redirected to its user.

All reflectable moves are non-damaging, and most non-damaging moves that target other Pokémon are reflectable.

A move reflected by this ability or magic coat cannot be reflected back.', 'magic-bounce', '可以不受到由对手使出的
变化招式影响，并将其反弹。', '魔法镜'),
       (157,
        'Whenever a grass-type move hits this Pokémon, its Attack rises one stage, negating any other effect on it.',
        'sap-sipper', '受到草属性的招式攻击时，
不会受到伤害，而是攻击会提高。', '食草'),
       (158, 'This Pokémon''s non-damaging moves have their priority increased by one stage.', 'prankster',
        '可以率先使出变化招式。', '恶作剧之心'),
       (159,
        'During a sandstorm, this Pokémon''s rock-, ground-, and steel-type moves have 1.3× their base power.  This Pokémon does not take sandstorm damage, regardless of type.',
        'sand-force', '沙暴天气时，
岩石属性、地面属性
和钢属性的招式威力会提高。', '沙之力'),
       (160, 'Whenever a move makes contact with this Pokémon, the move''s user takes 1/8 of its maximum HP in damage.

This ability functions identically to rough skin.', 'iron-barbs', '用铁刺给予接触到自己的
对手伤害。', '铁刺'),
       (161, 'This Pokémon switches between Standard Mode and Zen Mode after each turn depending on its HP.  Below 50% of its maximum HP, it switches to Zen Mode, and at 50% or above, it switches to Standard Mode.

This Pokémon returns to Standard Mode upon leaving battle or losing this ability.  This ability has no effect if this Pokémon is not a darmanitan.',
        'zen-mode', 'ＨＰ变为一半以下时，
样子会改变。', '达摩模式'),
       (162,
        'All friendly Pokémon''s moves, including this Pokémon''s own moves, have 1.1× their usual accuracy while this Pokémon is in battle.',
        'victory-star', '自己和同伴的命中率会提高。', '胜利之星'),
       (163, 'This Pokémon''s moves completely ignore abilities that could hinder or prevent their effect on the target.

For example, this Pokémon''s moves ignore abilities that would fully negate them, such as water absorb; abilities that would prevent any of their effects, such as clear body, shell armor, or sticky hold; and abilities that grant any general protective benefit, such as simple, snow cloak, or thick fat.  If an ability could either hinder or help this Pokémon''s moves, e.g. dry skin or unaware, the ability is ignored either way.

Abilities that do not fit this description, even if they could hinder moves in some other way, are not affected.  For example, cursed body only affects potential future uses of the move, while liquid ooze and shadow tag can only hinder a move''s effect on the user.  This ablity cannot ignore type or form changes granted by abilities, for example color change or forecast; nor effects that were caused by abilities but are no longer tied to an ability, such as the',
        'turboblaze', '可以不受对手特性的干扰，
向对手使出招式。', '涡轮火焰'),
       (193,
        'After this Pokémon is hit by a move, if that move caused this Pokémon''s HP to drop below half, it switches out.',
        'wimp-out', 'ＨＰ变为一半时，
会慌慌张张逃走，
退回同行队伍中。', '跃跃欲逃'),
       (194,
        'After this Pokémon is hit by a move, if that move caused this Pokémon''s HP to drop below half, it switches out.',
        'emergency-exit', 'ＨＰ变为一半时，
为了回避危险，
会退回到同行队伍中。', '危险回避'),
       (195, 'Raises this Pokémon''s Defense by two stages when it''s hit by a Water move.', 'water-compaction', '受到水属性的招式攻击时，
防御会大幅提高。', '遇水凝固'),
       (196, 'This Pokémon''s moves critical hit against poisoned targets.', 'merciless', '攻击中毒状态的对手时，
必定会击中要害。', '不仁不义'),
       (164, 'This Pokémon''s moves completely ignore abilities that could hinder or prevent their effect on the target.

For example, this Pokémon''s moves ignore abilities that would fully negate them, such as water absorb; abilities that would prevent any of their effects, such as clear body, shell armor, or sticky hold; and abilities that grant any general protective benefit, such as simple, snow cloak, or thick fat.  If an ability could either hinder or help this Pokémon''s moves, e.g. dry skin or unaware, the ability is ignored either way.

Abilities that do not fit this description, even if they could hinder moves in some other way, are not affected.  For example, cursed body only affects potential future uses of the move, while liquid ooze and shadow tag can only hinder a move''s effect on the user.  This ablity cannot ignore type or form changes granted by abilities, for example color change or forecast; nor effects that were caused by abilities but are no longer tied to an ability, such as the',
        'teravolt', '可以不受对手特性的干扰，
向对手使出招式。', '兆级电压'),
       (165, 'Protects allies against moves that affect their mental state.', 'aroma-veil', '可以防住向自己和同伴
发出的心灵攻击。', '芳香幕'),
       (166, 'Protects friendly grass Pokémon from having their stats lowered by other Pokémon.', 'flower-veil', '我方的草属性宝可梦
能力不会降低，
也不会变为异常状态。', '花幕'),
       (167, 'Restores HP upon eating a Berry, in addition to the Berry''s effect.', 'cheek-pouch', '无论是哪种树果，
食用后，ＨＰ都会回复。', '颊囊'),
       (168, 'Changes the bearer''s type to match each move it uses.

The type change takes place just before the move is used.', 'protean', '变为与自己使出的招式
相同的属性。', '变幻自如'),
       (169, 'Halves damage from physical attacks.', 'fur-coat', '对手给予的物理招式的
伤害会减半。', '毛皮大衣'),
       (170, 'Steals the target''s held item when the bearer uses a damaging move.', 'magician', '夺走被自己的招式
击中的对手的道具。', '魔术师'),
       (171, 'Protects against bullet, ball, and bomb-based moves.', 'bulletproof', '可以防住对手的
球和弹类招式。', '防弹'),
       (172, 'Raises Special Attack by two stages upon having any stat lowered.', 'competitive', '如果能力被降低，
特攻就会大幅提高。', '好胜'),
       (173, 'Strengthens biting moves to 1.5× their power.', 'strong-jaw', '因为颚部强壮，
啃咬类招式的威力会提高。', '强壮之颚'),
       (174,
        'Turns the bearer''s normal-type moves into ice-type moves.  Moves changed by this ability have 1.3× their power.',
        'refrigerate', '一般属性的招式
会变为冰属性。
威力会少量提高。', '冰冻皮肤'),
       (175, 'Prevents friendly Pokémon from sleeping.', 'sweet-veil', '我方的宝可梦
不会变为睡眠状态。', '甜幕'),
       (176,
        'Changes aegislash to Blade Forme before using a damaging move, or Shield Forme before using kings shield.',
        'stance-change', '如果使出攻击招式，会变为刀剑形态，
如果使出招式“王者盾牌”，
会变为盾牌形态。', '战斗切换'),
       (177, 'Raises flying moves'' priority by one stage.', 'gale-wings', 'ＨＰ全满时，
飞行属性的招式可以率先使出。', '疾风之翼'),
       (178, 'Strengthens aura and pulse moves to 1.5× their power.', 'mega-launcher', '波动和波导类招式的
威力会提高。', '超级发射器'),
       (179, 'Boosts Defense while grassy terrain is in effect.', 'grass-pelt', '在青草场地时，
防御会提高。', '草之毛皮'),
       (180, 'Passes the bearer''s held item to an ally when the ally uses up its item.', 'symbiosis', '同伴使用道具时，
会把自己持有的道具传递给同伴。', '共生'),
       (181, 'Strengthens moves that make contact to 1.33× their power.', 'tough-claws', '接触到对手的招式
威力会提高。', '硬爪'),
       (182,
        'Turns the bearer''s normal-type moves into fairy moves.  Moves changed by this ability have 1.3× their power.',
        'pixilate', '一般属性的招式
会变为妖精属性。
威力会少量提高。', '妖精皮肤'),
       (183, 'Lowers attacking Pokémon''s Speed by one stage on contact.', 'gooey', '对于用攻击接触到自己的对手，
会降低其速度。', '黏滑'),
       (184,
        'Turns the bearer''s normal-type moves into flying-type moves.  Moves changed by this ability have 1.3× their power.',
        'aerilate', '一般属性的招式
会变为飞行属性。
威力会少量提高。', '飞行皮肤'),
       (185, 'Lets the bearer hit twice with damaging moves.  The second hit has half power.', 'parental-bond',
        '亲子俩可以合计攻击２次。', '亲子爱'),
       (186, 'Strengthens dark moves for all friendly and opposing Pokémon.', 'dark-aura', '全体的恶属性招式变强。',
        '暗黑气场'),
       (187, 'Strengthens fairy moves for all friendly and opposing Pokémon.', 'fairy-aura', '全体的妖精属性招式变强。',
        '妖精气场'),
       (188,
        'While this Pokémon is on the field, dark aura and fairy aura weaken moves of their respective types to 2/3 their power, rather than strengthening them.',
        'aura-break', '让气场的效果发生逆转，
降低威力。', '气场破坏'),
       (189, 'When this Pokémon enters battle or gains this ability, the weather becomes heavy rain.  Heavy rain has all the properties of rain dance and also causes damaging Fire moves to fail.

Heavy rain ends when this Pokémon leaves battle or loses this ability, or when this ability is nullified.  The weather cannot otherwise be changed except by the effects of delta stream and desolate land.

air lock and cloud nine will prevent the effects of heavy rain, including allowing Fire moves to work, but will not allow the weather to be changed.',
        'primordial-sea', '变为不会受到
火属性攻击的天气。', '始源之海'),
       (190, 'When this Pokémon enters battle or gains this ability, the weather becomes extremely harsh sunlight.  Extremely harsh sunlight has all the properties of sunny day and also causes damaging Water moves to fail.

Extremely harsh sunlight ends when this Pokémon leaves battle or loses this ability, or when this ability is nullified.  The weather cannot otherwise be changed except by the effects of delta stream and primordial sea.

air lock and cloud nine will prevent the effects of extremely harsh sunlight, including allowing Water moves to work, but will not allow the weather to be changed.',
        'desolate-land', '变为不会受到
水属性攻击的天气。', '终结之地'),
       (191, 'When this Pokémon enters battle or gains this ability, the weather becomes a mysterious air current.  A mysterious air current causes moves to not be super effective against Flying; they do neutral damage instead.  anticipation and stealth rock are not affected.

The mysterious air current ends when this Pokémon leaves battle or loses this ability, or when this ability is nullified.  The weather cannot otherwise be changed except by the effects of desolate land and primordial sea.

air lock and cloud nine will prevent the effect of a mysterious air current, but will not allow the weather to be changed.',
        'delta-stream', '变为令飞行属性的弱点
消失的天气。', '德尔塔气流'),
       (192, 'Raises this Pokémon''s Defense by one stage when it takes damage from a move.', 'stamina', '受到攻击时，
防御会提高。', '持久力'),
       (197, 'When this Pokémon enters battle and at the end of each turn, if its HP is 50% or above, it changes into Meteor Form; otherwise, it changes into Core Form.  In Meteor Form, it cannot be given a major status ailment (though existing ones are not cured), cannot become drowsy from yawn, and cannot use rest (which will simply fail).

This ability cannot be copied, replaced, or nullified.  This ability only takes effect for Minior.', 'shields-down', 'ＨＰ变为一半时，
壳会坏掉，变得有攻击性。', '界限盾壳'),
       (198, 'This Pokémon''s moves have double power against Pokémon that switched in this turn.', 'stakeout', '可以对替换出场的对手
以２倍的伤害进行攻击。', '蹲守'),
       (199,
        'When this Pokémon is hit by a Fire move, the damage is halved.  When this Pokémon uses a Water move, the power is doubled. This Pokémon cannot be burned, and if it becomes burned, the burn is immediately cured.',
        'water-bubble', '降低与自己相对的火属性
招式的威力，不会灼伤。', '水泡'),
       (200, 'This Pokémon''s Steel moves have 1.5× power.', 'steelworker', '钢属性的招式威力会提高。', '钢能力者'),
       (201,
        'Whenever this Pokémon takes damage from a move that causes its HP to drop below 50%, its Special Attack rises by one stage.',
        'berserk', '因对手的攻击
ＨＰ变为一半时，
特攻会提高。', '怒火冲天'),
       (202, 'During Hail, this Pokémon has double Speed.', 'slush-rush', '冰雹天气时，
速度会提高。', '拨雪'),
       (203, 'A move used by this Pokémon will not make contact.', 'long-reach', '可以不接触对手
就使出所有的招式。', '远隔'),
       (204, 'When this Pokémon uses a move that is sound-based, that move''s type is Water.', 'liquid-voice', '所有的声音招式
都变为水属性。', '湿润之声'),
       (205, 'This Pokémon''s healing moves have their priority increased by 3.', 'triage', '可以率先使出回复招式。',
        '先行治疗'),
       (206, 'When this Pokémon uses a Normal moves, that move is Electric its power is 1.2×.', 'galvanize', '一般属性的招式
会变为电属性。
威力会少量提高。', '电气皮肤'),
       (207, 'Doubles this Pokémon''s Speed on Electric Terrain.', 'surge-surfer', '电气场地时，
速度会变为２倍。', '冲浪之尾'),
       (208, 'If this Pokémon is a wishiwashi and level 20 or above, then when it enters battle and at the start of each turn, it becomes Schooling Form if its HP is 25% or higher and Solo Form otherwise.

This ability cannot be replaced, copied, or nullified.', 'schooling', 'ＨＰ多的时候会聚起来变强。
ＨＰ剩余量变少时，
群体会分崩离析。', '鱼群'),
       (209, 'If this Pokémon is in its Disguised Form and takes damage from a move, it switches to its Busted Form and the damage is prevented.  Other effects are not prevented.

This ability cannot be copied or replaced.  This ability only takes effect for Mimikyu.', 'disguise', '通过画皮覆盖住身体，
可以防住１次攻击。', '画皮'),
       (210, 'Transforms this Pokémon into Ash-Greninja after fainting an opponent.  Water Shuriken''s power is 20 and always hits three times.

This ability cannot be copied or replaced.  This ability only takes effect for Greninja.', 'battle-bond', '打倒对手时，与训练家的牵绊会增强，
变为小智版甲贺忍蛙。
飞水手里剑的招式威力会增强。', '牵绊变身'),
       (211, 'Transforms 10% or 50% Zygarde into Complete Forme when its HP is below 50%.

This ability cannot be copied or replaced.  This ability only takes effect for Zygarde.', 'power-construct', 'ＨＰ变为一半时，
细胞们会赶来支援，
变为完全体形态。', '群聚变形'),
       (212,
        'This Pokémon''s moves and item ignore the usual immunity of Poison and Steel Pokémon when attempting to inflict poison.',
        'corrosion', '可以使钢属性和毒属性的宝可梦
也陷入中毒状态。', '腐蚀'),
       (213,
        'This Pokémon always acts as though it were Asleep.  It cannot be given another status ailment; it''s unaffected by yawn; it can use sleep talk; and so on.',
        'comatose', '总是半梦半醒的状态，
绝对不会醒来。
可以就这么睡着进行攻击。', '绝对睡眠'),
       (214,
        'When an opposing Pokémon attempts to use a move that targets this Pokémon or an ally, and that move has priority, it will fail.',
        'queenly-majesty', '向对手施加威慑力，
使其无法对我方使出先制招式。', '女王的威严'),
       (215,
        'When this Pokémon faints from an opponent''s move, that opponent takes damage equal to the HP this Pokémon had remaining.',
        'innards-out', '被对手打倒的时候，
会给予对手相当于
ＨＰ剩余量的伤害。', '飞出的内在物'),
       (216, 'Whenever another Pokémon uses a dance move, this Pokémon will use the same move immediately afterwards.',
        'dancer', '有谁使出跳舞招式时，
自己也能就这么接着使出跳舞招式。', '舞者'),
       (217, 'Ally Pokémon''s moves have their power increased to 1.3×.', 'battery', '会提高我方的
特殊招式的威力。', '蓄电池'),
       (218, 'Damage from contact moves is halved.  Damage from Fire moves is doubled.', 'fluffy', '会将对手所给予的接触类招式的伤害减半，
但火属性招式的伤害会变为２倍。', '毛茸茸'),
       (219,
        'When an opposing Pokémon attempts to use a move that targets this Pokémon or an ally, and that move has priority, it will fail.',
        'dazzling', '让对手吓一跳，
使其无法对我方使出先制招式。', '鲜艳之躯'),
       (220, 'This Pokémon''s Special Attack rises by one stage every time any Pokémon faints.', 'soul-heart', '宝可梦每次变为濒死状态时，
特攻会提高。', '魂心'),
       (221,
        'When this Pokémon takes regular damage from a contact move, the attacking Pokémon''s Speed lowers by one stage.',
        'tangling-hair', '对于用攻击接触到自己的对手，
会降低其速度。', '卷发'),
       (222, 'When an ally faints, this Pokémon gains its Ability.', 'receiver', '继承被打倒的同伴的特性，
变为相同的特性。', '接球手'),
       (223, 'When an ally faints, this Pokémon gains its Ability.', 'power-of-alchemy', '继承被打倒的同伴的特性，
变为相同的特性。', '化学之力'),
       (224, 'Raises this Pokémon''s highest stat by one stage when it faints another Pokémon.', 'beast-boost', '打倒对手的时候，
自己最高的那项能力会提高。', '异兽提升'),
       (225, 'Changes this Pokémon''s type to match its held Memory.

This ability cannot be copied, replaced, or nullified.  This ability only takes effect for Silvally.', 'rks-system', '根据持有的存储碟，
自己的属性会改变。', 'ＡＲ系统'),
       (226, 'When this Pokémon enters battle, it changes the terrain to electric terrain.', 'electric-surge', '出场时，
会布下电气场地。', '电气制造者'),
       (227, 'When this Pokémon enters battle, it changes the terrain to psychic terrain.', 'psychic-surge', '出场时，
会布下精神场地。', '精神制造者'),
       (228, 'When this Pokémon enters battle, it changes the terrain to misty terrain.', 'misty-surge', '出场时，
会布下薄雾场地。', '薄雾制造者'),
       (229, 'When this Pokémon enters battle, it changes the terrain to grassy terrain.', 'grassy-surge', '出场时，
会布下青草场地。', '青草制造者'),
       (230, 'This Pokémon''s stats cannot be lowered by other Pokémon''s moves or abilities.  This effect only applies to normal stat modifications and not more exotic effects such as topsy turvy or power swap.

This Ability is not bypassed by mold breaker, teravolt, or turboblaze.', 'full-metal-body', '不会因为对手的招式或特性
而被降低能力。', '金属防护'),
       (231, 'When this Pokémon has full HP, regular damage (not fixed damage!) from moves is halved.

This ability cannot be nullified.', 'shadow-shield', 'ＨＰ全满时，
受到的伤害会变少。', '幻影防守'),
       (232, 'Super-effective damage this Pokémon takes is reduced to 0.75×.

This Ability is not bypassed by mold breaker, teravolt, or turboblaze.', 'prism-armor', '受到效果绝佳的攻击时，
可以减弱其威力。', '棱镜装甲'),
       (233, 'Increases super-effective damage dealt to 1.25×.', 'neuroforce', '效果绝佳的攻击，
威力会变得更强。', '脑核之力'),
       (234, 'Boosts the Pokémon’s Attack stat the first time the Pokémon enters a battle.', 'intrepid-sword', '出场时，
攻击会提高。', '不挠之剑'),
       (235, 'Boosts the Pokémon’s Defense stat the first time the Pokémon enters a battle.', 'dauntless-shield', '出场时，
防御会提高。', '不屈之盾'),
       (236,
        'Changes the Pokémon’s type to the type of the move it’s about to use. This works only once each time the Pokémon enters battle.',
        'libero', '变为与自己使出的招式
相同的属性。', '自由者'),
       (237,
        'At any time after the first Poké Ball is thrown and fails to catch a Pokémon, at the end of a turn, if a Pokémon with Ball Fetch is on the field and not holding another item, it will pick up the same type of ball as the first one thrown.',
        'ball-fetch', '没有携带道具时，
会拾取第１个投出后
捕捉失败的精灵球。', '捡球'),
       (238,
        'When the Pokémon is hit by an attack, it scatters cotton fluff around and lowers the Speed stats of all Pokémon except itself.',
        'cotton-down', '受到攻击后撒下棉絮，
降低除自己以外的
所有宝可梦的速度。', '棉絮'),
       (239, 'Ignores the effects of opposing Pokémon’s Abilities and moves that draw in moves.', 'propeller-tail', '能无视具有吸引
对手招式效果的
特性或招式的影响。', '螺旋尾鳍'),
       (240, 'Bounces back only the stat-lowering effects that the Pokémon receives.', 'mirror-armor', '只反弹自己受到的
能力降低效果。', '镜甲'),
       (241,
        'When the Pokémon uses Surf or Dive, it will come back with prey. When it takes damage, it will spit out the prey to attack.',
        'gulp-missile', '冲浪或潜水时会叼来猎物。
受到伤害时，
会吐出猎物进行攻击。', '一口导弹'),
       (242, 'Ignores the effects of opposing Pokémon’s Abilities and moves that draw in moves.', 'stalwart', '能无视具有吸引
对手招式效果的
特性或招式的影响。', '坚毅'),
       (243,
        'Raises the user''s Speed by three stages when it is hit by a Fire- or Water-type attack. The attack still deals damage.',
        'steam-engine', '受到水属性或
火属性的招式攻击时，
速度会巨幅提高。', '蒸汽机'),
       (244, 'Boosts the power of sound-based moves. The Pokémon also takes half the damage from these kinds of moves.',
        'punk-rock', '声音招式的威力会提高。
受到的声音招式伤害会减半。', '庞克摇滚'),
       (245, 'The Pokémon creates a sandstorm when it’s hit by an attack.', 'sand-spit', '受到攻击时，
会刮起沙暴。', '吐沙'),
       (246, 'The Pokémon is protected by ice scales, which halve the damage taken from special moves.', 'ice-scales', '由于有冰鳞粉的守护，
受到的特殊攻击伤害会减半。', '冰鳞粉'),
       (247, 'Ripens Berries and doubles their effect.', 'ripen', '使树果成熟，
效果变为２倍。', '熟成'),
       (248,
        'When an Eiscue that''s in its Ice Face form is hit by a physical move, it takes no damage and changes into its Noice Face form. If a hailstorm or snowstorm begins while Eiscue is in its Noice Face form, or if it is sent out in its Noice Face form during hail, it will immediately revert to its Ice Face form.',
        'ice-face', '头部的冰会代替自己承受
物理攻击，但是样子会改变。
下冰雹时，冰会恢复原状。', '结冻头'),
       (249, ' "The power of moves used by the allies of the Pokémon with Power Spot is increased by 30%."',
        'power-spot', '只要处在相邻位置，
招式的威力就会提高。', '能量点'),
       (250, 'Changes the Pokémon’s type depending on the terrain.', 'mimicry', '宝可梦的属性会根据
场地的状态而变化。', '拟态'),
       (251,
        'When the Pokémon enters a battle, the effects of Light Screen, Reflect, and Aurora Veil are nullified for both opposing and ally Pokémon.',
        'screen-cleaner', '出场时，敌方和我方的光墙、
反射壁和极光幕的效果会消失。', '除障'),
       (252, 'Powers up the Steel-type moves of the Pokémon and its allies.', 'steely-spirit', '我方的钢属性
攻击威力会提高。', '钢之意志'),
       (253,
        'When the Pokémon with Perish Body is hit with a contact move, both the attacker and the Pokémon with Perish Body will faint in three turns.',
        'perish-body', '受到接触类招式攻击时，
双方都会在３回合后变为濒死状态。
替换后效果消失。', '灭亡之躯'),
       (254, 'The Pokémon exchanges Abilities with a Pokémon that hits it with a move that makes direct contact.',
        'wandering-spirit', '与使用接触类招式
攻击自己的宝可梦互换特性。', '游魂'),
       (255, 'Boosts the Pokémon’s Attack stat but only allows the use of the first selected move.', 'gorilla-tactics', '虽然攻击会提高，
但是只能使出
一开始所选的招式。', '一猩一意'),
       (256,
        'While the Pokémon is in the battle, the effects of all other Pokémon’s Abilities will be nullified or will not be triggered.',
        'neutralizing-gas', '特性为化学变化气体的宝可梦在场时，
场上所有宝可梦的
特性效果都会消失或者无法生效。', '化学变化气体'),
       (257,
        'Prevents the Pokémon and its teammates from being poisoned. It also cures teammates of poisoning when it enters the battlefield.',
        'pastel-veil', '自己和同伴都不会
陷入中毒的异常状态。', '粉彩护幕'),
       (258,
        'The Pokémon changes its form, alternating between its Full Belly Mode and Hangry Mode after the end of every turn.',
        'hunger-switch', '每回合结束时会在
满腹花纹与空腹花纹之间
交替改变样子。', '饱了又饿'),
       (259,
        'If a Pokémon with this Ability selects a damaging move, it has a 30% chance of going first in its priority bracket.',
        'quick-draw', '有时能比对手先一步行动。', '速击'),
       (260,
        'If the Pokémon uses moves that make direct contact, it can attack the target even if the target protects itself.',
        'unseen-fist', '如果使出的是接触到对手的招式，
就可以无视守护效果进行攻击。', '无形拳'),
       (261,
        'When the Pokémon enters a battle, it scatters medicine from its shell, which removes all stat changes from allies.',
        'curious-medicine', '出场时会从贝壳撒药，
将我方的能力变化复原。', '怪药'),
       (262, 'Increases the power of Electric-type moves used by this Pokémon by 50%.', 'transistor',
        '电属性的招式威力会提高。', '电晶体'),
       (264, 'When the Pokémon knocks out a target, it utters a chilling neigh, which boosts its Attack stat.',
        'chilling-neigh', '打倒对手时
会用冰冷的声音嘶鸣
并提高攻击。', '苍白嘶鸣'),
       (265, 'When the Pokémon knocks out a target, it utters a terrifying neigh, which boosts its Sp. Atk stat.',
        'grim-neigh', '打倒对手时
会用恐怖的声音嘶鸣
并提高特攻。', '漆黑嘶鸣'),
       (266, '', 'as-one-glastrier', '兼备蕾冠王的紧张感和
雪暴马的苍白嘶鸣这两种特性。', '人马一体'),
       (267, '', 'as-one-spectrier', '兼备蕾冠王的紧张感和
灵幽马的漆黑嘶鸣这两种特性。', '人马一体'),
       (268, 'Contact with the Pokémon changes the attacker’s Ability to Lingering Aroma.', 'lingering-aroma',
        'Contact with the Pokémon changes the attacker''s Ability to Lingering Aroma.', 'Lingering Aroma'),
       (269, 'Turns the ground into Grassy Terrain when the Pokémon is hit by an attack.', 'seed-sower',
        'Turns the ground into Grassy Terrain when the Pokémon is hit by an attack.', 'Seed Sower'),
       (270, 'Boosts the Attack stat when the Pokémon is hit by a Fire-type move. The Pokémon also cannot be burned.',
        'thermal-exchange',
        'Boosts the Attack stat when the Pokémon is hit by a Fire-type move. The Pokémon also cannot be burned.',
        'Thermal Exchange'),
       (271,
        'When an attack causes its HP to drop to half or less, the Pokémon gets angry. This lowers its Defense and Sp. Def stats but boosts its Attack, Sp. Atk, and Speed stats.',
        'anger-shell',
        'When an attack causes its HP to drop to half or less, the Pokémon gets angry. This lowers its Defense and Sp. Def stats but boosts its Attack, Sp. Atk, and Speed stats.',
        'Anger Shell'),
       (272,
        'The Pokémon’s pure salt protects it from status conditions and halves the damage taken from Ghost-type moves.',
        'purifying-salt',
        'The Pokémon''s pure salt protects it from status conditions and halves the damage taken from Ghost-type moves.',
        'Purifying Salt'),
       (273, 'The Pokémon takes no damage when hit by Fire-type moves. Instead, its Defense stat is sharply boosted.',
        'well-baked-body',
        'The Pokémon takes no damage when hit by Fire-type moves. Instead, its Defense stat is sharply boosted.',
        'Well-Baked Body'),
       (274,
        'Boosts the Pokémon’s Attack stat if Tailwind takes effect or if the Pokémon is hit by a wind move. The Pokémon also takes no damage from wind moves.',
        'wind-rider',
        'Boosts the Pokémon''s Attack stat if Tailwind takes effect or if the Pokémon is hit by a wind move. The Pokémon also takes no damage from wind moves.',
        'Wind Rider'),
       (275,
        'Boosts the Pokémon’s Attack stat if intimidated. Moves and items that would force the Pokémon to switch out also fail to work.',
        'guard-dog',
        'Boosts the Pokémon’s Attack stat if intimidated. Moves and items that would force the Pokémon to switch out also fail to work.',
        'Guard Dog'),
       (276, 'Increases the power of Rock-type moves used by this Pokémon by 50%.', 'rocky-payload',
        'Powers up Rock-type moves.', 'Rocky Payload'),
       (277,
        'The Pokémon becomes charged when it is hit by a wind move, boosting the power of the next Electric-type move the Pokémon uses.',
        'wind-power',
        'The Pokémon becomes charged when it is hit by a wind move, boosting the power of the next Electric-type move the Pokémon uses.',
        'Wind Power'),
       (278, 'The Pokémon transforms into its Hero Form when it switches out.', 'zero-to-hero',
        'The Pokémon transforms into its Hero Form when it switches out.', 'Zero to Hero'),
       (279,
        'When the Pokémon enters a battle, it goes inside the mouth of an ally Dondozo if one is on the field. The Pokémon then issues commands from there.',
        'commander',
        'When the Pokémon enters a battle, it goes inside the mouth of an ally Dondozo if one is on the field. The Pokémon then issues commands from there.',
        'Commander'),
       (280,
        'The Pokémon becomes charged when it takes damage, boosting the power of the next Electric-type move the Pokémon uses.',
        'electromorphosis',
        'The Pokémon becomes charged when it takes damage, boosting the power of the next Electric-type move the Pokémon uses.',
        'Electromorphosis'),
       (281, 'Boosts the Pokémon’s most proficient stat in harsh sunlight or if the Pokémon is holding Booster Energy.',
        'protosynthesis',
        'Boosts the Pokémon''s most proficient stat in harsh sunlight or if the Pokémon is holding Booster Energy.',
        'Protosynthesis'),
       (282,
        'Boosts the Pokémon’s most proficient stat on Electric Terrain or if the Pokémon is holding Booster Energy.',
        'quark-drive',
        'Boosts the Pokémon''s most proficient stat on Electric Terrain or if the Pokémon is holding Booster Energy.',
        'Quark Drive'),
       (283, 'A body of pure, solid gold gives the Pokémon full immunity to other Pokémon’s status moves.',
        'good-as-gold', 'A body of pure, solid gold gives the Pokémon full immunity to other Pokémon''s status moves.',
        'Good as Gold'),
       (284, 'The power of the Pokémon’s ruinous vessel lowers the Sp. Atk stats of all Pokémon except itself.',
        'vessel-of-ruin',
        'The power of the Pokémon''s ruinous vessel lowers the Sp. Atk stats of all Pokémon except itself.',
        'Vessel of Ruin'),
       (285, 'The power of the Pokémon’s ruinous sword lowers the Defense stats of all Pokémon except itself.',
        'sword-of-ruin',
        'The power of the Pokémon''s ruinous sword lowers the Defense stats of all Pokémon except itself.',
        'Sword of Ruin'),
       (286, 'The power of the Pokémon’s ruinous wooden tablets lowers the Attack stats of all Pokémon except itself.',
        'tablets-of-ruin',
        'The power of the Pokémon''s ruinous wooden tablets lowers the Attack stats of all Pokémon except itself.',
        'Tablets of Ruin'),
       (287, 'The power of the Pokémon’s ruinous beads lowers the Sp. Def stats of all Pokémon except itself.',
        'beads-of-ruin',
        'The power of the Pokémon''s ruinous beads lowers the Sp. Def stats of all Pokémon except itself.',
        'Beads of Ruin'),
       (288,
        'Turns the sunlight harsh when the Pokémon enters a battle. The ancient pulse thrumming through the Pokémon also boosts its Attack stat in harsh sunlight.',
        'orichalcum-pulse',
        'Turns the sunlight harsh when the Pokémon enters a battle. The ancient pulse thrumming through the Pokémon also boosts its Attack stat in harsh sunlight.',
        'Orichalcum Pulse'),
       (289,
        'Turns the ground into Electric Terrain when the Pokémon enters a battle. The futuristic engine within the Pokémon also boosts its Sp. Atk stat on Electric Terrain.',
        'hadron-engine',
        'Turns the ground into Electric Terrain when the Pokémon enters a battle. The futuristic engine within the Pokémon also boosts its Sp. Atk stat on Electric Terrain.',
        'Hadron Engine'),
       (290, 'If an opponent’s stat is boosted, the Pokémon seizes the opportunity to boost the same stat for itself.',
        'opportunist',
        'If an opponent''s stat is boosted, the Pokémon seizes the opportunity to boost the same stat for itself.',
        'Opportunist'),
       (291,
        'When the Pokémon eats a Berry, it will regurgitate that Berry at the end of the next turn and eat it one more time.',
        'cud-chew',
        'When the Pokémon eats a Berry, it will regurgitate that Berry at the end of the next turn and eat it one more time.',
        'Cud Chew'),
       (292, 'Increases the power of all ''slicing'' moves by 50%.', 'sharpness', 'Powers up slicing moves.',
        'Sharpness'),
       (293,
        'When the Pokémon enters a battle, its Attack and Sp. Atk stats are slightly boosted for each of the allies in its party that have already been defeated.',
        'supreme-overlord',
        'When the Pokémon enters a battle, its Attack and Sp. Atk stats are slightly boosted for each of the allies in its party that have already been defeated.',
        'Supreme Overlord'),
       (294, 'When the Pokémon enters a battle, it copies an ally’s stat changes.', 'costar',
        'When the Pokémon enters a battle, it copies an ally''s stat changes.', 'Costar'),
       (295,
        'Scatters poison spikes at the feet of the opposing team when the Pokémon takes damage from physical moves.',
        'toxic-debris',
        'Scatters poison spikes at the feet of the opposing team when the Pokémon takes damage from physical moves.',
        'Toxic Debris'),
       (296,
        'The mysterious tail covering the Pokémon’s head makes opponents unable to use priority moves against the Pokémon or its allies.',
        'armor-tail',
        'The mysterious tail covering the Pokémon''s head makes opponents unable to use priority moves against the Pokémon or its allies.',
        'Armor Tail'),
       (297, 'If hit by a Ground-type move, the Pokémon has its HP restored instead of taking damage.', 'earth-eater',
        'If hit by a Ground-type move, the Pokémon has its HP restored instead of taking damage.', 'Earth Eater'),
       (298,
        'The Pokémon will always act more slowly when using status moves, but these moves will be unimpeded by the Ability of the target.',
        'mycelium-might',
        'The Pokémon will always act more slowly when using status moves, but these moves will be unimpeded by the Ability of the target.',
        'Mycelium Might'),
       (299,
        'Mind''s Eye enables the Pokémon with this Ability to hit Ghost-type Pokémon with damage-dealing Normal- and Fighting-type moves. It also prevents other Pokémon from lowering the Pokémon''s accuracy and ignores changes to the opponents'' evasion.',
        'minds-eye',
        'The Pokémon ignores changes to opponents'' evasiveness, its accuracy can''t be lowered, and it can hit Ghost types with Normal-type and Fighting-type moves',
        '心眼'),
       (300,
        'A sickly sweet scent spreads across the field the first time the Pokémon enters a battle, lowering the evasiveness of opposing Pokémon.',
        'supersweet-syrup', 'Lowers the evasion of opposing Pokémon by 1 stage when first sent into battle',
        '甘露之蜜'),
       (301,
        'When the Pokémon enters a battle, it showers its ally with hospitality, restoring a small amount of the ally''s HP.',
        'hospitality',
        'When the Pokémon enters a battle, it showers its ally with hospitality, restoring a small amount of the ally''s HP',
        '款待'),
       (302, 'The power of the Pokémon''s toxic chain may badly poison any target the Pokémon hits with a move.',
        'toxic-chain',
        'The power of the Pokémon''s toxic chain may badly poison any target the Pokémon hits with a move', '毒锁链'),
       (303, 'Embody Aspect boosts a particular stat, depending on the form of a Terastallized Ogerpon',
        'embody-aspect',
        'The Pokémon''s heart fills with memories, causing the Mask to shine and one of the Pokémon''s stats to be boosted.',
        '面影辉映'),
       (304,
        'When the Pokémon enters a battle, it absorbs the energy around itself and transforms into its Terastal Form.',
        'tera-shift',
        'When the Pokémon enters a battle, it absorbs the energy around itself and transforms into its Terastal Form.',
        '太晶变形'),
       (305,
        'The Pokémon’s shell contains the powers of each type. All damage-dealing moves that hit the Pokémon when its HP is full will not be very effective.',
        'tera-shell',
        'The Pokémon''s shell contains the powers of each type. All damage-dealing moves that hit the Pokémon when its HP is full will not be very effective.',
        '太晶甲壳'),
       (306,
        'When Terapagos changes into its Stellar Form, it uses its hidden powers to eliminate all effects of weather and terrain, reducing them to zero.',
        'teraform-zero',
        'When Terapagos changes into its Stellar Form, it uses its hidden powers to eliminate all effects of weather and terrain, reducing them to zero.',
        '归零化境'),
       (307, 'When a Pokémon is poisoned by any of Pecharunt''s moves, it will also become confused.',
        'poison-puppeteer', 'Pokémon poisoned by Pecharunt''s moves will also become confused.', '毒傀儡'),
       (10001, '', 'mountaineer', '', 'Mountaineer'),
       (10002, '', 'wave-rider', '', 'Wave Rider'),
       (10003, '', 'skater', '', 'Skater'),
       (10004, '', 'thrust', '', 'Thrust'),
       (10005, '', 'perception', '', 'Perception'),
       (10006, '', 'parry', '', 'Parry'),
       (10007, '', 'instinct', '', 'Instinct'),
       (10008, '', 'dodge', '', 'Dodge'),
       (10009, '', 'jagged-edge', '', 'Jagged Edge'),
       (10010, '', 'frostbite', '', 'Frostbite'),
       (10011, '', 'tenacity', '', 'Tenacity'),
       (10012, '', 'pride', '', 'Pride'),
       (10013, '', 'deep-sleep', '', 'Deep Sleep'),
       (10014, '', 'power-nap', '', 'Power Nap'),
       (10015, '', 'spirit', '', 'Spirit'),
       (10016, '', 'warm-blanket', '', 'Warm Blanket'),
       (10017, '', 'gulp', '', 'Gulp'),
       (10018, '', 'herbivore', '', 'Herbivore'),
       (10019, '', 'sandpit', '', 'Sandpit'),
       (10020, '', 'hot-blooded', '', 'Hot Blooded'),
       (10021, '', 'medic', '', 'Medic'),
       (10022, '', 'life-force', '', 'Life Force'),
       (10023, '', 'lunchbox', '', 'Lunchbox'),
       (10024, '', 'nurse', '', 'Nurse'),
       (10025, '', 'melee', '', 'Melee'),
       (10026, '', 'sponge', '', 'Sponge'),
       (10027, '', 'bodyguard', '', 'Bodyguard'),
       (10028, '', 'hero', '', 'Hero'),
       (10029, '', 'last-bastion', '', 'Last Bastion'),
       (10030, '', 'stealth', '', 'Stealth'),
       (10031, '', 'vanguard', '', 'Vanguard'),
       (10032, '', 'nomad', '', 'Nomad'),
       (10033, '', 'sequence', '', 'Sequence'),
       (10034, '', 'grass-cloak', '', 'Grass Cloak'),
       (10035, '', 'celebrate', '', 'Celebrate'),
       (10036, '', 'lullaby', '', 'Lullaby'),
       (10037, '', 'calming', '', 'Calming'),
       (10038, '', 'daze', '', 'Daze'),
       (10039, '', 'frighten', '', 'Frighten'),
       (10040, '', 'interference', '', 'Interference'),
       (10041, '', 'mood-maker', '', 'Mood Maker'),
       (10042, '', 'confidence', '', 'Confidence'),
       (10043, '', 'fortune', '', 'Fortune'),
       (10044, '', 'bonanza', '', 'Bonanza'),
       (10045, '', 'explode', '', 'Explode'),
       (10046, '', 'omnipotent', '', 'Omnipotent'),
       (10047, '', 'share', '', 'Share'),
       (10048, '', 'black-hole', '', 'Black Hole'),
       (10049, '', 'shadow-dash', '', 'Shadow Dash'),
       (10050, '', 'sprint', '', 'Sprint'),
       (10051, '', 'disgust', '', 'Disgust'),
       (10052, '', 'high-rise', '', 'High-rise'),
       (10053, '', 'climber', '', 'Climber'),
       (10054, '', 'flame-boost', '', 'Flame Boost'),
       (10055, '', 'aqua-boost', '', 'Aqua Boost'),
       (10056, '', 'run-up', '', 'Run Up'),
       (10057, '', 'conqueror', '', 'Conqueror'),
       (10058, '', 'shackle', '', 'Shackle'),
       (10059, '', 'decoy', '', 'Decoy'),
       (10060, '', 'shield', '', 'Shield');
COMMIT;