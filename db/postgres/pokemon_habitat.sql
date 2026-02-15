BEGIN;
insert into public.pokemon_habitat (id, internal_name, name)
values (1, 'cave', 'cave'),
       (2, 'forest', 'forest'),
       (3, 'grassland', 'grassland'),
       (4, 'mountain', 'mountain'),
       (5, 'rare', 'rare'),
       (6, 'rough-terrain', 'rough terrain'),
       (7, 'sea', 'sea'),
       (8, 'urban', 'urban'),
       (9, 'waters-edge', 'water''s edge');
COMMIT;