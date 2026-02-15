BEGIN;
insert into public.item_attribute (id, description, internal_name, name)
values (1, 'Has a count in the bag', 'countable', 'Countable'),
       (2, 'Consumed when used', 'consumable', 'Consumable'),
       (3, 'Usable outside battle', 'usable-overworld', 'Usable_overworld'),
       (4, 'Usable in battle', 'usable-in-battle', 'Usable_in_battle'),
       (5, 'Can be held by a Pokémon', 'holdable', 'Holdable'),
       (6, 'Works passively when held', 'holdable-passive', 'Holdable_passive'),
       (7, 'Usable by a Pokémon when held', 'holdable-active', 'Holdable_active'),
       (8, 'Appears in Sinnoh Underground', 'underground', 'Underground');
COMMIT;