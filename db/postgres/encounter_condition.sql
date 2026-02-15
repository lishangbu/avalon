BEGIN;
insert into public.encounter_condition (id, internal_name, name)
values (1, 'swarm', 'Swarm'),
       (2, 'time', 'Time of day'),
       (3, 'radar', 'PokeRadar'),
       (4, 'slot2', 'Gen 3 game in slot 2'),
       (5, 'radio', 'Radio'),
       (6, 'season', 'Season'),
       (7, 'starter', 'Chosen Starter'),
       (8, 'tv-option', 'Chosen dialogue at the news report'),
       (9, 'story-progress', 'Story Progress'),
       (10, 'other', 'Miscellaneous'),
       (11, 'item', 'item'),
       (12, 'weekday', 'weekday'),
       (13, 'first-party-pokemon', 'first-party-pokemon'),
       (14, 'special-encounter', 'special-encounter');
COMMIT;