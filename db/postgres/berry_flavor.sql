BEGIN;
insert into public.berry_flavor (id, internal_name, name)
values (1, 'spicy', '辣'),
       (2, 'dry', '涩'),
       (3, 'sweet', '甜'),
       (4, 'bitter', '苦'),
       (5, 'sour', '酸');
COMMIT;