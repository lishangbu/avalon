BEGIN;
insert into public.berry_firmness (id, internal_name, name)
values (5, 'super-hard', '非常坚硬'),
       (4, 'very-hard', '很坚硬'),
       (3, 'hard', '坚硬'),
       (2, 'soft', '柔软'),
       (1, 'very-soft', '很柔软');
COMMIT;