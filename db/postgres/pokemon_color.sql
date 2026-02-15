BEGIN;
insert into public.pokemon_color (id, internal_name, name)
values (1, 'black', '黑色'),
       (2, 'blue', '蓝色'),
       (3, 'brown', '褐色'),
       (4, 'gray', '灰色'),
       (5, 'green', '绿色'),
       (6, 'pink', '粉红色'),
       (7, 'purple', '紫色'),
       (8, 'red', '红色'),
       (9, 'white', '白色'),
       (10, 'yellow', '黄色');
COMMIT;