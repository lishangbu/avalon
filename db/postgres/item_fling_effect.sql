BEGIN;
insert into public.item_fling_effect (id, effect, internal_name, name)
values  (1, 'Badly poisons the target.', 'badly-poison', 'badly-poison'),
        (2, 'Burns the target.', 'burn', 'burn'),
        (3, 'Immediately activates the berry’s effect on the target.', 'berry-effect', 'berry-effect'),
        (4, 'Immediately activates the herb’s effect on the target.', 'herb-effect', 'herb-effect'),
        (5, 'Paralyzes the target.', 'paralyze', 'paralyze'),
        (6, 'Poisons the target.', 'poison', 'poison'),
        (7, 'Target will flinch if it has not yet gone this turn.', 'flinch', 'flinch');
COMMIT;