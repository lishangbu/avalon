BEGIN;
insert into public.move_damage_class (id, description, internal_name, name)
values (1, '没有伤害', 'status', '变化'),
       (2, '物理伤害，受攻击和防御影响', 'physical', '物理'),
       (3, '特殊伤害，受特攻和特防影响', 'special', '特殊');
COMMIT;