-- @formatter:off
BEGIN TRANSACTION;
-- 插入蛋群数据
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (1,'Monster','怪兽','像是怪兽一样，或者比较野性。','这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (2,'Water 1','水中1','可以两栖或多栖。','这个蛋群的宝可梦大多原型基于两栖动物和水边生活的多栖动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (3,'Bug','虫','外表长得像虫子。','这个蛋群的宝可梦大多原型基于昆虫和节肢动物。但也有例外，例如小嘴蜗、壶壶原型是软体动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (4,'Flying','飞行','外表长得像鸟、蝙蝠等会飞行的生物。','这个蛋群的宝可梦原型大多基于鸟类、蝙蝠、会飞的爬行动物甚至是神话中会飞的小妖精。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (5,'Field','陆上','最大的蛋群，住在陆地上的宝可梦基本都属于这个群。','这个蛋群的宝可梦大多原型基于哺乳动物和爬行动物，以及翅膀退化的鸟类。但也有例外，例如橡实果原型是植物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (6,'Fairy','妖精','外表可爱或具有传说灵异性质的生物。','这个蛋群的宝可梦大多原型基于可爱的小型动物和神话中的妖精。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (7,'Grass','植物','外表长得像植物。','这个蛋群的宝可梦大多原型基于植物和真菌，以及身上长有植物或真菌的动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (8,'Human-Like','人型','两足行走。','这个蛋群的宝可梦都是直立行走的人型生物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (9,'Water 3','水中3','水中无脊椎动物。','这个蛋群的宝可梦大多原型基于非鱼类的深海水生动物。但也有例外，例如始祖小鸟原型是爬行动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (10,'Mineral','矿物','结晶或硅基生物。','这个蛋群的宝可梦大多原型基于无机物和身上带有无机物的生物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (11,'Amorphous','不定形','没有固定外表。','这个蛋群的宝可梦大多原型基于软体动物、灵体，以及身体柔软的生物或非生物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (12,'Water 2','水中2','像是鱼一类的脊椎动物。','这个蛋群的宝可梦大多原型基于鱼类，乌贼以及章鱼。但也有例外，例如吼鲸王原型是哺乳动物。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (13,'Ditto','百变怪','顾名思义，百变怪是这个群中唯一的宝可梦，可以和除了未发现群及百变怪群以外的所有宝可梦生蛋（无视性别）。','这个蛋群只有百变怪。处于这个蛋群的宝可梦可以与除未发现群和百变怪群外的任何蛋群的宝可梦生蛋，蛋的种类必然是另一方。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (14,'Dragon','龙','外表长得像龙或者具有龙的特质的宝可梦。','这个蛋群的宝可梦大多原型基于传说中的龙以及与龙有关的动物（蜥蜴、海马等）。但也有例外，例如七夕青鸟原型是鸟类但是它是龙属性，鲤鱼王、丑丑鱼原型是鱼类但是它们进化后的原型是传说中的龙。');
INSERT INTO EGG_GROUP (ID,INTERNAL_NAME,NAME,TEXT,CHARACTERISTICS) VALUES (15,'Undiscovered','未发现','不能和任何宝可梦生蛋。','属于此蛋群的宝可梦都无法生蛋。换装皮卡丘、小智版甲贺忍蛙、戴着帽子的皮卡丘为特殊形态的宝可梦，不能生蛋。尼多娜和尼多后进化前的尼多兰不属于这个蛋群可以生蛋，幼年宝可梦进化后不属于这个蛋群可以生蛋。');
-- 插入招式分类数据
INSERT INTO MOVE_CATEGORY (ID,INTERNAL_NAME, DESCRIPTION, NAME) VALUES ( 1,'Physical', '物理招式是一种能够造成伤害的招式。如果招式特效没有特别说明，则此类招式使用攻击方的攻击与防御方的防御来计算最终的伤害。','物理');
INSERT INTO MOVE_CATEGORY (ID,INTERNAL_NAME,DESCRIPTION,  NAME) VALUES ( 2,'Special', '特殊招式是一种能够造成伤害的招式。如果招式特效没有特别说明，则此类招式使用攻击方的特攻与防御方的特防来计算最终的伤害。','特殊');
INSERT INTO MOVE_CATEGORY (ID,INTERNAL_NAME,DESCRIPTION,  NAME) VALUES ( 3,'Status', '变化招式是不能够直接造成伤害的招式。此类招式的威力全部为零，尽管有的招式可以通过施加状态来间接对对方造成伤害。','变化');
--插入属性数据
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 1,'Normal', '一般');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 2,'Fire', '火');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 3,'Fighting', '格斗');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 4,'Water', '水');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 5,'Flying', '飞行');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 6,'Grass', '草');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 7,'Poison', '毒');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 8,'Electric', '电');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 9,'Ground', '地面');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 10,'Psychic', '超能力');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 11,'Rock', '岩石');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 12,'Ice', '冰');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 13,'Bug', '虫');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 14,'Dragon', '龙');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 15,'Ghost', '幽灵');
INSERT INTO TYPE (ID,INTERNAL_NAME,  NAME) VALUES ( 16,'Dark', '恶');
INSERT INTO TYPE (ID,INTERNAL_NAME,NAME) VALUES ( 17,'Steel', '钢');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 18,'Fairy', '妖精');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 19,'Stellar', '星晶');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 20,'???', '???');
INSERT INTO TYPE (ID,INTERNAL_NAME, NAME) VALUES ( 21,'None', '无属性');
-- 插入特性数据
INSERT INTO ABILITY (ID,INTERNAL_NAME,EFFECT,INFO,NAME,TEXT) VALUES (1,'Stench','使用招式攻击对手造成伤害时，对方有10%几率陷入畏缩状态。\n该效果与王者之证／锐利之牙不叠加。\n该效果与特效为畏缩的招式（比如空气之刃）不叠加。\n连续招式每一下连续的攻击都会有相同的几率造成对手畏缩。','"可以被交换", "可以被其他特性覆盖", "可以被其他宝可梦复制", "受无特性状态影响", "变身时特性有效", "不在入场时发动"','恶臭','通过释放臭臭的气味，在攻击的时候，有时会使对手畏缩。');
COMMIT;
-- @formatter:on