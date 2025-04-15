-- @formatter:off
BEGIN TRANSACTION;
-- 插入蛋群数据
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Monster','怪兽','像是怪兽一样，或者比较野性。','这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Water 1','水中1','可以两栖或多栖。','这个蛋群的宝可梦大多原型基于两栖动物和水边生活的多栖动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Bug','虫','外表长得像虫子。','这个蛋群的宝可梦大多原型基于昆虫和节肢动物。但也有例外，例如小嘴蜗、壶壶原型是软体动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Flying','飞行','外表长得像鸟、蝙蝠等会飞行的生物。','这个蛋群的宝可梦原型大多基于鸟类、蝙蝠、会飞的爬行动物甚至是神话中会飞的小妖精。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Field','陆上','最大的蛋群，住在陆地上的宝可梦基本都属于这个群。','这个蛋群的宝可梦大多原型基于哺乳动物和爬行动物，以及翅膀退化的鸟类。但也有例外，例如橡实果原型是植物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Fairy','妖精','外表可爱或具有传说灵异性质的生物。','这个蛋群的宝可梦大多原型基于可爱的小型动物和神话中的妖精。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Grass','植物','外表长得像植物。','这个蛋群的宝可梦大多原型基于植物和真菌，以及身上长有植物或真菌的动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Human-Like','人型','两足行走。','这个蛋群的宝可梦都是直立行走的人型生物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Water 3','水中3','水中无脊椎动物。','这个蛋群的宝可梦大多原型基于非鱼类的深海水生动物。但也有例外，例如始祖小鸟原型是爬行动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Mineral','矿物','结晶或硅基生物。','这个蛋群的宝可梦大多原型基于无机物和身上带有无机物的生物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Amorphous','不定形','没有固定外表。','这个蛋群的宝可梦大多原型基于软体动物、灵体，以及身体柔软的生物或非生物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Water 2','水中2','像是鱼一类的脊椎动物。','这个蛋群的宝可梦大多原型基于鱼类，乌贼以及章鱼。但也有例外，例如吼鲸王原型是哺乳动物。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Ditto','百变怪','顾名思义，百变怪是这个群中唯一的宝可梦，可以和除了未发现群及百变怪群以外的所有宝可梦生蛋（无视性别）。','这个蛋群只有百变怪。处于这个蛋群的宝可梦可以与除未发现群和百变怪群外的任何蛋群的宝可梦生蛋，蛋的种类必然是另一方。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Dragon','龙','外表长得像龙或者具有龙的特质的宝可梦。','这个蛋群的宝可梦大多原型基于传说中的龙以及与龙有关的动物（蜥蜴、海马等）。但也有例外，例如七夕青鸟原型是鸟类但是它是龙属性，鲤鱼王、丑丑鱼原型是鱼类但是它们进化后的原型是传说中的龙。');
INSERT INTO EGG_GROUP ("group",NAME,TEXT,CHARACTERISTICS) VALUES ('Undiscovered','未发现','不能和任何宝可梦生蛋。','属于此蛋群的宝可梦都无法生蛋。换装皮卡丘、小智版甲贺忍蛙、戴着帽子的皮卡丘为特殊形态的宝可梦，不能生蛋。尼多娜和尼多后进化前的尼多兰不属于这个蛋群可以生蛋，幼年宝可梦进化后不属于这个蛋群可以生蛋。');
-- 插入世代数据
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (1, 'I', '第一世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (2, 'II', '第二世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (3, 'III', '第三世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (4, 'IV', '第四世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (5, 'V', '第五世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (6, 'VI', '第六世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (7, 'VII', '第七世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (8, 'VIII', '第八世代');
INSERT INTO GENERATION (ID, CODE, NAME) VALUES (9, 'IX', '第九世代');
-- 插入招式分类数据
INSERT INTO MOVE_CATEGORY (CATEGORY,  NAME) VALUES ( 'Physical', '物理');
INSERT INTO MOVE_CATEGORY (CATEGORY,  NAME) VALUES ( 'Special', '特殊');
INSERT INTO MOVE_CATEGORY (CATEGORY,  NAME) VALUES ( 'Status', '变化');
--插入属性数据
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Normal', '一般');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Fire', '火');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Fighting', '格斗');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Water', '水');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Flying', '飞行');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Grass', '草');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Poison', '毒');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Electric', '电');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Ground', '地面');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Psychic', '超能力');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Rock', '岩石');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Ice', '冰');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Bug', '虫');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Dragon', '龙');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Ghost', '幽灵');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Dark', '恶');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Steel', '钢');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Fairy', '妖精');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'Stellar', '星晶');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( '???', '???');
INSERT INTO TYPE (TYPE,  NAME) VALUES ( 'None', '无属性');
COMMIT;
-- @formatter:on