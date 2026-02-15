BEGIN;
insert into public.gender (id, internal_name)
values (1, 'female'),
       (2, 'male'),
       (3, 'genderless');
COMMIT;