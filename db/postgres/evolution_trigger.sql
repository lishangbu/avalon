BEGIN;
insert into public.evolution_trigger (id, internal_name, name)
values (1, 'level-up', 'Level up'),
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
       (16, 'gimmmighoul-coins', 'Collect 999 Gimmighoul Coins');
COMMIT;