BEGIN;
insert into public.move_target (id, description, internal_name, name)
values (1, 'One specific move.  How this move is chosen depends upon on the move being used.', 'specific-move',
        'specific-move'),
       (2, 'One other Pokémon on the field, selected by the trainer.  Stolen moves reuse the same target.',
        'selected-pokemon-me-first', 'selected-pokemon-me-first'),
       (3, 'The user''s ally (if any).', 'ally', 'ally'),
       (4, 'The user''s side of the field.  Affects the user and its ally (if any).', 'users-field', 'users-field'),
       (5, 'Either the user or its ally, selected by the trainer.', 'user-or-ally', 'user-or-ally'),
       (6, 'The opposing side of the field.  Affects opposing Pokémon.', 'opponents-field', 'opponents-field'),
       (7, 'The user.', 'user', 'user'),
       (8, 'One opposing Pokémon, selected at random.', 'random-opponent', 'random-opponent'),
       (9, 'Every other Pokémon on the field.', 'all-other-pokemon', 'all-other-pokemon'),
       (10, 'One other Pokémon on the field, selected by the trainer.', 'selected-pokemon', 'selected-pokemon'),
       (11, 'All opposing Pokémon.', 'all-opponents', 'all-opponents'),
       (12, 'The entire field.  Affects all Pokémon.', 'entire-field', 'entire-field'),
       (13, 'The user and its allies.', 'user-and-allies', 'user-and-allies'),
       (14, 'Every Pokémon on the field.', 'all-pokemon', 'all-pokemon'),
       (15, 'All of the user''s allies.', 'all-allies', 'all-allies'),
       (16, '', 'fainting-pokemon', 'fainting-pokemon');
COMMIT;