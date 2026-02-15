BEGIN;
insert into public.move_category (id, description, internal_name, name)
values (0, 'Inflicts damage', 'damage', 'damage'),
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
       (13, 'Unique effect', 'unique', 'unique');
COMMIT;