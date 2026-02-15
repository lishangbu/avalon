BEGIN;
insert into public.move_learn_method (id, description, internal_name, name)
values (1, 'Learned when a Pokémon reaches a certain level.', 'level-up', 'Level up'),
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
        'zygarde-cube', 'Zygarde Cube');
COMMIT;