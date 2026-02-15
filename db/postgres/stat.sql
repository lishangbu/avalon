BEGIN;
insert into public.stat (id, game_index, internal_name, is_battle_only, name, move_damage_class_id)
values (1, 1, 'hp', false, 'HP', null),
       (2, 2, 'attack', false, '攻击', 2),
       (3, 3, 'defense', false, '防御', 2),
       (4, 5, 'special-attack', false, '特攻', 3),
       (5, 6, 'special-defense', false, '特防', 3),
       (6, 4, 'speed', false, '速度', null),
       (7, 0, 'accuracy', true, '命中', null),
       (8, 0, 'evasion', true, '闪避', null);
COMMIT;